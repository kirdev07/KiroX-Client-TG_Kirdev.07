package com.kirox.app.ui.v3.screens.chat

import com.kirox.app.ui.v3.components.kxSelectedState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion

/*
 * Helpers for the second batch of `chat` boards (DeleteChat, Report, AntiSpam, …).
 */

/** CSS hex colour «#RRGGBB» → Color. */
internal fun hex(s: String): Color = Color(0xFF000000 or s.removePrefix("#").toLong(16))

/** Solid letter avatar used by these mockups: circle, colour, centred letter. */
@Composable
internal fun LetterAv(letter: String, color: Color, size: Dp, fontSize: Int, modifier: Modifier = Modifier, ink: Color = Kx.text,
                      shape: androidx.compose.ui.graphics.Shape = CircleShape) {
    Box(modifier.size(size).clip(shape).background(color), contentAlignment = Alignment.Center) {
        Text(letter, style = ts(fontSize, W800, ink), maxLines = 1)
    }
}

/**
 * Bottom sheet as these boards draw it: scrim fade, kxSheet .35s cubic-bezier(.2,.8,.2,1), radius 30 top,
 * vertical gradient [top]→[bottom], 1px top border, handle 40×4 [handleAlpha] with margin-bottom [handleGap].
 */
@Composable
internal fun BSheet(visible: Boolean, onDismiss: () -> Unit, top: Color = Color(0xFC28302C), bottom: Color = Color(0xFF121614),
                    borderAlpha: Float = .16f, handleAlpha: Float = .25f, scrim: Color = Color.Black.copy(alpha = .55f),
                    padding: PaddingValues = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
                    handleGap: Dp = 14.dp, handle: Boolean = true, radius: Dp = 30.dp, content: @Composable ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(enabled = visible, onBack = onDismiss) // fixer-final: Back closes the sheet, not the board
    val shape = RoundedCornerShape(topStart = radius, topEnd = radius)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(180))) {
            Box(Modifier.fillMaxSize().background(scrim).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        }
        AnimatedVisibility(visible, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(350, easing = KxMotion.rise)) { it }, exit = slideOutVertically(tween(250)) { it }) {
            Column(Modifier.fillMaxWidth().clip(shape).background(Color(0xFF101412)).background(Brush.verticalGradient(listOf(top, bottom)))
                .drawBehind { drawLine(Kx.w(borderAlpha), Offset(0f, .5f), Offset(size.width, .5f), 1.dp.toPx()) }
                .clickable(remember { MutableInteractionSource() }, null) {}.navigationBarsPadding().padding(padding)) {
                if (handle) Box(Modifier.fillMaxWidth().padding(bottom = handleGap), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(handleAlpha)))
                }
                content()
            }
        }
    }
}

/**
 * Board-local toast (kxToast 2s: rise 16px + fade in the first 12 %, hold, fade out after 85 %), bottom-centred.
 * Change [runKey] to replay; [text] null hides it.
 */
@Composable
internal fun BToast(text: String?, runKey: Int, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
                    bottom: Dp = 26.dp, durationMs: Int = 2000) {
    if (text == null) return
    val p = remember(runKey) { Animatable(0f) }
    LaunchedEffect(runKey) { p.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    val v = p.value
    val a = when { v < .12f -> v / .12f; v > .85f -> (1f - v) / .15f; else -> 1f }
    Box(modifier.fillMaxWidth().padding(bottom = bottom), contentAlignment = Alignment.BottomCenter) {
        Row(Modifier.graphicsLayer { alpha = a; translationY = (1f - a) * 16.dp.toPx() }
            .clip(RoundedCornerShape(18.dp)).background(Color(0xF528302C)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) androidx.compose.material3.Icon(icon, null, Modifier.size(18.dp), tint = Kx.accent)
            Text(text, style = ts(13, W700), maxLines = 1)
        }
    }
}

/** Toast of the later boards (EmojiMix, Agenda…): #232A26, border .16, 14sp w600, 20dp filled icon, rise 10px, 2.4s. Put inside key(runKey). */
@Composable
internal fun MixToast(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, bottom: Int) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(2400, easing = LinearEasing)) }
    val v = p.value; val al = when { v < .12f -> v / .12f; v > .85f -> (1f - v) / .15f; else -> 1f }
    val ty = if (v < .12f) 10f * (1f - v / .12f) else 0f
    Row(modifier.padding(bottom = bottom.dp).graphicsLayer { alpha = al; translationY = ty.dp.toPx() }
        .clip(RoundedCornerShape(18.dp)).background(Kx.sheetTop).border(1.dp, Kx.w(.16f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(icon, null, Modifier.size(20.dp), tint = Kx.accent); Text(text, style = ts(14, W600), maxLines = 1)
    }
}

/** Standard 46×28 switch of these boards (#3F9A6C / .14, knob 22 #EEF3EF / #9AA59F). */
@Composable
internal fun BSwitch(on: Boolean) = com.kirox.app.ui.v3.components.KxSwitch(on)

/** 84 gradient circle with a filled icon: kxPop (.5 → 1.12 → 1, .5s) + an outline ring pulsing out twice. */
@Composable
internal fun PopBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, ringTo: Float = 1.6f, ringAlpha: Float = .6f, ringMs: Int = 1800,
                      size: Dp = 84.dp, iconSize: Dp = 44.dp) {
    val reduced = com.kirox.app.ui.v3.theme.LocalKxReducedMotion.current
    val pop = remember { Animatable(if (reduced) 1f else 0f) }
    val ring = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) pop.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { ring.snapTo(0f); ring.animateTo(1f, tween(ringMs, easing = LinearOutSlowInEasing)) } }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (ring.value in 0.001f..0.999f) Box(Modifier.fillMaxSize().graphicsLayer {
            val s = .8f + (ringTo - .8f) * ring.value; scaleX = s; scaleY = s; alpha = ringAlpha * (1f - ring.value)
        }.border(2.dp, Kx.accent, CircleShape))
        val p = pop.value
        val s = if (p < .6f) .5f + .62f * (p / .6f) else 1.12f - .12f * ((p - .6f) / .4f)
        Box(Modifier.size(size).graphicsLayer { scaleX = s; scaleY = s; alpha = (p / .6f).coerceIn(0f, 1f) }.clip(CircleShape)
            .cssGradient(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, null, Modifier.size(iconSize), tint = Kx.onAccent)
        }
    }
}

/** Multi-line comment box: rgba(0,0,0,.25), border .1, r14, padding 10/12. */
@Composable
internal fun BTextArea(value: String, onValue: (String) -> Unit, placeholder: String, height: Dp, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .25f))
        .border(1.dp, Kx.w(.1f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.TopStart) {
        ChInput(value, onValue, placeholder, Modifier.fillMaxSize(), size = 14, singleLine = false)
    }
}

/** Radio dot: 22 ring 2px, 10 dot. */
@Composable
internal fun BRadio(on: Boolean, onColor: Color = Kx.accent, offColor: Color = Kx.w(.3f), size: Dp = 22.dp, dot: Dp = 10.dp) {
    Box(Modifier.kxSelectedState(on).size(size).border(2.dp, if (on) onColor else offColor, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(dot).clip(CircleShape).background(if (on) onColor else Color.Transparent))
    }
}

/** Undo snackbar with a 5 s countdown ring (kxUp .25s; ring stroke-dashoffset 0→75.4 linear). */
@Composable
internal fun BUndoBar(visible: Boolean, runKey: Int, left: Int, text: String, onUndo: () -> Unit, modifier: Modifier = Modifier, seconds: Int = 5) {
    AnimatedVisibility(visible, modifier, enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { (20 * it / 56) }, exit = fadeOut(tween(150))) {
        val p = remember(runKey) { Animatable(0f) }
        LaunchedEffect(runKey) { p.snapTo(0f); p.animateTo(1f, tween(seconds * 1000, easing = LinearEasing)) }
        Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 18.dp).fillMaxWidth().height(56.dp)
            .shadow(30.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = .45f), spotColor = Color.Black.copy(alpha = .45f))
            .clip(RoundedCornerShape(20.dp)).background(Color(0xF728302C)).border(1.dp, Kx.w(.14f), RoundedCornerShape(20.dp))
            .padding(start = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = 12.dp.toPx(); val w = 3.dp.toPx()
                    val tl = Offset(center.x - r, center.y - r); val sz = androidx.compose.ui.geometry.Size(2 * r, 2 * r)
                    drawCircle(Kx.w(.12f), r, style = Stroke(w))
                    val sweep = 360f * (1f - p.value)
                    if (sweep > 0f) drawArc(Kx.accent, -90f, sweep, false, tl, sz, style = Stroke(w, cap = StrokeCap.Round))
                }
                Text("$left", style = ts(12, W800))
            }
            Text(text, style = ts(14, W700), modifier = Modifier.weight(1f), maxLines = 1)
            Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).kxClick(onClick = onUndo).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text("Вернуть", style = ts(14, W800, Kx.accent))
            }
        }
    }
}
