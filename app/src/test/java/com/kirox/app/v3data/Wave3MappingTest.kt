package com.kirox.app.v3data

import com.kirox.app.ui.v3.data.KxAccountMapping
import com.kirox.app.ui.v3.data.KxChat3Mapping
import com.kirox.app.ui.v3.data.KxMediaMapping
import com.kirox.app.ui.v3.model.KxEntity
import com.kirox.app.ui.v3.model.KxEntityType
import com.kirox.app.ui.v3.model.KxFolderInfo
import org.drinkless.tdlib.TdApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Pure wave3 mappings: TelegramKxMedia.kt, TelegramKxAccount.kt, TelegramKxChat3.kt. */
class Wave3MappingTest {

    // ---- media ----
    @Test fun chaptersFromCaption() {
        val c = KxMediaMapping.chapters("Обзор\n0:00 Вступление\n1:30 — Часть два\n1:02:03 Итоги")
        assertEquals(listOf(0, 90, 3723), c.map { it.first })
        assertEquals("Часть два", c[1].second)
    }

    @Test fun chaptersNeedStartAtZeroAndTwoLines() {
        assertTrue(KxMediaMapping.chapters("1:00 Начало\n2:00 Конец").isEmpty()) // YouTube rule: first chapter at 0:00
        assertTrue(KxMediaMapping.chapters("0:00 Одна").isEmpty())
        assertTrue(KxMediaMapping.chapters("0:00 A\n0:75 B").isEmpty()) // 75 s is not a timecode
    }

    @Test fun trafficCategories() {
        assertEquals("photo", KxMediaMapping.category(TdApi.FileTypePhoto()))
        assertEquals("video", KxMediaMapping.category(TdApi.FileTypeVideoNote()))
        assertEquals("voice", KxMediaMapping.category(TdApi.FileTypeVoiceNote()))
        assertEquals("music", KxMediaMapping.category(TdApi.FileTypeAudio()))
        assertEquals("stickers", KxMediaMapping.category(TdApi.FileTypeSticker()))
        assertEquals("messages", KxMediaMapping.category(null))
        assertEquals("messages", KxMediaMapping.category(TdApi.FileTypeNone()))
    }

    @Test fun trafficNetworks() {
        assertEquals(0, KxMediaMapping.network(TdApi.NetworkTypeMobile()))
        assertEquals(1, KxMediaMapping.network(TdApi.NetworkTypeWiFi()))
        assertEquals(1, KxMediaMapping.network(TdApi.NetworkTypeOther()))
        assertEquals(2, KxMediaMapping.network(TdApi.NetworkTypeMobileRoaming()))
        assertEquals(-1, KxMediaMapping.network(TdApi.NetworkTypeNone()))
    }

    @Test fun callProblems() {
        val p = KxMediaMapping.problems(listOf("echo", "bogus", "video", "dropped"))
        assertEquals(3, p.size)
        assertTrue(p[0] is TdApi.CallProblemEcho)
        assertTrue(p[1] is TdApi.CallProblemDistortedVideo)
        assertTrue(p[2] is TdApi.CallProblemDropped)
    }

    // ---- account ----
    @Test fun flags() {
        assertEquals("🇰🇿", KxAccountMapping.flag("KZ"))
        assertEquals("", KxAccountMapping.flag("kz"))
        assertEquals("", KxAccountMapping.flag("FT1"))
    }

    @Test fun codeSentWording() {
        assertEquals("Код отправлен по SMS", KxAccountMapping.codeSent(TdApi.AuthenticationCodeTypeSms(5)))
        assertEquals("Код отправлен", KxAccountMapping.codeSent(null))
    }

    @Test fun accentPremium() {
        val free = TdApi.AccentColor().apply { id = 6; darkThemeColors = intArrayOf(1); lightThemeColors = intArrayOf(2) }
        val paid = TdApi.AccentColor().apply { id = 7; darkThemeColors = IntArray(0); lightThemeColors = intArrayOf(3) }
        assertFalse(KxAccountMapping.accent(free).premium)
        assertTrue(KxAccountMapping.accent(paid).premium)
    }

    @Test fun filterWordsMergeWithoutRepeats() {
        val presets = listOf(listOf("скидка", "Промокод"), listOf("airdrop"), listOf("спойлер"))
        assertEquals(listOf("промокод", "скидка", "спойлер"), KxAccountMapping.filterWords(listOf("промокод"), 0b101, presets))
        assertEquals(listOf("x"), KxAccountMapping.filterWords(listOf("x"), 0, presets))
    }

    @Test fun dndSilenced() {
        val now = 1_000_000L
        // manual switch, until a time
        assertTrue(KxAccountMapping.silenced(true, now + 60, false, 0, 0, now, 12, 1, emptySet()))
        assertFalse(KxAccountMapping.silenced(true, now - 1, false, 0, 0, now, 12, 1, emptySet()))
        assertTrue(KxAccountMapping.silenced(true, 0, false, 0, 0, now, 12, 1, emptySet()))
        // exceptions always alert
        assertFalse(KxAccountMapping.silenced(true, 0, false, 0, 0, now, 12, 7, setOf(7L)))
        // night window across midnight 22..7
        assertTrue(KxAccountMapping.silenced(false, 0, true, 22, 7, now, 23, 1, emptySet()))
        assertTrue(KxAccountMapping.silenced(false, 0, true, 22, 7, now, 6, 1, emptySet()))
        assertFalse(KxAccountMapping.silenced(false, 0, true, 22, 7, now, 7, 1, emptySet()))
        assertFalse(KxAccountMapping.silenced(false, 0, true, 5, 5, now, 5, 1, emptySet())) // empty window
    }

    @Test fun dndUntil() {
        val now = 10_000L
        assertEquals(now + 3600, KxAccountMapping.dndUntil(0, now, 12, 0, 8))
        assertEquals(now + 10 * 3600 - 1800, KxAccountMapping.dndUntil(2, now, 22, 30, 8)) // 22:30 → 08:00
        assertEquals(now + 24 * 3600, KxAccountMapping.dndUntil(2, now, 8, 0, 8))
        assertEquals(0L, KxAccountMapping.dndUntil(3, now, 8, 0, 8))
    }

    @Test fun folderRoundTripKeepsPinnedOrder() {
        val info = KxFolderInfo("Работа", "Work", 3, chatIds = listOf(10, 11, 12), excludedIds = listOf(99), groups = true, excludeMuted = true)
        val created = KxAccountMapping.folder(info, null)
        assertEquals(info, KxAccountMapping.folderInfo(created))
        val base = KxAccountMapping.folder(info, null).apply { pinnedChatIds = longArrayOf(12, 10); includedChatIds = longArrayOf(11) }
        val edited = KxAccountMapping.folder(info.copy(chatIds = listOf(10, 11)), base) // chat 12 removed
        assertEquals(listOf(10L), edited.pinnedChatIds.toList())
        assertEquals(listOf(11L), edited.includedChatIds.toList())
        assertNull(KxAccountMapping.folder(info.copy(icon = ""), null).icon)
    }

    @Test fun sunTimesKnownPlaces() {
        // Moscow, 21 Dec, UTC+3: sunrise 08:58, sunset 15:57 (timeanddate)
        val (rise, set) = KxAccountMapping.sunTimes(55.75, 37.62, 355, 3.0)!!
        assertEquals(8.97, rise, 0.25); assertEquals(15.95, set, 0.25)
        // Equator at the equinox: ~06:00 / ~18:10
        val (r2, s2) = KxAccountMapping.sunTimes(0.0, 0.0, 80, 0.0)!!
        assertEquals(6.07, r2, 0.25); assertEquals(18.12, s2, 0.25)
        // Polar day: no sunset
        assertNull(KxAccountMapping.sunTimes(80.0, 0.0, 172, 0.0))
    }

    // ---- chat ----
    @Test fun entitiesForSending() {
        assertTrue(KxChat3Mapping.entity(KxEntity(0, 3, KxEntityType.Bold))!!.type is TdApi.TextEntityTypeBold)
        assertNull(KxChat3Mapping.entity(KxEntity(0, 3, KxEntityType.TextLink, "")))
        assertNull(KxChat3Mapping.entity(KxEntity(0, 3, KxEntityType.Mention)))
        assertNull(KxChat3Mapping.entity(KxEntity(0, 0, KxEntityType.Italic)))
        val f = KxChat3Mapping.formatted("hello", listOf(KxEntity(0, 2, KxEntityType.Code), KxEntity(3, 9, KxEntityType.Bold)))
        assertEquals(1, f.entities.size) // out-of-range entity dropped, TDLib would reject the whole message
    }

    @Test fun detectedEntities() {
        val text = "see t.me/kirox or a@b.kz, +7 701 000"
        val url = KxChat3Mapping.detected(text, TdApi.TextEntity(4, 10, TdApi.TextEntityTypeUrl()))!!
        assertEquals("https://t.me/kirox", url.url)
        assertEquals("mailto:a@b.kz", KxChat3Mapping.detected(text, TdApi.TextEntity(18, 6, TdApi.TextEntityTypeEmailAddress()))!!.url)
        assertEquals("tel:+7701000", KxChat3Mapping.detected(text, TdApi.TextEntity(26, 10, TdApi.TextEntityTypePhoneNumber()))!!.url)
        assertNull(KxChat3Mapping.detected(text, TdApi.TextEntity(30, 50, TdApi.TextEntityTypeUrl()))) // out of range
    }

    private fun day(y: Int, m: Int, d: Int) = Calendar.getInstance().apply { clear(); set(y, m - 1, d, 15, 0) }

    @Test fun birthdayCountdown() {
        val now = day(2026, 11, 23)
        assertEquals(0, KxChat3Mapping.daysUntil(23, 11, now))
        assertEquals(1, KxChat3Mapping.daysUntil(24, 11, now))
        assertEquals(364, KxChat3Mapping.daysUntil(22, 11, now)) // passed → next year
        // 29.02 in 2027 (not leap) counts as 01.03.2027
        assertEquals(KxChat3Mapping.daysUntil(1, 3, day(2027, 2, 20)), KxChat3Mapping.daysUntil(29, 2, day(2027, 2, 20)))
        assertEquals(9, KxChat3Mapping.daysUntil(29, 2, day(2028, 2, 20))) // leap year: the real day
    }

    @Test fun readDates() {
        assertEquals("", KxChat3Mapping.readNote(TdApi.MessageReadDateTooOld()).first)
        assertTrue(KxChat3Mapping.readNote(TdApi.MessageReadDateMyPrivacyRestricted()).second.isNotEmpty())
        assertTrue(KxChat3Mapping.readNote(TdApi.MessageReadDateRead(1_795_348_800)).first.startsWith("Прочитано"))
    }
}
