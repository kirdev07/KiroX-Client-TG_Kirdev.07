package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

/** Shared pieces of the settings sub-screens (the boards repeat them 1:1). */

/** cubic-bezier(.34,1.5,.5,1): the springy knob / pick easing of the settings boards. */
internal val SpringEase = CubicBezierEasing(.34f, 1.5f, .5f, 1f)
internal val RiseEase = KxMotion.rise

/** CSS radial-gradient(circle, c, transparent 65%) on a [d] circle at left/top ([x],[y]), or right when [alignEnd]. */
@Composable
internal fun BoxScope.SGlow(color: Color, d: Dp = 380.dp, x: Dp = (-90).dp, y: Dp = (-110).dp, alignEnd: Boolean = false) {
    Canvas(Modifier.align(if (alignEnd) Alignment.TopEnd else Alignment.TopStart).offset(if (alignEnd) -x else x, y).size(d)) {
        val reach = size.minDimension / 2 * 1.41421f * .65f
        drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius = reach), radius = size.minDimension / 2)
    }
}

/** Glow colour rgba(70,160,110,a). */
internal fun glowGreen(a: Float) = Color(70, 160, 110).copy(alpha = a)

/** Header of the sub-screens: 44 glass back button, 22sp/800 title, padding 10 12 [bottom], gap 8. */
@Composable
internal fun SHeader(title: String, onBack: () -> Unit, bottom: Dp = 4.dp, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = bottom),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = onBack)
        Text(title, style = KxType.h1, maxLines = 1, modifier = Modifier.weight(1f))
        actions()
    }
}

/** Grouped tiles: 22 outer corners, 6 inner. */
internal fun groupShape(i: Int, n: Int, big: Dp = 22.dp, small: Dp = 6.dp): Shape = when {
    n == 1 -> RoundedCornerShape(big)
    i == 0 -> RoundedCornerShape(big, big, small, small)
    i == n - 1 -> RoundedCornerShape(small, small, big, big)
    else -> RoundedCornerShape(small)
}

internal val TileBg = Color.White.copy(alpha = .045f)

/** Section caption: 14sp/700 #C9D2CC, padding 0 2 8. */
@Composable
internal fun SCaption(text: String, modifier: Modifier = Modifier, bottom: Dp = 8.dp) =
    Text(text, style = KxType.style(14.sp, FontWeight.Bold, Kx.text2), modifier = modifier.padding(start = 2.dp, end = 2.dp, bottom = bottom))

/** Small hint under a group: 12sp #8A958F. */
@Composable
internal fun SHint(text: String, modifier: Modifier = Modifier) =
    Text(text, style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp), modifier = modifier.padding(horizontal = 4.dp))

/** 58dp tile row: 36 r12 icon tile (bg w.07, tinted filled icon 20), title 15 + sub 12, chevron 20. */
@Composable
internal fun SNavRow(icon: ImageVector, title: String, sub: String?, shape: Shape, tint: Color = Kx.accent, tileBg: Color = Kx.w(.07f),
                     height: Dp = 58.dp, value: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(height).clip(shape).background(TileBg).kxClick(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(tileBg), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = tint)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = KxType.style(15.sp), maxLines = 1)
            if (sub != null) Text(sub, style = KxType.style(12.sp, color = Kx.text4), maxLines = 1)
        }
        if (value != null) Text(value, style = KxType.style(13.sp, color = Kx.text4))
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = Kx.text4)
    }
}

/**
 * The white-knob switch of the settings boards: [w]x[h], knob [knob] white with shadow 0 2 6 .4,
 * track inset highlight, knob travel with cubic-bezier(.34,1.5,.5,1) .35s, track colour .3s.
 */
@Composable
internal fun SSwitch(on: Boolean, trackOn: Color = Kx.accent, trackOff: Color = Kx.badgeMuted, w: Dp = 50.dp, h: Dp = 30.dp, knob: Dp = 24.dp) {
    val pad = (h - knob) / 2
    val reduced = LocalKxReducedMotion.current
    val x by animateDpAsState(if (on) w - knob - pad * 2 else 0.dp, if (reduced) tween(0) else tween(350, easing = SpringEase), label = "knob")
    val track by animateColorAsState(if (on) trackOn else trackOff, tween(if (reduced) 0 else 300), label = "track")
    Box(Modifier.size(w, h).clip(RoundedCornerShape(h / 2)).background(track).topHighlight(Kx.w(.15f))) {
        Box(Modifier.offset(pad + x, pad).size(knob).shadow(3.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape).background(Color.White))
    }
}

/** 54dp switch row of the tiles (plain 20dp #8A958F icon, gap 12, label 15). */
@Composable
internal fun SSwitchRow(icon: ImageVector?, label: String, on: Boolean, shape: Shape, sub: String? = null, trackOn: Color = Kx.accent,
                        height: Dp = 54.dp, onFlip: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = height).clip(shape).background(TileBg).kxClick(role = Role.Switch, onClick = onFlip).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = Kx.text4)
        Column(Modifier.weight(1f).padding(vertical = if (sub != null) 8.dp else 0.dp)) {
            Text(label, style = KxType.style(15.sp))
            if (sub != null) Text(sub, style = KxType.style(12.sp, color = Kx.text4, lineHeight = 16.sp))
        }
        SSwitch(on, trackOn)
    }
}

/** Solid bottom sheet (r30 top, gradient #232A26→#161B18, top line .14, grabber 40x4). */
@Composable
internal fun BoxScope.SSheet(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.matchParentSize().background(Color(0x99050706)).kxClick("Закрыть", onClick = onClose))
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
        .background(Kx.sheetGradient).topHighlight(Kx.w(.14f)).kxClick(null) {}.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.2f)))
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** Gradient helper: CSS linear-gradient(deg, stops) drawn behind. */
internal fun Modifier.cssGradient(deg: Float, vararg stops: Pair<Float, Color>): Modifier = this.drawBehind {
    drawRect(Brush.linearGradient(colorStops = stops, start = cssAngleStart(deg, size), end = cssAngleEnd(deg, size)))
}

/** 1dp border helper with the boards' white alpha. */
internal fun Modifier.wBorder(a: Float, shape: Shape): Modifier = this.border(1.dp, Kx.w(a), shape)

@Suppress("unused") private val keepOffset = Offset.Zero
