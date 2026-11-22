package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class LsCat(val k: String, val t: String, val s: String, val gb: Float, val c: Color)
private val LS_CATS = listOf(LsCat("video", "Видео", "смотрели больше месяца назад", 1.6f, Kx.premium), LsCat("photo", "Фото", "из каналов и больших групп", .7f, Color(0xFF7FB8E0)),
    LsCat("files", "Файлы", "скачанные документы и архивы", .4f, Kx.warning), LsCat("other", "Стикеры и прочее", "временные файлы", .2f, Kx.accent))
private fun lsFmt(gb: Float) = if (gb >= 1f) String.format(java.util.Locale.US, "%.1f", gb).replace('.', ',') + " ГБ" else "${Math.round(gb * 1024)} МБ"

/** Board «Мало места»: warning bar over the chat list → quick-clean sheet (category bar, checkboxes, progress button) → freed bar. */
@Composable
fun LowSpaceScreen(ctx: KxScreenCtx) {
    // demo: needs store (device free space, storage stats by type — getStorageStatisticsFast/optimizeStorage) — numbers are demo
    var sheet by remember { mutableStateOf(false) }
    var on by remember { mutableStateOf(setOf("video", "photo", "other")) }
    var p by remember { mutableIntStateOf(-1) }
    var freed by remember { mutableFloatStateOf(0f) }
    val total = LS_CATS.sumOf { it.gb.toDouble() }.toFloat()
    val sel = LS_CATS.filter { it.k in on }.sumOf { it.gb.toDouble() }.toFloat()
    val running = p >= 0
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (true) { delay(70); val n = p + 5; if (n >= 100) { p = -1; sheet = false; freed = sel; break } else p = n }
    }
    androidx.activity.compose.BackHandler(sheet && !running) { sheet = false }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Чаты", style = KxType.style(26.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Search, null, Modifier.size(24.dp), tint = Kx.text3)
            }
            key(freed > 0f) {
                val d = remember { Animatable(0f) }
                LaunchedEffect(Unit) { d.animateTo(1f, tween(350, easing = TEase)) }
                val m = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp).graphicsLayer { alpha = d.value; translationY = -(1f - d.value) * 14.dp.toPx() }
                if (freed == 0f) Row(m.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.warning.copy(alpha = .1f)).border(1.dp, Kx.warning.copy(alpha = .35f), RoundedCornerShape(20.dp))
                    .kxClick { sheet = true }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Storage, null, Modifier.size(24.dp), tint = Kx.warning)
                    Column(Modifier.weight(1f)) {
                        Text("На телефоне осталось 1,1 ГБ", style = KxType.style(14.sp, FontWeight.ExtraBold))
                        Text("kiroX может освободить ${lsFmt(total)} — чаты не пострадают", style = KxType.style(12.sp, color = Color(0xFFE8D2A0)))
                    }
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(24.dp), tint = Kx.warning)
                } else Row(m.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.accent.copy(alpha = .08f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(24.dp), tint = Kx.accent)
                    Text("Освобождено ${lsFmt(freed)} · теперь свободно ${lsFmt(1.1f + freed)}", style = KxType.style(14.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).kxClick { freed = 0f }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text("Ещё раз", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).alpha(.55f).padding(horizontal = 12.dp)) {
                listOf(0xFFC0443C, 0xFF2F6E9E, 0xFF3F9A6C, 0xFF6A4C9C, 0xFFB07A2E, 0xFF5C6661).forEachIndexed { i, c ->
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(Color(c)))
                        Column(Modifier.weight(1f)) {
                            Box(Modifier.fillMaxWidth((40 + (i * 13) % 30) / 100f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Kx.w(.16f)))
                            Box(Modifier.padding(top = 8.dp).fillMaxWidth((60 + (i * 17) % 30) / 100f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Kx.w(.08f)))
                        }
                    }
                }
            }
        }
        if (sheet) TSheet({ if (!running) sheet = false }, "Быстрая чистка", Brush.verticalGradient(listOf(Color(40, 48, 44).copy(alpha = .99f), Color(18, 22, 20))), grabberGap = 14.dp, bottomPad = 24.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val br = if (running) tCycles(400, 2, easing = TEaseInOut, key = running) else 0f
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CleaningServices, null, Modifier.size(26.dp).graphicsLayer { rotationZ = if (br > 0f) kf(br, fa(0f, .5f, 1f), fa(-12f, 12f, -12f)) else 0f }, tint = Kx.accent)
                }
                Column(Modifier.weight(1f)) {
                    Text("Быстрая чистка", style = KxType.style(18.sp, FontWeight.ExtraBold))
                    Text("Удаляем только копии на телефоне — в облаке всё останется", style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Kx.w(.06f))) {
                LS_CATS.forEach { c ->
                    val w by animateFloatAsState(if (c.k in on) c.gb / total else 0f, tween(500, easing = TEase), label = "bw")
                    if (w > 0.001f) Box(Modifier.fillMaxHeight().weight(w).background(c.c).drawBehind { drawLine(Color(0xFF161B18), Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 2f * density) })
                }
                val rest = 1f - LS_CATS.filter { it.k in on }.sumOf { (it.gb / total).toDouble() }.toFloat()
                if (rest > 0.001f) Spacer(Modifier.weight(rest))
            }
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LS_CATS.forEach { c ->
                    val sel1 = c.k in on
                    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).kxClick(role = Role.Checkbox) { if (!running) on = if (sel1) on - c.k else on + c.k }.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(c.c))
                        Column(Modifier.weight(1f)) { Text(c.t, style = KxType.style(15.sp, FontWeight.Bold)); Text(c.s, style = KxType.style(12.sp, color = Kx.text3)) }
                        Text(lsFmt(c.gb), style = KxType.style(14.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"))
                        val cb by animateColorAsState(if (sel1) Kx.accent else Kx.w(.3f), tween(200), label = "cb")
                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (sel1) Kx.accent else Color.Transparent).border(2.dp, cb, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                            if (sel1) Icon(Icons.Rounded.Check, null, Modifier.size(17.dp), tint = Kx.onAccent)
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(17.dp), tint = Kx.text3)
                Text("Секретные чаты не трогаем — их нет в облаке.", style = KxType.style(12.sp, color = Kx.text3))
            }
            Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
                .background(if (sel > 0f) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.w(.07f))).kxClick { if (sel > 0f && !running) p = 0 }, contentAlignment = Alignment.Center) {
                if (running) Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(p / 100f).background(Kx.onAccent.copy(alpha = .18f)))
                Text(if (running) "Чистим… $p%" else if (sel > 0f) "Освободить ${lsFmt(sel)}" else "Выберите, что удалить", style = KxType.style(16.sp, FontWeight.ExtraBold, if (sel > 0f) Kx.onAccent else Kx.text4))
            }
        }
    }
}
