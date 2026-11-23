package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private val BrKinds: List<Triple<String, ImageVector, String>> = listOf(Triple("crash", Icons.Outlined.ErrorOutline, "Вылетает"), Triple("send", Icons.AutoMirrored.Outlined.Send, "Не отправляется"),
    Triple("slow", Icons.Outlined.HourglassBottom, "Тормозит"), Triple("look", Icons.Outlined.Palette, "Криво выглядит"), Triple("other", Icons.Outlined.MoreHoriz, "Другое"))

/** Board «Сообщить об ошибке»: kind chips, description, blurred screenshot with circle mark, tech log switch (what's in / not in), send → done. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BugReportScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    val context = androidx.compose.ui.platform.LocalContext.current
    var kind by remember { mutableStateOf("send") }
    var shot by remember { mutableStateOf(true) }
    var mark by remember { mutableStateOf(true) }
    var log by remember { mutableStateOf(true) }
    var st by remember { mutableStateOf("form") }
    var flying by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(flying) { if (flying) { delay(500); st = "done"; flying = false } }
    val reduced = LocalKxReducedMotion.current

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChRound(Icons.Outlined.Close, "Закрыть", 44.dp, border = Color.Transparent, onClick = ctx.back)
            // critique-A: the bug sits in the title area, not top-right where it read as a button.
            BugIcon()
            Text("Сообщить об ошибке", style = ts(20, W800), modifier = Modifier.weight(1f))
        }
        if (st == "form") {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Text("Что случилось?", style = ts(13, W700, Kx.accent))
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BrKinds.forEach { (k, ic, t) ->
                        val on = kind == k
                        Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                            .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(17.dp)).kxClick(t) { kind = k }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(ic, null, Modifier.size(16.dp), tint = if (on) Kx.accent else Kx.text2); Text(t, style = ts(13, W700, if (on) Kx.accent else Kx.text2))
                        }
                    }
                }
                Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(84.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)) { ChInput(note, { note = it }, "Что вы делали и что пошло не так?", Modifier.fillMaxSize(), size = 14, singleLine = false) }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Скриншот", style = ts(13, W700, Kx.accent), modifier = Modifier.weight(1f))
                    Text(if (shot) "Убрать" else "Приложить последний", style = ts(12, W800, if (shot) Kx.danger else Kx.accent), modifier = Modifier.kxClick { shot = !shot })
                }
                if (shot) Box(Modifier.padding(top = 8.dp).chIn(dy = 8.dp, durationMs = 250).fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp)).background(hex("#111614"))
                    .border(1.dp, Kx.w(.1f), RoundedCornerShape(18.dp))) {
                    Column(Modifier.fillMaxSize().padding(12.dp).graphicsLayer { alpha = .8f }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth(.5f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Kx.w(.15f)))
                        Box(Modifier.fillMaxWidth(.6f).height(34.dp).clip(RoundedCornerShape(12.dp)).background(Kx.w(.08f)))
                        Box(Modifier.align(Alignment.End).fillMaxWidth(.55f).height(34.dp).clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .35f)))
                        Box(Modifier.align(Alignment.End).fillMaxWidth(.4f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(Kx.danger.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
                            Text("⚠ не отправлено", style = ts(11, color = hex("#FFC4BE")))
                        }
                    }
                    if (mark) key(mark) { MarkEllipse() }
                    Row(Modifier.align(Alignment.BottomStart).padding(10.dp).height(26.dp).clip(RoundedCornerShape(13.dp)).background(Color.Black.copy(alpha = .5f)).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.BlurOn, null, Modifier.size(14.dp), tint = Kx.text); Text("Имена и текст размыты", style = ts(11, W700))
                    }
                    Row(Modifier.align(Alignment.TopEnd).padding(10.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .55f)).kxClick { mark = !mark }
                        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Draw, null, Modifier.size(16.dp), tint = Kx.warning); Text(if (mark) "Стереть отметку" else "Обвести", style = ts(12, W800, Kx.warning))
                    }
                }
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().heightIn(min = 56.dp).kxClick("Приложить технический журнал", role = androidx.compose.ui.semantics.Role.Switch) { log = !log },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Description, null, tint = Kx.text3)
                    Column(Modifier.weight(1f)) { Text("Приложить технический журнал", style = ts(15, W700)); Text("Поможет быстрее найти причину", style = ts(12, color = Kx.text3)) }
                    BSwitch(log)
                }
                if (log) Row(Modifier.chIn(dy = 8.dp, durationMs = 250).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Есть в журнале", style = ts(12, W800, Kx.accent))
                        Text("Версия kiroX 1.0.0-rc.1\nМодель телефона, Android\nКоды ошибок и время", style = ts(12, color = Kx.text2, lh = 19.2.sp), modifier = Modifier.padding(top = 6.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Нет в журнале", style = ts(12, W800, Kx.danger))
                        Text("Текст сообщений\nНомер телефона\nКлючи и пароли", style = ts(12, color = Kx.text2, lh = 19.2.sp), modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep)
                .kxClick {
                    // wave3: no report server — the real app hands the report (+ crash journal) to Android's share sheet; screenshots are not attached.
                    if (demo) flying = true
                    else com.kirox.app.ui.v3.data.KxBugReport.share(context, "Ошибка в kiroX: " + (BrKinds.firstOrNull { it.first == kind }?.third ?: ""), note, log)
                }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                val fp = remember { Animatable(0f) }
                LaunchedEffect(flying) { if (flying && !reduced) fp.animateTo(1f, tween(500, easing = FastOutLinearInEasing)) else fp.snapTo(0f) }
                Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp).graphicsLayer {
                    translationX = 160.dp.toPx() * fp.value; translationY = -260.dp.toPx() * fp.value; rotationZ = 20f * fp.value; alpha = 1f - fp.value
                }, tint = Kx.onAccent)
                Text("Отправить разработчику", style = ts(16, W800, Kx.onAccent))
            }
        } else Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.chPop(.5f, 500).size(88.dp).clip(RoundedCornerShape(30.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.TaskAlt, null, Modifier.size(46.dp), tint = Kx.onAccent)
            }
            Text("Спасибо, отчёт ушёл", style = ts(20, W800), modifier = Modifier.padding(top = 14.dp))
            Text("Номер отчёта #2611-84. Если понадобятся детали, разработчик напишет в «Помощь».", style = ts(14, color = Kx.text3, lh = 20.3.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            Box(Modifier.padding(top = 20.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f)).kxClick {
                st = "form"; kind = "send"; shot = true; mark = true; log = true
            }.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) { Text("Новый отчёт", style = ts(14, W700, Kx.accent)) }
        }
    }
}

/** bug_report with kxShake (±10° → ±6°, 1.4s, delay .3s, twice). */
@Composable
private fun BugIcon() {
    val p = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(Unit) { if (!reduced) { delay(300); repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(1400)) } } }
    val v = p.value
    val at = floatArrayOf(0f, .2f, .4f, .6f, .8f, 1f); val vs = floatArrayOf(0f, -10f, 10f, -6f, 6f, 0f)
    var rot = 0f
    for (i in 1 until at.size) if (v <= at[i]) { rot = vs[i - 1] + (vs[i] - vs[i - 1]) * (v - at[i - 1]) / (at[i] - at[i - 1]); break }
    Icon(Icons.Rounded.BugReport, null, Modifier.graphicsLayer { rotationZ = rot }, tint = Kx.warning)
}

/** Amber ellipse (cx 270/358, cy 150/200, rx 80, ry 34) drawn on with kxMark (.7s). */
@Composable
private fun MarkEllipse() {
    val p = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(Unit) { if (reduced) p.snapTo(1f) else p.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    Canvas(Modifier.fillMaxSize()) {
        val sx = size.width / 358f; val sy = size.height / 200f
        val path = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset((270 - 80) * sx, (150 - 34) * sy), Size(160 * sx, 68 * sy))) }
        val pm = PathMeasure().apply { setPath(path, false) }
        val seg = Path(); pm.getSegment(0f, pm.length * p.value, seg, true)
        drawPath(seg, Kx.warning, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
    }
}
