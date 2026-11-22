package com.kirox.app.v3data

import com.kirox.app.telegram.serviceText
import org.drinkless.tdlib.TdApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** telegram/serviceText (shared by the v3 chat list and history) against official Telegram (ru). */
class ServiceTextTest {
    private val names = mapOf(7L to "Анна", 8L to "Олег", 100L to "Вы")
    private fun msg(content: TdApi.MessageContent, from: Long = 7, out: Boolean = false, channelPost: Boolean = false) = TdApi.Message().apply {
        senderId = TdApi.MessageSenderUser(from); this.content = content; isOutgoing = out; isChannelPost = channelPost
    }
    private fun line(m: TdApi.Message, channel: Boolean = false) = serviceText(m, names[(m.senderId as TdApi.MessageSenderUser).userId]!!, channel) { names[it] ?: "Пользователь" }

    @Test fun groupLines() {
        assertEquals("Анна вступил(а) в группу", line(msg(TdApi.MessageChatAddMembers(longArrayOf(7)))))
        assertEquals("Анна добавил(а) Олег", line(msg(TdApi.MessageChatAddMembers(longArrayOf(8)))))
        assertEquals("Анна покинул(а) группу", line(msg(TdApi.MessageChatDeleteMember(7))))
        assertEquals("Анна удалил(а) Олег", line(msg(TdApi.MessageChatDeleteMember(8))))
        assertEquals("Анна закрепил(а) сообщение", line(msg(TdApi.MessagePinMessage(1))))
        assertEquals("Анна изменил(а) название на «Книги»", line(msg(TdApi.MessageChatChangeTitle("Книги"))))
    }

    @Test fun channelLines() {
        assertEquals("Канал переименован в «Новости»", line(msg(TdApi.MessageChatChangeTitle("Новости"), channelPost = true), channel = true))
        assertEquals("Сообщение закреплено", line(msg(TdApi.MessagePinMessage(1)), channel = true))
        assertEquals("Анна присоединился(-ась) к каналу", line(msg(TdApi.MessageChatJoinByLink()), channel = true))
    }

    /** Telegram: «Вы закрепили сообщение», «Вы покинули группу» — never «Вы закрепил(а)». */
    @Test fun ownLinesUsePluralVerbs() {
        assertEquals("Вы закрепили сообщение", line(msg(TdApi.MessagePinMessage(1), from = 100, out = true)))
        assertEquals("Вы покинули группу", line(msg(TdApi.MessageChatDeleteMember(100), from = 100, out = true)))
        assertEquals("Вы создали группу «Книги»", line(msg(TdApi.MessageBasicGroupChatCreate("Книги", longArrayOf()), from = 100, out = true)))
    }

    @Test fun callLines() {
        fun call(out: Boolean, reason: TdApi.CallDiscardReason, seconds: Int, video: Boolean = false) =
            line(msg(TdApi.MessageCall().apply { discardReason = reason; duration = seconds; isVideo = video }, from = if (out) 100 else 7, out = out))
        assertEquals("Исходящий звонок · 3 мин", call(true, TdApi.CallDiscardReasonHungUp(), 180))
        assertEquals("Входящий видеозвонок · 42 сек", call(false, TdApi.CallDiscardReasonHungUp(), 42, video = true))
        assertEquals("Пропущенный звонок", call(false, TdApi.CallDiscardReasonMissed(), 0))
        assertEquals("Отменённый звонок", call(true, TdApi.CallDiscardReasonMissed(), 0))
    }

    /** Incoming call you declined: Telegram says «Отклонённый звонок», not «Пропущенный». */
    @Test fun declinedIncomingCall() {
        assertEquals("Отклонённый звонок", line(msg(TdApi.MessageCall().apply { discardReason = TdApi.CallDiscardReasonDeclined(); duration = 0 })))
    }

    @Test fun ordinaryMessageIsNotService() {
        assertNull(line(msg(TdApi.MessageText().apply { text = TdApi.FormattedText("привет", emptyArray()) })))
    }
}
