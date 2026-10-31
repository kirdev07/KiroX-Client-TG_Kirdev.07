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

// Media inside message bubbles. Files download on tap (videos, music, documents) or by themselves
// when small (GIFs, stickers, previews); playback uses one Media3 player per visible item.

/** Auto-download limits for the screen; none when not provided (previews, tests). */
val LocalAutoDownload = androidx.compose.runtime.staticCompositionLocalOf { com.kirox.app.telegram.AutoDownloadRule() }

/** «Сохранять в галерею» is on for the open chat's type. */
val LocalSaveToGallery = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Copies an incoming photo or video to the gallery once it is on disk; each file only once. */
@Composable
internal fun SaveToGalleryEffect(message: MessageUi) {
    val (file, video) = when (message.kind) {
        MessageKind.Photo -> (message.photo ?: return) to false
        MessageKind.Video -> (message.media?.file ?: return) to true
        else -> return
    }
    val context = LocalContext.current
    val path = rememberFileState(file).path ?: return
    LaunchedEffect(path) {
        val media = (context.applicationContext as com.kirox.app.KiroXApplication).telegram.media
        if (media.markSaved(path)) saveToGallery(context, path, video)
    }
}

/** GIFs up to this size download and loop by themselves, like in Telegram. */
private const val AUTO_ANIMATION_BYTES = 8L * 1024 * 1024

/** Download state of [file], or an empty state outside a chat screen (previews, tests). */
@Composable
private fun rememberFileState(file: TelegramFileRef): TelegramFileState {
    val files = LocalTelegramFiles.current
    val flow = remember(file.id, files) { files?.state(file) ?: MutableStateFlow(TelegramFileState(path = file.localPath)) }
    return flow.collectAsState().value
}

private fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun mediaAspect(media: TelegramMedia): Float =

    if (media.width > 0 && media.height > 0) (media.width.toFloat() / media.height).coerceIn(0.5f, 2.2f) else 16f / 9f

/** Round button over media: download arrow, progress with cancel, or play. */
@Composable
private fun MediaStateBadge(state: TelegramFileState, playIcon: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
        when {
            state.path != null -> if (playIcon) Text("▶", color = Color.White, fontSize = 16.sp)
            state.downloading -> {
                CircularProgressIndicator(progress = { state.fraction.coerceAtLeast(0.02f) }, modifier = Modifier.size(38.dp), color = Color.White, strokeWidth = 2.dp, trackColor = Color(0x33FFFFFF))
                Text("×", color = Color.White, fontSize = 18.sp)
            }
            else -> Text("↓", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Preview frame of a video or GIF in a stable aspect box. */
@Composable
private fun MediaFrame(thumbnail: TelegramFileRef?, aspect: Float, content: @Composable () -> Unit) {
    val width = with(LocalDensity.current) { 280.dp.roundToPx() }
    val bitmap = rememberTelegramBitmap(thumbnail, width, priority = 16)
    Box(
        Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(12.dp)).background(Fusion.colors.surface3),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        content()
    }
}

@Composable
internal fun VideoMessage(media: TelegramMedia, thumbnail: TelegramFileRef?) {
    val files = LocalTelegramFiles.current
    val state = rememberFileState(media.file)
    var playing by remember(media.file.id) { mutableStateOf<android.net.Uri?>(null) }
    val rule = LocalAutoDownload.current
    LaunchedEffect(media.file.id, rule) { if (state.path == null && !state.downloading && media.size in 1..rule.maxVideoBytes) files?.download(media.file) }
    // A tap plays at once: a saved video from the phone, otherwise online while it downloads.
    Box(Modifier.clickable {
        val path = state.path
        playing = if (path != null) android.net.Uri.fromFile(File(path)) else {
            files?.download(media.file)
            com.kirox.app.media.TelegramStream.uri(media.file.id)
        }
    }) {
        MediaFrame(thumbnail, mediaAspect(media)) { MediaStateBadge(state, playIcon = true) }
        val label = listOf(formatDuration(media.duration), if (state.downloading) formatFileSize(state.downloaded) + " / " + formatFileSize(state.total) else formatFileSize(media.size))
            .filter { it.isNotBlank() }.joinToString(" · ")
        Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp))
    }
    playing?.let { uri -> FullscreenVideo(uri, key = "tg:${media.file.id}") { playing = null } }
}

@Composable
internal fun AnimationMessage(media: TelegramMedia, thumbnail: TelegramFileRef?, preview: Boolean = false) {
    val files = LocalTelegramFiles.current
    val state = rememberFileState(media.file)
    // A preview (sticker panel tile) shows only the frame: no download, no player, no own taps,
    // so a grid of GIFs does not open a decoder per tile and a tap reaches the tile (send).
    val gifs = LocalAutoDownload.current.gifs
    if (!preview) LaunchedEffect(media.file.id, files, gifs) {
        if (gifs && state.path == null && !state.downloading && media.size in 1..AUTO_ANIMATION_BYTES) files?.download(media.file)
    }
    Box(if (preview) Modifier else Modifier.clickable(enabled = state.path == null) { if (state.downloading) files?.cancel(media.file) else files?.download(media.file) }) {
        MediaFrame(thumbnail, mediaAspect(media)) {
            val path = state.path
            if (preview) Unit
            else if (path != null) LoopingVideo(path, Modifier.fillMaxSize()) else MediaStateBadge(state, playIcon = false)
        }
        Text("GIF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

/** Round video message: tap downloads it, then plays and stops it inside the circle with sound. */
@Composable
internal fun VideoNoteMessage(media: TelegramMedia, thumbnail: TelegramFileRef?) {
    val files = LocalTelegramFiles.current
    val state = rememberFileState(media.file)
    var playing by remember(media.file.id) { mutableStateOf(false) }
    var wantPlay by remember(media.file.id) { mutableStateOf(false) }
    LaunchedEffect(state.path, wantPlay) { if (wantPlay && state.path != null) { playing = true; wantPlay = false } }
    val size = 220.dp
    val bitmap = rememberTelegramBitmap(thumbnail, with(LocalDensity.current) { size.roundToPx() }, priority = 16)
    Box(
        Modifier.size(size).clip(CircleShape).background(Fusion.colors.surface3).clickable {
            when {
                state.path != null -> playing = !playing
                state.downloading -> { files?.cancel(media.file); wantPlay = false }
                else -> { files?.download(media.file); wantPlay = true }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        val path = state.path
        if (playing && path != null) OneShotVideo(path, Modifier.fillMaxSize()) { playing = false }
        else {
            bitmap?.let { Image(it, contentDescription = "Видеосообщение", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            MediaStateBadge(state, playIcon = true)
            Text(formatDuration(media.duration), color = Color.White, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

/** Music file: plays through the shared voice player once downloaded. */
@Composable
internal fun AudioMessage(media: TelegramMedia) {
    val context = LocalContext.current
    val files = LocalTelegramFiles.current
    val state = rememberFileState(media.file)
    val playingPath by VoicePlayer.playingPath.collectAsState()
    val current by VoicePlayer.track.collectAsState()
    // The chat's music in order: a tap plays from here on, like a playlist.
    val chatTracks = LocalChatTracks.current
    val isCurrent = current?.file?.id == media.file.id
    val playing = isCurrent && playingPath != null
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable {
            when {
                state.downloading && !isCurrent && current == null -> files?.cancel(media.file)
                else -> {
                    val queue = chatTracks.ifEmpty { listOf(media.toTrack()) }
                    val index = queue.indexOfFirst { it.file.id == media.file.id }.takeIf { it >= 0 }
                    if (index != null) VoicePlayer.playQueue(context, queue, index) else VoicePlayer.playQueue(context, listOf(media.toTrack()), 0)
                }
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Fusion.colors.surface3), contentAlignment = Alignment.Center) {
            when {
                playing -> Text("❚❚", color = Fusion.colors.textPrimary, fontSize = 13.sp)
                state.path != null -> Text("▶", color = Fusion.colors.textPrimary, fontSize = 14.sp)
                state.downloading -> CircularProgressIndicator(progress = { state.fraction.coerceAtLeast(0.02f) }, modifier = Modifier.size(34.dp), color = Fusion.colors.textPrimary, strokeWidth = 2.dp)
                else -> IconGlyph(KiroXIcon.Music, "Музыка", Modifier.size(20.dp), Fusion.colors.textPrimary)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.widthIn(max = 220.dp).then(if (isCurrent) Modifier.clickable { MusicPlayerUi.open() } else Modifier)) {
            Text(media.title.ifBlank { "Аудио" }, color = if (isCurrent) Fusion.colors.accent else Fusion.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val details = listOf(media.performer, formatDuration(media.duration), formatFileSize(media.size)).filter { it.isNotBlank() }.joinToString(" · ")
            Text(details, color = Fusion.colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Document: tap downloads it (tap again cancels), then opens it in another app. */
@Composable
internal fun DocumentMessage(media: TelegramMedia, onNotice: (String) -> Unit) {
    val files = LocalTelegramFiles.current
    var viewing by remember(media.file.id) { mutableStateOf<String?>(null) }
    viewing?.let { path -> com.kirox.app.ui.files.FileViewer(path, media.title.ifBlank { java.io.File(path).name }, media.mimeType) { viewing = null } }
    val state = rememberFileState(media.file)
    val rule = LocalAutoDownload.current
    LaunchedEffect(media.file.id, rule) { if (state.path == null && !state.downloading && media.size in 1..rule.maxOtherBytes) files?.download(media.file) }
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable {
            val path = state.path
            when {
                path != null -> viewing = path
                state.downloading -> files?.cancel(media.file)
                else -> files?.download(media.file)
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Fusion.colors.surface3), contentAlignment = Alignment.Center) {
            when {
                state.downloading -> {
                    CircularProgressIndicator(progress = { state.fraction.coerceAtLeast(0.02f) }, modifier = Modifier.size(34.dp), color = Fusion.colors.textPrimary, strokeWidth = 2.dp)
                    Text("×", color = Fusion.colors.textPrimary, fontSize = 15.sp)
                }
                state.path != null -> IconGlyph(KiroXIcon.File, "Файл", Modifier.size(20.dp), Fusion.colors.textPrimary)
                else -> Text("↓", color = Fusion.colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.widthIn(max = 220.dp)) {
            Text(media.title.ifBlank { "Файл" }, color = Fusion.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val size = if (state.downloading) "${formatFileSize(state.downloaded)} из ${formatFileSize(state.total)}" else formatFileSize(media.size)
            Text(size, color = Fusion.colors.textSecondary, fontSize = 12.sp)
        }
    }
}

/**
 * Sticker: static WEBP as an image, TGS through Lottie, WEBM as a muted looping video. Animated files
 * are small and download by themselves; until then the preview is shown.
 */
@Composable
internal fun StickerMessage(media: TelegramMedia, thumbnail: TelegramFileRef?, size: androidx.compose.ui.unit.Dp = 160.dp, animate: Boolean = true) {
    val files = LocalTelegramFiles.current
    val state = rememberFileState(media.file)
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    LaunchedEffect(media.file.id, files, animate) { if (animate && state.path == null && !state.downloading) files?.download(media.file, priority = 12) }
    val path = state.path
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            animate && path != null && media.lottieSticker -> LottieSticker(path, Modifier.fillMaxSize())
            animate && path != null && media.animatedSticker -> LoopingVideo(path, Modifier.fillMaxSize())
            else -> {
                val bitmap = rememberTelegramBitmap(if (media.animatedSticker) thumbnail else media.file, sizePx, priority = 16)
                if (bitmap != null) Image(bitmap, contentDescription = "Стикер ${media.title}", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                else Text(media.title.ifBlank { "Стикер" }, fontSize = (size.value / 3).sp)
            }
        }
    }
}

/** TGS sticker: gzip-compressed Lottie JSON, played in a loop. */
@Composable
private fun LottieSticker(path: String, modifier: Modifier) {
    val json by androidx.compose.runtime.produceState<String?>(null, path) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { java.util.zip.GZIPInputStream(File(path).inputStream()).bufferedReader().use { it.readText() } }.getOrNull()
        }
    }
    val source = json ?: return
    val composition by com.airbnb.lottie.compose.rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.JsonString(source), cacheKey = path)
    com.airbnb.lottie.compose.LottieAnimation(composition, iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever, modifier = modifier)
}
