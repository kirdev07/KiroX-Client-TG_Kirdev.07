package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
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
    onAllReactions: () -> Unit = {},
    /** Group / channel: the delete sheet says «Удалить у всех участников» instead of the chat title. */
    isGroup: Boolean = false,
    /** Extra actions opening the message's own boards (info, who read, replace media, remind…). */
    more: List<Triple<ImageVector, String, () -> Unit>> = emptyList(),
    /** qa-real: the message is in «Избранное» — no «В Избранное», delete text says so. */
    inSaved: Boolean = false,
) {
    var deleting by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler { if (deleting) deleting = false else onDismiss() }
    val clipboard = LocalClipboardManager.current
    val reduced = LocalKxReducedMotion.current
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    val strip = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { strip.animateTo(1f, tween(300, easing = CubicBezierEasing(.34f, 1.4f, .5f, 1f))) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(250, easing = KxMotion.rise)) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Short / narrow screens: the menu starts higher and scrolls; the reaction cells shrink to fit (board: 170dp, 40dp).
        val menuTop = (maxHeight - 520.dp).coerceIn(56.dp, 170.dp)
        val cell = ((maxWidth - 92.dp) / 7).coerceIn(30.dp, 40.dp)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = appear.value }.background(if (deleting) Color(0x99050706) else Color(0x8C050706)).kxClick(if (deleting) "Отмена" else "Закрыть меню", onClick = onDismiss))
        if (!deleting) Column(
            Modifier.fillMaxWidth().pointerInput(onDismiss) { detectTapGestures { onDismiss() } }.verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, top = menuTop, bottom = 16.dp),
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
                    Box(Modifier.graphicsLayer { alpha = pop.value.coerceIn(0f, 1f); translationY = (1 - pop.value) * 12.dp.toPx(); scaleX = .4f + .6f * pop.value; scaleY = scaleX }.size(cell).clip(CircleShape).background(if (mine) Kx.accent.copy(alpha = .25f) else Color.Transparent).kxClick("Реакция $e") { onReact(e) },
                        contentAlignment = Alignment.Center) { Text(e, style = KxType.style(24.sp)) }
                }
                Box(Modifier.size(34.dp).clip(CircleShape).background(Kx.fillHi).kxClick("Все реакции", onClick = onAllReactions), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ExpandMore, null, Modifier.size(20.dp), tint = Kx.text)
                }
            }
            // The lifted message
            // qa-real: formatting kept, media named (and its preview shown) instead of «Медиа».
            val preview = (m.thumb ?: m.media)?.takeIf { m.kind == com.kirox.app.ui.v3.model.KxMsgKind.Photo || m.kind == com.kirox.app.ui.v3.model.KxMsgKind.Video || m.kind == com.kirox.app.ui.v3.model.KxMsgKind.Gif }
            if (preview != null) KxImage(preview, Modifier.widthIn(max = 300.dp).width(220.dp).heightIn(max = 220.dp).height(if (m.aspect > 0f) (220f / m.aspect).coerceIn(90f, 220f).dp else 160.dp)
                .clip(RoundedCornerShape(18.dp)), maxPx = 640)
            if (m.text.isNotEmpty() || preview == null) Column(
                Modifier.widthIn(max = 300.dp).width(IntrinsicSize.Max).graphicsLayer { val s = .96f + .06f * appear.value; scaleX = s; scaleY = s }
                    .shadow(40.dp, if (m.out) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp), spotColor = Color.Black.copy(alpha = .5f))
                    .clip(if (m.out) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                    .background(if (m.out) Brush.linearGradient(listOf(Color(0xFF2E6B4B), Color(0xFF1F4D36))) else Brush.linearGradient(listOf(Color(0xFF1B211E), Color(0xFF1B211E))))
            ) {
                // fixer-final (qa-real2): a reply keeps its quoted message in the lifted preview, as in the chat bubble.
                m.reply?.let { r ->
                    val ink = if (m.out) Color(0xFFA8D5BC) else Kx.accent
                    Column(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 6.dp).clip(RoundedCornerShape(6.dp, 12.dp, 12.dp, 6.dp)).background(Color.Black.copy(alpha = .18f))
                        .drawBehind { drawRect(ink, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }.padding(start = 13.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)) {
                        Text(r.sender, style = KxType.style(13.sp, FontWeight.Bold, ink), maxLines = 1)
                        Text(r.text, style = KxType.style(13.sp, color = if (m.out) Color(0xFFBFDCCB) else Kx.text3), maxLines = 2)
                    }
                }
                Text(menuPreviewText(m), style = KxType.style(15.sp, color = if (m.out) Color(0xFFE6F6EC) else Kx.text, lineHeight = 21.sp),
                    modifier = Modifier.padding(start = 13.dp, end = 13.dp, top = if (m.reply != null) 6.dp else 9.dp, bottom = 7.dp))
            }
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
                if (m.canForward && !inSaved) MenuItem(Icons.Outlined.BookmarkBorder, "В Избранное", onClick = onSave)
                if (m.canPin) MenuItem(Icons.Outlined.PushPin, if (m.pinned) "Открепить" else "Закрепить", onClick = onPin)
                if (m.canEdit && m.kind in EditableKinds) MenuItem(Icons.Outlined.Edit, if (m.kind == com.kirox.app.ui.v3.model.KxMsgKind.Text) "Изменить" else "Изменить подпись", onClick = onEdit)
                MenuItem(Icons.Outlined.CheckCircle, "Выбрать", onClick = onSelect)
                more.forEach { (ic, label, action) -> MenuItem(ic, label, onClick = action) }
                if (m.canDeleteForMe || m.canDeleteForAll) MenuItem(Icons.Outlined.Delete, "Удалить…", danger = true) { deleting = true }
            }
        }
        if (deleting) DeleteSheet(m, peerName, canDeleteForAll, onCancel = onDismiss, onDelete = onDelete, modifier = Modifier.align(Alignment.BottomCenter), canDeleteForMe = m.canDeleteForMe, isGroup = isGroup, inSaved = inSaved)
    }
}

/** What a message is, in words (menu / delete previews): text with its formatting, or «Фото», «Голосовое сообщение 0:12»… */
internal fun menuKindLabel(m: KxMessage): String = when (m.kind) {
    com.kirox.app.ui.v3.model.KxMsgKind.Photo -> "Фото"
    com.kirox.app.ui.v3.model.KxMsgKind.Video -> "Видео" + m.meta.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    com.kirox.app.ui.v3.model.KxMsgKind.Gif -> "GIF"
    com.kirox.app.ui.v3.model.KxMsgKind.Voice -> "Голосовое сообщение" + m.meta.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    com.kirox.app.ui.v3.model.KxMsgKind.VideoNote -> "Видеосообщение" + m.meta.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    com.kirox.app.ui.v3.model.KxMsgKind.Sticker -> "Стикер"
    com.kirox.app.ui.v3.model.KxMsgKind.File -> m.fileName.ifBlank { "Файл" }
    com.kirox.app.ui.v3.model.KxMsgKind.Music -> m.fileName.ifBlank { "Аудио" }
    com.kirox.app.ui.v3.model.KxMsgKind.Location -> "Геопозиция"
    com.kirox.app.ui.v3.model.KxMsgKind.Poll -> "Опрос" + m.poll?.question?.let { ": $it" }.orEmpty()
    com.kirox.app.ui.v3.model.KxMsgKind.Contact -> "Контакт"
    com.kirox.app.ui.v3.model.KxMsgKind.Dice -> "Кубик"
    else -> "Сообщение"
}

private fun menuPreviewText(m: KxMessage): AnnotatedString {
    if (m.text.isEmpty()) return AnnotatedString(menuKindLabel(m))
    return androidx.compose.ui.text.buildAnnotatedString {
        append(m.text)
        m.entities.forEach { e ->
            val start = e.offset; val end = e.offset + e.length
            if (start < 0 || end > m.text.length || start >= end) return@forEach
            val st = when (e.type) {
                com.kirox.app.ui.v3.model.KxEntityType.Bold -> androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)
                com.kirox.app.ui.v3.model.KxEntityType.Italic -> androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                com.kirox.app.ui.v3.model.KxEntityType.Underline -> androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                com.kirox.app.ui.v3.model.KxEntityType.Strike -> androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                com.kirox.app.ui.v3.model.KxEntityType.Code, com.kirox.app.ui.v3.model.KxEntityType.Pre -> androidx.compose.ui.text.SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                com.kirox.app.ui.v3.model.KxEntityType.Link, com.kirox.app.ui.v3.model.KxEntityType.TextLink -> androidx.compose.ui.text.SpanStyle(color = Color(0xFF8FD3AC), textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                else -> null
            }
            if (st != null) addStyle(st, start, end)
        }
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
/** Kinds whose text (or caption) can be edited when TDLib says canEdit. */
private val EditableKinds = setOf(com.kirox.app.ui.v3.model.KxMsgKind.Text, com.kirox.app.ui.v3.model.KxMsgKind.Photo, com.kirox.app.ui.v3.model.KxMsgKind.Video,
    com.kirox.app.ui.v3.model.KxMsgKind.Gif, com.kirox.app.ui.v3.model.KxMsgKind.File, com.kirox.app.ui.v3.model.KxMsgKind.Music, com.kirox.app.ui.v3.model.KxMsgKind.Voice)

@Composable
fun DeleteSheet(m: KxMessage, peerName: String, canDeleteForAll: Boolean, onCancel: () -> Unit, onDelete: (Boolean) -> Unit, modifier: Modifier = Modifier, canDeleteForMe: Boolean = true, isGroup: Boolean = false,
                inSaved: Boolean = false, count: Int = 1) {
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
        Text(if (count > 1) "Удалить сообщения: $count?" else "Удалить сообщение?", style = KxType.style(18.sp, FontWeight.ExtraBold))
        if (count == 1) KxLine(m.text.ifEmpty { menuKindLabel(m) }, KxType.style(14.sp, color = Kx.text2), Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.05f)).padding(horizontal = 12.dp, vertical = 10.dp))
        if (forAllOnly) Text("Сообщение удалится у всех участников чата.", style = KxType.caption, modifier = Modifier.padding(top = 6.dp))
        else if (canDeleteForAll) Row(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 52.dp).kxClick(role = androidx.compose.ui.semantics.Role.Checkbox) { both = !both },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (both) Kx.accent else Color.Transparent)
                .border(2.dp, if (both) Kx.accent else Kx.w(.3f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                if (both) Icon(Icons.Rounded.Check, null, Modifier.size(17.dp), tint = Kx.onAccent)
            }
            Text(if (isGroup) "Удалить у всех участников" else "Также удалить у: $peerName", style = KxType.style(15.sp), modifier = Modifier.weight(1f))
        } else Text(when { inSaved -> "Сообщение удалится из Избранного."; m.out -> "Сообщение удалится только у вас."; else -> "Чужое сообщение удалится только у вас." },
            style = KxType.caption, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxButton("Отмена", Modifier.weight(1f), KxButtonKind.Quiet, height = 50.dp, onClick = onCancel)
            KxButton("Удалить", Modifier.weight(1f), KxButtonKind.Danger, height = 50.dp) { onDelete(forAllOnly || (canDeleteForAll && both)) }
        }
    }
}
