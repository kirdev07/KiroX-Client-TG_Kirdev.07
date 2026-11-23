package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.model.KxFound
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

/**
 * Hashtag search on the real account: store.searchMessages("#tag") over all chats, split into my chats / channels / Saved
 * by the chat's kind. Telegram's public-post search (paid with Stars beyond a free quota) is not used.
 */
@Composable
internal fun HashtagLive(ctx: KxScreenCtx) {
    var tag by remember { mutableStateOf(ctx.arg?.removePrefix("#")?.takeIf { it.toLongOrNull() == null }.orEmpty()) }
    var sc by remember { mutableIntStateOf(0) }
    var found by remember { mutableStateOf<List<KxFound>?>(emptyList()) }
    LaunchedEffect(tag) {
        val t = tag.trim().removePrefix("#")
        if (t.isEmpty()) { found = emptyList(); return@LaunchedEffect }
        found = null; delay(400)
        found = ctx.store.searchMessages("#$t")
    }
    val kinds = (ctx.store.chats + ctx.store.archivedChats).associate { it.id to it.kind }
    fun scopeOf(f: KxFound) = when (kinds[f.chatId]) { KxChatKind.Channel -> 1; KxChatKind.Saved -> 2; else -> if (f.chatId == ctx.store.me.id) 2 else 0 }
    val list = found.orEmpty().filter { scopeOf(it) == sc }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoBack(ctx.back)
                Row(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.08f)).border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(23.dp))
                    .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("#", style = mts(18, W8, Kx.accent))
                    MoInput(tag, { tag = it.removePrefix("#") }, "хэштег", Modifier.weight(1f), size = 16)
                }
            }
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Мои чаты", "Каналы", "Избранное").forEachIndexed { i, t ->
                    val on = sc == i; val n = found.orEmpty().count { scopeOf(it) == i }
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .18f) else Color.Transparent)
                        .kxClick { sc = i }, contentAlignment = Alignment.Center) { Text(t + if (n > 0) " · $n" else "", style = mts(13, W7, if (on) Kx.accent else MoSub), maxLines = 1) }
                }
            }
            when {
                tag.isBlank() -> C3Empty("Введите хэштег — найдём его во всех ваших чатах", Modifier.weight(1f))
                found == null -> C3Empty("Ищем…", Modifier.weight(1f))
                list.isEmpty() -> C3Empty("Ничего не нашлось", Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { "${it.chatId}:${it.messageId}" }) { f ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick { ctx.openChat(f.chatId); ctx.store.jumpTo(f.chatId, f.messageId) }.padding(14.dp)) {
                            Row { Text(f.chatTitle, style = mts(13, W8, Kx.accent), modifier = Modifier.weight(1f), maxLines = 1); Text(f.time, style = mts(11, color = MoMuted)) }
                            Text(f.text, style = mts(14), maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
