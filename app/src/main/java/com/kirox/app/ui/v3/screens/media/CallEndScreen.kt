package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private val RateHints = listOf("Нажмите на звезду", "Очень плохо", "Плохо", "Нормально", "Хорошо", "Отлично!")
private val CallIssues = listOf("Эхо" to Icons.Outlined.GraphicEq, "Шум" to Icons.Outlined.NoiseAware, "Прерывалось" to Icons.Outlined.SignalCellularConnectedNoInternet0Bar,
    "Меня не слышали" to Icons.Outlined.MicOff, "Не слышно собеседника" to Icons.Outlined.HearingDisabled, "Задержка" to Icons.Outlined.Timer)

/** Board «Звонок завершён»: summary, call-back actions, star rating with issues, thanks. */
@Composable
fun CallEndScreen(ctx: KxScreenCtx) {
    // demo: needs store (last call summary + rateCall(rating, issues, comment, attachLog))
    var rate by remember { mutableIntStateOf(0) }
    var pop by remember { mutableIntStateOf(0) }
    val issues = remember { mutableStateListOf<String>() }
    var note by remember { mutableStateOf("") }
    var log by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }
    var hit by remember { mutableStateOf("") }
    val low = rate in 1..3

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        DriftGlow(Kx.accentDeep.copy(alpha = .4f), 320.dp, Modifier.offset((-60).dp, (-40).dp), 9000, 30f, -20f, 1.15f)
        DriftGlow(Color(47, 110, 158).copy(alpha = .3f), 260.dp, Modifier.align(Alignment.TopEnd).offset(80.dp, 180.dp), 11000, 30f, -20f, 1.15f, startOffsetMs = 4000)
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) { MRoundBtn(Icons.Outlined.Close, "Закрыть", highlight = null) { ctx.back() } }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 30.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.padding(top = 4.dp).size(108.dp)) {
                        CallRings(Modifier.matchParentSize(), color = Kx.accent.copy(alpha = .5f), periodMs = 2400, fromAlpha = .5f)
                        Box(Modifier.matchParentSize().clip(CircleShape).cssGrad(135f, Color(0xFFC0443C), Color(0xFF6A4C9C)), contentAlignment = Alignment.Center) { T("А", 40.sp, FontWeight.ExtraBold) }
                    }
                    T("Анна", 24.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp))
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.CallEnd, null, Modifier.size(18.dp), tint = Kx.danger)
                        T("Звонок завершён · 12:47", 14.sp, color = Kx.text2)
                    }
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(Icons.Outlined.Lock, "Защищён", Kx.accent); InfoChip(Icons.Outlined.SwapVert, "14,2 МБ", Kx.text2); InfoChip(Icons.Outlined.Wifi, "Wi-Fi", Kx.text2)
                    }
                }
                Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EndAct(Icons.Rounded.Call, if (hit == "call") "Звоним…" else "Перезвонить", true, Modifier.weight(1f)) { hit = "call"; ctx.open("VideoCall") }
                    EndAct(Icons.Rounded.Videocam, if (hit == "video") "Звоним…" else "Видео", false, Modifier.weight(1f)) { hit = "video"; ctx.open("VideoCall") }
                    EndAct(Icons.AutoMirrored.Rounded.Chat, "Написать", false, Modifier.weight(1f)) { hit = "chat"; ctx.back() }
                }
                Column(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.surface.copy(alpha = .5f)).background(Brush.verticalGradient(listOf(Kx.w(.12f), Kx.w(.04f))))
                    .border(1.dp, Kx.w(.14f), RoundedCornerShape(26.dp)).padding(horizontal = 16.dp, vertical = 18.dp)) {
                    if (!done) {
                        T("Как было слышно?", 16.sp, FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth().wrapContentWidth())
                        T(RateHints[rate], 13.sp, color = Kx.text3, modifier = Modifier.padding(top = 4.dp).fillMaxWidth().wrapContentWidth())
                        Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                            (1..5).forEach { n -> Star(n, rate, pop) { rate = n; pop++ } }
                        }
                        if (low) Column(Modifier.padding(top = 14.dp).kxRise(0, 250)) {
                            T("Что пошло не так?", 13.sp, FontWeight.Bold, Kx.accent)
                            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                CallIssues.forEach { (label, icon) ->
                                    val on = label in issues
                                    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Color(0xFFE5534B).copy(alpha = .16f) else Kx.w(.05f))
                                        .border(1.dp, if (on) Color(0xFFE5534B).copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(17.dp))
                                        .kxClick { if (on) issues.remove(label) else issues.add(label) }.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(icon, null, Modifier.size(16.dp), tint = if (on) Kx.danger else Kx.text2)
                                        T(label, 13.sp, FontWeight.SemiBold, if (on) Kx.danger else Kx.text2)
                                    }
                                }
                            }
                            Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(70.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .25f))
                                .border(1.dp, Kx.w(.1f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                                if (note.isEmpty()) T("Расскажите подробнее (необязательно)", 14.sp, color = Kx.text4)
                                BasicTextField(note, { note = it }, Modifier.fillMaxSize(), textStyle = ts(14.sp), cursorBrush = SolidColor(Kx.accent))
                            }
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(if (log) Kx.accent else Color.Transparent)
                                    .border(1.5.dp, if (log) Kx.accent else Kx.w(.3f), RoundedCornerShape(7.dp)).kxClick("Приложить технический отчёт") { log = !log }, contentAlignment = Alignment.Center) {
                                    if (log) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Kx.onAccent)
                                }
                                T("Приложить технический отчёт о звонке", 12.sp, color = Kx.text3)
                            }
                        }
                        if (rate > 0) Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(50.dp).kxRise(0, 200).clip(RoundedCornerShape(25.dp))
                            .cssGrad(135f, Kx.accentMid, Kx.accentDeep).kxClick { done = true }, contentAlignment = Alignment.Center) {
                            T("Отправить оценку", 15.sp, FontWeight.ExtraBold, Kx.onAccent)
                        }
                    } else Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val t = remember { Animatable(0f) }
                        LaunchedEffect(Unit) { t.animateTo(1f, tween(450, easing = Ease)) }
                        val sc = if (t.value < .6f) .6f + .5f * t.value / .6f else 1.1f - .1f * (t.value - .6f) / .4f
                        Box(Modifier.size(60.dp).graphicsLayer { scaleX = sc; scaleY = sc; alpha = (t.value / .6f).coerceAtMost(1f) }.clip(CircleShape).background(Kx.accent.copy(alpha = .16f)),
                            contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Favorite, null, Modifier.size(34.dp), tint = Kx.accent) }
                        T("Спасибо за оценку", 16.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
                        val picked = CallIssues.map { it.first }.filter { it in issues }
                        androidx.compose.material3.Text(if (low) (if (picked.isNotEmpty()) "Разберёмся: ${picked.joinToString(", ").lowercase()}." else "Постараемся, чтобы в следующий раз было лучше.")
                            else "Рады, что всё было хорошо слышно.", style = ts(13.sp, color = Kx.text3), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                        T("Изменить оценку", 13.sp, FontWeight.Bold, Kx.accent, modifier = Modifier.padding(top = 10.dp).kxClick { done = false })
                    }
                }
                T("Не спрашивать после звонков", 13.sp, color = Kx.text4, modifier = Modifier.padding(top = 14.dp).align(Alignment.CenterHorizontally).kxClick { ctx.back() })
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String, iconTint: Color) {
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Kx.w(.06f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(16.dp), tint = iconTint)
        T(label, 12.sp, color = Kx.text2)
    }
}

@Composable
private fun EndAct(icon: ImageVector, label: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ink = if (primary) Kx.onAccent else Kx.text
    Column(modifier.height(76.dp).clip(RoundedCornerShape(22.dp)).then(if (primary) Modifier.cssGrad(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.06f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(22.dp)))
        .kxClick(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
        Icon(icon, null, tint = ink)
        T(label, 13.sp, FontWeight.Bold, ink)
    }
}

/** Star: filled up to the rating, color by rating, kxStar .35s pop with 40ms stagger on each tap. */
@Composable
private fun Star(n: Int, rate: Int, pop: Int, onClick: () -> Unit) {
    val on = n <= rate
    val ink = if (on) (if (rate <= 2) Kx.danger else if (rate == 3) Kx.warning else Kx.accent) else Kx.w(.22f)
    val a = remember { Animatable(1f) }
    LaunchedEffect(pop) {
        if (pop > 0 && on) { kotlinx.coroutines.delay((n - 1) * 40L); a.snapTo(0f); a.animateTo(1f, tween(350, easing = Ease)) }
    }
    val v = a.value
    val sc = if (v < .45f) 1f + .35f * v / .45f else 1.35f - .35f * (v - .45f) / .55f
    val rot = if (v < .45f) -8f * v / .45f else -8f + 8f * (v - .45f) / .55f
    Box(Modifier.size(50.dp).clip(CircleShape).kxClick("$n из 5", onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(if (on) Icons.Rounded.Star else Icons.Outlined.StarOutline, null, Modifier.size(40.dp).graphicsLayer { scaleX = sc; scaleY = sc; rotationZ = rot }, tint = ink)
    }
}
