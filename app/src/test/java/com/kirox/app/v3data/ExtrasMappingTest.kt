package com.kirox.app.v3data

import com.kirox.app.settings.Tweaks
import com.kirox.app.telegram.NotifyScope
import com.kirox.app.telegram.PrivacyLevel
import com.kirox.app.telegram.SharedMediaKind
import com.kirox.app.telegram.TelegramSession
import com.kirox.app.ui.v3.data.KxExtrasMapping
import com.kirox.app.ui.v3.model.KxSharedTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure round-5 mappings in ui/v3/data/TelegramKxStoreExtras.kt. */
class ExtrasMappingTest {
    private val now = 1_795_348_800_000L // Sun 2026-11-22 12:00 UTC

    @Test fun sessions() {
        val current = KxExtrasMapping.session(TelegramSession(1, true, true, "kiroX 3.0", "Xiaomi 13", "Android", "Алматы, Казахстан", 0, false), now)
        assertEquals("в сети", current.time)
        assertTrue(current.current)
        val other = KxExtrasMapping.session(TelegramSession(2, false, true, "Telegram Desktop", "", "Windows", "Москва", (now / 1000).toInt() - 86_400, true), now)
        assertEquals("Windows", other.device) // empty device name falls back to the platform
        assertTrue(other.passwordPending)
    }

    @Test fun privacyLevelsRoundTrip() {
        PrivacyLevel.entries.forEach { assertEquals(it, KxExtrasMapping.level(KxExtrasMapping.level(it))) }
        assertEquals(PrivacyLevel.Nobody, KxExtrasMapping.level(7)) // unknown index must never widen visibility
    }

    @Test fun scopesRoundTrip() {
        NotifyScope.entries.forEach { assertEquals(it, KxExtrasMapping.scope(KxExtrasMapping.scopeKey(it))) }
        assertNull(KxExtrasMapping.scope("reactions"))
    }

    @Test fun sharedTabs() {
        assertEquals(SharedMediaKind.Media, KxExtrasMapping.shared(KxSharedTab.Media))
        assertEquals(SharedMediaKind.Voice, KxExtrasMapping.shared(KxSharedTab.Voice))
    }

    /** Every v3 switch writes the same KiroxTweaks field it reads, so ghost mode in the repositories follows it. */
    @Test fun tweakKeysWriteWhatTheyRead() {
        val base = Tweaks()
        KxExtrasMapping.tweakKeys.forEach { (key, accessor) ->
            val (get, set) = accessor
            assertEquals(key, true, get(set(base, true)))
            assertEquals(key, false, get(set(base, false)))
        }
        val ghost = KxExtrasMapping.tweakKeys.getValue("ghost").second(base, true)
        assertTrue(ghost.ghostRead) // ghost + ghostNoRead (default true) → repositories stop sending ViewMessages
    }

    /** Only one key may change per setter (a copy() typo would silently flip another switch). */
    @Test fun tweakSettersAreIndependent() {
        val base = Tweaks()
        val before = KxExtrasMapping.tweaks(base)
        KxExtrasMapping.tweakKeys.forEach { (key, accessor) ->
            val after = KxExtrasMapping.tweaks(accessor.second(base, !before.getValue(key)))
            val changed = after.filter { (k, v) -> before[k] != v }.keys
            assertEquals(key, setOf(key), changed)
        }
    }
}
