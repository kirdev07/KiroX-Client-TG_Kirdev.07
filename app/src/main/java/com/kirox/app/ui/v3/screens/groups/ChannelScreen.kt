package com.kirox.app.ui.v3.screens.groups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.model.*
import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

// Board posts for the demo store. A real channel (ctx.arg = chat id) shows store.messages / header / postStats;
// sponsored messages and the per-hour views chart are not in the store yet, so they show only on the demo.
private data class CPost(val title: String, val text: String, val image: Boolean, val views: String, val time: String, val comments: Int, val reacts: List<Pair<String, Int>>,
                         val id: Long = 0, val path: String? = null, val mine: Set<String> = emptySet(), val day: String? = null)
private val CPosts = listOf(
    CPost("Премьера недели: «Северный ветер»", "Красивая драма о дороге домой. В кино с пятницы, сеансы с 18:00.", true, "2,4 тыс.", "18:20", 34, listOf("🔥" to 214, "❤️" to 96)),
    CPost("Опрос на выходные", "Комедия или триллер? Пишите в комментариях, выберем фильм для субботы.", false, "1,8 тыс.", "20:05", 61, listOf("😂" to 58, "👍" to 41)),
)
private val CBars = listOf(20, 35, 30, 55, 80, 100, 92, 70, 60, 48, 40, 28)
private val CardBg = Color(0xFF141917)

/** Board «Канал»: channel header pill, posts with reactions, sponsored card, subscribe/mute bar, menu, leave confirm, post stats sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChannelScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = ctx.arg?.toLongOrNull()
    val live = store !is com.kirox.app.ui.v3.demo.KxDemoStore && chatId != null
    if (live) DisposableEffect(chatId) {
        val wasOpen = store.historyChatId == chatId
        if (!wasOpen) store.openChat(chatId!!)
        onDispose { if (!wasOpen) store.closeChat(chatId!!) }
    }
    val header = if (live) store.header(chatId!!) else null
    val posts = if (!live) CPosts else store.messages(chatId!!).filter { it.kind != KxMsgKind.Service }.takeLast(30).map { m ->
        val lines = m.text.split('\n', limit = 2)
        val titled = lines.size > 1 && lines[0].length <= 80
        CPost(if (titled) lines[0] else "", if (titled) lines[1] else m.text, m.media != null || m.thumb != null || m.kind == KxMsgKind.Photo || m.kind == KxMsgKind.Video, m.views, m.time, m.comments, m.reactions.map { it.emoji to it.count },
            id = m.id, path = if (m.kind == KxMsgKind.Photo) m.media ?: m.thumb else m.thumb ?: m.media, mine = m.reactions.filter { it.mine }.map { it.emoji }.toSet(), day = m.dayHeader)
    }
    var subDemo by rememberSaveable { mutableStateOf(false) }
    var mutedDemo by rememberSaveable { mutableStateOf(false) }
    val sub = header?.let { it.composer != KxComposer.Join } ?: subDemo
    val muted = header?.muted ?: mutedDemo
    val scope = rememberCoroutineScope()
    var postStats by remember { mutableStateOf<KxPostStats?>(null) }
    val mine = remember { mutableStateMapOf<String, Boolean>() }
    var popped by remember { mutableStateOf<String?>(null) }
    var popTick by remember { mutableIntStateOf(0) }
    var stats by remember { mutableStateOf(false) }
    var adminDemo by rememberSaveable { mutableStateOf(false) }
    val admin = header?.let { it.composer == KxComposer.Write } ?: adminDemo
    var more by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    var ad by rememberSaveable { mutableStateOf(true) }
    var why by remember { mutableStateOf(false) }
    val tip = rememberGxTip()

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Box(Modifier.fillMaxSize().cssRadial(.7f, .4f, 1f, .15f, Color(0x24D08A3C)).cssRadial(.6f, .35f, 0f, .8f, Color(0x1A3F9A6C)))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Spacer(Modifier.height(76.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val minH = maxHeight
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState(), reverseScrolling = true)) {
                    Column(Modifier.fillMaxWidth().heightIn(min = minH).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)) {
                        if (!live) Text("Сегодня", style = KxType.style(12.sp, FontWeight.SemiBold, Kx.text3),
                            modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).background(Kx.w(.06f)).padding(horizontal = 12.dp, vertical = 4.dp))
                        posts.forEachIndexed { i, p ->
                            p.day?.let { d ->
                                Text(d, style = KxType.style(12.sp, FontWeight.SemiBold, Kx.text3),
                                    modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).background(Kx.w(.06f)).padding(horizontal = 12.dp, vertical = 4.dp))
                            }
                            Column(Modifier.fillMaxWidth().gxRise(0, 400).clip(RoundedCornerShape(22.dp)).background(CardBg)) {
                                if (p.image) Box(Modifier.fillMaxWidth().height(150.dp).cssLinearStops(150f, 0f to Color(0xFFC97A58), .75f to Color(0xFF3A2014)), contentAlignment = Alignment.Center) {
                                    if (p.path == null) Icon(Icons.Rounded.Theaters, null, Modifier.size(46.dp), tint = Kx.w(.3f))
                                    KxImage(p.path, Modifier.matchParentSize())
                                }
                                Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp)) {
                                    if (p.title.isNotEmpty()) Text(p.title, style = KxType.style(15.sp, FontWeight.Bold))
                                    Text(p.text, style = KxType.style(14.sp, color = Kx.text2, lineHeight = 19.sp), modifier = Modifier.padding(top = 3.dp))
                                }
                                FlowRow(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    p.reacts.forEach { (e, n) ->
                                        val key = "$i$e"
                                        if (live) {
                                            val on = e in p.mine
                                            CReact(e, n, on, if (popped == key) popTick else 0) { store.react(chatId!!, p.id, e); popped = key; popTick++ }
                                        } else {
                                            val on = mine[key] == true
                                            CReact(e, n + if (on) 1 else 0, on, if (popped == key) popTick else 0) { mine[key] = !on; popped = key; popTick++ }
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Row(Modifier.height(30.dp).kxClick("Статистика поста") {
                                        if (!admin) tip.say("Статистика видна только админам канала")
                                        else if (!live) stats = true
                                        else scope.launch { postStats = store.postStats(chatId!!, p.id); if (postStats != null) stats = true else tip.say("Статистика этого поста пока недоступна") }
                                    },
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Outlined.Visibility, null, Modifier.size(15.dp), tint = Kx.text4)
                                        Text("${p.views} · ${p.time}", style = KxType.style(11.sp, color = Kx.text4))
                                    }
                                }
                                if (!live || p.comments > 0) Row(Modifier.fillMaxWidth().height(42.dp).hairline(top = true).kxClick { if (live) ctx.open("Comments:$chatId:${p.id}") else ctx.open("Comments") }.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row {
                                        Box(Modifier.size(20.dp).clip(CircleShape).background(CardBg).padding(2.dp).clip(CircleShape).background(Color(0xFF4B3F7A)))
                                        Box(Modifier.offset(x = (-6).dp).size(20.dp).clip(CircleShape).background(CardBg).padding(2.dp).clip(CircleShape).background(Color(0xFF2D4E73)))
                                    }
                                    Text("${p.comments} ${gxPlural(p.comments, "комментарий", "комментария", "комментариев")}", style = KxType.style(13.sp, FontWeight.SemiBold, Kx.accent),
                                        modifier = Modifier.weight(1f).offset(x = (-6).dp))
                                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = Kx.accent)
                                }
                            }
                        }
                        if (ad && !live) CAd(why, onWhy = { why = !why }, onHide = { tip.say("Скрыть рекламу можно с Telegram Premium") })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!sub) {
                    GxPillButton("Подписаться", Modifier.weight(1f).gxRise(0, 300), height = 56.dp, brush = Brush.linearGradient(listOf(Kx.accent, Kx.accentDeep)),
                        ink = Kx.onAccent, size = 16, weight = FontWeight.ExtraBold, icon = Icons.Rounded.AddCircle) { if (live) store.joinChat(chatId!!) else { subDemo = true; tip.say("Вы подписались на канал") } }
                } else {
                    CBarButton(if (muted) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications, if (muted) "Включить звук" else "Без звука",
                        if (muted) Kx.danger else Kx.text, Modifier.weight(1f)) { if (live) store.setMuted(chatId!!, !muted) else mutedDemo = !muted }
                    CBarButton(Icons.Outlined.Forum, "Обсуждение", Kx.text, Modifier.weight(1f)) {
                        if (!live) ctx.open("Comments")
                        else posts.lastOrNull { it.comments > 0 }?.let { ctx.open("Comments:$chatId:${it.id}") } ?: tip.say("У канала нет обсуждения")
                    }
                }
            }
        }
        // Header (absolute top 10)
        Row(Modifier.statusBarsPadding().padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CRoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", ctx.back)
            Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).border(1.dp, Kx.w(.16f), RoundedCornerShape(27.dp)).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    GxLetter("", Color(0xFFD08A3C), Color(0xFF7A4A16), Color(0xFFFFE9C9), size = 42.dp, icon = Icons.Rounded.Movie, iconSize = 22.dp)
                    KxImage(header?.avatar, Modifier.size(42.dp).clip(CircleShape), maxPx = 160)
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        KxLine(header?.title ?: "Кино по пятницам", KxType.style(15.sp, FontWeight.Bold), Modifier.weight(1f, false))
                        if (!live) Icon(Icons.Rounded.Verified, "Подтверждён", Modifier.size(16.dp), tint = Kx.accent)
                    }
                    Text(header?.let { h -> if (h.members > 0) "${h.members.toString().reversed().chunked(3).joinToString(" ").reversed()} ${gxPlural(h.members, "подписчик", "подписчика", "подписчиков")}" else h.status }
                        ?: if (sub) "24 501 подписчик" else "24 500 подписчиков", style = KxType.style(12.sp, color = Kx.text3), maxLines = 1)
                }
            }
            CRoundButton(Icons.Rounded.MoreVert, "Ещё") { more = true }
        }

        // More menu
        if (more) BackHandler { more = false }
        GxScrim(more, .35f) { more = false }
        AnimatedVisibility(more, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 64.dp, end = 12.dp), enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            Column(Modifier.width(250.dp).shadow(24.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E2421))
                .border(1.dp, Kx.w(.12f), RoundedCornerShape(20.dp)).padding(6.dp)) {
                if (!live) CMenuItem(Icons.Outlined.AdminPanelSettings, if (admin) "Смотреть как подписчик" else "Макет: смотреть как админ") { adminDemo = !adminDemo; more = false }
                CMenuItem(Icons.Outlined.Search, "Поиск по каналу") { more = false; ctx.open("ChatSearch") }
                if (sub) CMenuItem(Icons.AutoMirrored.Outlined.Logout, "Покинуть канал", danger = true) { more = false; leave = true }
            }
        }
        GxTip(tip)
        GxConfirm(leave, "Покинуть «${header?.title ?: "Кино по пятницам"}»?", "Канал пропадёт из списка чатов. Вернуться можно в любой момент через поиск.",
            confirm = "Покинуть", cancel = "Остаться", onCancel = { leave = false }) {
            leave = false
            if (live) store.deleteChat(chatId!!) else { subDemo = false; mutedDemo = false }
            tip.say("Вы покинули канал")
        }
        GxSheet(stats, onClose = { stats = false }) {
            Text("Статистика поста", style = KxType.style(19.sp, FontWeight.ExtraBold))
            val ps = postStats.takeIf { live }
            val total = ps?.reactions ?: (310 + mine.count { it.key.startsWith("0") && it.value })
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CStat(Icons.Outlined.Visibility, Kx.accent, ps?.views?.toString() ?: "2 418", "Просмотры", Modifier.weight(1f))
                    CStat(Icons.AutoMirrored.Outlined.Forward, Color(0xFFA9CBF5), ps?.forwards?.toString() ?: "86", "Репосты", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CStat(Icons.Rounded.Favorite, Color(0xFFFF9CC8), "$total", "Реакции", Modifier.weight(1f))
                    CStat(Icons.Outlined.ChatBubbleOutline, Color(0xFFFFD27A), ps?.comments?.toString() ?: "34", "Комментарии", Modifier.weight(1f))
                }
            }
            if (!live) Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(12.dp)) {
                Text("Просмотры по часам", style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(bottom = 8.dp))
                Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CBars.forEachIndexed { i, h ->
                        Box(Modifier.weight(1f).fillMaxHeight(h / 100f).gxGrowY(600, i * 40)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(Brush.verticalGradient(listOf(Kx.accent, Kx.accentDeep))))
                    }
                }
            }
        }
    }
}

@Composable
private fun CReact(e: String, n: Int, on: Boolean, popKey: Int, onTap: () -> Unit) {
    val bg by animateColorAsState(if (on) Color(0x338FD3AC) else Kx.w(.06f), tween(200), label = "rb")
    val bd by animateColorAsState(if (on) Color(0x808FD3AC) else Color.Transparent, tween(200), label = "rbd")
    val s = remember { Animatable(1f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(popKey) {
        if (popKey > 0 && !reduced) { s.snapTo(1f); s.animateTo(1.3f, tween(160)); s.animateTo(1f, tween(240)) }
    }
    Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(15.dp)).kxClick(e, onClick = onTap).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(e, style = KxType.style(13.sp), modifier = Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value })
        Text("$n", style = KxType.style(13.sp))
    }
}

@Composable
private fun CAd(why: Boolean, onWhy: () -> Unit, onHide: () -> Unit) {
    Column(Modifier.fillMaxWidth().gxRise(0, 400).clip(RoundedCornerShape(22.dp)).background(CardBg).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))
        .padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.height(20.dp).clip(RoundedCornerShape(10.dp)).background(Kx.w(.08f)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                Text("Реклама", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.text2))
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).kxClick(onClick = onWhy).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                Text("Почему я это вижу?", style = KxType.style(12.sp, FontWeight.Bold, Kx.text3))
            }
            Box(Modifier.size(36.dp).clip(CircleShape).kxClick("Скрыть рекламу", onClick = onHide), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text3)
            }
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GxLetter("", Color(0xFF2F6E9E), Color(0xFF0B1F2E), Color(0xFFCFE6FF), size = 44.dp, shape = RoundedCornerShape(12.dp), icon = Icons.AutoMirrored.Rounded.MenuBook, iconSize = 22.dp)
            Column(Modifier.weight(1f)) {
                Text("Книжный клуб онлайн", style = KxType.style(14.sp, FontWeight.ExtraBold))
                Text("Одна книга в месяц и обсуждение по пятницам", style = KxType.style(13.sp, color = Kx.text2, lineHeight = 18.sp))
            }
        }
        AnimatedVisibility(why) {
            Text("Рекламу в больших публичных каналах показывает Telegram. Она не зависит от ваших личных данных и чатов. kiroX показывает её по правилам Telegram API.",
                style = KxType.style(12.sp, color = Kx.text3, lineHeight = 17.sp),
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Kx.w(.05f)).padding(horizontal = 10.dp, vertical = 8.dp))
        }
        GxPillButton("Открыть канал", Modifier.padding(top = 10.dp).fillMaxWidth(), height = 44.dp, bg = Color(0x1F8FD3AC), ink = Kx.accent, size = 14, weight = FontWeight.ExtraBold) {}
    }
}

@Composable
private fun CRoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(46.dp).clip(CircleShape).border(1.dp, Kx.w(.16f), CircleShape).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(24.dp), tint = Kx.text)
    }
}

@Composable
private fun CBarButton(icon: ImageVector, label: String, ink: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.gxRise(0, 300).height(56.dp).clip(RoundedCornerShape(28.dp)).background(Kx.sheetBottom).border(1.dp, Kx.w(.1f), RoundedCornerShape(28.dp)).kxClick(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(24.dp), tint = ink)
        Text(label, style = KxType.style(15.sp, FontWeight.Bold, ink), maxLines = 1)
    }
}

@Composable
private fun CMenuItem(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).kxClick(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (danger) Kx.danger else Kx.accent)
        Text(label, style = KxType.style(15.sp, color = if (danger) Kx.danger else Kx.text), maxLines = 1)
    }
}

@Composable
private fun CStat(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = tint)
        Text(value, style = KxType.style(22.sp, FontWeight.ExtraBold))
        Text(label, style = KxType.style(12.sp, color = Kx.text4))
    }
}
