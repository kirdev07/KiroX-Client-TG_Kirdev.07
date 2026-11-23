package com.kirox.app.ui.v3.screens.extra

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private const val VoDur = 100
private val VoEmb = listOf(Color(0xFFFFD27A), Color(0xFFF07A5A), Color(0xFFFFB36B), Kx.warning)

/** Board «Одноразовое фото»: spoiler cover → 10 s view with the ring timer (FLAG_SECURE while open) → burn with embers → gone. */
@Composable
fun ViewOnceScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { val c = ctx.arg?.substringBefore(":")?.toLongOrNull(); if (c == null) com.kirox.app.ui.v3.screens.more.C3NeedChat(ctx, "Просмотр один раз") else com.kirox.app.ui.v3.screens.more.ViewOnceLive(ctx, c); return }
    // demo: needs store (view-once photo message: open → TDLib starts self-destruct, viewed state)
    var phase by remember { mutableStateOf("hidden") }
    var left by remember { mutableIntStateOf(VoDur) }
    var warn by remember { mutableIntStateOf(0) }
    val view = phase == "view"; val burn = phase == "burn"
    LaunchedEffect(phase) {
        if (phase == "view") while (true) { delay(100); val l = left - 1; if (l <= 0) { left = 0; phase = "burn"; break } else left = l }
        if (phase == "burn") { delay(900); phase = "gone" }
    }
    LaunchedEffect(warn) { if (warn > 0) { delay(2600); warn = 0 } }
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(view) {
        if (view) activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (view) activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF050606))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", bg = Kx.w(.08f), border = null) { ctx.back() }
                ExLetter("Л", 38.dp, Color(0xFFC0443C), Color(0xFF6A4C9C), 135f, Kx.text, 16.sp)
                Column(Modifier.weight(1f)) { Text("Лиза", style = ts(15.sp, FontWeight.ExtraBold)); Text("одноразовое фото · 21:40", style = ts(12.sp, color = Kx.text3)) }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    val frac by animateFloatAsState(left / VoDur.toFloat(), tween(100, easing = LinearEasing), label = "ring")
                    Canvas(Modifier.fillMaxSize()) {
                        val u = size.width / 44f; val r = 18f * u
                        drawCircle(Color.Black.copy(alpha = .35f), r); drawCircle(Kx.w(.12f), r, style = Stroke(3f * u))
                        rotate(-90f) { drawArc(Kx.accent, 0f, 360f * frac, false, Offset(center.x - r, center.y - r), androidx.compose.ui.geometry.Size(2 * r, 2 * r), style = Stroke(3f * u, cap = StrokeCap.Round)) }
                    }
                    Text("${(left + 9) / 10}", style = ts(14.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"))
                }
            }
            Box(Modifier.weight(1f).padding(horizontal = 12.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF0E1211))) {
                if (view || burn) {
                    key(burn) {
                        val inP = if (!burn) enter(250) else 1f
                        val b = if (burn) enter(900, easing = CubicBezierEasing(.42f, 0f, 1f, 1f)) else 0f
                        val blurDp = if (b < .6f) 6f * (b / .6f) else 6f + 10f * ((b - .6f) / .4f)
                        Box(Modifier.matchParentSize().graphicsLayer {
                            val s = (.9f + .1f * inP) * (if (b > .6f) 1f + .08f * ((b - .6f) / .4f) else 1f); scaleX = s; scaleY = s
                            alpha = inP * (if (b < .6f) 1f - .3f * (b / .6f) else .7f - .7f * ((b - .6f) / .4f))
                        }.blur(blurDp.dp)) {
                            Canvas(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color(0xFFF07A5A), .55f to Color(0xFF7A3B6A), 1f to Color(0xFF1E1530)))
                                .ellipseBg(.6f, .4f, .5f, .3f, 0f to Color(0xFFFFE3A3), .6f to Color(0x00FFE3A3))) {
                                val sx = size.width / 366f; val sy = size.height / 560f
                                val p = Path().apply { moveTo(0f, 420 * sy); lineTo(80 * sx, 330 * sy); lineTo(140 * sx, 380 * sy); lineTo(220 * sx, 290 * sy); lineTo(300 * sx, 360 * sy); lineTo(366 * sx, 320 * sy); lineTo(366 * sx, 560 * sy); lineTo(0f, 560 * sy); close() }
                                drawPath(p, Color(0xFF2A1A3A))
                                drawOval(Color(0xFFFFD27A).copy(alpha = .9f), Offset(137 * sx, 144 * sy), androidx.compose.ui.geometry.Size(92 * sx, 92 * sy))
                            }
                            Text("Сюрприз для мамы готов 🎁", style = ts(15.sp, FontWeight.Bold).copy(shadow = Shadow(Color.Black.copy(alpha = .5f), Offset(0f, 1f), 8f)),
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
                        }
                    }
                    if (burn) BoxWithConstraints(Modifier.matchParentSize()) {
                        repeat(14) { i ->
                            val p = enter(900, (i % 6) * 50, EaseOutCss)
                            if (p < 1f) Box(Modifier.offset(maxWidth * ((8 + i * 6.5f) / 100f), maxHeight * (1f - (20 + (i * 37) % 60) / 100f) - 6.dp).size(6.dp).graphicsLayer {
                                translationX = ((i % 5) - 2) * 18f * density * p; translationY = -140f * density * p; val s = 1f - p; scaleX = s; scaleY = s; alpha = 1f - p
                            }.clip(CircleShape).background(VoEmb[i % 4]))
                        }
                    }
                }
                if (phase == "hidden") {
                    val sp = kfLoop(3000, label = "spoil")
                    Column(Modifier.matchParentSize().background(Color(0xFF121816)).drawBehind {
                        val step = 10.dp.toPx(); val off = 30.dp.toPx() * sp
                        var y = -step + (off % step); while (y < size.height + step) { var x = -step + (off % step); while (x < size.width + step) {
                            drawCircle(Kx.w(.35f), 1.25.dp.toPx(), Offset(x + step / 2, y + step / 2)); x += step }; y += step }
                    }.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                        val p = enter(300)
                        Box(Modifier.graphicsLayer { alpha = p; val s = .9f + .1f * p; scaleX = s; scaleY = s }.size(74.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .14f))
                            .border(1.dp, Kx.accent.copy(alpha = .4f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.LooksOne, null, Modifier.size(38.dp), tint = Kx.accent) }
                        Text("Фото можно посмотреть один раз", style = ts(18.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center)
                        Text("Нажмите кнопку внизу — у вас будет 10 секунд. Потом фото сгорит", style = ts(13.sp, color = Kx.text3, lh = 18.85.sp), textAlign = TextAlign.Center)
                    }
                }
                if (phase == "gone") Column(Modifier.matchParentSize().exIn(400, 300, 0.dp).padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(54.dp), tint = Kx.dashed)
                    Text("Фото исчезло", style = ts(18.sp, FontWeight.ExtraBold))
                    Text("Лиза увидит, что вы его открыли", style = ts(13.sp, color = Kx.text3), textAlign = TextAlign.Center)
                }
                if (warn > 0) key(warn) {
                    val w = kfRun(600, 3)
                    Row(Modifier.padding(12.dp).fillMaxWidth().graphicsLayer { translationY = -3.dp.toPx() * tri(w) }.clip(RoundedCornerShape(16.dp)).background(Kx.dangerSolid.copy(alpha = .88f))
                        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.ScreenshotMonitor, null, Modifier.size(20.dp), tint = Color.White)
                        Text("Скриншоты одноразовых фото заблокированы", style = ts(13.sp, FontWeight.Bold, Color.White))
                    }
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.Outlined.Screenshot, "Сделать скриншот", Modifier.graphicsLayer { alpha = if (view) 1f else .35f }, size = 54.dp, bg = Kx.w(.07f), border = null) { if (view) warn++ }
                val gone = phase == "gone"
                val a by animateFloatAsState(if (view || gone) 0f else 1f, tween(200), label = "mb")
                val bg by animateColorAsState(if (view) Kx.accent.copy(alpha = .18f) else Kx.w(.07f), tween(200), label = "mbg")
                val ink = if (view) Kx.accent else if (gone) Kx.text2 else Kx.onAccent
                val text = if (view) "Закрыть сейчас" else if (burn) "Исчезает…" else if (gone) "Показать пример ещё раз" else "Открыть фото"
                Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(bg).drawBehind {
                    drawRect(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep), cssAngleStart(135f, size), cssAngleEnd(135f, size)), alpha = a)
                }.kxClick(text) {
                    when (phase) { "gone" -> { phase = "hidden"; left = VoDur }; "hidden" -> phase = "view"; "view" -> { left = 0; phase = "burn" } }
                }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (view) Icons.Rounded.Close else if (gone) Icons.Rounded.Replay else Icons.Rounded.Visibility, null, Modifier.size(24.dp), tint = ink)
                    Text(text, style = ts(16.sp, FontWeight.ExtraBold, ink))
                }
            }
        }
    }
}
