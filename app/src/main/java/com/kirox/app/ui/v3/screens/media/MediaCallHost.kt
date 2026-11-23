package com.kirox.app.ui.v3.screens.media

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kirox.app.ui.v3.KxNavRequests
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxCallPhase
import com.kirox.app.ui.v3.model.KxCallState
import com.kirox.app.ui.v3.model.KxGroupCallState
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.theme.*
import com.kirox.app.voip.CallAudioRoute
import com.kirox.app.voip.CallAudioRouter
import com.kirox.app.voip.CallMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * wave6 port-calls: the v3 call UI owns every call on the real account. KxCallHost is drawn by the real-account
 * host (KxTelegramHost) over KxApp and does what the old ui/call/CallScreen + GroupCallScreen + MiniCallChip did:
 * ringtone/vibration (CallMedia), audio route earpiece/speaker/headset/Bluetooth (CallAudioRouter + route sheet),
 * proximity screen-off at the ear, over the lock screen while the call is on screen, minimise to a v3 chip / video
 * window, camera on/off/flip, participants' cameras and shared screens, screen sharing, raised hand, decline with a
 * text reply, Bluetooth permission. The call logic itself stays in the voip package and the Telegram repositories.
 */

/** State of the call host shared with the call boards (VideoCall / KeyCheck / Pip … only expand or shrink it). */
object KxCallUi {
    /** The one-to-one call shows as the top chip (or a small video window) instead of full screen. */
    var minimized by mutableStateOf(false)
    var groupMinimized by mutableStateOf(false)
    /** The KeyCheck board asked for the emoji-key panel. */
    var keyOpen by mutableStateOf(false)
    fun expand() { minimized = false; groupMinimized = false }
}

private val GlassBtn = Color.White.copy(alpha = .12f)
private val OnBtn = Color(0xFFEEF3EF)
private val EndRed = Color(0xFFE5534B)
private val SheetBg = Color(0xFF121614)
private val ChipBg = Color(0xEB141A17)

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

/** Absorbs taps so nothing under a full-screen call reacts. */
@Composable
private fun Modifier.swallowTouches(): Modifier = clickable(remember { MutableInteractionSource() }, indication = null) {}

@Composable
fun KxCallHost(store: KxStore, modifier: Modifier = Modifier, reducedMotion: Boolean = false) = KxTheme(reducedMotion) {
    KxToastHost {
        val toast = LocalKxToast.current
        // Outlives the call screens: a text reply is sent after the call has already gone.
        val hostScope = rememberCoroutineScope()
        Box(modifier.fillMaxSize()) {
            store.groupCall?.let { g -> GroupCallLayer(store, g, toast) }
            OneToOneLayer(store, store.activeCall, toast, hostScope)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// One-to-one
// ---------------------------------------------------------------------------------------------

@Composable
private fun OneToOneLayer(store: KxStore, c: KxCallState?, toast: (String) -> Unit, hostScope: CoroutineScope) {
    var prev by remember { mutableStateOf<KxCallState?>(null) }
    var wasMinimized by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf<KxCallState?>(null) }
    LaunchedEffect(c) {
        if (c != null) { prev = c; ended = null; wasMinimized = KxCallUi.minimized; return@LaunchedEffect }
        val p = prev ?: return@LaunchedEffect
        prev = null
        val last = store.lastCall
        // The finished call on screen gets its summary (failure text, «Нет ответа», duration, rating); a declined
        // incoming call or one finished while minimised just goes.
        if (last != null && last.peerId == p.peerId && !wasMinimized && p.phase != KxCallPhase.Incoming) ended = last
        KxCallUi.minimized = false
    }
    if (c != null) {
        val context = LocalContext.current
        val incoming = c.phase == KxCallPhase.Incoming
        // Ringtone and vibration while the incoming call waits (CallMedia also stops it when it stops ringing).
        DisposableEffect(incoming) {
            if (incoming) CallMedia.startRinging(context)
            onDispose { CallMedia.stopRinging() }
        }
        LaunchedEffect(incoming) { if (incoming) KxCallUi.minimized = false }
        val router = rememberCallRouter(c)
        if (KxCallUi.minimized && !incoming) MiniCall(store, c) else LiveCallScreen(store, c, router, toast, hostScope)
    } else ended?.let { last ->
        EndedPanel(store, last, toast, onClose = { ended = null; store.dismissCall() })
    }
}

/**
 * Audio routing of call [c] (owned by CallMedia for the whole call), switched on once connected; asks for the
 * Bluetooth permission on Android 12+ so a connected headset can be used.
 */
@Composable
private fun rememberCallRouter(c: KxCallState): CallAudioRouter {
    val context = LocalContext.current
    val router = remember(c.id) { CallMedia.router(context, c.id) }
    val talking = c.phase == KxCallPhase.Active || c.phase == KxCallPhase.Connecting
    val video = c.video
    val bt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && router.isRunning) { router.stop(); router.start(video) }
    }
    LaunchedEffect(router, talking) {
        if (!talking) return@LaunchedEffect
        if (!router.isRunning) router.start(video)
        if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            runCatching { bt.launch(android.Manifest.permission.BLUETOOTH_CONNECT) }
    }
    return router
}

/** Seconds since the call connected, ticking. */
@Composable
internal fun callSecondsOf(c: KxCallState): Int {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(c.sinceMs) { while (c.sinceMs > 0) { now = System.currentTimeMillis(); delay(1000) } }
    return if (c.sinceMs > 0) ((now - c.sinceMs) / 1000).toInt().coerceAtLeast(0) else 0
}

internal fun callStatus(c: KxCallState, sec: Int): String = when (c.phase) {
    KxCallPhase.Calling -> if (c.video) "Видеозвонок…" else "Вызов…"
    KxCallPhase.Ringing -> "Звонок идёт…"
    KxCallPhase.Incoming -> if (c.video) "Входящий видеозвонок…" else "Входящий звонок…"
    KxCallPhase.Connecting -> "Соединение…"
    KxCallPhase.Active -> when {
        c.reconnecting -> "Переподключаемся…"
        c.signalBars <= 1 -> fmtClock(sec) + " · плохая связь"
        else -> fmtClock(sec)
    }
    KxCallPhase.Ended -> c.endReason.ifBlank { "Звонок завершён" }
    KxCallPhase.Failed -> c.failure.ifBlank { "Не удалось позвонить" }
}

/** Round avatar of a call: photo over the letter. */
@Composable
internal fun CallAvatar(name: String, avatar: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).cssGrad(140f, Kx.accentMid.copy(alpha = .55f), Kx.accentDeep.copy(alpha = .8f)), contentAlignment = Alignment.Center) {
        T(name.take(1).uppercase(), (size.value * .36f).sp, FontWeight.ExtraBold)
        KxImage(avatar, Modifier.matchParentSize(), maxPx = (size.value * 3).toInt())
    }
}

@Composable
private fun CallSignalBars(bars: Int) {
    Row(Modifier.height(14.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..4).forEach { i ->
            val col = if (i <= bars) (if (bars >= 3) Kx.accent else if (bars == 2) Kx.warning else Kx.danger) else Kx.w(.25f)
            Box(Modifier.width(3.dp).height((3 * i + 2).dp).clip(RoundedCornerShape(1.dp)).background(col))
        }
    }
}

private fun routeIcon(r: CallAudioRoute): ImageVector = when (r) {
    CallAudioRoute.Earpiece -> Icons.Rounded.PhoneInTalk
    CallAudioRoute.Speaker -> Icons.AutoMirrored.Rounded.VolumeUp
    CallAudioRoute.Headset -> Icons.Rounded.Headphones
    CallAudioRoute.Bluetooth -> Icons.Rounded.BluetoothAudio
}

private fun routeName(r: CallAudioRoute) = when (r) {
    CallAudioRoute.Earpiece -> "Телефон"; CallAudioRoute.Speaker -> "Динамик"
    CallAudioRoute.Headset -> "Наушники"; CallAudioRoute.Bluetooth -> "Наушники Bluetooth"
}

/** «Вывод звука»: phone, speaker, wired headset, Bluetooth — only what is connected; the current one ticked. */
@Composable
private fun RouteSheet(visible: Boolean, current: CallAudioRoute, routes: Set<CallAudioRoute>, onPick: (CallAudioRoute) -> Unit, onClose: () -> Unit) {
    MSheet(visible, onClose, dim = .6f, top = SheetBg, bottom = SheetBg, border = Kx.w(.08f), highlight = null,
        handleColor = Color(0xFF3A4540), handleWidth = 36.dp, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp)) {
        T("Вывод звука", 17.sp, FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
        CallAudioRoute.entries.filter { it in routes }.forEach { o ->
            val on = o == current
            Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f))
                .kxClick(routeName(o)) { onPick(o) }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(routeIcon(o), null, tint = Kx.accent)
                T(routeName(o), 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (on) Icon(Icons.Rounded.CheckCircle, "Выбрано", tint = Kx.accent)
            }
        }
    }
}

/** A WebRTC renderer handed to the engine through [attach] while on screen, released when it leaves. */
@Composable
private fun CallRenderer(attach: (Any?) -> Unit, mirror: Boolean, fit: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val renderer = remember { org.webrtc.TextureViewRenderer(context) }
    val currentAttach by rememberUpdatedState(attach)
    DisposableEffect(renderer) {
        renderer.init(com.kirox.app.voip.VideoCapturerDevice.getSharedEGLContext(), null)
        renderer.setScalingType(if (fit) org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT else org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        currentAttach(renderer)
        onDispose { currentAttach(null); renderer.release() }
    }
    LaunchedEffect(mirror) { renderer.setMirror(mirror) }
    AndroidView({ renderer }, modifier)
}

/** Full-screen one-to-one call (board «Видеозвонок»; the incoming state is board «Входящий звонок»). */
@Composable
private fun LiveCallScreen(store: KxStore, c: KxCallState, router: CallAudioRouter, toast: (String) -> Unit, hostScope: CoroutineScope) {
    val context = LocalContext.current
    val route by router.route.collectAsState()
    val routes by router.available.collectAsState()
    val live = c.phase == KxCallPhase.Active
    val talking = live || c.phase == KxCallPhase.Connecting
    // Over the lock screen with the screen on while the call is on screen; off again when it leaves.
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        activity?.let { runCatching { it.setShowWhenLocked(true); it.setTurnScreenOn(true) } }
        onDispose { activity?.let { runCatching { it.setShowWhenLocked(false); it.setTurnScreenOn(false) } } }
    }
    // Screen off at the ear during a voice call through the earpiece (the lock is held by CallMedia).
    val earMode = talking && route == CallAudioRoute.Earpiece && !c.cameraOn && !c.remoteVideo
    DisposableEffect(earMode) {
        CallMedia.setProximity(context, earMode)
        onDispose { CallMedia.setProximity(context, false) }
    }
    BackHandler { if (c.phase != KxCallPhase.Incoming) KxCallUi.minimized = true }
    if (c.phase == KxCallPhase.Incoming) { IncomingCall(store, c, toast, hostScope); return }

    val sec = callSecondsOf(c)
    var ui by remember { mutableStateOf(true) }
    var keyOpen by remember { mutableStateOf(false) }
    LaunchedEffect(KxCallUi.keyOpen) { if (KxCallUi.keyOpen) { keyOpen = true; KxCallUi.keyOpen = false } }
    var outSheet by remember { mutableStateOf(false) }
    var pipLow by remember { mutableStateOf(false) }
    val video = com.kirox.app.telegram.TelegramVoipRepository.mediaAvailable && talking && (c.remoteVideo || c.cameraOn)
    val uiShown = !(c.remoteVideo && video) || ui
    val uiA by animateFloatAsState(if (uiShown) 1f else 0f, tween(300, easing = Ease), label = "ui")

    Box(Modifier.fillMaxSize().background(Kx.bg).swallowTouches()) {
        if (video) {
            if (c.remoteVideo) {
                CallRenderer({ store.attachCallVideo(it, null) }, mirror = false, fit = false, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxWidth().height(180.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .55f), Color.Transparent))))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .7f)))))
                Box(Modifier.fillMaxSize().padding(top = 70.dp, bottom = 210.dp).clickableNoRipple("Показать или скрыть кнопки") { ui = !ui; keyOpen = false })
            }
        }
        if (!(video && c.remoteVideo)) Box(Modifier.fillMaxSize().ellipseGlow(Kx.accentDeep.copy(alpha = .28f), .5f, .35f, .8f, .5f, .7f)) {
            Column(Modifier.align(Alignment.Center).padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(132.dp)) {
                    if (!live) CallRings(Modifier.matchParentSize())
                    CallAvatar(c.name, c.avatar, 132.dp)
                }
                T(c.name, 28.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp, start = 24.dp, end = 24.dp), maxLines = 1)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (live && !c.remoteMuted && !c.reconnecting) SpeakBars()
                    if (c.remoteMuted && live) Icon(Icons.Rounded.MicOff, null, Modifier.size(16.dp), tint = Kx.danger)
                    T(callStatus(c, sec), 14.sp, color = Kx.text3)
                }
                if (c.remoteMuted && live) T("У собеседника выключен микрофон", 13.sp, color = Kx.text4, modifier = Modifier.padding(top = 6.dp))
                if (live && c.video && !c.remoteVideo) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.VideocamOff, null, Modifier.size(18.dp), tint = Kx.text3)
                    T("Камера собеседника выключена", 13.sp, color = Kx.text3)
                }
            }
        }

        // Own camera in the corner; a tap moves it down and back.
        if (video && c.cameraOn) {
            val top by animateDpAsState(if (pipLow) 470.dp else if (uiShown) 74.dp else 24.dp, tween(450, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)), label = "pip")
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = top, end = 16.dp).size(104.dp, 150.dp)
                .shadow(36.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = .55f), spotColor = Color.Black.copy(alpha = .55f))
                .clip(RoundedCornerShape(22.dp)).background(Color(0xFF15181E)).border(1.dp, Kx.w(.22f), RoundedCornerShape(22.dp))) {
                CallRenderer({ store.attachCallVideo(null, it) }, mirror = c.frontCamera, fit = false, modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().clickableNoRipple("Переместить своё видео") { pipLow = !pipLow })
                if (c.micMuted) Box(Modifier.align(Alignment.BottomStart).padding(6.dp).size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = .55f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MicOff, null, Modifier.size(15.dp), tint = Kx.danger)
                }
            }
        }

        // Top bar.
        Column(Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 12.dp).fillMaxWidth()
            .graphicsLayer { alpha = uiA; translationY = (1f - uiA) * -20.dp.toPx() }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Rounded.KeyboardArrowDown, "Свернуть звонок", size = 42.dp, bg = Kx.w(.1f), border = Kx.w(.14f)) { KxCallUi.minimized = true }
                if (video && c.remoteVideo) Column(Modifier.weight(1f, fill = false)) {
                    T(c.name, 17.sp, FontWeight.ExtraBold, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (live && !c.remoteMuted) SpeakBars()
                        if (c.remoteMuted) Icon(Icons.Rounded.MicOff, null, Modifier.size(14.dp), tint = Kx.danger)
                        T(callStatus(c, sec), 12.sp, color = Kx.text2)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (live) CallSignalBars(c.signalBars)
                if (c.emojis.isNotEmpty()) Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.1f)).border(1.dp, Kx.w(.14f), RoundedCornerShape(19.dp))
                    .kxClick("Ключ шифрования") { keyOpen = !keyOpen }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text(c.emojis.joinToString(""), style = ts(16.sp).copy(letterSpacing = 2.sp))
                }
            }
            if (!com.kirox.app.telegram.TelegramVoipRepository.mediaAvailable) Box(Modifier.padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(Kx.warning.copy(alpha = .14f))
                .padding(horizontal = 10.dp, vertical = 4.dp)) { T("Звук и видео в звонках пока недоступны в этой версии", 11.sp, FontWeight.SemiBold, Kx.warning) }
            if (c.reconnecting || (live && c.signalBars <= 1)) {
                val (title, sub) = if (c.reconnecting) "Переподключаемся…" to "Звонок продолжится сам, как только вернётся сеть" else "Слабая связь" to (if (c.cameraOn) "Видео может подтормаживать" else "Звук может прерываться")
                Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xB83C2C0E)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (c.reconnecting) Icons.Rounded.Sync else Icons.Outlined.SignalCellularAlt, null, tint = Kx.warning)
                    Column(Modifier.weight(1f)) { T(title, 14.sp, FontWeight.Bold); T(sub, 12.sp, color = Kx.text3) }
                    if (c.cameraOn && live) Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.12f)).kxClick { store.turnCallCamera(false) }.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center) { T("Только голос", 12.sp, FontWeight.Bold) }
                }
            }
        }

        AnimatedVisibility(keyOpen && c.emojis.isNotEmpty(), Modifier.statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 70.dp),
            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 1.04f), exit = fadeOut(tween(150))) {
            Column(Modifier.fillMaxWidth().shadow(50.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f))
                .clip(RoundedCornerShape(24.dp)).background(Kx.surface).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(24.dp)).topHighlight(Kx.w(.18f)).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(c.emojis.joinToString(""), style = ts(34.sp).copy(letterSpacing = 6.sp))
                T("Звонок защищён", 15.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
                Text("Если у ${c.name} те же эмодзи, звонок зашифрован сквозным шифрованием и его слышите только вы двое",
                    style = ts(13.sp, color = Kx.text2).copy(lineHeight = 18.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Box(Modifier.padding(top = 12.dp).height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.accent).kxClick { keyOpen = false }.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center) { T("Понятно", 14.sp, FontWeight.Bold, Kx.onAccent) }
            }
        }

        // Controls.
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 26.dp)
            .graphicsLayer { alpha = uiA; translationY = (1f - uiA) * 30.dp.toPx() }) {
            // critique-C P1: one control row, one rule (white = on; the mic is white while it is on, MicOff on glass when muted).
            val speakerOn = route != CallAudioRoute.Earpiece
            KxCallControls(listOf(
                KxCallButton(routeIcon(route), if (routes.size > 2) route.label else "Динамик", speakerOn) { if (routes.size > 2) outSheet = true else router.toggle() },
                KxCallButton(if (c.cameraOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff, "Камера", c.cameraOn) { store.turnCallCamera(!c.cameraOn) },
                KxCallButton(Icons.Rounded.Cameraswitch, "Сменить") { if (c.cameraOn) store.flipCallCamera() else toast("Сначала включите камеру") },
                KxCallButton(if (c.micMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic, "Микрофон", !c.micMuted) { store.muteCallMic(!c.micMuted) },
                KxCallButton(Icons.Rounded.CallEnd, "Завершить", end = true) { outSheet = false; keyOpen = false; store.hangUp() },
            ), Modifier.clip(RoundedCornerShape(34.dp)).background(Kx.surface.copy(alpha = .7f)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(34.dp)).topHighlight(Kx.w(.18f)).padding(horizontal = 4.dp, vertical = 14.dp))
        }

        RouteSheet(outSheet, route, routes, onPick = { router.select(it); outSheet = false }, onClose = { outSheet = false })
    }
}

/** Board «Входящий звонок»: rings, slide (or tap) to answer, decline, decline with a text, open the chat. */
@Composable
private fun IncomingCall(store: KxStore, c: KxCallState, toast: (String) -> Unit, hostScope: CoroutineScope) {
    // After a cold start from a push the engine may still be loading: answering appears when it is ready.
    val engineReady by com.kirox.app.voip.NativeCall.ready.collectAsState()
    val canAnswer = engineReady || store.callEngineReady
    var slide by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf(false) }
    var own by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(slide) { if (slide) { delay(450); store.answerCall(); delay(2500); slide = false } }
    fun declineWith(text: String) {
        reply = false
        store.hangUp()
        hostScope.launch {
            val chat = store.privateChat(c.peerId)
            if (chat == null) toast("Не удалось отправить сообщение") else { store.send(chat, text); toast("Отправлено: «$text»") }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF070908)).swallowTouches()) {
        DriftGlow(Color(120, 100, 200).copy(alpha = .35f), 420.dp, Modifier.offset((-80).dp, (-60).dp), 9000, -20f, 14f, 1.08f)
        DriftGlow(Kx.accentDeep.copy(alpha = .32f), 440.dp, Modifier.align(Alignment.BottomEnd).offset(120.dp, (-120).dp), 11000, -20f, 14f, 1.08f, startOffsetMs = 3000)
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.align(Alignment.TopCenter).padding(top = 54.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.08f)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(15.dp), tint = Kx.accent)
                T("${if (c.video) "Видеозвонок" else "Аудиозвонок"} · kiroX", 12.sp, color = Kx.text2)
            }
            Box(Modifier.align(Alignment.TopCenter).padding(top = 150.dp).size(140.dp)) {
                CallRings(Modifier.matchParentSize(), color = Kx.accent.copy(alpha = .6f), periodMs = 2200, toScale = 2.1f, fromAlpha = .5f)
                CallAvatar(c.name, c.avatar, 140.dp, Modifier.shadow(50.dp, CircleShape, ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f)))
            }
            Column(Modifier.padding(top = 320.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                T(c.name, 32.sp, FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 24.dp), maxLines = 1)
                T(callStatus(c, 0), 15.sp, color = Kx.text3, modifier = Modifier.padding(top = 8.dp))
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 230.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                IncomingRound(Icons.Outlined.Sms, "Сообщение") { reply = true }
            }
            if (canAnswer) BoxWithConstraints(Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 120.dp).fillMaxWidth().height(76.dp)
                .clip(RoundedCornerShape(38.dp)).background(Kx.surface.copy(alpha = .6f)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(38.dp)).topHighlight(Kx.w(.18f))) {
                val travel = maxWidth - 76.dp
                val density = LocalDensity.current
                var drag by remember { mutableFloatStateOf(0f) }
                val hintA by animateFloatAsState(if (slide || drag > 0f) 0f else 1f, tween(200), label = "hint")
                Row(Modifier.fillMaxSize().padding(start = 92.dp, end = 20.dp).graphicsLayer { alpha = hintA }, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)) {
                    T("Сдвиньте, чтобы ответить", 15.sp, FontWeight.SemiBold, Kx.text2, maxLines = 1)
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = Kx.text2)
                }
                val dragDp = with(density) { drag.toDp() }
                val kx by animateDpAsState(if (slide) travel else dragDp.coerceIn(0.dp, travel), tween(if (slide) 300 else 0, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "knob")
                Box(Modifier.padding(6.dp).offset(x = kx).size(64.dp).shadow(22.dp, CircleShape, ambientColor = Kx.accentDeep.copy(alpha = .5f), spotColor = Kx.accentDeep.copy(alpha = .5f))
                    .clip(CircleShape).cssGrad(140f, Kx.accentMid, Kx.accentDeep)
                    .pointerInput(travel) {
                        val max = travel.toPx()
                        detectHorizontalDragGestures(onDragEnd = { if (drag > max * .7f) slide = true else drag = 0f }, onDragCancel = { drag = 0f }) { _, dx -> drag = (drag + dx).coerceIn(0f, max) }
                    }
                    .kxClick("Ответить") { slide = true }, contentAlignment = Alignment.Center) {
                    Icon(if (c.video) Icons.Rounded.Videocam else Icons.Rounded.Call, null, Modifier.size(30.dp), tint = Kx.onAccent)
                }
            } else T("Ответьте на другом устройстве — kiroX ещё загружает звонки", 13.sp, color = Kx.text3,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 40.dp, end = 40.dp, bottom = 140.dp))
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).size(160.dp, 48.dp).clip(RoundedCornerShape(24.dp)).background(EndRed.copy(alpha = .16f))
                .border(1.dp, EndRed.copy(alpha = .4f), RoundedCornerShape(24.dp)).kxClick("Отклонить") { store.hangUp() },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CallEnd, null, tint = Kx.danger)
                T("Отклонить", 15.sp, FontWeight.Bold, Kx.danger)
            }
        }

        MSheet(reply, { reply = false; own = null }, dim = .6f, top = SheetBg, bottom = SheetBg, border = Kx.w(.08f), highlight = null,
            handleColor = Color(0xFF3A4540), handleWidth = 36.dp, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp)) {
            T("Отклонить и написать", 18.sp, FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
            val typed = own
            if (typed == null) {
                listOf("Не могу говорить, напишу позже", "Перезвоню через 10 минут", "Я за рулём", "Что случилось?").forEach { t ->
                    Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.045f))
                        .kxClick(t) { declineWith(t) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        T(t, 15.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Outlined.Send, null, tint = Kx.accent)
                    }
                }
                Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).kxClick { own = "" }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Edit, null, tint = Kx.accent)
                    T("Свой текст…", 15.sp, FontWeight.SemiBold, Kx.accent)
                }
                Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).kxClick {
                    reply = false
                    store.hangUp()
                    hostScope.launch { store.privateChat(c.peerId)?.let { KxNavRequests.requestChat(it) } ?: toast("Не удалось открыть чат") }
                }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, null, tint = Kx.accent)
                    T("Отклонить и открыть чат", 15.sp, FontWeight.SemiBold, Kx.accent)
                }
            } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).heightIn(min = 50.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(horizontal = 14.dp, vertical = 14.dp)) {
                    if (typed.isEmpty()) T("Сообщение", 15.sp, color = Kx.text4)
                    BasicTextField(typed, { own = it.take(500) }, Modifier.fillMaxWidth(), textStyle = ts(15.sp), cursorBrush = SolidColor(Kx.accent), maxLines = 4)
                }
                val ready = typed.isNotBlank()
                Box(Modifier.size(50.dp).clip(CircleShape).background(if (ready) Kx.accent else Kx.w(.08f)).kxClick("Отправить", enabled = ready) { declineWith(typed.trim()) },
                    contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Outlined.Send, null, tint = if (ready) Kx.onAccent else Kx.text4) }
            }
        }
    }
}

@Composable
private fun IncomingRound(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.kxClick(label, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Kx.w(.08f)).border(1.dp, Kx.w(.12f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Kx.text2) }
        T(label, 12.sp, color = Kx.text2)
    }
}

/** Minimised call over the app: a v3 chip at the top (and the other side's camera in a small window). */
@Composable
private fun MiniCall(store: KxStore, c: KxCallState) {
    val sec = callSecondsOf(c)
    val live = c.phase == KxCallPhase.Active
    Box(Modifier.fillMaxSize()) {
        if (live && c.remoteVideo && com.kirox.app.telegram.TelegramVoipRepository.mediaAvailable) Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 62.dp, end = 12.dp)
            .size(104.dp, 150.dp).kxRise(0, 300).shadow(40.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = .6f), spotColor = Color.Black.copy(alpha = .6f))
            .clip(RoundedCornerShape(18.dp)).background(Color(0xFF15181E)).border(1.dp, Kx.w(.2f), RoundedCornerShape(18.dp))) {
            CallRenderer({ store.attachCallVideo(it, null) }, mirror = false, fit = false, modifier = Modifier.matchParentSize())
            Box(Modifier.matchParentSize().clickableNoRipple("Развернуть звонок") { KxCallUi.minimized = false })
        }
        CallChip(Modifier.align(Alignment.TopCenter), onOpen = { KxCallUi.minimized = false }, openLabel = "Вернуться к звонку",
            icon = if (c.video) Icons.Rounded.Videocam else Icons.Rounded.Call, title = c.name,
            status = if (live) callStatus(c, sec) else callStatus(c, 0), speaking = live && !c.remoteMuted,
            micMuted = c.micMuted, onMic = { store.muteCallMic(!c.micMuted) }, endLabel = "Завершить звонок", onEnd = { store.hangUp() })
    }
}

@Composable
private fun CallChip(modifier: Modifier, onOpen: () -> Unit, openLabel: String, icon: ImageVector, title: String, status: String, speaking: Boolean,
                     micMuted: Boolean, onMic: () -> Unit, endLabel: String, onEnd: () -> Unit) {
    Row(modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 6.dp).fillMaxWidth().height(52.dp).kxRise(0, 250)
        .shadow(30.dp, RoundedCornerShape(26.dp), ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f))
        .clip(RoundedCornerShape(26.dp)).background(ChipBg).background(Brush.verticalGradient(listOf(Kx.accent.copy(alpha = .14f), Kx.accent.copy(alpha = .04f))))
        .border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(26.dp)).kxClick(openLabel, onClick = onOpen).padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(30.dp).clip(CircleShape).cssGrad(140f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(17.dp), tint = Kx.onAccent)
        }
        Column(Modifier.weight(1f)) {
            T(title, 14.sp, FontWeight.ExtraBold, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (speaking) SpeakBars(height = 10.dp, max = 10.dp, min = 3.dp, barWidth = 2.dp)
                T(status, 12.sp, color = Kx.accent, maxLines = 1)
            }
        }
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (micMuted) OnBtn else Kx.w(.1f)).kxClick(if (micMuted) "Включить микрофон" else "Выключить микрофон", onClick = onMic),
            contentAlignment = Alignment.Center) { Icon(if (micMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic, null, Modifier.size(20.dp), tint = if (micMuted) Kx.bg else OnBtn) }
        Box(Modifier.size(40.dp).clip(CircleShape).background(EndRed).kxClick(endLabel, onClick = onEnd), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.CallEnd, null, Modifier.size(20.dp), tint = Color.White)
        }
    }
}

/** After a call that was on screen: result (or the reason it failed), call again, write, rate (board «Звонок завершён»). */
@Composable
private fun EndedPanel(store: KxStore, last: KxCallState, toast: (String) -> Unit, onClose: () -> Unit) {
    var rating by remember(last.id) { mutableStateOf(false) }
    var touched by remember(last.id) { mutableStateOf(false) }
    LaunchedEffect(last.id, rating, touched) { if (!rating && !touched) { delay(4000); onClose() } }
    BackHandler(onBack = onClose)
    if (rating) {
        Box(Modifier.fillMaxSize().background(Kx.bg).swallowTouches()) {
            RealCallEnd(KxScreenCtx(store, back = onClose, open = { }, openChat = { KxNavRequests.requestChat(it); onClose() }, toast = toast))
        }
        return
    }
    val failed = last.phase == KxCallPhase.Failed
    Column(Modifier.fillMaxSize().background(Kx.bg).ellipseGlow(Kx.accentDeep.copy(alpha = .18f), .5f, .42f, .7f, .45f, .7f).swallowTouches()
        .statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        MRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.08f), border = null) { onClose() }
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
            CallAvatar(last.name, last.avatar, 96.dp)
            Text(if (failed) last.failure.ifBlank { "Не удалось позвонить" } else last.endReason.ifBlank { "Звонок завершён" },
                style = ts(22.sp, FontWeight.ExtraBold, if (failed) Kx.danger else Kx.text), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp))
            T((if (last.video) "Видеозвонок" else "Аудиозвонок") + " · " + last.name + (if (last.endedSeconds > 0) " · ${fmtClock(last.endedSeconds)}" else ""), 14.sp, color = Kx.text3, maxLines = 1)
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillBtn(if (last.video) Icons.Rounded.Videocam else Icons.Rounded.Call, "Позвонить снова", Kx.accent, Kx.onAccent) { onClose(); store.call(last.peerId, last.video) }
                PillBtn(Icons.AutoMirrored.Outlined.Chat, "Написать", Kx.w(.08f), Kx.text) { onClose(); KxNavRequests.requestChat(last.peerId) }
            }
            if (last.endedSeconds > 0) Box(Modifier.padding(top = 4.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).kxClick { touched = true; rating = true }.padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center) { T("Оценить звонок", 14.sp, FontWeight.Bold, Kx.accent) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Video chats
// ---------------------------------------------------------------------------------------------

@Composable
private fun GroupCallLayer(store: KxStore, g: KxGroupCallState, toast: (String) -> Unit) {
    val context = LocalContext.current
    val failed = g.failure.isNotBlank()
    // Video chats start on the speaker, like a conference; the router lives as long as the chat, minimised too.
    val router = remember(g.chatId) { CallAudioRouter(context) }
    val bt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && router.isRunning) { router.stop(); router.start(isVideo = true) }
    }
    DisposableEffect(router, failed) {
        if (!failed) {
            router.start(isVideo = true)
            if (Build.VERSION.SDK_INT >= 31 && context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                runCatching { bt.launch(android.Manifest.permission.BLUETOOTH_CONNECT) }
        }
        onDispose { router.stop() }
    }
    val err = store.groupCallError
    LaunchedEffect(err) { if (err != null) { toast(err); store.clearGroupCallError() } }
    // Screen sharing asks the system for consent first; the dialog also lets the user pick one app.
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val data = r.data
        if (r.resultCode == Activity.RESULT_OK && data != null) store.startGroupScreenShare(data)
    }
    val askScreen: () -> Unit = {
        val projection = context.getSystemService(android.media.projection.MediaProjectionManager::class.java)
        if (projection == null) toast("Показ экрана недоступен на этом телефоне")
        else runCatching { consent.launch(projection.createScreenCaptureIntent()) }.onFailure { toast("Показ экрана недоступен на этом телефоне") }
    }
    LaunchedEffect(g.chatId) { KxCallUi.groupMinimized = false }
    if (KxCallUi.groupMinimized && !failed) {
        val count = "${g.count} ${ruPlural(g.count, "участник", "участника", "участников")}"
        Box(Modifier.fillMaxSize()) {
            CallChip(Modifier.align(Alignment.TopCenter), onOpen = { KxCallUi.groupMinimized = false }, openLabel = "Вернуться в видеочат",
                icon = Icons.Rounded.Groups, title = g.title.ifBlank { "Видеочат" },
                status = when { g.reconnecting -> "Переподключаемся…"; !g.connected -> "Подключаемся…"; else -> "Видеочат · $count" },
                speaking = g.members.any { it.speaking }, micMuted = g.muted,
                onMic = { if (g.canUnmute || !g.muted) store.muteGroupMic(!g.muted) else toast("Админы выключили микрофоны — поднимите руку") },
                endLabel = "Выйти из видеочата", onEnd = { store.leaveGroupCall() })
        }
    } else LiveGroupScreen(store, g, router, askScreen, toast)
}

/** Board «Видеочат» (VideoConf when cameras are on): tiles with cameras and shared screens, speaker line, controls, participants. */
@Composable
private fun LiveGroupScreen(store: KxStore, g: KxGroupCallState, router: CallAudioRouter, askScreen: () -> Unit, toast: (String) -> Unit) {
    val failed = g.failure.isNotBlank()
    val route by router.route.collectAsState()
    val routes by router.available.collectAsState()
    var confirmLeave by remember { mutableStateOf(false) }
    var people by remember { mutableStateOf(false) }
    var outSheet by remember { mutableStateOf(false) }
    BackHandler { if (failed) store.dismissGroupCall() else KxCallUi.groupMinimized = true }
    val clock by rememberLoopClock()
    val count = "${g.count} ${ruPlural(g.count, "участник", "участника", "участников")}"
    val glass = GlassBtn; val on = OnBtn

    Box(Modifier.fillMaxSize().background(Kx.bg).ellipseGlow(Kx.accentDeep.copy(alpha = .22f), .5f, 0f, .9f, .5f, .6f).swallowTouches()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MRoundBtn(Icons.Rounded.KeyboardArrowDown, if (failed) "Закрыть" else "Свернуть", size = 42.dp, bg = Kx.w(.07f), border = Kx.w(.1f)) {
                    if (failed) store.dismissGroupCall() else KxCallUi.groupMinimized = true
                }
                Column(Modifier.weight(1f)) {
                    T(g.title.ifBlank { "Видеочат" }, 17.sp, FontWeight.ExtraBold, maxLines = 1)
                    T(when { failed -> g.failure; g.reconnecting -> "Переподключаемся…"; !g.connected -> "Подключаемся…"; else -> "Видеочат · $count" },
                        12.sp, color = if (failed) Kx.danger else Kx.text3, maxLines = 2)
                }
                if (!failed) MRoundBtn(Icons.Outlined.Group, "Участники", size = 42.dp, bg = Kx.w(.07f), border = Kx.w(.1f)) { people = true }
            }
            if (failed) {
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                    Box(Modifier.size(88.dp).clip(RoundedCornerShape(30.dp)).cssGrad(145f, Color(0xFF2A1D1D), Color(0xFF171212)).border(1.dp, Color(0xFF3A2A2A), RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PhoneDisabled, null, Modifier.size(44.dp), tint = Kx.danger) }
                    Text(g.failure, style = ts(18.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 30.dp))
                    Box(Modifier.padding(top = 12.dp).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.accent).kxClick { store.dismissGroupCall() }.padding(horizontal = 22.dp),
                        contentAlignment = Alignment.Center) { T("Закрыть", 14.sp, FontWeight.Bold, Kx.onAccent) }
                }
                return@Column
            }
            val speaker = g.members.firstOrNull { it.speaking && !it.self } ?: g.members.firstOrNull { it.speaking }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp).height(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (speaker != null) {
                    SpeakBars(height = 12.dp, max = 12.dp, periodMs = 800, stepMs = 150)
                    T("Говорит: ${speaker.name}", 13.sp, color = Kx.accent, maxLines = 1)
                } else if (g.screenSharing) {
                    Icon(Icons.Rounded.ScreenShare, null, Modifier.size(16.dp), tint = Kx.accent)
                    T("Вы показываете экран", 13.sp, FontWeight.Bold, Kx.accent)
                } else T(if (g.muted) "Микрофон выключен" else "Вы в эфире", 13.sp, color = Kx.text3)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (g.members.isEmpty()) androidx.compose.material3.CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp), color = Kx.accent, strokeWidth = 2.dp)
                LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 220.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Shared screens first, full width: they are what everyone is looking at.
                    items(g.members.filter { it.screenEndpoint != null }, key = { it.key + ":screen" }, span = { GridItemSpan(maxLineSpan) }) { m ->
                        val endpoint = m.screenEndpoint!!
                        Box(Modifier.height(240.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF101412))) {
                            key(endpoint) { CallRenderer({ store.attachGroupVideo(endpoint, it) }, mirror = false, fit = true, modifier = Modifier.matchParentSize()) }
                            TileLabel(m.name + " · экран", m.muted, false, m.hand, clock, Modifier.align(Alignment.BottomStart))
                        }
                    }
                    items(g.members, key = { it.key }) { m ->
                        Box(Modifier.height(168.dp).talkRing(m.speaking, clock, 24.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF151A1C))) {
                            val endpoint = m.videoEndpoint
                            when {
                                m.self && g.cameraOn -> key("self") { CallRenderer({ store.attachGroupVideo(null, it) }, mirror = g.frontCamera, fit = false, modifier = Modifier.matchParentSize()) }
                                endpoint != null -> key(endpoint) { CallRenderer({ store.attachGroupVideo(endpoint, it) }, mirror = false, fit = false, modifier = Modifier.matchParentSize()) }
                                else -> CallAvatar(m.name, m.avatar, 74.dp, Modifier.align(Alignment.Center))
                            }
                            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(60.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .6f)))))
                            TileLabel(m.name, m.muted, m.speaking, m.hand, clock, Modifier.align(Alignment.BottomStart))
                        }
                    }
                }
            }
        }

        if (!failed) Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 24.dp)) {
            // Less used actions as chips above the bar (VideoCall board pattern).
            val connected = g.connected
            if (g.cameraOn || (connected && (g.canEnableVideo || g.screenSharing))) Row(Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                if (g.cameraOn) CallChip(Icons.Rounded.Cameraswitch, "Повернуть камеру") { store.flipGroupCamera() }
                if (connected && (g.canEnableVideo || g.screenSharing))
                    CallChip(if (g.screenSharing) Icons.Outlined.StopScreenShare else Icons.Outlined.ScreenShare, if (g.screenSharing) "Остановить показ" else "Показать экран") {
                        if (g.screenSharing) store.stopGroupScreenShare() else askScreen()
                    }
            }
            val speakerOn = route != CallAudioRoute.Earpiece
            val live = !g.muted
            // critique-C P1: the same control row as 1:1 calls (white = on; no green «live» mic).
            KxCallControls(listOf(
                KxCallButton(if (g.cameraOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff, "Камера", g.cameraOn) {
                    if (g.cameraOn) store.turnGroupCamera(false) else if (g.canEnableVideo) store.turnGroupCamera(true) else toast("Камеры в этом видеочате выключены")
                },
                KxCallButton(routeIcon(route), if (routes.size > 2) route.label else if (speakerOn) "Динамик" else "Телефон", speakerOn) {
                    if (routes.size > 2) outSheet = true else router.toggle()
                },
                KxCallButton(if (live) Icons.Rounded.Mic else Icons.Rounded.MicOff, if (!g.canUnmute && g.muted) "Нельзя" else "Микрофон", live) {
                    if (g.canUnmute || !g.muted) store.muteGroupMic(!g.muted) else toast("Админы выключили микрофоны — поднимите руку")
                },
                KxCallButton(Icons.Rounded.FrontHand, if (g.handRaised) "Опустить руку" else "Рука", g.handRaised) { store.raiseGroupHand(!g.handRaised) },
                KxCallButton(Icons.Rounded.CallEnd, "Выйти", end = true) { confirmLeave = true },
            ), Modifier.shadow(40.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f))
                .clip(RoundedCornerShape(34.dp)).background(Kx.surface).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(34.dp)).topHighlight(Kx.w(.18f)).padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 12.dp))
        }

        MSheet(people, { people = false }, dim = .6f, top = SheetBg, bottom = SheetBg, border = Kx.w(.08f), highlight = null,
            handleColor = Color(0xFF3A4540), handleWidth = 36.dp, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp), modifier = Modifier.heightIn(max = 620.dp)) {
            T("Участники · $count", 19.sp, FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                g.members.forEach { m ->
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CallAvatar(m.name, m.avatar, 44.dp, Modifier.talkRing(m.speaking, clock, null))
                        Column(Modifier.weight(1f)) {
                            T(m.name, 15.sp, FontWeight.Bold, maxLines = 1)
                            T(when { m.speaking -> "говорит"; m.hand -> "хочет сказать"; m.screenEndpoint != null -> "показывает экран"; m.muted -> "микрофон выключен"; else -> "слушает" }, 12.sp,
                                color = if (m.speaking) Kx.accent else if (m.hand) Kx.warning else Kx.text4)
                        }
                        if (m.video) Icon(Icons.Rounded.Videocam, null, Modifier.size(20.dp), tint = Kx.text3)
                        if (m.hand) Icon(Icons.Rounded.FrontHand, null, Modifier.size(20.dp), tint = Kx.warning)
                        Icon(if (m.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, null, Modifier.size(20.dp), tint = if (m.muted) Kx.text4 else if (m.speaking) Kx.accent else Kx.text)
                    }
                }
            }
        }
        RouteSheet(outSheet, route, routes, onPick = { router.select(it); outSheet = false }, onClose = { outSheet = false })
        MConfirm(if (confirmLeave) "Выйти из видеочата?" else null, "Вы сможете вернуться, пока он идёт.", "Выйти",
            onNo = { confirmLeave = false }, onYes = { confirmLeave = false; store.leaveGroupCall() })
    }
}

@Composable
private fun TileLabel(name: String, muted: Boolean, speaking: Boolean, hand: Boolean, @Suppress("UNUSED_PARAMETER") clock: Long, modifier: Modifier) {
    Row(modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        T(name, 13.sp, FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
        if (hand) Icon(Icons.Rounded.FrontHand, null, Modifier.size(18.dp), tint = Kx.warning)
        Icon(if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, null, Modifier.size(18.dp), tint = if (muted) Kx.text4 else if (speaking) Kx.accent else Kx.text)
    }
}

@Composable
private fun GroupBtn(icon: ImageVector, label: String, bg: Color, ink: Color, onClick: () -> Unit) {
    Column(Modifier.width(60.dp).kxClick(label, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(24.dp), tint = ink) }
        Text(label, style = ts(11.sp, color = Kx.text2), maxLines = 1, softWrap = false, modifier = Modifier.wrapContentWidth(unbounded = true))
    }
}
