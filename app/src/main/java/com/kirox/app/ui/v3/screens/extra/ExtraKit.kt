package com.kirox.app.ui.v3.screens.extra

import android.graphics.BlurMaskFilter
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared pieces of the «extra» boards (onboarding, auth extras, profile extras). */

internal val ExBg = Color(0xFF0B0D0C)
internal val EaseCss = CubicBezierEasing(.25f, .1f, .25f, 1f)
internal val EaseInOut = CubicBezierEasing(.42f, 0f, .58f, 1f)
internal val EaseOutCss = CubicBezierEasing(0f, 0f, .58f, 1f)
internal val EaseSpring = CubicBezierEasing(.34f, 1.45f, .5f, 1f)
internal val EaseSheet = CubicBezierEasing(.2f, .9f, .3f, 1f)

internal fun ts(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Kx.text, lh: TextUnit = TextUnit.Unspecified, ls: TextUnit = TextUnit.Unspecified): TextStyle =
    KxType.style(size, weight, color, lh).copy(letterSpacing = ls)

/**
 * A CSS keyframe animation run [times] (decorative) after [delayMs]; returns the progress inside the
 * current iteration (0..1), -1 before/after (= the element's own style). Reduced motion → always -1.
 */
@Composable
internal fun kfRun(periodMs: Int, times: Int = 2, delayMs: Int = 0, key: Any? = Unit): Float {
    val reduced = LocalKxReducedMotion.current
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(reduced, key) {
        if (reduced) return@LaunchedEffect
        delay(delayMs.toLong())
        a.snapTo(0f)
        a.animateTo(times.toFloat(), tween(periodMs * times, easing = LinearEasing))
        a.snapTo(0f)
    }
    val v = a.value
    return if (v <= 0f || v >= times) -1f else v - kotlin.math.floor(v)
}

/** Endless keyframe animation (process ones: spinners, scan lines). Negative delay = start offset. */
@Composable
internal fun kfLoop(periodMs: Int, delayMs: Int = 0, label: String = "loop"): Float {
    if (LocalKxReducedMotion.current) return 0f
    val t = rememberInfiniteTransition(label = label)
    val off = if (delayMs < 0) StartOffset(-delayMs) else StartOffset(delayMs, StartOffsetType.Delay)
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), initialStartOffset = off), label = label)
    return v
}

/** 0 → 1 → 0 with ease-in-out per half (keyframes 0%,100% = base, 50% = peak). */
internal fun tri(t: Float): Float {
    if (t <= 0f) return 0f
    val h = if (t < .5f) t * 2 else (1f - t) * 2
    return EaseInOut.transform(h.coerceIn(0f, 1f))
}

/** One-shot enter progress (0→1) with delay/easing, like `animation: x .5s ease .1s both`. */
@Composable
internal fun enter(durMs: Int, delayMs: Int = 0, easing: Easing = EaseCss, key: Any? = Unit): Float {
    val reduced = LocalKxReducedMotion.current
    val a = remember(key) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(key) { if (!reduced) { a.snapTo(0f); delay(delayMs.toLong()); a.animateTo(1f, tween(durMs, easing = easing)) } }
    return a.value
}

/** kxIn / kxRise: fade + lift by [dy]. */
@Composable
internal fun Modifier.exIn(durMs: Int = 500, delayMs: Int = 0, dy: Dp = 18.dp, easing: Easing = EaseCss, key: Any? = Unit): Modifier {
    val p = enter(durMs, delayMs, easing, key)
    return this.graphicsLayer { alpha = p; translationY = (1f - p) * dy.toPx() }
}

/** kxPop: opacity 0 → 1, scale .6 → 1.08 (70%) → 1. */
@Composable
internal fun Modifier.exPop(durMs: Int = 450, delayMs: Int = 0, easing: Easing = EaseCss, key: Any? = Unit): Modifier {
    val p = enter(durMs, delayMs, easing, key)
    return this.graphicsLayer {
        alpha = p.coerceIn(0f, 1f)
        val s = if (p < .7f) .6f + (1.08f - .6f) * (p / .7f) else 1.08f - .08f * ((p - .7f) / .3f)
        scaleX = s; scaleY = s
    }
}

/** CSS linear-gradient(deg, stops). */
internal fun Modifier.lin(deg: Float, vararg stops: Pair<Float, Color>): Modifier {
    // Copy the vararg before capturing it (a captured vararg produced an illegal "<anonymous>" method in the class file).
    val st: List<Pair<Float, Color>> = stops.toList()
    return this.drawBehind { drawRect(Brush.linearGradient(colorStops = st.toTypedArray(), start = cssAngleStart(deg, size), end = cssAngleEnd(deg, size))) }
}
internal fun Modifier.lin2(deg: Float, a: Color, b: Color): Modifier = lin(deg, 0f to a, 1f to b)

/** CSS radial-gradient(Wx% Hy% at cx% cy%, stops…) — an ellipse sized relative to the box. */
internal fun Modifier.ellipseBg(wPct: Float, hPct: Float, cxPct: Float, cyPct: Float, vararg stops: Pair<Float, Color>): Modifier {
  val st: List<Pair<Float, Color>> = stops.toList()
  return this.drawBehind {
    val stops = st.toTypedArray()
    val rx = size.width * wPct; val ry = size.height * hPct
    val c = Offset(size.width * cxPct, size.height * cyPct)
    val k = ry / rx
    scale(1f, k, pivot = c) {
        drawRect(Brush.radialGradient(colorStops = stops, center = c, radius = rx), topLeft = Offset(0f, c.y - c.y / k), size = androidx.compose.ui.geometry.Size(size.width, size.height / k))
    }
  }
}

/** radial-gradient(circle, c, transparent 65%) filling the box. */
internal fun Modifier.circleGlow(c: Color, stop: Float = .65f): Modifier = this.drawBehind {
    val reach = size.minDimension / 2 * 1.41421f * stop
    drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, radius = reach), radius = size.minDimension / 2)
}

/** CSS box-shadow (outer, blurred) for a rounded rect of [radius]. */
internal fun Modifier.boxShadow(color: Color, blur: Dp, y: Dp = 0.dp, radius: Dp = 0.dp, spread: Dp = 0.dp): Modifier = this.drawBehind {
    val b = blur.toPx(); val s = spread.toPx(); val r = radius.toPx() + s
    drawIntoCanvas { cv ->
        val p = Paint().asFrameworkPaint().apply {
            isAntiAlias = true; this.color = color.toArgb()
            if (b > 0f) maskFilter = BlurMaskFilter(b / 2f, BlurMaskFilter.Blur.NORMAL)
        }
        cv.nativeCanvas.drawRoundRect(-s, -s + y.toPx(), size.width + s, size.height + s + y.toPx(), r, r, p)
    }
}

/** inset 0 Npx 0 rgba(255,255,255,a): a thin highlight inside the top edge (clip the parent to the shape). */
internal fun Modifier.insetTop(a: Float, px: Dp = 1.dp): Modifier = this.drawBehind {
    drawRect(Color.White.copy(alpha = a), size = androidx.compose.ui.geometry.Size(size.width, px.toPx()))
}

internal fun Modifier.wb(a: Float, r: Dp): Modifier = this.border(1.dp, Kx.w(a), RoundedCornerShape(r))

/** Round glass button of the boards (44, bg .06–.1, 1px border). */
@Composable
internal fun ExRoundBtn(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp, bg: Color = Kx.w(.06f),
                        border: Color? = Kx.w(.09f), tint: Color = Kx.text, iconSize: Dp = 24.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(size / 2)
    Box(modifier.size(size).clip(shape).background(bg).then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier).kxClick(label, onClick = onClick),
        contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }
}

/** Board header «← Title» (padding 12 12 0, 44 button, 20sp 800 title, gap 10). */
@Composable
internal fun ExHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, size: TextUnit = 20.sp, sub: String? = null,
                      icon: ImageVector = androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ArrowBack, label: String = "Назад",
                      actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ExRoundBtn(icon, label, onClick = onBack)
        Column(Modifier.weight(1f)) {
            KxLine(title, ts(size, FontWeight.ExtraBold), Modifier.kxHeading())
            if (sub != null) KxLine(sub, ts(12.sp, color = Kx.text4))
        }
        actions()
    }
}

/** Gradient CTA (linear-gradient(100deg,#6FC497,#3F9A6C), radius, ink #06150D) with an optional shine. */
@Composable
internal fun ExCta(text: String, modifier: Modifier = Modifier, height: Dp = 56.dp, radius: Dp = 18.dp, size: TextUnit = 16.sp, weight: FontWeight = FontWeight.ExtraBold,
                   enabled: Boolean = true, shine: Boolean = false, shadow: Boolean = true, icon: ImageVector? = null,
                   lead: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val sh = if (shine) kfRun(3200, 2, 1000) else 0f
    Box(modifier.fillMaxWidth().height(height)
        .then(if (shadow && enabled) Modifier.boxShadow(Color(0x593F9A6C), 28.dp, 10.dp, radius) else Modifier)
        .clip(RoundedCornerShape(radius))
        .then(if (enabled) Modifier.lin2(100f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.08f)))
        .kxClick(text, enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        if (shine) Box(Modifier.align(Alignment.CenterStart).width(60.dp).fillMaxHeight().graphicsLayer {
            // kxShine: translateX(-120% → 620%) during the first 60% of the cycle, skewX(-20deg) ≈ slight rotation.
            val p = (sh / .6f).coerceIn(0f, 1f); val e = EaseInOut.transform(p)
            translationX = (-1.2f + 7.4f * e) * this.size.width; alpha = if (sh < 0f) 0f else 1f; rotationZ = 0f
        }.background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .35f), Color.Transparent))))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = if (enabled) Kx.onAccent else Kx.text4)
            lead?.invoke()
            Text(text, style = ts(size, weight, if (enabled) Kx.onAccent else Kx.text4))
        }
    }
}

/** Full-screen success overlay (radial green, 96 tile with check pop, title, sub, buttons). */
@Composable
internal fun BoxScope.ExSuccess(title: String, sub: String, buttons: @Composable RowScope.() -> Unit) {
    val f = enter(400)
    Column(Modifier.matchParentSize().graphicsLayer { alpha = f }
        .ellipseBg(.7f, .45f, .5f, .45f, 0f to Color(0x403F9A6C), .7f to ExBg)
        .clickable(interactionSource = null, indication = null) {},
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).exPop(500, easing = EaseSpring).clip(RoundedCornerShape(30.dp)).lin2(140f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
            Icon(androidx.compose.material.icons.Icons.Rounded.Check, null, Modifier.size(52.dp), tint = Kx.onAccent)
        }
        Text(title, style = ts(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 20.dp), textAlign = TextAlign.Center)
        Text(sub, style = ts(14.sp, color = Kx.text3), modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
        Row(Modifier.padding(top = 30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), content = buttons)
    }
}

/** Small pill button (height, radius = h/2). */
@Composable
internal fun ExPill(text: String, modifier: Modifier = Modifier, height: Dp = 44.dp, bg: Color = Kx.w(.08f), ink: Color = Kx.text, size: TextUnit = 14.sp,
                    weight: FontWeight = FontWeight.Bold, padH: Dp = 18.dp, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(bg).kxClick(text, onClick = onClick).padding(horizontal = padH),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = ink)
        Text(text, style = ts(size, weight, ink), maxLines = 1)
    }
}

/**
 * Bottom sheet of the boards: scrim fade, slide up (kxSheet .34s cubic-bezier(.2,.9,.3,1)),
 * radius 30 top, solid [bg], 1px top line, grabber 36x4 #3A4540.
 */
@Composable
internal fun BoxScope.ExSheet(onClose: () -> Unit, bg: Color = Color(0xFF121614), scrim: Float = .5f, pad: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp),
                              grabber: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose) // fixer-final: Back closes the sheet, not the board
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(if (reduced) 1f else 0f) }
    val fade = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { launch { fade.animateTo(1f, tween(250)) }; p.animateTo(1f, tween(340, easing = EaseSheet)) }
    Box(Modifier.matchParentSize().graphicsLayer { alpha = fade.value }.background(Color.Black.copy(alpha = scrim)).kxClick("Отмена", onClick = onClose))
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = (1f - p.value) * size.height }
        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Color(0xFF101412)).background(bg)
        .drawBehind { drawLine(Kx.w(.08f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
        .clickable(interactionSource = null, indication = null) {}
        .navigationBarsPadding().padding(pad)) {
        if (grabber) { Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.dashed)); Spacer(Modifier.height(18.dp)) }
        content()
    }
}

/** Centered confirm dialog (r28, #1B211E). */
@Composable
internal fun BoxScope.ExDialog(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = enter(250, easing = EaseSpring)
    Box(Modifier.matchParentSize().graphicsLayer { alpha = p.coerceIn(0f, 1f) }.background(Color.Black.copy(alpha = .6f)).kxClick("Отмена", onClick = onClose))
    Column(Modifier.align(Alignment.Center).padding(horizontal = 28.dp).fillMaxWidth().graphicsLayer { alpha = p.coerceIn(0f, 1f); val s = .92f + .08f * p; scaleX = s; scaleY = s }
        .clip(RoundedCornerShape(28.dp)).background(Kx.surface2).border(1.dp, Kx.w(.08f), RoundedCornerShape(28.dp))
        .clickable(interactionSource = null, indication = null) {}.padding(22.dp), content = content)
}

@Suppress("unused") private val keepCorner = CornerRadius.Zero

/** Board-local toast (kxToast: fade/lift in 12%, hold to 85%, fade out), #232A26 pill with an icon. */
internal class ExToastState {
    var n by mutableIntStateOf(0)
    var text by mutableStateOf("")
    var icon by mutableStateOf(androidx.compose.material.icons.Icons.Rounded.CheckCircle)
    fun show(t: String, i: ImageVector = androidx.compose.material.icons.Icons.Rounded.CheckCircle) { text = t; icon = i; n++ }
}

@Composable
internal fun rememberExToast() = remember { ExToastState() }

@Composable
internal fun BoxScope.ExToast(state: ExToastState, bottom: Dp = 34.dp, durMs: Int = 2400) {
    if (state.n == 0) return
    key(state.n) {
        var alive by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) { delay(durMs.toLong()); alive = false }
        if (alive) ExToastBody(state, bottom, durMs)
    }
}

@Composable
private fun BoxScope.ExToastBody(state: ExToastState, bottom: Dp, durMs: Int) {
    run {
        val t = enter(durMs, easing = LinearEasing)
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom, start = 16.dp, end = 16.dp).graphicsLayer {
            alpha = when { t < .12f -> t / .12f; t < .85f -> 1f; else -> 1f - (t - .85f) / .15f }; translationY = if (t < .12f) (1f - t / .12f) * 10.dp.toPx() else 0f
        }.clip(RoundedCornerShape(18.dp)).background(Kx.surface3).border(1.dp, Kx.w(.16f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(state.icon, null, Modifier.size(20.dp), tint = Kx.accent)
            Text(state.text, style = ts(14.sp, FontWeight.SemiBold), maxLines = 1)
        }
    }
}

/** The gradient sheet variant: rgba(40,48,44,.96) → rgba(18,22,20,.98), top line .16, grabber 40x4 .25, kxSheet .3s rise. */
@Composable
internal fun BoxScope.ExGlassSheet(onClose: () -> Unit, pad: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp),
                                   grabGap: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose) // fixer-final: Back closes the sheet, not the board
    val p = enter(300, easing = KxMotion.rise)
    val f = enter(200)
    Box(Modifier.matchParentSize().graphicsLayer { alpha = f }.background(Color.Black.copy(alpha = .55f)).kxClick("Закрыть", onClick = onClose))
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = (1f - p) * size.height }
        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Kx.bg)
        .background(Brush.verticalGradient(listOf(Color(0xF528302C), Color(0xFA121614)))).insetTop(.16f)
        .clickable(interactionSource = null, indication = null) {}
        .imePadding().navigationBarsPadding().padding(pad)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
        Spacer(Modifier.height(grabGap))
        content()
    }
}

/** Opens the system «Поделиться» sheet with [text]. */
internal fun exShare(context: android.content.Context, text: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
    runCatching { context.startActivity(android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Standard sub-screen bar of these boards: padding 10 12 6, gap 8, 44 glass back button, 20sp 800 title. */
@Composable
internal fun ExBar(title: String, onBack: () -> Unit, icon: ImageVector = androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ArrowBack,
                   label: String = "Назад", actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExRoundBtn(icon, label, onClick = onBack)
        Text(title, style = ts(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f), maxLines = 1)
        actions()
    }
}

/** Opens an Android intent, ignoring «no app» failures. */
internal fun exStart(context: android.content.Context, intent: android.content.Intent) {
    runCatching { context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Bottom CTA area with the fade (linear-gradient(180deg, transparent, bg 40%)), padding 14 16 22. */
@Composable
internal fun BoxScope.ExBottom(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Kx.bg.copy(alpha = 0f), .4f to Kx.bg))
        .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), content = content)
}

/** Pill CTA 54 (r27): gradient 135deg when [on], quiet .07 with #5C6661 ink when off. */
@Composable
internal fun ExPillCta(text: String, on: Boolean, icon: ImageVector?, modifier: Modifier = Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val a by animateFloatAsState(if (on) 1f else 0f, tween(250), label = "cta")
    val ink = if (on) (if (danger) Color.White else Kx.onAccent) else Color(0xFF5C6661)
    Row(modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.w(.07f)).drawBehind {
        if (danger) drawRect(Kx.dangerSolid, alpha = a)
        else drawRect(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep), cssAngleStart(135f, size), cssAngleEnd(135f, size)), alpha = a)
    }.kxClick(text, onClick = onClick), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, null, Modifier.size(24.dp), tint = ink)
        Text(text, style = ts(16.sp, FontWeight.ExtraBold, ink), maxLines = 1)
    }
}

/** Danger confirm of the boards: scrim .6, card left/right 24, padding 20, r28, #232A26 → #161B18, border .14; Отмена + red action. */
@Composable
internal fun BoxScope.ExConfirm(title: String, text: String, yes: String, onNo: () -> Unit, onYes: () -> Unit) {
    val p = enter(220, easing = EaseCss)
    Box(Modifier.matchParentSize().graphicsLayer { alpha = p }.background(Color.Black.copy(alpha = .6f)).kxClick("Отмена", onClick = onNo))
    Column(Modifier.align(Alignment.Center).padding(horizontal = 24.dp).fillMaxWidth().graphicsLayer { alpha = p; val s = .95f + .05f * p; scaleX = s; scaleY = s }
        .clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Kx.surface3, Kx.sheetBottom))).border(1.dp, Kx.w(.14f), RoundedCornerShape(28.dp))
        .clickable(interactionSource = null, indication = null) {}.padding(20.dp)) {
        Text(title, style = ts(18.sp, FontWeight.ExtraBold))
        Text(text, style = ts(14.sp, color = Kx.text2, lh = 20.sp), modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.08f)).kxClick("Отмена", onClick = onNo), contentAlignment = Alignment.Center) {
                Text("Отмена", style = ts(15.sp, FontWeight.Bold))
            }
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.dangerSolid).kxClick(yes, onClick = onYes), contentAlignment = Alignment.Center) {
                Text(yes, style = ts(15.sp, FontWeight.ExtraBold, Color.White))
            }
        }
    }
}
