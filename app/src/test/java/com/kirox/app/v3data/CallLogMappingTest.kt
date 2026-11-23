package com.kirox.app.v3data

import com.kirox.app.telegram.TelegramCallKind
import com.kirox.app.telegram.callPage
import com.kirox.app.telegram.toCall
import com.kirox.app.ui.v3.data.kxCalls
import com.kirox.app.ui.v3.model.KxCallDir
import org.drinkless.tdlib.TdApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** lead walk-through: SearchCallMessages answer → TelegramCall → KxCall rows of the Calls tab. */
class CallLogMappingTest {
    private val now = 1_763_900_000L * 1000 // 23 Nov 2025, any fixed instant
    private val nowSec = (now / 1000).toInt()

    private fun msg(id: Long, chat: Long, date: Int, content: TdApi.MessageContent, out: Boolean = false) = TdApi.Message().apply {
        this.id = id; chatId = chat; this.date = date; isOutgoing = out; this.content = content
    }
    private fun call(reason: TdApi.CallDiscardReason = TdApi.CallDiscardReasonHungUp(), duration: Int = 30, video: Boolean = false) =
        TdApi.MessageCall().apply { discardReason = reason; this.duration = duration; isVideo = video }

    @Test fun pageKeepsCallMessagesAndOffset() {
        val found = TdApi.FoundMessages().apply {
            totalCount = -1; nextOffset = "abc"
            messages = arrayOf(
                msg(3, 10, nowSec - 60, call()),
                msg(2, 11, nowSec - 120, TdApi.MessageGroupCall().apply { wasMissed = true; isVideo = true }),
                msg(1, 12, nowSec - 180, TdApi.MessageText()),
            )
        }
        val page = callPage(found)
        assertEquals(listOf(3L, 2L), page.messages.map { it.id })
        assertEquals("abc", page.nextOffset)
        assertFalse(page.complete)
        assertTrue(callPage(TdApi.FoundMessages().apply { messages = emptyArray(); nextOffset = "" }).complete)
        // An empty chunk with an offset is not the end of the log.
        assertFalse(callPage(TdApi.FoundMessages().apply { messages = emptyArray(); nextOffset = "x" }).complete)
    }

    @Test fun groupCallMessageIsACall() {
        val missed = toCall(msg(2, 11, nowSec, TdApi.MessageGroupCall().apply { wasMissed = true; isVideo = true }), "B", null)
        assertNotNull(missed)
        assertEquals(TelegramCallKind.Missed, missed!!.kind)
        assertTrue(missed.isVideo && missed.isGroup)
        assertEquals(TelegramCallKind.Outgoing, toCall(msg(4, 11, nowSec, TdApi.MessageGroupCall().apply { duration = 70 }, out = true), "B", null)?.kind)
    }

    @Test fun rowsGroupedAndMissedCounted() {
        val list = listOf(
            msg(5, 10, nowSec - 60, call(TdApi.CallDiscardReasonMissed(), 0)),
            msg(4, 10, nowSec - 120, call(TdApi.CallDiscardReasonDeclined(), 0)),
            msg(3, 11, nowSec - 600, call(duration = 125), out = true),
            msg(2, 10, nowSec - 3 * 86_400, call(duration = 40)),
        ).mapNotNull { toCall(it, "P${it.chatId}", null) }
        val (rows, missed) = kxCalls(list.shuffled(), avatar = { "/a/${it.chatId}" }, colorIndex = { 2 }, now = now)
        assertEquals(3, rows.size)
        assertEquals(KxCallDir.Missed, rows[0].dir)
        assertEquals(2, rows[0].count)            // two missed calls from the same person, same day → «(2)»
        assertEquals(KxCallDir.Outgoing, rows[1].dir)
        assertEquals("P11", rows[1].name)
        assertEquals("2 мин", rows[1].duration)
        assertEquals(KxCallDir.Incoming, rows[2].dir)
        assertEquals("/a/10", rows[0].avatar)
        assertEquals(2, rows[0].colorIndex)
        assertEquals(2, missed)
        assertTrue(rows.all { it.day.isNotEmpty() })
    }
}
