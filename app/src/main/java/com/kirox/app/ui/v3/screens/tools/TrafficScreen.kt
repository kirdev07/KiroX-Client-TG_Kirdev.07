package com.kirox.app.ui.v3.screens.tools

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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TR_NETS = listOf(Triple("Мобильная", Icons.Outlined.SignalCellularAlt, Icons.Rounded.SignalCellularAlt),
    Triple("Wi-Fi", Icons.Outlined.Wifi, Icons.Rounded.Wifi), Triple("Роуминг", Icons.Outlined.Public, Icons.Rounded.Public))
private val TR_CATS = listOf<Triple<String, ImageVector, Color>>(
    Triple("Видео", Icons.Rounded.Movie, Color(0xFF2F6E9E)), Triple("Фото", Icons.Rounded.Image, Color(0xFF3F9A6C)),
    Triple("Голосовые и музыка", Icons.Rounded.GraphicEq, Color(0xFF6A4C9C)), Triple("Звонки", Icons.Rounded.Call, Color(0xFFB07A2E)),
    Triple("Файлы", Icons.Rounded.Description, Color(0xFFC0443C)), Triple("Сообщения и прочее", Icons.Rounded.Chat, Color(0xFF5C6661)))
// demo: needs store — [in, out] MB per category, per network, for 30 days
private val TR_DATA = listOf(
    listOf(820 to 64, 410 to 120, 96 to 38, 210 to 190, 140 to 22, 48 to 31),
    listOf(4200 to 180, 1650 to 420, 380 to 90, 640 to 590, 2100 to 340, 160 to 95),
    listOf(12 to 1, 8 to 3, 2 to 1, 0 to 0, 0 to 0, 3 to 2))
private val TR_PER = listOf("7 дней" to .26f, "30 дней" to 1f, "Всё время" to 3.4f)

private fun trFmt(mb: Float): String = if (mb >= 1000) String.format(java.util.Locale.US, if (mb >= 10000) "%.1f" else "%.2f", mb / 1000).replace('.', ',') + " ГБ" else "${Math.round(mb)} МБ"

/** Board «Использование сети»: network tabs, animated donut by category, live speed sparkline, period chips, category list, reset with confirm. */
@Composable
fun TrafficScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → GetNetworkStatistics (since the last reset, all networks) + ResetNetworkStatistics
    val real = ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<com.kirox.app.ui.v3.model.KxTraffic?>(null) }
    var statsKey by remember { mutableIntStateOf(0) }
    if (real) LaunchedEffect(statsKey) { while (true) { stats = ctx.store.networkStats(); delay(5000) } }
    var net by rememberSaveable { mutableIntStateOf(0) }
    var per by rememberSaveable { mutableIntStateOf(1) }
    var sel by remember { mutableIntStateOf(-1) }
    var confirm by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(listOf(false, false, false)) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(700); tick++ } }
    val toast = remember { TToast() }
    val k = if (reset[net]) 0f else TR_PER[per].second
    val rows = if (!real) TR_DATA[net].map { (i, o) -> i * k to o * k } else List(6) { n ->
        val keys = when (n) { 0 -> setOf("video"); 1 -> setOf("photo"); 2 -> setOf("voice", "music"); 3 -> setOf("calls"); 4 -> setOf("files", "stickers"); else -> setOf("messages", "other") }
        val r = stats?.rows.orEmpty().filter { it.network == net && it.category in keys }
        r.sumOf { it.received } / 1_000_000f to r.sumOf { it.sent } / 1_000_000f
    }
    val tot = rows.sumOf { (i, o) -> (i + o).toDouble() }.toFloat()
    val tin = rows.sumOf { it.first.toDouble() }.toFloat(); val tout = tot - tin
    val selRow = if (sel >= 0) rows[sel] else null
    val ease = CubicBezierEasing(.2f, .8f, .2f, 1f)

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.kirox.app.ui.v3.components.KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Text("Использование сети", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            }
            // Network tabs
            val tShape = RoundedCornerShape(22.dp)
            var tabsW by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val pill by animateFloatAsState(net.toFloat(), tween(300, easing = ease), label = "pill")
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().clip(tShape).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), tShape)
                .onSizeChanged { tabsW = it.width }.padding(4.dp)) {
                val cell = with(density) { ((tabsW - 8.dp.toPx()) / 3f).toDp() }
                Box(Modifier.offset(x = cell * pill).width(cell).height(40.dp).clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Kx.accent.copy(alpha = .24f), Kx.accent.copy(alpha = .12f)))).border(1.dp, Kx.accent.copy(alpha = .4f), RoundedCornerShape(18.dp)))
                Row {
                    TR_NETS.forEachIndexed { i, (label, ic, icOn) ->
                        val on = net == i
                        val ink by animateColorAsState(if (on) Kx.text else Kx.text3, tween(200), label = "ink")
                        Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(18.dp)).kxClick(role = Role.Tab) { net = i; sel = -1 },
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (on) icOn else ic, null, Modifier.size(18.dp), tint = ink); Spacer(Modifier.width(6.dp))
                            Text(label, style = KxType.style(13.sp, FontWeight.Bold, ink))
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 40.dp)) {
                // Donut
                val fracs = rows.map { (i, o) -> if (tot > 0) (i + o) / tot else 0f }
                val anim = fracs.mapIndexed { n, f -> animateFloatAsState(f, tween(700, easing = ease), label = "a$n").value }
                val widths = List(6) { n -> animateFloatAsState(if (sel == n) 26f else if (sel >= 0) 14f else 20f, tween(200), label = "w$n").value }
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(210.dp)) {
                        val r = 86.dp.toPx(); val tl = Offset(center.x - r, center.y - r); val sz = Size(r * 2, r * 2)
                        drawCircle(Kx.w(.06f), r, style = Stroke(20.dp.toPx()))
                        val circ = (2 * Math.PI * 86).toFloat(); val gapDeg = if (tot > 0) 4f / circ * 360f else 0f
                        var acc = 0f
                        anim.forEachIndexed { n, f ->
                            val sweep = (f * 360f - gapDeg).coerceAtLeast(0f)
                            if (sweep > 0f) drawArc(TR_CATS[n].third, -90f + acc, sweep, false, tl, sz, style = Stroke(widths[n].dp.toPx()))
                            acc += f * 360f
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (selRow != null) TR_CATS[sel].first else if (real) "Всего" else "Всего за " + TR_PER[per].first.lowercase(), style = KxType.style(12.sp, color = Kx.text3), textAlign = TextAlign.Center)
                        Text(trFmt(selRow?.let { it.first + it.second } ?: tot), style = KxType.style(30.sp, FontWeight.ExtraBold).copy(letterSpacing = (-.5).sp), modifier = Modifier.padding(top = 2.dp))
                        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Outlined.South, null, Modifier.size(15.dp), tint = Kx.accent); Text(trFmt(selRow?.first ?: tin), style = KxType.style(12.sp, color = Kx.text2))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Outlined.North, null, Modifier.size(15.dp), tint = Kx.warning); Text(trFmt(selRow?.second ?: tout), style = KxType.style(12.sp, color = Kx.text2))
                            }
                        }
                    }
                }
                // Live speed (simulated → demo only)
                val sShape = RoundedCornerShape(18.dp)
                if (!real) Row(Modifier.padding(top = 2.dp).fillMaxWidth().clip(sShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), sShape).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val reduced = LocalKxReducedMotion.current
                    val pulse by rememberInfiniteTransition(label = "p").animateFloat(.35f, 1f, infiniteRepeatable(tween(700, easing = TEaseInOut), RepeatMode.Reverse), label = "pa")
                    Box(Modifier.size(8.dp).graphicsLayer { alpha = if (reduced) 1f else pulse }.clip(CircleShape).background(Kx.accent))
                    Text("Сейчас", style = KxType.style(13.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                    Row(Modifier.height(22.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(16) { j ->
                            val v = if (net == 2) .1f else ((kotlin.math.sin((tick + j) * .9) * .5 + .5) * .8 + ((tick * 7 + j * 13) % 5) / 25.0).toFloat()
                            val h by animateDpAsState(maxOf(3, Math.round(v * 22)).dp, tween(400), label = "h$j")
                            Box(Modifier.size(4.dp, h).clip(RoundedCornerShape(2.dp)).background(Brush.verticalGradient(listOf(Kx.accent, Kx.accentDeep))))
                        }
                    }
                    val spd = if (net == 2) .4 else (kotlin.math.sin(tick * .9) * .5 + .5) * (if (net == 1) 6.2 else 1.8) + .2
                    Text(if (spd < 1) "${Math.round(spd * 1000)} КБ/с" else String.format(java.util.Locale.US, "%.1f", spd).replace('.', ',') + " МБ/с",
                        style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), textAlign = TextAlign.End, modifier = Modifier.width(74.dp))
                }
                // Period (Telegram counts only since the last reset → demo only)
                if (!real) Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TR_PER.forEachIndexed { i, (label, _) ->
                        val on = per == i
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "pb")
                        Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).kxClick(role = Role.RadioButton) { per = i }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = KxType.style(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2))
                        }
                    }
                }
                // Categories
                val cShape = RoundedCornerShape(22.dp)
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(cShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), cShape)) {
                    TR_CATS.forEachIndexed { n, (label, ic, c) ->
                        val (i, o) = rows[n]
                        val bg by animateColorAsState(if (sel == n) Kx.accent.copy(alpha = .08f) else Color.Transparent, tween(200), label = "cb")
                        val pct by animateFloatAsState(if (tot > 0) (i + o) / tot else 0f, tween(600, easing = ease), label = "pc")
                        Row(Modifier.fillMaxWidth().background(bg).drawBehind { drawLine(Kx.w(.05f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                            .kxClick { sel = if (sel == n) -1 else n }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TIconTile(ic, Color.White, c, 38.dp)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(label, style = KxType.style(15.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                                    Text(trFmt(i + o), style = KxType.style(14.sp, FontWeight.ExtraBold))
                                }
                                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.07f))) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(pct).clip(RoundedCornerShape(2.dp)).background(c))
                                }
                                Text("Получено ${trFmt(i)} · отправлено ${trFmt(o)}", style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
                val rShape = RoundedCornerShape(20.dp)
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(52.dp).clip(rShape).background(Kx.dangerSolid.copy(alpha = .1f)).border(1.dp, Kx.dangerSolid.copy(alpha = .25f), rShape)
                    .kxClick { confirm = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.RestartAlt, null, Modifier.size(24.dp), tint = Kx.danger)
                    Text("Сбросить статистику", style = KxType.style(15.sp, FontWeight.Bold, Kx.danger))
                }
                Text(if (real) (stats?.since?.takeIf { it.isNotBlank() }?.let { "Считается с $it (с последнего сброса). " } ?: "") + "kiroX считает только свой трафик — у оператора цифры могут отличаться."
                    else if (reset[net]) "Статистика сброшена только что." else "Считается с 3 сентября. kiroX считает только свой трафик — у оператора цифры могут отличаться.",
                    style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp))
            }
        }
        if (confirm) TConfirmSheet(Icons.Outlined.RestartAlt, "Сбросить статистику?",
            if (real) "Telegram обнулит счётчики сразу для всех сетей. Сами файлы и сообщения не удалятся." else "Счётчики для сети «${TR_NETS[net].first}» начнутся с нуля. Сами файлы и сообщения не удалятся.", "Сбросить", { confirm = false }) {
            sel = -1
            if (real) scope.launch { val err = ctx.store.resetNetworkStats(); statsKey++; toast.show(err ?: "Статистика сброшена", Icons.Rounded.Done) }
            else { reset = reset.mapIndexed { i, v -> if (i == net) true else v }; toast.show("Статистика сброшена", Icons.Rounded.Done) }
        }
        TToastHost(toast, 34.dp)
    }
}
