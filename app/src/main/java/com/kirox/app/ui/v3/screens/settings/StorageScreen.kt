package com.kirox.app.ui.v3.screens.settings

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

/** cubic-bezier(.34,1.4,.5,1): segmented lens. */
internal val LensEase = CubicBezierEasing(.34f, 1.4f, .5f, 1f)

/**
 * Segmented control of the settings boards: 40dp track w.05 r20 padding 3, lens r17 accent .18 with
 * inset .14, moves with cubic-bezier(.34,1.4,.5,1) .45s; labels 13/700, selected #EEF3EF else #8A958F.
 */
@Composable
internal fun SSegment(labels: List<String>, selected: Int, icons: List<ImageVector>? = null, height: Int = 40, onPick: (Int) -> Unit) {
    var w by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val reduced = LocalKxReducedMotion.current
    val n = labels.size
    val x by animateFloatAsState(selected.toFloat(), if (reduced) tween(0) else tween(450, easing = LensEase), label = "lens")
    val r = (height / 2).dp
    Box(Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(r)).background(Kx.w(.05f)).padding(3.dp).onSizeChanged { w = it.width }) {
        val segW = with(density) { (w / n).toDp() }
        Box(Modifier.graphicsLayer { translationX = x * w / n }.width(segW).fillMaxHeight().clip(RoundedCornerShape(r - 3.dp))
            .background(Kx.accent.copy(alpha = .18f)).topHighlight(Kx.w(.14f)))
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, l ->
                val c by animateColorAsState(if (i == selected) Kx.text else Kx.text4, tween(300), label = "seg")
                Row(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(r - 3.dp)).kxClick(role = Role.Tab) { onPick(i) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (icons != null) { Icon(icons[i], null, Modifier.size(17.dp), tint = c); Spacer(Modifier.width(5.dp)) }
                    Text(l, style = KxType.style(13.sp, FontWeight.Bold, c), maxLines = 1)
                }
            }
        }
    }
}

/** Option pills row (flex 1, 32dp, r12, accent when selected). */
@Composable
internal fun SPills(labels: List<String>, selected: Int, height: Int = 32, radius: Int = 12, font: Int = 12, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, l ->
            val bg by animateColorAsState(if (i == selected) Kx.accent else Kx.w(.07f), tween(250), label = "pill")
            Box(Modifier.weight(1f).height(height.dp).clip(RoundedCornerShape(radius.dp)).background(bg).kxClick { onPick(i) }, contentAlignment = Alignment.Center) {
                Text(l, style = KxType.style(font.sp, FontWeight.Bold, if (i == selected) Kx.onAccent else Kx.text2), maxLines = 1)
            }
        }
    }
}

/** Board «Данные и память»: cache donut + legend, clear button, auto-download per network, keep-media pills. */
@Composable
fun StorageScreen(ctx: KxScreenCtx) {
    // demo: needs store (storage statistics by type, clear cache action, auto-download presets, keep-media period)
    val parts = listOf(Triple("Фото", 410, Color(0xFF8FD3AC)), Triple("Видео", 320, Color(0xFF7FC4F0)), Triple("Документы", 170, Color(0xFFFFAE80)),
        Triple("Музыка", 130, Color(0xFFC9A6FF)), Triple("Голосовые", 96, Color(0xFFFFD27A)))
    var net by rememberSaveable { mutableIntStateOf(0) }
    var state by rememberSaveable { mutableStateOf("idle") }
    var keep by rememberSaveable { mutableIntStateOf(2) }
    var auto by remember { mutableStateOf(listOf(
        mapOf("photo" to true, "video" to true, "file" to false, "music" to true),
        mapOf("photo" to true, "video" to false, "file" to false, "music" to false),
        mapOf("photo" to false, "video" to false, "file" to false, "music" to false))) }
    val done = state == "done"
    LaunchedEffect(state) { if (state == "busy") { delay(1400); state = "done" } }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glowGreen(.18f), alignEnd = true)
        Column(Modifier.fillMaxSize()) {
            SHeader("Данные и память", ctx.back, bottom = 2.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val shape = RoundedCornerShape(26.dp)
                Row(Modifier.sRise(0).fillMaxWidth().clip(shape).background(TileBg).border(1.dp, Kx.w(.06f), shape).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                        val sum = parts.sumOf { it.second }.toFloat()
                        val cols = parts.map { p -> animateColorAsState(if (done) Kx.badgeMuted else p.third, tween(800, easing = CssEase), label = "d").value }
                        Canvas(Modifier.matchParentSize()) {
                            var acc = 0f
                            val sw = 16.dp.toPx()
                            parts.forEachIndexed { i, p ->
                                val sweep = p.second / sum * 360f
                                drawArc(cols[i], -90f + acc, sweep + .5f, false, topLeft = Offset(sw / 2, sw / 2),
                                    size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw), style = Stroke(sw))
                                acc += sweep
                            }
                        }
                        Column(Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF121614)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(if (done) "0 МБ" else "1,1 ГБ", style = KxType.style(20.sp, FontWeight.ExtraBold))
                            Text("кэш kiroX", style = KxType.style(11.sp, color = Kx.text4))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        parts.forEach { (label, mb, c) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(c))
                                Text(label, style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                                Text(if (done) "0 МБ" else "$mb МБ", style = KxType.style(12.sp, color = Kx.text4))
                            }
                        }
                    }
                }
                ClearButton(state) { when (state) { "busy" -> Unit; "done" -> state = "idle"; else -> state = "busy" } }

                Column(Modifier.sRise(80)) {
                    GroupLabel("Автозагрузка медиа")
                    SSegment(listOf("Wi-Fi", "Мобильная", "Роуминг"), net, listOf(Icons.Outlined.Wifi, Icons.Outlined.SignalCellularAlt, Icons.Outlined.Luggage)) { net = it }
                    val kDefs = listOf(listOf("photo", "Фото", "До 5 МБ"), listOf("video", "Видео", "До 50 МБ"), listOf("file", "Файлы", "До 10 МБ"), listOf("music", "Музыка", "До 20 МБ"))
                    val kIcons = listOf(Icons.Outlined.Image, Icons.Outlined.Movie, Icons.Outlined.Description, Icons.Outlined.MusicNote)
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        kDefs.forEachIndexed { i, (k, label, limit) ->
                            val on = auto[net][k] == true
                            Row(Modifier.fillMaxWidth().height(50.dp).clip(groupShape(i, 4, 20.dp)).background(TileBg)
                                .kxClick(role = Role.Switch) { auto = auto.toMutableList().also { it[net] = it[net] + (k to !on) } }.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(kIcons[i], null, Modifier.size(20.dp), tint = Kx.text4)
                                Column(Modifier.weight(1f)) {
                                    Text(label, style = KxType.style(14.sp))
                                    Text(if (on) limit else "Только вручную", style = KxType.style(11.sp, color = Kx.text4))
                                }
                                SSwitch(on, w = 44.dp, h = 26.dp, knob = 20.dp, highlight = false)
                            }
                        }
                    }
                }
                val keepOpts = listOf("3 дня", "Неделя", "Месяц", "Всегда")
                Column(Modifier.sRise(120).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TileBg).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Хранить медиа", style = KxType.style(14.sp), modifier = Modifier.weight(1f))
                        Text(keepOpts[keep], style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
                    }
                    Spacer(Modifier.height(10.dp))
                    SPills(keepOpts, keep) { keep = it }
                }
            }
        }
    }
}

@Composable
private fun ClearButton(state: String, onClick: () -> Unit) {
    val done = state == "done"
    val bg by animateColorAsState(if (done) Kx.accent.copy(alpha = .16f) else Kx.accent, tween(300), label = "cb")
    val ink = if (done) Color(0xFFC8F0D8) else Kx.onAccent
    Row(Modifier.sRise(40).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp)).background(bg).kxClick(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (state == "busy") {
            val t = rememberInfiniteTransition(label = "spin")
            val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "a")
            Canvas(Modifier.size(18.dp).graphicsLayer { rotationZ = a }) {
                val sw = 3.dp.toPx()
                drawCircle(Color(6, 21, 13).copy(alpha = .3f), size.minDimension / 2 - sw / 2, style = Stroke(sw))
                drawArc(Kx.onAccent, -135f, 90f, false, topLeft = Offset(sw / 2, sw / 2), size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw), style = Stroke(sw))
            }
            Spacer(Modifier.width(8.dp))
        }
        if (done) {
            val pop = remember { Animatable(0f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, tween(400, easing = CssEase)) }
            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(24.dp).graphicsLayer {
                val p = pop.value; alpha = (p / .7f).coerceAtMost(1f)
                val s = if (p < .7f) .6f + (1.08f - .6f) * (p / .7f) else 1.08f - .08f * ((p - .7f) / .3f); scaleX = s; scaleY = s
            }, tint = ink)
            Spacer(Modifier.width(8.dp))
        }
        Text(when (state) { "busy" -> "Очищаем…"; "done" -> "Освобождено 1,1 ГБ"; else -> "Очистить кэш" }, style = KxType.style(15.sp, FontWeight.ExtraBold, ink))
    }
}
