package com.kirox.app.ui.v3.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.model.KxSharedTab
import com.kirox.app.ui.v3.theme.Kx

private val SG = listOf(Color(0xFFF2A65A) to Color(0xFFC0587E), Color(0xFF7EC8E3) to Color(0xFF3F9A6C), Color(0xFF141B3A) to Color(0xFF2C2F6E), Color(0xFFD9E4EE) to Color(0xFF5B7389),
    Color(0xFFF7D774) to Color(0xFF7A3A16), Color(0xFF3F9A6C) to Color(0xFF14243A), Color(0xFF9E3F5C) to Color(0xFF2E1420), Color(0xFFB07A2E) to Color(0xFF3A2A14), Color(0xFF6A4C9C) to Color(0xFF14121C))
private val Tabs = listOf("Медиа" to KxSharedTab.Media, "Файлы" to KxSharedTab.Files, "Ссылки" to KxSharedTab.Links, "Голос" to KxSharedTab.Voice, "Музыка" to KxSharedTab.Music)

// demo: used when the store has nothing shared for this chat (no arg / empty)
private val DemoFiles = listOf(listOf("PDF", "Билеты_на_поезд.pdf", "284 КБ · 20 нояб"), listOf("DOC", "Список покупок.docx", "18 КБ · 18 нояб"), listOf("ZIP", "Фото с дачи.zip", "42 МБ · 12 нояб"),
    listOf("XLS", "Бюджет поездки.xlsx", "36 КБ · 3 нояб"), listOf("APK", "kiroX-beta.apk", "31 МБ · 28 окт"))
private val DemoLinks = listOf(listOf("Рецепт паэльи", "Классический рецепт за 40 минут", "recipes.example.com"), listOf("Карта маршрута", "Поездка за город · 54 км", "maps.example.com"),
    listOf("Билеты в кино", "Сеанс в субботу, 19:30", "cinema.example.org"), listOf("Погода на выходные", "+3°, без осадков", "weather.example.net"))
private val DemoVoice = listOf(listOf("Анна", "Голосовое · 0:21 · сегодня", ""), listOf("Вы", "Голосовое · 0:06 · сегодня", ""), listOf("Анна", "Кружок · 0:15 · вчера", "circle"), listOf("Анна", "Голосовое · 1:02 · 18 нояб", ""))
private val DemoMusic = listOf(listOf("Морской бриз", "Lumen Band · 3:42"), listOf("Ночной город", "Neon Drive · 4:05"), listOf("Осень", "Тёплый ламповый · 2:58"), listOf("Дорога домой", "Tvoi Druz’ya · 3:21"))
private fun extColor(e: String) = when (e.uppercase()) { "PDF" -> Color(0xFFC0443C); "DOC", "DOCX" -> Color(0xFF2F6E9E); "ZIP", "RAR" -> Color(0xFFB07A2E); "XLS", "XLSX" -> Color(0xFF3F9A6C); else -> Color(0xFF6A4C9C) }
private val LinkCols = listOf(Color(0xFFB07A2E), Color(0xFF3F9A6C), Color(0xFFA8456A), Color(0xFF2F6E9E))

/** Board «Медиа и файлы»: lens tabs, month-grouped media grid with long-press selection + action bar, files, links, voice and music lists. arg = chatId. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedMediaScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = ctx.arg?.toLongOrNull() ?: store.chats.firstOrNull { it.kind == KxChatKind.Private }?.id ?: 0L
    val title = store.header(chatId).title
    var tab by remember { mutableIntStateOf(0) }
    var selMode by remember { mutableStateOf(false) }
    val sel = remember { mutableStateListOf<Int>() }
    var confirm by remember { mutableStateOf(false) }
    val items = store.shared(chatId, Tabs[tab].second)
    BackHandler(selMode) { selMode = false; sel.clear() }
    val months: List<Pair<String, List<Int>>> = if (tab == 0 && items.isEmpty()) listOf("Ноябрь" to (0 until 9).toList(), "Октябрь" to (9 until 15).toList())
        else items.indices.groupBy { items[it].month.ifEmpty { "Недавние" } }.toList()
    val mediaCount = if (items.isNotEmpty()) items.size else 15
    val sub = if (selMode) "Нажимайте, чтобы выбрать ещё" else when (tab) {
        0 -> "$mediaCount фото и видео"
        1 -> (items.size.takeIf { it > 0 } ?: 5).let { "$it ${plural(it, "файл", "файла", "файлов")}" }
        2 -> (items.size.takeIf { it > 0 } ?: 4).let { "$it ${plural(it, "ссылка", "ссылки", "ссылок")}" }
        3 -> "${items.size.takeIf { it > 0 } ?: 4} голосовых и кружка"
        else -> (items.size.takeIf { it > 0 } ?: 4).let { "$it ${plural(it, "трек", "трека", "треков")}" }
    }
    fun toggle(k: Int) { if (k in sel) sel.remove(k) else sel.add(k) }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(if (selMode) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack, if (selMode) "Отменить выбор" else "Назад") {
                    if (selMode) { selMode = false; sel.clear() } else ctx.back()
                }
                Column(Modifier.weight(1f)) { Text(if (selMode) "Выбрано: ${sel.size}" else title, style = ts(20, W800), maxLines = 1); Text(sub, style = ts(12, color = Kx.text3)) }
                Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Календарь") { ctx.open("JumpDate:$chatId") }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CalendarMonth, null, tint = Kx.text) }
            }
            Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.05f)).padding(4.dp)) {
                var w by remember { mutableIntStateOf(0) }
                val d = LocalDensity.current
                val lens by animateFloatAsState(tab.toFloat(), tween(400, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)), label = "lens")
                Box(Modifier.fillMaxWidth().height(36.dp).onSizeChanged { w = it.width }) {
                    Box(Modifier.graphicsLayer { translationX = lens * w / 5f }.width(with(d) { (w / 5f).toDp() }).height(36.dp).clip(RoundedCornerShape(18.dp))
                        .background(Kx.accent.copy(alpha = .16f)).border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(18.dp)))
                    Row(Modifier.fillMaxSize()) {
                        Tabs.forEachIndexed { i, (label, _) ->
                            val ink by animateColorAsState(if (i == tab) Kx.accent else Kx.text3, tween(200), label = "ti")
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).kxClick(label) { tab = i; selMode = false; sel.clear() }, contentAlignment = Alignment.Center) {
                                Text(label, style = ts(12, W800, ink), maxLines = 1)
                            }
                        }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 100.dp)) {
                if (tab == 0) months.forEach { (name, ks) ->
                    if (ks.isEmpty()) return@forEach
                    item(key = "h$name") { Text(name, style = ts(13, W800, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 6.dp)) }
                    ks.chunked(3).forEach { row ->
                        item(key = "r$name${row.first()}") {
                            Row(Modifier.padding(bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                row.forEach { k ->
                                    val it = items.getOrNull(k)
                                    val on = k in sel
                                    val scale by animateFloatAsState(if (on) .9f else 1f, tween(200), label = "s")
                                    val r by animateDpAsState(if (on) 14.dp else 6.dp, tween(200), label = "r")
                                    val g = SG[((it?.look ?: k) * 5) % SG.size]
                                    Box(Modifier.weight(1f).aspectRatio(1f).chPop(.92f, 300, k * 20).graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(r))
                                        .cssGradient(150f, g.first, g.second)
                                        .combinedClickable(onClickLabel = "Медиа ${k + 1}", onLongClick = { if (selMode) toggle(k) else { selMode = true; sel.clear(); sel.add(k) } }) {
                                            if (selMode) toggle(k) else ctx.open((if (it?.video != null) "VideoPlayer:" else "Viewer:") + "$chatId:${it?.messageId ?: 0}")
                                        }) {
                                        val video = it?.video ?: if (k % 4 == 2) "0:${(10 + (k * 7) % 50).toString().padStart(2, '0')}" else null
                                        if (video != null) Row(Modifier.align(Alignment.BottomStart).padding(6.dp).height(18.dp).clip(RoundedCornerShape(9.dp)).background(Color.Black.copy(alpha = .5f))
                                            .padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(12.dp), tint = Kx.text); Text(video, style = ts(11, W700))
                                        }
                                        if (selMode) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Black.copy(alpha = .25f))
                                            .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Kx.onAccent) }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                if (tab == 1) {
                    val rows = if (items.isNotEmpty()) items.map { listOf(it.fileExt.uppercase().ifEmpty { "FILE" }.take(4), it.title, it.sub) } else DemoFiles
                    itemsIndexed(rows) { i, f -> ListRow(i, { Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(extColor(f[0])), contentAlignment = Alignment.Center) {
                        Text(f[0], style = ts(11, androidx.compose.ui.text.font.FontWeight.Black, Color.White)) } }, f[1], f[2], trailing = Icons.Outlined.Download) }
                }
                if (tab == 2) {
                    val rows = if (items.isNotEmpty()) items.map { listOf(it.title, it.sub, "") } else DemoLinks
                    itemsIndexed(rows) { i, l -> ListRow(i, { Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(LinkCols[i % 4]), contentAlignment = Alignment.Center) {
                        Text(l[0].take(1).uppercase(), style = ts(20, W800, Color.White)) } }, l[0], l[1], link = l[2].ifEmpty { null }) }
                }
                if (tab == 3) {
                    val rows = if (items.isNotEmpty()) items.map { listOf(it.title, it.sub, it.video.orEmpty()) } else DemoVoice
                    itemsIndexed(rows) { i, v -> ListRow(i, { Box(Modifier.size(44.dp).clip(CircleShape).cssGradient(140f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                        Icon(if (v[2] == "circle") Icons.Rounded.RadioButtonChecked else Icons.Rounded.Mic, null, tint = Kx.onAccent) } }, v[0], v[1]) }
                }
                if (tab == 4) {
                    val rows = if (items.isNotEmpty()) items.map { listOf(it.title, it.sub) } else DemoMusic
                    itemsIndexed(rows) { i, m -> val g = SG[listOf(1, 2, 4, 5)[i % 4]]
                        ListRow(i, { Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).cssGradient(150f, g.first, g.second), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = .85f)) } }, m[0], m[1], trailing = Icons.Outlined.PlayCircle) }
                }
            }
        }
        AnimatedVisibility(selMode && sel.isNotEmpty(), Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 22.dp),
            enter = slideInVertically(tween(300, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f))) { it * 2 }) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color(0xF0161B18)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(30.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                // Real items carry messageId / fileId (KxSharedItem): forward opens «Переслать» with the first one (that board takes one message),
                // «Сохранить» downloads the files, «В чате» jumps to the first one, delete (after the confirm) removes the messages for me.
                val picked = sel.mapNotNull { items.getOrNull(it) }
                BarAct(Icons.AutoMirrored.Outlined.Forward, "Переслать", Kx.text) {
                    val first = picked.firstOrNull { it.messageId != 0L }
                    selMode = false; sel.clear(); ctx.open(if (first != null) "Forward:$chatId:${first.messageId}" else "Forward")
                }
                BarAct(Icons.Outlined.Download, "Сохранить", Kx.text) {
                    val files = picked.filter { it.fileId != 0 }
                    selMode = false; sel.clear()
                    if (files.isNotEmpty()) { files.forEach { store.download(it.fileId) }; ctx.toast("Файлы загружаются на телефон") } else ctx.toast("Сохранено в галерею")
                }
                BarAct(Icons.AutoMirrored.Outlined.Chat, "В чате", Kx.text) {
                    val first = picked.firstOrNull { it.messageId != 0L }
                    selMode = false; sel.clear(); ctx.openChat(chatId); first?.let { store.jumpTo(chatId, it.messageId) }
                }
                BarAct(Icons.Outlined.Delete, "Удалить", Kx.danger) { confirm = true }
            }
        }
        ChConfirm(if (confirm) "Удалить выбранное?" else null, "Сообщения с этими файлами удалятся у вас. В личном чате можно удалить и у собеседника.", "Удалить", { confirm = false }) {
            val ids = sel.mapNotNull { items.getOrNull(it)?.messageId?.takeIf { id -> id != 0L } }
            if (ids.isNotEmpty()) store.delete(chatId, ids, false)
            confirm = false; selMode = false; sel.clear()
        }
    }
}

@Composable
private fun BarAct(icon: ImageVector, label: String, ink: Color, tap: () -> Unit) {
    Column(Modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).kxClick(label, onClick = tap).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) { Icon(icon, null, tint = ink); Text(label, style = ts(11, W600, ink)) }
}

@Composable
private fun ListRow(i: Int, lead: @Composable () -> Unit, title: String, sub: String, link: String? = null, trailing: ImageVector? = null) {
    Row(Modifier.fillMaxWidth().chIn(i * 40, 8.dp, 250).padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = if (link != null) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        lead()
        Column(Modifier.weight(1f)) {
            Text(title, style = ts(15, if (link != null || trailing == Icons.Outlined.PlayCircle || trailing == null) W700 else W600), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = ts(if (link != null) 13 else 12, color = if (link != null) Kx.text3 else Kx.text4), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            if (link != null) Text(link, style = ts(12, color = Kx.accent), modifier = Modifier.padding(top = 2.dp))
        }
        if (trailing != null) Icon(trailing, null, tint = Kx.accent)
    }
}

