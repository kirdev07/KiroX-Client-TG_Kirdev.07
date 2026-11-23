package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared pieces of the tools boards (they repeat these 1:1). */

internal val TTile = Color.White.copy(alpha = .045f)
internal val TSpring = CubicBezierEasing(.34f, 1.5f, .5f, 1f)
internal val TSheetEase = CubicBezierEasing(.2f, .9f, .25f, 1f)
internal val TEase = CubicBezierEasing(.25f, .1f, .25f, 1f)
internal val TEaseInOut = CubicBezierEasing(.42f, 0f, .58f, 1f)

/** Board root: header (44 glass back, 22/800 title, padding 10 12 [headerBottom]) + scroll column padding 4 16 30. */
@Composable
internal fun TScreen(title: String, onBack: () -> Unit, headerBottom: Dp = 6.dp, scroll: Boolean = true, sub: String? = null, titleSize: Int = if (sub != null) 20 else 22,
                     bottomPad: Dp = 30.dp, bg: Color = Kx.bg, above: @Composable ColumnScope.() -> Unit = {},
                     actions: @Composable RowScope.() -> Unit = {}, overlay: @Composable BoxScope.() -> Unit = {},
                     content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {
            THeader(title, onBack, headerBottom, sub, titleSize, actions)
            above()
            Column(Modifier.fillMaxWidth().weight(1f).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPad), content = content)
        }
        overlay()
    }
}

@Composable
internal fun THeader(title: String, onBack: () -> Unit, bottom: Dp = 6.dp, sub: String? = null, titleSize: Int = if (sub != null) 20 else 22,
                     actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = bottom),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(title, style = KxType.style(titleSize.sp, FontWeight.ExtraBold), maxLines = 1, modifier = Modifier.kxHeading())
            if (sub != null) Text(sub, style = KxType.style(12.sp, color = Kx.text3), maxLines = 1)
        }
        actions()
    }
}

/** Accent section caption: 13/800 #8FD3AC, padding 16 4 6. */
@Composable
internal fun TCap(text: String, modifier: Modifier = Modifier, top: Dp = 16.dp, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = top, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.weight(1f).kxHeading())
        if (trailing != null) Text(trailing, style = KxType.style(12.sp, color = Kx.text4))
    }
}

/** Grey hint under a group: 12sp #8A958F, line 17. */
@Composable
internal fun THint(text: String, modifier: Modifier = Modifier, top: Dp = 8.dp) =
    Text(text, style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp), modifier = modifier.padding(start = 6.dp, end = 6.dp, top = top))

/** r22 tile group with w.045 fill. */
@Composable
internal fun TGroup(modifier: Modifier = Modifier, radius: Dp = 22.dp, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(TTile), content = content)

/** 1px w.06 line on top of rows 2..n. */
internal fun Modifier.topLine(show: Boolean, a: Float = .06f): Modifier =
    if (!show) this else this.drawBehind { drawLine(Kx.w(a), Offset(0f, .5f), Offset(size.width, .5f), 1f) }

/** 22dp radio: 2dp ring (#8FD3AC on / #3A4540 off), 10dp dot. */
@Composable
internal fun TRadio(on: Boolean, size: Dp = 22.dp, dot: Dp = 10.dp, off: Color = Kx.dashed) {
    val d by animateColorAsState(if (on) Kx.accent else Color.Transparent, tween(200), label = "dot")
    Box(Modifier.kxSelectedState(on).size(size).border(2.dp, if (on) Kx.accent else off, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(dot).clip(CircleShape).background(d))
    }
}

/** White-knob switch: [w]x[h], knob [knob], spring travel .35s, track colour .3s. */
@Composable
internal fun TSwitch(on: Boolean, trackOn: Color = Kx.accent, trackOff: Color = Kx.badgeMuted, w: Dp = 50.dp, h: Dp = 30.dp, knob: Dp = 24.dp,
                     knobOn: Color = Color.White, knobOff: Color = Color.White, shadow: Boolean = true) {
    val pad = (h - knob) / 2
    val reduced = LocalKxReducedMotion.current
    val x by animateDpAsState(if (on) w - knob - pad * 2 else 0.dp, if (reduced) tween(0) else tween(350, easing = TSpring), label = "knob")
    val track by animateColorAsState(if (on) trackOn else trackOff, tween(if (reduced) 0 else 300), label = "track")
    Box(Modifier.kxToggleState(on).size(w, h).clip(RoundedCornerShape(h / 2)).background(track)) {
        Box(Modifier.offset(pad + x, pad).size(knob).then(if (shadow) Modifier.shadow(3.dp, CircleShape) else Modifier)
            .clip(CircleShape).background(if (on) knobOn else knobOff))
    }
}

/** Dark-knob switch of the newer boards: 50x30, knob 24 #06150D on / #8A958F off, track accent / w.12. */
@Composable
internal fun TDarkSwitch(on: Boolean, trackOn: Color = Kx.accent) =
    TSwitch(on, trackOn, Kx.w(.12f), knobOn = Kx.onAccent, knobOff = Kx.text4, shadow = false)

/** Icon + title/sub + trailing row (padding 13 16, gap 12). */
@Composable
internal fun TRow(icon: ImageVector?, title: String, sub: String? = null, modifier: Modifier = Modifier, iconTint: Color = Kx.accent,
                  subColor: Color = Kx.text3, titleColor: Color = Kx.text, vPad: Dp = 13.dp, onClick: (() -> Unit)? = null,
                  leading: (@Composable () -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().then(if (onClick != null) Modifier.kxClick(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = vPad),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (leading != null) leading() else if (icon != null) Icon(icon, null, Modifier.size(24.dp), tint = iconTint)
        Column(Modifier.weight(1f)) {
            Text(title, style = KxType.style(15.sp, FontWeight.SemiBold, titleColor))
            if (sub != null) Text(sub, style = KxType.style(12.sp, color = subColor, lineHeight = 16.sp), modifier = Modifier.padding(top = 2.dp))
        }
        trailing()
    }
}

/** 36 r12 tinted icon tile. */
@Composable
internal fun TIconTile(icon: ImageVector, tint: Color = Kx.accent, bg: Color = tint.copy(alpha = .1f), size: Dp = 36.dp, radius: Dp = 12.dp, iconSize: Dp = 20.dp) =
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }

/** Range slider: 28 tall, 8 track, 26 thumb #EEF3EF; [track] draws the full track (default fill colour to value, rest w.10). */
@Composable
internal fun TSlider(value: Float, min: Float, max: Float, label: String, color: Color = Kx.accent, step: Float = 1f,
                     track: Brush? = null, onChange: (Float) -> Unit) {
    var w by remember { mutableFloatStateOf(1f) }
    val thumb = with(LocalDensity.current) { 26.dp.toPx() }
    fun pick(x: Float) {
        val f = ((x - thumb / 2) / (w - thumb)).coerceIn(0f, 1f)
        val raw = min + f * (max - min)
        onChange((Math.round(raw / step) * step).coerceIn(min, max))
    }
    val frac = ((value - min) / (max - min)).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(28.dp).semantics { contentDescription = label }.onSizeChanged { w = it.width.toFloat() }
        .pointerInput(min, max, step) { detectTapGestures { pick(it.x) } }
        .pointerInput(min, max, step) { detectHorizontalDragGestures { ch, _ -> pick(ch.position.x) } }
        .drawBehind {
            val ty = size.height / 2 - 4.dp.toPx(); val r = CornerRadius(4.dp.toPx())
            val cx = thumb / 2 + frac * (size.width - thumb)
            if (track != null) drawRoundRect(track, Offset(0f, ty), Size(size.width, 8.dp.toPx()), r)
            else { drawRoundRect(Kx.w(.1f), Offset(0f, ty), Size(size.width, 8.dp.toPx()), r); drawRoundRect(color, Offset(0f, ty), Size(cx, 8.dp.toPx()), r) }
            drawCircle(Color.Black.copy(alpha = .35f), thumb / 2 + 2.dp.toPx(), Offset(cx, size.height / 2 + 2.dp.toPx()))
            drawCircle(Kx.text, thumb / 2, Offset(cx, size.height / 2))
        })
}

/** CSS linear-gradient(deg, stops) drawn behind. */
internal fun Modifier.tGrad(deg: Float, vararg stops: Pair<Float, Color>): Modifier = this.drawBehind {
    drawRect(Brush.linearGradient(colorStops = stops, start = cssAngleStart(deg, size), end = cssAngleEnd(deg, size)))
}

/** kxRise: fade + [dy] lift. */
@Composable
internal fun Modifier.tRise(delayMs: Int = 0, durMs: Int = 450, dy: Dp = 10.dp): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(durMs, easing = TEase)) }
    return this.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * dy.toPx() }
}

/** Decorative keyframe run N times (0→1 each), stays at [rest] afterwards. */
@Composable
internal fun tCycles(durMs: Int, times: Int = 2, delayMs: Int = 0, rest: Float = 0f, easing: Easing = LinearEasing, key: Any = Unit): Float {
    val reduced = LocalKxReducedMotion.current
    val a = remember(key) { Animatable(rest) }
    LaunchedEffect(key, reduced) {
        if (reduced) return@LaunchedEffect
        delay(delayMs.toLong())
        repeat(times) { a.snapTo(0f); a.animateTo(1f, tween(durMs, easing = easing)) }
        a.snapTo(rest)
    }
    return a.value
}

/** Bottom sheet: backdrop .55, r30 top, solid #1B211E, grabber 40x4, slide .38s. */
@Composable
internal fun BoxScope.TSheet(onClose: () -> Unit, label: String? = null, bg: Brush = TSheetBg, grabberGap: Dp = 16.dp, bottomPad: Dp = 22.dp, content: @Composable ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose) // fixer-final: Back closes the sheet, not the board
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(if (reduced) 1f else 0f) }
    val fade = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { launch { fade.animateTo(1f, tween(200)) }; p.animateTo(1f, tween(380, easing = TSheetEase)) }
    Box(Modifier.matchParentSize().graphicsLayer { alpha = fade.value }.background(Color.Black.copy(alpha = .55f)).kxClick("Закрыть", onClick = onClose))
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = (1f - p.value) * size.height }
        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(bg)
        .drawBehind { drawLine(Kx.w(.16f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
        .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
        .clickable(interactionSource = null, indication = null) {}
        .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = bottomPad)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
        Spacer(Modifier.height(grabberGap))
        content()
    }
}

/** linear-gradient(180deg, rgba(40,48,44,.96), rgba(18,22,20,.98)) of the confirm sheets. */
internal val TSheetBg = Brush.verticalGradient(listOf(Color(40, 48, 44).copy(alpha = .96f), Color(18, 22, 20).copy(alpha = .98f)))

/** Danger confirm sheet: 56 red icon disc, 19/800 title, 14 text, Отмена / red action (50 tall). */
@Composable
internal fun BoxScope.TConfirmSheet(icon: ImageVector, title: String, text: String, yes: String, onClose: () -> Unit, danger: Boolean = true, onYes: () -> Unit) {
    TSheet(onClose, title) {
        val c = if (danger) Kx.danger else Kx.accent
        Box(Modifier.size(56.dp).clip(CircleShape).background(if (danger) Kx.dangerSolid.copy(alpha = .14f) else Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(30.dp), tint = c)
        }
        Text(title, style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
        Text(text, style = KxType.style(14.sp, color = Kx.text3, lineHeight = 20.sp), modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TPill("Отмена", Modifier.weight(1f), h = 50.dp, bg = solid(Kx.w(.08f)), ink = Kx.text, weight = FontWeight.Bold, onClick = onClose)
            TPill(yes, Modifier.weight(1f), h = 50.dp, bg = if (danger) solid(Kx.dangerSolid) else Kx.buttonGradient, ink = if (danger) Color.White else Kx.onAccent) { onClose(); onYes() }
        }
    }
}

/** Small round 32dp w.07 button (steppers). */
@Composable
internal fun TRoundBtn(icon: ImageVector, label: String, size: Dp = 32.dp, iconSize: Dp = 18.dp, bg: Color = Kx.w(.07f), tint: Color = Kx.text, onClick: () -> Unit) =
    Box(Modifier.size(size).clip(CircleShape).background(bg).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(iconSize), tint = tint)
    }

/** Pill button with optional icon (height [h], radius h/2). */
@Composable
internal fun TPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, h: Dp = 52.dp, bg: Brush = Kx.buttonGradient,
                   ink: Color = Kx.onAccent, size: Int = 15, weight: FontWeight = FontWeight.ExtraBold, border: Color? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(h / 2)
    Row(modifier.height(h).clip(shape).background(bg).then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
        .kxClick(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp), tint = ink); Spacer(Modifier.width(8.dp)) }
        Text(text, style = KxType.style(size.sp, weight, ink), maxLines = 1)
    }
}

internal fun solid(c: Color): Brush = Brush.linearGradient(listOf(c, c))

/** Russian plural: 1 чат / 2 чата / 5 чатов. */
internal fun tPlural(n: Int, one: String, few: String, many: String): String {
    val m = kotlin.math.abs(n) % 100; val d = m % 10
    return if (m in 11..19) many else if (d == 1) one else if (d in 2..4) few else many
}

internal fun groupShape(i: Int, n: Int, big: Dp = 22.dp, small: Dp = 6.dp): Shape = when {
    n == 1 -> RoundedCornerShape(big)
    i == 0 -> RoundedCornerShape(big, big, small, small)
    i == n - 1 -> RoundedCornerShape(small, small, big, big)
    else -> RoundedCornerShape(small)
}

/** Board-local toast (kxToast 2.4s: rise in 12%, hold, fade from 85%): #232A26, r18, line .16, icon 20 accent. */
class TToast {
    var text by mutableStateOf("")
    var icon by mutableStateOf<ImageVector?>(null)
    var id by mutableLongStateOf(0L)
    var tint by mutableStateOf(Kx.accent)
    var action by mutableStateOf<String?>(null)
    var onAction: () -> Unit = {}
    fun show(t: String, ic: ImageVector? = null, c: Color = Kx.accent, act: String? = null, onAct: () -> Unit = {}) { text = t; icon = ic; tint = c; action = act; onAction = onAct; id = System.nanoTime() }
    fun hide() { id = 0L }
}

@Composable
internal fun BoxScope.TToastHost(t: TToast, bottom: Dp = 96.dp, durMs: Int = 2400) {
    val p = remember { Animatable(1f) }
    LaunchedEffect(t.id) { if (t.id != 0L) { p.snapTo(0f); p.animateTo(1f, tween(durMs, easing = LinearEasing)) } }
    if (t.id == 0L || p.value >= 1f) return
    val v = p.value
    val a = if (v < .12f) v / .12f else if (v < .85f) 1f else (1f - v) / .15f
    val dy = if (v < .12f) (1f - v / .12f) * 10f else 0f
    Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom).graphicsLayer { alpha = a; translationY = dy * density }
        .clip(RoundedCornerShape(18.dp)).background(Kx.surface3).border(1.dp, Kx.w(.16f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        t.icon?.let { Icon(it, null, Modifier.size(20.dp), tint = t.tint) }
        Text(t.text, style = KxType.style(14.sp, FontWeight.SemiBold), maxLines = 1)
        t.action?.let { a -> Text(a, style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 6.dp).kxClick { t.hide(); t.onAction() }) }
    }
}

/** Bottom save bar: fades from transparent to #0A0C0B at 40%, padding 14 16 22, 54 gradient button with glow. */
@Composable
internal fun BoxScope.TSaveBar(text: String, icon: ImageVector?, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Kx.bg.copy(alpha = 0f), .4f to Kx.bg))
        .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
        TPill(text, Modifier.fillMaxWidth().shadow(if (enabled) 14.dp else 0.dp, RoundedCornerShape(27.dp), spotColor = Kx.accentDeep, ambientColor = Kx.accentDeep),
            icon, 54.dp, if (enabled) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.fillHi), if (enabled) Kx.onAccent else Kx.text4, 16, enabled = enabled, onClick = onClick)
    }
}

/** Scope / period chip: 34 tall r17, accent .14 + accent ink when on, w.05 + #C9D2CC off. */
@Composable
internal fun TChip(label: String, on: Boolean, modifier: Modifier = Modifier, h: Dp = 34.dp, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "chip")
    Box(modifier.height(h).clip(RoundedCornerShape(h / 2)).background(bg).kxClick(onClick = onClick).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, style = KxType.style(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2), maxLines = 1)
    }
}

/** Linear keyframe lookup: [k] = stops 0..1, [v] = values. */
internal fun kf(p: Float, k: FloatArray, v: FloatArray, e: Easing? = null): Float {
    if (p <= k[0]) return v[0]
    for (i in 1 until k.size) if (p <= k[i]) { val d = k[i] - k[i - 1]; if (d <= 0f) return v[i]
        val f = (p - k[i - 1]) / d; return v[i - 1] + (v[i] - v[i - 1]) * (e?.transform(f) ?: f) }
    return v.last()
}
internal fun fa(vararg f: Float) = f

/** Shake keyframes (0/100 → 0, 20/60 → -6, 40/80 → +6) in dp. */
internal fun shakeX(p: Float, amp: Float = 6f): Float =
    if (p <= 0f || p >= 1f) 0f else kf(p, fa(0f, .2f, .4f, .6f, .8f, 1f), fa(0f, -amp, amp, -amp, amp, 0f))
