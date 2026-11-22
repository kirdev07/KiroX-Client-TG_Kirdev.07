package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Range slider of the boards: 28dp tall, 8dp track (fill [color] to the value, rest w.10), 26dp #EEF3EF thumb. */
@Composable
internal fun SSlider(value: Int, min: Int, max: Int, color: Color, step: Int = 1, label: String, onChange: (Int) -> Unit) {
    var w by remember { mutableFloatStateOf(1f) }
    val thumb = with(androidx.compose.ui.platform.LocalDensity.current) { 26.dp.toPx() }
    fun pick(x: Float) {
        val f = ((x - thumb / 2) / (w - thumb)).coerceIn(0f, 1f)
        val raw = min + f * (max - min)
        onChange(((Math.round(raw / step) * step).coerceIn(min, max)))
    }
    val frac = (value - min).toFloat() / (max - min)
    Box(Modifier.fillMaxWidth().height(28.dp)
        .semantics { contentDescription = label }.onSizeChanged { w = it.width.toFloat() }
        .pointerInput(min, max, step) { detectTapGestures { pick(it.x) } }
        .pointerInput(min, max, step) { detectHorizontalDragGestures { ch, _ -> pick(ch.position.x) } }
        .drawBehind {
            val ty = size.height / 2 - 4.dp.toPx(); val r = CornerRadius(4.dp.toPx())
            drawRoundRect(Kx.w(.1f), Offset(0f, ty), Size(size.width, 8.dp.toPx()), r)
            val cx = thumb / 2 + frac * (size.width - thumb)
            drawRoundRect(color, Offset(0f, ty), Size(cx, 8.dp.toPx()), r)
            drawCircle(Color.Black.copy(alpha = .35f), thumb / 2 + 2.dp.toPx(), Offset(cx, size.height / 2 + 2.dp.toPx()))
            drawCircle(Kx.text, thumb / 2, Offset(cx, size.height / 2))
        })
}


/** Russian plural: 1 функция / 2 функции / 5 функций. */
internal fun plural(n: Int, one: String, few: String, many: String): String {
    val m = kotlin.math.abs(n) % 100; val d = m % 10
    return if (m in 11..19) many else if (d == 1) one else if (d in 2..4) few else many
}

/** Board «Энергосбережение»: animated battery hero, demo charge slider, threshold slider, what-to-turn-off switches. */
@Composable
fun PowerScreen(ctx: KxScreenCtx) {
    // demo: needs store (power saving threshold, disabled features list; battery level comes from the system — the board's slider is a demo)
    var batt by rememberSaveable { mutableIntStateOf(62) }
    var thr by rememberSaveable { mutableIntStateOf(15) }
    var off by remember { mutableStateOf(listOf(true, true, true, false, true, false)) }
    val saving = thr >= 100 || (thr > 0 && batt <= thr)
    val low = batt <= 20
    val battColor by animateColorAsState(if (saving) Kx.warning else if (low) Kx.danger else Kx.accent, tween(400), label = "bc")
    val glow = if (saving) Color(232, 176, 74).copy(alpha = .45f) else if (low) Kx.danger.copy(alpha = .45f) else Kx.accent.copy(alpha = .4f)
    val cnt = off.count { it }
    val heroA by animateColorAsState(if (saving) Color(232, 176, 74).copy(alpha = .18f) else Kx.accentDeep.copy(alpha = .18f), tween(400), label = "ha")
    val heroB by animateColorAsState(if (saving) Color(232, 176, 74).copy(alpha = .3f) else Kx.accent.copy(alpha = .2f), tween(400), label = "hb")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back, modifier = Modifier.border(1.dp, Kx.w(.09f), CircleShape))
                Text("Энергосбережение", style = KxType.h1, modifier = Modifier.weight(1f))
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
                val shape = RoundedCornerShape(26.dp)
                Row(Modifier.fillMaxWidth().clip(shape).cssGradient(150f, 0f to heroA, 1f to Kx.w(.03f)).border(1.dp, heroB, shape).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Battery(batt, battColor, glow, saving)
                    Column(Modifier.weight(1f)) {
                        Text("$batt%", style = KxType.style(34.sp, FontWeight.ExtraBold, lineHeight = 34.sp))
                        Text(if (saving) "Энергосбережение включено" else "Обычный режим", style = KxType.style(14.sp, FontWeight.Bold, if (saving) Kx.warning else Kx.accent), modifier = Modifier.padding(top = 6.dp))
                        Text(if (saving) "$cnt ${plural(cnt, "функция отключена", "функции отключены", "функций отключено")}, чтобы телефон дольше работал"
                            else if (thr == 0) "Автоматически не включается" else "Включится при $thr% и ниже",
                            style = KxType.style(12.sp, color = Kx.text3, lineHeight = 17.sp), modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp)) {
                    Row { Text("Макет: заряд телефона", style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.weight(1f)); Text("$batt%", style = KxType.style(12.sp, color = Kx.text4)) }
                    SSlider(batt, 1, 100, battColor, label = "Заряд телефона") { batt = it }
                }
                Text("Включать при заряде ниже", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(if (thr == 0) "Не включать" else if (thr >= 100) "Включено всегда" else "Порог заряда", style = KxType.style(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                        Text(if (thr == 0) "Выкл." else if (thr >= 100) "Всегда" else "$thr%", style = KxType.style(22.sp, FontWeight.ExtraBold, Kx.accent))
                    }
                    Spacer(Modifier.height(8.dp))
                    SSlider(thr, 0, 100, Kx.accent, 5, "Порог энергосбережения") { thr = it }
                    Row { Text("Выкл.", style = KxType.style(11.sp, color = Kx.text4), modifier = Modifier.weight(1f)); Text("Всегда", style = KxType.style(11.sp, color = Kx.text4)) }
                }
                Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text("Что отключать", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.weight(1f))
                    Text("$cnt из 6", style = KxType.style(12.sp, color = Kx.text4))
                }
                val opts = listOf<Triple<ImageVector, String, String>>(
                    Triple(Icons.Outlined.Animation, "Анимации интерфейса", "Переходы, лупа в нижней панели"),
                    Triple(Icons.Outlined.AutoAwesome, "Эффекты реакций", "Вспышки при отправке реакций"),
                    Triple(Icons.Outlined.PlayCircle, "Автозапуск видео и GIF", "Будут играть по нажатию"),
                    Triple(Icons.Outlined.BlurOn, "Стекло и размытие", "Панели станут плотнее"),
                    Triple(Icons.Outlined.EmojiEmotions, "Анимированные стикеры", "Покажутся первым кадром"),
                    Triple(Icons.Outlined.Sync, "Фоновое обновление", "Чаты обновятся при открытии"))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg)) {
                    opts.forEachIndexed { i, (ic, label, sub) ->
                        val on = off[i]; val active = on && saving
                        val iconBg by animateColorAsState(if (active) Color(232, 176, 74).copy(alpha = .14f) else Kx.accent.copy(alpha = .1f), tween(250), label = "ib")
                        val iconInk by animateColorAsState(if (active) Kx.warning else Kx.accent, tween(250), label = "ii")
                        Row(Modifier.fillMaxWidth().drawBehind { if (i > 0) drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
                            .kxClick(role = Role.Switch) { off = off.toMutableList().also { it[i] = !on } }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(ic, null, Modifier.size(20.dp), tint = iconInk) }
                            Column(Modifier.weight(1f)) {
                                Text(label, style = KxType.style(15.sp, FontWeight.SemiBold))
                                Text(if (active) "Сейчас отключено" else sub, style = KxType.style(12.sp, color = if (active) Kx.warning else Kx.text3), modifier = Modifier.padding(top = 2.dp))
                            }
                            DarkKnobSwitch(on)
                        }
                    }
                }
                Text("Сообщения и уведомления продолжают приходить. Отключается только то, что тратит заряд впустую.", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp),
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
            }
        }
    }
}

/** 50x30 switch: track accent / w.12, knob #06150D / #8A958F, travel 20 with cubic-bezier(.34,1.45,.5,1) .25s. */
@Composable
private fun DarkKnobSwitch(on: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val x by animateDpAsState(if (on) 20.dp else 0.dp, if (reduced) tween(0) else tween(250, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)), label = "k")
    val track by animateColorAsState(if (on) Kx.accent else Kx.w(.12f), tween(200), label = "t")
    Box(Modifier.size(50.dp, 30.dp).clip(RoundedCornerShape(15.dp)).background(track)) {
        Box(Modifier.offset(3.dp + x, 3.dp).size(24.dp).clip(CircleShape).background(if (on) Kx.onAccent else Kx.text4))
    }
}

@Composable
private fun Battery(batt: Int, color: Color, glow: Color, saving: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val level by animateFloatAsState(batt / 100f, tween(500, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "lvl")
    val inf = rememberInfiniteTransition(label = "batt")
    val w1 by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "w1")
    val w2 by inf.animateFloat(360f, 0f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "w2")
    val pulse by inf.animateFloat(.35f, 1f, infiniteRepeatable(tween(1200, easing = CssEaseInOut), RepeatMode.Reverse), label = "pulse")
    val halo = remember { Animatable(0f) }
    val bubbles = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        launch { repeat(2) { halo.animateTo(1f, tween(1200, easing = CssEaseInOut)); halo.animateTo(0f, tween(1200, easing = CssEaseInOut)) } }
        listOf(Triple(0, 2600, 0), Triple(1, 3200, 900), Triple(2, 2900, 1700)).forEach { (i, d, del) ->
            launch { delay(del.toLong()); repeat(2) { bubbles[i].snapTo(0f); bubbles[i].animateTo(1f, tween(d, easing = FastOutLinearInEasing)) } }
        }
    }
    Box(Modifier.size(62.dp, 104.dp)) {
        Box(Modifier.offset(21.dp, 0.dp).size(20.dp, 8.dp).clip(RoundedCornerShape(3.dp, 3.dp, 0.dp, 0.dp)).background(Kx.dashed))
        Box(Modifier.padding(top = 7.dp).fillMaxSize().drawBehind {
            // kxHalo: 0 0 28px 4px glow at 50%
            if (halo.value > 0f) {
                val g = (4.dp.toPx() + 14.dp.toPx()) * halo.value
                drawRoundRect(Brush.radialGradient(listOf(glow.copy(alpha = glow.alpha * halo.value), Color.Transparent), center, size.maxDimension / 2 + g), Offset(-g, -g),
                    Size(size.width + 2 * g, size.height + 2 * g), CornerRadius(14.dp.toPx() + g))
            }
        }.clip(RoundedCornerShape(14.dp)).border(3.dp, Kx.dashed, RoundedCornerShape(14.dp))) {
            Canvas(Modifier.matchParentSize().padding(3.dp)) {
                val fillH = size.height * level
                val top = size.height - fillH
                clipRect(0f, top, size.width, size.height) {
                    drawRect(color, Offset(0f, top), Size(size.width, fillH))
                    val ph = if (reduced) 0f else 1f
                    val s1 = 140.dp.toPx(); val o1 = Offset(-42.dp.toPx(), top - 132.dp.toPx())
                    rotate(w1 * ph, Offset(o1.x + s1 / 2, o1.y + s1 / 2)) { drawRoundRect(Color(0xFF141917).copy(alpha = .5f), o1, Size(s1, s1), CornerRadius(s1 * .42f)) }
                    val s2 = 150.dp.toPx(); val o2 = Offset(-46.dp.toPx(), top - 144.dp.toPx())
                    rotate(w2 * ph, Offset(o2.x + s2 / 2, o2.y + s2 / 2)) { drawRoundRect(Color(0xFF141917), o2, Size(s2, s2), CornerRadius(s2 * .38f)) }
                    listOf(Triple(12f, 6f, .55f), Triple(30f, 4f, .5f), Triple(22f, 5f, .45f)).forEachIndexed { i, (x, d, a) ->
                        val p = bubbles[i].value
                        if (p > 0f && p < 1f) {
                            val alpha = if (p < .15f) p / .15f else 1f - (p - .15f) / .85f
                            val sc = .6f + .4f * p
                            drawCircle(Color.White.copy(alpha = a * alpha), d.dp.toPx() / 2 * sc, Offset(x.dp.toPx() + d.dp.toPx() / 2, size.height - 4.dp.toPx() - d.dp.toPx() / 2 - 70.dp.toPx() * p))
                        }
                    }
                }
            }
            // inset glow pulse
            Box(Modifier.matchParentSize().graphicsLayer { alpha = if (reduced) 1f else pulse }.drawBehind {
                val g = 18.dp.toPx()
                drawRect(Brush.verticalGradient(listOf(glow, Color.Transparent), 0f, g)); drawRect(Brush.verticalGradient(listOf(Color.Transparent, glow), size.height - g, size.height))
                drawRect(Brush.horizontalGradient(listOf(glow, Color.Transparent), 0f, g)); drawRect(Brush.horizontalGradient(listOf(Color.Transparent, glow), size.width - g, size.width))
            })
            if (saving) {
                val bolt = remember { Animatable(0f) }
                LaunchedEffect(Unit) { if (!reduced) repeat(2) { bolt.animateTo(1f, tween(800, easing = CssEaseInOut)); bolt.animateTo(0f, tween(800, easing = CssEaseInOut)) } }
                Icon(Icons.Rounded.Eco, null, Modifier.align(Alignment.Center).size(28.dp).graphicsLayer { alpha = .6f + .4f * bolt.value; val s = 1f + .15f * bolt.value; scaleX = s; scaleY = s }, tint = Kx.bg)
            }
        }
    }
}

@Suppress("unused") private val keepShadow = Modifier.shadow(0.dp)
