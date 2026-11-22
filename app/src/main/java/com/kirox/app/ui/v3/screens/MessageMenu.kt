package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxMessage
import com.kirox.app.ui.v3.theme.*

private val MenuGlass = Brush.verticalGradient(listOf(Color(0xF22E3A34), Color(0xF5181E1B)))
private val Reactions = listOf("❤️", "🔥", "👍", "😂", "😮", "😢", "🎉")

/**
 * Long-press menu of a message (board "Меню сообщения"): dimmed blurred backdrop, reactions strip,
 * the lifted message, a 230dp action list; «Удалить…» opens the delete sheet with
 * «Также удалить у: <имя>» for own messages.
 */
@Composable
fun MessageMenuSheet(
    m: KxMessage, peerName: String, onDismiss: () -> Unit, onReply: () -> Unit, onEdit: () -> Unit,
    onReact: (String) -> Unit, onDelete: (forEveryone: Boolean) -> Unit,
    onForward: () -> Unit = onDismiss, onPin: () -> Unit = onDismiss, onSave: () -> Unit = onDismiss, onSelect: () -> Unit = onDismiss,
    onTranslate: () -> Unit = onDismiss, onCopied: () -> Unit = onDismiss, canDeleteForAll: Boolean = m.out,
) {
    var deleting by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val reduced = LocalKxReducedMotion.current
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    val strip = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { strip.animateTo(1f, tween(300, easing = CubicBezierEasing(.34f, 1.4f, .5f, 1f))) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(250, easing = KxMotion.rise)) }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = appear.value }.background(if (deleting) Color(0x99050706) else Color(0x8C050706)).kxClick(if (deleting) "Отмена" else "Закрыть меню", onClick = onDismiss))
        if (!deleting) Column(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 170.dp),
            horizontalAlignment = if (m.out) Alignment.End else Alignment.Start, verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Reactions strip
            Row(
                Modifier.graphicsLayer { alpha = strip.value.coerceIn(0f, 1f); translationY = (1 - strip.value) * 10.dp.toPx(); scaleX = .8f + .2f * strip.value; scaleY = scaleX }
                    .height(52.dp).shadow(30.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = .5f)).clip(RoundedCornerShape(26.dp))
                    .background(MenuGlass).border(1.dp, Kx.stroke, RoundedCornerShape(26.dp)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Reactions.forEachIndexed { i, e ->
                    val mine = m.reactions.any { it.emoji == e && it.mine }
                    val pop = remember { Animatable(if (reduced) 1f else 0f) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(i * 30L); pop.animateTo(1f, tween(300, easing = CubicBezierEasing(.34f, 1.5f, .5f, 1f))) }
                    Box(Modifier.graphicsLayer { alpha = pop.value.coerceIn(0f, 1f); translationY = (1 - pop.value) * 12.dp.toPx(); scaleX = .4f + .6f * pop.value; scaleY = scaleX }.size(40.dp).clip(CircleShape).background(if (mine) Kx.accent.copy(alpha = .25f) else Color.Transparent).kxClick("Реакция $e") { onReact(e) },
                        contentAlignment = Alignment.Center) { Text(e, style = KxType.style(24.sp)) }
                }
                Box(Modifier.size(34.dp).clip(CircleShape).background(Kx.fillHi).kxClick("Все реакции") {}, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ExpandMore, null, Modifier.size(20.dp), tint = Kx.text)
                }
            }
            // The lifted message
            Text(
                m.text.ifEmpty { "Медиа" }, style = KxType.style(15.sp, color = if (m.out) Color(0xFFE6F6EC) else Kx.text, lineHeight = 21.sp),
                modifier = Modifier.widthIn(max = 300.dp).graphicsLayer { val s = .96f + .06f * appear.value; scaleX = s; scaleY = s }
                    .shadow(40.dp, if (m.out) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp), spotColor = Color.Black.copy(alpha = .5f))
                    .clip(if (m.out) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                    .background(if (m.out) Brush.linearGradient(listOf(Color(0xFF2E6B4B), Color(0xFF1F4D36))) else Brush.linearGradient(listOf(Color(0xFF1B211E), Color(0xFF1B211E))))
                    .padding(start = 13.dp, end = 13.dp, top = 9.dp, bottom = 7.dp)
            )
            // Actions
            Column(
                Modifier.width(230.dp).graphicsLayer { alpha = appear.value; translationY = (1 - appear.value) * -8.dp.toPx() }
                    .shadow(40.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = .6f)).clip(RoundedCornerShape(22.dp))
                    .background(MenuGlass).border(1.dp, Kx.stroke, RoundedCornerShape(22.dp)).padding(6.dp)
            ) {
                MenuItem(Icons.AutoMirrored.Outlined.Reply, "Ответить", onClick = onReply)
                if (m.canCopy && m.text.isNotEmpty()) MenuItem(Icons.Outlined.ContentCopy, "Копировать") { clipboard.setText(AnnotatedString(m.text)); onCopied() }
                if (m.text.isNotEmpty()) MenuItem(Icons.Outlined.Translate, if (m.translation != null) "Показать оригинал" else "Перевести", onClick = onTranslate)
                if (m.canForward) MenuItem(Icons.AutoMirrored.Outlined.Forward, "Переслать", onClick = onForward)
                if (m.canForward) MenuItem(Icons.Outlined.BookmarkBorder, "В Избранное", onClick = onSave)
                if (m.canPin) MenuItem(Icons.Outlined.PushPin, if (m.pinned) "Открепить" else "Закрепить", onClick = onPin)
                if (m.canEdit && m.kind == com.kirox.app.ui.v3.model.KxMsgKind.Text) MenuItem(Icons.Outlined.Edit, "Изменить", onClick = onEdit)
                MenuItem(Icons.Outlined.CheckCircle, "Выбрать", onClick = onSelect)
                if (m.canDeleteForMe || m.canDeleteForAll) MenuItem(Icons.Outlined.Delete, "Удалить…", danger = true) { deleting = true }
            }
        }
        if (deleting) DeleteSheet(m, peerName, canDeleteForAll, onCancel = onDismiss, onDelete = onDelete, modifier = Modifier.align(Alignment.BottomCenter), canDeleteForMe = m.canDeleteForMe)
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).kxClick(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (danger) Kx.danger else Kx.accent)
        Text(label, style = KxType.style(15.sp, color = if (danger) Kx.danger else Kx.text))
    }
}

/** Bottom sheet «Удалить сообщение?» (radius 30 on top, solid — no glass). */
@Composable
fun DeleteSheet(m: KxMessage, peerName: String, canDeleteForAll: Boolean, onCancel: () -> Unit, onDelete: (Boolean) -> Unit, modifier: Modifier = Modifier, canDeleteForMe: Boolean = true) {
    // Telegram's three cases: both (checkbox), for everyone only (supergroups/channels), only for me.
    val forAllOnly = canDeleteForAll && !canDeleteForMe
    var both by remember { mutableStateOf(true) }
    val y = remember { Animatable(1f) }
    LaunchedEffect(Unit) { y.animateTo(0f, tween(300, easing = KxMotion.rise)) }
    Column(
        modifier.fillMaxWidth().graphicsLayer { translationY = y.value * size.height }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E2421), Color(0xFF121614))))
            .drawBehind { drawLine(Kx.stroke, androidx.compose.ui.geometry.Offset(0f, .5f), androidx.compose.ui.geometry.Offset(size.width, .5f), 1f) }
            .navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
        Text("Удалить сообщение?", style = KxType.style(18.sp, FontWeight.ExtraBold))
        KxLine(m.text.ifEmpty { "Медиа" }, KxType.style(14.sp, color = Kx.text2), Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.05f)).padding(horizontal = 12.dp, vertical = 10.dp))
        if (forAllOnly) Text("Сообщение удалится у всех участников чата.", style = KxType.caption, modifier = Modifier.padding(top = 6.dp))
        else if (canDeleteForAll) Row(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 52.dp).kxClick(role = androidx.compose.ui.semantics.Role.Checkbox) { both = !both },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (both) Kx.accent else Color.Transparent)
                .border(2.dp, if (both) Kx.accent else Kx.w(.3f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                if (both) Icon(Icons.Rounded.Check, null, Modifier.size(17.dp), tint = Kx.onAccent)
            }
            Text("Также удалить у: $peerName", style = KxType.style(15.sp), modifier = Modifier.weight(1f))
        } else Text("Чужое сообщение удалится только у вас.", style = KxType.caption, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxButton("Отмена", Modifier.weight(1f), KxButtonKind.Quiet, height = 50.dp, onClick = onCancel)
            KxButton("Удалить", Modifier.weight(1f), KxButtonKind.Danger, height = 50.dp) { onDelete(forAllOnly || (canDeleteForAll && both)) }
        }
    }
}
