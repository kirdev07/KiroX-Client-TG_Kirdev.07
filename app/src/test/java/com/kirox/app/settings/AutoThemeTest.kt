package com.kirox.app.settings

import com.kirox.app.telegram.TelegramLink
import com.kirox.app.telegram.telegramLinkOf
import com.kirox.app.ui.theme.FusionPalette
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoThemeTest {
    private val auto = AutoTheme(true, FusionPalette.Expressive, FusionPalette.AmoledBlack, dayStart = 7, nightStart = 22)

    @Test fun dayAndNightByHour() {
        assertEquals(FusionPalette.AmoledBlack, auto.paletteAt(6))
        assertEquals(FusionPalette.Expressive, auto.paletteAt(7))
        assertEquals(FusionPalette.Expressive, auto.paletteAt(21))
        assertEquals(FusionPalette.AmoledBlack, auto.paletteAt(22))
        assertEquals(FusionPalette.AmoledBlack, auto.paletteAt(0))
    }

    @Test fun nightStartingAtMidnight() {
        val late = auto.copy(nightStart = 0)
        assertEquals(FusionPalette.Expressive, late.paletteAt(23))
        assertEquals(FusionPalette.AmoledBlack, late.paletteAt(0))
        assertEquals(FusionPalette.AmoledBlack, late.paletteAt(6))
    }

    @Test fun folderLinksAreRecognised() {
        assertEquals(TelegramLink.AddList("https://t.me/addlist/AbCdEf"), telegramLinkOf("https://t.me/addlist/AbCdEf"))
        assertEquals(TelegramLink.Username("kiroxclient"), telegramLinkOf("https://t.me/kiroxclient"))
    }
}
