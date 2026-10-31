package com.kirox.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * kiroX design tokens: the sizes every screen is built from. Colors live in [FusionColors]; these
 * are spacing, corner radii, text sizes and the common heights. A new design changes the values
 * here instead of numbers scattered over screens.
 */
object FusionSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

object FusionRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    /** Small actions: text buttons, menu targets. */
    val s = 10.dp
    /** Pills, chips, file tiles. */
    val m = 12.dp
    /** Fields, toasts, list tiles. */
    val l = 14.dp
    /** Cards and settings groups. */
    val card = 20.dp
    /** Big pictures (covers). */
    val cover = 28.dp
}

object FusionType {
    val caption = 12.sp
    val small = 13.sp
    val body = 14.sp
    val bodyLarge = 15.sp
    val title = 16.sp
    val titleLarge = 17.sp
    val headline = 20.sp
    val display = 22.sp
}

object FusionSize {
    val touch = 48.dp
    val icon = 24.dp
    val iconSmall = 20.dp
    val button = 52.dp
    val listRow = 72.dp
    val hairline = 1.dp
    /** Round icon buttons in bars. */
    val iconButton = 44.dp
    /** Icons inside them. */
    val barIcon = 22.dp
    /** Screen top bars. */
    val bar = 56.dp
    /** List tiles with an icon or a letter. */
    val tile = 44.dp
}
