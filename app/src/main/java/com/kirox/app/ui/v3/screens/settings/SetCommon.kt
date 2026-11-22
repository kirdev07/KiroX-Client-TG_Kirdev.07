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
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
internal fun SSwitch(on: Boolean, trackOn: Color = Kx.accent, trackOff: Color = Kx.badgeMuted, w: Dp = 50.dp, h: Dp = 30.dp, knob: Dp = 24.dp, highlight: Boolean = true, knobShadow: Boolean = true) {
    val pad = (h - knob) / 2
    val reduced = LocalKxReducedMotion.current
    val x by animateDpAsState(if (on) w - knob - pad * 2 else 0.dp, if (reduced) tween(0) else tween(350, easing = SpringEase), label = "knob")
    val track by animateColorAsState(if (on) trackOn else trackOff, tween(if (reduced) 0 else 300), label = "track")
    Box(Modifier.size(w, h).clip(RoundedCornerShape(h / 2)).background(track).then(if (highlight) Modifier.topHighlight(Kx.w(.15f)) else Modifier)) {
        Box(Modifier.offset(pad + x, pad).size(knob).then(if (knobShadow) Modifier.shadow(3.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black) else Modifier)
            .clip(CircleShape).background(Color.White))
    }
}

/** 54dp switch row of the tiles (plain 20dp #8A958F icon, gap 12, label 15). */
@Composable
internal fun SSwitchRow(icon: ImageVector?, label: String, on: Boolean, shape: Shape, sub: String? = null, trackOn: Color = Kx.accent,
                        height: Dp = 54.dp, small: Boolean = false, subSize: Int = 12, onFlip: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = height).clip(shape).background(TileBg).kxClick(role = Role.Switch, onClick = onFlip).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = Kx.text4)
        Column(Modifier.weight(1f).padding(vertical = if (sub != null) 8.dp else 0.dp)) {
            Text(label, style = KxType.style(15.sp))
            if (sub != null) Text(sub, style = KxType.style(subSize.sp, color = Kx.text4, lineHeight = (subSize + 4).sp))
        }
        if (small) SSwitch(on, trackOn, w = 48.dp, h = 28.dp, knob = 22.dp, highlight = false) else SSwitch(on, trackOn)
    }
}

/**
 * Bottom sheet of the settings boards: backdrop rgba(0,0,0,.55) kxFade .2s, sheet r30 top,
 * gradient rgba(34,44,39,.94)→rgba(16,20,18,.97), top line .16 + inset .2, padding 10 16 24,
 * grabber 40x4 .25 (margin-bottom 14), kxSheet .38s cubic-bezier(.2,.9,.25,1).
 */
@Composable
internal fun BoxScope.SSheet(onClose: () -> Unit, label: String? = null, top: Color = Color(34, 44, 39).copy(alpha = .94f), bottom: Color = Color(16, 20, 18).copy(alpha = .97f),
                             bottomPad: Dp = 24.dp, grabberGap: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(if (reduced) 1f else 0f) }
    val fade = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { launch { fade.animateTo(1f, tween(200)) }; p.animateTo(1f, tween(380, easing = SheetEase)) }
    Box(Modifier.matchParentSize().graphicsLayer { alpha = fade.value }.background(Color.Black.copy(alpha = .55f)).kxClick("Закрыть", onClick = onClose))
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = (1f - p.value) * size.height }
        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .drawBehind { drawLine(Kx.w(.2f), Offset(0f, 1.5f), Offset(size.width, 1.5f), 1f); drawLine(Kx.w(.16f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
        .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
        .clickable(interactionSource = null, indication = null) {}
        .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = bottomPad)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
        Spacer(Modifier.height(grabberGap))
        content()
    }
}

/** cubic-bezier(.2,.9,.25,1) of kxSheet. */
internal val SheetEase = CubicBezierEasing(.2f, .9f, .25f, 1f)
/** CSS `ease`. */
internal val CssEase = CubicBezierEasing(.25f, .1f, .25f, 1f)

/** kxRise of the settings boards: fade + [dy] lift, CSS ease by default. */
@Composable
internal fun Modifier.sRise(delayMs: Int = 0, durMs: Int = 450, dy: Dp = 10.dp, easing: Easing = CssEase): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(durMs, easing = easing)) }
    return this.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * dy.toPx() }
}

/** Radio choice row of the picker sheets (56, r18, accent tint when on, radio dot springs in). */
@Composable
internal fun SChoice(icon: ImageVector?, iconOn: ImageVector?, label: String, on: Boolean, sub: String? = null, onPick: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.05f), tween(250), label = "bg")
    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.06f), tween(250), label = "bd")
    val dot by animateFloatAsState(if (on) 1f else 0f, if (reduced) tween(0) else tween(300, easing = CubicBezierEasing(.34f, 1.6f, .5f, 1f)), label = "dot")
    val shape = RoundedCornerShape(18.dp)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(bg).border(1.dp, bd, shape).kxClick(role = Role.RadioButton, onClick = onPick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) Icon(if (on && iconOn != null) iconOn else icon, null, Modifier.size(22.dp), tint = if (on) Kx.accent else Kx.text3)
        Column(Modifier.weight(1f).padding(vertical = if (sub != null) 8.dp else 0.dp)) {
            Text(label, style = KxType.style(15.sp, FontWeight.SemiBold))
            if (sub != null) Text(sub, style = KxType.style(12.sp, color = Kx.text4))
        }
        Box(Modifier.size(22.dp).border(2.dp, if (on) Kx.accent else Color(0xFF5E6964), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).graphicsLayer { scaleX = dot; scaleY = dot }.clip(CircleShape).background(Kx.accent))
        }
    }
}

/** Gradient helper: CSS linear-gradient(deg, stops) drawn behind. */
internal fun Modifier.cssGradient(deg: Float, vararg stops: Pair<Float, Color>): Modifier = this.drawBehind {
    drawRect(Brush.linearGradient(colorStops = stops, start = cssAngleStart(deg, size), end = cssAngleEnd(deg, size)))
}

/** 1dp border helper with the boards' white alpha. */
internal fun Modifier.wBorder(a: Float, shape: Shape): Modifier = this.border(1.dp, Kx.w(a), shape)

@Suppress("unused") private val keepOffset = Offset.Zero

/** Size as the settings boards write it: «1,1 ГБ», «410 МБ», «0 МБ». */
internal fun sBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f ГБ".format(java.util.Locale.ROOT, bytes / (1L shl 30).toDouble()).replace('.', ',')
    else -> "${(bytes + (1L shl 19)) / (1L shl 20)} МБ"
}

/** Shows store actions' failures (store.error, e.g. FLOOD_WAIT, no rights) as a toast while the screen is open. */
@Composable
internal fun StoreErrorToasts(ctx: com.kirox.app.ui.v3.KxScreenCtx) {
    val err = ctx.store.error
    LaunchedEffect(err) { if (err != null) { ctx.toast(err); ctx.store.clearError() } }
}
