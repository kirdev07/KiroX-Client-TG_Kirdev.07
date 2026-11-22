package com.kirox.app.ui.v3.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

// demo: needs store (chat event log for 48h with filters; report spam, unban)
private data class LE(val id: Int, val k: String, val icon: ImageVector, val day: String, val who: String, val what: String, val quote: String = "",
                      val t: String, val act: String = "", val old: String? = null)
private val LEvents = listOf(
    LE(1, "del", Icons.Rounded.Delete, "Сегодня", "Дмитрий", "· удалено сообщение · Гость_8812", "Заработок от 5000 в день, пиши в личку!!!", "18:12", "report"),
    LE(2, "ban", Icons.Rounded.Block, "Сегодня", "Дмитрий", "· блокировка навсегда · Гость_8812", "", "18:12", "unban"),
    LE(3, "pin", Icons.Rounded.PushPin, "Сегодня", "Айгерим", "· закреплено сообщение", "Встреча в субботу в 12:00, коворкинг на Абая", "15:40"),
    LE(4, "edit", Icons.Rounded.Edit, "Сегодня", "Вы", "изменили описание группы", "Обсуждаем Kotlin, Compose и KMP", "11:05", old = "Обсуждаем Kotlin"),
    LE(5, "join", Icons.Rounded.GroupAdd, "Вчера", "Сергей", "· вступление по ссылке «Для митапа»", "", "21:30"),
    LE(6, "ban", Icons.Rounded.Timer, "Вчера", "Айгерим", "· ограничение · Олег: без медиа на 1 день", "", "19:02"),
    LE(7, "edit", Icons.Rounded.Speed, "Вчера", "Вы", "включили медленный режим — 1 минута", "", "10:15"),
)
private val LFilters = listOf(Triple("all", Icons.Outlined.Apps, "Все"), Triple("del", Icons.Outlined.Delete, "Удаления"), Triple("ban", Icons.Outlined.Block, "Ограничения"),
    Triple("edit", Icons.Outlined.Edit, "Правки"), Triple("pin", Icons.Outlined.PushPin, "Закрепы"), Triple("join", Icons.Outlined.GroupAdd, "Участники"))
private val LColors = mapOf("del" to (Kx.danger to Color(0x24FF8A80)), "ban" to (Kx.warning to Color(0x24E8B04A)), "edit" to (Color(0xFF7FB8E0) to Color(0x247FB8E0)),
    "pin" to (Kx.accent to Color(0x248FD3AC)), "join" to (Kx.text2 to Color.White.copy(alpha = .08f)))

/** Board «Недавние действия»: filter chips, timeline grouped by day, quotes (old → new), report/unban actions. */
@Composable
fun AdminLogScreen(ctx: KxScreenCtx) {
    var f by remember { mutableStateOf("all") }
    var k by remember { mutableIntStateOf(0) }
    val done = remember { mutableStateListOf<Int>() }
    val tip = rememberGxTip()
    val list = LEvents.filter { f == "all" || it.k == f }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    Text("Недавние действия", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Text("Kotlin-клуб · за последние 48 часов", style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LFilters.forEach { (key, icon, t) ->
                    val on = f == key
                    val ink = if (on) Kx.accent else Kx.text2
                    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Color(0x298FD3AC) else Kx.w(.05f))
                        .border(1.dp, if (on) Color(0x738FD3AC) else Kx.w(.08f), RoundedCornerShape(17.dp)).kxClick(t) { f = key; k++ }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(icon, null, Modifier.size(16.dp), tint = ink)
                        Text(t, style = KxType.style(13.sp, FontWeight.Bold, ink), maxLines = 1)
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 16.dp)) {
                var lastDay = ""
                list.forEach { r ->
                    val showDay = r.day != lastDay; lastDay = r.day
                    val (ic, ibg) = LColors.getValue(r.k)
                    val d = r.id in done
                    if (showDay) Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                        Text(r.day, style = KxType.style(11.sp, FontWeight.Bold, Kx.text3), modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Kx.w(.06f)).padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                    key(r.id, k) {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).gxSlideIn().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.width(36.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(ibg), contentAlignment = Alignment.Center) { Icon(r.icon, null, Modifier.size(19.dp), tint = ic) }
                                Box(Modifier.padding(top = 4.dp).weight(1f).width(2.dp).clip(RoundedCornerShape(1.dp)).background(Kx.w(.06f)))
                            }
                            Column(Modifier.weight(1f).padding(bottom = 6.dp)) {
                                Text(buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(r.who) }; append(" ")
                                    withStyle(SpanStyle(color = Kx.text2)) { append(r.what) }
                                }, style = KxType.style(14.sp, lineHeight = 19.6.sp))
                                if (r.quote.isNotEmpty()) Text(buildAnnotatedString {
                                    if (r.old != null) { withStyle(SpanStyle(color = Kx.text4, textDecoration = TextDecoration.LineThrough)) { append(r.old) }; append(" → ") }
                                    append(r.quote)
                                }, style = KxType.style(13.sp, color = Kx.text2, lineHeight = 18.2.sp), modifier = Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(Kx.w(.05f)).drawBehind { drawRect(ic, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }
                                    .padding(start = 13.dp, end = 10.dp, top = 8.dp, bottom = 8.dp))
                                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(r.t, style = KxType.style(11.sp, color = Kx.text4))
                                    if (r.act.isNotEmpty()) {
                                        val label = if (d) (if (r.act == "report") "Отправлено" else "Снято") else (if (r.act == "report") "Сообщить о спаме" else "Снять блокировку")
                                        Box(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(if (d) Color(0x248FD3AC) else Kx.w(.08f))
                                            .kxClick(label) { if (!d) { done.add(r.id); tip.say(if (r.act == "report") "Жалоба на спам отправлена" else "Блокировка снята") } }
                                            .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                            Text(label, style = KxType.style(12.sp, FontWeight.ExtraBold, if (d) Kx.accent else Kx.text))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, null, Modifier.size(44.dp), tint = Kx.text4)
                    Text("Ничего за 48 часов", style = KxType.style(15.sp, FontWeight.Bold, Kx.text2), modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        GxTip(tip, bottom = 26.dp, icon = Icons.Rounded.CheckCircle, durationMs = 2000, bg = Color(0xF528302C), textSize = 13)
    }
}

/** kxIn of the timeline boards: fade + slide from [dx]dp (x), .25s. */
@Composable
fun Modifier.gxSlideIn(durMs: Int = 250, dx: Int = -10): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, androidx.compose.animation.core.tween(durMs)) }
    return graphicsLayer { alpha = p.value; translationX = (1f - p.value) * dx * density }
}
