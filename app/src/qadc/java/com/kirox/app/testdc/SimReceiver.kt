package com.kirox.app.testdc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.kirox.app.BuildConfig
import com.kirox.app.KiroXApplication
import com.kirox.app.telegram.SessionLocks
import com.kirox.app.telegram.TdException
import com.kirox.app.telegram.TelegramAccount
import com.kirox.app.telegram.TelegramAccounts
import com.kirox.app.telegram.TelegramSessionManager
import com.kirox.app.telegram.messagePreview
import com.kirox.app.telegram.tdlibParameters
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * qadc build (package com.kirox.app.testdc) only: "simulated users" for QA agents. A broadcast tells one of the app's signed-in
 * test accounts to act in a chat (send / reply / react / typing / read / edit / delete / dump); results
 * go to logcat under the tag [TAG]. An account that is not open in the UI gets its own short-lived
 * TDLib client (like a notification peek); the open account uses the app's own client.
 *
 *   adb shell am broadcast -a com.kirox.app.testdc.SIM -p com.kirox.app.testdc --es cmd send --es as 1234 --es chat "Alice" --es text "hi"
 *
 * Full reference: design-reference/kirox-v3/qa/testdc.md
 */
class SimReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Belt and braces: this class only exists in the qadc source set (package com.kirox.app.testdc), which always uses the test DC.
        if (!BuildConfig.TELEGRAM_USE_TEST_DC) return
        val extras = Bundle(intent.extras ?: Bundle())
        val pending = goAsync()
        SimHub.handle(context.applicationContext, extras) { pending.finish() }
    }
}

private const val TAG = "KxSim"

private fun log(rid: String, line: String) {
    // One logcat line per entry; logcat cuts long lines, so text is kept short and on one line.
    Log.i(TAG, (if (rid.isNotEmpty()) "[$rid] " else "") + line.replace('\n', '⏎').take(900))
}

/** Something that answers TDLib queries for one account. */
private interface Td {
    suspend fun <T : TdApi.Object> q(function: TdApi.Function<T>): T
}

private object SimHub {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val sessions = HashMap<String, SimSession>()
    private const val COMMAND_TIMEOUT_MS = 50_000L

    fun handle(context: Context, extras: Bundle, done: () -> Unit) {
        val rid = extras.getString("rid").orEmpty()
        val cmd = extras.getString("cmd").orEmpty().lowercase(Locale.ROOT)
        scope.launch {
            try {
                withTimeout(COMMAND_TIMEOUT_MS) { run(context, cmd, extras, rid) }
            } catch (error: TdException) {
                log(rid, "ERROR $cmd: ${error.error.code} ${error.error.message}")
            } catch (error: Throwable) {
                log(rid, "ERROR $cmd: ${error.javaClass.simpleName}: ${error.message}")
            } finally {
                done()
            }
        }
    }

    private suspend fun run(context: Context, cmd: String, x: Bundle, rid: String) {
        val accounts = TelegramAccounts(context).accounts.value
        if (cmd == "accounts" || cmd.isEmpty() || cmd == "help") {
            val active = (context as? KiroXApplication)?.telegram?.client?.activeSession
            accounts.forEach { log(rid, "account session=${it.session} title=\"${it.title}\" phone=…${it.phone.takeLast(4)}${if (it.session == active) " (open in UI)" else ""}") }
            if (cmd != "accounts") log(rid, "commands: accounts chats dump send reply react typing read edit delete join online close — see qa/testdc.md")
            log(rid, "OK accounts ${accounts.size}")
            return
        }
        val account = pickAccount(accounts, x.getString("as").orEmpty())
        if (cmd == "close") { sessions.remove(account.session)?.close(); log(rid, "OK close ${account.session}"); return }
        val td = tdFor(context, account)
        val who = account.title.ifBlank { account.session }
        when (cmd) {
            "online" -> {
                val on = x.getString("value", "true") != "false"
                td.q(TdApi.SetOption("online", TdApi.OptionValueBoolean(on)))
                log(rid, "OK online=$on $who"); return
            }
            "chats" -> {
                runCatching { td.q(TdApi.LoadChats(TdApi.ChatListMain(), x.getInt("limit", 30))) }
                val ids = td.q(TdApi.GetChats(TdApi.ChatListMain(), x.getInt("limit", 30))).chatIds
                ids.forEach { id -> val chat = td.q(TdApi.GetChat(id)); log(rid, "chat id=$id title=\"${chat.title}\" unread=${chat.unreadCount}") }
                log(rid, "OK chats ${ids.size} as $who"); return
            }
        }
        val chatRef = x.getString("chat") ?: error("missing --es chat <title | @username | +phone | chat id | me>")
        val chatId = resolveChat(td, chatRef)
        val chat = td.q(TdApi.GetChat(chatId))
        val myId = td.q(TdApi.GetMe()).id
        when (cmd) {
            "dump" -> dump(td, chat, x.getInt("limit", 20), myId, rid)
            "send", "reply" -> {
                val text = x.getString("text") ?: error("missing --es text")
                val replyTo = if (cmd == "reply" || x.containsKey("msg") || x.containsKey("back")) targetMessage(td, chatId, x, myId) else null
                val sent = td.q(TdApi.SendMessage(chatId, null, replyTo?.let { TdApi.InputMessageReplyToMessage(it.id, null, 0, "") }, null, null, textContent(text, x.getBoolean("md", false))))
                log(rid, "OK $cmd as $who in \"${chat.title}\" tempId=${sent.id}${replyTo?.let { " replyTo=${it.id}" }.orEmpty()}")
            }
            "react" -> {
                val emoji = x.getString("emoji") ?: "👍"
                val target = targetMessage(td, chatId, x, myId)
                if (x.getBoolean("remove", false)) td.q(TdApi.RemoveMessageReaction(chatId, target.id, TdApi.ReactionTypeEmoji(emoji)))
                else td.q(TdApi.AddMessageReaction(chatId, target.id, TdApi.ReactionTypeEmoji(emoji), x.getBoolean("big", false), false))
                log(rid, "OK react $emoji${if (x.getBoolean("remove", false)) " removed" else ""} as $who on #${target.id} in \"${chat.title}\"")
            }
            "typing" -> {
                val action = chatAction(x.getString("action", "typing"))
                val seconds = x.getInt("secs", 5).coerceIn(1, 40)
                // Telegram shows an action for ~5 s: it is repeated until the time is up, then cancelled.
                var left = seconds
                while (left > 0) {
                    td.q(TdApi.SendChatAction(chatId, null, "", action))
                    val step = minOf(4, left); delay(step * 1000L); left -= step
                }
                if (action !is TdApi.ChatActionCancel) td.q(TdApi.SendChatAction(chatId, null, "", TdApi.ChatActionCancel()))
                log(rid, "OK typing ${x.getString("action", "typing")} ${seconds}s as $who in \"${chat.title}\"")
            }
            "read" -> {
                td.q(TdApi.OpenChat(chatId))
                val ids = history(td, chatId, x.getInt("limit", 20)).map { it.id }.toLongArray()
                td.q(TdApi.ViewMessages(chatId, ids, TdApi.MessageSourceChatHistory(), true))
                td.q(TdApi.CloseChat(chatId))
                log(rid, "OK read ${ids.size} as $who in \"${chat.title}\"")
            }
            "edit" -> {
                val text = x.getString("text") ?: error("missing --es text")
                val target = targetMessage(td, chatId, x, myId, own = true)
                td.q(TdApi.EditMessageText(chatId, target.id, null, textContent(text, x.getBoolean("md", false))))
                log(rid, "OK edit #${target.id} as $who in \"${chat.title}\"")
            }
            "delete" -> {
                val target = targetMessage(td, chatId, x, myId, own = true)
                td.q(TdApi.DeleteMessages(chatId, longArrayOf(target.id), x.getBoolean("revoke", true)))
                log(rid, "OK delete #${target.id} as $who in \"${chat.title}\"")
            }
            "join" -> { td.q(TdApi.JoinChat(chatId)); log(rid, "OK join \"${chat.title}\" as $who") }
            else -> error("unknown cmd \"$cmd\"")
        }
    }

    private fun pickAccount(accounts: List<TelegramAccount>, ref: String): TelegramAccount {
        require(ref.isNotBlank()) { "missing --es as <session | last digits of the phone | name>; cmd=accounts lists them" }
        val digits = ref.filter(Char::isDigit)
        return accounts.firstOrNull { it.session == ref }
            ?: accounts.firstOrNull { digits.length >= 3 && digits.length == ref.trimStart('+').length && it.phone.filter(Char::isDigit).endsWith(digits) }
            ?: accounts.firstOrNull { it.title.equals(ref, ignoreCase = true) }
            ?: accounts.firstOrNull { it.title.contains(ref, ignoreCase = true) }
            ?: error("no account \"$ref\"; cmd=accounts lists them")
    }

    /** The app's own client when it has this account open; otherwise a client of the harness. */
    private suspend fun tdFor(context: Context, account: TelegramAccount): Td {
        val main = (context as? KiroXApplication)?.telegram?.client
        if (main != null && main.activeSession == account.session && SessionLocks.isHeld(account.session) && sessions[account.session]?.closed != false) {
            check(main.ready.value) { "account ${account.session} is open in the UI but not signed in / not ready" }
            return object : Td { override suspend fun <T : TdApi.Object> q(function: TdApi.Function<T>): T = main.query(function) }
        }
        val existing = sessions[account.session]?.takeIf { !it.closed }
        val sim = existing ?: SimSession(context, account).also { sessions[account.session] = it; it.open() }
        sim.awaitReady()
        sim.touch()
        return object : Td { override suspend fun <T : TdApi.Object> q(function: TdApi.Function<T>): T = sim.query(function) }
    }

    private suspend fun resolveChat(td: Td, ref: String): Long {
        ref.toLongOrNull()?.let { return it }
        if (ref.equals("me", true) || ref.equals("saved", true)) return td.q(TdApi.CreatePrivateChat(td.q(TdApi.GetMe()).id, false)).id
        if (ref.startsWith("@")) return td.q(TdApi.SearchPublicChat(ref.drop(1))).id
        if (ref.startsWith("+")) {
            val user = td.q(TdApi.SearchUserByPhoneNumber(ref.drop(1), false))
            return td.q(TdApi.CreatePrivateChat(user.id, false)).id
        }
        // A title: make sure the main list is loaded, then search the known chats.
        runCatching { td.q(TdApi.LoadChats(TdApi.ChatListMain(), 100)) }
        val ids = td.q(TdApi.SearchChats(ref, null, 50)).chatIds
        val chats = ids.map { td.q(TdApi.GetChat(it)) }
        return (chats.firstOrNull { it.title.equals(ref, true) } ?: chats.firstOrNull())?.id ?: error("no chat \"$ref\"")
    }

    private suspend fun history(td: Td, chatId: Long, limit: Int): List<TdApi.Message> {
        val out = ArrayList<TdApi.Message>()
        var from = 0L
        // TDLib may answer with fewer messages than asked (local part first): ask again from the oldest one.
        repeat(5) {
            if (out.size >= limit) return@repeat
            val page = td.q(TdApi.GetChatHistory(chatId, from, 0, limit - out.size, false)).messages.orEmpty().filterNotNull()
            if (page.isEmpty()) return out
            out += page.filter { m -> out.none { it.id == m.id } }
            from = page.last().id
        }
        return out.take(limit)
    }

    /** --el msg <id>, or --ei back N (0 = newest), or by default the newest message not sent by this account. */
    private suspend fun targetMessage(td: Td, chatId: Long, x: Bundle, myId: Long, own: Boolean = false): TdApi.Message {
        if (x.containsKey("msg")) return td.q(TdApi.GetMessage(chatId, x.getLong("msg")))
        val list = history(td, chatId, 50)
        if (x.containsKey("back")) return list.getOrNull(x.getInt("back")) ?: error("no message back=${x.getInt("back")}")
        return list.firstOrNull { mine(it, myId) == own } ?: error(if (own) "no own message to target" else "no message from others to target")
    }

    private fun mine(m: TdApi.Message, myId: Long) = (m.senderId as? TdApi.MessageSenderUser)?.userId == myId || m.isOutgoing

    private suspend fun dump(td: Td, chat: TdApi.Chat, limit: Int, myId: Long, rid: String) {
        val names = HashMap<Long, String>()
        suspend fun name(sender: TdApi.MessageSender?): String = when (sender) {
            is TdApi.MessageSenderUser -> names.getOrPut(sender.userId) {
                runCatching { td.q(TdApi.GetUser(sender.userId)).let { u -> "${u.firstName} ${u.lastName}".trim() } }.getOrDefault("user${sender.userId}")
            }
            is TdApi.MessageSenderChat -> names.getOrPut(sender.chatId) { runCatching { td.q(TdApi.GetChat(sender.chatId)).title }.getOrDefault("chat${sender.chatId}") }
            else -> "?"
        }
        val time = SimpleDateFormat("HH:mm:ss", Locale.ROOT)
        val messages = history(td, chat.id, limit.coerceIn(1, 100))
        log(rid, "dump \"${chat.title}\" id=${chat.id} unread=${chat.unreadCount} lastReadOutbox=${chat.lastReadOutboxMessageId} (newest first)")
        messages.forEach { m ->
            val reply = (m.replyTo as? TdApi.MessageReplyToMessage)?.let { " reply->#${it.messageId}" }.orEmpty()
            val reactions = m.interactionInfo?.reactions?.reactions.orEmpty().joinToString(" ") { r ->
                ((r.type as? TdApi.ReactionTypeEmoji)?.emoji ?: "★") + r.totalCount + if (r.isChosen) "*" else ""
            }.let { if (it.isEmpty()) "" else " [$it]" }
            val flags = buildString {
                if (mine(m, myId)) append(" out")
                if (m.editDate != 0) append(" edited")
                if (m.sendingState != null) append(" sending")
            }
            log(rid, "#${m.id} ${time.format(Date(m.date * 1000L))} ${name(m.senderId)}$flags$reply$reactions: ${messagePreview(m)}")
        }
        log(rid, "OK dump ${messages.size}")
    }

    private fun textContent(text: String, markdown: Boolean): TdApi.InputMessageText {
        val formatted = if (markdown) runCatching {
            Client.execute(TdApi.ParseTextEntities(text, TdApi.TextParseModeMarkdown(2))) as TdApi.FormattedText
        }.getOrElse { TdApi.FormattedText(text, emptyArray()) } else TdApi.FormattedText(text, emptyArray())
        return TdApi.InputMessageText(formatted, null, true)
    }

    private fun chatAction(name: String): TdApi.ChatAction = when (name.lowercase(Locale.ROOT)) {
        "typing" -> TdApi.ChatActionTyping()
        "voice", "recording_voice" -> TdApi.ChatActionRecordingVoiceNote()
        "video", "recording_video" -> TdApi.ChatActionRecordingVideo()
        "videonote", "recording_video_note" -> TdApi.ChatActionRecordingVideoNote()
        "photo", "upload_photo" -> TdApi.ChatActionUploadingPhoto(0)
        "document", "upload_document" -> TdApi.ChatActionUploadingDocument(0)
        "sticker", "choosing_sticker" -> TdApi.ChatActionChoosingSticker()
        "location" -> TdApi.ChatActionChoosingLocation()
        "contact" -> TdApi.ChatActionChoosingContact()
        "game" -> TdApi.ChatActionStartPlayingGame()
        "cancel" -> TdApi.ChatActionCancel()
        else -> error("unknown action \"$name\"")
    }

    /** A harness-owned TDLib client for an account that is not open in the UI; closes itself when idle. */
    private class SimSession(context: Context, val account: TelegramAccount) {
        private val session = TelegramSessionManager(context, account.session)
        private val ready = CompletableDeferred<Unit>()
        @Volatile private var client: Client? = null
        @Volatile private var lock: Any? = null
        private val wantsParameters = java.util.concurrent.atomic.AtomicBoolean(false)
        @Volatile private var lastUsed = System.currentTimeMillis()
        @Volatile var closed = false; private set
        private val main = android.os.Handler(android.os.Looper.getMainLooper())

        fun open() {
            val key = session.keyStore.load() ?: error("account ${account.session} has no stored key (never signed in)")
            check(session.hasDatabase) { "account ${account.session} has no database (never signed in)" }
            // The main client (user switches to this account) or a notification peek asks us to let go.
            lock = SessionLocks.acquireHandle(account.session) { close() }
                ?: error("account ${account.session} is busy (open in the UI or in a notification peek); try again")
            val handler = Client.ResultHandler { update -> onUpdate(update, key) }
            val created = runCatching { Client.create(handler, null, null) }.getOrElse {
                release(); throw it
            }
            client = created
            if (wantsParameters.compareAndSet(true, false)) created.send(tdlibParameters(session, key), null)
            scheduleIdleCheck()
        }

        private fun onUpdate(update: TdApi.Object, key: ByteArray) {
            when (update) {
                is TdApi.UpdateAuthorizationState -> when (val state = update.authorizationState) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> {
                        // May arrive before Client.create returned: open() sends it then.
                        wantsParameters.set(true)
                        client?.let { c -> if (wantsParameters.compareAndSet(true, false)) c.send(tdlibParameters(session, key), null) }
                    }
                    is TdApi.AuthorizationStateReady -> { log("", "client ready: ${account.session}"); ready.complete(Unit) }
                    is TdApi.AuthorizationStateClosed -> { closed = true; release(); ready.completeExceptionally(IllegalStateException("closed")) }
                    is TdApi.AuthorizationStateWaitPhoneNumber, is TdApi.AuthorizationStateWaitCode, is TdApi.AuthorizationStateWaitPassword,
                    is TdApi.AuthorizationStateLoggingOut -> {
                        ready.completeExceptionally(IllegalStateException("account ${account.session} is not signed in (${state.javaClass.simpleName})"))
                        close()
                    }
                    else -> Unit
                }
                is TdApi.UpdateMessageSendSucceeded -> log("", "sent ${account.session}: #${update.oldMessageId} -> #${update.message.id}")
                is TdApi.UpdateMessageSendFailed -> log("", "send FAILED ${account.session}: #${update.oldMessageId} ${update.error.code} ${update.error.message}")
                else -> Unit
            }
        }

        suspend fun awaitReady() = ready.await()

        fun touch() { lastUsed = System.currentTimeMillis() }

        @Suppress("UNCHECKED_CAST")
        suspend fun <T : TdApi.Object> query(function: TdApi.Function<T>): T = suspendCancellableCoroutine { cont ->
            touch()
            val c = client ?: return@suspendCancellableCoroutine cont.resumeWithException(IllegalStateException("client closed"))
            c.send(function) { result ->
                touch()
                if (result is TdApi.Error) cont.resumeWithException(TdException(result)) else cont.resume(result as T)
            }
        }

        fun close() {
            if (closed) return
            client?.send(TdApi.Close(), null)
        }

        private fun release() { lock?.let { SessionLocks.release(account.session, it) }; lock = null }

        private fun scheduleIdleCheck() {
            main.postDelayed({
                if (closed) return@postDelayed
                if (System.currentTimeMillis() - lastUsed > IDLE_MS) { log("", "closing idle client ${account.session}"); close() } else scheduleIdleCheck()
            }, 15_000)
        }

        companion object { const val IDLE_MS = 180_000L }
    }
}
