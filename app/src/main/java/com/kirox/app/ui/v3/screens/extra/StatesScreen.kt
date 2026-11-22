package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class StChat(val name: String, val last: String, val time: String, val from: Color, val to: Color, val ink: Color)
private val StChats = listOf(
    StChat("Анна", "Возьмёшь мне капучино?", "15:41", Color(0xFF4B3F7A), Color(0xFF2A2240), Color(0xFFDCD0FF)),
    StChat("Курс английского", "Марина: Тест в субботу", "15:37", Color(0xFF2F6E9E), Color(0xFF14243A), Color(0xFFCFE6FF)),
    StChat("Сергей", "Ок, тогда до пятницы", "14:02", Color(0xFF3F7A5A), Color(0xFF1E3A2A), Color(0xFFCFF2DD)),
    StChat("Мама", "Позвони, как освободишься", "12:15", Color(0xFF9E3F5C), Color(0xFF2E1420), Color(0xFFFFD0DD)),
    StChat("Избранное", "Заметка: купить батарейки", "Вчера", Kx.accentMid, Kx.accentDeep, Kx.onAccent),
)
private val StTabs = listOf(
    Triple("ok", "Обычно", Icons.Outlined.CheckCircle), Triple("connecting", "Подключение", Icons.Outlined.Sync),
    Triple("updating", "Обновление", Icons.Outlined.Downloading), Triple("offline", "Без сети", Icons.Outlined.WifiOff),
    Triple("loading", "Загрузка", Icons.Outlined.HourglassEmpty), Triple("empty", "Пусто", Icons.Outlined.Inbox),
    Triple("error", "Ошибка", Icons.Outlined.ErrorOutline), Triple("update", "Новая версия", Icons.Outlined.SystemUpdate),
)

/** Board «Состояния и ошибки»: the chat list in every state (connecting, offline, skeleton, empty, error, update) + switcher. */
@Composable
fun StatesScreen(ctx: KxScreenCtx) {
    var st by remember { mutableStateOf("ok") }
    var upd by remember { mutableStateOf("idle") }
    var job by remember { mutableIntStateOf(0) }
    var jobKind by remember { mutableStateOf("") }
    LaunchedEffect(job) {
        when (jobKind) {
            "retry" -> { delay(1800); st = "ok" }
            "upd" -> { delay(3100); st = "ok"; upd = "idle" }
        }
    }
    val retry: () -> Unit = { st = "connecting"; jobKind = "retry"; job++ }
    val status = st == "connecting" || st == "updating"
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                key(status) {
                    if (!status) Text("Чаты", style = ts(26.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f).exIn(300, dy = 10.dp))
                    else Row(Modifier.weight(1f).exIn(300, dy = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StSpinner(18.dp, 2.5.dp)
                        Text(if (st == "connecting") "Подключение" else "Обновление", style = ts(20.sp, FontWeight.ExtraBold))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(3) { i ->
                                val d = kfLoop(1200, i * 200, "d$i")
                                val a = when { d < .4f -> .25f + .75f * (d / .4f); d < .8f -> 1f - .75f * ((d - .4f) / .4f); else -> .25f }
                                Text(".", style = ts(20.sp, FontWeight.ExtraBold), modifier = Modifier.graphicsLayer { alpha = a })
                            }
                        }
                    }
                }
                ExRoundBtn(Icons.Outlined.Search, "Поиск", size = 42.dp, border = null) { ctx.open("Search") }
            }
            if (st == "offline") key("off") {
                Row(Modifier.stDrop().padding(start = 12.dp, end = 12.dp, top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.warning.copy(alpha = .1f))
                    .border(1.dp, Kx.warning.copy(alpha = .3f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.WifiOff, null, Modifier.size(24.dp), tint = Kx.warning)
                    Column(Modifier.weight(1f)) {
                        Text("Нет подключения", style = ts(14.sp, FontWeight.Bold))
                        Text("Сообщения отправятся, когда появится сеть", style = ts(12.sp, color = Kx.text2), modifier = Modifier.padding(top = 2.dp))
                    }
                    ExPill("Повторить", height = 32.dp, bg = Kx.warning.copy(alpha = .18f), ink = Kx.warning, size = 12.sp, weight = FontWeight.ExtraBold, padH = 12.dp, onClick = retry)
                }
            }
            if (st == "update") key("upd") {
                Column(Modifier.stDrop().padding(start = 12.dp, end = 12.dp, top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .lin(120f, 0f to Kx.accentDeep.copy(alpha = .25f), 1f to Kx.w(.04f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(24.dp), tint = Kx.accent)
                        Column(Modifier.weight(1f)) {
                            Text(if (upd == "run") "Скачиваем kiroX 2.4…" else "Доступна версия 2.4", style = ts(14.sp, FontWeight.Bold))
                            Text(if (upd == "run") "Можно продолжать пользоваться" else "Новый дизайн и исправления", style = ts(12.sp, color = Kx.text2), modifier = Modifier.padding(top = 2.dp))
                        }
                        if (upd == "idle") ExPill("Обновить", height = 32.dp, bg = Kx.accent, ink = Kx.onAccent, size = 12.sp, weight = FontWeight.ExtraBold, padH = 12.dp) {
                            upd = "run"; jobKind = "upd"; job++
                        }
                    }
                    if (upd == "run") {
                        val bar = enter(3000, easing = LinearEasing)
                        Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.1f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(bar).background(Kx.accent))
                        }
                    }
                }
            }
            key(st) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 130.dp)) {
                    when (st) {
                        "loading" -> {
                            val t = kfLoop(1200, label = "shimmer")
                            listOf(.6f to .85f, .45f to .7f, .7f to .6f, .5f to .8f, .65f to .55f, .4f to .75f, .55f to .65f).forEach { (w1, w2) ->
                                Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(52.dp).clip(CircleShape).shimmer(t))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.fillMaxWidth(w1).height(12.dp).clip(RoundedCornerShape(6.dp)).shimmer(t))
                                        Box(Modifier.fillMaxWidth(w2).height(10.dp).clip(RoundedCornerShape(5.dp)).shimmer(t))
                                    }
                                }
                            }
                        }
                        "empty" -> StBig(Icons.Outlined.Forum, Kx.accent, "Пока тихо", "Напишите кому-нибудь первым или найдите канал по интересам", float = true) {
                            ExPill("Написать", bg = Kx.accent, ink = Kx.onAccent, weight = FontWeight.ExtraBold, padH = 16.dp, icon = Icons.Outlined.Edit) { ctx.open("NewMsg") }
                            ExPill("Найти каналы", padH = 16.dp, icon = Icons.Outlined.Explore) { ctx.open("Explore") }
                        }
                        "error" -> StBig(Icons.Outlined.CloudOff, Kx.danger, "Не удалось загрузить", "Сервер не ответил. Проверьте интернет или попробуйте позже.", float = false) {
                            ExPill("Попробовать снова", icon = Icons.Outlined.Refresh, onClick = retry)
                        }
                        else -> StChats.forEachIndexed { i, c ->
                            val pending = st == "offline" && i == 0
                            Row(Modifier.exIn(300, i * 40, 10.dp).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(52.dp).clip(CircleShape).lin2(140f, c.from, c.to), contentAlignment = Alignment.Center) {
                                    Text(c.name.take(1), style = ts(19.sp, FontWeight.Bold, c.ink))
                                }
                                Column(Modifier.weight(1f)) {
                                    Row { Text(c.name, style = ts(15.sp, FontWeight.Bold), modifier = Modifier.weight(1f)); Text(c.time, style = ts(12.sp, color = Kx.text4)) }
                                    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (pending) Icon(Icons.Outlined.Schedule, null, Modifier.size(15.dp), tint = Kx.warning)
                                        KxLine(if (pending) "Вы: Уже иду, возьму 👍" else c.last, ts(14.sp, color = Kx.text4), Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // State switcher (glass panel, 4x2).
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 20.dp).fillMaxWidth()
            .boxShadow(Color(0x80000000), 40.dp, 18.dp, 26.dp).clip(RoundedCornerShape(26.dp)).background(Kx.surface)
            .background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f)))).insetTop(.18f).border(1.dp, Kx.w(.16f), RoundedCornerShape(26.dp)).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StTabs.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { (id, label, ic) ->
                        val on = id == st
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .18f) else Color.Transparent, tween(200), label = "tb")
                        val ink by animateColorAsState(if (on) Kx.accent else Kx.text2, tween(200), label = "ti")
                        Column(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(bg).kxClick(label) { st = id; upd = "idle"; jobKind = ""; job++ },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)) {
                            Icon(ic, null, Modifier.size(17.dp), tint = ink)
                            Text(label, style = ts(11.sp, FontWeight.Bold, ink, lh = 12.sp), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StBig(icon: ImageVector, ink: Color, title: String, sub: String, float: Boolean, buttons: @Composable RowScope.() -> Unit) {
    val f = if (float) tri(kfRun(4000)) else 0f
    Column(Modifier.fillMaxWidth().exIn(400, dy = 10.dp).padding(start = 30.dp, end = 30.dp, top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(110.dp).graphicsLayer { translationY = -8.dp.toPx() * f }.clip(RoundedCornerShape(36.dp))
            .then(if (float) Modifier.lin2(145f, Color(0xFF1D2A23), Color(0xFF121714)).border(1.dp, Color(0xFF2A3A31), RoundedCornerShape(36.dp))
            else Modifier.background(ink.copy(alpha = .08f)).border(1.dp, ink.copy(alpha = .25f), RoundedCornerShape(36.dp))), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(54.dp), tint = ink)
        }
        Text(title, style = ts(20.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 20.dp))
        Text(sub, style = ts(14.sp, color = Kx.text3, lh = 20.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = buttons)
    }
}

@Composable
internal fun StSpinner(size: Dp, stroke: Dp, track: Color = Kx.accent.copy(alpha = .25f), head: Color = Kx.accent) {
    val spin = kfLoop(800, label = "spin")
    Box(Modifier.size(size).graphicsLayer { rotationZ = 360f * spin }.drawBehind {
        val w = stroke.toPx()
        drawCircle(track, radius = this.size.minDimension / 2 - w / 2, style = Stroke(w))
        drawArc(head, -135f, 90f, false, Offset(w / 2, w / 2), Size(this.size.width - w, this.size.height - w), style = Stroke(w))
    })
}

/** kxDrop .35s: slides down from -100% of its height. */
@Composable
private fun Modifier.stDrop(): Modifier {
    val p = enter(350, easing = EaseSheet)
    return this.graphicsLayer { translationY = -(1f - p) * size.height }
}

/** kxShimmer: #151A17 with a #222924 band (0/60/120px) moving -200 → 200px, tiled every 400px. */
private fun Modifier.shimmer(t: Float): Modifier = this.drawBehind {
    val base = Color(0xFF151A17); val hi = Color(0xFF222924)
    drawRect(base)
    val px = 1.dp.toPx(); val pos = (-200f + 400f * t) * px
    for (o in listOf(pos, pos + 400f * px, pos - 400f * px)) {
        drawRect(Brush.horizontalGradient(0f to base, .5f to hi, 1f to base, startX = o, endX = o + 120f * px), topLeft = Offset(o, 0f), size = Size(120f * px, size.height))
    }
}
