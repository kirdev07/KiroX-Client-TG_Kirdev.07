package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ScheduleSend
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.KxButton
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxChatHeader
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType

/**
 * qa-real: long-press on «Отправить» in the chat (and attach «Позже»): send silently, schedule with a real date/time
 * picker, or «Когда будет в сети» (private chats). The typed text stays in the composer until something is sent.
 */
@Composable
internal fun ChatSendOptionsSheet(
    h: KxChatHeader, text: String, onDismiss: () -> Unit,
    onSilent: () -> Unit, onSchedule: (at: Int, label: String) -> Unit, onWhenOnline: () -> Unit,
    startWithPicker: Boolean = false,
) {
    var picker by remember { mutableStateOf(startWithPicker) }
    androidx.activity.compose.BackHandler { if (picker && !startWithPicker) picker = false else onDismiss() }
    val days = remember { com.kirox.app.ui.v3.screens.chat.realDays() }
    val now = remember { java.util.Calendar.getInstance() }
    var day by remember { mutableIntStateOf(0) }
    // Default: the next full hour (today), so the first «Отправить» is always in the future.
    var hh by remember { mutableIntStateOf((now.get(java.util.Calendar.HOUR_OF_DAY) + 1) % 24) }
    var mm by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { if (now.get(java.util.Calendar.HOUR_OF_DAY) == 23) day = 1 }
    var err by remember { mutableStateOf<String?>(null) }
    val y = remember { Animatable(1f) }
    LaunchedEffect(Unit) { y.animateTo(0f, tween(300, easing = KxMotion.rise)) }
    fun unixAt(): Int = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_MONTH, day); set(java.util.Calendar.HOUR_OF_DAY, hh); set(java.util.Calendar.MINUTE, mm); set(java.util.Calendar.SECOND, 0)
    }.timeInMillis.div(1000).toInt()
    fun p2(n: Int) = n.toString().padStart(2, '0')
    val whenLabel = (if (day == 0) "сегодня" else if (day == 1) "завтра" else days[day].lowercase()) + " в $hh:${p2(mm)}"

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x99050706)).kxClick("Закрыть", onClick = onDismiss))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = y.value * size.height }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E2421), Color(0xFF121614))))
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 22.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
            if (text.isNotBlank()) Text(text.trim(), style = KxType.style(14.sp, color = Kx.text2), maxLines = 2,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.05f)).padding(horizontal = 12.dp, vertical = 10.dp))
            if (!picker) {
                Column(Modifier.padding(top = 10.dp)) {
                    SoRow(Icons.Outlined.NotificationsOff, "Без звука", "Уведомление придёт тихо", Kx.text, onSilent)
                    SoRow(Icons.AutoMirrored.Outlined.ScheduleSend, "Отложить", "Выбрать дату и время", Kx.accent) { picker = true }
                    // features: Telegram sends «when online» only while the last seen is visible (not «недавно»).
                    if (h.kind == KxChatKind.Private && com.kirox.app.ui.v3.model.KxSendWhen.lastSeenVisible(h.status)) SoRow(Icons.Outlined.HowToReg, "Когда будет в сети", h.status.ifBlank { h.title }, Kx.text, onWhenOnline)
                }
            } else {
                Text("Когда отправить?", style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    days.forEachIndexed { i, d ->
                        val on = day == i
                        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f))
                            .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Color.Transparent, RoundedCornerShape(18.dp))
                            .kxClick(d) { day = i; err = null }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            Text(d, style = KxType.style(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text))
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoWheel(Modifier.weight(1f), "Час", hh, 24, { p2(it) }) { hh = it; err = null }
                    SoWheel(Modifier.weight(1f), "Минуты", mm / 5, 12, { p2(it * 5) }) { mm = it * 5; err = null }
                }
                err?.let { Text(it, style = KxType.style(13.sp, color = Kx.danger), modifier = Modifier.padding(top = 8.dp)) }
                KxButton("Отправить $whenLabel", Modifier.padding(top = 14.dp).fillMaxWidth(), height = 54.dp) {
                    val at = unixAt()
                    if (at <= System.currentTimeMillis() / 1000 + 30) err = "Выберите время в будущем" else onSchedule(at, whenLabel)
                }
            }
        }
    }
}

@Composable
private fun SoRow(icon: ImageVector, label: String, sub: String, ink: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).kxClick(label, onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = ink)
        Column(Modifier.weight(1f)) {
            Text(label, style = KxType.style(15.sp, FontWeight.SemiBold, ink))
            Text(sub, style = KxType.style(12.sp, color = Kx.text4), maxLines = 1)
        }
    }
}

@Composable
private fun SoWheel(modifier: Modifier, title: String, value: Int, len: Int, fmt: (Int) -> String, set: (Int) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f)).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = KxType.style(11.sp, color = Kx.text4), modifier = Modifier.padding(top = 4.dp))
        Box(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(12.dp)).kxClick("$title: раньше") { set((value - 1 + len) % len) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ExpandLess, null, tint = Kx.text3)
        }
        Text(fmt(value), style = KxType.style(24.sp, FontWeight.ExtraBold, Kx.accent).copy(fontFeatureSettings = "tnum"), modifier = Modifier.padding(vertical = 2.dp))
        Box(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(12.dp)).kxClick("$title: позже") { set((value + 1) % len) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ExpandMore, null, tint = Kx.text3)
        }
    }
}
