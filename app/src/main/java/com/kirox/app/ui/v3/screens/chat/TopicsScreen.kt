package com.kirox.app.ui.v3.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.Kx

private val TCol = listOf("Зелёный" to Color(0xFF3F9A6C), "Синий" to Color(0xFF2F6E9E), "Фиолетовый" to Color(0xFF6A4C9C), "Янтарный" to Color(0xFFB07A2E),
    "Розовый" to Color(0xFFA8456A), "Бирюзовый" to Color(0xFF2E8A86))
private val TIcons: List<ImageVector> = listOf(Icons.Rounded.Tag, Icons.Rounded.Campaign, Icons.AutoMirrored.Rounded.Help, Icons.Rounded.BugReport, Icons.Rounded.Code, Icons.Rounded.Work,
    Icons.Rounded.School, Icons.Rounded.SportsEsports, Icons.Rounded.Palette, Icons.Rounded.MusicNote, Icons.Rounded.PhotoCamera, Icons.Rounded.RocketLaunch,
    Icons.Rounded.Lightbulb, Icons.Rounded.Event, Icons.Rounded.ShoppingBag, Icons.Rounded.Favorite)
private data class Topic(val name: String, val icon: Int, val c: Int, val who: String, val last: String, val time: String, val badge: Int, val pinned: Boolean = false, val closed: Boolean = false)

/** Board «Темы»: forum topics as rows or tiles, topic chat, and the new-topic sheet (color + icon). */
@Composable
fun TopicsScreen(ctx: KxScreenCtx) {
    // Opened as "Topics:<chatId>": the list is store.forumTopics(chatId); a tap opens the topic as the conversation (store.openTopic + openChat);
    // «Создать» = store.createTopic(chatId, title). demo: needs store (topic colour/icon are not sent / not read yet: drawn from the board's palette)
    val store = ctx.store
    val chatId = ctx.arg?.toLongOrNull()
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    val realTopics by produceState<List<com.kirox.app.ui.v3.model.KxTopic>?>(null, chatId, reload) { if (chatId != null) value = store.forumTopics(chatId) }
    val header = chatId?.let { store.header(it) }
    val list = remember { mutableStateListOf(
        Topic("Общий чат", 0, 0, "Дана", "Кто идёт на митап в субботу?", "15:40", 18, pinned = true),
        Topic("Объявления", 1, 3, "Админ", "Новые правила форума", "12:02", 1, pinned = true, closed = true),
        Topic("Вопросы новичков", 2, 1, "Олег", "Как подключить TDLib?", "11:15", 6),
        Topic("Баги и краши", 3, 4, "Тимур", "Падает на Android 14 при старте", "Вчера", 0),
        Topic("Compose", 4, 2, "Марина", "Покажу пример с анимацией", "Вчера", 3),
        Topic("Вакансии", 5, 5, "Алина", "Ищем Kotlin-разработчика", "Пн", 0)) }
    LaunchedEffect(realTopics) {
        val rt = realTopics ?: return@LaunchedEffect
        list.clear()
        rt.forEachIndexed { i, x ->
            val who = x.preview.substringBefore(": ", "")
            list += Topic(x.title, if (x.general) 0 else (i % TIcons.size), i % TCol.size, who, if (who.isEmpty()) x.preview else x.preview.substringAfter(": "), x.time, x.unread, x.pinned, x.closed)
        }
    }
    var open by remember { mutableIntStateOf(-1) }
    var grid by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var nName by remember { mutableStateOf("") }
    var nc by remember { mutableIntStateOf(0) }
    var ni by remember { mutableIntStateOf(0) }
    var nKey by remember { mutableIntStateOf(0) }
    val t = list.getOrNull(open)
    BackHandler(t != null) { open = -1 }
    fun openAt(i: Int) {
        val rt = realTopics?.getOrNull(i)
        if (chatId != null && rt != null) { store.openTopic(chatId, rt.id, rt.title); ctx.openChat(chatId); return }
        list[i] = list[i].copy(badge = 0); open = i
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.05f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 42.dp) { if (t != null) open = -1 else ctx.back() }
                if (t == null) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).cssGradient(140f, Kx.accentDeep, Color(0xFF1B2E3A)), contentAlignment = Alignment.Center) { Text((header?.title ?: "A").take(1).uppercase(), style = ts(16, W800, Color(0xFFDFF5E8))) }
                    Column(Modifier.weight(1f)) { Text(header?.title ?: "Android-разработка", style = ts(16, W800), maxLines = 1); Text(if (header != null) "форум · ${header.status}" else "форум · 2 140 участников", style = ts(12, color = Kx.text3)) }
                    Box(Modifier.size(42.dp).clip(CircleShape).kxClick("Поиск") { ctx.open(if (chatId != null) "ChatSearch:$chatId" else "ChatSearch") }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Search, null, tint = Kx.text) }
                } else {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(TCol[t.c].second), contentAlignment = Alignment.Center) { Icon(TIcons[t.icon], null, Modifier.size(22.dp), tint = Color.White) }
                    Column(Modifier.weight(1f)) { Text(t.name, style = ts(16, W800), maxLines = 1); Text("тема в «Android-разработка»", style = ts(12, color = Kx.text3), maxLines = 1) }
                }
            }
            if (t == null) Box(Modifier.weight(1f)) {
                Column {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Triple(false, Icons.AutoMirrored.Outlined.ViewList, "Списком"), Triple(true, Icons.Outlined.GridView, "Плиткой")).forEach { (v, icon, label) ->
                            val on = grid == v
                            val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "v")
                            Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).kxClick(label) { grid = v }.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(icon, null, Modifier.size(18.dp), tint = if (on) Kx.accent else Kx.text2); Text(label, style = ts(13, W700, if (on) Kx.accent else Kx.text2))
                            }
                        }
                    }
                    if (!grid) LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 100.dp)) {
                        itemsIndexed(list, key = { _, x -> x.name }) { i, x ->
                            Row(Modifier.fillMaxWidth().chIn(i * 40, 10.dp).alpha(if (x.closed) .75f else 1f).clip(RoundedCornerShape(18.dp)).kxClick(x.name) { openAt(i) }
                                .padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(46.dp).clip(CircleShape).background(TCol[x.c].second), contentAlignment = Alignment.Center) { Icon(TIcons[x.icon], null, tint = Color.White) }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        KxLine(x.name, ts(15, W700), Modifier.weight(1f))
                                        if (x.pinned) Icon(Icons.Rounded.PushPin, null, Modifier.size(14.dp).rotate(45f), tint = Kx.accent)
                                        if (x.closed) Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp), tint = Kx.text4)
                                        Text(x.time, style = ts(12, color = Kx.text4))
                                    }
                                    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(buildAnnotatedString { withStyle(SpanStyle(color = Kx.text2, fontWeight = FontWeight.Bold)) { append("${x.who}:") }; append(" ${x.last}") },
                                            style = ts(13, color = Kx.text4), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        if (x.badge > 0) KxBadge("${x.badge}")
                                    }
                                }
                            }
                        }
                    } else LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(list, key = { _, x -> x.name }) { i, x ->
                            Box(Modifier.height(118.dp).chIn(i * 40, 10.dp).alpha(if (x.closed) .75f else 1f).clip(RoundedCornerShape(22.dp))
                                .cssGradient(150f, TCol[x.c].second.copy(alpha = .2f), Kx.w(.03f)).border(1.dp, Kx.w(.07f), RoundedCornerShape(22.dp)).kxClick(x.name) { openAt(i) }.padding(12.dp)) {
                                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Box(Modifier.size(38.dp).clip(CircleShape).background(TCol[x.c].second), contentAlignment = Alignment.Center) { Icon(TIcons[x.icon], null, Modifier.size(20.dp), tint = Color.White) }
                                    Text(x.name, style = ts(14, W700, lh = 18.sp))
                                }
                                if (x.badge > 0) KxBadge("${x.badge}", Modifier.align(Alignment.TopEnd).offset(2.dp, 0.dp))
                            }
                        }
                    }
                }
                Row(Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 26.dp).height(56.dp).shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Kx.accentDeep)
                    .clip(RoundedCornerShape(20.dp)).cssGradient(140f, Kx.accentMid, Kx.accentDeep).kxClick { sheet = true; nName = ""; nc = 0; ni = 0 }.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AddComment, null, tint = Kx.onAccent); Text("Новая тема", style = ts(15, W800, Kx.onAccent))
                }
            } else key(open) {
                Column(Modifier.weight(1f).chIn(dx = 40.dp, dy = 0.dp, durationMs = 300, easing = androidx.compose.animation.core.CubicBezierEasing(.2f, .9f, .3f, 1f))) {
                    val msgs = listOf(
                        Triple("Марина", Color(0xFF6A4C9C) to Color(0xFFC7B5FF), if (t.closed) "Пожалуйста, ознакомьтесь с правилами 📌" else "Всем привет! Пишем сюда по теме «${t.name}»"),
                        Triple(t.who, Color(0xFF2F6E9E) to Color(0xFF9FCBFF), t.last),
                        Triple("Олег", Color(0xFF3F7A5A) to Color(0xFFA9E2C2), if (t.closed) "Принято 👍" else "Подписался на тему, интересно"))
                    Column(Modifier.weight(1f).fillMaxWidth().ellipseGlow(1f, 0f, .72f, .36f, TCol[t.c].second.copy(alpha = .2f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                        Text("Тема «${t.name}» создана", style = ts(12, color = Kx.text3), modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).background(Kx.w(.06f)).padding(horizontal = 12.dp, vertical = 5.dp))
                        msgs.forEach { (who, cols, text) ->
                            Row(Modifier.fillMaxWidth(.86f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(cols.first), contentAlignment = Alignment.Center) { Text(who.take(1), style = ts(13, W700, Color.White)) }
                                Column(Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)).background(Color(0xFF161B18)).padding(horizontal = 12.dp, vertical = 7.dp)) {
                                    Text(who, style = ts(12, W800, cols.second)); Text(text, style = ts(15, lh = 20.25.sp))
                                }
                            }
                        }
                    }
                    if (t.closed) Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = Kx.text4); Text("Тема закрыта, писать могут только админы", style = ts(13, color = Kx.text4), textAlign = TextAlign.Center)
                    } else Box(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 18.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f))
                        .border(1.dp, Kx.w(.1f), RoundedCornerShape(23.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                        Text("Сообщение в «${t.name}»", style = ts(15, color = Kx.text4), maxLines = 1)
                    }
                }
            }
        }
        ChSheet(sheet, { sheet = false }, scrim = Color.Black.copy(alpha = .5f), bg = Brush.verticalGradient(listOf(Color(0xFF121614), Color(0xFF121614)))) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp).imePadding()) {
                Text("Новая тема", style = ts(18, W800), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val col by animateColorAsState(TCol[nc].second, tween(250), label = "nc")
                    key(nKey) { Box(Modifier.size(56.dp).chPop(.6f, 300, fade = false).clip(CircleShape).background(col), contentAlignment = Alignment.Center) { Icon(TIcons[ni], null, Modifier.size(28.dp), tint = Color.White) } }
                    Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                        androidx.compose.foundation.text.BasicTextField(nName, { nName = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = ts(16, W600),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Kx.accent), decorationBox = { inner -> Box { if (nName.isEmpty()) Text("Название темы", style = ts(16, W600, Color(0xFF7C8781))); inner() } })
                    }
                }
                Text("Цвет", style = ts(13, color = Kx.text3), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp))
                Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TCol.forEachIndexed { i, (label, c) ->
                        Box(Modifier.size(36.dp).then(if (i == nc) Modifier.border(2.dp, c, CircleShape).padding(2.dp).border(2.dp, Color(0xFF121614), CircleShape) else Modifier)
                            .clip(CircleShape).background(c).kxClick(label) { nc = i; nKey++ })
                    }
                }
                Text("Значок", style = ts(13, color = Kx.text3), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp))
                TIcons.indices.chunked(8).forEach { row ->
                    Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { i ->
                            Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (i == ni) TCol[nc].second.copy(alpha = .27f) else Kx.w(.04f)).kxClick { ni = i; nKey++ },
                                contentAlignment = Alignment.Center) { Icon(TIcons[i], null, Modifier.size(21.dp), tint = if (i == ni) Color.White else Kx.text3) }
                        }
                    }
                }
                val can = nName.isNotBlank()
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).then(if (can) Modifier.cssGradient(100f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                    .kxClick {
                        if (can) {
                            if (chatId != null) {
                                val title = nName.trim(); sheet = false
                                scope.launch {
                                    val id = store.createTopic(chatId, title)
                                    if (id != null) { store.openTopic(chatId, id, title); reload++; ctx.openChat(chatId) }
                                    else ctx.toast(store.error ?: "Не удалось создать тему").also { store.clearError() }
                                }
                            } else { list.add(0, Topic(nName.trim(), ni, nc, "Вы", "Тема создана", "сейчас", 0)); sheet = false; open = 0 }
                        }
                    },
                    contentAlignment = Alignment.Center) { Text("Создать тему", style = ts(15, W800, if (can) Kx.onAccent else Color(0xFF5C6661))) }
            }
        }
    }
}
