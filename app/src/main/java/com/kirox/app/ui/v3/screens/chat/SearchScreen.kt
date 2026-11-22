package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.Kx

// demo: needs store (recent search queries are not kept by the store yet)
private val RecentQueries = listOf("кино", "сборка", "билеты", "фото с гор")
private val SearchTabs = listOf("Всё", "Чаты", "Сообщения", "Медиа", "Ссылки", "Файлы")

private class ChatHit(val chat: KxChat, val title: AnnotatedString, val sub: String)
private class MsgHit(val chat: KxChat, val text: AnnotatedString, val time: String, val key: String)

/** Board «Поиск»: global search over the store's chats, contacts and loaded messages. */
@Composable
fun SearchScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var recent by remember { mutableStateOf(RecentQueries) }
    val q = query.trim()
    val chats = store.chats

    val chatHits = remember(q, chats, store.contacts) {
        if (q.isEmpty()) emptyList() else {
            val fromChats = chats.mapNotNull { c ->
                splitHit(c.title, q)?.let { ChatHit(c, it, chatSub(store, c)) }
            }
            val known = fromChats.map { it.chat.id }.toSet()
            fromChats + store.contacts.filter { it.id !in known }.mapNotNull { p ->
                splitHit(p.name, q)?.let { ChatHit(KxChat(p.id, p.name, online = p.online, colorIndex = p.colorIndex), it, p.status) }
            }
        }
    }
    val msgHits = remember(q, chats) {
        if (q.isEmpty()) emptyList() else chats.flatMap { c ->
            store.messages(c.id).asReversed().filter { it.text.isNotEmpty() }.mapNotNull { m ->
                splitHit(m.text, q)?.let { MsgHit(c, it, m.time, "${c.id}:${m.id}") }
            }
        }
    }
    val showChats = (tab == 0 || tab == 1) && chatHits.isNotEmpty()
    val showMsgs = (tab == 0 || tab == 2) && msgHits.isNotEmpty()

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        KxGlow(diameter = 380.dp, x = (-90).dp, y = (-110).dp, alpha = .16f)
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", onClick = ctx.back)
                val border by animateColorAsState(if (q.isNotEmpty()) Kx.accent.copy(alpha = .45f) else Kx.w(.16f), tween(300), label = "fb")
                Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(Kx.w(.12f), Kx.w(.05f)))).border(1.dp, border, RoundedCornerShape(24.dp))
                    .topHighlight(Kx.w(.2f)).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.accent)
                    ChInput(query, { query = it }, "Чаты, сообщения, люди", Modifier.weight(1f), size = 16)
                    if (q.isNotEmpty()) Box(Modifier.size(32.dp).clip(CircleShape).background(Kx.w(.1f)).kxClick("Очистить") { query = "" },
                        contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SearchTabs.forEachIndexed { i, label ->
                    val on = i == tab
                    val bg by animateColorAsState(if (on) Kx.accent else Kx.w(.06f), tween(250), label = "tb")
                    val ink by animateColorAsState(if (on) Kx.onAccent else Kx.text2, tween(250), label = "ti")
                    Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(bg).kxClick { tab = i }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center) { Text(label, style = ts(12, W700, ink)) }
                }
            }
            if (q.isEmpty()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                    Text("Часто пишете", style = ts(14, W800), modifier = Modifier.padding(bottom = 10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        chats.filter { it.kind != KxChatKind.Saved && !it.archived }.take(5).forEach { c ->
                            Column(Modifier.width(58.dp).kxClick(c.title) { query = c.title.substringBefore(' ') },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                ChatAvatar(c, 54.dp)
                                Text(c.title.substringBefore(' '), style = ts(11), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (recent.isNotEmpty()) {
                        Row(Modifier.padding(top = 20.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Недавние запросы", style = ts(14, W800), modifier = Modifier.weight(1f))
                            Text("Очистить", style = ts(12, W700, Kx.accent), modifier = Modifier.kxClick { recent = emptyList() })
                        }
                        recent.forEach { r ->
                            Row(Modifier.fillMaxWidth().height(46.dp).kxClick(r) { query = r }, verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.History, null, Modifier.size(20.dp), tint = Kx.text4)
                                Text(r, style = ts(15, color = Kx.text2), modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.NorthWest, null, Modifier.size(18.dp), tint = Kx.text4)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp)) {
                    if (showChats) {
                        item { ChCaption("Чаты и люди · ${chatHits.size}") }
                        items(chatHits, key = { "c" + it.chat.id }) { h ->
                            Row(Modifier.fillMaxWidth().height(58.dp).chIn().clip(RoundedCornerShape(16.dp)).kxClick { ctx.openChat(h.chat.id) }.padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ChatAvatar(h.chat, 44.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(h.title, style = ts(15, W600), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(h.sub, style = ts(12, color = Kx.text4), maxLines = 1)
                                }
                            }
                        }
                    }
                    if (showMsgs) {
                        item { ChCaption("Сообщения · ${msgHits.size}", Modifier.padding(top = 10.dp)) }
                        items(msgHits, key = { "m" + it.key }) { h ->
                            Row(Modifier.fillMaxWidth().chIn().clip(RoundedCornerShape(16.dp)).kxClick { ctx.openChat(h.chat.id) }.padding(horizontal = 4.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ChatAvatar(h.chat, 44.dp)
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        KxLine(h.chat.title, ts(14, W600), Modifier.weight(1f))
                                        Text(h.time, style = ts(11, W500, Kx.text4))
                                    }
                                    Text(h.text, style = ts(13, color = Kx.text2, lh = 18.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    if (!showChats && !showMsgs) item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.SearchOff, null, Modifier.size(44.dp), tint = Kx.dashed)
                            Spacer(Modifier.height(8.dp))
                            Text("По запросу «$q» ничего нет", style = ts(15, color = Kx.text4), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}


/** «@anna · в сети», «Группа · 128 участников», «Канал · …» from the store's header. */
internal fun chatSub(store: KxStore, c: KxChat): String {
    val st = store.header(c.id).status
    return when (c.kind) {
        KxChatKind.Group -> if (st.isNotEmpty()) "Группа · $st" else "Группа"
        KxChatKind.Channel -> if (st.isNotEmpty()) "Канал · $st" else "Канал"
        KxChatKind.Saved -> "Ваши сохранённые сообщения"
        KxChatKind.Bot -> "Бот"
        else -> st.ifEmpty { if (c.online) "в сети" else "был(а) недавно" }
    }
}
