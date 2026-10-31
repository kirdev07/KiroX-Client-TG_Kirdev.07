package com.kirox.app.settings

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorsTest {
    private fun luminance(c: Color): Double {
        fun ch(v: Float) = if (v <= 0.03928f) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test
    fun presetsAreReadable() {
        CustomTheme.PRESETS.forEach { theme ->
            val c = theme.colors()
            assertTrue("${theme.name}: text on background", contrast(c.textPrimary, c.background) >= 12.0)
            assertTrue("${theme.name}: text on panels", contrast(c.textPrimary, c.surface1) >= 9.0)
            assertTrue("${theme.name}: secondary text", contrast(c.textSecondary, c.surface1) >= 4.5)
            assertTrue("${theme.name}: my messages", contrast(c.onOutgoing, c.outgoingBubble) >= 3.0)
            assertTrue("${theme.name}: selected tab", contrast(c.onSelectedPill, c.selectedPill) >= 3.0)
        }
    }

    @Test
    fun presetIdsAreUnique() {
        assertEquals(CustomTheme.PRESETS.size, CustomTheme.PRESETS.map { it.id }.toSet().size)
        assertTrue(CustomTheme.PRESETS.all { it.isPreset })
    }

    @Test
    fun oneColorThemeKeepsHueAndContrast() {
        listOf(0xFF4C9DFF, 0xFFFF6680, 0xFF4ADE8E, 0xFFFFC34D, 0xFF9AA3AE, 0xFF202020).forEach { base ->
            val theme = CustomTheme.fromAccent("x", "x", base)
            val c = theme.colors()
            assertTrue(contrast(c.textPrimary, c.background) >= 12.0)
            assertTrue(contrast(c.onOutgoing, c.outgoingBubble) >= 3.0)
            assertTrue(contrast(c.onSelectedPill, c.selectedPill) >= 3.0)
        }
    }

    @Test
    fun hsvRoundTrip() {
        listOf(0xFF4C9DFF, 0xFFFF6680, 0xFF000000, 0xFFFFFFFF, 0xFF123456).forEach { color ->
            val (h, s, v) = CustomTheme.hsvOf(color)
            val back = CustomTheme.toArgb(h, s, v)
            listOf(16, 8, 0).forEach { shift ->
                val diff = kotlin.math.abs(((color shr shift) and 0xFF) - ((back shr shift) and 0xFF))
                assertTrue("channel off by $diff for ${color.toString(16)}", diff <= 1)
            }
        }
    }
}
