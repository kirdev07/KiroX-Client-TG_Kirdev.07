package com.kirox.app.telegram

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

/**
 * Call history. TDLib has no separate call log: it is the list of MessageCall / MessageGroupCall
 * messages from SearchCallMessages, paged by an opaque offset, plus new call messages from updates.
 *
 * lead walk-through (release fix): the log stayed empty on the real account. The page request was
 * sent whenever the UI asked, also before TDLib reached AuthorizationStateReady or while no client was
 * running (start-up, background pause, session lock): `client.send` then returned false and its
 * callback never ran, so `loading` stayed true for the whole session and every later ask (the Calls tab
 * shown, scrolling) returned at the `loading` check; an answer lost with a closing client did the same.
 * Now a page is asked only while TDLib is Ready (Ready itself re-reads the first page), an unsent or
 * unanswered request never blocks the log, and answers from an older request are ignored.
 */
class TelegramCallRepository(private val client: TelegramClient, private val chats: TelegramChatRepository) {
    private val calls = HashMap<Pair<Long, Long>, TdApi.Message>() // (chatId, messageId)
    private var nextOffset = ""
    private var loading = false
    private var complete = false
    private var failures = 0
    private var failed = false
    /** Generation of the request in flight; answers of older requests are dropped. */
    private var request = 0
    private var emptyPages = 0

    private val _state = MutableStateFlow(TelegramCallsState())
    val state: StateFlow<TelegramCallsState> = _state.asStateFlow()

    init {
        client.addUpdateHandler { update ->
            when (update) {
                is TdApi.UpdateNewMessage -> if (isCallContent(update.message.content)) {
                    calls[key(update.message)] = update.message; publish()
                }
                is TdApi.UpdateMessageContent -> calls[key(update.chatId, update.messageId)]?.let {
                    it.content = update.newContent; publish()
                }
                is TdApi.UpdateDeleteMessages -> if (update.isPermanent) {
                    var removed = false
                    update.messageIds.forEach { if (calls.remove(key(update.chatId, it)) != null) removed = true }
                    if (removed) publish()
                }
                is TdApi.UpdateAuthorizationState -> when (update.authorizationState) {
                    // Requests sent before Ready fail or are never answered: the log is (re)read from here.
                    is TdApi.AuthorizationStateReady -> {
                        request++; loading = false; nextOffset = ""; complete = false; failed = false; failures = 0; emptyPages = 0
                        requestPage()
                    }
                    is TdApi.AuthorizationStateClosed -> {
                        request++; loading = false
                        // A background pause keeps the list; logout / account switch start from nothing.
                        if (!client.paused) {
                            calls.clear(); nextOffset = ""; complete = false; failures = 0; failed = false; emptyPages = 0
                        }
                        publish()
                    }
                }
            }
        }
    }

    /**
     * Loads the next page. [retry] (a screen showing the log) also restarts a log whose earlier
     * requests failed and re-asks an empty «complete» one.
     */
    fun loadMore(retry: Boolean = false): Unit = client.post {
        if (retry && failed) { failed = false; failures = 0 }
        if (retry && complete && calls.isEmpty()) { complete = false; nextOffset = ""; emptyPages = 0 }
        requestPage()
    }

    /** Runs on the TDLib dispatcher. */
    private fun requestPage() {
        if (loading || complete || failed) return
        // Not signed in / not running yet: the Ready update asks for the first page.
        if (!client.ready.value) return
        loading = true
        val id = ++request
        publish()
        val sent = client.send(TdApi.SearchCallMessages(nextOffset, PAGE_SIZE, false)) { result ->
            if (id != request) return@send
            loading = false
            when (result) {
                is TdApi.FoundMessages -> {
                    failures = 0
                    val page = callPage(result)
                    page.messages.forEach { calls[key(it)] = it }
                    nextOffset = page.nextOffset
                    complete = page.complete
                    if (page.messages.isNotEmpty()) emptyPages = 0
                    // TDLib may answer with an empty chunk that still has an offset: read on, a few times.
                    else if (!complete) { if (++emptyPages <= MAX_EMPTY_PAGES) client.post { requestPage() } else complete = true }
                }
                is TdApi.Error -> when {
                    !client.ready.value -> Unit // the Ready update asks again
                    ++failures <= 3 -> client.post(3_000L * failures) { requestPage() }
                    else -> failed = true
                }
            }
            publish()
        }
        if (!sent) { request++; loading = false; publish(); return }
        // An answer lost with a closing client must not block the log: the next ask sends again.
        client.post(ANSWER_TIMEOUT_MS) { if (id == request && loading) { request++; loading = false; publish() } }
    }

    private fun publish() {
        _state.value = TelegramCallsState(
            calls.values.mapNotNull { message ->
                val chat = chats.chat(message.chatId)
                toCall(message, chat?.title.orEmpty(), chat?.photo?.small?.toRef())
            }.sortedByDescending { it.date },
            loading, complete || failed
        )
    }

    private fun key(message: TdApi.Message) = key(message.chatId, message.id)
    private fun key(chatId: Long, messageId: Long) = chatId to messageId

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_EMPTY_PAGES = 5
        const val ANSWER_TIMEOUT_MS = 20_000L
    }
}

internal fun isCallContent(content: TdApi.MessageContent?): Boolean = content is TdApi.MessageCall || content is TdApi.MessageGroupCall

/** One SearchCallMessages answer: the call messages in it, the offset of the next chunk and whether the log ends here. */
internal data class CallPage(val messages: List<TdApi.Message>, val nextOffset: String, val complete: Boolean)

internal fun callPage(result: TdApi.FoundMessages): CallPage {
    val next = result.nextOffset.orEmpty()
    val messages = result.messages.orEmpty().filterNotNull().filter { isCallContent(it.content) }
    return CallPage(messages, next, next.isEmpty())
}

internal fun toCall(message: TdApi.Message, peerName: String, photo: TelegramFileRef?): TelegramCall? = when (val call = message.content) {
    is TdApi.MessageCall -> {
        val notAnswered = call.discardReason is TdApi.CallDiscardReasonMissed || call.discardReason is TdApi.CallDiscardReasonDeclined
        TelegramCall(
            messageId = message.id,
            chatId = message.chatId,
            peerName = peerName,
            photo = photo,
            kind = when {
                message.isOutgoing && notAnswered -> TelegramCallKind.Unanswered
                message.isOutgoing -> TelegramCallKind.Outgoing
                notAnswered -> TelegramCallKind.Missed
                else -> TelegramCallKind.Incoming
            },
            isVideo = call.isVideo,
            durationSeconds = call.duration,
            date = message.date
        )
    }
    // Newer apps make 1:1 calls as group calls not bound to a chat (conference calls): same row.
    is TdApi.MessageGroupCall -> TelegramCall(
        messageId = message.id,
        chatId = message.chatId,
        peerName = peerName,
        photo = photo,
        kind = when {
            message.isOutgoing && call.wasMissed -> TelegramCallKind.Unanswered
            message.isOutgoing -> TelegramCallKind.Outgoing
            call.wasMissed -> TelegramCallKind.Missed
            else -> TelegramCallKind.Incoming
        },
        isVideo = call.isVideo,
        durationSeconds = call.duration,
        date = message.date,
        isGroup = true
    )
    else -> null
}
