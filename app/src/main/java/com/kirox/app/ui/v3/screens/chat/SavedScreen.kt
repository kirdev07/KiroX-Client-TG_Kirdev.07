package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.Kx

private data class SavedItem(val id: Long, val text: String, val tag: String?, val key: String?, val time: String, val from: String?, val fwd: String?, val img: Boolean)

// Demo list when the Saved chat has no messages. Tags: store.savedTags() (emoji, label, count) for the real Saved chat.
// demo: needs store (filtering by tag and the saved-from chat of each message are not in the store yet)
private val DemoSaved = listOf(
    SavedItem(1, "Купить билеты на субботу и попкорн", "🛒 Купить", "buy", "22:26", "Анна Ким", null, false),
    SavedItem(2, "Идея: тёмная тема с зелёным оттенком для kiroX", "💡 Идеи", "idea", "21:40", null, null, false),
    SavedItem(3, "Сборка прошла, проверить APK на телефоне", "⭐ Важное", "important", "19:02", "Команда kiroX", "Максим Ли", false),
    SavedItem(4, "Маршрут до озера", null, null, "вчера", "Илья Петров", "Илья Петров", true),
    SavedItem(5, "Статья про жидкое стекло в интерфейсах", "📚 Прочитать", "read", "пн", "Кино по пятницам", "Кино по пятницам", false),
)
private val SavedTags = listOf(Triple("important", "⭐", "Важное"), Triple("idea", "💡", "Идеи"), Triple("buy", "🛒", "Купить"), Triple("read", "📚", "Прочитать"))

@Composable
private fun gxLoadTags(store: com.kirox.app.ui.v3.model.KxStore, on: Boolean): List<KxSavedTag> =
    produceState(emptyList<KxSavedTag>(), on) { value = if (on) store.savedTags() else emptyList() }.value

/** Board «Избранное»: the Saved chat's messages (newest at the bottom), tags, «По чатам» grouping, note composer. */
@Composable
fun SavedScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val savedChat = store.chats.firstOrNull { it.kind == KxChatKind.Saved }
    val real = savedChat?.let { c -> store.messages(c.id).filter { it.kind != KxMsgKind.Service } }.orEmpty()
    val msgs = if (real.isNotEmpty()) real.asReversed().map {
        SavedItem(it.id, it.text.ifEmpty { it.fileName.ifEmpty { it.meta } }, null, null, it.time, it.forwardedFrom, it.forwardedFrom,
            it.kind == KxMsgKind.Photo || it.kind == KxMsgKind.Video)
    } else DemoSaved
    var byChats by rememberSaveable { mutableStateOf(false) }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var from by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    val realTags = gxLoadTags(store, real.isNotEmpty())
    val tags = if (real.isEmpty()) SavedTags else realTags.map { Triple(it.emoji, it.emoji, it.label) }
    fun tagCount(k: String) = if (real.isEmpty()) msgs.count { it.key == k } else realTags.firstOrNull { it.emoji == k }?.count ?: 0
    val list = msgs.filter { (tag == null || it.key == tag) && (from == null || it.from == from) }
    val sources = msgs.mapNotNull { it.from }.distinct()

    ChChatBg {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 46.dp, outline = true, onClick = ctx.back)
                Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).border(1.dp, Kx.w(.16f), RoundedCornerShape(27.dp)).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KxAvatar("И", 0, size = 42.dp, icon = Icons.Rounded.Bookmark, colorIndex = 7)
                    Column(Modifier.weight(1f)) {
                        Text("Избранное", style = ts(16, W700))
                        Text(when {
                            byChats -> "Сохранено из ${sources.size} ${plural(sources.size, "чата", "чатов", "чатов")}"
                            from != null || tag != null -> "Найдено: ${list.size}"
                            else -> "${msgs.size} ${plural(msgs.size, "сохранённое", "сохранённых", "сохранённых")}"
                        }, style = ts(12, color = Kx.text3))
                    }
                }
                ChRound(Icons.Outlined.Search, "Поиск", 46.dp, outline = true) { savedChat?.let { ctx.open("ChatSearch:${it.id}") } ?: ctx.open("ChatSearch") }
            }
            Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val vbg by animateColorAsState(if (byChats) Kx.accent else Kx.w(.07f), tween(250), label = "v")
                Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(vbg).kxClick { byChats = !byChats; tag = null; from = null }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Outlined.Forum, null, Modifier.size(17.dp), tint = if (byChats) Kx.onAccent else Kx.text2)
                    Text("По чатам", style = ts(13, W700, if (byChats) Kx.onAccent else Kx.text2))
                }
                tags.forEach { (k, e, label) ->
                    val on = tag == k
                    val bg by animateColorAsState(if (on) Kx.accent else Kx.w(.07f), tween(250), label = "t")
                    val ink = if (on) Kx.onAccent else Kx.text2
                    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).kxClick { if (real.isNotEmpty()) ctx.toast("Фильтр по тегам пока не переносится") else { tag = if (on) null else k; byChats = false } }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("$e $label ", style = ts(13, W700, ink))
                        Text("${tagCount(k)}", style = ts(13, W700, ink.copy(alpha = .7f)))
                    }
                }
            }
            Text("Теги в Избранном — функция Telegram Premium", style = ts(11, color = Kx.text4), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
            if (byChats) {
                LazyColumn(Modifier.fillMaxSize().padding(top = 4.dp), contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(sources) { i, name ->
                        val mine = msgs.filter { it.from == name }
                        Row(Modifier.fillMaxWidth().height(66.dp).chIn(i * 40).clip(RoundedCornerShape(18.dp)).kxClick { from = name; byChats = false; tag = null }.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val chat = store.chats.firstOrNull { it.title == name }
                            if (chat != null) ChatAvatar(chat, 48.dp) else KxAvatar(name, name.hashCode().toLong(), size = 48.dp)
                            Column(Modifier.weight(1f)) {
                                Text(name, style = ts(15, W600), maxLines = 1)
                                Text(mine.firstOrNull()?.text.orEmpty(), style = ts(13, color = Kx.text4), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Box(Modifier.heightIn(min = 22.dp).widthIn(min = 24.dp).clip(RoundedCornerShape(11.dp)).background(Kx.accent.copy(alpha = .16f)).padding(horizontal = 7.dp),
                                contentAlignment = Alignment.Center) { Text("${mine.size}", style = ts(12, W800, Kx.accent)) }
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = true, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(list, key = { it.id }) { m -> SavedBubble(m) }
                    from?.let { f -> item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Из чата «$f»", style = ts(12, W700, Kx.accent), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .12f))
                                .kxClick { from = null }.padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                    } }
                }
                Row(Modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 22.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                        .border(1.dp, Kx.w(.16f), RoundedCornerShape(28.dp)).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Прикрепить") { ctx.toast("Вложения откроются в чате «Избранное»") }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AddCircle, null, tint = Kx.text2)
                        }
                        ChInput(note, { note = it }, "Заметка себе…", Modifier.weight(1f))
                    }
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Kx.accent).kxClick(if (note.isBlank()) "Голосовое" else "Отправить") {
                        if (note.isNotBlank() && savedChat != null) { store.send(savedChat.id, note.trim()); note = "" }
                        else if (note.isBlank()) ctx.toast("Удерживайте, чтобы записать голосовое")
                    }, contentAlignment = Alignment.Center) {
                        Icon(if (note.isBlank()) Icons.Rounded.Mic else Icons.AutoMirrored.Rounded.Send, null, tint = Kx.onAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedBubble(m: SavedItem) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Column(Modifier.fillMaxWidth(.84f).wrapContentWidth(Alignment.End).chIn().clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
            .cssGradient(160f, Kx.accentDark, Color(0xFF1F4D36)).width(IntrinsicSize.Max)) {
            if (m.fwd != null) Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Forward, null, Modifier.size(14.dp), tint = Kx.accentLight)
                Text("Переслано от ${m.fwd}", style = ts(12, color = Kx.accentLight), maxLines = 1)
            }
            if (m.img) Box(Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp).fillMaxWidth().widthIn(min = 200.dp).height(110.dp).clip(RoundedCornerShape(14.dp))
                .cssGradient(150f, Color(0xFF3E7A5A), Color(0xFF0F1A14)))
            Text(m.text, style = ts(15, color = Color(0xFFE6F6EC), lh = 20.sp), modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp))
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (m.tag != null) Text(m.tag, style = ts(11, W700, Color(0xFFE6F6EC)), modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .22f)).padding(horizontal = 8.dp, vertical = 2.dp))
                Spacer(Modifier.weight(1f))
                Text(m.time, style = ts(11, color = Kx.accentLight))
            }
        }
    }
}
