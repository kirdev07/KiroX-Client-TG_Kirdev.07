package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class NbP(val name: String, val l: String, val c: Color, val x: Float, val y: Float)
private val NbPeople = listOf(NbP("Мария", "М", Color(0xFFB07A2E), .22f, .30f), NbP("Дмитрий", "Д", Color(0xFF2F6E9E), .80f, .24f), NbP("Олег", "О", Color(0xFF5C6661), .84f, .70f),
    NbP("Дана", "Д", Color(0xFFC0443C), .18f, .74f), NbP("Ерлан", "Е", Color(0xFF6A4C9C), .52f, .90f))

/**
 * Board «Группа рядом» (idea, marked on the board): radar with pulse + sweep, people popping in, pick who to add, room code, create group.
 * Found people are a simulation → only on the demo store; a real account keeps searching with nobody found.
 */
@Composable
fun NearbyScreen(ctx: KxScreenCtx) {
    // demo: needs store (needs a kiroX server — Telegram removed «People nearby», see the note on the board)
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    var found by remember { mutableIntStateOf(0) }
    val pick = remember { mutableStateListOf(true, true, false, false, false) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(demo) { if (demo) while (found < NbPeople.size) { delay(1100); found++ } }
    val n = (0 until found).count { pick[it] }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                Column(Modifier.weight(1f)) { Text("Группа рядом", style = ts(20.sp, FontWeight.ExtraBold)); Text("Для тех, кто сейчас в одной комнате", style = ts(12.sp, color = Kx.text3)) }
            }
            // critique-B: the «idea» banner only on the demo store (a real account never reaches this board, see ExtraDevOnly).
            if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.warning.copy(alpha = .1f))
                .border(1.dp, Kx.warning.copy(alpha = .35f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Construction, null, Modifier.size(18.dp), tint = Kx.warning)
                Text("Идея, не решено. Telegram убрал «Людей рядом», поэтому для этого экрана нужен свой сервер kiroX. Без него экран не делаем.", style = ts(12.sp, color = Color(0xFFF2D9A6), lh = 17.sp))
            }
            Box(Modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(300.dp)) {
                    Box(Modifier.matchParentSize().border(1.dp, Kx.accent.copy(alpha = .15f), CircleShape))
                    Box(Modifier.matchParentSize().padding(50.dp).border(1.dp, Kx.accent.copy(alpha = .18f), CircleShape))
                    Box(Modifier.matchParentSize().padding(100.dp).border(1.dp, Kx.accent.copy(alpha = .22f), CircleShape))
                    if (!done) {
                        val p = kfLoop(2400, label = "pulse")
                        Box(Modifier.matchParentSize().graphicsLayer { val s = .3f + .7f * EaseOutCss.transform(p); scaleX = s; scaleY = s; alpha = .8f * (1f - p) }.border(2.dp, Kx.accent.copy(alpha = .5f), CircleShape))
                    }
                    val sw = kfRun(3000, 2)
                    Box(Modifier.matchParentSize().graphicsLayer { rotationZ = if (sw < 0f) 0f else 360f * sw }.clip(CircleShape).drawBehind {
                        rotate(-90f) { drawCircle(Brush.sweepGradient(0f to Kx.accent.copy(alpha = .35f), .25f to Color.Transparent, 1f to Color.Transparent)) }
                    })
                    Box(Modifier.align(Alignment.Center).size(64.dp).boxShadow(Kx.accent.copy(alpha = .5f), 30.dp, radius = 32.dp).clip(CircleShape).lin2(135f, Kx.accentMid, Kx.accentDeep),
                        contentAlignment = Alignment.Center) { Text("Вы", style = ts(22.sp, FontWeight.Black, Kx.onAccent)) }
                    NbPeople.take(found).forEachIndexed { i, d ->
                        val on = pick[i]
                        key(i) {
                            val p = enter(400, easing = CubicBezierEasing(.3f, 1.4f, .5f, 1f))
                            Column(Modifier.layout { m, c ->
                                val pl = m.measure(c.copy(minWidth = 0, minHeight = 0))
                                layout(c.maxWidth, c.maxHeight) { pl.place((c.maxWidth * d.x - pl.width / 2f).toInt(), (c.maxHeight * d.y - pl.height / 2f).toInt()) }
                            }.graphicsLayer { scaleX = p; scaleY = p }.kxClick(d.name + if (on) ", выбрано" else "") { if (!done) pick[i] = !on },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(54.dp).then(if (on) Modifier.border(2.dp, Kx.accent, CircleShape) else Modifier).padding(3.dp)) { ExLetter(d.l, 48.dp, d.c, font = 18.sp, ink = Kx.text) }
                                Text(d.name, style = ts(11.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2))
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (done) "Группа «Митап 22.11» создана" else if (found < NbPeople.size) "Ищем людей рядом…" else "Нашли ${NbPeople.size} человек", style = ts(16.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center)
                Text(if (done) "Участникам пришло приглашение" else "Нажмите на тех, кого добавить · выбрано $n", style = ts(13.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Код комнаты — скажите его вслух", style = ts(12.sp, color = Kx.text3), modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("4", "7", "1", "9").forEach { c ->
                        Box(Modifier.size(32.dp, 40.dp).clip(RoundedCornerShape(10.dp)).background(Kx.accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Text(c, style = ts(22.sp, FontWeight.Black, Kx.accent)) }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
                val act = n > 0 && !done
                val ink = if (act) Kx.onAccent else Kx.text3
                Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).then(if (act) Modifier.lin2(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                    .kxClick(if (done) "Открыть группу" else "Создать группу") { if (n > 0) done = true }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.GroupAdd, null, Modifier.size(24.dp), tint = ink)
                    Text(if (done) "Открыть группу" else "Создать группу · ${n + 1}", style = ts(16.sp, FontWeight.ExtraBold, ink))
                }
                Text("Видят только те, кто тоже открыл этот экран и ввёл код. Местоположение не сохраняется.", style = ts(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }
}
