package com.kirox.app.ui.v3.screens.groups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import com.kirox.app.ui.v3.screens.media.mSheetDrag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

/** Shared bits of the groups section (Group, Channel, admin boards). */

/** CSS linear-gradient(deg, colors…) filling the (already clipped) box. */
fun Modifier.cssLinear(deg: Float, from: Color, to: Color): Modifier = drawBehind {
    drawRect(Brush.linearGradient(listOf(from, to), cssAngleStart(deg, size), cssAngleEnd(deg, size)))
}

/** CSS linear-gradient with explicit stops (0..1). */
fun Modifier.cssLinearStops(deg: Float, vararg stops: Pair<Float, Color>): Modifier = drawBehind {
    drawRect(Brush.linearGradient(*stops, start = cssAngleStart(deg, size), end = cssAngleEnd(deg, size)))
}

/** CSS radial-gradient(W% H% at X% Y%, c, transparent) — elliptical glow over the whole box. */
fun Modifier.cssRadial(wFrac: Float, hFrac: Float, xFrac: Float, yFrac: Float, color: Color): Modifier = drawBehind {
    val rx = size.width * wFrac; val ry = size.height * hFrac
    if (rx <= 0f || ry <= 0f) return@drawBehind
    val sy = ry / rx
    val c = Offset(size.width * xFrac, size.height * yFrac)
    scale(1f, sy, c) { drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), c, rx), rx * 2f, c) }
}

/** 1px hairline at the bottom (or top) of a row. */
fun Modifier.hairline(color: Color = Kx.w(.06f), top: Boolean = false): Modifier = drawBehind {
    val y = if (top) .5f else size.height - .5f
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
}

/** Mockup kxRise: fade + [dy] lift, [durMs] ease, after [delayMs]. Runs once. */
@Composable
fun Modifier.gxRise(delayMs: Int = 0, durMs: Int = 400, dy: Dp = 10.dp, easing: Easing = FastOutSlowInEasing): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(durMs, easing = easing)) }
    return graphicsLayer { alpha = p.value; translationY = (1f - p.value) * dy.toPx() }
}

/** kxPop on appear: 0 → 1.04 → 1 with fade (scale from [from]). */
@Composable
fun Modifier.gxPopIn(durMs: Int = 500, from: Float = .7f, delayMs: Int = 0): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(durMs, easing = KxMotion.rise)) }
    return graphicsLayer {
        val v = p.value
        val s = if (v < .7f) from + (1.04f - from) * (v / .7f) else 1.04f - .04f * ((v - .7f) / .3f)
        scaleX = s; scaleY = s; alpha = (v / .7f).coerceAtMost(1f)
    }
}

/** kxGrow: scaleX 0 → 1 from the left (progress bars). */
@Composable
fun Modifier.gxGrowX(durMs: Int = 800, delayMs: Int = 200): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(durMs, easing = KxMotion.rise)) }
    return graphicsLayer { scaleX = p.value; transformOrigin = TransformOrigin(0f, .5f) }
}

/** kxBar: scaleY 0 → 1 from the bottom (chart bars). */
@Composable
fun Modifier.gxGrowY(durMs: Int = 600, delayMs: Int = 0): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(durMs, easing = KxMotion.rise)) }
    return graphicsLayer { scaleY = p.value; transformOrigin = TransformOrigin(.5f, 1f) }
}

/** Tap without ripple (scrims). */
fun Modifier.gxPlainClick(onClick: () -> Unit): Modifier = clickable(MutableInteractionSource(), null, onClick = onClick)

/** Round glass button of the hero headers (Group, Profile): gradient .14→.05, 1px .16 border. */
@Composable
fun GxGlassButton(icon: ImageVector, label: String, size: Dp = 44.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0x66161B18)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
        .border(1.dp, Kx.w(.16f), CircleShape).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(24.dp), tint = Kx.text)
    }
}

/** Sub-screen root used by most admin boards: black, top-right glow (380 at -90/-110), header + content. */
@Composable
fun GxSubScreen(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, glowAlpha: Float = .18f,
                actions: @Composable RowScope.() -> Unit = {}, overlay: @Composable BoxScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Kx.bg)) {
        KxGlow(diameter = 380.dp, x = (-90).dp, y = (-110).dp, alpha = glowAlpha, alignEnd = true)
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            KxTopBar(title, onBack = onBack, actions = actions)
            content()
        }
        overlay()
    }
}

/** Dim layer that fades in; tap closes. */
@Composable
fun GxScrim(visible: Boolean, alpha: Float = .6f, onClick: (() -> Unit)? = null) {
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)).then(if (onClick != null) Modifier.gxPlainClick(onClick) else Modifier.gxPlainClick {}))
    }
}

/**
 * Generic confirm dialog of the boards: left/right 24, padding 20, radius 28, gradient #232A26→#161B18,
 * 1px .14 border; title 18/800, text 14 #C9D2CC lh 20; two 48dp buttons (quiet / red #E5534B).
 */
@Composable
fun BoxScope.GxConfirm(visible: Boolean, title: String, text: String, confirm: String, cancel: String = "Отмена",
                       danger: Boolean = true, onCancel: () -> Unit, onConfirm: () -> Unit) {
    if (visible) BackHandler(onBack = onCancel)
    GxScrim(visible, .6f, onCancel)
    AnimatedVisibility(visible, Modifier.align(Alignment.Center), enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
        Column(Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(RoundedCornerShape(KxShape.dialog)).background(Kx.sheetGradient)
            .border(1.dp, Kx.w(.14f), RoundedCornerShape(KxShape.dialog)).gxPlainClick {}.padding(20.dp)) {
            Text(title, style = KxType.style(18.sp, FontWeight.ExtraBold))
            Text(text, style = KxType.style(14.sp, color = Kx.text2, lineHeight = 20.sp), modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GxPillButton(cancel, Modifier.weight(1f), bg = Kx.w(.08f), ink = Kx.text, weight = FontWeight.Bold, onClick = onCancel)
                GxPillButton(confirm, Modifier.weight(1f), bg = if (danger) Kx.dangerSolid else Kx.accent, ink = if (danger) Color.White else Kx.onAccent,
                    weight = FontWeight.ExtraBold, onClick = onConfirm)
            }
        }
    }
}

@Composable
fun GxPillButton(text: String, modifier: Modifier = Modifier, height: Dp = 48.dp, bg: Color = Kx.w(.08f), brush: Brush? = null, ink: Color = Kx.text,
                 size: Int = 15, weight: FontWeight = FontWeight.Bold, icon: ImageVector? = null, enabled: Boolean = true,
                 iconSize: Dp = 22.dp, gap: Dp = 8.dp, hPad: Dp = 12.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(height / 2)
    Row(modifier.height(height).clip(shape).then(if (brush != null) Modifier.background(brush) else Modifier.background(bg))
        .kxClick(enabled = enabled, onClick = onClick).padding(horizontal = hPad),
        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, null, Modifier.size(iconSize), tint = ink)
        // critique-C P1: the label never clips mid-word — it shrinks to 13sp first, then ends with «…».
        val fit = remember(text) { mutableStateOf(size.toFloat()) }
        Text(text, style = KxType.style(fit.value.sp, weight, ink), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false,
            modifier = Modifier.weight(1f, fill = false),
            onTextLayout = { r -> if (r.hasVisualOverflow && fit.value > 13f) fit.value = fit.value - 1f })
    }
}

/** Bottom sheet of the boards: radius 30 top, solid gradient, 40x4 handle, padding 10/16/24. */
@Composable
fun BoxScope.GxSheet(visible: Boolean, onClose: () -> Unit, scrimAlpha: Float = .6f, bottomPad: Dp = 24.dp, handleGap: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    if (visible) BackHandler(onBack = onClose)
    GxScrim(visible, scrimAlpha, onClose)
    AnimatedVisibility(visible, Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(tween(350, easing = CubicBezierEasing(.2f, .9f, .25f, 1f))) { it },
        exit = slideOutVertically(tween(250)) { it }) {
        Column(Modifier.mSheetDrag(onClose).fillMaxWidth().clip(RoundedCornerShape(topStart = KxShape.sheet, topEnd = KxShape.sheet))
            .background(Brush.verticalGradient(listOf(Color(0xFF222C27), Color(0xFF101412)))).hairline(Kx.w(.16f), top = true)
            .gxPlainClick {}.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = bottomPad)) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = handleGap).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
            content()
        }
    }
}

/** Board-local status tip (info icon, 18 radius, bottom 96, 2.2s by default; boards pass their own look). */
class GxTipState {
    var text by mutableStateOf(""); var icon by mutableStateOf<ImageVector?>(null); var tint by mutableStateOf<Color?>(null); var id by mutableLongStateOf(0L)
    /** critique-C P1: optional «Отменить» on the tip (bulk actions, removals); the tip then stays 5 s. */
    var action by mutableStateOf<String?>(null); var onAction by mutableStateOf<(() -> Unit)?>(null)
    fun say(t: String, i: ImageVector? = null, c: Color? = null, action: String? = null, onAction: (() -> Unit)? = null) {
        text = t; icon = i; tint = c; this.action = action; this.onAction = onAction; id = System.nanoTime()
    }
}

@Composable fun rememberGxTip() = remember { GxTipState() }

@Composable
fun BoxScope.GxTip(state: GxTipState, bottom: Dp = 96.dp, icon: ImageVector = Icons.Outlined.Info, durationMs: Long = 2200,
                   bg: Color = Color(0xF828322D), border: Color = Kx.w(.12f), textSize: Int = 13, iconSize: Dp = 18.dp,
                   padH: Dp = 16.dp, padV: Dp = 10.dp) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(state.id) { if (state.id != 0L) { shown = true; delay(if (state.action != null) maxOf(durationMs, 5000L) else durationMs); shown = false } }
    AnimatedVisibility(shown, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom),
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 3 }, exit = fadeOut(tween(300))) {
        Row(Modifier.clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(horizontal = padH, vertical = padV), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(state.icon ?: icon, null, Modifier.size(iconSize), tint = state.tint ?: Kx.accent)
            Text(state.text, style = KxType.style(textSize.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            val act = state.action
            if (act != null) Text(act, style = KxType.style(textSize.sp, FontWeight.ExtraBold, Kx.accent),
                modifier = Modifier.kxHitClick(act, RoundedCornerShape(12.dp)) { state.onAction?.invoke(); shown = false }.padding(horizontal = 6.dp, vertical = 4.dp))
        }
    }
}

/** Letter avatar from explicit gradient colors (members lists of the boards). */
@Composable
fun GxLetter(letter: String, from: Color, to: Color, ink: Color, size: Dp = 40.dp, fontSize: Int = 15, shape: Shape = CircleShape,
             online: Boolean = false, ring: Color = Kx.bg, icon: ImageVector? = null, iconSize: Dp = 20.dp, deg: Float = 140f) {
    Box(Modifier.size(size)) {
        Box(Modifier.matchParentSize().clip(shape).cssLinear(deg, from, to), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, null, Modifier.size(iconSize), tint = ink)
            else Text(letter, style = KxType.style(fontSize.sp, FontWeight.Bold, ink))
        }
        if (online) Box(Modifier.align(Alignment.BottomEnd).offset(1.dp, 1.dp).size(12.dp).clip(CircleShape).background(ring).padding(2.dp).clip(CircleShape).background(Kx.accent))
    }
}

/** Demo people used across the admin boards (same letters/colors as the mockups). */
data class GxPerson(val name: String, val letter: String, val from: Color, val to: Color, val ink: Color)

object GxPeople {
    val me = GxPerson("Вы", "В", Color(0xFF3E7A5A), Color(0xFF173A28), Color(0xFFDDF5E5))
    val maxim = GxPerson("Максим Ли", "М", Color(0xFF2D4E73), Color(0xFF1A2A40), Color(0xFFC6DBF5))
    val anna = GxPerson("Анна Ким", "А", Color(0xFF4B3F7A), Color(0xFF2A2240), Color(0xFFDCD0FF))
    val ilya = GxPerson("Илья Петров", "И", Color(0xFFD08A3C), Color(0xFF7A4A16), Color(0xFFFFE9C9))
}

/** Russian plural: 1 участник, 2 участника, 5 участников. */
fun gxPlural(n: Int, one: String, few: String, many: String): String {
    val m = kotlin.math.abs(n) % 100; val d = m % 10
    return if (m in 11..19) many else if (d == 1) one else if (d in 2..4) few else many
}

/** 24500 → «24 500» (thin grouping like the mockups). */
fun gxNum(n: Int): String = n.toString().reversed().chunked(3).joinToString(" ").reversed()

// ---- Store wiring helpers (boards open with "<Board>:<chatId>"; without an id they keep the board's demo data) ----

/** Chat id from open("Board:<chatId>") (also "<chatId>:<more>"); null = the board's own demo. */
fun gxChatId(arg: String?): Long? = arg?.substringBefore(':')?.toLongOrNull()

/** Second part of "chatId:x" (user id, message id…). */
fun gxArg2(arg: String?): Long? = arg?.substringAfter(':', "")?.toLongOrNull()

/** Loads once per [keys] (bump a key to reload); null while loading or when [load] returns null. */
@Composable
fun <T> gxLoad(vararg keys: Any?, load: suspend () -> T?): T? = produceState<T?>(null, *keys) { value = load() }.value

/** A real person (name + id) in the demo look: same four gradients as the mockups, picked by id. */
fun gxPerson(name: String, id: Long): GxPerson {
    val looks = listOf(GxPeople.maxim, GxPeople.anna, GxPeople.ilya, GxPeople.me)
    val l = looks[((id % looks.size + looks.size) % looks.size).toInt()]
    return l.copy(name = name, letter = name.trim().take(1).uppercase().ifEmpty { "?" })
}

/** Stats value as the boards write it: «21 410», «4,5». */
fun gxStatValue(v: Double): String = if (v == Math.floor(v) && kotlin.math.abs(v) < 1e12) gxNum(v.toLong().toInt()) else String.format(java.util.Locale.US, "%.1f", v).replace('.', ',')

/** «+12%» / «−3%» against the previous period; empty when there is nothing to compare with. */
fun gxDelta(cur: Double, prev: Double): String {
    if (prev == 0.0) return ""
    val p = ((cur - prev) / kotlin.math.abs(prev) * 100).let { kotlin.math.round(it).toInt() }
    return if (p >= 0) "+$p%" else "−${-p}%"
}
