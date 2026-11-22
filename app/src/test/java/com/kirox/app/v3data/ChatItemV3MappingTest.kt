package com.kirox.app.v3data

import com.kirox.app.telegram.ChatActivity
import com.kirox.app.telegram.TelegramDelivery
import com.kirox.app.telegram.TelegramMessageKind
import com.kirox.app.telegram.chatActivityLineV3
import com.kirox.app.telegram.toChatItem
import org.drinkless.tdlib.TdApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The v3 list facts the binder added to telegram/toChatItem, checked against Telegram's behaviour. */
class ChatItemV3MappingTest {
    private val me = 100L
    private val anna = TdApi.User().apply { id = 7; firstName = "Анна"; lastName = "Смирнова"; type = TdApi.UserTypeRegular() }
    private val bot = TdApi.User().apply { id = 8; firstName = "Доставка"; lastName = ""; type = TdApi.UserTypeBot() }
    private val users = mapOf(anna.id to anna, bot.id to bot)
    private val main = TdApi.ChatPosition(TdApi.ChatListMain(), 1, false, null)

    private fun text(t: String) = TdApi.MessageText().apply { text = TdApi.FormattedText(t, emptyArray()) }

    private fun message(id: Long, out: Boolean, from: Long, content: TdApi.MessageContent = text("привет"), state: TdApi.MessageSendingState? = null) = TdApi.Message().apply {
        this.id = id; isOutgoing = out; date = 100; this.content = content; sendingState = state
        senderId = TdApi.MessageSenderUser(from)
    }

    private fun chat(type: TdApi.ChatType, last: TdApi.Message?, readOutbox: Long = 0, settings: TdApi.ChatNotificationSettings? = null) = TdApi.Chat().apply {
        id = 1; title = "Чат"; this.type = type; lastMessage = last; lastReadOutboxMessageId = readOutbox
        notificationSettings = settings ?: TdApi.ChatNotificationSettings().apply { useDefaultMuteFor = true }
    }

    private val privateWithAnna = TdApi.ChatTypePrivate(anna.id)
    private val group = TdApi.ChatTypeSupergroup().apply { supergroupId = 5; isChannel = false }
    private val channel = TdApi.ChatTypeSupergroup().apply { supergroupId = 6; isChannel = true }

    private fun item(c: TdApi.Chat, scope: IntArray? = null) = toChatItem(c, main, false, myId = me, peer = users::get, scopeMuteFor = scope)

    @Test fun senderPrefixLikeTelegram() {
        assertEquals("Анна", item(chat(group, message(1, false, anna.id))).lastSender) // first name only
        assertEquals("Вы", item(chat(group, message(1, true, me))).lastSender)
        assertNull(item(chat(privateWithAnna, message(1, false, anna.id))).lastSender)
        assertNull(item(chat(channel, message(1, false, anna.id))).lastSender)
        assertNull(item(chat(TdApi.ChatTypePrivate(me), message(1, true, me))).lastSender) // Saved Messages
    }

    /** «Вы: Сообщение» for «Вы закрепили сообщение» is wrong: service lines have no sender prefix. */
    @Test fun serviceMessageHasNoSenderPrefix() {
        val pin = message(1, true, me, TdApi.MessagePinMessage(5))
        assertNull(item(chat(group, pin)).lastSender)
        val join = message(2, false, anna.id, TdApi.MessageChatJoinByLink())
        assertNull(item(chat(group, join)).lastSender)
    }

    /** The list must not say «Сообщение» for a join/pin: Telegram shows the service line. */
    @Test fun serviceMessagePreviewIsReadable() {
        val join = message(2, false, anna.id, TdApi.MessageChatJoinByLink())
        val line = item(chat(group, join)).lastServiceText.orEmpty()
        assertTrue(line, line.contains("Анна") && line.contains("вступил"))
    }

    @Test fun deliveryIncludesSendingAndFailed() {
        assertEquals(TelegramDelivery.Sending, item(chat(privateWithAnna, message(10, true, me, state = TdApi.MessageSendingStatePending()))).lastDelivery)
        assertEquals(TelegramDelivery.Failed, item(chat(privateWithAnna, message(10, true, me, state = TdApi.MessageSendingStateFailed()))).lastDelivery)
        assertEquals(TelegramDelivery.Read, item(chat(privateWithAnna, message(10, true, me), readOutbox = 10)).lastDelivery)
        assertEquals(TelegramDelivery.Sent, item(chat(privateWithAnna, message(11, true, me), readOutbox = 10)).lastDelivery)
        assertEquals(TelegramDelivery.None, item(chat(privateWithAnna, message(11, false, anna.id))).lastDelivery)
        assertEquals(TelegramDelivery.None, item(chat(TdApi.ChatTypePrivate(me), message(11, true, me))).lastDelivery)
    }

    @Test fun muteFollowsScopeDefaults() {
        val scope = intArrayOf(0, 0, 3_600 * 24 * 400) // private, groups, channels (muted)
        assertTrue(item(chat(channel, null), scope).isMutedResolved)
        assertFalse(item(chat(group, null), scope).isMutedResolved)
        assertFalse(item(chat(privateWithAnna, null), scope).isMutedResolved)
        val ownOff = TdApi.ChatNotificationSettings().apply { useDefaultMuteFor = false; muteFor = 0 }
        assertFalse(item(chat(channel, null, settings = ownOff), scope).isMutedResolved)
        val ownOn = TdApi.ChatNotificationSettings().apply { useDefaultMuteFor = false; muteFor = 3600 }
        assertTrue(item(chat(privateWithAnna, null, settings = ownOn), scope).isMutedResolved)
    }

    @Test fun onlineBotAndKind() {
        val nowS = System.currentTimeMillis() / 1000
        anna.status = TdApi.UserStatusOnline((nowS + 300).toInt())
        assertTrue(item(chat(privateWithAnna, null)).isOnline)
        anna.status = TdApi.UserStatusOnline((nowS - 300).toInt()) // expired
        assertFalse(item(chat(privateWithAnna, null)).isOnline)
        assertTrue(item(chat(TdApi.ChatTypePrivate(bot.id), null)).isBot)
        assertTrue(item(chat(TdApi.ChatTypeSecret(3, anna.id), null)).isSecret)
    }

    @Test fun voiceDurationAndKind() {
        val voice = TdApi.MessageVoiceNote().apply { voiceNote = TdApi.VoiceNote().apply { duration = 24 }; caption = TdApi.FormattedText("", emptyArray()) }
        val i = item(chat(privateWithAnna, message(1, false, anna.id, voice)))
        assertEquals(TelegramMessageKind.Voice, i.lastKind)
        assertEquals(24, i.lastDuration)
    }

    @Test fun typingLinesLikeTelegram() {
        assertEquals("печатает…", chatActivityLineV3(listOf("Анна"), ChatActivity.Typing, false))
        assertEquals("Анна печатает…", chatActivityLineV3(listOf("Анна"), ChatActivity.Typing, true))
        assertEquals("Анна и Максим печатают…", chatActivityLineV3(listOf("Анна", "Максим"), ChatActivity.Typing, true))
        assertEquals("Анна и ещё 2 человека печатают…", chatActivityLineV3(listOf("Анна", "Б", "В"), ChatActivity.Typing, true))
        assertEquals("Анна и ещё 5 человек печатают…", chatActivityLineV3(listOf("Анна", "Б", "В", "Г", "Д", "Е"), ChatActivity.Typing, true))
    }

    /** Two people recording: the verb must be plural too («записывают голосовое»). */
    @Test fun pluralVerbForOtherActivities() {
        assertEquals("Анна и Максим записывают голосовое…", chatActivityLineV3(listOf("Анна", "Максим"), ChatActivity.RecordingVoice, true))
    }
}
