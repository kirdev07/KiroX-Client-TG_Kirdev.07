package com.kirox.app.v3data

import com.kirox.app.settings.BubbleShape
import com.kirox.app.settings.ChatDensity
import com.kirox.app.settings.ChatTextSize
import com.kirox.app.settings.ChatWallpaper
import com.kirox.app.settings.DoubleTapAction
import com.kirox.app.settings.PowerSaving
import com.kirox.app.telegram.CallDataSaving
import com.kirox.app.ui.v3.data.KxLocalMapping
import com.kirox.app.ui.v3.model.KxPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Board index <-> old-app setting conversions in ui/v3/data/TelegramKxLocal.kt (binder2, round 6). */
class LocalSettingsMappingTest {

    @Test fun bubblesMapByMeaningNotOrdinal() {
        // Board: 0 Острые, 1 Мягкие, 2 Круглые. Old: Soft 22dp, Medium 16dp, Strict 6dp.
        assertEquals(BubbleShape.Strict, KxLocalMapping.bubbles(0))
        assertEquals(BubbleShape.Medium, KxLocalMapping.bubbles(1))
        assertEquals(BubbleShape.Soft, KxLocalMapping.bubbles(2))
        BubbleShape.entries.forEach { assertEquals(it, KxLocalMapping.bubbles(KxLocalMapping.bubbles(it))) }
        assertEquals(BubbleShape.Medium, KxLocalMapping.bubbles(9)) // unknown -> old default
    }

    @Test fun textSizeAndDensityRoundTrip() {
        ChatTextSize.entries.forEach { assertEquals(it, KxLocalMapping.textSize(KxLocalMapping.textSize(it))) }
        ChatDensity.entries.forEach { assertEquals(it, KxLocalMapping.density(KxLocalMapping.density(it))) }
        assertEquals(ChatTextSize.Normal, KxLocalMapping.textSize(-1))
        assertEquals(ChatDensity.Normal, KxLocalMapping.density(7))
        assertEquals(4, ChatTextSize.entries.size) // board offers 4 sizes (13/15/17/19 sp)
        assertEquals(3, ChatDensity.entries.size)
    }

    @Test fun wallpaperMapsByName() {
        assertEquals(ChatWallpaper.Theme, KxLocalMapping.wallpaper(0))
        assertEquals(ChatWallpaper.Pattern, KxLocalMapping.wallpaper(1))
        assertEquals(ChatWallpaper.Gradient, KxLocalMapping.wallpaper(2))
        assertNull(KxLocalMapping.wallpaper(3)) // «Ночь» is v3-only: never written into chat_wallpaper
        assertEquals(ChatWallpaper.None, KxLocalMapping.wallpaper(4))
        listOf(ChatWallpaper.Theme, ChatWallpaper.Pattern, ChatWallpaper.Gradient, ChatWallpaper.None)
            .forEach { assertEquals(it, KxLocalMapping.wallpaper(KxLocalMapping.wallpaper(it))) }
        assertEquals(1, KxLocalMapping.wallpaper(ChatWallpaper.Dots)) // old «Точки» shown as the closest board pick
    }

    @Test fun powerModeAndThreshold() {
        PowerSaving.entries.forEach { assertEquals(it, KxLocalMapping.powerMode(KxLocalMapping.powerMode(it))) }
        assertEquals(0, KxLocalMapping.threshold(PowerSaving.Off, 40))
        assertEquals(100, KxLocalMapping.threshold(PowerSaving.On, 40))
        assertEquals(40, KxLocalMapping.threshold(PowerSaving.Auto, 40))
    }

    @Test fun doubleTapAndCallData() {
        DoubleTapAction.entries.forEach { assertEquals(it, KxLocalMapping.doubleTap(KxLocalMapping.doubleTap(it))) }
        CallDataSaving.entries.forEach { assertEquals(it, KxLocalMapping.callData(KxLocalMapping.callData(it))) }
        assertEquals(CallDataSaving.Never, KxLocalMapping.callData(0)) // «Высокое» = never save data
        assertEquals(CallDataSaving.Always, KxLocalMapping.callData(2))
    }

    @Test fun nightMode() {
        assertEquals(1, KxLocalMapping.nightMode(true, 0)) // auto theme on = «По расписанию»
        assertEquals(0, KxLocalMapping.nightMode(false, 0))
        assertEquals(3, KxLocalMapping.nightMode(false, 1)) // schedule was turned off elsewhere
    }

    @Test fun tabKeysMatchDockOrder() {
        assertEquals(listOf("Контакты", "Звонки", "Чаты", "Истории", "Настройки", "Музыка"), KxLocalMapping.tabKeys)
    }

    @Test fun defaultsCoverEveryKey() {
        listOf(KxPrefs.THEME, KxPrefs.BUBBLES, KxPrefs.CHAT_WALLPAPER, KxPrefs.TEXT_SIZE, KxPrefs.DENSITY, KxPrefs.LANGUAGE,
            KxPrefs.POWER_THRESHOLD, KxPrefs.POWER_OFF, KxPrefs.POWER_MODE, KxPrefs.DOUBLE_TAP, KxPrefs.CALL_DATA,
            KxPrefs.NIGHT_MODE, KxPrefs.NIGHT_FROM, KxPrefs.NIGHT_TO, KxPrefs.WALL)
            .forEach { assert(KxPrefs.defaults.containsKey(it)) { "no default for $it" } }
    }
}
