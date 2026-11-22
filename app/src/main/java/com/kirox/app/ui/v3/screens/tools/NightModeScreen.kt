package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

/** Board «Ночной режим»: animated sky preview, demo hour slider, when-to-switch radio, schedule steppers, night theme pick. */
@Composable
fun NightModeScreen(ctx: KxScreenCtx) {
    // Mode / schedule / night theme: KxPrefs («По расписанию» is the app's auto theme). The hour slider only
    // previews the sky (real store starts at the current hour). Sunset times: fixed 8–17 as on the board.
    val store = ctx.store
    val mode = store.pref(KxPrefs.NIGHT_MODE).coerceIn(0, 3)
    val from = store.pref(KxPrefs.NIGHT_FROM).mod(24)
    val to = store.pref(KxPrefs.NIGHT_TO).mod(24)
    var hour by rememberSaveable { mutableIntStateOf(if (store is com.kirox.app.ui.v3.demo.KxDemoStore) 23 else java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) }
    val theme = store.pref(KxPrefs.NIGHT_THEME)
    fun inRange(x: Int, a: Int, b: Int) = if (a <= b) x in a until b else (x >= a || x < b)
    val night = when (mode) { 0 -> true; 1 -> inRange(hour, from, to); 2 -> !inRange(hour, 8, 17); else -> hour >= 20 || hour < 7 }
    val isDay = inRange(hour, 7, 18); val dusk = hour == 17 || hour == 18 || hour == 6
    fun pad(n: Int) = n.toString().padStart(2, '0') + ":00"

    TScreen("Ночной режим", ctx.back) {
        // Sky preview
        val skyTop by animateColorAsState(if (dusk) Color(0xFF2C2F6E) else if (isDay) Color(0xFF5FA8D3) else Color(0xFF05060F), tween(600), label = "s1")
        val skyMid by animateColorAsState(if (dusk) Color(0xFFC0587E) else if (isDay) Color(0xFF83C0E0) else Color(0xFF141B3A), tween(600), label = "s2")
        val skyBot by animateColorAsState(if (dusk) Color(0xFFF2A65A) else if (isDay) Color(0xFFA9D8EE) else Color(0xFF1E2A4A), tween(600), label = "s3")
        val midStop = if (dusk) .65f else if (isDay) .5f else .7f
        val t = if (isDay) (hour - 7) / 11f else ((hour + 24 - 18) % 24) / 13f
        val sunX by animateFloatAsState(.12f + t * .76f, tween(600, easing = TEase), label = "sx")
        val sunY by animateFloatAsState(.80f - kotlin.math.sin(t * Math.PI).toFloat() * .60f, tween(600, easing = TEase), label = "sy")
        val orb by animateColorAsState(if (isDay) Color(0xFFFFE9A8) else Color(0xFFE6ECFF), tween(600), label = "orb")
        val orbGlow = if (isDay) Color(255, 220, 140).copy(alpha = .8f) else Color(200, 215, 255).copy(alpha = .6f)
        val hill by animateColorAsState(if (isDay) Kx.accentDeep else Color(0xFF0D1A14), tween(600), label = "hill")
        val starsOp by animateFloatAsState(if (isDay) 0f else 1f, tween(600), label = "stars")
        val reduced = LocalKxReducedMotion.current
        // seconds since the board opened; stops once every star finished its 2 twinkles
        val clockA = remember { Animatable(0f) }
        LaunchedEffect(reduced) { if (!reduced) clockA.animateTo(8f, tween(8000, easing = LinearEasing)) }
        val clock = clockA.value
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(0f to skyTop, midStop to skyMid, 1f to skyBot))) {
            Canvas(Modifier.matchParentSize()) {
                // kxTwinkle: 2 runs per star, then rest at full opacity
                val sec = clock
                repeat(14) { i ->
                    val dur = 1.5f + (i % 4) * .6f
                    val local = sec + i * .3f % dur
                    val a = if (reduced || local >= dur * 2) 1f else { val ph = (local / dur) % 1f; .2f + .8f * (1f - kotlin.math.cos(ph * 2 * Math.PI).toFloat()) / 2f * 1f }
                    val z = (i % 3 + 1.5f).dp.toPx()
                    val x = ((i * 37) % 95 + 2) / 100f * size.width; val y = ((i * 23) % 55 + 5) / 100f * size.height
                    drawCircle(Color.White.copy(alpha = a * starsOp), z / 2, Offset(x + z / 2, y + z / 2))
                }
                val c = Offset(sunX * size.width, sunY * size.height); val r = 27.dp.toPx()
                drawCircle(Brush.radialGradient(listOf(orbGlow, Color.Transparent), c, r + 50.dp.toPx()), r + 50.dp.toPx(), c)
                drawCircle(orb, r, c)
                // hill: left/right -30, bottom -30, 90 tall, radius 50% 50% 0 0
                val hw = size.width + 60.dp.toPx(); val hh = 90.dp.toPx()
                drawOval(hill, Offset(-30.dp.toPx(), size.height + 30.dp.toPx() - hh), androidx.compose.ui.geometry.Size(hw, hh * 2))
            }
            Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .35f)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(if (night) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null, Modifier.size(15.dp), tint = Kx.text)
                    Text(if (night) "Ночная тема включена" else "Дневная тема", style = KxType.style(12.sp, FontWeight.ExtraBold))
                }
                Spacer(Modifier.weight(1f))
                Text(pad(hour), style = KxType.style(22.sp, FontWeight.ExtraBold).copy(shadow = Shadow(Color.Black.copy(alpha = .4f), Offset(0f, 4f), 16f)))
            }
        }
        Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)) {
            TSlider(hour.toFloat(), 0f, 23f, "Время суток для проверки",
                track = Brush.horizontalGradient(0f to Color(0xFFF2C66A), .45f to Color(0xFFC0587E), .7f to Color(0xFF2C2F6E), 1f to Color(0xFF0D0F22))) { hour = it.toInt() }
            Row {
                Text("00:00", style = KxType.style(11.sp, color = Kx.text4)); Spacer(Modifier.weight(1f))
                Text("Макет: время суток", style = KxType.style(11.sp, color = Kx.text4)); Spacer(Modifier.weight(1f))
                Text("23:00", style = KxType.style(11.sp, color = Kx.text4))
            }
        }

        TCap("Когда включать")
        TGroup {
            listOf(
                Triple(Icons.Outlined.DarkMode, "Всегда тёмная", "Без переключений"),
                Triple(Icons.Outlined.Schedule, "По расписанию", "С ${pad(from)} до ${pad(to)}"),
                Triple(Icons.Outlined.WbTwilight, "От заката до рассвета", "Сегодня с 17:12 до 08:04"),
                Triple(Icons.Outlined.Smartphone, "Как в системе", "Следовать настройке Android"),
            ).forEachIndexed { i, (ic, label, sub) ->
                TRow(ic, label, sub, Modifier.topLine(i > 0), onClick = { store.putPref(KxPrefs.NIGHT_MODE, i) }) { TRadio(i == mode) }
            }
        }

        if (mode == 1) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Triple(Icons.Outlined.Bedtime, "Включать в", 0), Triple(Icons.Outlined.WbSunny, "Выключать в", 1)).forEach { (ic, label, k) ->
                    val v = if (k == 0) from else to
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(TTile).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(ic, null, Modifier.size(16.dp), tint = Kx.text3); Text(label, style = KxType.style(12.sp, color = Kx.text3))
                        }
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TRoundBtn(Icons.Rounded.Remove, "Раньше") { if (k == 0) store.putPref(KxPrefs.NIGHT_FROM, (from + 23) % 24) else store.putPref(KxPrefs.NIGHT_TO, (to + 23) % 24) }
                            Text(pad(v), style = KxType.style(24.sp, FontWeight.ExtraBold, Kx.accent), textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 70.dp))
                            TRoundBtn(Icons.Rounded.Add, "Позже") { if (k == 0) store.putPref(KxPrefs.NIGHT_FROM, (from + 1) % 24) else store.putPref(KxPrefs.NIGHT_TO, (to + 1) % 24) }
                        }
                    }
                }
            }
        }

        TCap("Тема ночью")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple("kiroX тёмная", Color(0xFF0A0C0B), Kx.accent), Triple("Чистый чёрный", Color.Black, Kx.accent), Triple("Ночное небо", Color(0xFF0D1222), Color(0xFF9FB7FF)))
                .forEachIndexed { i, (label, bg, a) ->
                    val bd by animateColorAsState(if (i == theme) Kx.accent.copy(alpha = .5f) else Color.Transparent, tween(200), label = "bd")
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(TTile).border(1.dp, bd, RoundedCornerShape(18.dp))
                        .kxClick(role = Role.RadioButton) { store.putPref(KxPrefs.NIGHT_THEME, i) }.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp)).background(bg).padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.fillMaxWidth(.6f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(a))
                            Box(Modifier.fillMaxWidth(.8f).height(8.dp).align(Alignment.End).clip(RoundedCornerShape(4.dp)).background(Kx.w(.18f)))
                            Box(Modifier.fillMaxWidth(.5f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(a))
                        }
                        Text(label, style = KxType.style(12.sp, FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
        }
    }
}

@Suppress("unused") private val keepCircle = CircleShape
