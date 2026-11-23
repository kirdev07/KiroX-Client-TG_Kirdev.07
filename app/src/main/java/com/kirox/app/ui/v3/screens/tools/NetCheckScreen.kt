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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val NC_STEPS = listOf("Интернет", "Поиск серверов", "Серверы Telegram", "Скорость")
private val NC_DCS = listOf("DC1" to 186, "DC2" to 48, "DC3" to 192, "DC4" to 61, "DC5" to 214)

/** Board «Проверка соединения»: phone → internet → Telegram path, 4 step checks with spinners, data-centre latency bars, verdict; demo toggle norm / blocked. */
@Composable
fun NetCheckScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → network type, DNS lookup, TDLib PingProxy (direct + enabled proxy); no speed test, no per-DC list
    val real = ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore
    var res by remember { mutableStateOf<com.kirox.app.ui.v3.model.KxNetCheck?>(null) }
    val steps = if (real) NC_STEPS.take(3) else NC_STEPS
    var step by remember { mutableIntStateOf(-1) }
    var bad by remember { mutableStateOf(false) }
    var dc by remember { mutableStateOf(false) }
    var runId by remember { mutableIntStateOf(0) }
    LaunchedEffect(runId) {
        if (runId == 0) return@LaunchedEffect
        step = 0; dc = false
        if (real) {
            res = null
            val r = ctx.store.netCheck()
            res = r
            for (k in 0 until 3) { delay(300); step = k + 1 }
        } else for (k in 0 until 4) { delay(900); step = k + 1; dc = k + 1 >= 3 }
    }
    val done = step >= steps.size
    val r = res
    val failAt = if (!real) (if (bad) 2 else 99) else when {
        r == null -> 99
        !r.online -> 0
        r.dnsOk == false -> 1
        r.pingMs == null && r.proxyPingMs == null -> 2
        else -> 99
    }
    fun st(i: Int) = if (step < 0 || i > step) "wait" else if (i == step && !done) "run" else if (i >= failAt) "fail" else "ok"
    val subs = if (real) listOf(
        r?.let { if (it.online) it.network else "Нет подключения к сети" } ?: "",
        if (r?.dnsOk == false) "Телефон не находит адрес telegram.org — проблема у провайдера или в настройках сети" else "Работает",
        when {
            r == null -> ""
            r.pingMs != null -> "Ответ сервера за ${r.pingMs} мс · ${r.connection.lowercase()}"
            r.proxyPingMs != null -> "Через прокси ${r.proxy} за ${r.proxyPingMs} мс"
            else -> "Сервер не отвечает — возможно, блокировка"
        },
    ) else if (bad) listOf("Wi-Fi «Home_5G» · сигнал отличный", "Работает", "Сервер не отвечает — возможно, блокировка", "Не проверялось")
        else listOf("Wi-Fi «Home_5G» · сигнал отличный", "Работает", "Подключено к DC2 за 48 мс", "18,4 Мбит/с — фото и видео грузятся быстро")
    val running = step >= 0 && !done
    val reduced = LocalKxReducedMotion.current
    val inf = rememberInfiniteTransition(label = "nc")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "spin")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearOutSlowInEasing)), label = "pulse")
    val dash by inf.animateFloat(24f, 0f, infiniteRepeatable(tween(600, easing = LinearEasing)), label = "dash")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            THeader("Проверка соединения", ctx.back, titleSize = 20)
            val dShape = RoundedCornerShape(26.dp)
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().height(150.dp).clip(dShape).background(Kx.w(.03f))
                .drawBehind { drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .18f), .7f to Color.Transparent, center = Offset(size.width / 2, 0f), radius = size.width * .9f)) }
                .border(1.dp, Kx.w(.08f), dShape)) {
                fun lineC(k: Int) = when (st(k)) { "ok" -> Kx.accent; "fail" -> Kx.danger; else -> Kx.w(.15f) }
                Canvas(Modifier.matchParentSize()) {
                    val y = 70.dp.toPx(); val u = density
                    listOf(1 to (.18f to .5f), 2 to (.5f to .82f)).forEach { (k, xs) ->
                        val ph = if (st(k) == "run" && !reduced) dash * u else 0f
                        drawLine(lineC(k), Offset(size.width * xs.first, y), Offset(size.width * xs.second, y), 3 * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6 * u, 6 * u), ph))
                    }
                }
                listOf(Triple(Icons.Rounded.PhoneAndroid, "Телефон", .18f), Triple(Icons.Rounded.Router, "Интернет", .5f), Triple(Icons.AutoMirrored.Rounded.Send, "Telegram", .82f)).forEachIndexed { k, (ic, label, x) ->
                    val v = st(k)
                    val bg by animateColorAsState(when (v) { "ok" -> Kx.accent.copy(alpha = .16f); "fail" -> Kx.dangerSolid.copy(alpha = .16f); "run" -> Kx.w(.08f); else -> Kx.w(.04f) }, tween(300), label = "nb")
                    val ink by animateColorAsState(when (v) { "ok" -> Kx.accent; "fail" -> Kx.danger; "run" -> Kx.text; else -> Kx.text4 }, tween(300), label = "ni")
                    val bd by animateColorAsState(when (v) { "ok", "run" -> Kx.accent.copy(alpha = .5f); "fail" -> Kx.dangerSolid.copy(alpha = .5f); else -> Kx.w(.1f) }, tween(300), label = "nd")
                    BoxWithConstraints(Modifier.matchParentSize()) {
                        Column(Modifier.offset(x = maxWidth * x - 30.dp, y = 40.dp).width(60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                if (v == "run" && !reduced) Box(Modifier.matchParentSize().graphicsLayer { val s = .6f + 1.6f * pulse; scaleX = s; scaleY = s; alpha = .7f * (1f - pulse) }.border(2.dp, Kx.accent, CircleShape))
                                Box(Modifier.matchParentSize().clip(CircleShape).background(bg).border(1.5.dp, bd, CircleShape), contentAlignment = Alignment.Center) { Icon(ic, null, Modifier.size(28.dp), tint = ink) }
                            }
                            Text(label, style = KxType.style(11.sp, color = Kx.text2), maxLines = 1)
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    steps.forEachIndexed { i, label ->
                        val v = st(i)
                        Row(Modifier.fillMaxWidth().bottomLine().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                when (v) {
                                    "wait" -> Box(Modifier.size(10.dp).clip(CircleShape).background(Kx.w(.2f)))
                                    "run" -> Canvas(Modifier.size(22.dp).graphicsLayer { rotationZ = spin }) {
                                        val sw = 2.5f * density
                                        drawCircle(Kx.accent.copy(alpha = .25f), size.minDimension / 2 - sw / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                                        drawArc(Kx.accent, -135f, 90f, false, Offset(sw / 2, sw / 2), androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                                    }
                                    else -> key(v) { PopIcon(if (v == "fail") Icons.Rounded.Cancel else Icons.Rounded.CheckCircle, 24.dp, if (v == "fail") Kx.danger else Kx.accent, 300) }
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(label, style = KxType.style(15.sp, FontWeight.SemiBold))
                                Text(if (v == "wait") "Ожидает" else if (v == "run") "Проверяем…" else subs[i], style = KxType.style(12.sp, color = if (v == "fail") Kx.danger else Kx.text4))
                            }
                        }
                    }
                }
                if (dc && !bad && !real) Column(Modifier.tRise(0, 300, 8.dp)) {
                    TCap("Серверы Telegram · задержка", top = 16.dp)
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        NC_DCS.forEach { (name, ms) ->
                            val c = if (ms < 80) Kx.accent else if (ms < 150) Kx.warning else Kx.text3
                            val w = remember { Animatable(0f) }
                            LaunchedEffect(Unit) { w.animateTo(minOf(1f, ms / 220f), tween(600, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) }
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(name, style = KxType.style(13.sp, FontWeight.Bold), modifier = Modifier.width(56.dp))
                                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Kx.w(.06f))) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(w.value).clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(c))
                                }
                                Text("$ms мс", style = KxType.style(13.sp, FontWeight.Bold, if (ms < 80) Kx.accent else Kx.text2).copy(fontFeatureSettings = "tnum"), textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
                            }
                        }
                        Text("Ваш сервер — DC2 (Европа). Меньше — лучше.", style = KxType.style(11.sp, color = Kx.text4), modifier = Modifier.padding(top = 6.dp))
                    }
                }
                if (done) {
                    val bad = if (real) failAt < 99 else bad
                    val c = if (bad) Kx.danger else Kx.accent
                    Row(Modifier.padding(top = 14.dp).tRise(0, 300, 8.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(if (bad) Kx.dangerSolid.copy(alpha = .08f) else Kx.accent.copy(alpha = .08f))
                        .border(1.dp, if (bad) Kx.dangerSolid.copy(alpha = .3f) else Kx.accent.copy(alpha = .3f), RoundedCornerShape(22.dp))
                        .then(if (bad) Modifier.kxClick { ctx.open("Proxy") } else Modifier).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (bad) Icons.Rounded.VpnLock else Icons.Rounded.Verified, null, Modifier.size(28.dp), tint = c)
                        Column(Modifier.weight(1f)) {
                            Text(if (bad) (if (real && failAt == 0) "Нет интернета" else "Telegram недоступен в этой сети") else "Всё в порядке", style = KxType.style(15.sp, FontWeight.ExtraBold))
                            Text(if (bad) "Включите прокси в «Данные и память → Прокси» или попробуйте мобильный интернет." else "Соединение быстрое и стабильное. Если что-то не грузится — дело не в сети.",
                                style = KxType.style(13.sp, color = Kx.text2, lineHeight = 19.sp), modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Kx.bg.copy(alpha = 0f), .4f to Kx.bg)).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!real) Row(Modifier.height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.w(.06f)).kxClick { bad = !bad; step = -1; dc = false; runId = 0 }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(if (bad) Icons.Outlined.WifiOff else Icons.Outlined.Wifi, null, Modifier.size(18.dp), tint = Kx.text3)
                Text(if (bad) "Пример: блок" else "Пример: норма", style = KxType.style(12.sp, FontWeight.Bold, Kx.text3))
            }
            TPill(if (running) "Проверяем…" else if (done) "Проверить снова" else "Начать проверку", Modifier.weight(1f), Icons.Rounded.NetworkCheck, 54.dp,
                if (running) solid(Kx.w(.07f)) else Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), if (running) Color(0xFF5C6661) else Kx.onAccent, 16) { runId++ }
        }
    }
}
