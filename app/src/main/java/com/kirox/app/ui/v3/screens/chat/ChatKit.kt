package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChat
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

/*
 * Small building blocks shared by the `chat` section boards (Search, NewMsg, Saved, …).
 * Values are the ones repeated across those mockups.
 */

/** Text style shortcut. */
internal fun ts(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = Kx.text, lh: TextUnit = TextUnit.Unspecified): TextStyle =
    KxType.style(size.sp, weight, color, lh)

internal val W400 = FontWeight.Normal
internal val W500 = FontWeight.Medium
internal val W600 = FontWeight.SemiBold
internal val W700 = FontWeight.Bold
internal val W800 = FontWeight.ExtraBold

/** CSS-angle linear gradient drawn behind. */
internal fun Modifier.cssGradient(deg: Float, a: Color, b: Color): Modifier = this.drawBehind {
    drawRect(Brush.linearGradient(listOf(a, b), cssAngleStart(deg, size), cssAngleEnd(deg, size)))
}

/** Elliptical radial glow: radial-gradient(wx% hy% at cx% cy%, c, transparent). */
internal fun Modifier.ellipseGlow(cx: Float, cy: Float, wx: Float, hy: Float, color: Color): Modifier = this.drawBehind {
    val rx = size.width * wx; val ry = size.height * hy
    val c = Offset(size.width * cx, size.height * cy)
    scale(1f, ry / rx, c) {
        drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), c, rx), rx, c)
    }
}

/** kxIn / kxRise: fade + lift [dy] once on appear. */
@Composable
internal fun Modifier.chIn(delayMs: Int = 0, dy: Dp = 8.dp, durationMs: Int = 300, dx: Dp = 0.dp, easing: Easing = FastOutSlowInEasing): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(durationMs, easing = easing)) }
    return this.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * dy.toPx(); translationX = (1f - p.value) * dx.toPx() }
}

/** kxPop / kxChip: scale from [from] with a small overshoot. */
@Composable
internal fun Modifier.chPop(from: Float = .6f, durationMs: Int = 300, delayMs: Int = 0, fade: Boolean = true): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(durationMs, easing = CubicBezierEasing(.34f, 1.56f, .64f, 1f))) }
    return this.graphicsLayer { val s = from + (1f - from) * p.value; scaleX = s; scaleY = s; if (fade) alpha = p.value.coerceIn(0f, 1f) }
}

/** Round header button: 44 glass (.06 + border .09) like most sub-screens; [outline] = transparent with .16 border. */
@Composable
internal fun ChRound(icon: ImageVector, label: String, size: Dp = 44.dp, iconSize: Dp = 24.dp, outline: Boolean = false,
                     bg: Color = if (outline) Color.Transparent else Kx.w(.06f), border: Color = if (outline) Kx.w(.16f) else Kx.w(.09f),
                     tint: Color = Kx.text, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(size).clip(CircleShape).background(bg).border(1.dp, border, CircleShape).kxClick(label, onClick = onClick),
        contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize), tint = tint) }
}

/** Single-line text input with a placeholder (#7C8781 like the mockups). */
@Composable
internal fun ChInput(value: String, onValue: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, size: Int = 15,
                     singleLine: Boolean = true, color: Color = Kx.text, keyboard: KeyboardOptions = KeyboardOptions.Default) {
    BasicTextField(value, onValue, modifier, singleLine = singleLine, textStyle = ts(size, color = color), cursorBrush = SolidColor(Kx.accent),
        keyboardOptions = keyboard,
        decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) { if (value.isEmpty()) Text(placeholder, style = ts(size, color = Color(0xFF7C8781)), maxLines = 1); inner() } })
}

/** <mark>: rgba(143,211,172,.28) behind the hit. */
internal fun marked(pre: String, hit: String, post: String): AnnotatedString = buildAnnotatedString {
    append(pre); withStyle(SpanStyle(background = Color(0x478FD3AC), color = Kx.text)) { append(hit) }; append(post)
}

/** Mockup split(): up to 28 chars before the hit with a leading «…». Null when [q] is not in [text]. */
internal fun splitHit(text: String, q: String): AnnotatedString? {
    val i = text.lowercase().indexOf(q.lowercase()); if (i < 0 || q.isEmpty()) return null
    val from = maxOf(0, i - 28)
    return marked((if (from > 0) "…" else "") + text.substring(from, i), text.substring(i, i + q.length), text.substring(i + q.length))
}

/** Avatar of a store chat (Saved → bookmark on green). */
@Composable
internal fun ChatAvatar(c: KxChat, size: Dp) {
    if (c.kind == KxChatKind.Saved) KxAvatar(c.title, c.id, size = size, icon = Icons.Rounded.Bookmark, colorIndex = 7)
    else KxAvatar(c.title, c.id, size = size, colorIndex = c.colorIndex,
        icon = if (c.avatarIcon == "campaign") Icons.Rounded.Campaign else null, photo = c.avatar) // qa-real: real photos (search, «Часто пишете», forward…)
}

/** Section caption: 13px w800 accent, padding 0 4 6. */
@Composable
internal fun ChCaption(text: String, modifier: Modifier = Modifier) =
    Text(text, style = ts(13, W800, Kx.accent), modifier = modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp))

/** Bottom sheet: scrim fade + kxSheet slide, radius 30 top, solid #161B18 → [bg]. Tap on the scrim closes. */
@Composable
internal fun ChSheet(visible: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier, scrim: Color = Color.Black.copy(alpha = .55f),
                     bg: Brush = Brush.verticalGradient(listOf(Color(0xFF1B211E), Color(0xFF131715))),
                     content: @Composable ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(enabled = visible, onBack = onDismiss) // fixer-final: Back closes the sheet, not the board
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().background(scrim).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        }
        AnimatedVisibility(visible, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(350, easing = KxMotion.rise)) { it }, exit = slideOutVertically(tween(250)) { it }) {
            Column(modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Color(0xFF101412)).background(bg)
                .border(1.dp, Kx.w(.08f), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .clickable(remember { MutableInteractionSource() }, null) {}.navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.2f)))
                }
                content()
            }
        }
    }
}

/** Gradient primary pill (linear-gradient(100deg,#6FC497,#3F9A6C)) or disabled .07. */
@Composable
internal fun ChPrimary(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, height: Dp = 56.dp, radius: Dp = 18.dp,
                       icon: ImageVector? = null, onClick: () -> Unit) {
    Row(modifier.height(height).clip(RoundedCornerShape(radius))
        .then(if (enabled) Modifier.cssGradient(100f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
        .kxClick(enabled = true, onClick = onClick).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, null, Modifier.size(22.dp), tint = if (enabled) Kx.onAccent else Kx.text4)
        Text(text, style = ts(16, W800, if (enabled) Kx.onAccent else Kx.text4), maxLines = 1)
    }
}

/** Chat-like board root: black + radial-gradient(70% 40% at 100% 15%, rgba(63,154,108,.16)). */
@Composable
internal fun ChChatBg(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Kx.bg).ellipseGlow(1f, .15f, .7f, .4f, Color(0x293F9A6C)), content = content)
}

/** Russian plural: 1 сообщение, 2 сообщения, 5 сообщений. */
internal fun plural(n: Int, one: String, few: String, many: String): String {
    val m = kotlin.math.abs(n) % 100; val d = m % 10
    return if (m in 11..19) many else if (d == 1) one else if (d in 2..4) few else many
}

/** Confirm dialog for dangerous actions (left/right 24, r28, #232A26→#161B18, red action). */
@Composable
internal fun ChConfirm(title: String?, text: String, yes: String, onNo: () -> Unit, onYes: () -> Unit) {
    AnimatedVisibility(title != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).clickable(remember { MutableInteractionSource() }, null, onClick = onNo), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Kx.sheetGradient).border(1.dp, Kx.w(.14f), RoundedCornerShape(28.dp))
                .clickable(remember { MutableInteractionSource() }, null) {}.padding(20.dp)) {
                Text(title.orEmpty(), style = ts(18, W800))
                Text(text, style = ts(14, color = Kx.text2, lh = 20.sp), modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.08f)).kxClick(onClick = onNo), contentAlignment = Alignment.Center) { Text("Отмена", style = ts(15, W700)) }
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.dangerSolid).kxClick(onClick = onYes), contentAlignment = Alignment.Center) { Text(yes, style = ts(15, W800, Color.White)) }
                }
            }
        }
    }
}
