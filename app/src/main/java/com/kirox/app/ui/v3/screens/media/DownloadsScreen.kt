package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class Dl(val name: String, val size: Double, val icon: ImageVector, val c: Color, val from: String, val got: Double, val done: Boolean = false,
                      val paused: Boolean = false, val gone: Boolean = false)

// demo: needs store (TDLib file downloads: progress, pause/resume, cancel)
private val DlSeed = listOf(
    Dl("Фильм про горы.mp4", 1460.0, Icons.Rounded.Movie, Color(0xFF2F6E9E), "Анна", 410.0),
    Dl("Фото с дачи.zip", 42.0, Icons.Rounded.FolderZip, Color(0xFFB07A2E), "Мама", 30.0),
    Dl("Лекция 7 — Compose.pdf", 18.0, Icons.Rounded.PictureAsPdf, Color(0xFFC0443C), "Kotlin-клуб", 3.0),
    Dl("Подкаст выпуск 42.mp3", 64.0, Icons.Rounded.GraphicEq, Color(0xFF6A4C9C), "Подкасты", 64.0, done = true),
    Dl("kiroX-beta.apk", 31.0, Icons.Rounded.Android, Color(0xFF3F9A6C), "Избранное", 31.0, done = true),
)

private fun fmtMb(mb: Double): String = if (mb >= 1000) String.format(java.util.Locale.US, "%.2f", mb / 1000).replace('.', '.') + " ГБ"
    else (if (mb < 10) String.format(java.util.Locale.US, "%.1f", mb) else Math.round(mb).toString()) + " МБ"

/** Board «Загрузки»: summary bar, active/done tabs, striped progress, pause/resume/cancel. */
@Composable
fun DownloadsScreen(ctx: KxScreenCtx) {
    var tab by remember { mutableIntStateOf(0) }
    val list = remember { mutableStateListOf(*DlSeed.toTypedArray()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            list.indices.forEach { i ->
                val f = list[i]
                if (f.gone || f.done || f.paused) return@forEach
                val rate = listOf(22.0, 2.6, 1.1).getOrElse(i) { 2.0 }
                val got = minOf(f.size, f.got + rate)
                list[i] = f.copy(got = got, done = got >= f.size)
            }
        }
    }
    val live = list.filter { !it.gone }
    val active = live.filter { !it.done }
    val running = active.filter { !it.paused }
    val tot = active.sumOf { it.size }; val got = active.sumOf { it.got }
    val speedMb = if (running.isNotEmpty()) 4.2 else 0.0
    val shown = live.map { it to list.indexOf(it) }.filter { (f, _) -> if (tab == 0) !f.done else f.done }
    val allPaused = running.isEmpty() && active.isNotEmpty()

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MRoundBtn(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") { ctx.back() }
            T("Загрузки", 22.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Row(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.06f))
                .kxClick { list.indices.forEach { i -> if (!list[i].done) list[i] = list[i].copy(paused = !allPaused) } }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (allPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, null, Modifier.size(18.dp), tint = Kx.text)
                T(if (allPaused) "Продолжить все" else "Пауза для всех", 13.sp, FontWeight.Bold)
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).cssGrad(150f, Kx.accentDeep.copy(alpha = .18f), Kx.w(.03f))
            .border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                T(if (active.isNotEmpty()) "Скачивается ${active.size} из ${live.size}" else "Всё скачано", 15.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f).alignByBaseline())
                T(if (running.isNotEmpty()) String.format(java.util.Locale.US, "%.1f", speedMb) + " МБ/с" else if (active.isNotEmpty()) "на паузе" else "", 13.sp, FontWeight.Bold, Kx.accent, modifier = Modifier.alignByBaseline())
            }
            val pct by animateFloatAsState(if (tot > 0) (got / tot).toFloat() else 1f, tween(500, easing = LinearEasing), label = "tot")
            Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Kx.w(.08f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(pct).clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep))))
            }
            T(if (active.isNotEmpty()) "${fmtMb(got)} из ${fmtMb(tot)}" + (if (running.isNotEmpty()) " · осталось ~${maxOf(1, Math.round((tot - got) / maxOf(speedMb, 1.0) / 60).toInt())} мин" else "")
              else "Файлы лежат в папке kiroX", 12.sp, color = Kx.text3, modifier = Modifier.padding(top = 6.dp))
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Активные" to 0, "Готово" to 1).forEach { (label, i) ->
                val on = tab == i
                val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "tb")
                Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).kxClick { tab = i }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    T("$label · ${if (i == 0) active.size else live.size - active.size}", 13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2)
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 30.dp)) {
            itemsIndexed(shown, key = { _, (f, i) -> "$tab/$i/${f.done}" }) { k, (f, i) -> DlRow(f, Modifier.kxRise(k * 40, 250),
                onToggle = { list[i] = list[i].copy(paused = !list[i].paused) }, onCancel = { list[i] = list[i].copy(gone = true) }) }
            if (shown.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 60.dp).kxRise(0, 300), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.DownloadDone, null, Modifier.size(54.dp), tint = Kx.dashed)
                    T(if (tab == 0) "Нет активных загрузок" else "Пока ничего не скачано", 16.sp, FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun DlRow(f: Dl, modifier: Modifier, onToggle: () -> Unit, onCancel: () -> Unit) {
    Row(modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(50.dp)) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).background(f.c), contentAlignment = Alignment.Center) { Icon(f.icon, null, tint = Color.White) }
            if (f.done) {
                val p = remember { Animatable(0f) }
                LaunchedEffect(Unit) { p.animateTo(1f, tween(400, easing = Ease)) }
                val sc = if (p.value < .6f) .5f + .75f * p.value / .6f else 1.25f - .25f * (p.value - .6f) / .4f
                Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(20.dp).graphicsLayer { scaleX = sc; scaleY = sc }.clip(CircleShape).background(Kx.bg).padding(2.dp)
                    .clip(CircleShape).background(Kx.accent), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, Modifier.size(13.dp), tint = Kx.onAccent) }
            }
        }
        Column(Modifier.weight(1f)) {
            T(f.name, 14.sp, FontWeight.Bold, maxLines = 1)
            if (!f.done) {
                val pct by animateFloatAsState((f.got / f.size).toFloat(), tween(500, easing = LinearEasing), label = "p")
                // kxStripe .8s linear ×2: 45° stripes sliding 28px.
                val clock by rememberPlayClock(!f.paused, 1600)
                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Kx.w(.08f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(pct).clip(RoundedCornerShape(3.dp)).drawBehind {
                        if (f.paused) drawRect(Color(0xFF5C6661)) else {
                            val shift = 28.dp.toPx() * ((clock % 800) / 800f)
                            val p = 14.dp.toPx() * 0.7071f * 2
                            drawRect(Brush.linearGradient(0f to Kx.accent, .5f to Kx.accent, .5f to Kx.accentMid, 1f to Kx.accentMid,
                                start = Offset(shift, 0f), end = Offset(shift + p / 2, p / 2), tileMode = TileMode.Repeated))
                        }
                    })
                }
            }
            T(if (f.done) "${fmtMb(f.size)} · готово" else "${fmtMb(f.got)} из ${fmtMb(f.size)}" + (if (f.paused) " · пауза" else " · ${Math.round(f.got / f.size * 100)}%"), 12.sp,
                color = if (f.done) Kx.accent else if (f.paused) Kx.warning else Kx.text4, modifier = Modifier.padding(top = 4.dp))
        }
        if (!f.done) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Kx.w(.07f)).kxClick(if (f.paused) "Продолжить" else "Пауза", onClick = onToggle), contentAlignment = Alignment.Center) {
                Icon(if (f.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, Modifier.size(20.dp), tint = if (f.paused) Kx.accent else Kx.text)
            }
            Box(Modifier.size(38.dp).clip(CircleShape).kxClick("Отменить", onClick = onCancel), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(20.dp), tint = Kx.text4) }
        } else T(f.from, 12.sp, color = Kx.text4)
    }
}
