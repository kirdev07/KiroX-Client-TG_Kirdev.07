package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

// Board demo (no chat id). With "GroupStats:<chatId>" the 4 KPIs are the first rows of store.chatStats(chatId).
// demo: needs store (activity heatmap, top senders and joined/left graphs are not in the store yet)
private val GDays = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
private val GHours = listOf(0, 3, 6, 9, 12, 15, 18, 21)
private val GM = listOf(
    listOf(2, 0, 1, 14, 30, 28, 46, 40), listOf(3, 1, 1, 16, 34, 31, 52, 38), listOf(2, 0, 2, 18, 29, 35, 61, 44), listOf(4, 1, 1, 15, 33, 30, 49, 41),
    listOf(5, 2, 1, 12, 27, 26, 38, 57), listOf(9, 4, 0, 4, 18, 26, 33, 36), listOf(6, 2, 0, 3, 14, 22, 30, 27),
)
private val GTop = listOf(Triple("Дмитрий", "Д", Color(0xFF3F9A6C)) to 214, Triple("Айгерим", "А", Color(0xFFB07A2E)) to 168, Triple("Сергей", "С", Color(0xFF6A4C9C)) to 131,
    Triple("Ерлан", "Е", Color(0xFFC0443C)) to 96, Triple("Дана", "Д", Color(0xFF7FB8E0)) to 72)

/** Board «Статистика группы»: 4 KPIs, tappable 7×8 activity heatmap, top members bars, joined/left/total. */
@Composable
fun GroupStatsScreen(ctx: KxScreenCtx) {
    var sel by remember { mutableStateOf(2 to 6) }
    val max = GM.flatten().max()
    fun shade(v: Int): Color = when { v == 0 -> Kx.w(.03f); v.toFloat() / max < .25f -> Color(0x1F8FD3AC); v.toFloat() / max < .5f -> Color(0x598FD3AC); v.toFloat() / max < .8f -> Color(0xA68FD3AC); else -> Kx.accent }
    val v = GM[sel.first][sel.second]; val h = GHours[sel.second]
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val stats = gxLoad(chatId) { chatId?.let { store.chatStats(it) } }

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            Column(Modifier.weight(1f)) {
                Text("Статистика группы", style = KxType.style(20.sp, FontWeight.ExtraBold))
                Text(if (chatId == null) "Android Dev KZ · 15–21 ноября" else store.header(chatId).title + (stats?.period?.let { " · $it" } ?: ""), style = KxType.style(12.sp, color = Kx.text3))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
            val icons = listOf(Icons.Outlined.Chat, Icons.Outlined.Person, Icons.Outlined.Visibility, Icons.Outlined.GroupAdd)
            val kpis = if (chatId != null) stats?.rows.orEmpty().take(4).mapIndexed { i, (t, cur, prev) ->
                val d = gxDelta(cur, prev); listOf(icons[i], t, gxStatValue(cur), d, if (d.startsWith("−")) Kx.danger else Kx.accent)
            } else listOf(
                listOf(Icons.Outlined.Chat, "Сообщений", "1 104", "+18% к прошлой неделе", Kx.accent), listOf(Icons.Outlined.Person, "Писали", "312", "+9%", Kx.accent),
                listOf(Icons.Outlined.Visibility, "Читали", "3 960", "−3%", Kx.danger), listOf(Icons.Outlined.GroupAdd, "Новых", "64", "+22%", Kx.accent),
            )
            kpis.chunked(2).forEachIndexed { r, pair ->
                Row(Modifier.padding(top = if (r == 0) 0.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEachIndexed { c, kp ->
                        Column(Modifier.weight(1f).gxRise((r * 2 + c) * 50, 300, 8.dp).clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Kx.w(.08f), Kx.w(.035f))))
                            .border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp)).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(kp[0] as ImageVector, null, Modifier.size(16.dp), tint = Kx.text3)
                                Text(kp[1] as String, style = KxType.style(12.sp, color = Kx.text3))
                            }
                            Text(kp[2] as String, style = KxType.style(22.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"), modifier = Modifier.padding(top = 4.dp))
                            Text(kp[3] as String, style = KxType.style(12.sp, FontWeight.Bold, kp[4] as Color))
                        }
                    }
                }
            }
            Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.07f), RoundedCornerShape(22.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Когда пишут · по часам и дням недели", style = KxType.style(15.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                    Text("${GDays[sel.first]}, $h:00–${h + 3}:00 · $v сообщ.", style = KxType.style(12.sp, color = Kx.text3), textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 130.dp))
                }
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(22.dp))
                        GHours.forEach { hh -> Text("$hh", style = KxType.style(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.weight(1f).height(16.dp)) }
                    }
                    GM.forEachIndexed { di, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(GDays[di], style = KxType.style(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                            row.forEachIndexed { hi, value ->
                                val on = sel.first == di && sel.second == hi
                                Box(Modifier.weight(1f).height(24.dp).gxScaleIn((di * 8 + hi) * 6, 300).semantics {
                                    contentDescription = "${GDays[di]}, ${GHours[hi]}:00–${GHours[hi] + 3}:00 — $value сообщений"
                                }.then(if (on) Modifier.border(2.dp, Kx.text, RoundedCornerShape(5.dp)) else Modifier).clip(RoundedCornerShape(5.dp)).background(shade(value))
                                    .gxPlainClick { sel = di to hi })
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("меньше", style = KxType.style(11.sp, color = Kx.text4))
                    listOf(Color(0x1A8FD3AC), Color(0x598FD3AC), Color(0xA68FD3AC), Kx.accent).forEach { Box(Modifier.size(14.dp, 10.dp).clip(RoundedCornerShape(3.dp)).background(it)) }
                    Text("больше", style = KxType.style(11.sp, color = Kx.text4))
                }
            }
            Text("Самые активные", style = KxType.style(15.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 16.dp))
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GTop.forEachIndexed { i, (p, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${i + 1}", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.text4), textAlign = TextAlign.End, modifier = Modifier.width(18.dp))
                        Box(Modifier.size(34.dp).clip(CircleShape).background(p.third), contentAlignment = Alignment.Center) { Text(p.second, style = KxType.style(13.sp, FontWeight.ExtraBold)) }
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(p.first, style = KxType.style(13.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                                Text("$n", style = KxType.style(13.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"))
                            }
                            Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Kx.w(.06f))) {
                                val pr = remember { Animatable(0f) }
                                val reduced = LocalKxReducedMotion.current
                                LaunchedEffect(Unit) { if (reduced) pr.snapTo(1f) else pr.animateTo(1f, tween(800, easing = KxMotion.rise)) }
                                Box(Modifier.fillMaxWidth(n.toFloat() / GTop[0].second * pr.value).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Kx.accent.copy(alpha = 1f - i * .14f)))
                            }
                        }
                    }
                }
            }
            Text("Участники за неделю", style = KxType.style(15.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 16.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Triple("+64", "вступили", Kx.accent) to Color(0x148FD3AC), Triple("−18", "вышли", Kx.danger) to Color(0x14FF8A80), Triple("+46", "итого", Kx.text) to Kx.w(.05f)).forEach { (t, bg) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(bg).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(t.first, style = KxType.style(20.sp, FontWeight.ExtraBold, t.third))
                        Text(t.second, style = KxType.style(11.sp, color = Kx.text3))
                    }
                }
            }
        }
    }
}
