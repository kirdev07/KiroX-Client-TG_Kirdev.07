package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

/** Shared pieces of the «more» boards (message actions, composer extras). Values repeat across those mockups. */

internal val MoEase = CubicBezierEasing(.25f, .1f, .25f, 1f)
internal val MoEaseOut = CubicBezierEasing(0f, 0f, .58f, 1f)
internal val MoEaseInOut = CubicBezierEasing(.42f, 0f, .58f, 1f)
internal val MoInk = Color(0xFF06150D)
internal val MoMuted = Color(0xFF8A958F)
internal val MoSub = Color(0xFF9AA59F)
internal val MoAmber = Color(0xFFE8B04A)
internal val MoRed = Color(0xFFE5534B)
internal val MoRedInk = Color(0xFFFF8A80)

internal val W4 = FontWeight.Normal
internal val W5 = FontWeight.Medium
internal val W6 = FontWeight.SemiBold
internal val W7 = FontWeight.Bold
internal val W8 = FontWeight.ExtraBold

/** Text style shortcut: size in sp, line height as a CSS multiplier when [lh] > 0. */
internal fun mts(size: Int, weight: FontWeight = W4, color: Color = Kx.text, lh: Float = 0f, ls: TextUnit = TextUnit.Unspecified): TextStyle =
    KxType.style(size.sp, weight, color, if (lh > 0f) (size * lh).sp else TextUnit.Unspecified).copy(letterSpacing = ls)

/** One-shot 0→1 progress, like `animation: x .3s ease both`; restarts when [key] changes. */
@Composable
internal fun moEnter(durMs: Int, delayMs: Int = 0, easing: Easing = MoEase, key: Any? = Unit): Float {
    val reduced = LocalKxReducedMotion.current
    val a = remember(key) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(key) { if (!reduced) { a.snapTo(0f); delay(delayMs.toLong()); a.animateTo(1f, tween(durMs, easing = easing)) } }
    return a.value
}

/** kxIn: fade + lift by [dy]. */
@Composable
internal fun Modifier.moIn(durMs: Int = 300, delayMs: Int = 0, dy: Dp = 8.dp, easing: Easing = MoEase, key: Any? = Unit): Modifier {
    val p = moEnter(durMs, delayMs, easing, key)
    return this.graphicsLayer { alpha = p; translationY = (1f - p) * dy.toPx() }
}

/** kxFade. */
@Composable
internal fun Modifier.moFade(durMs: Int = 200, key: Any? = Unit): Modifier {
    val p = moEnter(durMs, key = key)
    return this.graphicsLayer { alpha = p }
}

/** Endless keyframe progress (process animations). */
@Composable
internal fun moLoop(periodMs: Int, delayMs: Int = 0, label: String = "loop"): Float {
    if (LocalKxReducedMotion.current) return 0f
    val t = rememberInfiniteTransition(label = label)
    val off = if (delayMs < 0) StartOffset(-delayMs) else StartOffset(delayMs, StartOffsetType.Delay)
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), initialStartOffset = off), label = label)
    return v
}

/** CSS linear-gradient(deg, a, b). */
internal fun Modifier.moLin(deg: Float, a: Color, b: Color): Modifier = this.drawBehind {
    drawRect(Brush.linearGradient(listOf(a, b), cssAngleStart(deg, size), cssAngleEnd(deg, size)))
}

/** The accent bubble / button gradient: linear-gradient(135deg, #6FC497, #3F9A6C). */
internal fun Modifier.moAccent(deg: Float = 135f): Modifier = moLin(deg, Kx.accentMid, Kx.accentDeep)

/** 1px line at the bottom / top edge (header and composer separators). */
internal fun Modifier.moBottomLine(color: Color = Kx.w(.06f)): Modifier = this.drawBehind {
    drawLine(color, Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1.dp.toPx())
}
internal fun Modifier.moTopLine(color: Color = Kx.w(.06f)): Modifier = this.drawBehind {
    drawLine(color, Offset(0f, .5f), Offset(size.width, .5f), 1.dp.toPx())
}

/** Round button: 44, bg .06, 1px .09 border (the boards' back button). */
@Composable
internal fun MoRound(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp, iconSize: Dp = 24.dp,
                     bg: Color = Kx.w(.06f), border: Color? = Kx.w(.09f), tint: Color = Kx.text, onClick: () -> Unit) {
    Box(modifier.size(size).clip(CircleShape).background(bg).then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier)
        .kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }
}

@Composable
internal fun MoBack(onClick: () -> Unit) = MoRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", onClick = onClick)

/** Letter avatar on linear-gradient(135deg, a, b) (or a flat color when [b] is null). */
@Composable
internal fun MoLetter(letter: String, a: Color, b: Color? = null, size: Dp = 40.dp, fontSize: Int = 16, modifier: Modifier = Modifier, square: Dp? = null) {
    val shape = if (square != null) RoundedCornerShape(square) else CircleShape
    Box(modifier.size(size).clip(shape).then(if (b != null) Modifier.moLin(135f, a, b) else Modifier.background(a)), contentAlignment = Alignment.Center) {
        Text(letter, style = mts(fontSize, W8))
    }
}

/** Chat header: padding 10/12/8, gap 10, avatar 40, name 16 w800, sub 12; border-bottom .06. */
@Composable
internal fun MoChatHeader(onBack: () -> Unit, letter: String, a: Color, b: Color?, name: String, sub: String, subColor: Color = MoSub,
                          modifier: Modifier = Modifier, line: Boolean = true, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().then(if (line) Modifier.moBottomLine() else Modifier).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MoBack(onBack)
        MoLetter(letter, a, b)
        Column(Modifier.weight(1f)) {
            Text(name, style = mts(16, W8), maxLines = 1)
            Text(sub, style = mts(12, color = subColor), maxLines = 1)
        }
        actions()
    }
}

/** Sub-screen title header: padding 10/12/6, gap 8, h1 20 w800, optional sub 12 #9AA59F. */
@Composable
internal fun MoTitleHeader(title: String, onBack: () -> Unit, sub: String? = null, modifier: Modifier = Modifier, size: Int = 20,
                           actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MoBack(onBack)
        Column(Modifier.weight(1f)) {
            Text(title, style = mts(size, W8), maxLines = 1, modifier = Modifier.kxHeading())
            if (sub != null) Text(sub, style = mts(12, color = MoSub), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        actions()
    }
}

/** Board text input with a placeholder. */
@Composable
internal fun MoInput(value: String, onValue: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, size: Int = 15,
                     color: Color = Kx.text, placeholderColor: Color = Color(0xFF7C8781), singleLine: Boolean = true,
                     keyboard: KeyboardOptions = KeyboardOptions.Default) {
    BasicTextField(value, onValue, modifier, singleLine = singleLine, textStyle = mts(size, color = color), cursorBrush = SolidColor(Kx.accent),
        keyboardOptions = keyboard,
        decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) { if (value.isEmpty()) Text(placeholder, style = mts(size, color = placeholderColor), maxLines = 1); inner() } })
}

/**
 * The boards' gradient sheet: scrim rgba(0,0,0,.55) kxFade .2s, rgba(40,48,44,.97) → rgba(18,22,20,.99) on solid black,
 * radius 30 top, 1px top line .16, grabber 40x4 .25, kxSheet .3s cubic-bezier(.2,.8,.2,1).
 */
@Composable
internal fun BoxScope.MoSheet(onClose: () -> Unit, pad: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
                              grabGap: Dp = 14.dp, scrim: Float = .55f, durMs: Int = 300,
                              colors: List<Color> = listOf(Color(0xF828302C), Color(0xFC121614)), line: Color = Kx.w(.16f), between: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose) // fixer-final: Back closes the sheet, not the board
    val p = moEnter(durMs, easing = KxMotion.rise)
    val f = moEnter(200)
    Box(Modifier.matchParentSize().graphicsLayer { alpha = f }.background(Color.Black.copy(alpha = scrim)).kxClick("Закрыть", onClick = onClose))
    between() // above the dim, below the sheet (z-index 3 vs 4 on the boards)
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = (1f - p) * size.height }
        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Kx.bg)
        .background(Brush.verticalGradient(colors))
        .drawBehind { drawLine(line, Offset(0f, .5f), Offset(size.width, .5f), 1.dp.toPx()) }
        .clickable(interactionSource = null, indication = null) {}
        .imePadding().navigationBarsPadding().padding(pad)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
        Spacer(Modifier.height(grabGap))
        content()
    }
}

/** Board-local toast (kxToast 2.4s: in 12%, hold to 85%, out), #232A26 pill, accent icon. */
internal class MoToastState {
    var n by mutableIntStateOf(0)
    var text by mutableStateOf("")
    var icon by mutableStateOf(Icons.Rounded.CheckCircle)
    fun show(t: String, i: ImageVector = Icons.Rounded.CheckCircle) { text = t; icon = i; n++ }
}

@Composable
internal fun rememberMoToast() = remember { MoToastState() }

@Composable
internal fun BoxScope.MoToast(state: MoToastState, bottom: Dp = 30.dp, durMs: Int = 2400) {
    if (state.n == 0) return
    key(state.n) {
        var alive by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) { delay(durMs.toLong()); alive = false }
        if (alive) MoToastBody(state, bottom, durMs)
    }
}

@Composable
private fun BoxScope.MoToastBody(state: MoToastState, bottom: Dp, durMs: Int) {
    run {
        val t = moEnter(durMs, easing = LinearEasing)
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom, start = 16.dp, end = 16.dp).graphicsLayer {
            alpha = when { t < .12f -> t / .12f; t < .85f -> 1f; else -> 1f - (t - .85f) / .15f }
            translationY = if (t < .12f) (1f - t / .12f) * 10.dp.toPx() else 0f
        }.clip(RoundedCornerShape(18.dp)).background(Kx.surface3).border(1.dp, Kx.w(.16f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(state.icon, null, Modifier.size(20.dp), tint = Kx.accent)
            Text(state.text, style = mts(14, W6), maxLines = 1)
        }
    }
}

/** Russian plural: 1 сообщение, 2 сообщения, 5 сообщений. */
internal fun moPlural(n: Int, one: String, few: String, many: String): String {
    val m = kotlin.math.abs(n) % 100; val d = m % 10
    return if (m in 11..19) many else if (d == 1) one else if (d in 2..4) few else many
}

/** Small pill button (36 high, r18, .06/.08 bg, 13 w700). */
@Composable
internal fun MoPill(text: String, modifier: Modifier = Modifier, height: Dp = 36.dp, bg: Color = Kx.w(.06f), ink: Color = Kx.text, size: Int = 13,
                    weight: FontWeight = W7, padH: Dp = 12.dp, icon: ImageVector? = null, iconSize: Dp = 16.dp, gap: Dp = 4.dp, onClick: () -> Unit) {
    Row(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(bg).kxClick(onClick = onClick).padding(horizontal = padH),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
        if (icon != null) Icon(icon, null, Modifier.size(iconSize), tint = ink)
        Text(text, style = mts(size, weight, ink), maxLines = 1)
    }
}

/** 0 → 1 → 0 (keyframes base / peak / base). */
internal fun moTri(t: Float): Float { if (t <= 0f || t >= 1f) return 0f; return if (t < .5f) t * 2 else (1f - t) * 2 }

/** Remembers where an element sits (root coordinates) so a copy can be drawn above a scrim (CSS z-index over the dim). */
internal class MoLift { var x by mutableFloatStateOf(0f); var y by mutableFloatStateOf(0f); var w by mutableIntStateOf(0) }

internal fun Modifier.moTrack(l: MoLift): Modifier = this.onGloballyPositioned { val p = it.positionInRoot(); l.x = p.x; l.y = p.y; l.w = it.size.width }

/** Draws [content] at the tracked place of [target], relative to the box tracked by [root]. */
@Composable
internal fun MoLifted(target: MoLift, root: MoLift, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    Box(modifier.offset { IntOffset((target.x - root.x).toInt(), (target.y - root.y).toInt()) }.then(if (target.w > 0) Modifier.width(with(d) { target.w.toDp() }) else Modifier)) { content() }
}

/** Collapses the measured height to [f] (0..1) — the max-height part of kxPoof. */
internal fun Modifier.moCollapse(f: Float): Modifier = this.layout { m, c ->
    val p = m.measure(c); val h = (p.height * f.coerceIn(0f, 1f)).toInt()
    layout(p.width, h) { p.place(0, 0) }
}

/** CSS filter: saturate([amount]) (also grayscale when 0). */
internal fun Modifier.moSaturate(amount: Float): Modifier = this.drawWithContent {
    val paint = androidx.compose.ui.graphics.Paint().apply {
        colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(amount) })
    }
    drawIntoCanvas { c -> c.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint); drawContent(); c.restore() }
}

/** Decorative keyframes run [times] times then stop; returns the progress in the current iteration, -1 when idle. */
@Composable
internal fun moRun(periodMs: Int, times: Int = 2, delayMs: Int = 0, key: Any? = Unit): Float {
    val reduced = LocalKxReducedMotion.current
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(reduced, key) {
        if (reduced) return@LaunchedEffect
        delay(delayMs.toLong()); a.snapTo(0f)
        a.animateTo(times.toFloat(), tween(periodMs * times, easing = LinearEasing)); a.snapTo(0f)
    }
    val v = a.value
    return if (v <= 0f || v >= times) -1f else v - kotlin.math.floor(v)
}

/** box-shadow: 0 0 0 [spread] [color] around a rounded shape of [radius] (draw before clip). */
internal fun Modifier.moSpreadRing(spread: Dp, color: Color, radius: Dp): Modifier = this.drawBehind {
    val g = spread.toPx(); if (g <= 0f) return@drawBehind
    drawRoundRect(color, Offset(-g / 2, -g / 2), androidx.compose.ui.geometry.Size(size.width + g, size.height + g),
        androidx.compose.ui.geometry.CornerRadius(radius.toPx() + g / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(g))
}

/** kxGlow (0%,100% none; 50% spread [max] at [alpha]) run [times] times, ease-in-out. */
@Composable
internal fun Modifier.moGlow(periodMs: Int, max: Dp, alpha: Float, radius: Dp, times: Int = 2, key: Any? = Unit, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val t = moRun(periodMs, times, key = key)
    val v = if (t < 0f) 0f else MoEaseInOut.transform(moTri(t))
    return this.moSpreadRing(max * v, Kx.accent.copy(alpha = alpha * v), radius)
}

/** Bubble text: [text] + small time (alpha) + optional inline icon (done_all / schedule…). */
@Composable
internal fun MoBubbleText(text: String, time: String, ink: Color, size: Int = 15, timeAlpha: Float = .7f, icon: ImageVector? = null,
                          iconSize: Int = 15, lh: Float = 0f, prefix: (@Composable () -> Unit)? = null) {
    val s = androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        if (time.isNotEmpty()) { pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = 11.sp, color = ink.copy(alpha = timeAlpha))); append("  $time"); pop() }
        if (icon != null) { append(" "); appendInlineContent("ic", "·") }
    }
    Text(s, style = mts(size, color = ink, lh = lh), inlineContent = if (icon == null) emptyMap() else mapOf("ic" to androidx.compose.foundation.text.InlineTextContent(
        androidx.compose.ui.text.Placeholder(iconSize.sp, iconSize.sp, androidx.compose.ui.text.PlaceholderVerticalAlign.TextCenter)) {
        Icon(icon, null, Modifier.fillMaxSize(), tint = ink.copy(alpha = .85f))
    }))
}

/** Static composer of the boards: 48 high field .07 with a gray «Сообщение». */
@Composable
internal fun MoStaticComposer(text: String = "Сообщение") {
    Box(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) { Text(text, style = mts(15, color = MoMuted)) }
    }
}

/** Incoming bubble (r 18 18 18 6, .07): optional author line (12 w800), text 15 + time. */
@Composable
internal fun MoIncoming(text: String, time: String = "", author: String? = null, authorColor: Color = MoAmber, modifier: Modifier = Modifier,
                        bg: Color = Kx.w(.07f), size: Int = 15) {
    Column(modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(bg).padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (author != null) Text(author, style = mts(12, W8, authorColor))
        MoBubbleText(text, time, Kx.text, size = size, timeAlpha = .6f)
    }
}

/** Dashed 1px border following any [shape] (border: 1px dashed). */
internal fun Modifier.moDashed(shape: androidx.compose.ui.graphics.Shape, color: Color, width: Dp = 1.dp, dash: Dp = 4.dp, gap: Dp = 3.dp): Modifier = this.drawBehind {
    val o = shape.createOutline(size, layoutDirection, this)
    drawOutline(o, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))))
}

/**
 * Glass popup over a dim (kxDim + kxMenu): gradient .16 → .07 on a dark base (stands in for backdrop blur), 1px .16,
 * shadow 0 16 40 black .5; slides [dy] and scales from [from] around the [end]-side bottom corner.
 */
@Composable
internal fun BoxScope.MoPopup(onClose: () -> Unit, width: Dp, bottom: Dp, end: Boolean, side: Dp = 12.dp, pad: Dp = 8.dp, radius: Dp = 24.dp,
                              scrim: Float = .5f, from: Float = .92f, dy: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.matchParentSize().moFade(200).background(Color.Black.copy(alpha = scrim)).kxClick("Закрыть", onClick = onClose))
    val p = moEnter(250, easing = KxMotion.rise)
    Column(Modifier.align(if (end) Alignment.BottomEnd else Alignment.BottomStart).navigationBarsPadding()
        .padding(start = if (end) 0.dp else side, end = if (end) side else 0.dp, bottom = bottom).width(width).graphicsLayer {
            alpha = p; translationY = (1f - p) * dy.toPx(); val s = from + (1f - from) * p; scaleX = s; scaleY = s
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (end) 1f else 0f, 1f)
        }.shadow(24.dp, RoundedCornerShape(radius), ambientColor = Color.Black, spotColor = Color.Black).clip(RoundedCornerShape(radius))
        .background(Color(0xFF1E2421)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f))))
        .border(1.dp, Kx.w(.16f), RoundedCornerShape(radius)).padding(pad), content = content)
}

/** CSS keyframes for one property: [stops] are (offset 0..1, value); each segment eased with [easing] (CSS applies it per segment). */
internal fun moKf(t: Float, vararg stops: Pair<Float, Float>, easing: Easing = MoEaseInOut): Float {
    if (t <= stops.first().first) return stops.first().second
    for (i in 1 until stops.size) {
        val (t1, v1) = stops[i]
        if (t <= t1) { val (t0, v0) = stops[i - 1]; val f = if (t1 > t0) easing.transform((t - t0) / (t1 - t0)) else 1f; return v0 + (v1 - v0) * f }
    }
    return stops.last().second
}

/** linear-gradient([deg], colors) with background-size 300% 100% at background-position [p] (0..1): the kxPan shimmer. */
internal fun Modifier.moPan(deg: Float, colors: List<Color>, p: Float): Modifier = this.drawBehind {
    val bw = size.width * 3f
    val big = androidx.compose.ui.geometry.Size(bw, size.height)
    val shift = -(bw - size.width) * p
    val s = cssAngleStart(deg, big); val e = cssAngleEnd(deg, big)
    drawRect(Brush.linearGradient(colors, Offset(s.x + shift, s.y), Offset(e.x + shift, e.y)))
}

/** kxPan 4s linear alternate: 0 → 1 → 0…; [times] < 0 = infinite. */
@Composable
internal fun moPanProgress(periodMs: Int = 4000, times: Int = -1): Float {
    if (times < 0) { val v = moLoop(periodMs * 2, label = "pan"); return if (v < .5f) v * 2 else 2f - v * 2 }
    val t = moRun(periodMs * 2, 1)
    return if (t < 0f) 0f else moTri(t)
}

/** Blinking caret (2 x 18, accent, kxCaret 1s step-end infinite). */
@Composable
internal fun MoCaret(modifier: Modifier = Modifier, h: Dp = 18.dp) {
    val c = moLoop(1000, label = "caret")
    Box(modifier.padding(start = 1.dp).size(2.dp, h).graphicsLayer { alpha = if (c < .5f) 1f else 0f }.background(Kx.accent))
}

/** Horizontal band gradient of [bandW] moving from [fromX] to [toX] (background-position), repeated or a single band. */
internal fun Modifier.moShimmer(p: Float, colors: List<Color>, bandW: Dp, fromX: Dp, toX: Dp, repeat: Boolean = true): Modifier = this.drawBehind {
    val w = bandW.toPx(); val x = fromX.toPx() + (toX.toPx() - fromX.toPx()) * p
    drawRect(Brush.linearGradient(colors, Offset(x, 0f), Offset(x + w, 0f),
        tileMode = if (repeat) androidx.compose.ui.graphics.TileMode.Repeated else androidx.compose.ui.graphics.TileMode.Decal))
}

/** keep_off: the outlined pin with a slash. */
@Composable
internal fun MoKeepOff(size: Dp = 20.dp, tint: Color = MoSub) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(androidx.compose.material.icons.Icons.Outlined.PushPin, null, Modifier.fillMaxSize(), tint = tint)
        Box(Modifier.size(size * 1.1f, 2.dp).graphicsLayer { rotationZ = -45f }.background(tint))
    }
}

/** Toast of the later boards: rgba(40,48,44,.96), 1px .12, r18, 13 w700, check_circle 18; in from +16 (2 s, 12%/85%). */
@Composable
internal fun BoxScope.MoNoteToast(text: String, k: Int, bottom: Dp = 26.dp, durMs: Int = 2000, icon: ImageVector = Icons.Rounded.CheckCircle) {
    if (text.isEmpty()) return
    key(k) {
        val p = moEnter(durMs, easing = LinearEasing)
        val a = when { p < .12f -> p / .12f; p < .85f -> 1f; else -> 1f - (p - .85f) / .15f }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom).graphicsLayer { alpha = a; translationY = (1f - a) * 16.dp.toPx() }
            .clip(RoundedCornerShape(18.dp)).background(Color(0xF528302C)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = Kx.accent)
            Text(text, style = mts(13, W7), maxLines = 1)
        }
    }
}
