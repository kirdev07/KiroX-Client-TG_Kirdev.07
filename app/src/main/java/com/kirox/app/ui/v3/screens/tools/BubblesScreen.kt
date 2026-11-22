package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

private data class BbChat(val name: String, val l: String, val c: List<Color>, val n: Int, val msgs: List<Pair<String, Boolean>>)
private val BB_INIT = listOf(
    BbChat("Анна", "А", listOf(Color(0xFFC0443C), Color(0xFF6A4C9C)), 2, listOf("Ты уже в пути?" to false, "Мы у входа на каток 🙂" to false)),
    BbChat("Мама", "М", listOf(Color(0xFFB07A2E), Color(0xFF6A4C9C)), 1, listOf("Возьми шарф, там холодно" to false)))
private val BB_QUICK = listOf("Еду, буду через 20 минут", "Уже паркуюсь", "👍")

/** Board «Пузыри чатов»: another app (map) underneath, floating chat heads with badges, expanded mini chat with quick replies, dismiss target. */
@Composable
fun BubblesScreen(ctx: KxScreenCtx) {
    // demo: needs Android Bubbles (Notification.BubbleMetadata) + a bubble activity; chats are demo
    var list by remember { mutableStateOf(BB_INIT) }
    var open by remember { mutableIntStateOf(-1) }
    var gone by remember { mutableStateOf(false) }
    var gen by remember { mutableIntStateOf(0) }
    val cur = if (open >= 0) list[open] else list[0]

    Box(Modifier.fillMaxSize().background(Color(0xFFF3F1EC))) {
        // The other app: a map
        Canvas(Modifier.matchParentSize()) {
            val u = density
            drawRect(Color(0xFFE9E6DE))
            var x = 0f; while (x < size.width) { drawLine(Color.Black.copy(alpha = .05f), Offset(x, 0f), Offset(x, size.height), u); x += 40 * u }
            var y = 0f; while (y < size.height) { drawLine(Color.Black.copy(alpha = .05f), Offset(0f, y), Offset(size.width, y), u); y += 40 * u }
            drawRect(Color.White, Offset(0f, 300 * u), Size(size.width, 22 * u)); drawRect(Color.White, Offset(180 * u, 0f), Size(22 * u, size.height))
            drawRoundRect(Color(0xFFCFE3C8), Offset(30 * u, 120 * u), Size(120 * u, 140 * u), CornerRadius(12 * u))
            drawRoundRect(Color(0xFFC9DCEB), Offset(size.width - 170 * u, 420 * u), Size(140 * u, 120 * u), CornerRadius(12 * u))
            val p = Path().apply { moveTo(90 * u, 620 * u); cubicTo(150 * u, 520 * u, 170 * u, 420 * u, 191 * u, 311 * u); cubicTo(212 * u, 202 * u, 260 * u, 200 * u, 300 * u, 140 * u) }
            drawPath(p, Color(0xFF3A7BD5), style = Stroke(6 * u, cap = StrokeCap.Round))
            drawCircle(Kx.dangerSolid, 10 * u, Offset(300 * u, 140 * u))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().height(50.dp).shadow(7.dp, RoundedCornerShape(25.dp)).clip(RoundedCornerShape(25.dp)).background(Color.White)
                .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(24.dp), tint = Kx.text4); Text("Медеу", style = KxType.style(15.sp, color = Kx.text4))
            }
            Box(Modifier.padding(start = 16.dp, top = 10.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .6f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("Другое приложение — карты", style = KxType.style(11.sp, color = Color.White))
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 20.dp).fillMaxWidth().shadow(7.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                .background(Color.White).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("24 мин · 11 км", style = KxType.style(17.sp, FontWeight.ExtraBold, Color(0xFF1C1F1D)))
                Text("Через пр. Достык · пробок нет", style = KxType.style(13.sp, color = Kx.text4))
            }
        }
        // Expanded chat
        if (open >= 0) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .35f)).kxClick("Свернуть") { open = -1 })
            key(open) {
                val a = remember { Animatable(0f) }
                LaunchedEffect(Unit) { a.animateTo(1f, tween(300, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) }
                val sh = RoundedCornerShape(28.dp)
                Column(Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 150.dp).fillMaxWidth().height(470.dp)
                    .graphicsLayer { transformOrigin = TransformOrigin(1f, 0f); val s = .9f + .1f * a.value; scaleX = s; scaleY = s; translationY = -(1f - a.value) * 20.dp.toPx(); alpha = a.value }
                    .shadow(30.dp, sh).clip(sh).background(Kx.bg)) {
                    Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(cur.c)), contentAlignment = Alignment.Center) { Text(cur.l, style = KxType.style(15.sp, FontWeight.ExtraBold)) }
                        Column(Modifier.weight(1f)) { Text(cur.name, style = KxType.style(15.sp, FontWeight.ExtraBold)); Text("в сети", style = KxType.style(11.sp, color = Kx.accent)) }
                        TRoundBtn(Icons.Outlined.OpenInFull, "Открыть в kiroX", 36.dp, 20.dp) { open = -1 }
                        TRoundBtn(Icons.Outlined.Remove, "Свернуть в пузырь", 36.dp, 20.dp) { open = -1 }
                    }
                    Column(Modifier.fillMaxWidth().weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                        cur.msgs.forEachIndexed { i, (t, mine) ->
                            key(i) {
                                val r = if (mine) RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp)
                                Box(Modifier.align(if (mine) Alignment.End else Alignment.Start).fillMaxWidth(.8f).wrapContentWidth(if (mine) Alignment.End else Alignment.Start).tRise(0, 250, 8.dp)
                                    .clip(r).background(if (mine) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.w(.08f))).padding(horizontal = 11.dp, vertical = 7.dp)) {
                                    Text(t, style = KxType.style(14.sp, color = if (mine) Kx.onAccent else Kx.text))
                                }
                            }
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BB_QUICK.forEach { q ->
                            Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.accent.copy(alpha = .12f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(17.dp))
                                .kxClick { list = list.mapIndexed { i, b -> if (i == open) b.copy(msgs = b.msgs + (q to true)) else b } }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                Text(q, style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
                            }
                        }
                    }
                }
            }
        }
        // Bubble stack
        val top by animateDpAsState(if (open >= 0) 86.dp else 220.dp, tween(350, easing = CubicBezierEasing(.2f, .8f, .2f, 1f)), label = "top")
        if (!gone) Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 10.dp, top = top), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            list.forEachIndexed { i, b ->
                key(gen, i) {
                    val p = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(i * 80L); p.animateTo(1f, tween(400, easing = CubicBezierEasing(.3f, 1.4f, .5f, 1f))) }
                    val s = kf(p.value.coerceIn(0f, 1f), fa(0f, .6f, 1f), fa(0f, 1.15f, 1f))
                    Box(Modifier.size(58.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = (p.value / .6f).coerceIn(0f, 1f) }) {
                        Box(Modifier.matchParentSize().shadow(11.dp, CircleShape).clip(CircleShape).background(Brush.linearGradient(b.c)).border(2.5.dp, if (open == i) Kx.accent else Color.White, CircleShape)
                            .kxClick(b.name + if (b.n > 0) ", ${b.n} новых" else "") { open = if (open == i) -1 else i; list = list.mapIndexed { k, x -> if (k == i) x.copy(n = 0) else x } },
                            contentAlignment = Alignment.Center) { Text(b.l, style = KxType.style(22.sp, FontWeight.ExtraBold, Color.White)) }
                        if (b.n > 0 && open != i) {
                            val bd = tCycles(1400, 2, easing = TEaseInOut, key = gen)
                            val bs = 1f + .2f * (1f - kotlin.math.cos(bd * 2 * Math.PI).toFloat()) / 2f
                            Box(Modifier.align(Alignment.TopEnd).offset(3.dp, (-3).dp).graphicsLayer { scaleX = bs; scaleY = bs }.heightIn(min = 20.dp).widthIn(min = 20.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF3F1EC)).padding(2.dp).clip(RoundedCornerShape(8.dp)).background(Kx.accent).padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                                Text("${b.n}", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.onAccent))
                            }
                        }
                    }
                }
            }
        }
        if (!gone && open < 0) {
            val x = remember { Animatable(0f) }
            LaunchedEffect(Unit) { x.animateTo(1f, tween(300, easing = TEase)) }
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp).graphicsLayer { translationY = (1f - x.value) * 40.dp.toPx(); alpha = x.value }
                .size(60.dp).clip(CircleShape).background(Color.Black.copy(alpha = .55f)).kxClick("Убрать пузыри") { gone = true; open = -1 }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, null, Modifier.size(30.dp), tint = Color.White)
            }
        }
        if (gone) Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 130.dp).height(44.dp).shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
            .background(Kx.bg).kxClick { gone = false; list = BB_INIT; gen++ }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.ChatBubble, null, Modifier.size(18.dp), tint = Kx.accent)
            Text("Показать пример снова", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent))
        }
    }
}
