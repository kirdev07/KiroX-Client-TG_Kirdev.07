package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

/** Chats tab (board "Чаты"): brand header, stories chip, folder chips, archive row, chat rows. */
@Composable
fun ChatsScreen(
    store: KxStore,
    onOpenChat: (Long) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenStories: () -> Unit,
    onSearch: () -> Unit,
    onNewMessage: () -> Unit,
    onLongPressChat: (KxChat) -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 110.dp,
) {
    var folder by rememberSaveable { mutableIntStateOf(0) }
    val chats = store.chatsIn(folder)
    KxGlow()
    Column(Modifier.fillMaxSize()) {
        ChatsHeader(store, onOpenStories, onSearch, onNewMessage)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp).kxRise(140),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            store.folders.forEach { f ->
                KxChip(if (f.unread > 0) "${f.title} · ${f.unread}" else f.title, selected = folder == f.id) { folder = f.id }
            }
            KxChip("+ Папка", selected = false, dashed = true) {}
        }
        store.connection?.let { Text(it, style = KxType.caption.copy(color = Kx.accent), modifier = Modifier.padding(start = 20.dp, top = 8.dp)) }
        LazyColumn(Modifier.fillMaxSize().padding(top = 6.dp).kxRise(200), contentPadding = PaddingValues(bottom = bottomPadding)) {
            if (folder == 0 && store.archivedChats.isNotEmpty()) item("archive") { ArchiveRow(store.archivedChats, onOpenArchive) }
            items(chats, key = { it.id }) { chat ->
                SwipeToRead(enabled = chat.unread > 0, onRead = { store.markRead(chat.id) }) {
                    ChatRow(chat, onClick = { onOpenChat(chat.id) }, onLongClick = { onLongPressChat(chat) })
                }
            }
        }
    }
}

@Composable
private fun ChatsHeader(store: KxStore, onOpenStories: () -> Unit, onSearch: () -> Unit, onNewMessage: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp).kxRise(),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(buildAnnotatedString {
            append("kiro"); withStyle(SpanStyle(color = Kx.accent, fontWeight = FontWeight.ExtraBold)) { append("X") }
        }, style = KxType.brand.copy(letterSpacing = .2.sp))
        val unseen = store.stories.filter { it.unseen && !it.mine }
        if (unseen.isNotEmpty()) Row(
            Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).kxClick("Истории: ${unseen.size} ${plural(unseen.size, "новая", "новых", "новых")}", onClick = onOpenStories)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                unseen.take(3).forEachIndexed { i, p ->
                    KxAvatar(p.name, p.id, Modifier.offset(x = (i * 20).dp), size = 30.dp, story = true, colorIndex = p.colorIndex)
                }
                Spacer(Modifier.width((30 + (unseen.take(3).size - 1) * 20).dp))
            }
            Text("${unseen.size} ${plural(unseen.size, "история", "истории", "историй")}", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
        }
        Spacer(Modifier.weight(1f))
        KxIconButton(Icons.Rounded.Search, "Поиск", onClick = onSearch)
        KxIconButton(Icons.Rounded.Edit, "Новое сообщение", accent = true, onClick = onNewMessage)
    }
}

@Composable
private fun ArchiveRow(chats: List<KxChat>, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp).kxClick("Архив", onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Box(Modifier.padding(horizontal = 4.dp).size(46.dp).clip(CircleShape).background(Kx.surface2), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Inventory2, null, tint = Kx.text3)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Архив", style = KxType.style(15.sp, FontWeight.SemiBold, Kx.text2), modifier = Modifier.weight(1f))
                val n = chats.count { it.unread > 0 }
                if (n > 0) KxBadge(n.toString(), muted = true)
            }
            KxLine(chats.joinToString { it.title }, KxType.style(13.sp, color = Kx.text4))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRow(chat: KxChat, onClick: () -> Unit, onLongClick: () -> Unit, selected: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).background(if (selected) Kx.accent.copy(alpha = .1f) else Kx.bg)
            .combinedClickable(interactionSource = null, indication = ripple(color = Kx.accent), onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        val icon = when {
            chat.kind == KxChatKind.Saved -> Icons.Rounded.Bookmark
            chat.avatarIcon == "campaign" -> Icons.Rounded.Campaign
            else -> null
        }
        // Rows carry no story ring: stories live in the header chip (board «Чаты»).
        if (chat.kind == KxChatKind.Saved) KxAvatar(chat.title, chat.id, size = 54.dp, icon = icon,
            swatch = KxAvatarPalette.Swatch(Color(0xFF2F6E4D), Color(0xFF173A28), Color(0xFFC8F0D8)))
        else KxAvatar(chat.title, chat.id, size = 54.dp, icon = icon, online = chat.online, colorIndex = chat.colorIndex)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (chat.kind) {
                    KxChatKind.Group -> Icon(Icons.Rounded.Group, null, Modifier.size(16.dp), tint = Kx.text4)
                    KxChatKind.Secret -> Icon(Icons.Rounded.Lock, null, Modifier.size(15.dp), tint = Kx.accent)
                    else -> {}
                }
                Text(chat.title, style = KxType.style(16.sp, if (chat.unread > 0 && !chat.muted) FontWeight.Bold else FontWeight.SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (chat.verified) Icon(Icons.Rounded.Verified, null, Modifier.size(16.dp), tint = Kx.accent)
                if (chat.muted) Icon(Icons.Rounded.NotificationsOff, null, Modifier.size(15.dp), tint = Kx.text4)
              }
                if (chat.pinned) Icon(Icons.Rounded.PushPin, null, Modifier.size(15.dp), tint = Kx.text4)
                when (chat.out) {
                    KxOutState.Read -> Icon(Icons.Rounded.DoneAll, null, Modifier.size(15.dp), tint = Kx.accent)
                    KxOutState.Sent -> Icon(Icons.Rounded.Done, null, Modifier.size(15.dp), tint = Kx.text4)
                    KxOutState.Sending -> Icon(Icons.Rounded.Schedule, null, Modifier.size(14.dp), tint = Kx.text4)
                    KxOutState.Failed -> Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(15.dp), tint = Kx.danger)
                    KxOutState.None -> {}
                }
                val fresh = chat.unread > 0 && !chat.muted
                Text(chat.time, style = KxType.style(12.sp, if (fresh) FontWeight.SemiBold else FontWeight.Normal, if (fresh) Kx.accent else Kx.text4))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    when {
                        chat.typing != null -> KxTypingDots(text = chat.typing)
                        chat.draft != null -> Text(buildAnnotatedString {
                            withStyle(SpanStyle(color = Kx.danger)) { append("Черновик: ") }; append(chat.draft)
                        }, style = KxType.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        else -> Text(buildAnnotatedString {
                            chat.previewSender?.let { withStyle(SpanStyle(color = if (it == "Вы") Kx.text4 else Kx.text)) { append("$it: ") } }
                            append(chat.preview)
                        }, style = KxType.bodySmall.copy(color = if (chat.unread > 0 && chat.previewSender != "Вы" && !chat.muted) Kx.text2 else Kx.text4),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (chat.unreadMention) Box(Modifier.size(22.dp).clip(CircleShape).background(Kx.warning), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AlternateEmail, "Упоминание", Modifier.size(15.dp), tint = Kx.warningInk)
                }
                if (chat.unread > 0) KxBadge(if (chat.unread > 999) "999+" else chat.unread.toString(), muted = chat.muted)
                else if (chat.markedUnread) KxBadge("", muted = chat.muted, size = 14.dp)
            }
        }
    }
}

/** Swipe right to mark read (green "Прочитано" strip behind the row, as on the mockup). */
@Composable
fun SwipeToRead(enabled: Boolean, onRead: () -> Unit, content: @Composable () -> Unit) {
    if (!enabled) { content(); return }
    val x = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val limit = with(LocalDensity.current) { 104.dp.toPx() }
    Box(Modifier.height(72.dp).clip(RoundedCornerShape(0.dp))) {
        Row(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xFF1E5A3C), Color(0xFF2F7A55)))).padding(start = 22.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.MarkChatRead, null, tint = Color(0xFFDDF5E5))
            Text("Прочитано", style = KxType.style(13.sp, FontWeight.SemiBold, Color(0xFFDDF5E5)))
        }
        Box(Modifier.graphicsLayer { translationX = x.value }.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = { scope.launch { if (x.value > limit * .8f) onRead(); x.animateTo(0f, tween(250, easing = KxMotion.rise)) } },
                onDragCancel = { scope.launch { x.animateTo(0f) } }
            ) { _, d -> scope.launch { x.snapTo((x.value + d).coerceIn(0f, limit * 1.2f)) } }
        }) { content() }
    }
}

fun plural(n: Int, one: String, few: String, many: String): String {
    val m = kotlin.math.abs(n) % 100; val d = m % 10
    return if (m in 11..19) many else if (d == 1) one else if (d in 2..4) few else many
}
