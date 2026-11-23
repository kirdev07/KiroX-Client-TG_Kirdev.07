package com.kirox.app.ui.v3.screens.groups

import com.kirox.app.ui.v3.model.kxCount
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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlin.math.roundToInt
import kotlin.math.sin

// Board demo (no chat id). With "ChannelStats:<chatId>" the 4 tiles are the first rows of store.chatStats(chatId).
// demo: needs store (graphs, join sources and top posts are not in the store yet: they stay the board's demo)
private val SPer = listOf("7 дней" to 7, "30 дней" to 30, "90 дней" to 90)
private val SMon = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
private fun sSeries(days: Int): List<Int> {
    val out = ArrayList<Int>(); var v = 18400 - days * 22
    for (i in 0..days) { v += Math.round(18 + sin(i * 0.9) * 26 + (if (i == (days * 0.6).toInt()) 140 else 0)).toInt(); out.add(v) }
    return out
}
private fun sDate(days: Int, i: Int): String {
    val c = java.util.Calendar.getInstance().apply { set(2026, 10, 22); add(java.util.Calendar.DAY_OF_MONTH, -(days - i)) }
    return "${c.get(java.util.Calendar.DAY_OF_MONTH)} ${SMon[c.get(java.util.Calendar.MONTH)]}"
}
// critique-C P2: one Russian number format — «12,3 тыс.» compact, «3 100» grouped exact.
internal fun gxK(n: Int): String = kxCount(n)
internal fun gxRu(n: Int): String = (if (n < 0) "−" else "") + gxNum(kotlin.math.abs(n)).replace(' ', ' ')

/** Board «Статистика канала»: period filter, 4 tiles, tappable subscribers line chart, sources bars, top posts. */
@Composable
fun ChannelStatsScreen(ctx: KxScreenCtx) {
    var per by rememberSaveable { mutableIntStateOf(1) }
    var hover by remember { mutableIntStateOf(-1) }
    val days = SPer[per].second
    val data = remember(days) { sSeries(days) }
    val gained = data.last() - data.first()
    val views = days * 1840
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val stats = gxLoad(chatId) { chatId?.let { store.chatStats(it) } }

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            Column(Modifier.weight(1f)) {
                Text("Статистика", style = KxType.style(20.sp, FontWeight.ExtraBold))
                Text(if (chatId == null) "Android Weekly" else store.header(chatId).title + (stats?.period?.let { " · $it" } ?: ""), style = KxType.style(12.sp, color = Kx.text3))
            }
        }
        GxSegmented(SPer.map { it.first }, per, Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth()) { per = it; hover = -1 }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp)) {
            val tiles = if (chatId != null) stats?.rows.orEmpty().take(4).map { (t, cur, prev) -> Triple(t, gxStatValue(cur), gxDelta(cur, prev)) } else listOf(
                Triple("Подписчики", gxRu(data.last()), "+" + gxRu(gained)), Triple("Просмотры", gxK(views), "+12%"),
                Triple("Охват поста", gxK((data.last() * 0.64).roundToInt()), "+4%"), Triple("Отписались", gxRu((days * 6.2).roundToInt()), "−3%"),
            )
            tiles.chunked(2).forEachIndexed { r, row ->
                Row(Modifier.padding(top = if (r == 0) 0.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { c, (label, value, delta) ->
                        val k = r * 2 + c; val down = if (chatId != null) delta.startsWith("−") else k == 3
                        key(per) {
                            Column(Modifier.weight(1f).gxRise(k * 50, 300, 8.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Text(label, style = KxType.style(12.sp, color = Kx.text3))
                                Text(value, style = KxType.style(22.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"), modifier = Modifier.padding(top = 4.dp))
                                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(if (down) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp, null, Modifier.size(15.dp), tint = if (down) Kx.text3 else Kx.accent)
                                    Text(delta, style = KxType.style(12.sp, FontWeight.Bold, if (down) Kx.text3 else Kx.accent))
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.padding(top = 12.dp).gxCard().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Подписчики", style = KxType.style(15.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f).alignByBaseline())
                    Text("${sDate(days, 0)} — ${sDate(days, days)}", style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.alignByBaseline())
                }
                key(per) { LineChart(data, days, hover) { hover = if (hover == it) -1 else it } }
                Text("Нажмите на график, чтобы увидеть день", style = KxType.style(11.sp, color = Kx.text4))
            }
            Column(Modifier.padding(top = 12.dp).gxCard().padding(14.dp)) {
                Text("Откуда приходят", style = KxType.style(15.sp, FontWeight.ExtraBold))
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Поиск" to .38, "Ссылки в других каналах" to .27, "Пересылки постов" to .18, "Приглашения" to .11, "Другое" to .06).forEach { (label, f) ->
                        key(per, label) { GxBarRow(label, gxRu((gained * f).roundToInt()), "${(f * 100).roundToInt()}%", (f / .38).toFloat()) }
                    }
                }
            }
            Column(Modifier.padding(top = 12.dp).gxCard().padding(14.dp)) {
                Text("Лучшие посты", style = KxType.style(15.sp, FontWeight.ExtraBold))
                listOf("Вышел Kotlin 2.3 — разбираем" to 24800, "Compose 1.9: общие переходы" to 21300, "10 плагинов для Android Studio" to 17900).forEachIndexed { i, (t, v) ->
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(Color(0x1F8FD3AC)), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.accent))
                        }
                        KxLine(t, KxType.style(14.sp), Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Outlined.Visibility, null, Modifier.size(14.dp), tint = Kx.text3)
                            Text(gxK(v), style = KxType.style(12.sp, color = Kx.text3))
                        }
                    }
                }
            }
        }
    }
}

/** Card look of the stats boards: radius 22, white .04, 1px .08 border. */
fun Modifier.gxCard(radius: Int = 22): Modifier = fillMaxWidth().clip(RoundedCornerShape(radius.dp)).background(Color.White.copy(alpha = .04f))
    .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(radius.dp))

/** Period chip of the stats boards (34dp pill, accent tint when on). */
/** critique-C P2: THE range picker of the stats boards — one segmented track, the picked segment tinted. */
@Composable
fun GxSegmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    Row(modifier.clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { i, t ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Color(0x2E8FD3AC) else Color.Transparent, tween(200), label = "seg")
            Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(13.dp)).background(bg).kxSelectedState(on).kxClick(t, androidx.compose.ui.semantics.Role.Tab) { onPick(i) }.padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center) {
                Text(t, style = KxType.style(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text3), maxLines = 1)
            }
        }
    }
}

@Composable
fun GxSegChip(label: String, on: Boolean, modifier: Modifier = Modifier, height: Int = 34, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Color(0x248FD3AC) else Kx.w(.05f), tween(200), label = "pb")
    val ink by animateColorAsState(if (on) Kx.accent else Kx.text2, tween(200), label = "pi")
    Box(modifier.height(height.dp).clip(RoundedCornerShape((height / 2).dp)).background(bg).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, style = KxType.style(13.sp, FontWeight.Bold, ink), maxLines = 1)
    }
}

/** Label · value · pct row with an 8dp bar growing from the left (kxBar .6s). */
@Composable
fun GxBarRow(label: String, value: String, pct: String, frac: Float, color: Color = Kx.accent) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = KxType.style(13.sp, color = Kx.text2), modifier = Modifier.weight(1f).alignByBaseline())
            Text(value, style = KxType.style(13.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"), modifier = Modifier.alignByBaseline())
            Text(pct, style = KxType.style(12.sp, color = Kx.text4), textAlign = TextAlign.End, modifier = Modifier.width(44.dp).alignByBaseline())
        }
        Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Kx.w(.06f))) {
            val p = remember { Animatable(0f) }
            val reduced = LocalKxReducedMotion.current
            LaunchedEffect(Unit) { if (reduced) p.snapTo(1f) else p.animateTo(1f, tween(600, easing = KxMotion.rise)) }
            Box(Modifier.fillMaxWidth((frac * p.value).coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(color))
        }
    }
}

@Composable
private fun LineChart(data: List<Int>, days: Int, hover: Int, onHit: (Int) -> Unit) {
    val min = data.min(); val max = data.max()
    val pad = ((max - min) * 0.1).takeIf { it != 0.0 } ?: 1.0
    val lo = min - pad; val hi = max + pad
    val n = 12
    fun idxOf(k: Int) = Math.round((k + 0.5) / n * days).toInt()
    val draw = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(Unit) { if (reduced) draw.snapTo(1f) else draw.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    val tm = rememberTextMeasurer()
    val label = TextStyle(fontSize = 10.sp, color = Kx.text4)
    BoxWithConstraints(Modifier.padding(top = 10.dp).fillMaxWidth().height(170.dp)) {
        val wDp = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            val sy = size.height / 170f
            // critique-C P2: y labels live in a left gutter, outside the plot (they sat on the line).
            val gutter = 44.dp.toPx()
            fun X(i: Int) = gutter + i.toFloat() / days * (size.width - gutter)
            fun Y(v: Int) = ((140 - ((v - lo) / (hi - lo)) * 120) * sy).toFloat()
            listOf(20f to .06f, 80f to .06f, 140f to .12f).forEach { (y, a) -> drawLine(Color.White.copy(alpha = a), Offset(gutter, y * sy), Offset(size.width, y * sy), 1.dp.toPx()) }
            val line = Path().apply { data.forEachIndexed { i, v -> if (i == 0) moveTo(X(i), Y(v)) else lineTo(X(i), Y(v)) } }
            val area = Path().apply { addPath(line); lineTo(size.width, 140 * sy); lineTo(gutter, 140 * sy); close() }
            drawPath(area, Brush.verticalGradient(listOf(Kx.accent.copy(alpha = .28f), Kx.accent.copy(alpha = 0f)), 0f, 140 * sy))
            val pm = PathMeasure().apply { setPath(line, false) }
            val seg = Path(); pm.getSegment(0f, pm.length * draw.value, seg, true)
            drawPath(seg, Kx.accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (hover >= 0) {
                val i = idxOf(hover); val hx = X(i); val hy = Y(data[i])
                drawLine(Kx.text.copy(alpha = .35f), Offset(hx, 12 * sy), Offset(hx, 140 * sy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
                drawCircle(Kx.bg, 8.dp.toPx(), Offset(hx, hy)); drawCircle(Kx.accent, 6.dp.toPx(), Offset(hx, hy))
            }
            fun txt(s: String, x: Float, baseline: Float, end: Boolean = false) {
                val r = tm.measure(s, label)
                drawText(r, topLeft = Offset(if (end) x - r.size.width else x, baseline * sy - r.firstBaseline))
            }
            txt(gxK(Math.round(hi).toInt()), gutter - 6.dp.toPx(), 24f, end = true); txt(gxK(Math.round(lo).toInt()), gutter - 6.dp.toPx(), 144f, end = true)
            txt(sDate(days, 0), gutter, 162f); txt(sDate(days, days), size.width, 162f, end = true)
        }
        Row(Modifier.fillMaxWidth().padding(start = 44.dp, bottom = 24.dp).fillMaxHeight()) {
            repeat(n) { k ->
                val i = idxOf(k)
                Box(Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = "${sDate(days, i)}: ${gxRu(data[i])}" }.gxPlainClick { onHit(k) })
            }
        }
        if (hover >= 0) {
            val i = idxOf(hover); val prev = data[maxOf(0, i - 1)]; val d = data[i] - prev
            val px = i.toFloat() / days * wDp.value
            Column(Modifier.offset(x = (px - 60f).coerceIn(0f, 220f * wDp.value / 330f).dp, y = (-4).dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xF0222A26)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f)))).border(1.dp, Kx.w(.16f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(sDate(days, i), style = KxType.style(12.sp, color = Kx.text3))
                Text("${gxRu(data[i])} подписчиков", style = KxType.style(12.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"))
                Text((if (d >= 0) "+" else "−") + kotlin.math.abs(d) + " за день", style = KxType.style(12.sp, FontWeight.Bold, if (d >= 0) Kx.accent else Kx.text3))
            }
        }
    }
}
