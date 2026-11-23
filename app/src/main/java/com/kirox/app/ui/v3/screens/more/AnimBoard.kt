package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxPrefs
import com.kirox.app.ui.v3.model.pref
import com.kirox.app.ui.v3.theme.*
import java.util.Calendar

private class AnimScene(val icon: ImageVector, val title: String, val sub: String, val tile: Color, val ink: Color)

private val AnimScenes = listOf(
    AnimScene(Icons.Rounded.AutoMode, "Авто (по дате)", "Сама меняется к праздникам", Color(0xFF26302B), Color(0xFFC9D2CC)),
    AnimScene(Icons.Rounded.AutoAwesome, "Основная", "Мерцающие частицы kiroX", Color(0xFF1F3A2C), Color(0xFF8FD3AC)),
    AnimScene(Icons.Rounded.DarkMode, "Хэллоуин", "Тыквы, летучие мыши, паутина", Color(0xFF3B2A1A), Color(0xFFFFAE5E)),
    AnimScene(Icons.Rounded.AcUnit, "Зима", "Снежинки, сугробы и иней", Color(0xFF1F2E40), Color(0xFFBFE3FF)),
    AnimScene(Icons.Rounded.Celebration, "Новый год", "Гирлянда, ёлка и салют", Color(0xFF3A1E2A), Color(0xFFFF9CC8)),
)

/** «Авто»: the scene for today's date (Halloween 20 Oct–2 Nov, New Year 20 Dec–10 Jan, winter Dec–Feb, otherwise the main one). */
internal fun animAutoScene(cal: Calendar = Calendar.getInstance()): Int {
    val m = cal.get(Calendar.MONTH); val d = cal.get(Calendar.DAY_OF_MONTH)
    return when {
        (m == Calendar.OCTOBER && d >= 20) || (m == Calendar.NOVEMBER && d <= 2) -> 2
        (m == Calendar.DECEMBER && d >= 20) || (m == Calendar.JANUARY && d <= 10) -> 4
        m == Calendar.DECEMBER || m == Calendar.JANUARY || m == Calendar.FEBRUARY || m == Calendar.NOVEMBER -> 3
        else -> 1
    }
}

/**
 * Board «Анимации» (Anim.dc.html): on/off switch, scene preview, 5 scenes, interface speed and effect quality.
 * Saved in KxPrefs.ANIM_*; «Выкл.» speed or the switch off turn the whole v3 UI to reduced motion (KxApp).
 */
@Composable
fun AnimScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val on = store.pref(KxPrefs.ANIM_ON) != 0
    val scene = store.pref(KxPrefs.ANIM_SCENE).coerceIn(0, 4)
    val speed = store.pref(KxPrefs.ANIM_SPEED).coerceIn(0, 2)
    val qual = store.pref(KxPrefs.ANIM_QUAL).coerceIn(0, 2)
    val real = if (scene == 0) animAutoScene() else scene
    val k = listOf(1f, 2f, 4f)[speed]
    val animOn = on && speed != 2
    val reduced = LocalKxReducedMotion.current
    val track by animateColorAsState(if (on) Color(0xFF8FD3AC) else Color(0xFF2A322E), tween(300), label = "track")
    val knobX by animateDpAsState(if (on) 22.dp else 0.dp, tween(350, easing = CubicBezierEasing(.34f, 1.5f, .5f, 1f)), label = "knob")

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0C0B))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoBack(ctx.back)
                Text("Анимации", style = mts(22, W8), modifier = Modifier.weight(1f))
                Box(Modifier.size(52.dp, 30.dp).clip(RoundedCornerShape(15.dp)).background(track).kxClick("Анимации", Role.Switch) { store.putPref(KxPrefs.ANIM_ON, if (on) 0 else 1) }) {
                    Box(Modifier.padding(3.dp).offset(x = knobX).size(24.dp).clip(CircleShape).background(Color.White))
                }
            }
            // Scene preview (240dp, radius 26)
            val bg: Brush = if (!on) Brush.verticalGradient(listOf(Color(0xFF0E1210), Color(0xFF0E1210))) else when (real) {
                1 -> Brush.radialGradient(listOf(Color(0xFF12301F), Color(0xFF0A0C0B)))
                2 -> Brush.verticalGradient(0f to Color(0xFF2A1A3A), .6f to Color(0xFF1A1020), 1f to Color(0xFF0A0C0B))
                3 -> Brush.verticalGradient(0f to Color(0xFF13243A), .6f to Color(0xFF0D1726), 1f to Color(0xFF0A0C0B))
                else -> Brush.verticalGradient(0f to Color(0xFF1E1830), .6f to Color(0xFF120E1C), 1f to Color(0xFF0A0C0B))
            }
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().height(240.dp).clip(RoundedCornerShape(26.dp)).background(bg)
                .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(26.dp))) {
                if (animOn) AnimParticles(real, k, if (qual == 2) 7 else 14, reduced, Modifier.matchParentSize())
                else Text("Анимации выключены", style = mts(14, color = Color(0xFF8A958F)), modifier = Modifier.align(Alignment.Center))
                Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(buildAnnotatedString { append("kiro"); withStyle(SpanStyle(color = Color(0xFF8FD3AC), fontWeight = FontWeight.ExtraBold)) { append("X") } },
                        style = KxType.style(30.sp, FontWeight.Light))
                    val sub = if (!on) "Без анимаций" else when (real) { 1 -> "Основная"; 2 -> "Хэллоуин"; 3 -> if (scene == 0) "Авто → Зима" else "Зима"; else -> "Новый год" }
                        .let { if (scene == 0 && real != 3) "Авто → $it" else it }
                    Text(sub, style = mts(12, color = Color.White.copy(alpha = .7f)))
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp).alpha(if (on) 1f else .45f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Сцена", style = mts(13, W8, Color(0xFF8FD3AC)), modifier = Modifier.padding(horizontal = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnimScenes.forEachIndexed { i, c ->
                        val sel = i == scene
                        Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(if (sel) Color(0x1A8FD3AC) else Color.White.copy(alpha = .045f))
                            .border(1.dp, if (sel) Color(0x668FD3AC) else Color.Transparent, RoundedCornerShape(18.dp))
                            .kxClick(c.title, Role.RadioButton) { store.putPref(KxPrefs.ANIM_SCENE, i) }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(c.tile), contentAlignment = Alignment.Center) { Icon(c.icon, null, Modifier.size(20.dp), tint = c.ink) }
                            Column(Modifier.weight(1f)) { Text(c.title, style = mts(14, W7)); Text(c.sub, style = mts(11, color = Color(0xFF8A958F))) }
                            Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (sel) Color(0xFF8FD3AC) else Color(0xFF5E6964), CircleShape), contentAlignment = Alignment.Center) {
                                if (sel) Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF8FD3AC)))
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimSegment("Скорость интерфейса", listOf("Обычные", "Меньше", "Выкл."), speed, Modifier.weight(1f)) { store.putPref(KxPrefs.ANIM_SPEED, it) }
                    AnimSegment("Качество эффектов", listOf("Авто", "Высокое", "Лёгкое"), qual, Modifier.weight(1f)) { store.putPref(KxPrefs.ANIM_QUAL, it) }
                }
            }
        }
    }
}

@Composable
private fun AnimSegment(label: String, items: List<String>, cur: Int, modifier: Modifier, onPick: (Int) -> Unit) {
    Column(modifier) {
        Text(label, style = mts(12, W7, Color(0xFF9AA59F)), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEachIndexed { i, t ->
                Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(12.dp)).background(if (i == cur) Color(0xFF8FD3AC) else Color.White.copy(alpha = .07f))
                    .kxClick(t) { onPick(i) }, contentAlignment = Alignment.Center) {
                    Text(t, style = mts(11, W7, if (i == cur) Color(0xFF06150D) else Color(0xFFC9D2CC)), maxLines = 1)
                }
            }
        }
    }
}

/** The scene's particles (twinkle / fall / blinking bulbs); [k] slows them down («Меньше» = ×2). */
@Composable
private fun AnimParticles(scene: Int, k: Float, n: Int, reduced: Boolean, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "anim-scene")
    val clock by t.animateFloat(0f, 1f, infiniteRepeatable(tween((8000 * k).toInt(), easing = LinearEasing)), label = "clock")
    val time = if (reduced) .3f else clock
    fun rnd(i: Int) = ((i * 47) % 100) / 100f
    Canvas(modifier) {
        val w = size.width; val h = size.height
        fun tw(i: Int, period: Float) = (.35f + .65f * (0.5f + 0.5f * kotlin.math.sin((time * 8f / period + i * .25f) * 2 * Math.PI.toFloat())))
        when (scene) {
            1 -> repeat(n) { i ->
                val c = Offset(rnd(i + 3) * w, (((i * 29) % 70 + 6) / 100f) * h)
                drawCircle(Color(0xFF8FD3AC).copy(alpha = .25f * tw(i, 2.2f)), (5 + i % 3).dp.toPx(), c)
                drawCircle(Color(0xFFA6E3C1).copy(alpha = tw(i, 2.2f)), (1.5f + (i % 3) * .5f).dp.toPx(), c)
            }
            2 -> {
                drawCircle(Color(0x99FFC878), 30.dp.toPx(), Offset(w * .72f + 23.dp.toPx(), h * .1f + 23.dp.toPx()))
                drawCircle(Color(0xFFFFE3A8), 23.dp.toPx(), Offset(w * .72f + 23.dp.toPx(), h * .1f + 23.dp.toPx()))
                repeat(n) { i ->
                    if (i % 4 == 0) { val y = h * (.7f - ((time * 2 + i * .13f) % 1f) * .6f); drawCircle(Color(0xFFFF9A3C).copy(alpha = .8f), 5.dp.toPx(), Offset(rnd(i + 7) * w, y)) }
                    else drawCircle(Color(0xFFFFAE5E).copy(alpha = tw(i, 1.8f)), 2.dp.toPx(), Offset(rnd(i + 2) * w, (((i * 31) % 60 + 10) / 100f) * h))
                }
            }
            3 -> repeat(n + 4) { i ->
                val fall = ((time * 8f / (4 + i % 3)) + i * .35f / (4 + i % 3)) % 1f
                drawCircle(Color(0xE6E1F0FF), (2 + (i % 3)).dp.toPx(), Offset(rnd(i + 5) * w + kotlin.math.sin(fall * 6f + i) * 6.dp.toPx(), fall * (h + 20.dp.toPx()) - 10.dp.toPx()))
            }
            else -> {
                val cols = listOf(Color(0xFFFF6B6B), Color(0xFFFFD27A), Color(0xFF8FD3AC), Color(0xFF7FC4F0), Color(0xFFFF9CC8))
                repeat(10) { i ->
                    val c = Offset(w * (.06f + i * .095f) + 4.5.dp.toPx(), h * (.08f + (i % 2) * .05f) + 4.5.dp.toPx())
                    val a = tw(i, 1.2f)
                    drawCircle(cols[i % 5].copy(alpha = .3f * a), 9.dp.toPx(), c); drawCircle(cols[i % 5].copy(alpha = a), 4.5.dp.toPx(), c)
                }
                listOf(.22f to .34f, .70f to .30f, .48f to .22f).forEachIndexed { j, (x, y) ->
                    val p = ((time * 8f / 2.2f) + j * .3f) % 1f
                    drawCircle(cols[j + 1].copy(alpha = 1f - p), (10 + 30 * p).dp.toPx(), Offset(w * x, h * y), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                }
            }
        }
    }
}
