package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxChat
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.model.KxMessage
import com.kirox.app.ui.v3.theme.*

@Composable
internal fun PeekBubbles(msgs: List<KxMessage>?, modifier: Modifier = Modifier) {
    when {
        msgs == null -> C3Empty("Загрузка…", modifier)
        msgs.isEmpty() -> C3Empty("Сообщений нет", modifier)
        else -> Column(modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            msgs.takeLast(8).forEach { m ->
                Box(Modifier.fillMaxWidth(), contentAlignment = if (m.out) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp)).then(if (m.out) Modifier.moAccent() else Modifier.background(Kx.w(.08f)))
                        .padding(horizontal = 12.dp, vertical = 7.dp)) {
                        if (m.sender != null) Text(m.sender, style = mts(11, W8, MoAmber))
                        Text(m.text, style = mts(14, color = if (m.out) MoInk else Kx.text), maxLines = 5, overflow = TextOverflow.Ellipsis)
                        Text(m.time, style = mts(10, color = (if (m.out) MoInk else Kx.text).copy(alpha = .6f)), modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }
    }
}

/** ChatPeek on the real account: the last messages via store.peekHistory (nothing is marked read) and the chat-list actions. */
@Composable
internal fun ChatPeekLive(ctx: KxScreenCtx) {
    val store = ctx.store
    var open by remember { mutableStateOf<Long?>(ctx.c3Chat) }
    val chats = store.chats.take(30)
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoTitleHeader("Подсмотреть", ctx.back, sub = "Нажмите на чат — он не станет прочитанным")
            if (chats.isEmpty()) C3Empty(if (store.chatsLoading) "Загрузка…" else "Чатов нет", Modifier.weight(1f))
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(chats, key = { it.id }) { c -> PeekRow(c) { open = c.id } }
            }
        }
        open?.let { id ->
            val c = (store.chats + store.archivedChats).firstOrNull { it.id == id }
            val msgs = c3Load(id) { store.peekHistory(id, 12) }
            MoSheet({ open = null }) {
                Text(c?.title ?: store.header(id).title, style = mts(18, W8), maxLines = 1)
                Text(store.header(id).status, style = mts(12, color = MoSub), modifier = Modifier.padding(bottom = 10.dp))
                PeekBubbles(msgs, Modifier.heightIn(max = 360.dp))
                Row(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val unread = (c?.unread ?: 0) > 0 || c?.markedUnread == true
                    MoPill(if (unread) "Прочитано" else "Непрочитанным", Modifier.weight(1f), size = 12) {
                        if (unread) store.markRead(id) else store.markUnread(id); ctx.toast(if (unread) "Отмечено прочитанным" else "Отмечено непрочитанным"); open = null
                    }
                    MoPill(if (c?.pinned == true) "Открепить" else "Закрепить", Modifier.weight(1f), size = 12) { store.setPinned(id, c?.pinned != true); open = null }
                }
                Row(Modifier.padding(top = 6.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MoPill(if (c?.muted == true) "Включить звук" else "Без звука", Modifier.weight(1f), size = 12) { store.setMuted(id, c?.muted != true); open = null }
                    MoPill(if (c?.archived == true) "Из архива" else "В архив", Modifier.weight(1f), size = 12) { store.setArchived(id, c?.archived != true); open = null }
                    MoPill("Открыть", Modifier.weight(1f), size = 12, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent) { open = null; ctx.openChat(id) }
                }
            }
        }
    }
}

@Composable
private fun PeekRow(c: KxChat, onTap: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).kxClick(c.title, onClick = onTap).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(c.title, style = mts(15, W7), maxLines = 1)
            Text(listOfNotNull(c.previewSender, c.preview).joinToString(": "), style = mts(12, color = MoMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(c.time, style = mts(11, color = MoMuted))
            if (c.unread > 0) Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).background(if (c.muted) Kx.w(.2f) else Kx.accent).padding(horizontal = 7.dp, vertical = 1.dp)) {
                Text("${c.unread}", style = mts(11, W8, MoInk))
            }
        }
    }
}

/** NextUnread on the real account: channels of the main list with unread posts, in list order; posts via peekHistory (not marked read). */
@Composable
internal fun NextUnreadLive(ctx: KxScreenCtx) {
    val store = ctx.store
    val channels = store.chats.filter { it.kind == KxChatKind.Channel && (it.unread > 0 || it.id == ctx.c3Chat) }
    var cur by remember { mutableStateOf(ctx.c3Chat ?: channels.firstOrNull()?.id) }
    val c = channels.firstOrNull { it.id == cur } ?: channels.firstOrNull()
    val next = c?.let { now -> channels.dropWhile { it.id != now.id }.drop(1).firstOrNull { it.unread > 0 } ?: channels.firstOrNull { it.id != now.id && it.unread > 0 } }
    val posts = if (c != null) c3Load(c.id) { store.peekHistory(c.id, 6) } else null
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoTitleHeader(c?.title ?: "Каналы", ctx.back, sub = c?.let { if (it.unread > 0) "${it.unread} ${moPlural(it.unread, "непрочитанный", "непрочитанных", "непрочитанных")}" else "прочитано" })
            if (c == null) C3Empty("Каналов с непрочитанными постами нет", Modifier.weight(1f))
            else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) { PeekBubbles(posts) }
            Row(Modifier.fillMaxWidth().moTopLine().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (c != null) MoPill("Открыть канал", Modifier.weight(1f), height = 46.dp) { ctx.openChat(c.id) }
                MoPill(if (next != null) "Дальше: ${next.title}" else "Других непрочитанных нет", Modifier.weight(1.4f), height = 46.dp,
                    bg = if (next != null) Kx.accent.copy(alpha = .16f) else Kx.w(.05f), ink = if (next != null) Kx.accent else MoMuted) { if (next != null) cur = next.id }
            }
        }
    }
}
