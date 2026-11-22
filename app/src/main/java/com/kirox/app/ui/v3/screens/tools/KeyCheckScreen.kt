package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxIconButton
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val KC_KEY = listOf("🦊", "🌵", "🚲", "🍋")
private val KC_COLORS = listOf(Color(0xFF8FD3AC), Color(0xFFE8B04A), Color(0xFF6FA9D8), Color(0xFFF07AA0), Color(0xFFB39DDB))

/** Board «Сверьте ключ» (call encryption): pulsing shield, 4 flipping emoji, how-to steps, match / mismatch with confetti or shake. */
@Composable
fun KeyCheckScreen(ctx: KxScreenCtx) {
    // demo: needs store (active call: peer name, duration, emoji fingerprint; discardCall) — emoji/peer are demo
    var res by remember { mutableStateOf("") }
    var sec by remember { mutableIntStateOf(254) }
    var n by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); sec++ } }
    val ok = res == "ok"; val bad = res == "bad"
    val glowC by animateColorAsState(if (ok) Kx.accentDeep.copy(alpha = .3f) else if (bad) Kx.dangerSolid.copy(alpha = .25f) else Kx.accentDeep.copy(alpha = .14f), tween(500), label = "g")
    val shake = remember { Animatable(0f) }
    LaunchedEffect(res) { if (bad) { shake.snapTo(0f); shake.animateTo(1f, tween(450, easing = TEase)); shake.snapTo(0f) } }

    Column(Modifier.fillMaxSize().background(Kx.bg).drawBehind {
        drawRect(Brush.radialGradient(0f to glowC, .7f to Color.Transparent, center = Offset(size.width / 2, size.height * .15f), radius = size.width * .9f))
    }) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад к звонку", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            Column(Modifier.weight(1f)) {
                Text("Звонок · Анна", style = KxType.style(16.sp, FontWeight.ExtraBold))
                Text("${sec / 60}:" + (sec % 60).toString().padStart(2, '0'), style = KxType.style(12.sp, color = Kx.accent).copy(fontFeatureSettings = "tnum"))
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(start = 24.dp, end = 24.dp, top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(84.dp)) {
                val pulse = tCycles(2000, 2, easing = LinearOutSlowInEasing)
                if (pulse > 0f) Box(Modifier.matchParentSize().graphicsLayer { val s = 1f + .8f * pulse; scaleX = s; scaleY = s; alpha = .5f * (1f - pulse) }
                    .border(2.dp, if (bad) Kx.dangerSolid.copy(alpha = .6f) else Kx.accent.copy(alpha = .55f), CircleShape))
                val sh = remember(res) { Animatable(if (res.isEmpty()) 1f else 0f) }
                LaunchedEffect(res) { if (res.isNotEmpty()) sh.animateTo(1f, tween(500, easing = TEase)) }
                val ss = kf(sh.value, fa(0f, .6f, 1f), fa(.4f, 1.15f, 1f))
                Box(Modifier.matchParentSize().graphicsLayer { scaleX = ss; scaleY = ss; alpha = (sh.value / .6f).coerceAtMost(1f) }.clip(CircleShape)
                    .background(if (ok) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(if (bad) Kx.dangerSolid.copy(alpha = .18f) else Kx.accent.copy(alpha = .14f))),
                    contentAlignment = Alignment.Center) {
                    Icon(if (ok) Icons.Rounded.VerifiedUser else if (bad) Icons.Rounded.GppBad else Icons.Rounded.EnhancedEncryption, null, Modifier.size(44.dp),
                        tint = if (ok) Kx.onAccent else if (bad) Kx.danger else Kx.accent)
                }
                if (ok) repeat(12) { i ->
                    val c = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { delay(((i % 4) * 30).toLong()); c.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
                    val a = i / 12.0 * Math.PI * 2; val r = 70 + (i % 3) * 18
                    Box(Modifier.offset(38.dp, 38.dp).size(8.dp).graphicsLayer {
                        translationX = (kotlin.math.cos(a) * r).toFloat() * density * c.value; translationY = (kotlin.math.sin(a) * r).toFloat() * density * c.value
                        rotationZ = 260f * c.value; alpha = 1f - c.value
                    }.clip(RoundedCornerShape(2.dp)).background(KC_COLORS[i % KC_COLORS.size]))
                }
            }
            Text(if (ok) "Звонок защищён" else if (bad) "Ключи не совпали" else "Сверьте ключ", style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 18.dp))
            Text(if (ok) "Вы проверили: звонок шифруется от устройства до устройства, никто посередине его не слышит."
                else if (bad) "Возможно, кто-то вмешивается в соединение. Лучше завершить звонок и позвонить ещё раз."
                else "Если у собеседника те же четыре эмодзи в том же порядке, звонок зашифрован и его никто не подслушивает.",
                style = KxType.style(14.sp, color = Kx.text3, lineHeight = 21.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp).widthIn(max = 300.dp))

            val kShape = RoundedCornerShape(28.dp)
            val kbd by animateColorAsState(if (ok) Kx.accent.copy(alpha = .6f) else if (bad) Kx.dangerSolid.copy(alpha = .5f) else Kx.w(.16f), tween(300), label = "kb")
            Row(Modifier.padding(top = 26.dp).graphicsLayer { translationX = shakeX(shake.value, 8f) * density }.clip(kShape)
                .background(Brush.verticalGradient(listOf(Kx.w(.12f), Kx.w(.04f)))).border(1.dp, kbd, kShape).padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                KC_KEY.forEachIndexed { i, ch ->
                    val f = remember(n) { Animatable(0f) }
                    LaunchedEffect(n) { delay(i * 120L); f.animateTo(1f, tween(600, easing = CubicBezierEasing(.3f, 1.3f, .5f, 1f))) }
                    val p = f.value.coerceIn(0f, 1f)
                    val ry = kf(p, fa(0f, .6f, 1f), fa(90f, -15f, 0f)); val sc = kf(p, fa(0f, .6f, 1f), fa(.6f, 1.1f, 1f))
                    Box(Modifier.size(58.dp).graphicsLayer { rotationY = ry; scaleX = sc; scaleY = sc; alpha = (p / .6f).coerceAtMost(1f); cameraDistance = 600f / 58f * 8f * density }
                        .clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
                        Text(ch, style = KxType.style(36.sp))
                    }
                }
            }
            Text("Попросите собеседника открыть такой же экран", style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 12.dp))
            if (res.isEmpty()) Column(Modifier.padding(top = 22.dp).fillMaxWidth().tRise(400, 300, 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Скажите вслух, что видите: лиса, кактус, велосипед, лимон", "Собеседник сравнит со своим экраном", "Нажмите, совпало или нет").forEachIndexed { i, t ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.03f)).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent))
                        }
                        Text(t, style = KxType.style(13.sp, color = Kx.text2))
                    }
                }
            }
        }
        Row(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (res.isEmpty()) {
                TPill("Не совпадают", Modifier.weight(1f), h = 54.dp, bg = solid(Kx.dangerSolid.copy(alpha = .12f)), ink = Kx.danger, border = Kx.dangerSolid.copy(alpha = .3f)) { res = "bad" }
                TPill("Совпадают", Modifier.weight(1.3f), Icons.Rounded.Verified, 54.dp, Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))) { res = "ok" }
            } else {
                TPill("Проверить снова", h = 54.dp, bg = solid(Kx.w(.07f)), ink = Kx.text, weight = FontWeight.Bold) { res = ""; n++ }
                TPill(if (ok) "Вернуться к звонку" else "Завершить звонок", Modifier.weight(1f), if (ok) Icons.Rounded.Call else Icons.Rounded.CallEnd, 54.dp,
                    if (ok) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.dangerSolid), if (ok) Kx.onAccent else Color.White) { ctx.back() }
            }
        }
    }
}
