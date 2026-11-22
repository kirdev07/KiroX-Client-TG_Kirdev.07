package com.kirox.app.ui.v3.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private class Place(val name: String, val sub: String, val icon: ImageVector, val ink: Color, val x: Int, val y: Int)

// demo: needs store (nearby venues and sendLocation / sendLiveLocation are not in KxStore yet)
private val Places = listOf(
    Place("Кофейня «Зерно»", "Кафе · 120 м", Icons.Rounded.LocalCafe, Color(0xFFE8B04A), 250, 170),
    Place("Центральный парк", "Парк · 350 м", Icons.Rounded.Park, Kx.accent, 110, 100),
    Place("Станция метро", "Транспорт · 400 м", Icons.Rounded.Subway, Color(0xFF9FB7FF), 300, 330),
    Place("Книжный магазин", "Магазин · 520 м", Icons.AutoMirrored.Rounded.MenuBook, Kx.danger, 160, 280),
)
private val Durs = listOf("15 минут" to 900, "1 час" to 3600, "8 часов" to 28800, "Пока не остановлю" to 0)

/** Board «Геопозиция»: map picker with nearby places, live-location sheet, then the sent map bubble with a countdown ring. */
@Composable
fun LocationScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = ctx.arg?.toLongOrNull() ?: store.chats.firstOrNull { it.kind == KxChatKind.Private }?.id ?: 0L
    val h = store.header(chatId)
    var sent by remember { mutableStateOf(false) }
    var place by remember { mutableIntStateOf(-1) }
    var live by remember { mutableStateOf(false) }
    var dur by remember { mutableIntStateOf(1) }
    var sheet by remember { mutableStateOf(false) }
    var left by remember { mutableIntStateOf(3600) }
    var zoom by remember { mutableStateOf(false) }
    val sel = Places.getOrNull(place)
    val total = Durs[dur].second
    LaunchedEffect(live, sent) { while (live && sent) { delay(1000); left = maxOf(0, left - 60) } }
    BackHandler(sent || sheet) { if (sheet) sheet = false else sent = false }
    fun fmt(sec: Int) = if (sec >= 3600) "${Math.round(sec / 3600f)} ч" else "${maxOf(1, Math.round(sec / 60f))} мин"

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        if (!sent) Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(0.dp)).background(Color(0xFF0F1513))) {
                val mx by animateDpAsState(if (sel != null) ((195 - sel.x - 13 + 40) * .6f).dp else 0.dp, tween(600, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "mx")
                val my by animateDpAsState(if (sel != null) ((215 - sel.y - 13 + 40) * .6f).dp else 0.dp, tween(600, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "my")
                val mz by animateFloatAsState(if (zoom) 1.25f else 1f, tween(600, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "mz")
                Box(Modifier.requiredSize(470.dp, 510.dp).align(Alignment.Center).graphicsLayer { translationX = mx.toPx(); translationY = my.toPx(); scaleX = mz; scaleY = mz }) {
                    MapBlock(40, 60, 160, 120, 30, Color(0xFF15251C))
                    Box(Modifier.align(Alignment.TopEnd).offset((-30).dp, 300.dp).size(200.dp, 150.dp).clip(RoundedCornerShape(60.dp)).background(Color(0xFF15251C)))
                    Box(Modifier.offset((-20).dp, 380.dp).size(520.dp, 46.dp).rotate(-8f).background(Color(0xFF10202A)))
                    Box(Modifier.offset(0.dp, 230.dp).fillMaxWidth().height(14.dp).background(Color(0xFF262E2B)))
                    Box(Modifier.offset(0.dp, 140.dp).fillMaxWidth().height(8.dp).background(Color(0xFF1F2624)))
                    Box(Modifier.offset(210.dp, 0.dp).width(14.dp).fillMaxHeight().background(Color(0xFF262E2B)))
                    Box(Modifier.offset(90.dp, 0.dp).width(8.dp).fillMaxHeight().background(Color(0xFF1F2624)))
                    Box(Modifier.offset(330.dp, 0.dp).width(8.dp).fillMaxHeight().rotate(12f).background(Color(0xFF1F2624)))
                    Places.forEach { p ->
                        Box(Modifier.offset(p.x.dp, p.y.dp).size(26.dp).clip(CircleShape).background(Color(0xFF1E2622)).border(1.dp, Kx.w(.12f), CircleShape),
                            contentAlignment = Alignment.Center) { Icon(p.icon, null, Modifier.size(15.dp), tint = p.ink) }
                    }
                }
                MeDot(Modifier.align(Alignment.Center), pulse = true, inset = 3.dp)
                if (sel != null) key(place) {
                    val drop = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { drop.animateTo(1f, tween(400, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f))) }
                    Box(Modifier.align(Alignment.Center).offset(0.dp, (-28).dp).graphicsLayer { translationY = (1 - drop.value) * -30.dp.toPx() }
                        .size(36.dp).rotate(-45f).shadow(10.dp, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                        .cssGradient(140f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                        Icon(sel.icon, null, Modifier.size(18.dp).rotate(45f), tint = Kx.onAccent)
                    }
                }
                Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Kx.bg.copy(alpha = .85f), Color.Transparent))).statusBarsPadding().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChRound(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.1f), border = Kx.w(.14f), onClick = ctx.back)
                    Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.1f)).border(1.dp, Kx.w(.14f), RoundedCornerShape(22.dp))
                        .kxClick("Поиск места") { ctx.toast("Поиск мест появится вместе с картами") }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text3); Text("Поиск места", style = ts(14, color = Kx.text3))
                    }
                }
                Column(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChRound(Icons.Outlined.Layers, "Слой карты", bg = Kx.w(.1f), border = Kx.w(.14f)) { zoom = !zoom }
                    ChRound(Icons.Rounded.MyLocation, "Моё местоположение", bg = Kx.w(.1f), border = Kx.w(.14f), tint = Kx.accent) { place = -1 }
                }
            }
            Column(Modifier.weight(1f).offset(y = (-24).dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color(0xFF111513))
                .drawBehind { drawLine(Kx.w(.08f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 44.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.dashed))
                LocRow(Icons.Rounded.NearMe, "Отправить мою геопозицию", "Точность 12 м", true) { live = false; place = -1; sent = true }
                Spacer(Modifier.height(8.dp))
                LocRow(Icons.Outlined.ShareLocation, "Транслировать геопозицию", "Обновляется, пока вы двигаетесь", false) { sheet = true }
                Text("Места рядом", style = ts(13, W800, Kx.accent), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp))
                Places.forEachIndexed { i, p ->
                    val on = place == i
                    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .08f) else Color.Transparent, tween(200), label = "pl")
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).kxClick(p.name) { if (on) { live = false; sent = true } else place = i }
                        .padding(horizontal = 8.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.06f)), contentAlignment = Alignment.Center) { Icon(p.icon, null, Modifier.size(21.dp), tint = p.ink) }
                        Column(Modifier.weight(1f)) { Text(p.name, style = ts(15, W600), maxLines = 1); Text(p.sub, style = ts(12, color = Kx.text4), modifier = Modifier.padding(top = 2.dp)) }
                        if (on) Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.accent).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                            Text("Отправить", style = ts(13, W800, Kx.onAccent))
                        }
                    }
                }
            }
        }
        if (sent) Column(Modifier.fillMaxSize().ellipseGlow(1f, 0f, .72f, .36f, Kx.accentDeep.copy(alpha = .18f)).statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 42.dp) { sent = false }
                KxAvatar(h.title, chatId, size = 38.dp, colorIndex = h.colorIndex)
                Column { Text(h.title, style = ts(16, W800), maxLines = 1); Text(h.status, style = ts(12, color = if (h.online) Kx.accent else Kx.text4), maxLines = 1) }
            }
            if (live) Row(Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(44.dp).chIn(dy = 10.dp).clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .1f))
                .border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(16.dp)).padding(start = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.ShareLocation, null, Modifier.size(20.dp), tint = Kx.accent)
                Text("Вы транслируете геопозицию · ${if (total > 0) "ещё " + fmt(left) else "без ограничения"}", style = ts(13, W600), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).kxClick { live = false }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text("Остановить", style = ts(13, W700, Kx.danger))
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 30.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                val appear = remember { Animatable(0f) }
                LaunchedEffect(Unit) { appear.animateTo(1f, tween(450, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) }
                Column(Modifier.width(270.dp).graphicsLayer { alpha = appear.value; translationY = (1 - appear.value) * 40.dp.toPx(); val s = .95f + .05f * appear.value; scaleX = s; scaleY = s }
                    .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp)).cssGradient(160f, Color(0xFF23523B), Color(0xFF183A2A))) {
                    Box(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF0F1513))) {
                        Box(Modifier.offset(0.dp, 70.dp).fillMaxWidth().height(10.dp).background(Color(0xFF262E2B)))
                        Box(Modifier.offset(120.dp, 0.dp).width(10.dp).fillMaxHeight().background(Color(0xFF262E2B)))
                        MapBlock(20, 14, 80, 46, 16, Color(0xFF15251C))
                        Box(Modifier.align(Alignment.BottomEnd).offset((-18).dp, (-12).dp).size(100.dp, 50.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF15251C)))
                        MeDot(Modifier.align(Alignment.Center), pulse = live, inset = 0.dp, gradient = true)
                    }
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            KxLine(if (live) "Геопозиция в реальном времени" else sel?.name ?: "Моя геопозиция", ts(14, W700))
                            Text(if (live) "обновлено только что" else sel?.sub ?: "Точность 12 м", style = ts(12, color = Color(0xFFA9D9BF)), modifier = Modifier.padding(top = 2.dp))
                        }
                        if (live) {
                            val frac by animateFloatAsState(if (total > 0) left / total.toFloat() else 1f, tween(600), label = "ring")
                            Box(Modifier.size(34.dp).drawBehind {
                                drawCircle(Kx.w(.15f)); drawArc(Kx.accent, -90f, 360f * frac, true)
                                drawCircle(Color(0xFF1C4231), radius = size.minDimension / 2 - 3.dp.toPx())
                            }, contentAlignment = Alignment.Center) {
                                Text(if (total > 0) fmt(left).replace(" мин", "м").replace(" ч", "ч") else "∞", style = ts(11, W800))
                            }
                        }
                    }
                }
            }
        }
        ChSheet(sheet, { sheet = false }, scrim = Color.Black.copy(alpha = .5f), bg = Brush.verticalGradient(listOf(Color(0xFF121614), Color(0xFF121614)))) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp)) {
                Text("Транслировать геопозицию", style = ts(19, W800), modifier = Modifier.padding(horizontal = 4.dp))
                Text("${h.title.substringBefore(' ')} будет видеть, где вы, в течение выбранного времени. Остановить можно в любой момент.",
                    style = ts(13, color = Kx.text3, lh = 19.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 14.dp))
                Durs.chunked(2).forEachIndexed { r, row ->
                    Row(Modifier.padding(top = if (r > 0) 8.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { c, (label, _) ->
                            val i = r * 2 + c; val on = i == dur
                            val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.045f), tween(200), label = "d")
                            Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, if (on) Kx.accent.copy(alpha = .5f) else Color.Transparent, RoundedCornerShape(18.dp))
                                .kxClick(label) { dur = i }, contentAlignment = Alignment.Center) {
                                Text(label, style = ts(15, W700, if (on) Kx.accent else Kx.text), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).cssGradient(100f, Kx.accentMid, Kx.accentDeep)
                    .kxClick { sheet = false; live = true; place = -1; left = if (total > 0) total else 3600; sent = true }, contentAlignment = Alignment.Center) {
                    Text("Начать трансляцию", style = ts(15, W700, Kx.onAccent))
                }
            }
        }
    }
}

@Composable
private fun MapBlock(x: Int, y: Int, w: Int, h: Int, r: Int, c: Color) =
    Box(Modifier.offset(x.dp, y.dp).size(w.dp, h.dp).clip(RoundedCornerShape(r.dp)).background(c))

/** «Me» dot: 22dp with the kxPulse halo (scale .6→2.4, 2s, endless while shown — a process animation). */
@Composable
private fun MeDot(modifier: Modifier, pulse: Boolean, inset: Dp, gradient: Boolean = false) {
    val reduced = LocalKxReducedMotion.current
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing)), label = "p")
    Box(modifier.size(22.dp)) {
        if (pulse && !reduced) Box(Modifier.matchParentSize().graphicsLayer { val s = .6f + 1.8f * p; scaleX = s; scaleY = s; alpha = .7f * (1 - p) }.clip(CircleShape).background(Kx.accent.copy(alpha = .5f)))
        Box(Modifier.matchParentSize().padding(inset).drawBehind {
            if (!gradient) drawCircle(Kx.accent, radius = size.minDimension / 2 + 2.dp.toPx())
            if (gradient) drawCircle(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)))
            else drawCircle(Kx.bg)
            drawCircle(if (gradient) Color(0xFF0F1513) else Kx.accent, radius = size.minDimension / 2 - 3.dp.toPx(), style = if (gradient) Stroke(3.dp.toPx()) else androidx.compose.ui.graphics.drawscope.Fill)
        })
    }
}

@Composable
private fun LocRow(icon: ImageVector, title: String, sub: String, primary: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (primary) Kx.accent.copy(alpha = .08f) else Kx.w(.045f))
        .then(if (primary) Modifier.border(1.dp, Kx.accent.copy(alpha = .22f), RoundedCornerShape(20.dp)) else Modifier)
        .kxClick(title, onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).then(if (primary) Modifier.cssGradient(140f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.accent.copy(alpha = .14f))),
            contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (primary) Kx.onAccent else Kx.accent) }
        Column(Modifier.weight(1f)) { Text(title, style = ts(15, W700)); Text(sub, style = ts(12, color = Kx.text3), modifier = Modifier.padding(top = 2.dp)) }
    }
}

