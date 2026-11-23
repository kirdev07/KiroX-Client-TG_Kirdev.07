package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private val DsTilts = listOf(-6f, 4f, -2f, 7f)

/** Board «Сканер документов»: viewfinder with a detected page frame + scan line, flash and snap into the page stack, filters, send as PDF. */
@Composable
fun DocScanScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var pages by remember { mutableIntStateOf(0) }
    var snap by remember { mutableStateOf(false) }
    var snapKey by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf("bw") }
    var auto by remember { mutableStateOf(true) }
    var tilt by remember { mutableIntStateOf(0) }
    var sent by remember { mutableStateOf(false) }
    LaunchedEffect(snapKey) { if (snapKey > 0) { delay(500); snap = false; pages++; tilt = (tilt + 1) % DsTilts.size } }
    val reduced = LocalKxReducedMotion.current
    val bw = filter == "bw"; val orig = filter == "orig"
    val paper by animateColorAsState(if (orig) hex("#E8DCC4") else if (bw) Color.White else hex("#F6F1E4"), tween(300), label = "paper")
    val ink = if (bw) hex("#0A0C0B") else hex("#1F3A6E"); val ink2 = if (bw) hex("#3A3A3A") else hex("#6E7C8C")
    val rot by animateFloatAsState(DsTilts[tilt], tween(400), label = "tilt")

    Column(Modifier.fillMaxSize().background(hex("#050606")).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChRound(Icons.Outlined.Close, "Закрыть", 44.dp, bg = Kx.w(.1f), border = Color.Transparent, onClick = ctx.back)
            Text("Сканер документов", style = ts(15, W800), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
            Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (auto) Kx.accent.copy(alpha = .18f) else Kx.w(.1f)).kxClick { auto = !auto }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center) { Text(if (auto) "Автосъёмка" else "Вручную", style = ts(12, W800, if (auto) Kx.accent else Kx.text)) }
        }
        // Viewfinder
        Box(Modifier.weight(1f).padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(hex("#14100C"))
            .ellipseGlow(.4f, .3f, .8f, .6f, hex("#3A2E22")), contentAlignment = Alignment.Center) {
            val sp = remember { Animatable(0f) }
            LaunchedEffect(snapKey) { if (snapKey > 0 && !reduced) { sp.snapTo(0f); sp.animateTo(1f, tween(500, easing = CubicBezierEasing(.5f, 0f, .8f, .4f))) } }
            val s = if (snap) sp.value else 0f
            Column(Modifier.size(230.dp, 310.dp).graphicsLayer {
                rotationZ = rot; translationX = -120.dp.toPx() * s; translationY = 330.dp.toPx() * s
                val sc = 1f - .82f * s; scaleX = sc; scaleY = sc; alpha = 1f - .4f * s
            }.shadow(40.dp, RoundedCornerShape(4.dp), ambientColor = Color.Black.copy(alpha = .6f), spotColor = Color.Black.copy(alpha = .6f))
                .clip(RoundedCornerShape(4.dp)).background(paper).padding(horizontal = 18.dp, vertical = 22.dp)) {
                Box(Modifier.fillMaxWidth(.6f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(ink))
                listOf(14 to .9f, 8 to .84f, 8 to .7f, 20 to .88f, 8 to .92f, 8 to .6f).forEach { (top, w) ->
                    Box(Modifier.padding(top = top.dp).fillMaxWidth(w).height(6.dp).clip(RoundedCornerShape(3.dp)).background(ink2))
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.align(Alignment.End).padding(bottom = 4.dp).size(70.dp, 30.dp).graphicsLayer { rotationZ = -12f; alpha = .7f }.border(2.dp, ink, androidx.compose.foundation.shape.GenericShape { sz, _ ->
                    addOval(androidx.compose.ui.geometry.Rect(0f, 0f, sz.width, sz.height)) }))
            }
            if (!snap && !sent) key(pages) { DetectFrame(rot) }
            if (snap) key(snapKey) {
                val f = remember { Animatable(.9f) }
                LaunchedEffect(Unit) { f.animateTo(0f, tween(350)) }
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = f.value }.background(Color.White))
            }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)) {
                val hp = remember { Animatable(0f) }
                LaunchedEffect(Unit) { if (!reduced) repeat(2) { hp.snapTo(0f); hp.animateTo(1f, tween(1600, easing = LinearEasing)) } else hp.snapTo(1f) }
                val ha = if (hp.value >= 1f) 1f else .6f + .4f * kotlin.math.sin(Math.PI * hp.value).toFloat()
                Text(if (sent) "Отправлено в «Работа» как PDF" else if (snap) "Снято!" else if (auto) "Документ найден — держите ровно" else "Нажмите на кнопку", style = ts(13, W700),
                    modifier = Modifier.graphicsLayer { alpha = ha }.clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 14.dp, vertical = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            listOf("Цвет" to "color", "Ч/Б документ" to "bw", "Оригинал" to "orig").forEach { (l, k) ->
                val on = filter == k
                Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .18f) else Kx.w(.08f)).kxClick(l) { filter = k }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center) { Text(l, style = ts(12, W700, if (on) Kx.accent else Kx.text2)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(56.dp)) {
                repeat(minOf(3, pages)) { i ->
                    key(i) {
                        Box(Modifier.fillMaxSize().chPop(.8f, 300, fade = true).graphicsLayer { rotationZ = i * 6f - 6f; translationX = (i * 2).dp.toPx(); translationY = (-i * 2).dp.toPx() }
                            .clip(RoundedCornerShape(10.dp)).background(hex("#F2EFE6")).border(2.dp, Kx.bg, RoundedCornerShape(10.dp)))
                    }
                }
                if (pages > 0) Box(Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).heightIn(min = 22.dp).widthIn(min = 22.dp).clip(CircleShape).background(Kx.accent).padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center) { Text("$pages", style = ts(12, W800, Kx.onAccent)) }
            }
            val shutter by animateColorAsState(if (snap) Kx.accent else Kx.text, tween(200), label = "sh")
            Box(Modifier.size(76.dp).border(4.dp, Kx.text, CircleShape).clip(CircleShape).kxClick("Снять страницу") {
                if (!sent && !snap) { snap = true; snapKey++ }
            }, contentAlignment = Alignment.Center) { Box(Modifier.size(60.dp).clip(CircleShape).background(shutter)) }
            val ready = pages > 0 && !sent
            Box(Modifier.size(56.dp).clip(CircleShape).then(if (ready) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.1f))).kxClick("Готово") {
                if (sent) { sent = false; pages = 0 }
                else if (pages > 0) {
                    // demo: needs store (camera document scanner + send pages as one PDF to a chat)
                    if (demo) sent = true else ctx.toast("Отправка сканов пока недоступна")
                }
            }, contentAlignment = Alignment.Center) {
                Icon(if (sent) Icons.Rounded.Replay else Icons.AutoMirrored.Rounded.Send, null, tint = if (ready) Kx.onAccent else Kx.text3)
            }
        }
    }
}

/** 240×320 green frame with corner dots, glow and a scan line (top 6 % ↔ 90 %, 1.6s alternate, endless while detecting). */
@Composable
private fun DetectFrame(rot: Float) {
    val reduced = LocalKxReducedMotion.current
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(.06f, .9f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    Box(Modifier.size(240.dp, 320.dp).chPop(.8f, 300).graphicsLayer { rotationZ = rot }
        .shadow(20.dp, RoundedCornerShape(6.dp), clip = false, ambientColor = Kx.accent.copy(alpha = .4f), spotColor = Kx.accent.copy(alpha = .4f))
        .background(Kx.accent.copy(alpha = .08f), RoundedCornerShape(6.dp)).border(3.dp, Kx.accent, RoundedCornerShape(6.dp))
        .drawBehind {
            val r = 6.dp.toPx(); val o = -1.dp.toPx()
            listOf(Offset(o, o), Offset(size.width - o, o), Offset(o, size.height - o), Offset(size.width - o, size.height - o)).forEach { drawCircle(Kx.accent, r, it) }
            if (!reduced) {
                val yy = size.height * y
                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Kx.accent, Color.Transparent)), Offset(0f, yy), androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx()))
            }
        })
}
