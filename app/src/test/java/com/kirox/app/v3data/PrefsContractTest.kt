package com.kirox.app.v3data

import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.model.KxPrefs
import com.kirox.app.ui.v3.model.pref
import com.kirox.app.ui.v3.model.prefOn
import com.kirox.app.ui.v3.model.togglePref
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** KxPrefs helpers and the demo store's round-6 members (board defaults, read-only keys, per-chat wallpaper keys). */
class PrefsContractTest {
    @Test fun demoStartsWithBoardDefaults() {
        val s = KxDemoStore()
        assertEquals(1, s.pref(KxPrefs.BUBBLES))
        assertEquals(15, s.pref(KxPrefs.POWER_THRESHOLD))
        // Power board: animations, reactions, autoplay and stickers are off by default (bits 0,1,2,4).
        assertEquals(listOf(true, true, true, false, true, false), List(6) { s.pref(KxPrefs.POWER_OFF) and (1 shl it) != 0 })
    }

    @Test fun readOnlyKeysIgnoreWrites() {
        val s = KxDemoStore()
        s.putPref(KxPrefs.OWN_THEMES, 9)
        assertEquals(1, s.pref(KxPrefs.OWN_THEMES))
        s.togglePref(KxPrefs.TRANSLATE)
        assertFalse(s.prefOn(KxPrefs.TRANSLATE))
    }

    @Test fun perChatWallpaperFallsBackToTheBaseDefault() {
        val s = KxDemoStore()
        assertEquals(1, s.pref("${KxPrefs.WALL_DIM}:42"))
        s.putPref("${KxPrefs.WALL}:42", 3)
        assertEquals(3, s.pref("${KxPrefs.WALL}:42"))
        assertEquals(0, s.pref(KxPrefs.WALL))
    }

    @Test fun tabNamesAndPasscode() = runBlocking {
        val s = KxDemoStore()
        s.renameTab(2, "Болталка")
        assertEquals("Болталка", s.tabNames[2])
        s.renameTab(2, " ")
        assertTrue(s.tabNames.isEmpty())
        assertTrue(s.passcodeSet)
        assertTrue(s.checkPasscode("1234"))
        assertFalse(s.checkPasscode("0000"))
    }
}
