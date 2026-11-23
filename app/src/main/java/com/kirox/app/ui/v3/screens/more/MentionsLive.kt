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
import com.kirox.app.ui.v3.model.KxFound
import com.kirox.app.ui.v3.theme.*

/**
 * Mentions on the real account. With a chat (arg): its unread mentions («@») and unread reactions («❤») via
 * store.unreadMentions (SearchChatMessages with the unread filters); a tap opens the chat at that message.
 * Without a chat: the chats that have unread mentions or reactions.
 */
@Composable
internal fun MentionsLive(ctx: KxScreenCtx, chatId: Long?) {
    var chat by remember { mutableStateOf(chatId) }
    var reactions by remember { mutableStateOf(false) }
    val c = chat
    val found = if (c != null) c3Load(c, reactions) { ctx.store.unreadMentions(c, reactions) } else null
    val chats = ctx.store.chats.filter { it.unreadMention || it.unreadReaction }
    fun open(f: KxFound) { ctx.openChat(f.chatId); ctx.store.jumpTo(f.chatId, f.messageId) }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoTitleHeader(if (c == null) "Упоминания" else ctx.store.header(c).title, { if (c != null && chatId == null) chat = null else ctx.back() },
                sub = if (c == null) "Чаты с непрочитанными @ и реакциями" else if (reactions) "Новые реакции" else "Непрочитанные упоминания")
            if (c != null) Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoPill("@ Упоминания", bg = if (!reactions) Kx.accent.copy(alpha = .16f) else Kx.w(.06f), ink = if (!reactions) Kx.accent else Kx.text2) { reactions = false }
                MoPill("❤ Реакции", bg = if (reactions) Kx.accent.copy(alpha = .16f) else Kx.w(.06f), ink = if (reactions) Kx.accent else Kx.text2) { reactions = true }
            }
            when {
                c == null && chats.isEmpty() -> C3Empty("Непрочитанных упоминаний и реакций нет", Modifier.weight(1f))
                c == null -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chats, key = { it.id }) { ch ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick { chat = ch.id }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(ch.title, style = mts(15, W7), modifier = Modifier.weight(1f), maxLines = 1)
                            if (ch.unreadMention) Text("@", style = mts(16, W8, Kx.accent))
                            if (ch.unreadReaction) Text("❤", style = mts(15))
                        }
                    }
                }
                found == null -> C3Empty("Загрузка…", Modifier.weight(1f))
                found.isEmpty() -> C3Empty(if (reactions) "Новых реакций нет" else "Непрочитанных упоминаний нет", Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(found, key = { it.messageId }) { f ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick { open(f) }.padding(14.dp)) {
                            Text(f.text, style = mts(14), maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(f.time, style = mts(11, color = MoMuted), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
