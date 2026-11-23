package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** Board «Открыть подарок»: gift bubble wobbles → tap → box shakes, lid flies, teddy pops with rays + sparks → keep / convert to stars / show in profile. */
@Composable
fun GiftOpenScreen(ctx: KxScreenCtx) {
    if (ctx.store !is KxDemoStore) { com.kirox.app.ui.v3.screens.more.GiftsLive(ctx); return }
    val demo = ctx.store is KxDemoStore
    var phase by remember { mutableStateOf("closed") }
    var show by remember { mutableStateOf(true) }
    // Demo opening animation. Real account: GiftsLive (received gifts, show/hide, convert to stars behind a confirm).
    var stars by remember { mutableIntStateOf(120) }
    var sk by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf("") }
    var openKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(openKey) { if (openKey > 0) { phase = "shake"; delay(900); phase = "lid"; delay(600); phase = "open" } }
    val reduced = LocalKxReducedMotion.current

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                LetterAv("Л", hex("#6A4C9C"), 40.dp, 16)
                Column(Modifier.weight(1f)) { Text("Лиза", style = ts(16, W800)); Text("в сети", style = ts(12, color = Kx.accent)) }
                key(sk) {
                    val c = remember { Animatable(if (sk > 0 && !reduced) 0f else 1f) }
                    LaunchedEffect(Unit) { c.animateTo(1f, tween(500)) }
                    Box(Modifier.graphicsLayer { val s = 1f + .3f * sin(Math.PI * c.value).toFloat(); scaleX = s; scaleY = s }.height(32.dp).clip(RoundedCornerShape(16.dp))
                        .background(Kx.warning.copy(alpha = .14f)).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text("★ $stars", style = ts(13, W800, Kx.warning)) }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                Text("С днём рождения!!! 🥳", style = ts(15), modifier = Modifier.fillMaxWidth(.78f).wrapContentWidth(Alignment.Start)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp))
                val done = phase == "done"
                Column(Modifier.width(230.dp).clip(RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)).background(Brush.verticalGradient(listOf(Kx.warning.copy(alpha = .16f), Kx.w(.04f))))
                    .border(1.dp, Kx.warning.copy(alpha = .3f), RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)).kxClick("Подарок · Лиза") { openKey++ }.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Wobble(if (done) (if (result == "stars") "⭐" else "🧸") else "🎁", phase == "closed")
                    Text(if (done) (if (result == "stars") "Обменяно на 40 ★" else "Плюшевый мишка") else "Подарок · Лиза", style = ts(15, W800), modifier = Modifier.padding(top = 6.dp))
                    Text(if (done) (if (result == "stars") "Звёзды на вашем балансе" else if (show) "Виден в вашем профиле" else "Скрыт из профиля") else "Нажмите, чтобы открыть",
                        style = ts(12, color = Kx.text2), modifier = Modifier.padding(top = 2.dp), textAlign = TextAlign.Center)
                    Box(Modifier.padding(top = 10.dp).height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.warning.copy(alpha = .2f)).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text(if (done) "Открыть ещё раз" else "Открыть", style = ts(13, W800, hex("#F2C66B")))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart) { Text("Сообщение", style = ts(15, color = Kx.text4)) }
        }

        if (phase == "shake" || phase == "lid" || phase == "open") {
            Box(Modifier.fillMaxSize().chIn(dy = 0.dp, durationMs = 250).drawBehind {
                val c = Offset(size.width / 2, size.height * .38f)
                val r = maxOf(kotlin.math.hypot(c.x, c.y), kotlin.math.hypot(c.x, size.height - c.y))
                drawRect(Brush.radialGradient(0f to Kx.warning.copy(alpha = .18f), .6f to Color.Black.copy(alpha = .88f), 1f to Color.Black.copy(alpha = .88f), center = c, radius = r))
            }.kxClick(null) {})
            Box(Modifier.statusBarsPadding().padding(top = 120.dp).fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                if (phase != "open") GiftBox(phase == "lid") else Reveal()
            }
            if (phase == "open") Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp).chIn(300, 14.dp, 400)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Плюшевый мишка", style = ts(22, W800))
                    Text("Лиза · стоимость 50 ★", style = ts(13, color = Kx.text2), modifier = Modifier.padding(top = 4.dp))
                }
                Text("«Пусть этот год будет самым тёплым! 💚»", style = ts(14, color = hex("#DDE5E0"), lh = 20.3.sp), modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp)).background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp))
                Row(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 52.dp).kxClick("Показать в профиле", role = androidx.compose.ui.semantics.Role.Switch) { show = !show }.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Badge, null, tint = Kx.text3)
                    Column(Modifier.weight(1f)) {
                        Text("Показать в профиле", style = ts(14, W700)); Text("Подарок увидят все, кто откроет ваш профиль", style = ts(12, color = Kx.text3))
                    }
                    BSwitch(show)
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(Kx.warning.copy(alpha = .14f)).kxClick {
                        if (demo) { stars += 40; sk++; result = "stars"; phase = "done" } else ctx.toast("Обмен подарков на звёзды пока недоступен")
                    }, contentAlignment = Alignment.Center) { Text("Обменять на 40 ★", style = ts(14, W800, hex("#F2C66B"))) }
                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).kxClick { result = "keep"; phase = "done" },
                        contentAlignment = Alignment.Center) { Text("Оставить", style = ts(15, W800, Kx.onAccent)) }
                }
                Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).kxClick { result = "keep"; phase = "done" }, contentAlignment = Alignment.Center) {
                    Text("Поблагодарить в чате", style = ts(14, W700, Kx.accent))
                }
            }
        }
    }
}

/** 64sp emoji; kxWobble (1.8s, twice) while the gift is closed. */
@Composable
private fun Wobble(e: String, on: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(on) { if (on && !reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(1800, easing = LinearEasing)) } else p.snapTo(0f) }
    val rot = keyLerp(p.value, floatArrayOf(0f, .15f, .3f, .45f, .6f, 1f), floatArrayOf(0f, -8f, 7f, -5f, 3f, 0f))
    Text(e, style = ts(64), modifier = Modifier.graphicsLayer { rotationZ = if (on) rot else 0f })
}

/** Piecewise-linear keyframes. */
private fun keyLerp(t: Float, at: FloatArray, v: FloatArray): Float {
    for (i in 1 until at.size) if (t <= at[i]) { val f = (t - at[i - 1]) / (at[i] - at[i - 1]); return v[i - 1] + (v[i] - v[i - 1]) * f }
    return v.last()
}

/** 150 red box with gold ribbon and 🎀; shakes (.3s loop) → lid flies (kxLid .5s) and the box shrinks away (kxBoxOut .5s, delay .25s). */
@Composable
private fun GiftBox(lid: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val shake = rememberInfiniteTransition(label = "shake")
    val sp by shake.animateFloat(0f, 1f, infiniteRepeatable(tween(300, easing = LinearEasing)), label = "sp")
    val lp = remember { Animatable(0f) }
    val out = remember { Animatable(0f) }
    LaunchedEffect(lid) { if (lid) { lp.animateTo(1f, tween(500, easing = CubicBezierEasing(.3f, .6f, .4f, 1f))) } }
    LaunchedEffect(lid) { if (lid) { delay(250); out.animateTo(1f, tween(500)) } }
    val sx = if (!lid && !reduced) keyLerp(sp, floatArrayOf(0f, .2f, .4f, .6f, .8f, 1f), floatArrayOf(0f, -6f, 6f, -4f, 4f, 0f)) else 0f
    val sr = if (!lid && !reduced) keyLerp(sp, floatArrayOf(0f, .2f, .4f, .6f, .8f, 1f), floatArrayOf(0f, -6f, 6f, -3f, 3f, 0f)) else 0f
    Box(Modifier.size(150.dp).graphicsLayer {
        translationX = sx.dp.toPx(); rotationZ = sr
        val s = 1f - .4f * out.value; scaleX = s; scaleY = s; translationY = 60.dp.toPx() * out.value * s; alpha = 1f - out.value
    }) {
        Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = 10.dp).fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)).cssGradient(135f, hex("#C0443C"), hex("#8E2E2A")),
            contentAlignment = Alignment.Center) { Box(Modifier.width(20.dp).fillMaxHeight().background(hex("#F2C66B"))) }
        Box(Modifier.padding(top = 30.dp).fillMaxWidth().height(34.dp).graphicsLayer {
            translationY = -220.dp.toPx() * lp.value; rotationZ = -40f * lp.value; alpha = 1f - lp.value
        }) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).cssGradient(135f, hex("#D85048"), hex("#A6372F")), contentAlignment = Alignment.Center) {
                Box(Modifier.width(22.dp).fillMaxHeight().background(hex("#F2C66B")))
            }
            Text("🎀", style = ts(40, lh = 40.sp), modifier = Modifier.align(Alignment.TopCenter).offset(y = (-26).dp))
        }
    }
}

/** Teddy reveal: rotating conic rays (12s, twice), 10 sparks flying out, kxGift pop then kxFloat (twice). */
@Composable
private fun Reveal() {
    val reduced = LocalKxReducedMotion.current
    val rays = remember { Animatable(0f) }
    val pop = remember { Animatable(if (reduced) 1f else 0f) }
    val fl = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { rays.snapTo(0f); rays.animateTo(1f, tween(12000, easing = LinearEasing)) } }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(600, easing = KxMotion.rise)); if (!reduced) repeat(2) { fl.snapTo(0f); fl.animateTo(1f, tween(2600, easing = LinearEasing)) } }
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = 360f * rays.value }) {
            val r = size.minDimension / 2; val far = kotlin.math.hypot(r, r)
            val gold = hex("#F2C66B").copy(alpha = .22f)
            val brush = Brush.radialGradient(0f to gold, .3f to gold, .7f to gold.copy(alpha = 0f), 1f to gold.copy(alpha = 0f), center = center, radius = far)
            repeat(18) { k -> drawArc(brush, -90f + k * 20f, 10f, true, Offset(center.x - r, center.y - r), androidx.compose.ui.geometry.Size(2 * r, 2 * r)) }
        }
        repeat(10) { i -> Spark(if (i % 2 == 1) "✨" else "⭐", (i / 10.0 * Math.PI * 2), i * 40) }
        val p = pop.value
        val s = if (p < .6f) 1.25f * p / .6f else 1.25f - .25f * (p - .6f) / .4f
        val rot = if (p < .6f) -30f + 38f * p / .6f else 8f - 8f * (p - .6f) / .4f
        val y = ((1 - cos(2 * Math.PI * fl.value)) / 2).toFloat()
        Text("🧸", style = ts(110), modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s; rotationZ = rot; translationY = -8.dp.toPx() * y })
    }
}

@Composable
private fun Spark(e: String, a: Double, delayMs: Int) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing)) }
    val v = p.value
    Text(e, style = ts(18), modifier = Modifier.graphicsLayer {
        translationX = (cos(a) * 120).toFloat().dp.toPx() * v; translationY = (sin(a) * 120).toFloat().dp.toPx() * v
        val s = .4f + .6f * v; scaleX = s; scaleY = s; alpha = if (v < .2f) v / .2f else 1f - (v - .2f) / .8f
    })
}
