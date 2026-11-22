package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PushLine(val who: String?, val text: String)
private data class PushGroup(val id: String, val title: String, val init: String, val from: Color, val to: Color, val ink: Color, val radius: Dp, val time: String, val lines: List<PushLine>)

// demo: needs store (notification groups come from the system; this board previews their look)
private val PushGroups = listOf(
    PushGroup("anna", "Анна", "А", Color(0xFF4B3F7A), Color(0xFF2A2240), Color(0xFFDCD0FF), 21.dp, "сейчас",
        listOf(PushLine(null, "Ты где? 🙂"), PushLine(null, "Мы уже у кофейни"), PushLine(null, "Возьмёшь мне капучино?"))),
    PushGroup("course", "Курс английского", "К", Color(0xFF2F6E9E), Color(0xFF14243A), Color(0xFFCFE6FF), 14.dp, "5 мин",
        listOf(PushLine("Марина", "Напоминаю про тест в субботу"), PushLine("Олег", "А можно перенести?"))),
)

/** Board «Уведомления»: lock-screen look of kiroX notifications (grouped, expand, reply / read / mute) + in-app banner. */
@Composable
fun PushScreen(@Suppress("UNUSED_PARAMETER") ctx: KxScreenCtx) {
    var open by remember { mutableStateOf("anna") }
    var reply by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    val gone = remember { mutableStateListOf<String>() }
    var leaving by remember { mutableStateOf("") }
    var banner by remember { mutableIntStateOf(0) }
    var toast by remember { mutableIntStateOf(0) }
    var toastText by remember { mutableStateOf("") }
    LaunchedEffect(leaving) { if (leaving.isNotEmpty()) { delay(350); gone.add(leaving); leaving = ""; reply = ""; draft = "" } }
    LaunchedEffect(banner) { if (banner > 0) { delay(3500); banner = 0 } }
    LaunchedEffect(toast) { if (toast > 0) { delay(2200); toast = 0 } }
    fun dismiss(id: String, text: String) { leaving = id; toastText = text; toast++ }
    val now = remember { Date() }
    Box(Modifier.fillMaxSize().lin(170f, 0f to Color(0xFF1B2E25), .55f to Color(0xFF0D1411), 1f to Color(0xFF070908))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)) {
                Text(SimpleDateFormat("HH:mm", Locale("ru")).format(now), style = ts(56.sp, FontWeight.Light, lh = 56.sp, ls = (-1).sp))
                Text(SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(now).replaceFirstChar { it.uppercase() }, style = ts(15.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp))
            }
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val list = PushGroups.filter { it.id !in gone }
                list.forEach { g ->
                    key(g.id) {
                        val isOpen = open == g.id
                        val replying = reply == g.id
                        val rot by animateFloatAsState(if (isOpen) 180f else 0f, tween(250), label = "rot")
                        Column(Modifier.pushAnim(leaving == g.id).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color(0xD1161C19)).border(1.dp, Kx.w(.08f), RoundedCornerShape(26.dp))) {
                            Row(Modifier.fillMaxWidth().kxClick(g.title) { open = if (isOpen) "" else g.id; reply = "" }.padding(start = 14.dp, end = 14.dp, top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).lin2(140f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.ChatBubble, null, Modifier.size(12.dp), tint = Kx.onAccent)
                                }
                                Text("kiroX", style = ts(12.sp, FontWeight.Bold, Kx.text2)); Text("·", style = ts(12.sp, color = Kx.text3)); Text(g.time, style = ts(12.sp, color = Kx.text3))
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(20.dp).graphicsLayer { rotationZ = rot }, tint = Kx.text3)
                            }
                            Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(42.dp).clip(RoundedCornerShape(g.radius)).lin2(140f, g.from, g.to), contentAlignment = Alignment.Center) {
                                    Text(g.init, style = ts(16.sp, FontWeight.Bold, g.ink))
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(g.title, style = ts(15.sp, FontWeight.ExtraBold))
                                        if (g.lines.size > 1) Text("${g.lines.size} новых", style = ts(12.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(bottom = 1.dp))
                                    }
                                    if (!isOpen) KxLine(g.lines.last().text, ts(14.sp, color = Kx.text2), Modifier.padding(top = 2.dp))
                                    else Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        g.lines.forEach { l ->
                                            Text(buildAnnotatedString {
                                                if (l.who != null) withStyle(SpanStyle(color = Kx.text, fontWeight = FontWeight.Bold)) { append("${l.who}: ") }
                                                append(l.text)
                                            }, style = ts(14.sp, color = Kx.text2, lh = 19.sp), modifier = Modifier.exIn(250, dy = (-10).dp))
                                        }
                                    }
                                }
                            }
                            if (isOpen) Box(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp)) {
                                if (replying) Row(Modifier.exIn(200, dy = (-10).dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f)).padding(start = 14.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) {
                                        if (draft.isEmpty()) Text("Ответ: ${g.title}", style = ts(14.sp, color = Kx.text4))
                                        BasicTextField(draft, { draft = it }, singleLine = true, textStyle = ts(14.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
                                    }
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.accent).kxClick("Отправить") { if (draft.isNotBlank()) dismiss(g.id, "Ответ отправлен: ${g.title}") },
                                        contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(18.dp), tint = Kx.onAccent) }
                                }
                                else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PushAct("Ответить", Kx.accent.copy(alpha = .14f), Kx.accent) { reply = g.id; draft = "" }
                                    PushAct("Прочитано", Kx.w(.07f), Kx.text) { dismiss(g.id, "Отмечено как прочитанное") }
                                    PushAct("Без звука", Kx.w(.07f), Kx.text) { dismiss(g.id, "Без звука на 1 час") }
                                }
                            }
                        }
                    }
                }
                if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(top = 40.dp).exIn(300, dy = (-10).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Новых уведомлений нет", style = ts(14.sp, color = Kx.text4))
                    ExPill("Прислать ещё", Modifier.padding(top = 14.dp), height = 40.dp, size = 13.sp, padH = 16.dp) { gone.clear(); open = "anna" }
                }
            }
        }
        ExPill("Баннер внутри приложения", Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp).border(1.dp, Kx.w(.1f), RoundedCornerShape(20.dp)),
            height = 40.dp, size = 13.sp, padH = 16.dp, icon = Icons.Outlined.NotificationsActive) { banner++ }
        if (toast > 0) key(toast) {
            val t = enter(2200, easing = EaseCss)
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 82.dp).fillMaxWidth().height(44.dp).graphicsLayer {
                alpha = when { t < .12f -> t / .12f; t < .85f -> 1f; else -> 1f - (t - .85f) / .15f }; translationY = if (t < .12f) (1f - t / .12f) * 16.dp.toPx() else 0f
            }.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E2622)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(16.dp)),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent)
                Text(toastText, style = ts(14.sp))
            }
        }
        if (banner > 0) key(banner) {
            val p = enter(400, easing = EaseSheet)
            Row(Modifier.statusBarsPadding().padding(10.dp).fillMaxWidth().graphicsLayer { translationY = -(1f - p) * size.height * 1.3f }
                .boxShadow(Color(0x80000000), 40.dp, 16.dp, 22.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF1E2622))
                .background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.06f)))).border(1.dp, Kx.w(.18f), RoundedCornerShape(22.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(CircleShape).lin2(140f, Color(0xFF3A6E7A), Color(0xFF14303A)), contentAlignment = Alignment.Center) {
                    Text("А", style = ts(15.sp, FontWeight.Bold, Color(0xFFCFF4FF)))
                }
                Column(Modifier.weight(1f)) {
                    Text("Алина", style = ts(14.sp, FontWeight.ExtraBold))
                    KxLine("Ты сегодня на уроке будешь?", ts(13.sp, color = Kx.text2))
                }
                Box(Modifier.size(32.dp).clip(CircleShape).kxClick("Скрыть") { banner = 0 }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text3)
                }
            }
        }
    }
}

@Composable
private fun RowScope.PushAct(text: String, bg: Color, ink: Color, onClick: () -> Unit) {
    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(19.dp)).background(bg).kxClick(text, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, style = ts(13.sp, FontWeight.Bold, ink))
    }
}

/** kxIn on appear; kxOut (slide right 120%, fade, collapse) while leaving. */
@Composable
private fun Modifier.pushAnim(leaving: Boolean): Modifier {
    val inP = enter(300)
    val out by animateFloatAsState(if (leaving) 1f else 0f, tween(350, easing = EaseCss), label = "out")
    return this.layout { m, c ->
        val p = m.measure(c)
        val h = (p.height * (1f - out)).toInt()
        layout(p.width, h) { p.placeRelative(0, 0) }
    }.graphicsLayer {
        alpha = inP * (1f - out); translationY = -(1f - inP) * 10.dp.toPx(); translationX = out * 1.2f * size.width
    }
}
