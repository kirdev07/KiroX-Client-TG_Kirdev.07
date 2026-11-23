package com.kirox.app.ui.v3.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * kiroX v3 ("black + soft green") tokens, taken 1:1 from the approved canvas mockups
 * (design-reference/kirox-v3). Every v3 screen uses these instead of raw numbers.
 */
/**
 * The live part of the tokens: the Custom board's colour theme (KxPrefs.THEME), the Access board's contrast /
 * bold / outlines / big buttons / colour-vision palette (KxPrefsAccount.ACCESS_*). Set by [KxApplyLook] from the
 * root; the default value is exactly the approved mockup look. Backed by snapshot state, so screens recompose.
 */
@androidx.compose.runtime.Immutable
data class KxLook(
    val accent: Color = Color(0xFF8FD3AC), val accentLight: Color = Color(0xFFA6E3C1), val accentMid: Color = Color(0xFF6FC497),
    val accentDeep: Color = Color(0xFF3F9A6C), val accentDark: Color = Color(0xFF2E6B4B), val bubbleTo: Color = Color(0xFF1F4D36),
    val danger: Color = Color(0xFFFF8A80), val dangerSolid: Color = Color(0xFFE5534B), val warning: Color = Color(0xFFE8B04A),
    val contrast: Boolean = false, val bold: Boolean = false, val outlines: Boolean = false, val bigButtons: Boolean = false,
)

object Kx {
    var look by androidx.compose.runtime.mutableStateOf(KxLook())
        internal set

    // Surfaces
    val bg get() = if (look.contrast) Color.Black else Color(0xFF0A0C0B)
    val surface = Color(0xFF151917)
    val surface2 = Color(0xFF1B211E)
    val surface3 = Color(0xFF232A26)
    val sheetTop = Color(0xFF232A26)
    val sheetBottom = Color(0xFF161B18)
    val dock = Color(0xEB1C221F) // rgba(28,34,31,.92)
    val badgeMuted = Color(0xFF2A322E)
    val dashed = Color(0xFF3A4540)

    // Text (high contrast: brighter secondary text)
    val text get() = if (look.contrast) Color.White else Color(0xFFEEF3EF)
    val text2 get() = if (look.contrast) Color(0xFFE4EAE6) else Color(0xFFC9D2CC)
    val text3 get() = if (look.contrast) Color(0xFFC9D2CC) else Color(0xFF9AA59F)
    val text4 get() = if (look.contrast) Color(0xFFB4BEB8) else Color(0xFF8A958F)
    val onAccent = Color(0xFF06150D)

    // Accent (Custom board colour theme)
    val accent get() = look.accent
    val accentLight get() = look.accentLight
    val accentMid get() = look.accentMid
    val accentDeep get() = look.accentDeep
    val accentDark get() = look.accentDark
    /** Outgoing bubble gradient end (start = [accentDark]). */
    val bubbleTo get() = look.bubbleTo
    val danger get() = look.danger
    val dangerSolid get() = look.dangerSolid
    val warning get() = look.warning
    val warningInk = Color(0xFF1E1405)
    val premium = Color(0xFFC9A0E0)
    val link = Color(0xFFA9CBF5)

    // White overlays used all over the mockups
    fun w(alpha: Float) = Color.White.copy(alpha = alpha)
    val line get() = w(if (look.contrast) .2f else .07f)
    val stroke get() = w(if (look.contrast) .4f else .14f)
    val fill = w(.06f)
    val fillHi = w(.08f)

    val accentGradient get() = Brush.linearGradient(listOf(accent, accentDeep))
    val buttonGradient get() = Brush.linearGradient(listOf(accentMid, accentDeep))
    val sheetGradient = Brush.verticalGradient(listOf(sheetTop, sheetBottom))
    val storyRing get() = Brush.sweepGradient(listOf(accent, accentDeep, accentLight, accent))
}

/** Theme + accessibility → [KxLook]. [theme]: (accent, bubble from, bubble to) of the Custom board's swatch; index 0 = the mockup look. */
object KxLookMapping {
    fun look(themeIndex: Int, theme: Triple<Color, Color, Color>?, accessFlags: Int, palette: Int): KxLook {
        var l = if (themeIndex == 0 || theme == null) KxLook() else {
            val (a, b1, b2) = theme
            KxLook(accent = a, accentLight = androidx.compose.ui.graphics.lerp(a, Color.White, .25f),
                accentMid = androidx.compose.ui.graphics.lerp(a, b1, .35f), accentDeep = androidx.compose.ui.graphics.lerp(a, b1, .6f),
                accentDark = b1, bubbleTo = b2)
        }
        // Colour-vision palettes: keep "good" and "bad" apart by lightness and hue the eye still tells apart.
        l = when (palette) {
            1 -> l.copy(accent = Color(0xFF7FC4F0), accentLight = Color(0xFFA9D8F5), accentMid = Color(0xFF5AA8DC), accentDeep = Color(0xFF2F7BB5),
                danger = Color(0xFFF2A65A), dangerSolid = Color(0xFFD9822B))          // deuteranopia: blue / orange
            2 -> l.copy(danger = Color(0xFFFF7FB0), dangerSolid = Color(0xFFD94F8A), warning = Color(0xFFFF9C6B)) // tritanopia: pink instead of red/yellow
            3 -> l.copy(accent = Color(0xFFDADADA), accentLight = Color(0xFFEFEFEF), accentMid = Color(0xFFBDBDBD), accentDeep = Color(0xFF8A8A8A),
                accentDark = Color(0xFF4A4A4A), bubbleTo = Color(0xFF333333), danger = Color(0xFFFFFFFF), dangerSolid = Color(0xFF6E6E6E), warning = Color(0xFFCFCFCF))
            else -> l
        }
        return l.copy(bold = accessFlags and 1 != 0, contrast = accessFlags and 2 != 0, outlines = accessFlags and 4 != 0, bigButtons = accessFlags and 8 != 0)
    }
}

/** Avatar gradients used by the mockups (top-left → bottom-right) with their letter colors. */
object KxAvatarPalette {
    data class Swatch(val from: Color, val to: Color, val ink: Color)
    val all = listOf(
        Swatch(Color(0xFF4B3F7A), Color(0xFF2A2240), Color(0xFFDCD0FF)),
        Swatch(Color(0xFF2D4E73), Color(0xFF1A2A40), Color(0xFFC6DBF5)),
        Swatch(Color(0xFF2E6B66), Color(0xFF18403C), Color(0xFFBDEDE8)),
        Swatch(Color(0xFF6B4E2E), Color(0xFF3A2A1A), Color(0xFFF2DDBF)),
        Swatch(Color(0xFFA24B5B), Color(0xFF5A2632), Color(0xFFFFD6DE)),
        Swatch(Color(0xFFD08A3C), Color(0xFF7A4A16), Color(0xFFFFE9C9)),
        Swatch(Color(0xFF3C7BD0), Color(0xFF1A3A6A), Color(0xFFD6E6FF)),
        Swatch(Color(0xFF2F6E4D), Color(0xFF173A28), Color(0xFFC8F0D8)),
    )
    /** Same id → same color, like Telegram's peer colors. */
    fun of(id: Long): Swatch = all[(((id % all.size) + all.size) % all.size).toInt()]
}

object KxType {
    /** Roboto Flex in the mockups; the system Roboto is the same family on Android. */
    val family: FontFamily = FontFamily.Default
    /** «Жирный шрифт» (Access board): every weight one step heavier. */
    fun style(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Kx.text, lineHeight: TextUnit = TextUnit.Unspecified) =
        TextStyle(fontFamily = family, fontSize = size, fontWeight = if (Kx.look.bold) FontWeight((weight.weight + 200).coerceAtMost(900)) else weight, color = color, lineHeight = lineHeight)

    val brand get() = style(26.sp, FontWeight.Light).copy(letterSpacing = 0.2.sp)
    val h1 get() = style(22.sp, FontWeight.ExtraBold)
    val h2 get() = style(20.sp, FontWeight.ExtraBold)
    val title get() = style(18.sp, FontWeight.ExtraBold)
    val name get() = style(16.sp, FontWeight.SemiBold)
    val nameBold get() = style(16.sp, FontWeight.Bold)
    val body get() = style(15.sp, lineHeight = 21.sp)
    val bodySmall get() = style(14.sp, color = Kx.text4, lineHeight = 20.sp)
    val chip get() = style(13.sp, FontWeight.Medium, Kx.text2)
    val caption get() = style(12.sp, color = Kx.text4)
    val tiny get() = style(11.sp, color = Kx.text4)
}

object KxShape {
    val sheet = 30.dp
    val dialog = 28.dp
    val card = 20.dp
    val pill = 999.dp
}

object KxMotion {
    /** cubic-bezier(.2,.8,.2,1): the "rise" used for screen content. */
    val rise = CubicBezierEasing(.2f, .8f, .2f, 1f)
    /** cubic-bezier(.34,1.3,.5,1): springy lens of the bottom dock. */
    val lens = CubicBezierEasing(.34f, 1.3f, .5f, 1f)
}

/** Lets tests and the demo app turn decorative animation off (prefers-reduced-motion). */
val LocalKxReducedMotion = staticCompositionLocalOf { false }

@Composable
fun KxTheme(reducedMotion: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKxReducedMotion provides reducedMotion, content = content)
}
