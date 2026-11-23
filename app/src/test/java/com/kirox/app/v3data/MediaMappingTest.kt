package com.kirox.app.v3data

import com.kirox.app.telegram.CallEndReason
import com.kirox.app.telegram.CallPhase
import com.kirox.app.telegram.TelegramActiveCall
import com.kirox.app.ui.v3.data.KxMediaMapping
import com.kirox.app.ui.v3.model.KxCallPhase
import org.drinkless.tdlib.TdApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure parts of ui/v3/data/TelegramKxMedia (wave3-media). */
class MediaMappingTest {
    @Test fun chaptersFromCaption() {
        val c = KxMediaMapping.chapters("Разбираем корутины.\n0:00 Вступление\n1:35 — Зачем нужны\n12:40 Итоги\nне глава 99:99")
        assertEquals(listOf(0 to "Вступление", 95 to "Зачем нужны", 760 to "Итоги"), c)
        assertEquals(3723, KxMediaMapping.chapters("0:00 a\n1:02:03 b")[1].first)
    }

    @Test fun chaptersNeedStartAndTwo() {
        assertTrue(KxMediaMapping.chapters("1:00 Середина\n2:00 Конец").isEmpty())
        assertTrue(KxMediaMapping.chapters("0:00 Одна").isEmpty())
        assertTrue(KxMediaMapping.chapters("").isEmpty())
    }

    @Test fun trafficCategories() {
        assertEquals("photo", KxMediaMapping.category(TdApi.FileTypePhoto()))
        assertEquals("video", KxMediaMapping.category(TdApi.FileTypeVideoNote()))
        assertEquals("voice", KxMediaMapping.category(TdApi.FileTypeVoiceNote()))
        assertEquals("music", KxMediaMapping.category(TdApi.FileTypeAudio()))
        assertEquals("files", KxMediaMapping.category(TdApi.FileTypeDocument()))
        assertEquals("messages", KxMediaMapping.category(TdApi.FileTypeNone()))
        assertEquals(0, KxMediaMapping.network(TdApi.NetworkTypeMobile()))
        assertEquals(1, KxMediaMapping.network(TdApi.NetworkTypeWiFi()))
        assertEquals(2, KxMediaMapping.network(TdApi.NetworkTypeMobileRoaming()))
        assertEquals(-1, KxMediaMapping.network(TdApi.NetworkTypeNone()))
    }

    @Test fun callProblems() {
        val p = KxMediaMapping.problems(listOf("echo", "dropped", "unknown"))
        assertEquals(2, p.size)
        assertTrue(p[0] is TdApi.CallProblemEcho && p[1] is TdApi.CallProblemDropped)
    }

    @Test fun callStates() {
        val base = TelegramActiveCall(7, 42, "Анна", null, isOutgoing = true, isVideo = false, phase = CallPhase.Calling(ringing = true))
        assertEquals(KxCallPhase.Ringing, KxMediaMapping.call(base, null).phase)
        val active = KxMediaMapping.call(base.copy(phase = CallPhase.Active(1000L, reconnecting = true), emojis = listOf("🐼")), "/a.jpg")
        assertEquals(KxCallPhase.Active, active.phase); assertEquals(1000L, active.sinceMs); assertTrue(active.reconnecting); assertEquals("/a.jpg", active.avatar)
        val ended = KxMediaMapping.call(base.copy(phase = CallPhase.Ended(CallEndReason.Declined, 0)), null)
        assertEquals(KxCallPhase.Ended, ended.phase); assertEquals("Звонок отклонён", ended.endReason)
        assertEquals("Ошибка", KxMediaMapping.call(base.copy(phase = CallPhase.Failed("Ошибка")), null).failure)
    }
}
