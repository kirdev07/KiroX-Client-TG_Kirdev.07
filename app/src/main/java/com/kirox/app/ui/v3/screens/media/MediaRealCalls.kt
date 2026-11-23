package com.kirox.app.ui.v3.screens.media

import com.kirox.app.ui.v3.model.kxDuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxCallPhase
import com.kirox.app.ui.v3.model.KxCallState
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * wave3-media: call boards on the real account, driven by the app's call service (TelegramVoipRepository /
 * TelegramGroupCallRepository through store.activeCall / groupCall / lastCall). The demo boards keep their
 * simulated calls for KxDemoStore. wave6: live calls are drawn by KxCallHost (MediaCallHost.kt).
 */

/** Which board shows the live call. */
internal enum class CallBoard { Video, Incoming, Key, Weak, Waiting, Pip }

@Composable
private fun NoCall(ctx: KxScreenCtx, text: String = "Сейчас нет звонка", sub: String = "Экран оживёт во время звонка") {
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().padding(16.dp)) {
        MRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", bg = Kx.w(.08f), border = null) { ctx.back() }
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.PhoneDisabled, null, Modifier.size(48.dp), tint = Kx.text4)
            T(text, 18.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
            Text(sub, style = ts(13.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            ctx.store.lastCall?.let { last ->
                Box(Modifier.padding(top = 18.dp).height(42.dp).clip(RoundedCornerShape(21.dp)).background(Kx.w(.08f)).kxClick { ctx.open("CallEnd") }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center) { T("Последний звонок: ${last.name}", 13.sp, FontWeight.Bold) }
            }
        }
    }
}

/** Blank stage while a board hands the live call over to the call host (one frame). */
@Composable
private fun HandOver(ctx: KxScreenCtx, action: () -> Unit) {
    LaunchedEffect(Unit) { action(); ctx.back() }
    Box(Modifier.fillMaxSize().background(Kx.bg))
}

/**
 * VideoCall / Incoming / KeyCheck / CallWeak / CallWaiting / Pip on the real account. wave6 (port-calls): the live
 * call is drawn by KxCallHost over the whole app (ringtone, audio route, proximity, lock screen, minimise chip…);
 * these boards only bring it back full screen (Pip shrinks it to the chip / video window) and close themselves.
 */
@Composable
internal fun RealCallBoard(ctx: KxScreenCtx, board: CallBoard) {
    if (ctx.store.activeCall != null) {
        HandOver(ctx) {
            if (board == CallBoard.Key) KxCallUi.keyOpen = true
            if (board == CallBoard.Pip) KxCallUi.minimized = true else KxCallUi.minimized = false
        }
        return
    }
    when (board) {
        CallBoard.Weak -> NoCall(ctx, sub = "Подсказки о слабой связи появятся во время звонка")
        CallBoard.Key -> NoCall(ctx, sub = "Эмодзи-ключ шифрования появляется во время звонка")
        CallBoard.Incoming -> NoCall(ctx, "Нет входящего звонка", "Входящий звонок откроется сам поверх приложения")
        CallBoard.Waiting -> NoCall(ctx, sub = "Во время разговора Telegram отвечает другим звонящим «занято» — удержание и второй звонок недоступны.")
        else -> NoCall(ctx)
    }
}

/** GroupCall / VideoConf on the real account: brings the video chat (drawn by KxCallHost) back full screen. */
@Composable
internal fun RealGroupCall(ctx: KxScreenCtx) {
    if (ctx.store.groupCall != null) { HandOver(ctx) { KxCallUi.groupMinimized = false }; return }
    NoCall(ctx, "Вы не в видеочате", "Зайдите в видеочат группы или канала — участники появятся здесь")
}

@Composable
private fun EndBtn(icon: ImageVector, label: String, accent: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(if (accent) Kx.accent else Kx.w(.12f)).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(28.dp), tint = if (accent) Kx.onAccent else Color.White)
        }
        T(label, 11.sp, color = Kx.text2)
    }
}

/** CallEnd on the real account: the last finished call and Telegram's call rating. */
@Composable
internal fun RealCallEnd(ctx: KxScreenCtx) {
    val c = ctx.store.lastCall
    if (c == null) { NoCall(ctx, "Звонков пока не было", "Итоги появятся после завершённого звонка"); return }
    val scope = rememberCoroutineScope()
    var stars by remember { mutableIntStateOf(0) }
    val issues = remember { mutableStateListOf<String>() }
    var comment by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    val opts = listOf("echo" to "Эхо", "noise" to "Шум", "interruptions" to "Прерывания", "distorted" to "Искажённый звук",
        "silentRemote" to "Не слышно собеседника", "silentLocal" to "Меня не слышно", "dropped" to "Звонок оборвался") + if (c.video) listOf("video" to "Плохое видео") else emptyList()
    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        MRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", bg = Kx.w(.08f), border = null) { ctx.back() }
        Box(Modifier.padding(top = 20.dp).size(96.dp).align(Alignment.CenterHorizontally).clip(CircleShape).background(Kx.w(.1f)), contentAlignment = Alignment.Center) {
            T(c.name.take(1).uppercase(), 36.sp, FontWeight.ExtraBold); KxImage(c.avatar, Modifier.matchParentSize(), maxPx = 300)
        }
        T(c.name, 24.sp, FontWeight.ExtraBold, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp))
        T((if (c.phase == KxCallPhase.Failed) c.failure.ifBlank { "Не удалось позвонить" } else c.endReason.ifBlank { "Звонок завершён" }) +
            (if (c.endedSeconds > 0) " · длительность ${kxDuration(c.endedSeconds)}" else ""), 14.sp, color = Kx.text3, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EndBtn(Icons.Rounded.Call, "Позвонить", accent = true) { ctx.store.call(c.peerId, false) }
            EndBtn(Icons.Rounded.Videocam, "Видеозвонок") { ctx.store.call(c.peerId, true) }
            EndBtn(Icons.Rounded.ChatBubble, "Написать") { ctx.openChat(c.peerId) }
        }
        if (c.endedSeconds > 0 && !sent) Column(Modifier.padding(top = 24.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Kx.w(.05f)).padding(16.dp)) {
            T("Как прошёл звонок?", 16.sp, FontWeight.ExtraBold)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { i -> Icon(if (i <= stars) Icons.Rounded.Star else Icons.Rounded.StarBorder, "$i", Modifier.size(36.dp).kxClick("Оценка $i") { stars = i }, tint = if (i <= stars) Kx.premium else Kx.text4) }
            }
            if (stars in 1..4) {
                FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    opts.forEach { (k, label) ->
                        val on = k in issues
                        Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.06f)).kxClick { if (on) issues.remove(k) else issues.add(k) }
                            .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { T(label, 12.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2) }
                    }
                }
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(12.dp)) {
                    if (comment.isEmpty()) T("Что было не так? (необязательно)", 14.sp, color = Kx.text4)
                    BasicTextField(comment, { comment = it }, Modifier.fillMaxWidth(), textStyle = ts(14.sp), cursorBrush = SolidColor(Kx.accent))
                }
            }
            if (stars > 0) Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.accent).kxClick {
                scope.launch { val err = ctx.store.rateCall(c.id, stars, comment, if (stars < 5) issues.toList() else emptyList()); if (err == null) sent = true else ctx.toast(err) }
            }, contentAlignment = Alignment.Center) { T("Отправить оценку", 14.sp, FontWeight.ExtraBold, Kx.onAccent) }
        }
        if (sent) T("Спасибо! Оценка отправлена в Telegram.", 14.sp, color = Kx.accent, modifier = Modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally))
    }
}

/** PlanCall on the real account: schedule a video chat (CreateVideoChat with a start date) in chat ctx.arg, or start it now. */
@Composable
internal fun RealPlanCall(ctx: KxScreenCtx) {
    val chatId = ctx.arg?.toLongOrNull()
    if (chatId == null) { NoCall(ctx, "Выберите группу или канал", "Запланировать видеочат можно из профиля группы или канала"); return }
    val scope = rememberCoroutineScope()
    val header = ctx.store.header(chatId)
    var title by remember { mutableStateOf("") }
    var day by remember { mutableIntStateOf(0) }
    var hour by remember { mutableIntStateOf(((java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) + 1) % 24)) }
    var rtmp by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<String?>(null) }
    fun startAt(): Int {
        val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, day); set(java.util.Calendar.HOUR_OF_DAY, hour); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) }
        return (cal.timeInMillis / 1000).toInt()
    }
    val days = listOf("Сегодня", "Завтра") + (2..6).map { d -> java.text.SimpleDateFormat("EE d", java.util.Locale.forLanguageTag("ru")).format(java.util.Date(System.currentTimeMillis() + d * 86_400_000L)) }
    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", bg = Kx.w(.08f), border = null) { ctx.back() }
            Column(Modifier.weight(1f)) { T("Запланировать звонок", 20.sp, FontWeight.ExtraBold); T(header.title, 12.sp, color = Kx.text3, maxLines = 1) }
        }
        done?.let { T(it, 16.sp, FontWeight.Bold, Kx.accent, modifier = Modifier.padding(top = 30.dp)) }
        if (done == null) PlanForm(title, { title = it }, days, day, { day = it }, hour, { hour = it }, rtmp, { rtmp = it }, busy, startAt() <= System.currentTimeMillis() / 1000 + 60,
            onPlan = {
                busy = true
                scope.launch { val err = ctx.store.planVideoChat(chatId, title, startAt(), rtmp); busy = false; if (err == null) done = "Звонок запланирован: ${days[day]}, %02d:00".format(hour) else ctx.toast(err) }
            },
            onNow = { ctx.store.joinGroupCall(chatId); ctx.back() })
    }
}

@Composable
private fun PlanForm(title: String, setTitle: (String) -> Unit, days: List<String>, day: Int, setDay: (Int) -> Unit, hour: Int, setHour: (Int) -> Unit,
                     rtmp: Boolean, setRtmp: (Boolean) -> Unit, busy: Boolean, past: Boolean, onPlan: () -> Unit, onNow: () -> Unit) {
    Column {
        Box(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).padding(14.dp)) {
            if (title.isEmpty()) T("Название (необязательно)", 15.sp, color = Kx.text4)
            BasicTextField(title, { setTitle(it.take(64)) }, Modifier.fillMaxWidth(), singleLine = true, textStyle = ts(15.sp), cursorBrush = SolidColor(Kx.accent))
        }
        T("День", 13.sp, FontWeight.Bold, Kx.text3, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { i, d -> Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (day == i) Kx.accent else Kx.w(.06f)).kxClick { setDay(i) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center) { T(d, 13.sp, FontWeight.Bold, if (day == i) Kx.onAccent else Kx.text2) } }
        }
        T("Время", 13.sp, FontWeight.Bold, Kx.text3, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..23).forEach { h -> Box(Modifier.size(54.dp, 34.dp).clip(RoundedCornerShape(17.dp)).background(if (hour == h) Kx.accent else Kx.w(.06f)).kxClick { setHour(h) },
                contentAlignment = Alignment.Center) { T("%02d:00".format(h), 12.sp, FontWeight.Bold, if (hour == h) Kx.onAccent else Kx.text2) } }
        }
        Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick { setRtmp(!rtmp) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.LiveTv, null, tint = Kx.text3)
            Column(Modifier.weight(1f)) { T("Трансляция (RTMP)", 14.sp, FontWeight.Bold); T("Эфир из OBS и других программ", 12.sp, color = Kx.text3) }
            Icon(if (rtmp) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null, tint = if (rtmp) Kx.accent else Kx.text4)
        }
        Box(Modifier.padding(top = 20.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(if (past) Kx.w(.08f) else Kx.accent).kxClick {
            if (!past && !busy) onPlan()
        }, contentAlignment = Alignment.Center) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Kx.onAccent, strokeWidth = 2.dp)
            else T(if (past) "Это время уже прошло" else "Запланировать", 15.sp, FontWeight.ExtraBold, if (past) Kx.text4 else Kx.onAccent)
        }
        Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.08f)).kxClick { onNow() }, contentAlignment = Alignment.Center) { T("Начать сейчас", 14.sp, FontWeight.Bold) }
    }
}
