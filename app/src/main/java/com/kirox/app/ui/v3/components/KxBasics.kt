package com.kirox.app.ui.v3.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.layout.layout
import androidx.compose.ui.composed
import androidx.compose.foundation.indication
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.theme.*

/** Click with a soft ripple; every tappable thing in v3 keeps at least a 44dp target. */
fun Modifier.kxClick(label: String? = null, role: Role = Role.Button, enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
        .clickable(
            interactionSource = null,
            indication = ripple(color = Kx.accent),
            enabled = enabled, role = role, onClick = onClick
        )

/**
 * a11y (P1-5): a click whose touch and TalkBack box is at least [min] (48dp) while the drawn element
 * keeps its size and the parent still lays it out at the drawn size — nothing visible changes.
 * Put it FIRST in the chain, before the element's own size/clip/background; [shape] clips the ripple
 * exactly like a `clip(shape).kxClick()` would.
 */
fun Modifier.kxHitClick(label: String?, shape: androidx.compose.ui.graphics.Shape = CircleShape, min: Dp = 48.dp, role: Role = Role.Button,
                        enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val drawn = remember { IntArray(2) }
    this.layout { m, c ->
        val minPx = min.roundToPx()
        val p = m.measure(c.copy(maxWidth = maxOf(c.maxWidth, minPx), maxHeight = maxOf(c.maxHeight, minPx)))
        val w = c.constrainWidth(drawn[0]); val h = c.constrainHeight(drawn[1])
        layout(w, h) { p.place((w - p.width) / 2, (h - p.height) / 2) }
    }
        .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
        .clickable(interactionSource = src, indication = null, enabled = enabled, role = role, onClick = onClick)
        .layout { m, c ->
            val minPx = min.roundToPx()
            val p = m.measure(c)
            drawn[0] = p.width; drawn[1] = p.height
            val w = c.constrainWidth(maxOf(p.width, minPx)); val h = c.constrainHeight(maxOf(p.height, minPx))
            layout(w, h) { p.place((w - p.width) / 2, (h - p.height) / 2) }
        }
        .clip(shape).indication(src, ripple(color = Kx.accent))
}

/** a11y: the drawn switch/checkbox reports on/off to the (merged) row around it — TalkBack says «Вкл/Выкл». */
fun Modifier.kxToggleState(on: Boolean): Modifier = this.semantics { toggleableState = ToggleableState(on) }
/** a11y: the drawn radio/selection mark reports «выбрано» to the (merged) row around it. */
fun Modifier.kxSelectedState(on: Boolean): Modifier = this.semantics { selected = on }
/** a11y: TalkBack heading (jump by headings). */
fun Modifier.kxHeading(): Modifier = this.semantics { heading() }

/**
 * a11y (P1-4): TalkBack custom actions for the row drawn inside — ChatSwipe/SwipeToRead provide the
 * swipe actions here and ChatRow puts them on its merged node.
 */
val LocalKxRowActions = staticCompositionLocalOf<List<CustomAccessibilityAction>> { emptyList() }

/** Round icon button (44dp drawn, 48dp touch/TalkBack box): plain glass fill or the accent gradient. */
@Composable
fun KxIconButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp, iconSize: Dp = 22.dp,
                 accent: Boolean = false, filled: Boolean = true, glass: Boolean = false, tint: Color = if (accent) Kx.onAccent else Kx.text2, onClick: () -> Unit) {
    Box(
        modifier.kxHitClick(label, CircleShape, onClick = onClick)
            .size(if (Kx.look.bigButtons) maxOf(size, 52.dp) else size).clip(CircleShape)
            .then(if (accent) Modifier.background(Kx.accentGradient) else if (filled || glass) Modifier.background(Kx.fill) else Modifier)
            .then(if (glass) Modifier.border(1.dp, Kx.w(.09f), CircleShape).topHighlight(Kx.w(.14f)) else Modifier)
            .then(if (Kx.look.outlines) Modifier.border(1.5.dp, Kx.w(.55f), CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }
}

/** Filter / folder chip: 36dp pill, accent when selected, dashed when it is an "add" chip. */
@Composable
fun KxChip(text: String, selected: Boolean, modifier: Modifier = Modifier, dashed: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.kxHitClick(null, shape, onClick = onClick).then(if (dashed) Modifier else Modifier.kxSelectedState(selected))
            .height(36.dp).clip(shape)
            .then(
                when {
                    // critique-C: selected = dark-green fill + green outline + green text (as the boards)
                    selected -> Modifier.background(Kx.accent.copy(alpha = .14f)).border(1.dp, Kx.accent.copy(alpha = .55f), shape)
                    dashed -> Modifier.dashedBorder(Kx.dashed, 18.dp)
                    else -> Modifier.background(Kx.w(.05f)).border(1.dp, Kx.line, shape)
                }
            )
            .padding(horizontal = if (dashed) 12.dp else 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = KxType.chip.copy(
            color = if (selected) Kx.accent else if (dashed) Kx.text4 else Kx.text2,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium), maxLines = 1)
    }
}

fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.dp): Modifier = this.then(
    Modifier.drawBehind {
        val r = radius.toPx()
        drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            style = Stroke(width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
    }
)

/** Counter badge (22dp). Muted chats get the gray one, mentions the amber "@". */
@Composable
fun KxBadge(text: String, modifier: Modifier = Modifier, muted: Boolean = false, size: Dp = 22.dp, mention: Boolean = false) {
    // critique-A S: one badge style (accent fill) + one special case: mention «@» = accent outline.
    Box(
        modifier.heightIn(min = size).widthIn(min = size).clip(RoundedCornerShape(size / 2))
            .then(if (mention) Modifier.border(1.5.dp, Kx.accent, RoundedCornerShape(size / 2)) else Modifier.background(if (muted) Kx.badgeMuted else Kx.accent)).padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = KxType.caption.copy(color = if (mention) Kx.accent else if (muted) Kx.text2 else Kx.onAccent,
            fontWeight = if (muted) FontWeight.Bold else FontWeight.ExtraBold), maxLines = 1)
    }
}

/** Letter (or icon) avatar on a 140deg gradient, with an optional online dot and story ring. */
@Composable
fun KxAvatar(name: String, id: Long, modifier: Modifier = Modifier, size: Dp = 54.dp, icon: ImageVector? = null,
             online: Boolean = false, story: Boolean = false, square: Boolean = false, colorIndex: Int = -1,
             swatch: KxAvatarPalette.Swatch = if (colorIndex >= 0) KxAvatarPalette.all[colorIndex % KxAvatarPalette.all.size] else KxAvatarPalette.of(id),
             ringColor: Color = Kx.bg, letterWeight: FontWeight = FontWeight.Bold, letterSize: androidx.compose.ui.unit.TextUnit = (size.value * 0.37f).sp,
             /** Local photo path (TDLib download); the letter shows until it is decoded. */
             photo: String? = null) {
    // critique-A S: one avatar rule — every peer (people, groups, channels) is a circle, as in Telegram; `square` is kept for old call sites.
    @Suppress("UNUSED_VARIABLE") val legacySquare = square
    val shape = CircleShape
    Box(modifier.size(size)) {
        val inner = if (story) Modifier.padding(2.dp) else Modifier
        // conic-gradient starts at 12 o'clock, sweepGradient at 3: rotate the ring by -90 degrees.
        if (story) Box(Modifier.matchParentSize().graphicsLayer { rotationZ = -90f }.clip(shape).background(Kx.storyRing))
        Box(
            Modifier.matchParentSize().then(inner)
                .then(if (story) Modifier.border(2.dp, ringColor, shape) else Modifier)
                .clip(shape)
                .drawBehind { drawRect(Brush.linearGradient(listOf(swatch.from, swatch.to), cssAngleStart(140f, this.size), cssAngleEnd(140f, this.size))) },
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(size * 0.44f), tint = swatch.ink)
            else Text(name.trim().take(1).uppercase().ifEmpty { "?" },
                style = KxType.style(letterSize, letterWeight, swatch.ink))
            if (photo != null) KxImage(photo, Modifier.matchParentSize(), maxPx = (size.value * 3).toInt())
        }
        // Mockup: 14px dot + 3px background border on a 54px avatar = 20 outer / 14 inner.
        if (online) Box(
            Modifier.align(Alignment.BottomEnd).size(size * 0.37f).clip(CircleShape)
                .background(ringColor).padding(size * 0.0555f).clip(CircleShape).background(Kx.accent)
        )
    }
}

/** Start/end points of a CSS linear-gradient(angle) over [size]. */
fun cssAngleStart(deg: Float, size: androidx.compose.ui.geometry.Size): Offset {
    val r = Math.toRadians(deg.toDouble()); val dx = kotlin.math.sin(r).toFloat(); val dy = -kotlin.math.cos(r).toFloat()
    val half = (kotlin.math.abs(size.width * dx) + kotlin.math.abs(size.height * dy)) / 2
    return Offset(size.width / 2 - dx * half, size.height / 2 - dy * half)
}
fun cssAngleEnd(deg: Float, size: androidx.compose.ui.geometry.Size): Offset {
    val s = cssAngleStart(deg, size); return Offset(size.width - s.x, size.height - s.y)
}

/** 1px inner highlight on the top edge (CSS inset 0 1px 0 rgba(255,255,255,a)). */
fun Modifier.topHighlight(color: Color): Modifier = this.then(Modifier.drawBehind {
    drawLine(color, Offset(size.height / 2, .5f), Offset(size.width - size.height / 2, .5f), 1f)
})

/**
 * Soft green glow: CSS radial-gradient(circle, c, transparent 65%) on a [diameter] circle. The
 * gradient reaches 0 at 65% of the farthest-corner distance (r * sqrt 2), like the browser.
 */
@Composable
fun KxGlow(modifier: Modifier = Modifier, diameter: Dp = 360.dp, x: Dp = (-80).dp, y: Dp = (-120).dp, alpha: Float = .18f, alignEnd: Boolean = false) {
    Box(Modifier.fillMaxWidth()) {
        Canvas(modifier.align(if (alignEnd) Alignment.TopEnd else Alignment.TopStart).offset(if (alignEnd) -x else x, y).size(diameter)) {
            val c = Color(0xFF46A06E)
            val reach = size.minDimension / 2 * 1.41421f * .65f
            drawCircle(Brush.radialGradient(listOf(c.copy(alpha = alpha), c.copy(alpha = 0f)), center, radius = reach), radius = size.minDimension / 2)
        }
    }
}

/** Screen root: black background + glow, content on top. */
@Composable
fun KxScreen(modifier: Modifier = Modifier, glow: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Kx.bg)) {
        if (glow) KxGlow()
        content()
    }
}

/** "kxRise": fade + 14dp lift when the screen appears, with a stagger delay. */
@Composable
fun Modifier.kxRise(delayMs: Int = 0, durationMs: Int = 600): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(delayMs.toLong()); p.animateTo(1f, tween(durationMs, easing = KxMotion.rise)) }
    return this.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 14.dp.toPx() }
}

/** Three blinking dots ("печатает…"). */
@Composable
fun KxTypingDots(color: Color = Kx.accent, text: String = "печатает", size: androidx.compose.ui.unit.TextUnit = 14.sp) {
    val t = rememberInfiniteTransition(label = "dots")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "phase")
    val reduced = LocalKxReducedMotion.current
    Row {
        Text(text, style = KxType.bodySmall.copy(color = color, fontSize = size))
        repeat(3) { i ->
            val local = ((phase - i * 0.1667f) % 1f + 1f) % 1f
            val a = if (reduced) 1f else if (local < 0.3f) 0.3f + local / 0.3f * 0.7f else if (local < 0.6f) 1f - (local - 0.3f) / 0.3f * 0.7f else 0.3f
            Text(".", style = KxType.bodySmall.copy(color = color, fontSize = size), modifier = Modifier.alpha(a))
        }
    }
}

/** One line of text that ends with "…". */
@Composable
fun KxLine(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) =
    Text(text, style = style, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)

/** Primary pill button (48dp+): gradient, or red for dangerous actions, or quiet glass. */
enum class KxButtonKind { Primary, Danger, Quiet }

@Composable
fun KxButton(text: String, modifier: Modifier = Modifier, kind: KxButtonKind = KxButtonKind.Primary, height: Dp = 52.dp,
             icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(height / 2)
    val bg: Modifier = when {
        !enabled -> Modifier.background(Kx.fillHi)
        kind == KxButtonKind.Primary -> Modifier.background(Kx.buttonGradient)
        kind == KxButtonKind.Danger -> Modifier.background(Kx.dangerSolid)
        else -> Modifier.background(Kx.fillHi)
    }
    val ink = when {
        !enabled -> Kx.text4
        kind == KxButtonKind.Primary -> Kx.onAccent
        kind == KxButtonKind.Danger -> Color.White
        else -> Kx.text
    }
    Row(
        modifier.height(if (Kx.look.bigButtons) maxOf(height, 58.dp) else height).clip(shape).then(bg)
            .then(if (Kx.look.outlines) Modifier.border(1.5.dp, Kx.w(.55f), shape) else Modifier)
            .kxClick(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp), tint = ink); Spacer(Modifier.width(8.dp)) }
        // critique-C: a long label shrinks down to 13sp, then ends with «…» (was cut: «Поделитьс»).
        var fs by remember(text) { mutableStateOf(15f) }
        Text(text, style = KxType.style(fs.sp, if (kind == KxButtonKind.Quiet) FontWeight.Bold else FontWeight.ExtraBold, ink), maxLines = 1, softWrap = false,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            onTextLayout = { if (it.hasVisualOverflow && fs > 13f) fs -= 1f })
    }
}

/**
 * A2: THE switch of v3 (every board uses it; the old local copies delegate here). 48x28, knob 22.
 * ON: near-white knob on the green track; OFF: grey knob on a dark track. Only [onTint] may tint the ON
 * track (e.g. the violet night mode of «Не беспокоить»); the old size/colour params are kept for the old
 * call sites but ignored, so every switch looks the same.
 */
@Composable
fun KxSwitch(checked: Boolean, modifier: Modifier = Modifier, @Suppress("UNUSED_PARAMETER") width: Dp = 48.dp, @Suppress("UNUSED_PARAMETER") height: Dp = 28.dp,
             @Suppress("UNUSED_PARAMETER") knob: Dp = 22.dp, @Suppress("UNUSED_PARAMETER") trackOn: Color = Kx.accentDeep, @Suppress("UNUSED_PARAMETER") trackOff: Color = Kx.w(.14f),
             @Suppress("UNUSED_PARAMETER") knobOn: Color = Kx.text, @Suppress("UNUSED_PARAMETER") knobOff: Color = Kx.text3, @Suppress("UNUSED_PARAMETER") shadow: Boolean = false, onTint: Color? = null) {
    val w = 48.dp; val h = 28.dp; val k = 22.dp
    val pad = (h - k) / 2
    val reduced = LocalKxReducedMotion.current
    val x by animateDpAsState(if (checked) w - k - pad else pad, if (reduced) snap() else spring(dampingRatio = .6f, stiffness = 700f), label = "knob")
    val track by androidx.compose.animation.animateColorAsState(if (checked) (onTint ?: Kx.accentDeep) else Kx.w(.14f), tween(if (reduced) 0 else 200), label = "track")
    val ink by androidx.compose.animation.animateColorAsState(if (checked) Color(0xFFF7FAF8) else Kx.text3, tween(if (reduced) 0 else 200), label = "ink")
    Box(modifier.kxToggleState(checked).size(w, h).clip(RoundedCornerShape(h / 2)).background(track)) {
        Box(Modifier.offset(x, pad).size(k).then(if (checked) Modifier.shadow(2.dp, CircleShape) else Modifier).clip(CircleShape).background(ink))
    }
}

/** Settings-style row: icon tile, title + subtitle, trailing slot. 60dp+ tall. */
@Composable
fun KxRow(title: String, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null, iconTint: Color = Kx.accent,
          danger: Boolean = false, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp).then(if (onClick != null) Modifier.kxClick(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (danger) Kx.dangerSolid.copy(alpha = .12f) else iconTint.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = if (danger) Kx.danger else iconTint)
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = KxType.style(15.sp, FontWeight.SemiBold, if (danger) Kx.danger else Kx.text))
            if (subtitle != null) Text(subtitle, style = KxType.caption.copy(fontSize = 13.sp, lineHeight = 18.sp), modifier = Modifier.padding(top = 2.dp))
        }
        if (trailing != null) { Spacer(Modifier.width(10.dp)); trailing() }
    }
}

/** Rounded group card holding rows (settings lists). */
@Composable
fun KxCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) = KxGroup(modifier, inset = 70.dp, content = content) // A1: one group look

@Composable
fun KxSectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = modifier.kxHeading().padding(start = 20.dp, top = 18.dp, bottom = 8.dp)) // critique-C: captions green

/** The purple "PREMIUM" chip. */
@Composable
fun KxPremiumChip(modifier: Modifier = Modifier) {
    Box(modifier.height(20.dp).clip(RoundedCornerShape(10.dp)).background(Kx.premium.copy(alpha = .18f)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text("Premium", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.premium))
    }
}

/** Flat top bar of sub-screens (Privacy board): 44dp glass back button, 22sp title, padding 10/12/2. */
@Composable
fun KxTopBar(title: String, modifier: Modifier = Modifier, subtitle: String? = null, onBack: (() -> Unit)? = null,
             actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onBack != null) KxBackButton(onBack)
        Column(Modifier.weight(1f)) {
            KxLine(title, KxType.h1, Modifier.kxHeading())
            // A5: header subtitles are one line; explanations live in the screen body.
            if (subtitle != null) Text(subtitle, style = KxType.caption.copy(fontSize = 13.sp, lineHeight = 18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Suppress("unused") private val keepBorderStroke = BorderStroke(1.dp, Color.Transparent)
@Suppress("unused") private val keepInteraction: MutableInteractionSource? = null

/** critique-A S: THE back button of every header (44dp circled glass, 48dp touch). X only for modals: [close] = true. */
@Composable
fun KxBackButton(onBack: () -> Unit, modifier: Modifier = Modifier, close: Boolean = false) =
    KxIconButton(if (close) androidx.compose.material.icons.Icons.Rounded.Close else androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
        if (close) "Закрыть" else "Назад", modifier, glass = true, tint = Kx.text, iconSize = 24.dp, onClick = onBack)

/** critique-A S: a trailing header action — the same circled 44dp glass button, always with a label for TalkBack. */
@Composable
fun KxHeaderAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, tint: Color = Kx.text2, onClick: () -> Unit) =
    KxIconButton(icon, label, modifier, glass = true, tint = tint, onClick = onClick)

/**
 * critique-A S: the «create» action of a tab (new chat / call / story / contact) — a bottom-right FAB in thumb reach,
 * above the floating dock. Put it in the tab's root Box: Modifier.align(Alignment.BottomEnd).
 */
@Composable
fun KxFab(icon: ImageVector, label: String, modifier: Modifier = Modifier, aboveDock: Boolean = true, onClick: () -> Unit) {
    Box(modifier.navigationBarsPadding().padding(end = 18.dp, bottom = if (aboveDock) 100.dp else 22.dp)
        .kxHitClick(label, RoundedCornerShape(20.dp), onClick = onClick)
        .size(56.dp).shadow(14.dp, RoundedCornerShape(20.dp), spotColor = Kx.accentDeep).clip(RoundedCornerShape(20.dp)).background(Kx.accentGradient),
        contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(26.dp), tint = Kx.onAccent) }
}

/**
 * critique-A S: demo-only controls (state switchers, «Макет: …» pills) live in ONE dashed «демо» tray, drawn only
 * for the demo store — never on a real account.
 */
@Composable
fun KxDemoControls(store: com.kirox.app.ui.v3.model.KxStore, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    if (store !is com.kirox.app.ui.v3.demo.KxDemoStore) return
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).dashedBorder(Kx.dashed, 16.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("демо", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.text4))
        content()
    }
}

/** Demo-only explanation («Демо: числа для примера») in the same dashed «демо» tray; nothing on a real account. */
@Composable
fun KxDemoHint(store: com.kirox.app.ui.v3.model.KxStore, text: String, modifier: Modifier = Modifier) =
    KxDemoControls(store, modifier) { Text(text, style = KxType.style(13.sp, color = Kx.text3, lineHeight = 18.sp)) }
