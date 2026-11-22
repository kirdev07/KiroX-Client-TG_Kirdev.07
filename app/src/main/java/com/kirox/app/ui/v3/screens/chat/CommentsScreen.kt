package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private data class Cm(val id: Int, val name: String, val l: String, val c: Color, val time: String, val text: String, val likes: Int, val at: Int,
                      val mine: Boolean = false, val admin: Boolean = false, val to: String? = null, val replies: List<Cm> = emptyList())

// demo: needs store (channel post comments thread: load, send reply, react are not in KxStore yet)
private val Seed = listOf(
    Cm(1, "Дмитрий", "Д", Color(0xFF2F6E9E), "09:41", "Переходы наконец-то без костылей, перевёл два экрана за вечер.", 48, 1, replies = listOf(
        Cm(11, "Android Weekly", "A", Color(0xFF3F9A6C), "09:50", "Покажете в следующем выпуске? Пришлите гифку 🙂", 21, 2, admin = true, to = "Дмитрий"),
        Cm(12, "Мария", "М", Color(0xFFB07A2E), "10:02", "А на старых телефонах не тормозит?", 6, 3, to = "Дмитрий"))),
    Cm(2, "Алекс", "А", Color(0xFF6A4C9C), "10:15", "Прирост в списках чувствуется даже на эмуляторе.", 17, 4),
    Cm(3, "Анна", "А", Color(0xFFC0443C), "10:31", "Превью в тёмной теме — лучшая новость недели 🌙", 63, 5, replies = listOf(
        Cm(31, "Олег", "О", Color(0xFF5C6661), "10:40", "Согласен, раньше переключал вручную каждый раз", 4, 6, to = "Анна"))),
)
private data class Row2(val c: Cm, val child: Boolean, val parent: Cm?)

/** Board «Комментарии»: channel post, sortable threaded comments with likes, replies and a reply bar. */
@Composable
fun CommentsScreen(ctx: KxScreenCtx) {
    var list by remember { mutableStateOf(Seed) }
    var sort by remember { mutableIntStateOf(0) }
    val open = remember { mutableStateMapOf(1 to true) }
    val liked = remember { mutableStateMapOf<Int, Boolean>() }
    var pop by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf<Triple<Int, String, String>?>(null) }
    var more by remember { mutableStateOf(false) }
    var mute by remember { mutableStateOf(false) }
    var next by remember { mutableIntStateOf(100) }
    var fresh by remember { mutableIntStateOf(0) }
    var typing by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(6000); typing = false }
    val total = list.sumOf { 1 + it.replies.size }
    val rows = buildList {
        list.sortedByDescending { if (sort == 0) it.likes else it.at }.forEach { c -> add(Row2(c, false, null)); if (open[c.id] == true) c.replies.forEach { add(Row2(it, true, c)) } }
    }
    val can = draft.isNotBlank()

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", onClick = ctx.back)
            Column(Modifier.weight(1f)) {
                Text("$total ${plural(total, "комментарий", "комментария", "комментариев")}", style = ts(20, W800))
                Text("Android Weekly · обсуждение", style = ts(12, color = Kx.text3))
            }
            ChRound(if (mute) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications, if (mute) "Включить уведомления об ответах" else "Выключить уведомления об ответах",
                border = Color.Transparent, tint = if (mute) Kx.text3 else Kx.accent) { mute = !mute }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(Kx.w(.08f), Kx.w(.03f))))
                    .border(1.dp, Kx.w(.1f), RoundedCornerShape(22.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(34.dp).clip(CircleShape).cssGradient(135f, Kx.accentMid, Color(0xFF2F6E9E)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Campaign, null, Modifier.size(18.dp), tint = Kx.onAccent)
                        }
                        Column(Modifier.weight(1f)) { Text("Android Weekly", style = ts(14, W800)); Text("Сегодня в 09:30 · 18,4 тыс. просмотров", style = ts(11, color = Kx.text4)) }
                    }
                    Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF121816))
                        .ellipseGlow(.3f, .2f, .8f * .6f, .9f * .6f, Kx.accentMid.copy(alpha = .55f)).ellipseGlow(.85f, .9f, .7f * .6f, .8f * .6f, Color(0xFF2F6E9E).copy(alpha = .6f)),
                        contentAlignment = Alignment.Center) { Icon(Icons.Outlined.DeveloperMode, null, Modifier.size(46.dp), tint = Color.White.copy(alpha = .85f)) }
                    Text("Вышел Compose 1.9: общие переходы между экранами стали стабильными, ленивые списки быстрее на 30%, а превью теперь понимают тёмную тему. Делитесь, что уже попробовали.",
                        style = ts(14, lh = 20.3.sp), maxLines = if (more) 12 else 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp).animateContentSize())
                    Text(if (more) "Свернуть" else "Читать полностью", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(top = 4.dp).kxClick { more = !more })
                }
            }
            item {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Обсуждение", style = ts(15, W800), modifier = Modifier.weight(1f))
                    listOf("По реакциям", "Новые").forEachIndexed { i, label ->
                        val on = sort == i
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "s")
                        Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(bg).kxClick(label) { sort = i }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = ts(12, W700, if (on) Kx.accent else Kx.text2))
                        }
                    }
                }
            }
            items(rows, key = { it.c.id }) { r ->
                val c = r.c
                val isLiked = liked[c.id] == true
                val n = c.likes + if (isLiked) 1 else 0
                val isOpen = open[c.id] == true
                val av: Dp = if (r.child) 28.dp else 36.dp
                val isNew = c.id == fresh
                val flash = remember { Animatable(if (isNew) 1f else 0f) }
                LaunchedEffect(isNew) { if (isNew) { delay(300); flash.animateTo(0f, tween(1600)) } }
                Row(Modifier.fillMaxWidth().then(if (isNew) Modifier.chIn(dy = 10.dp) else Modifier).drawBehind { if (flash.value > 0f) drawRect(Kx.accent.copy(alpha = .16f * flash.value)) }
                    .padding(start = if (r.child) 60.dp else 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(av).clip(CircleShape).background(c.c), contentAlignment = Alignment.Center) { Text(c.l, style = ts(13, W800, Color.White)) }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (c.mine) "Вы" else c.name, style = ts(13, W800, if (c.mine) Kx.accent else Kx.text))
                            if (c.admin) Text("автор", style = ts(11, W800, Kx.accent), modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Kx.accent.copy(alpha = .14f)).padding(horizontal = 6.dp, vertical = 1.dp))
                            Text(c.time, style = ts(11, color = Kx.text4))
                        }
                        if (r.child && c.to != null) Text("↳ ${c.to}", style = ts(12, color = Kx.accent), modifier = Modifier.padding(top = 2.dp))
                        Text(c.text, style = ts(14, color = Color(0xFFE2E9E4), lh = 19.6.sp), modifier = Modifier.padding(top = 3.dp))
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Ответить", style = ts(12, W700, Kx.text3), modifier = Modifier.kxClick { reply = Triple(r.parent?.id ?: c.id, if (c.mine) "себе" else c.name, c.text) })
                            if (!r.child && c.replies.isNotEmpty()) Row(Modifier.kxClick { open[c.id] = !isOpen }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(if (isOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(16.dp), tint = Kx.accent)
                                Text(if (isOpen) "Скрыть ответы" else "${c.replies.size} ${plural(c.replies.size, "ответ", "ответа", "ответов")}", style = ts(12, W700, Kx.accent))
                            }
                        }
                    }
                    Column(Modifier.width(36.dp).kxClick(if (isLiked) "Убрать ❤️" else "Нравится") { liked[c.id] = !isLiked; pop = if (!isLiked) c.id else 0 },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val ink = if (isLiked) Kx.danger else Kx.text4
                        key(isLiked && pop == c.id) { Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, null, Modifier.size(20.dp).then(if (isLiked && pop == c.id) Modifier.heart() else Modifier), tint = ink) }
                        Text(if (n >= 1000) String.format("%.1fK", n / 1000f) else "$n", style = ts(11, W700, ink))
                    }
                }
            }
            if (typing) item {
                Row(Modifier.padding(start = 60.dp, end = 16.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { repeat(3) { BounceDot(it * 150, 5.dp, Kx.accent) } }
                    Text("Ирина пишет комментарий", style = ts(12, color = Kx.text4))
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Kx.bg.copy(alpha = .6f), Kx.bg)))
            .drawBehind { drawLine(Kx.w(.06f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
            AnimatedVisibility(reply != null, enter = fadeIn(tween(200))) {
                val rp = reply
                Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.accent.copy(alpha = .08f))
                    .drawBehind { drawRect(Kx.accent, size = size.copy(width = 3.dp.toPx())) }.padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Reply, null, Modifier.size(18.dp), tint = Kx.accent)
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = Kx.accent, fontWeight = FontWeight.Bold)) { append(if (rp?.second == "себе") "Вы" else rp?.second.orEmpty()) }
                        withStyle(SpanStyle(color = Kx.text3)) { append(" ${rp?.third.orEmpty()}") }
                    }, style = ts(13), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Box(Modifier.size(28.dp).clip(CircleShape).kxClick("Отменить ответ") { reply = null }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text3) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f))))
                    .border(1.dp, Kx.w(.12f), RoundedCornerShape(25.dp)).padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChInput(draft, { draft = it }, if (reply != null) "Ваш ответ…" else "Комментарий…", Modifier.weight(1f))
                    Icon(Icons.Outlined.Mood, null, Modifier.padding(horizontal = 6.dp), tint = Kx.text3)
                }
                Box(Modifier.size(50.dp).clip(CircleShape).then(if (can) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f))).kxClick("Отправить") {
                    if (!can) return@kxClick
                    val id = next
                    val c = Cm(id, "Вы", "Я", Kx.accentDeep, "10:" + minOf(59, 44 + (next - 100)).toString().padStart(2, '0'), draft.trim(), 0, 100 + id, mine = true)
                    val rp = reply
                    if (rp != null) { list = list.map { p -> if (p.id == rp.first) p.copy(replies = p.replies + c.copy(to = if (rp.second == "себе") "Вы" else rp.second)) else p }; open[rp.first] = true; reply = null }
                    else { list = list + c; sort = 1 }
                    draft = ""; next = id + 1; fresh = id
                }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, tint = if (can) Kx.onAccent else Color(0xFF5C6661)) }
            }
        }
    }
}

/** kxHeart: 1 → 1.4 → 1, .35s. */
@Composable
private fun Modifier.heart(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(350)) }
    return graphicsLayer { val t = p.value; val s = if (t < .4f) 1f + .4f * t / .4f else 1.4f - .4f * (t - .4f) / .6f; scaleX = s; scaleY = s }
}
