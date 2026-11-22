package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// demo: needs store (chat invite link info: title, photo, private, members, known members, description, join-by-request; joinChatByInviteLink)
private val JConf = listOf(Kx.accent, Kx.warning, Color(0xFFC0443C), Color(0xFF7FB8E0), Color(0xFF6A4C9C))
private val SheetInk = Color(0xFF1A1F1C)

/** Board «Заявка в группу»: invite-link sheet over the chat → «Подать заявку» → waiting (hourglass) → accepted with confetti. */
@Composable
fun JoinRequestScreen(ctx: KxScreenCtx) {
    var st by rememberSaveable { mutableIntStateOf(0) }
    val reduced = LocalKxReducedMotion.current
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().hairline().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(24.dp), tint = Kx.text)
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFC0443C)), contentAlignment = Alignment.Center) { Text("А", style = KxType.style(16.sp, FontWeight.ExtraBold)) }
                Text("Анна", style = KxType.style(16.sp, FontWeight.ExtraBold))
            }
            Text(buildAnnotatedString { append("Залетай к нам в клуб настолок 🎲\n"); withStyle(SpanStyle(color = Kx.accent)) { append("t.me/+Hx7…") } },
                style = KxType.style(15.sp), modifier = Modifier.padding(14.dp).fillMaxWidth(.78f).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f))
                    .padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).gxPlainClick(ctx.back))
        val rise = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) { rise.animateTo(1f, tween(400, easing = KxMotion.rise)) }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = (1f - rise.value) * size.height }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Brush.verticalGradient(listOf(Color(0xFF28302C), Color(0xFF121614))))
            .hairline(Kx.w(.16f), top = true).gxPlainClick {}) {
            if (st == 2 && !reduced) BoxWithConstraints(Modifier.matchParentSize()) {
                val w = maxWidth
                repeat(18) { i ->
                    val t = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { delay((i % 6) * 80L); t.animateTo(1f, tween(1600, easing = FastOutLinearInEasing)) }
                    Box(Modifier.offset(x = w * (((i * 41) % 96 + 2) / 100f)).size(8.dp, 12.dp).graphicsLayer {
                        translationY = 260.dp.toPx() * t.value; rotationZ = 400f * t.value; alpha = 1f - t.value
                    }.clip(RoundedCornerShape(2.dp)).background(JConf[i % JConf.size]))
                }
            }
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(bottom = 16.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
                Box(Modifier.size(84.dp)) {
                    Box(Modifier.fillMaxSize().clip(CircleShape).cssLinear(135f, Color(0xFFB07A2E), Color(0xFF6A4C9C)), contentAlignment = Alignment.Center) { Text("🎲", style = KxType.style(40.sp)) }
                    val (bg, ink, icon) = when (st) { 2 -> Triple(Kx.accentDeep, Kx.onAccent, Icons.Rounded.Check); 1 -> Triple(Kx.warning, Kx.bg, Icons.Rounded.Schedule); else -> Triple(Color(0xFF5C6661), Kx.bg, Icons.Rounded.Lock) }
                    Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(28.dp).clip(CircleShape).background(SheetInk).padding(3.dp).clip(CircleShape).background(bg),
                        contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(16.dp), tint = ink) }
                }
                Text("Настолки по пятницам", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                Text("Частная группа · 214 участников", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        listOf("А" to Color(0xFFC0443C), "Д" to Color(0xFF2F6E9E), "Е" to Kx.accentDeep).forEachIndexed { i, (l, c) ->
                            Box(Modifier.offset(x = (-8 * i).dp).size(26.dp).clip(CircleShape).background(SheetInk).padding(2.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
                                Text(l, style = KxType.style(11.sp, FontWeight.ExtraBold))
                            }
                        }
                    }
                    Text("Анна, Дана и Ерлан уже здесь", style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.offset(x = (-16).dp))
                }
                when (st) {
                    0 -> {
                        Text("Собираемся по пятницам в 19:00. Играем в «Каркассон», «Кодовые имена» и всё новое. Новичкам объясняем правила 🙂",
                            style = KxType.style(13.sp, color = Kx.text2, lineHeight = 18.9.sp), modifier = Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                .background(Kx.w(.05f)).padding(horizontal = 14.dp, vertical = 12.dp))
                        Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x14E8B04A)).border(1.dp, Color(0x38E8B04A), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.HowToReg, null, Modifier.size(18.dp), tint = Kx.warning)
                            Text("Вступление по заявке — админы увидят ваш профиль и решат.", style = KxType.style(12.sp, color = Color(0xFFE8D2A0), lineHeight = 16.8.sp))
                        }
                        GxPillButton("Подать заявку", Modifier.padding(top = 12.dp).fillMaxWidth(), height = 54.dp, brush = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)),
                            ink = Kx.onAccent, size = 16, weight = FontWeight.ExtraBold) { st = 1 }
                    }
                    1 -> Column(Modifier.padding(top = 18.dp).gxRise(0, 300, 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val inf = rememberInfiniteTransition(label = "wait")
                        val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing)), label = "ring")
                        val hour by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "hour")
                        val dots by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "dots")
                        Box(Modifier.size(64.dp).drawBehind {
                            if (!reduced) {
                                val s = .8f + .8f * ring
                                drawCircle(Color(0x99E8B04A).copy(alpha = .6f * (1f - ring)), size.minDimension / 2 * s - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                            }
                        }.clip(CircleShape).background(Color(0x24E8B04A)), contentAlignment = Alignment.Center) {
                            val rot = if (reduced) 0f else hourRotation(hour)
                            Icon(Icons.Rounded.HourglassTop, null, Modifier.size(32.dp).graphicsLayer { rotationZ = rot }, tint = Kx.warning)
                        }
                        Row(Modifier.padding(top = 10.dp)) {
                            Text("Заявка отправлена", style = KxType.style(17.sp, FontWeight.ExtraBold))
                            val a = if (reduced) 1f else .3f + .7f * (1f - kotlin.math.abs(dots * 2f - 1f))
                            Text("…", style = KxType.style(17.sp, FontWeight.ExtraBold).copy(color = Kx.text.copy(alpha = a)))
                        }
                        Text("Пришлём уведомление, когда админы ответят. Группа появится в списке чатов сама.", style = KxType.style(13.sp, color = Kx.text3, lineHeight = 18.2.sp),
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GxPillButton("Закрыть", Modifier.weight(1f), height = 50.dp, size = 14, onClick = ctx.back)
                            Box(Modifier.weight(1f).drawBehind {
                                val o = 3.dp.toPx()
                                drawRoundRect(Color(0x808FD3AC), topLeft = androidx.compose.ui.geometry.Offset(-o, -o), size = androidx.compose.ui.geometry.Size(size.width + 2 * o, size.height + 2 * o),
                                    cornerRadius = CornerRadius(25.dp.toPx() + o), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
                            }) {
                                GxPillButton("Макет: админ одобрил", Modifier.fillMaxWidth(), height = 50.dp, bg = Color(0x248FD3AC), ink = Kx.accent, size = 13, weight = FontWeight.ExtraBold) { st = 2 }
                            }
                        }
                    }
                    else -> Column(Modifier.padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.gxPopIn(500, .5f).size(64.dp).clip(CircleShape).cssLinear(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Celebration, null, Modifier.size(34.dp), tint = Kx.onAccent)
                        }
                        Text("Вас приняли!", style = KxType.style(17.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                        Text("Добро пожаловать в «Настолки по пятницам»", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                        GxPillButton("Открыть чат", Modifier.padding(top = 14.dp).fillMaxWidth(), height = 54.dp, brush = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)),
                            ink = Kx.onAccent, size = 16, weight = FontWeight.ExtraBold, onClick = ctx.back)
                    }
                }
            }
        }
    }
}

/** kxHour: hold 0–40%, flip to 180 by 50%, hold to 90%, flip to 360. */
private fun hourRotation(t: Float): Float = when {
    t < .4f -> 0f
    t < .5f -> 180f * ((t - .4f) / .1f)
    t < .9f -> 180f
    else -> 180f + 180f * ((t - .9f) / .1f)
}
