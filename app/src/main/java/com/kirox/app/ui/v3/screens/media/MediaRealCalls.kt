package com.kirox.app.ui.v3.screens.media

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
 * simulated calls for KxDemoStore.
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

/** Seconds since the call connected, ticking. */
@Composable
private fun callSeconds(c: KxCallState): Int {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(c.sinceMs) { while (c.sinceMs > 0) { now = System.currentTimeMillis(); delay(1000) } }
    return if (c.sinceMs > 0) ((now - c.sinceMs) / 1000).toInt().coerceAtLeast(0) else 0
}

private fun phaseText(c: KxCallState, sec: Int) = when (c.phase) {
    KxCallPhase.Calling -> if (c.video) "Видеозвонок…" else "Вызов…"
    KxCallPhase.Ringing -> "Звонит…"
    KxCallPhase.Incoming -> if (c.video) "Входящий видеозвонок" else "Входящий звонок"
    KxCallPhase.Connecting -> "Соединение…"
    KxCallPhase.Active -> if (c.reconnecting) "Переподключаемся…" else fmtClock(sec)
    KxCallPhase.Ended -> c.endReason.ifBlank { "Звонок завершён" }
    KxCallPhase.Failed -> c.failure.ifBlank { "Не удалось позвонить" }
}

/** Remote camera full screen + own camera in the corner (WebRTC renderers fed by the call engine). */
@Composable
private fun CallVideoLayer(ctx: KxScreenCtx, c: KxCallState) {
    val context = LocalContext.current
    val remote = remember { org.webrtc.TextureViewRenderer(context) }
    val local = remember { org.webrtc.TextureViewRenderer(context) }
    DisposableEffect(Unit) {
        val egl = com.kirox.app.voip.VideoCapturerDevice.getSharedEGLContext()
        remote.init(egl, null); local.init(egl, null)
        remote.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        local.setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        local.setMirror(true)
        ctx.store.attachCallVideo(remote, local)
        onDispose { ctx.store.attachCallVideo(null, null); remote.release(); local.release() }
    }
    Box(Modifier.fillMaxSize()) {
        if (c.remoteVideo) AndroidView({ remote }, Modifier.fillMaxSize())
        if (c.cameraOn) Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 74.dp, end = 16.dp).size(104.dp, 150.dp).clip(RoundedCornerShape(22.dp))
            .border(1.dp, Kx.w(.22f), RoundedCornerShape(22.dp))) { AndroidView({ local }, Modifier.fillMaxSize()) }
    }
}

@Composable
private fun CallBtn(icon: ImageVector, label: String, on: Boolean = false, danger: Boolean = false, accent: Boolean = false, size: androidx.compose.ui.unit.Dp = 60.dp, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(size).clip(CircleShape).background(when { danger -> Color(0xFFE5534B); accent -> Kx.accent; on -> Color.White; else -> Kx.w(.12f) })
            .kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(28.dp), tint = when { danger -> Color.White; accent -> Kx.onAccent; on -> Kx.bg; else -> Color.White })
        }
        T(label, 11.sp, color = Kx.text2)
    }
}

@Composable
private fun SignalBars(bars: Int) {
    Row(Modifier.height(14.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..4).forEach { i ->
            val c = if (i <= bars) (if (bars >= 3) Kx.accent else if (bars == 2) Kx.warning else Kx.danger) else Kx.w(.25f)
            Box(Modifier.width(3.dp).height((3 * i + 2).dp).clip(RoundedCornerShape(1.dp)).background(c))
        }
    }
}

/** VideoCall / Incoming / KeyCheck / CallWeak / CallWaiting / Pip on the real account. */
@Composable
internal fun RealCallBoard(ctx: KxScreenCtx, board: CallBoard) {
    val c = ctx.store.activeCall
    // The call ended while this board was showing it → close the board (the CallEnd board offers the rating).
    var hadCall by remember { mutableStateOf(false) }
    if (c != null) hadCall = true
    LaunchedEffect(c == null, hadCall) { if (c == null && hadCall) { ctx.back(); if (ctx.store.lastCall != null) ctx.open("CallEnd") } }
    // Just after startCall (permission dialog, call not created yet): «Соединение…» for a few seconds instead of «нет звонка».
    var grace by remember { mutableStateOf(board == CallBoard.Video) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(4000); grace = false }
    if (c == null && (hadCall || grace)) {
        Box(Modifier.fillMaxSize().background(Kx.bg), contentAlignment = Alignment.Center) { T(if (hadCall) "Звонок завершён" else "Соединение…", 16.sp, color = Kx.text3) }
        return
    }
    if (c == null) {
        when (board) {
            CallBoard.Weak -> NoCall(ctx, sub = "Подсказки о слабой связи появятся во время звонка")
            CallBoard.Key -> NoCall(ctx, sub = "Эмодзи-ключ шифрования появляется во время звонка")
            CallBoard.Incoming -> NoCall(ctx, "Нет входящего звонка", "Входящий звонок откроется здесь сам")
            else -> NoCall(ctx)
        }
        return
    }
    val sec = callSeconds(c)
    var keyOpen by remember { mutableStateOf(board == CallBoard.Key) }
    val live = c.phase == KxCallPhase.Active
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        if (c.video || c.cameraOn || c.remoteVideo) CallVideoLayer(ctx, c)
        if (!c.remoteVideo) Box(Modifier.fillMaxSize().ellipseGlow(Kx.accentDeep.copy(alpha = .28f), .5f, .35f, .8f, .5f, .7f))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Rounded.KeyboardArrowDown, "Свернуть", size = 42.dp, bg = Kx.w(.1f), border = Kx.w(.14f)) { ctx.back() }
                Spacer(Modifier.weight(1f))
                if (live) SignalBars(c.signalBars)
                if (c.emojis.isNotEmpty()) Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.1f)).border(1.dp, Kx.w(.14f), RoundedCornerShape(19.dp))
                    .kxClick("Ключ шифрования") { keyOpen = !keyOpen }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text(c.emojis.joinToString(""), style = ts(16.sp).copy(letterSpacing = 2.sp))
                }
            }
            if (board == CallBoard.Weak || c.reconnecting || (live && c.signalBars <= 1)) {
                val (title, sub) = when {
                    c.reconnecting -> "Переподключаемся…" to "Звонок продолжится сам, как только вернётся сеть"
                    live && c.signalBars <= 1 -> "Слабая связь" to "Видео может подтормаживать"
                    else -> "Связь в порядке" to "Сигнал: ${c.signalBars} из 4"
                }
                Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xB83C2C0E)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (c.reconnecting) Icons.Rounded.Sync else Icons.Outlined.SignalCellularAlt, null, tint = Kx.warning)
                    Column(Modifier.weight(1f)) { T(title, 14.sp, FontWeight.Bold); T(sub, 12.sp, color = Kx.text3) }
                    if (c.cameraOn && live) Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.12f)).kxClick { ctx.store.turnCallCamera(false) }.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center) { T("Только голос", 12.sp, FontWeight.Bold) }
                }
            }
            if (board == CallBoard.Waiting) Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Info, null, tint = Kx.text3)
                T("Во время разговора Telegram отвечает другим звонящим «занято» — удержание и второй звонок недоступны.", 12.sp, color = Kx.text3)
            }
            if (keyOpen && c.emojis.isNotEmpty()) Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.07f)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(c.emojis.joinToString("  "), style = ts(40.sp))
                T("Если у ${c.name} те же эмодзи, звонок защищён сквозным шифрованием.", 13.sp, color = Kx.text2, modifier = Modifier.padding(top = 10.dp))
            }
            Spacer(Modifier.weight(1f))
            if (!c.remoteVideo) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(132.dp)) {
                    if (!live) CallRings(Modifier.matchParentSize())
                    Box(Modifier.matchParentSize().clip(CircleShape).background(Kx.w(.1f)), contentAlignment = Alignment.Center) {
                        T(c.name.take(1).uppercase(), 48.sp, FontWeight.ExtraBold)
                        KxImage(c.avatar, Modifier.matchParentSize(), maxPx = 400)
                    }
                }
            }
            T(c.name, 28.sp, FontWeight.ExtraBold, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 18.dp), maxLines = 1)
            Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (live && !c.remoteMuted) SpeakBars()
                if (c.remoteMuted) Icon(Icons.Rounded.MicOff, null, Modifier.size(16.dp), tint = Kx.danger)
                T(phaseText(c, sec), 14.sp, color = Kx.text3)
            }
            Spacer(Modifier.weight(1f))
            if (c.phase == KxCallPhase.Incoming) Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallBtn(Icons.Rounded.CallEnd, "Отклонить", danger = true, size = 72.dp) { ctx.store.hangUp() }
                CallBtn(if (c.video) Icons.Rounded.Videocam else Icons.Rounded.Call, "Ответить", accent = true, size = 72.dp) { ctx.store.answerCall() }
            } else Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallBtn(if (c.cameraOn) Icons.Rounded.Videocam else Icons.Outlined.VideocamOff, "Камера", on = c.cameraOn) { ctx.store.turnCallCamera(!c.cameraOn) }
                if (c.cameraOn) CallBtn(Icons.Rounded.Cameraswitch, "Повернуть") { ctx.store.flipCallCamera() }
                CallBtn(if (c.micMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic, if (c.micMuted) "Вкл. звук" else "Микрофон", on = c.micMuted) { ctx.store.muteCallMic(!c.micMuted) }
                CallBtn(Icons.Rounded.CallEnd, "Завершить", danger = true) { ctx.store.hangUp() }
            }
        }
    }
}

/** GroupCall / VideoConf on the real account: the video chat this account is in. */
@Composable
internal fun RealGroupCall(ctx: KxScreenCtx) {
    val g = ctx.store.groupCall
    if (g == null) { NoCall(ctx, "Вы не в видеочате", "Зайдите в видеочат группы или канала — участники появятся здесь"); return }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    val clock by rememberLoopClock()
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MRoundBtn(Icons.Rounded.KeyboardArrowDown, "Свернуть", size = 42.dp, bg = Kx.w(.1f), border = null) { ctx.back() }
                Column(Modifier.weight(1f)) {
                    T(g.title.ifBlank { "Видеочат" }, 17.sp, FontWeight.ExtraBold, maxLines = 1)
                    T(when { g.failure.isNotBlank() -> g.failure; g.reconnecting -> "Переподключаемся…"; !g.connected -> "Подключаемся…"
                        else -> "${g.count} ${ruPlural(g.count, "участник", "участника", "участников")}" }, 12.sp, color = if (g.failure.isNotBlank()) Kx.danger else Kx.accent)
                }
            }
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(g.members, key = { it.key }) { m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(78.dp).talkRing(m.speaking, clock, null)) {
                            Box(Modifier.matchParentSize().clip(CircleShape).background(Kx.w(.1f)), contentAlignment = Alignment.Center) {
                                T(m.name.take(1).uppercase(), 26.sp, FontWeight.ExtraBold)
                                KxImage(m.avatar, Modifier.matchParentSize(), maxPx = 240)
                            }
                            Box(Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape)
                                .background(if (m.hand) Kx.warning else if (m.muted) Kx.w(.2f) else Kx.accent), contentAlignment = Alignment.Center) {
                                Icon(if (m.hand) Icons.Rounded.BackHand else if (m.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, null, Modifier.size(13.dp), tint = Color.White)
                            }
                            if (m.video) Icon(Icons.Rounded.Videocam, null, Modifier.align(Alignment.TopEnd).size(18.dp), tint = Kx.accent)
                        }
                        T(m.name, 12.sp, FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallBtn(if (g.cameraOn) Icons.Rounded.Videocam else Icons.Outlined.VideocamOff, "Камера", on = g.cameraOn) { ctx.store.turnGroupCamera(!g.cameraOn) }
                CallBtn(if (g.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, if (!g.canUnmute && g.muted) "Нельзя" else if (g.muted) "Вкл. звук" else "Микрофон", on = !g.muted, accent = !g.muted) {
                    if (g.canUnmute || !g.muted) ctx.store.muteGroupMic(!g.muted) else ctx.toast("Админы выключили микрофоны")
                }
                CallBtn(Icons.Rounded.CallEnd, "Выйти", danger = true) {
                    confirm = KxConfirm("Выйти из видеочата?", "Вы сможете вернуться, пока он идёт.", "Выйти") { ctx.store.leaveGroupCall(); ctx.back() }
                }
            }
        }
        confirm?.let { KxConfirmDialog(it) { confirm = null } }
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
            (if (c.endedSeconds > 0) " · ${fmtClock(c.endedSeconds)}" else ""), 14.sp, color = Kx.text3, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
        Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CallBtn(Icons.Rounded.Call, "Позвонить", accent = true) { ctx.store.call(c.peerId, false) }
            CallBtn(Icons.Rounded.Videocam, "Видео") { ctx.store.call(c.peerId, true) }
            CallBtn(Icons.Rounded.ChatBubble, "Написать") { ctx.openChat(c.peerId) }
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
            onNow = { ctx.store.joinGroupCall(chatId); ctx.open("GroupCall") })
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
