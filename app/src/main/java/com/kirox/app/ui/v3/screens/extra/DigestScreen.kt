package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class DgItem(val kind: String, val chat: String, val l: String, val c: Color, val time: String, val text: String, val openText: String, val chatId: Long = 0)
private val DgDemo = listOf(
    DgItem("m", "Kotlin-клуб", "K", Color(0xFF2F6E9E), "23:41", "Дмитрий: @kirill_dev, глянешь мой PR до обеда?", "Ответить"),
    DgItem("m", "Выпускники 2018", "В", Color(0xFF6A4C9C), "22:10", "Олег: @kirill_dev ты в субботу с нами?", "Ответить"),
    DgItem("r", "Анна", "А", Color(0xFFC0443C), "07:52", "Ответ на ваше сообщение: «Да, давай в 7!»", "Открыть"),
    DgItem("c", "Мама", "М", Color(0xFFB07A2E), "07:30", "Не забудь позвонить бабушке, у неё сегодня день рождения 🎂", "Позвонить"),
    DgItem("c", "Лиза", "Л", Color(0xFFC0443C), "00:12", "3 новых фото", "Посмотреть"),
)
private val DgHdr = mapOf("m" to Triple(Icons.Rounded.AlternateEmail, "Вас упомянули", Kx.accent), "r" to Triple(Icons.AutoMirrored.Rounded.Reply, "Ответы вам", Color(0xFF6FA9D8)),
    "c" to Triple(Icons.Rounded.Favorite, "От близких", Color(0xFFF07AA0)))

/** Board «Сводка»: morning card (sun), progress, grouped important messages (mentions / replies / close people) with open + read (slide out), all-done state. */
@Composable
fun DigestScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    // Real account: mentions + unread private chats from the store (replies-to-you need a store field → tg-tasks).
    val items = remember(demo) {
        if (demo) DgDemo else {
            val chats = ctx.store.chats
            chats.filter { it.unreadMention }.take(4).map { DgItem("m", it.title, it.title.take(1).uppercase(), Color.Unspecified, it.time, (it.previewSender?.let { s -> "$s: " } ?: "") + it.preview, "Ответить", it.id) } +
                chats.filter { !it.unreadMention && it.kind == KxChatKind.Private && it.unread > 0 && !it.muted }.take(4)
                    .map { DgItem("c", it.title, it.title.take(1).uppercase(), Color.Unspecified, it.time, it.preview, "Открыть", it.id) }
        }
    }
    val total = if (demo) 214 else ctx.store.unreadTotal.coerceAtLeast(items.size)
    val done = remember { mutableStateListOf<Int>() }
    val leaving = remember { mutableStateListOf<Int>() }
    var finishN by remember { mutableIntStateOf(0) }
    LaunchedEffect(finishN) { if (finishN > 0) { delay(380); done.addAll(leaving); leaving.clear() } }
    fun finish(ids: List<Int>, open: Boolean = false) {
        ids.forEach { i -> if (i !in leaving) leaving.add(i); if (!demo && items[i].chatId != 0L) ctx.store.markRead(items[i].chatId) }
        if (open && !demo && ids.size == 1) ctx.openChat(items[ids[0]].chatId)
        finishN++
    }
    val left = items.indices.filter { it !in done }
    val n = items.size; val doneN = n - left.size
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Сводка", { ctx.back() }) { ExRoundBtn(Icons.Outlined.Tune, "Настройки сводки", border = null) {} }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.w(.04f))
                    .ellipseBg(.9f, .8f, 0f, 1f, 0f to Kx.accentDeep.copy(alpha = .25f), .6f to Color.Transparent)
                    .ellipseBg(1f, .9f, 1f, 0f, 0f to Kx.warning.copy(alpha = .25f), .6f to Color.Transparent)
                    .border(1.dp, Kx.w(.1f), RoundedCornerShape(26.dp)).padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val sun = tri(kfRun(4000))
                    Icon(Icons.Rounded.WbSunny, null, Modifier.size(44.dp).graphicsLayer { translationY = -4.dp.toPx() * sun; rotationZ = 15f * sun }, tint = Color(0xFFF2B866))
                    Column(Modifier.weight(1f)) {
                        Text("Доброе утро!", style = ts(18.sp, FontWeight.ExtraBold))
                        Text(if (left.isNotEmpty()) "Пока вы спали: ${left.size} важных из $total сообщений" else "Вы всё разобрали — можно пить кофе ☕", style = ts(13.sp, color = Kx.text2), modifier = Modifier.padding(top = 2.dp))
                    }
                }
                val pct by animateFloatAsState(if (n == 0) 1f else doneN / n.toFloat(), tween(400), label = "pct")
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Kx.w(.07f))) {
                    Box(Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep))))
                }
                Text("Разобрано $doneN из $n", style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 6.dp))
                var last = ""
                left.forEach { i ->
                    val it = items[i]
                    val hdr = it.kind != last; last = it.kind
                    key(i) {
                        val out by animateFloatAsState(if (i in leaving) 1f else 0f, tween(380, easing = EaseCss), label = "out")
                        val inP = enter(300)
                        Column(Modifier.layout { m, c -> val p = m.measure(c); val h = (p.height * (1f - out)).toInt(); layout(p.width, h) { p.place(0, 0) } }
                            .graphicsLayer { alpha = inP * (1f - out); translationY = (1f - inP) * 10.dp.toPx(); translationX = 80.dp.toPx() * out }) {
                            if (hdr) DgHdr.getValue(it.kind).let { (icon, text, ink) ->
                                Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(icon, null, Modifier.size(18.dp), tint = ink); Text(text, style = ts(13.sp, FontWeight.ExtraBold, ink))
                                }
                            }
                            Row(Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (it.c != Color.Unspecified) ExLetter(it.l, 42.dp, it.c, font = 16.sp, ink = Kx.text) else KxAvatar(it.chat, it.chatId, size = 42.dp, letterWeight = FontWeight.ExtraBold)
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(it.chat, style = ts(14.sp, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(it.time, style = ts(11.sp, color = Kx.text4))
                                    }
                                    Text(it.text, style = ts(14.sp, color = Color(0xFFE2E9E4), lh = 18.9.sp), modifier = Modifier.padding(top = 3.dp))
                                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ExPill(it.openText, height = 30.dp, bg = Kx.accent.copy(alpha = .14f), ink = Kx.accent, size = 12.sp, padH = 12.dp) { finish(listOf(i), open = true) }
                                        ExPill("Прочитано", height = 30.dp, bg = Kx.w(.06f), ink = Kx.text2, size = 12.sp, padH = 12.dp) { finish(listOf(i)) }
                                    }
                                }
                            }
                        }
                    }
                }
                if (left.isEmpty()) Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val p = enter(450)
                    Box(Modifier.graphicsLayer { val k = if (p < .6f) .4f + .75f * (p / .6f) else 1.15f - .15f * ((p - .6f) / .4f); scaleX = k; scaleY = k; alpha = (p / .6f).coerceAtMost(1f) }
                        .size(72.dp).clip(CircleShape).lin2(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Celebration, null, Modifier.size(40.dp), tint = Kx.onAccent) }
                    Text("Всё важное прочитано", style = ts(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                    Text("Остальные ${(total - n).coerceAtLeast(0)} сообщений подождут", style = ts(13.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                } else Row(Modifier.padding(top = 8.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.06f)).kxClick("Отметить всё прочитанным") { finish(left) },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DoneAll, null, Modifier.size(18.dp), tint = Kx.text2); Text("Отметить всё прочитанным", style = ts(14.sp, FontWeight.Bold, Kx.text2))
                }
                Text("Сводка собирается на телефоне: упоминания, ответы вам и сообщения близких. Приходит каждое утро в 8:00.", style = ts(12.sp, color = Kx.text4, lh = 17.4.sp),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp))
            }
        }
    }
}
