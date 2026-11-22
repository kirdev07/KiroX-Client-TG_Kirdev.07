package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class AdOpt(val label: String, val icon: ImageVector, val iconOn: ImageVector, val short: String)
private val AD_OPTS = listOf(
    AdOpt("Выключено", Icons.Outlined.TimerOff, Icons.Rounded.TimerOff, "выкл"),
    AdOpt("1 час", Icons.Outlined.HourglassTop, Icons.Rounded.HourglassTop, "1 час"),
    AdOpt("1 день", Icons.Outlined.Today, Icons.Rounded.Today, "1 день"),
    AdOpt("1 неделя", Icons.Outlined.DateRange, Icons.Rounded.DateRange, "1 нед."),
    AdOpt("1 месяц", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth, "1 мес."),
    AdOpt("Свой срок", Icons.Outlined.Tune, Icons.Rounded.Tune, ""),
)
private val AD_CUSTOM = listOf("1 час", "3 часа", "6 часов", "12 часов", "1 день", "2 дня", "3 дня", "1 неделя", "2 недели", "1 месяц", "3 месяца", "1 год")
private data class AdMsg(val text: String, val mine: Boolean, val time: String)
private val AD_MSG = listOf(AdMsg("Привет! Завтра в силе?", false, "18:02"), AdMsg("Да, в 10 у метро", true, "18:03"), AdMsg("Код от подъезда 4512", false, "18:04"))

/** Board «Автоудаление»: live ring + vanishing bubbles preview, period grid, custom period slider, default for new chats, save. */
@Composable
fun AutoDeleteScreen(ctx: KxScreenCtx) {
    // demo: needs store (chat auto-delete timer get/set, default timer for new chats)
    val name = ctx.arg?.toLongOrNull()?.let { runCatching { ctx.store.header(it).title }.getOrNull() } ?: "Анна"
    var opt by rememberSaveable { mutableIntStateOf(2) }
    var customVal by rememberSaveable { mutableIntStateOf(7) }
    var all by rememberSaveable { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(opt) { tick = 0; while (true) { delay(1000); tick++ } }
    val toast = remember { TToast() }
    val on = opt != 0
    val period = if (opt == 5) AD_CUSTOM[customVal] else AD_OPTS[opt].label
    val t = tick % 8
    val frac = if (on) t / 7f else 0f
    val goneIdx = if (on && t >= 6) 2 else -1

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    Text("Автоудаление", style = KxType.style(20.sp, FontWeight.ExtraBold), maxLines = 1)
                    Text("Чат: $name", style = KxType.style(12.sp, color = Kx.text3), maxLines = 1)
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)) {
                // Live preview
                val pShape = RoundedCornerShape(26.dp)
                Box(Modifier.fillMaxWidth().height(236.dp).clip(pShape).background(Color(0xFF101413))
                    .drawBehind {
                        val c = Offset(size.width * .8f, 0f)
                        drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .28f), .6f to Color.Transparent, center = c, radius = size.width * 1.2f))
                    }.border(1.dp, Kx.w(.08f), pShape)) {
                    val ringInk by animateColorAsState(if (on) Kx.accent else Color(0xFF5C6661), tween(300), label = "ri")
                    val off by animateFloatAsState(frac, if (frac == 0f) tween(0) else tween(900, easing = LinearEasing), label = "ro")
                    Box(Modifier.align(Alignment.TopCenter).padding(top = 18.dp).size(86.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.matchParentSize()) {
                            val r = 37.dp.toPx(); val sw = 6.dp.toPx(); val tl = Offset(center.x - r, center.y - r); val sz = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                            drawCircle(Kx.w(.08f), r, style = Stroke(sw))
                            // dashoffset grows → the visible arc shrinks from the end
                            if (off < .999f) drawArc(ringInk, -90f, 360f * (1f - off), false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if (on) Icons.Rounded.Timer else Icons.Rounded.TimerOff, null, Modifier.size(22.dp), tint = ringInk)
                            Text(if (on) (if (opt == 5) AD_CUSTOM[customVal] else AD_OPTS[opt].short) else "выкл",
                                style = KxType.style(12.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AD_MSG.forEachIndexed { i, m ->
                            val gone = i == goneIdx
                            val g by animateFloatAsState(if (gone) 1f else 0f, tween(800, easing = TEase), label = "g$i")
                            val shape = if (m.mine) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
                            val ink = if (m.mine) Kx.onAccent else Kx.text
                            Row(Modifier.align(if (m.mine) Alignment.End else Alignment.Start).fillMaxWidth(.74f).wrapContentWidth(if (m.mine) Alignment.End else Alignment.Start)
                                .graphicsLayer { alpha = 1f - g; val s = 1f - .08f * g; scaleX = s; scaleY = s; translationY = -6.dp.toPx() * g }
                                .then(if (g > 0f) Modifier.blur((6 * g).dp) else Modifier)
                                .clip(shape).background(if (m.mine) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.w(.08f)))
                                .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                                Text(m.text, style = KxType.style(14.sp, color = ink), modifier = Modifier.weight(1f, false))
                                Row(Modifier.padding(start = 8.dp).graphicsLayer { alpha = .7f }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (on && i > 0) Icon(Icons.Outlined.Timer, null, Modifier.size(13.dp), tint = ink)
                                    Text(m.time, style = KxType.style(11.sp, color = ink))
                                }
                            }
                        }
                    }
                }
                Text(if (on) "Сообщения исчезают у вас и у собеседника. Срок: $period после отправки." else "Сообщения хранятся, пока вы сами их не удалите.",
                    style = KxType.style(13.sp, color = Kx.text3, lineHeight = 19.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp))

                Text("Удалять сообщения через", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
                AD_OPTS.chunked(2).forEachIndexed { r, pair ->
                    Row(Modifier.padding(top = if (r > 0) 8.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEachIndexed { k, o ->
                            val i = r * 2 + k; val sel = opt == i
                            val bg by animateColorAsState(if (sel) Kx.accent.copy(alpha = .12f) else Kx.w(.04f), tween(200), label = "b")
                            val bd by animateColorAsState(if (sel) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "d")
                            val shape = RoundedCornerShape(18.dp)
                            Row(Modifier.weight(1f).height(58.dp).clip(shape).background(bg).border(1.dp, bd, shape).kxClick(role = Role.RadioButton) { opt = i }.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(if (sel) o.iconOn else o.icon, null, Modifier.size(22.dp), tint = if (sel) Kx.accent else Kx.text3)
                                Text(o.label, style = KxType.style(14.sp, FontWeight.Bold, if (sel) Kx.text else Kx.text2), modifier = Modifier.weight(1f))
                                if (sel) {
                                    val pop = remember { Animatable(.9f) }
                                    LaunchedEffect(Unit) { pop.animateTo(1f, tween(200, easing = TEase)) }
                                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = (pop.value - .9f) * 10f }, tint = Kx.accent)
                                }
                            }
                        }
                    }
                }

                if (opt == 5) {
                    val shape = RoundedCornerShape(20.dp)
                    Column(Modifier.padding(top = 10.dp).tRise(0, 250, 8.dp).fillMaxWidth().clip(shape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), shape).padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Свой срок", style = KxType.style(14.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                            Text(AD_CUSTOM[customVal], style = KxType.style(16.sp, FontWeight.ExtraBold, Kx.accent))
                        }
                        Spacer(Modifier.height(14.dp))
                        AdSlider(customVal, 11) { customVal = it }
                        Row(Modifier.padding(top = 8.dp)) {
                            Text("1 час", style = KxType.style(11.sp, color = Kx.text4)); Spacer(Modifier.weight(1f)); Text("1 год", style = KxType.style(11.sp, color = Kx.text4))
                        }
                    }
                }

                val eShape = RoundedCornerShape(22.dp)
                Column(Modifier.padding(top = 18.dp).fillMaxWidth().clip(eShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), eShape)) {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.History, null, Modifier.size(24.dp), tint = Kx.text4)
                        Text("Таймер действует только на новые сообщения — старые останутся", style = KxType.style(13.sp, color = Kx.text3, lineHeight = 18.sp))
                    }
                    Box(Modifier.padding(start = 52.dp).fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
                    TRow(Icons.Outlined.AutoDelete, "Для всех новых чатов", if (all && on) "Новые чаты сразу с таймером: $period" else "Сейчас: выключено",
                        iconTint = Kx.text3, subColor = Kx.text4, vPad = 14.dp, onClick = { all = !all }) {
                        TSmallSwitch(all)
                    }
                }
                Text("Сообщения удаляются у обоих. $name увидит, что вы включили автоудаление.", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp))
            }
        }
        TSaveBar("Сохранить", Icons.Rounded.Done) {
            toast.show(if (on) "Автоудаление: $period" else "Автоудаление выключено", if (on) Icons.Rounded.Timer else Icons.Rounded.TimerOff)
        }
        TToastHost(toast, 96.dp)
    }
}

/** 6dp w.10 track, 22dp accent thumb with a 3dp #0A0C0B border and 1dp accent ring. */
@Composable
private fun AdSlider(value: Int, max: Int, onChange: (Int) -> Unit) {
    var w by remember { mutableFloatStateOf(1f) }
    val thumb = with(LocalDensity.current) { 22.dp.toPx() }
    fun pick(x: Float) { onChange(Math.round(((x - thumb / 2) / (w - thumb)).coerceIn(0f, 1f) * max)) }
    Box(Modifier.fillMaxWidth().height(24.dp).semantics { contentDescription = "Свой срок автоудаления" }.onSizeChanged { w = it.width.toFloat() }
        .pointerInput(Unit) { detectTapGestures { pick(it.x) } }
        .pointerInput(Unit) { detectHorizontalDragGestures { ch, _ -> pick(ch.position.x) } }
        .drawBehind {
            val cy = size.height / 2
            drawRoundRect(Kx.w(.1f), Offset(0f, cy - 3.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            val cx = thumb / 2 + value.toFloat() / max * (size.width - thumb)
            drawCircle(Kx.accent, thumb / 2 + 1.dp.toPx(), Offset(cx, cy))
            drawCircle(Kx.bg, thumb / 2, Offset(cx, cy))
            drawCircle(Kx.accent, thumb / 2 - 3.dp.toPx(), Offset(cx, cy))
        })
}
