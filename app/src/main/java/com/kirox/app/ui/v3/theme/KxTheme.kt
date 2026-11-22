package com.kirox.app.ui.v3.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
object Kx {
    // Surfaces
    val bg = Color(0xFF0A0C0B)
    val surface = Color(0xFF151917)
    val surface2 = Color(0xFF1B211E)
    val surface3 = Color(0xFF232A26)
    val sheetTop = Color(0xFF232A26)
    val sheetBottom = Color(0xFF161B18)
    val dock = Color(0xEB1C221F) // rgba(28,34,31,.92)
    val badgeMuted = Color(0xFF2A322E)
    val dashed = Color(0xFF3A4540)

    // Text
    val text = Color(0xFFEEF3EF)
    val text2 = Color(0xFFC9D2CC)
    val text3 = Color(0xFF9AA59F)
    val text4 = Color(0xFF8A958F)
    val onAccent = Color(0xFF06150D)

    // Accent
    val accent = Color(0xFF8FD3AC)
    val accentLight = Color(0xFFA6E3C1)
    val accentMid = Color(0xFF6FC497)
    val accentDeep = Color(0xFF3F9A6C)
    val accentDark = Color(0xFF2E6B4B)
    val danger = Color(0xFFFF8A80)
    val dangerSolid = Color(0xFFE5534B)
    val warning = Color(0xFFE8B04A)
    val warningInk = Color(0xFF1E1405)
    val premium = Color(0xFFC9A0E0)
    val link = Color(0xFFA9CBF5)

    // White overlays used all over the mockups
    fun w(alpha: Float) = Color.White.copy(alpha = alpha)
    val line = w(.07f)
    val stroke = w(.14f)
    val fill = w(.06f)
    val fillHi = w(.08f)

    val accentGradient = Brush.linearGradient(listOf(Color(0xFF8FD3AC), Color(0xFF3F9A6C)))
    val buttonGradient = Brush.linearGradient(listOf(Color(0xFF6FC497), Color(0xFF3F9A6C)))
    val sheetGradient = Brush.verticalGradient(listOf(sheetTop, sheetBottom))
    val storyRing = Brush.sweepGradient(listOf(Color(0xFF8FD3AC), Color(0xFF3F9A6C), Color(0xFFA6E3C1), Color(0xFF8FD3AC)))
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
    fun style(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Kx.text, lineHeight: TextUnit = TextUnit.Unspecified) =
        TextStyle(fontFamily = family, fontSize = size, fontWeight = weight, color = color, lineHeight = lineHeight)

    val brand = style(26.sp, FontWeight.Light).copy(letterSpacing = 0.2.sp)
    val h1 = style(22.sp, FontWeight.ExtraBold)
    val h2 = style(20.sp, FontWeight.ExtraBold)
    val title = style(18.sp, FontWeight.ExtraBold)
    val name = style(16.sp, FontWeight.SemiBold)
    val nameBold = style(16.sp, FontWeight.Bold)
    val body = style(15.sp, lineHeight = 21.sp)
    val bodySmall = style(14.sp, color = Kx.text4, lineHeight = 20.sp)
    val chip = style(13.sp, FontWeight.Medium, Kx.text2)
    val caption = style(12.sp, color = Kx.text4)
    val tiny = style(11.sp, color = Kx.text4)
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
