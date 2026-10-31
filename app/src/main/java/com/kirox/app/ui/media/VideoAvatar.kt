package com.kirox.app.ui.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.kirox.app.KiroXApplication
import com.kirox.app.i18n.Text
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/** Telegram plays video avatars up to 10 seconds, square, 800 px. */
private const val AVATAR_MS = 10_000L
private const val AVATAR_SIDE = 800

/** Cuts [source] from [startMs] for up to 10 s, crops it to a square without sound, H.264. Null on failure. */
@OptIn(UnstableApi::class)
private suspend fun makeAvatar(context: Context, source: Uri, startMs: Long, durationMs: Long): File? = suspendCancellableCoroutine { done ->
    val out = File(File(context.cacheDir, "avatar").apply { mkdirs() }, "avatar_${System.currentTimeMillis()}.mp4")
    val clip = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(startMs).setEndPositionMs(minOf(startMs + AVATAR_MS, durationMs)).build()
    val item = EditedMediaItem.Builder(MediaItem.Builder().setUri(source).setClippingConfiguration(clip).build())
        .setRemoveAudio(true)
        .setEffects(Effects(emptyList(), listOf(Presentation.createForWidthAndHeight(AVATAR_SIDE, AVATAR_SIDE, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))))
        .build()
    val transformer = Transformer.Builder(context.applicationContext)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) { if (done.isActive) done.resume(out) }
            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                out.delete(); if (done.isActive) done.resume(null)
            }
        })
        .build()
    transformer.start(item, out.absolutePath)
    done.invokeOnCancellation { runCatching { transformer.cancel() }; out.delete() }
}

/**
 * «Видео на аватар»: choose which 10 seconds of the video to use (a frame of it is shown), then
 * kiroX crops it to a square, removes the sound and sets it as the profile video.
 */
@Composable
fun VideoAvatarDialog(source: Uri, onDismiss: () -> Unit, onNotice: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val users = remember { (context.applicationContext as KiroXApplication).telegram.users }
    var durationMs by remember { mutableLongStateOf(0L) }
    var start by remember { mutableFloatStateOf(0f) }
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(source) {
        durationMs = withContext(Dispatchers.IO) {
            runCatching { MediaMetadataRetriever().run { try { setDataSource(context, source); extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L } finally { release() } } }.getOrDefault(0L)
        }
    }
    val maxStart = (durationMs - AVATAR_MS).coerceAtLeast(0L)
    val startMs = (start * maxStart).toLong()
    LaunchedEffect(source, startMs) {
        kotlinx.coroutines.delay(150)
        frame = withContext(Dispatchers.IO) {
            runCatching { MediaMetadataRetriever().run { try { setDataSource(context, source); getFrameAtTime(startMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.asImageBitmap() } finally { release() } } }.getOrNull()
        }
    }
    fun time(ms: Long) = "%d:%02d".format(ms / 60_000, ms / 1000 % 60)
    AlertDialog(
        onDismissRequest = { if (busy == null) onDismiss() },
        containerColor = Fusion.colors.surface1,
        title = { Text("Видео на аватар", color = Fusion.colors.textPrimary) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(180.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    frame?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } ?: CircularProgressIndicator(color = Fusion.colors.accent)
                }
                Spacer(Modifier.height(12.dp))
                if (durationMs == 0L) Text("Читаю видео…", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                else {
                    Text("С ${time(startMs)} до ${time(minOf(startMs + AVATAR_MS, durationMs))}", color = Fusion.colors.textPrimary, fontSize = 14.sp)
                    if (maxStart > 0) Slider(start, { start = it }, enabled = busy == null,
                        colors = SliderDefaults.colors(thumbColor = Fusion.colors.accent, activeTrackColor = Fusion.colors.accent, inactiveTrackColor = Fusion.colors.surface3))
                    Text("Будет квадрат без звука, до 10 секунд — так Telegram показывает видео в профиле.", color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 16.sp)
                }
                busy?.let { Text(it, color = Fusion.colors.accent, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = busy == null && durationMs > 0, onClick = {
                busy = "Готовлю видео…"
                scope.launch {
                    val file = makeAvatar(context, source, startMs, durationMs)
                    if (file == null) { busy = null; onNotice("Не удалось подготовить видео"); return@launch }
                    busy = "Загружаю…"
                    runCatching { users.setProfileVideo(file.absolutePath) }
                        .onSuccess { onNotice("Видео-аватар установлен"); onDismiss() }
                        .onFailure { busy = null; onNotice("Telegram не принял видео") }
                }
            }) { Text("Поставить", color = Fusion.colors.accent) }
        },
        dismissButton = { TextButton(enabled = busy == null, onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}
