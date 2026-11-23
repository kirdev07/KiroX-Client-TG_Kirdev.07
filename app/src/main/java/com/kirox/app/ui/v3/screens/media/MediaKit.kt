package com.kirox.app.ui.v3.screens.media

import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.draggable
import com.kirox.app.ui.v3.components.kxHitClick
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.cssAngleEnd
import com.kirox.app.ui.v3.components.cssAngleStart
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.components.topHighlight
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxType
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

/** Shared drawing helpers of the media section (Music, calls, players, stories editor…). */

/** CSS `linear-gradient(<deg>, c1, c2 <stop>…)` as a background. Stops: color to position (0..1). */
fun Modifier.cssGrad(deg: Float, vararg stops: Pair<Float, Color>): Modifier = drawBehind {
    drawRect(Brush.linearGradient(*stops, start = cssAngleStart(deg, size), end = cssAngleEnd(deg, size)))
}

fun Modifier.cssGrad(deg: Float, from: Color, to: Color, toAt: Float = 1f): Modifier = cssGrad(deg, 0f to from, toAt to to)

/**
 * CSS `radial-gradient(rx% ry% at cx% cy%, color, transparent stop%)` over the element:
 * an ellipse centered at (cx, cy) fractions with radii (rx, ry) fractions of width/height.
 */
fun Modifier.ellipseGlow(color: Color, cx: Float, cy: Float, rx: Float, ry: Float, stop: Float = 1f): Modifier = drawBehind {
    val c = Offset(size.width * cx, size.height * cy)
    val rX = size.width * rx; val rY = size.height * ry
    if (rX <= 0f || rY <= 0f) return@drawBehind
    scale(1f, rY / rX, pivot = c) {
        drawRect(Brush.radialGradient(0f to color, stop to color.copy(alpha = 0f), center = c, radius = rX),
            topLeft = Offset(0f, c.y - (c.y) * rX / rY), size = androidx.compose.ui.geometry.Size(size.width, size.height * rX / rY + 1f))
    }
}

/** Round glass button of the boards: rgba(255,255,255,a) fill, 1px border, optional inset highlight. */
@Composable
fun MRoundBtn(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp, iconSize: Dp = 24.dp,
              bg: Color = Kx.w(.06f), border: Color? = Kx.w(.09f), highlight: Color? = null, tint: Color = Kx.text,
              shape: Shape = CircleShape, onClick: () -> Unit = {}) {
    // critique-C P2: drawn at [size], but the touch / TalkBack box is always 48dp.
    Box(
        modifier.kxHitClick(label, shape, onClick = onClick).size(size).clip(shape).background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .then(if (highlight != null) Modifier.topHighlight(highlight) else Modifier),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }
}

/**
 * critique-C P1: drag a bottom sheet down to close it (past 110dp or a fast fling); otherwise it springs back.
 * Put it first on the sheet surface; lists inside still scroll first.
 */
@Composable
fun Modifier.mSheetDrag(onClose: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val off = remember { androidx.compose.animation.core.Animatable(0f) }
    val limit = with(androidx.compose.ui.platform.LocalDensity.current) { 110.dp.toPx() }
    val close by rememberUpdatedState(onClose)
    val st = androidx.compose.foundation.gestures.rememberDraggableState { d -> scope.launch { off.snapTo((off.value + d).coerceAtLeast(0f)) } }
    return this.graphicsLayer { translationY = off.value }
        .draggable(st, androidx.compose.foundation.gestures.Orientation.Vertical, onDragStopped = { v ->
            if (off.value > limit || v > 1800f) { close(); kotlinx.coroutines.delay(320); off.snapTo(0f) }
            else off.animateTo(0f, androidx.compose.animation.core.spring(stiffness = 600f))
        })
}

/** Text style shortcut: size, weight, color. */
fun ts(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Kx.text): TextStyle = KxType.style(size, weight, color)

@Composable
fun T(text: String, size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Kx.text, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) =
    Text(text, style = ts(size, weight, color), modifier = modifier, maxLines = maxLines, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)

/**
 * A clock (ms) that advances only while [running] — CSS `animation-play-state: paused` works the
 * same way: the keyframes freeze where they are and resume later. Stops at [limitMs] (finite
 * iterations) and stays at 0 under reduced motion.
 */
@Composable
fun rememberPlayClock(running: Boolean, limitMs: Long = Long.MAX_VALUE): State<Long> {
    val t = remember { mutableLongStateOf(0L) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(running, reduced) {
        if (!running || reduced) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (t.longValue < limitMs) {
            withFrameMillis { now -> t.longValue = (t.longValue + (now - last)).coerceAtMost(limitMs); last = now }
        }
    }
    return t
}

/** A forever-running clock (ms) for process animations (rings, spinners, waveforms). */
@Composable
fun rememberLoopClock(running: Boolean = true): State<Long> = rememberPlayClock(running)

val EaseInOut = CubicBezierEasing(.42f, 0f, .58f, 1f)
val Ease = CubicBezierEasing(.25f, .1f, .25f, 1f)
val EaseOutBack = CubicBezierEasing(.34f, 1.4f, .5f, 1f)
@Suppress("unused") private val keep = FastOutSlowInEasing

/** Value of a 0%→a, 50%→b, 100%→a keyframe with ease-in-out per half, for a cycle of [periodMs]. */
fun pingPong(tMs: Long, periodMs: Int, delayMs: Int, a: Float, b: Float, easing: CubicBezierEasing = EaseInOut): Float {
    val t = tMs - delayMs
    if (t < 0) return a
    val p = (t % periodMs).toFloat() / periodMs
    return if (p < .5f) a + (b - a) * easing.transform(p * 2f) else b + (a - b) * easing.transform((p - .5f) * 2f)
}

/** Formats seconds as m:ss. */
fun fmtSec(s: Int): String = "${s / 60}:${(s % 60).toString().padStart(2, '0')}"

/** Dim backdrop + bottom sheet (radius 30 top, solid gradient, kxSheet .38s slide-up), closes on back/tap outside. */
@Composable
fun MSheet(visible: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier, dim: Float = .6f,
           top: Color = Color(0xFF222C27), bottom: Color = Color(0xFF101412),
           padding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp),
           handle: Boolean = true, border: Color = Kx.w(.16f), highlight: Color? = Kx.w(.2f), handleColor: Color = Kx.w(.25f), handleWidth: Dp = 40.dp,
           content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(visible, onClose)
    Box(Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedVisibility(visible, enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)).kxClick("Закрыть", onClick = onClose))
        }
        androidx.compose.animation.AnimatedVisibility(visible, Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(380, easing = CubicBezierEasing(.2f, .9f, .25f, 1f))) { it },
            exit = androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(250)) { it }) {
            val shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            androidx.compose.foundation.layout.Column(
                modifier.mSheetDrag(onClose).fillMaxWidth().clip(shape).background(Color(0xFF101412)).background(Brush.verticalGradient(listOf(top, bottom)))
                    .border(1.dp, border, shape).then(if (highlight != null) Modifier.topHighlight(highlight) else Modifier)
                    .kxClick { }.navigationBarsPadding().padding(padding)
            ) {
                if (handle) Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).size(handleWidth, 4.dp).clip(CircleShape).background(handleColor))
                content()
            }
        }
    }
}

/** Confirm dialog of the boards: 28 radius, title 18/800, text 14/20 #C9D2CC, Отмена + red action. */
@Composable
fun MConfirm(title: String?, text: String, yes: String, onNo: () -> Unit, onYes: () -> Unit, danger: Boolean = true) {
    androidx.activity.compose.BackHandler(title != null, onNo)
    androidx.compose.animation.AnimatedVisibility(title != null, enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).kxClick("Отмена", onClick = onNo), contentAlignment = Alignment.Center) {
            val shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
            androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF232A26), Color(0xFF161B18)))).border(1.dp, Kx.w(.14f), shape).kxClick { }.padding(20.dp)) {
                T(title ?: "", 18.sp, FontWeight.ExtraBold)
                Text(text, style = ts(14.sp, color = Kx.text2).copy(lineHeight = 20.sp), modifier = Modifier.padding(top = 8.dp))
                androidx.compose.foundation.layout.Row(Modifier.padding(top = 16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(48.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick(onClick = onNo), contentAlignment = Alignment.Center) { T("Отмена", 15.sp, FontWeight.Bold) }
                    Box(Modifier.weight(1f).height(48.dp).clip(CircleShape).background(if (danger) Color(0xFFE5534B) else Kx.accent).kxClick(onClick = onYes), contentAlignment = Alignment.Center) {
                        T(yes, 15.sp, FontWeight.ExtraBold, if (danger) Color.White else Kx.onAccent)
                    }
                }
            }
        }
    }
}

/** State holder for a pending confirm: title/text/yes + action. */
class MConfirmState {
    var title by mutableStateOf<String?>(null); var text by mutableStateOf(""); var yes by mutableStateOf("")
    private var action: (() -> Unit)? = null
    fun ask(t: String, x: String, y: String, fn: () -> Unit) { title = t; text = x; yes = y; action = fn }
    fun no() { title = null; action = null }
    fun yes() { val fn = action; no(); fn?.invoke() }
}

@Composable
fun MConfirmHost(s: MConfirmState, danger: Boolean = true) = MConfirm(s.title, s.text, s.yes, { s.no() }, { s.yes() }, danger)


/** Board-local toast (kxToast 2.2s: rise in, hold, fade out). Call [show]; draw [MToastHost] last in the screen Box. */
class MToastState {
    var text by mutableStateOf(""); var stamp by mutableLongStateOf(0L)
    fun show(t: String) { text = t; stamp = System.nanoTime() }
}

@Composable
fun BoxScope.MToastHost(s: MToastState, bottom: Dp = 96.dp, top: Dp? = null, icon: ImageVector = androidx.compose.material.icons.Icons.Rounded.CheckCircle) {
    if (s.stamp == 0L) return
    val p = remember(s.stamp) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(s.stamp) { p.animateTo(1f, androidx.compose.animation.core.tween(2200, easing = androidx.compose.animation.core.LinearEasing)); s.stamp = 0L }
    val v = p.value
    val a = when { v < .12f -> v / .12f; v < .85f -> 1f; else -> 1f - (v - .85f) / .15f }
    val ty = if (v < .12f) 16f * (1f - v / .12f) else 0f
    Row((if (top != null) Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = top)
        else Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = bottom)).fillMaxWidth().height(46.dp)
        .graphicsLayerCompat(a, ty).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(Color(0xFF1E2622))
        .border(1.dp, Kx.accent.copy(alpha = .25f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = Kx.accent)
        T(s.text, 14.sp)
    }
}

private fun Modifier.graphicsLayerCompat(a: Float, ty: Float) = this.then(Modifier.graphicsLayer { alpha = a; translationY = ty * density })

/** Centered pill toast with an icon (kxToast 2.4s), used by VideoTrim / StoryViews / StoryArchive. */
class MPillToastState {
    var text by mutableStateOf(""); var icon by mutableStateOf<ImageVector?>(null); var stamp by mutableLongStateOf(0L)
    fun show(t: String, i: ImageVector) { text = t; icon = i; stamp = System.nanoTime() }
}

@Composable
fun BoxScope.MPillToastHost(s: MPillToastState, bottom: Dp = 34.dp) {
    if (s.stamp == 0L) return
    val a = remember(s.stamp) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(s.stamp) { a.animateTo(1f, androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.LinearEasing)); s.stamp = 0L }
    val v = a.value
    val al = when { v < .12f -> v / .12f; v < .85f -> 1f; else -> 1f - (v - .85f) / .15f }
    Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom)
        .graphicsLayer { alpha = al; translationY = if (v < .12f) 10.dp.toPx() * (1 - v / .12f) else 0f }
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).background(Kx.surface3)
        .border(1.dp, Kx.w(.16f), androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        s.icon?.let { Icon(it, null, Modifier.size(20.dp), tint = Kx.accent) }
        T(s.text, 14.sp, FontWeight.SemiBold, maxLines = 1)
    }
}
