package com.kirox.app.ui.chat

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import com.kirox.app.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.core.content.edit
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.telegram.TelegramFileState
import com.kirox.app.telegram.TelegramMedia
import com.kirox.app.ui.components.LocalTelegramFiles
import com.kirox.app.ui.components.rememberTelegramBitmap
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.icons.KiroXIcon as IconGlyph
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

// Full-screen photo and video viewers and the ExoPlayer helpers behind videos, GIFs and video notes.

/** Photo opened full screen: pinch to zoom, drag when zoomed, double tap to reset, tap × to close. */
@Composable
internal fun FullscreenPhoto(file: TelegramFileRef, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // The dialog window may report 0 before its first layout.
        val width = windowWidthDp().coerceAtLeast(320)
        val bitmap = rememberTelegramBitmap(file, with(LocalDensity.current) { (width * 2).dp.roundToPx() }, priority = 32)
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (bitmap != null) Image(
                bitmap, contentDescription = "Фотография", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero }) }
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
            ) else CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp), color = Color.White, strokeWidth = 2.dp)
            // Approved media board: back arrow at the top left over a soft shade.
            Box(Modifier.fillMaxWidth().height(120.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent))))
            Box(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp).size(48.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                com.kirox.app.ui.icons.KiroXIcon(com.kirox.app.ui.icons.KiroXIcon.Back, "Закрыть", Modifier.size(24.dp), Color.White)
            }
            // Approved media board: round actions at the bottom — save to Downloads and open in another app.
            val files = com.kirox.app.ui.components.LocalTelegramFiles.current
            val path by remember(file.id, files) {
                if (files != null) files.observe(file, 32) else kotlinx.coroutines.flow.MutableStateFlow(file.localPath)
            }.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = rememberCoroutineScope()
            var status by remember { mutableStateOf<String?>(null) }
            path?.let { local ->
                Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ViewerAction(com.kirox.app.ui.icons.KiroXIcon.Storage, "Сохранить") {
                        scope.launch { status = if (saveToDownloads(context, local, "photo.jpg", "image/jpeg")) "Сохранено в Загрузки/kiroX" else "Не удалось сохранить" }
                    }
                    ViewerAction(com.kirox.app.ui.icons.KiroXIcon.Forward, "Поделиться") {
                        scope.launch { if (!shareFile(context, local, "photo.jpg", "image/jpeg")) status = "Нет приложения, чтобы поделиться" }
                    }
                    ViewerAction(com.kirox.app.ui.icons.KiroXIcon.Link, "Открыть в…") {
                        scope.launch { if (!openFile(context, local, "photo.jpg", "image/jpeg")) status = "Нет приложения для открытия" }
                    }
                }
            }
            status?.let { text ->
                Text(text, color = Color.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x99000000)).padding(horizontal = 14.dp, vertical = 8.dp))
                LaunchedEffect(text) { kotlinx.coroutines.delay(2200.milliseconds); status = null }
            }
        }
    }
}

@Composable
private fun ViewerAction(icon: com.kirox.app.ui.icons.KiroXIcon, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(Color(0x40FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { com.kirox.app.ui.icons.KiroXIcon(icon, label, Modifier.size(22.dp), Color.White) }
}

@Composable
private fun CloseButton(onClose: () -> Unit, modifier: Modifier) {
    // Back arrow at the top left, as in the approved media board.
    Box(modifier.statusBarsPadding().padding(6.dp).size(44.dp).clip(CircleShape).background(Color(0x66000000)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        com.kirox.app.ui.icons.KiroXIcon(com.kirox.app.ui.icons.KiroXIcon.Back, "Закрыть", Modifier.size(22.dp), Color.White)
    }
}

/** Where each long video was left, so it opens there next time. */
private object VideoPositions {
    private const val PREFS = "video_positions"
    private const val MIN_MS = 60_000L
    fun get(context: android.content.Context, key: String): Long = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getLong(key, 0L)
    fun put(context: android.content.Context, key: String, position: Long, duration: Long) {
        if (duration < MIN_MS) return
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit {
            if (position < 5_000 || position > duration - 10_000) remove(key) else putLong(key, position)
        }
    }
}

/**
 * Video opened full screen with the Media3 controls (seek, pause, speed in ⚙). Double tap on the left
 * or right half skips 10 seconds; the top bar turns the screen, switches fit / fill and repeat. Long
 * videos open where they were left. [source] is a file or an online [com.kirox.app.media.TelegramStream] address.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun FullscreenVideo(source: Uri, key: String = source.toString(), onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val player = rememberPlayer(source, loop = false, muted = false, autoPlay = true, startAt = remember(key) { VideoPositions.get(context, key) })
        var view by remember { mutableStateOf<PlayerView?>(null) }
        var fill by remember { mutableStateOf(false) }
        var loop by remember { mutableStateOf(false) }
        var landscape by remember { mutableStateOf(false) }
        var controls by remember { mutableStateOf(true) }
        var skipHint by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
        var buffering by remember { mutableStateOf(false) }
        LaunchedEffect(skipHint) { if (skipHint != null) { kotlinx.coroutines.delay(700); skipHint = null } }
        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { buffering = state == Player.STATE_BUFFERING }
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
                VideoPositions.put(context, key, player.currentPosition, player.duration)
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        BackHandler(onBack = onClose)
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true; this.player = player; keepScreenOn = true
                        setShowNextButton(false); setShowPreviousButton(false)
                        setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility -> controls = visibility == android.view.View.VISIBLE })
                        view = this
                    }
                },
                update = { it.resizeMode = if (fill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT },
                modifier = Modifier.fillMaxSize()
            )
            // Double tap: ±10 s on that side; single tap shows or hides the controls.
            Row(Modifier.fillMaxSize().padding(vertical = 96.dp)) {
                listOf(false, true).forEach { forward ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().pointerInput(player) {
                            detectTapGestures(
                                onTap = { view?.let { if (it.isControllerFullyVisible) it.hideController() else it.showController() } },
                                onDoubleTap = {
                                    player.seekTo((player.currentPosition + if (forward) 10_000 else -10_000).coerceIn(0L, player.duration.coerceAtLeast(0L)))
                                    skipHint = forward to ((skipHint?.takeIf { h -> h.first == forward }?.second ?: 0) + 10)
                                }
                            )
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        skipHint?.takeIf { it.first == forward }?.let { (_, seconds) ->
                            Text(if (forward) "+$seconds с ▶▶" else "◀◀ −$seconds с", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x66000000)).padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }
            }
            if (buffering) CircularProgressIndicator(Modifier.align(Alignment.Center).size(44.dp), color = Color.White, strokeWidth = 3.dp)
            if (controls) Row(
                Modifier.fillMaxWidth().align(Alignment.TopStart).background(Color(0x55000000)).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CloseButton(onClose, Modifier)
                Spacer(Modifier.weight(1f))
                VideoBarButton(if (loop) "Повтор: вкл" else "Повтор") { loop = !loop; player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }
                VideoBarButton(if (fill) "Вписать" else "Заполнить") { fill = !fill }
                VideoBarButton("Повернуть") {
                    landscape = !landscape
                    activity?.requestedOrientation = if (landscape) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
    }
}

@Composable
private fun VideoBarButton(label: String, onClick: () -> Unit) {
    Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp))
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Muted endless playback, for GIFs. */
@OptIn(UnstableApi::class)
@Composable
internal fun LoopingVideo(path: String, modifier: Modifier) {
    val player = rememberPlayer(Uri.fromFile(File(path)), loop = true, muted = true, autoPlay = true)
    AndroidView(
        factory = { context -> PlayerView(context).apply { useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; this.player = player } },
        modifier = modifier
    )
}

/** One play-through with sound, for video notes; [onEnd] when it finishes. */
@OptIn(UnstableApi::class)
@Composable
internal fun OneShotVideo(path: String, modifier: Modifier, onEnd: () -> Unit) {
    val player = rememberPlayer(Uri.fromFile(File(path)), loop = false, muted = false, autoPlay = true)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) onEnd() }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    AndroidView(
        factory = { context -> PlayerView(context).apply { useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; this.player = player } },
        modifier = modifier
    )
}

/**
 * ExoPlayer for one local file, released when it leaves the screen. Players with sound take audio
 * focus (music pauses) and stop the voice player; muted GIF players never touch audio focus.
 */
@Composable
private fun rememberPlayer(source: Uri, loop: Boolean, muted: Boolean, autoPlay: Boolean, startAt: Long = 0L): ExoPlayer {
    val context = LocalContext.current
    val player = remember(source) {
        if (!muted) VoicePlayer.stop()
        ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(com.kirox.app.media.TelegramStream.mediaSourceFactory(context))
            .build().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                /* handleAudioFocus = */ !muted
            )
            volume = if (muted) 0f else 1f
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            setMediaItem(MediaItem.fromUri(source), startAt)
            playWhenReady = autoPlay
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    // Nothing plays on with KiroX in the background; silent GIF loops carry on when it returns.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) { player.pause() }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_START) { if (loop && muted && autoPlay) player.play() }
    return player
}
