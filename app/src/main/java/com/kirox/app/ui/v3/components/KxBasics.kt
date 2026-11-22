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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

/** Round icon button (44dp): plain glass fill or the accent gradient. */
@Composable
fun KxIconButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp, iconSize: Dp = 22.dp,
                 accent: Boolean = false, filled: Boolean = true, glass: Boolean = false, tint: Color = if (accent) Kx.onAccent else Kx.text2, onClick: () -> Unit) {
    Box(
        modifier.size(size).clip(CircleShape)
            .then(if (accent) Modifier.background(Kx.accentGradient) else if (filled || glass) Modifier.background(Kx.fill) else Modifier)
            .then(if (glass) Modifier.border(1.dp, Kx.w(.09f), CircleShape).topHighlight(Kx.w(.14f)) else Modifier)
            .kxClick(label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }
}

/** Filter / folder chip: 36dp pill, accent when selected, dashed when it is an "add" chip. */
@Composable
fun KxChip(text: String, selected: Boolean, modifier: Modifier = Modifier, dashed: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.height(36.dp).clip(shape)
            .then(
                when {
                    selected -> Modifier.background(Kx.accent)
                    dashed -> Modifier.dashedBorder(Kx.dashed, 18.dp)
                    else -> Modifier.background(Kx.w(.05f)).border(1.dp, Kx.line, shape)
                }
            )
            .kxClick(onClick = onClick).padding(horizontal = if (dashed) 12.dp else 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = KxType.chip.copy(
            color = if (selected) Kx.onAccent else if (dashed) Kx.text4 else Kx.text2,
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
fun KxBadge(text: String, modifier: Modifier = Modifier, muted: Boolean = false, size: Dp = 22.dp) {
    Box(
        modifier.heightIn(min = size).widthIn(min = size).clip(RoundedCornerShape(size / 2))
            .background(if (muted) Kx.badgeMuted else Kx.accent).padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = KxType.caption.copy(color = if (muted) Kx.text2 else Kx.onAccent,
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
    val shape = if (square) RoundedCornerShape(size * 0.3f) else CircleShape
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
        modifier.height(height).clip(shape).then(bg).kxClick(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp), tint = ink); Spacer(Modifier.width(8.dp)) }
        Text(text, style = KxType.style(15.sp, if (kind == KxButtonKind.Quiet) FontWeight.Bold else FontWeight.ExtraBold, ink), maxLines = 1)
    }
}

/** Switch drawn the mockup way. Default 48x28 / knob 22 (most boards); each board passes its own values. */
@Composable
fun KxSwitch(checked: Boolean, modifier: Modifier = Modifier, width: Dp = 48.dp, height: Dp = 28.dp, knob: Dp = 22.dp,
             trackOn: Color = Kx.accentDeep, trackOff: Color = Kx.w(.14f), knobOn: Color = Kx.text, knobOff: Color = Kx.text3, shadow: Boolean = false) {
    val pad = (height - knob) / 2
    val x by animateDpAsState(if (checked) width - knob - pad else pad, spring(dampingRatio = .55f, stiffness = 700f), label = "knob")
    Box(modifier.size(width, height).clip(RoundedCornerShape(height / 2)).background(if (checked) trackOn else trackOff)) {
        Box(Modifier.offset(x, pad).size(knob).then(if (shadow) Modifier.shadow(3.dp, CircleShape) else Modifier).clip(CircleShape).background(if (checked) knobOn else knobOff))
    }
}

/** Settings-style row: icon tile, title + subtitle, trailing slot. 64dp tall. */
@Composable
fun KxRow(title: String, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null, iconTint: Color = Kx.accent,
          danger: Boolean = false, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp).then(if (onClick != null) Modifier.kxClick(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (danger) Kx.dangerSolid.copy(alpha = .12f) else iconTint.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
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
fun KxCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(KxShape.card)).background(Kx.w(.045f)).border(1.dp, Kx.w(.05f), RoundedCornerShape(KxShape.card)), content = content)
}

@Composable
fun KxSectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.text2), modifier = modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp))

/** The purple "PREMIUM" chip. */
@Composable
fun KxPremiumChip(modifier: Modifier = Modifier) {
    Box(modifier.height(20.dp).clip(RoundedCornerShape(10.dp)).background(Kx.premium.copy(alpha = .18f)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text("PREMIUM", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.premium))
    }
}

/** Flat top bar of sub-screens (Privacy board): 44dp glass back button, 22sp title, padding 10/12/2. */
@Composable
fun KxTopBar(title: String, modifier: Modifier = Modifier, subtitle: String? = null, onBack: (() -> Unit)? = null,
             actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onBack != null) KxIconButton(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, onClick = onBack)
        Column(Modifier.weight(1f)) {
            KxLine(title, KxType.h1)
            if (subtitle != null) Text(subtitle, style = KxType.caption.copy(fontSize = 13.sp, lineHeight = 18.sp), maxLines = 2)
        }
        actions()
    }
}

@Suppress("unused") private val keepBorderStroke = BorderStroke(1.dp, Color.Transparent)
@Suppress("unused") private val keepInteraction: MutableInteractionSource? = null
