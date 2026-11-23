package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.screens.more.C3NeedChat
import com.kirox.app.ui.v3.screens.more.c3Chat
import com.kirox.app.ui.v3.screens.more.c3Live
import com.kirox.app.ui.v3.screens.more.c3Load
import com.kirox.app.ui.v3.screens.more.c3Msg
import com.kirox.app.ui.v3.screens.more.c3Peek
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private class SeenP(val name: String, val av: Color, val seen: String, val r: String, val me: Boolean = false)

// Demo family chat. Real group (arg = chat id[:message id], else your last message): store.messageViewers (GetMessageViewers —
// small groups, last 7 days — with reactions from GetMessageAddedReactions). Telegram does not list who has not read, so that tab is hidden.
private val SeenPeople = listOf(
    SeenP("Мама", Color(0xFFA8456A), "15:21", "❤️"), SeenP("Папа", Color(0xFF2F6E9E), "15:24", "👍"),
    SeenP("Бабушка", Color(0xFFB07A2E), "15:40", "❤️"), SeenP("Дана", Color(0xFF6A4C9C), "16:02", "🎂"),
    SeenP("Тимур", Color(0xFF3F7A5A), "16:15", ""), SeenP("Дядя Олег", Color(0xFF2E8A86), "", ""),
)

/** Board «Кто прочитал»: group message with reaction chips, «Прочитали N из M», sheet with tabs (all / seen / unseen / per reaction). */
@Composable
fun SeenScreen(ctx: KxScreenCtx) {
    val chatId = ctx.c3Chat
    val live = chatId != null
    if (ctx.c3Live && chatId == null) C3NeedChat(ctx, "Кто прочитал") else SeenLoaded(ctx, chatId, live)
}

@Composable
private fun SeenLoaded(ctx: KxScreenCtx, chatId: Long?, live: Boolean) {
    val header = chatId?.let { ctx.store.header(it) }
    val peek = c3Peek(ctx.store, chatId, 40)
    val wantId = ctx.c3Msg
    val target = if (!live) null else (ctx.store.messages(chatId!!) + peek).let { all -> if (wantId != null) all.firstOrNull { it.id == wantId } else all.lastOrNull { it.out } }
    val targetId = wantId ?: target?.id
    val viewers = c3Load(chatId, targetId) { if (live && targetId != null) ctx.store.messageViewers(chatId!!, targetId) else null }
    val palette = listOf(Color(0xFFA8456A), Color(0xFF2F6E9E), Color(0xFFB07A2E), Color(0xFF6A4C9C), Color(0xFF3F7A5A), Color(0xFF2E8A86))
    val people = if (!live) SeenPeople else viewers.orEmpty().mapIndexed { i, v -> SeenP(v.name, palette[i % palette.size], v.time.ifBlank { "—" }, v.reaction) }
    val total = if (live) (header?.members ?: 0).let { if (it > 1) it - 1 else people.size } else people.size
    if (live && targetId == null) C3NeedChat(ctx, "Кто прочитал", if (peek.isEmpty()) "Загрузка…" else "Здесь нет ваших сообщений.")
    else SeenBody(ctx, chatId, live, header, target, targetId, people, total, viewers == null)
}

@Composable
private fun SeenBody(ctx: KxScreenCtx, chatId: Long?, live: Boolean, header: com.kirox.app.ui.v3.model.KxChatHeader?, target: com.kirox.app.ui.v3.model.KxMessage?,
                     targetId: Long?, people: List<SeenP>, total: Int, loading: Boolean) {
    var sheet by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf("all") }
    var mine by remember { mutableStateOf(target?.reactions?.firstOrNull { it.mine }?.emoji ?: "") }
    var floatN by remember { mutableIntStateOf(0) }
    var floatE by remember { mutableStateOf("") }
    val all = if (mine.isNotEmpty()) people + SeenP("Вы", Kx.accentDeep, "15:20", mine, true) else people
    val counts = linkedMapOf<String, Int>().also { m -> all.forEach { if (it.r.isNotEmpty()) m[it.r] = (m[it.r] ?: 0) + 1 } }
    val seenN = people.count { it.seen.isNotEmpty() }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.05f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", size = 42.dp) { ctx.back() }
                Box(Modifier.size(38.dp).clip(CircleShape).lin2(140f, Color(0xFF7A5A2E), Color(0xFF3A2A14)), contentAlignment = Alignment.Center) {
                    Text((header?.title ?: "С").take(1).uppercase(), style = ts(15.sp, FontWeight.Bold, Color(0xFFFFE2B8)))
                }
                Column(Modifier.weight(1f)) {
                    Text(header?.title ?: "Семья", style = ts(16.sp, FontWeight.ExtraBold), maxLines = 1); Text(header?.status ?: "7 участников", style = ts(12.sp, color = Kx.text3), maxLines = 1)
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().ellipseBg(1.2f, .6f, 1f, 0f, 0f to Kx.accentDeep.copy(alpha = .14f), .6f to Kx.bg).padding(12.dp),
                verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                Column(Modifier.fillMaxWidth(.82f), horizontalAlignment = Alignment.End) {
                    Column(Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)).lin2(160f, Kx.accentDark, Color(0xFF1F4D36)).padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 7.dp)) {
                        Text(if (live) target?.text?.ifBlank { "Сообщение" } ?: "Сообщение" else "В воскресенье все у бабушки в 14:00, не забудьте торт 🎂", style = ts(15.sp, color = Color(0xFFE6F6EC), lh = 20.7.sp))
                        Row(Modifier.align(Alignment.End).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (live) target?.time.orEmpty() else "15:20", style = ts(11.sp, color = Color(0xFFA9D9BF))); Icon(Icons.Outlined.DoneAll, null, Modifier.size(15.dp), tint = Color(0xFFA9D9BF))
                        }
                    }
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (if (live) counts.keys.toList() else listOf("❤️", "👍", "🎂")).filter { (counts[it] ?: 0) > 0 }.forEach { e ->
                            val on = mine == e
                            val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .2f) else Kx.w(.07f), tween(200), label = "cb")
                            val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .5f) else Color.Transparent, tween(200), label = "cd")
                            Box {
                                Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(15.dp))
                                    .kxClick(e) { if (live) ctx.store.react(chatId!!, targetId!!, e); if (on) { mine = "" } else { mine = e; floatE = e; floatN++ } }.padding(start = 6.dp, end = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    key(on) {
                                        val p = if (on) enter(400) else 1f
                                        Text(e, style = ts(16.sp), modifier = Modifier.graphicsLayer {
                                            val s = if (p < .6f) .4f + .9f * (p / .6f) else 1.3f - .3f * ((p - .6f) / .4f); scaleX = s; scaleY = s
                                        })
                                    }
                                    Text("${counts[e]}", style = ts(13.sp, FontWeight.Bold))
                                }
                                if (floatE == e && on && floatN > 0) key(floatN) {
                                    val p = enter(800, easing = EaseOutCss)
                                    if (p < 1f) Text(e, style = ts(18.sp), modifier = Modifier.offset(4.dp, (-4).dp).graphicsLayer {
                                        translationY = -60.dp.toPx() * p; val s = 1f + .6f * p; scaleX = s; scaleY = s; alpha = 1f - p
                                    })
                                }
                            }
                        }
                    }
                    Row(Modifier.padding(top = 6.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).kxClick("Кто прочитал") { sheet = true }
                        .padding(start = 4.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // faces: 24 + 2px border (content-box → 28), margin-left -6 each
                        Row(Modifier.offset(x = (-6).dp).padding(end = 0.dp), horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            people.filter { it.seen.isNotEmpty() }.take(3).forEach { f ->
                                Box(Modifier.size(28.dp).clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape).background(f.av),
                                    contentAlignment = Alignment.Center) { Text(f.name.take(1), style = ts(11.sp, FontWeight.ExtraBold, Color.White)) }
                            }
                        }
                        Text(if (live && loading) "Загрузка…" else if (live && seenN == 0) "Пока никто не прочитал" else "Прочитали $seenN из $total", style = ts(12.sp, FontWeight.Bold, Kx.text2), modifier = Modifier.offset(x = (-6).dp))
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp).offset(x = (-6).dp), tint = Kx.text2)
                    }
                }
            }
            Box(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 18.dp)) {
                Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(23.dp)).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart) { Text("Сообщение", style = ts(15.sp, color = Kx.text4)) }
            }
        }
        if (sheet) ExSheet({ sheet = false }, pad = PaddingValues(top = 10.dp), grabber = false) {
            Column(Modifier.height(560.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.dashed))
                Spacer(Modifier.height(12.dp))
                data class T(val id: String, val icon: ImageVector?, val e: String, val n: Int)
                val tabs = listOf(T("all", Icons.Outlined.Groups, "", people.size), T("seen", Icons.Outlined.DoneAll, "", seenN),
                    T("unseen", Icons.Outlined.Schedule, "", total - seenN)).filter { !live || it.id != "unseen" } + counts.map { (e, n) -> T(e, null, e, n) }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEach { t ->
                        val on = tab == t.id
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "tb")
                        val ink by animateColorAsState(if (on) Kx.accent else Kx.text2, tween(200), label = "ti")
                        Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(bg).kxClick(t.id) { tab = t.id }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (t.icon != null) Icon(t.icon, null, Modifier.size(17.dp), tint = ink) else Text(t.e, style = ts(16.sp))
                            Text("${t.n}", style = ts(13.sp, FontWeight.Bold, ink))
                        }
                    }
                }
                val list = when (tab) {
                    "all" -> all.filter { !it.me }; "seen" -> people.filter { it.seen.isNotEmpty() }
                    "unseen" -> people.filter { it.seen.isEmpty() }; else -> all.filter { it.r == tab }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 20.dp)) {
                    key(tab) {
                        list.forEachIndexed { i, p ->
                            val e = enter(250, i * 30)
                            Row(Modifier.graphicsLayer { alpha = e * (if (p.seen.isNotEmpty()) 1f else .6f); translationX = (1f - e) * 16.dp.toPx() }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(44.dp).clip(CircleShape).background(p.av), contentAlignment = Alignment.Center) {
                                    Text(p.name.take(1), style = ts(16.sp, FontWeight.ExtraBold, Color.White))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, style = ts(15.sp, FontWeight.Bold))
                                    val ink = if (p.seen.isNotEmpty()) Kx.accent else Kx.text4
                                    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(if (p.seen.isNotEmpty()) Icons.Outlined.DoneAll else Icons.Outlined.Schedule, null, Modifier.size(14.dp), tint = ink)
                                        Text(if (p.seen.isNotEmpty()) "прочитал(а) в ${p.seen}" else "ещё не прочитал(а)", style = ts(12.sp, color = ink))
                                    }
                                }
                                if (p.r.isNotEmpty()) Text(p.r, style = ts(22.sp))
                            }
                        }
                    }
                    if (tab == "all" || tab == "seen") Text("Время прочтения видно 7 дней. Кто скрыл время в сети, тот не попадает в список.",
                        style = ts(12.sp, color = Kx.text4, lh = 17.sp), modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 10.dp))
                }
            }
        }
    }
}
