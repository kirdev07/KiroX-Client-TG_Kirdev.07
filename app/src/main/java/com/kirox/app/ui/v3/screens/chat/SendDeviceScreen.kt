package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private class SdDev(val name: String, val icon: ImageVector, val st: String, val stInk: Color, val via: String)
private class SdFile(val name: String, val icon: ImageVector, val c: Color, val mb: Int)

// demo: needs store (own active devices with online state, send files to own device = upload to Saved with progress)
private val SdDevs = listOf(SdDev("Ноутбук", Icons.Rounded.LaptopMac, "рядом", Kx.accent, "Wi-Fi напрямую"), SdDev("Планшет", Icons.Rounded.TabletAndroid, "в сети", Kx.accent, "через облако"),
    SdDev("Рабочий ПК", Icons.Rounded.DesktopWindows, "активность: вчера", Kx.text4, "отправим, когда включится"))
private val SdFiles = listOf(SdFile("Презентация Compose.pdf", Icons.Rounded.PictureAsPdf, hex("#C0443C"), 18), SdFile("Фото с митапа (12)", Icons.Rounded.PhotoLibrary, hex("#2F6E9E"), 46),
    SdFile("notes.txt", Icons.Rounded.Description, hex("#5C6661"), 1))

/** Board «На своё устройство»: phone → device scene with flying files, device picker, per-file progress, send / queue for offline PC. */
@Composable
fun SendDeviceScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var dev by remember { mutableIntStateOf(0) }
    val prog = remember { mutableStateListOf(0f, 0f, 0f) }
    var phase by remember { mutableStateOf("idle") }
    var runKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(runKey) {
        if (runKey == 0) return@LaunchedEffect
        val speed = if (dev == 0) 9f else 3.5f
        while (true) {
            delay(120)
            for (i in 0..2) prog[i] = minOf(100f, prog[i] + speed * (40f / SdFiles[i].mb) * (.7f + (Math.random() * .6).toFloat()))
            if (prog.all { it >= 100f }) { phase = "done"; break }
        }
    }
    val d = SdDevs[dev]
    val sending = phase == "send"; val done = phase == "done"; val offline = dev == 2

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, onClick = ctx.back)
            Column(Modifier.weight(1f)) {
                Text("На своё устройство", style = ts(20, W800))
                Text("Без проводов и без лишних чатов", style = ts(12, color = Kx.text3))
            }
        }
        // Transfer scene
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth().height(170.dp).clip(RoundedCornerShape(26.dp)).background(Kx.w(.03f))
            .ellipseGlow(.5f, 0f, .9f, .8f, Kx.accentDeep.copy(alpha = .2f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(26.dp))) {
            var w by remember { mutableIntStateOf(0) }
            Canvas(Modifier.fillMaxSize().onSizeChanged { w = it.width }) {
                drawLine(if (sending) Kx.accent.copy(alpha = .6f) else Kx.w(.15f), Offset(100.dp.toPx(), size.height / 2), Offset(size.width - 108.dp.toPx(), size.height / 2), 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx())))
            }
            Box(Modifier.align(Alignment.CenterStart).padding(start = 30.dp).size(60.dp, 100.dp).border(2.dp, Kx.w(.4f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Smartphone, null, tint = Kx.text3)
            }
            DestDevice(Modifier.align(Alignment.CenterEnd).padding(end = 24.dp), d.icon, done)
            if (sending) listOf(0, 450, 900).forEach { FlyDoc(it, w) }
            Text(if (done) (if (offline) "В очереди — дойдёт, когда ПК включится" else "Готово! Файлы на «${d.name}»") else if (sending) "Передаём · ${d.via}" else "Выберите устройство и нажмите «Отправить»",
                style = ts(12, color = if (done) Kx.accent else Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 10.dp))
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text("Куда отправить", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SdDevs.forEachIndexed { i, x ->
                    val on = dev == i
                    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.04f), tween(200), label = "bg")
                    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "bd")
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp))
                        .kxClick(x.name) { if (!sending) { dev = i; phase = "idle"; for (j in 0..2) prog[j] = 0f } }.padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(x.icon, null, Modifier.size(28.dp), tint = if (on) Kx.accent else Kx.text3)
                        Text(x.name, style = ts(13, W700), modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(x.stInk)); Text(x.st, style = ts(11, color = x.stInk), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text("Что отправить", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp))) {
                SdFiles.forEachIndexed { i, f ->
                    val p = prog[i]
                    Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.05f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1.dp.toPx()) }
                        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(f.c), contentAlignment = Alignment.Center) { Icon(f.icon, null, Modifier.size(20.dp), tint = Color.White) }
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = ts(14, W700), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val pa by animateFloatAsState(p / 100f, tween(200, easing = LinearEasing), label = "p")
                            // critique-A: the progress bar appears only once sending has started (no «loading» look before).
                            if (p > 0f) Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.08f))) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(pa).background(Kx.accent))
                            }
                        }
                        Text(if (p >= 100f) (if (offline) "в очереди" else "✓") else if (p > 0f) "${p.toInt()}%" else "${f.mb} МБ",
                            style = ts(12, W700, if (p >= 100f) Kx.accent else Kx.text3), textAlign = TextAlign.End, modifier = Modifier.width(52.dp))
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
            Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
                .then(if (sending) Modifier.background(Kx.w(.07f)) else Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep)).kxClick {
                    if (sending) return@kxClick
                    if (!demo) { ctx.toast("Отправка на свои устройства пока недоступна"); return@kxClick }
                    phase = "send"; for (j in 0..2) prog[j] = 0f; runKey++
                }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                val ink = if (sending) Kx.text3 else Kx.onAccent
                Icon(if (done) Icons.Rounded.Replay else if (sending) Icons.Rounded.HourglassTop else Icons.AutoMirrored.Rounded.Send, null, tint = ink)
                Text(if (done) "Отправить ещё раз" else if (sending) "Передаём…" else "Отправить на «${d.name}»", style = ts(16, W800, ink))
            }
            Text("Файл уходит через ваше облако Telegram — в Избранное, откроется на любом вашем устройстве", style = ts(11, color = Kx.text4), textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth())
        }
    }
}

/** Destination 96×70 frame; turns green with kxGlow (30px halo .35, 1.6s, twice) when done. */
@Composable
private fun DestDevice(modifier: Modifier, icon: ImageVector, done: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val g = remember { Animatable(0f) }
    LaunchedEffect(done) { if (done && !reduced) repeat(2) { g.snapTo(0f); g.animateTo(1f, tween(1600, easing = LinearEasing)) } else g.snapTo(0f) }
    val bd by animateColorAsState(if (done) Kx.accent else Kx.w(.4f), tween(300), label = "bd")
    val bg by animateColorAsState(if (done) Kx.accent.copy(alpha = .12f) else Color.Transparent, tween(300), label = "bg")
    val glow = kotlin.math.sin(Math.PI * g.value).toFloat()
    Box(modifier.size(96.dp, 70.dp).drawBehind {
        if (glow > 0f) for (k in 1..6) {
            val e = (4 + k * 5).dp.toPx() * glow
            drawRoundRect(Kx.accent.copy(alpha = .35f * glow / (k + 1)), Offset(-e / 2, -e / 2), Size(size.width + e, size.height + e), CornerRadius(10.dp.toPx() + e / 2), style = Stroke(5.dp.toPx()))
        }
    }.clip(RoundedCornerShape(10.dp)).background(bg).border(2.dp, bd, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (done) Kx.accent else Kx.text3)
    }
}

/** description icon flying left 20 % → 80 % (fade in/out, scale .6), 1.4s, twice. */
@Composable
private fun FlyDoc(delayMs: Int, width: Int) {
    val p = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(Unit) { if (!reduced) { delay(delayMs.toLong()); repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(1400, easing = LinearEasing)) } } }
    val v = p.value
    if (v <= 0f || v >= 1f) return
    val a = when { v < .15f -> v / .15f; v > .85f -> (1f - v) / .15f; else -> 1f }
    val s = if (v < .15f) .6f + .4f * v / .15f else if (v > .85f) .6f + .4f * (1f - v) / .15f else 1f
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        Icon(Icons.Rounded.Description, null, Modifier.size(22.dp).graphicsLayer { translationX = width * (.2f + .6f * v); alpha = a; scaleX = s; scaleY = s }, tint = Kx.accent)
    }
}
