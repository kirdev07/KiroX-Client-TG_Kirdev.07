package com.kirox.app.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import com.kirox.app.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/** Telegram limits video notes to one minute. */
private const val MAX_NOTE_MS = 60_000L
/** Side of the square video sent; Telegram's own clients record at about this size. */
internal const val NOTE_SIDE = 384

/** A finished video note ready to send. */
class RecordedVideoNote(val file: File, val durationSeconds: Int, val length: Int)

/**
 * The camera side of a round video message (CameraX preview + recording, one minute, cropped square), shared by the
 * old full-screen recorder below and the v3 one (ui/v3 KxRoundRecorder, wave6). Created by [rememberVideoNoteCapture].
 */
class VideoNoteCapture internal constructor(val preview: PreviewView) {
    var front by mutableStateOf(true)
        internal set
    var recording by mutableStateOf<Recording?>(null)
        internal set
    var elapsedMs by mutableLongStateOf(0L)
        internal set
    var processing by mutableStateOf(false)
        internal set
    val isRecording: Boolean get() = recording != null
    val maxMs: Long get() = MAX_NOTE_MS
    internal var startImpl: () -> Unit = {}
    internal var finishImpl: (Boolean) -> Unit = {}
    /** Starts recording (stops any voice playback first). */
    fun start() = startImpl()
    /** Stops: [canceled] drops the clip, otherwise it is cropped and handed to onDone. */
    fun finish(canceled: Boolean) = finishImpl(canceled)
    /** Front/back camera; only before recording. */
    fun flip() { if (recording == null && !processing) front = !front }
}

/** Binds the camera while composed; leaving composition any other way than finish(false) drops the clip. */
@SuppressLint("MissingPermission") // The caller asks for CAMERA and RECORD_AUDIO before opening it.
@Composable
fun rememberVideoNoteCapture(onDone: (RecordedVideoNote) -> Unit, onCancel: () -> Unit, onError: (String) -> Unit): VideoNoteCapture {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { VideoNoteCapture(PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE }) }
    var startedAt by remember { mutableLongStateOf(0L) }
    val capture = remember {
        VideoCapture.Builder(Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD, androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))).build())
            .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
            .build()
    }
    val raw = remember { File(context.cacheDir, "notes").apply { mkdirs() }.let { File(it, "note_raw_${System.currentTimeMillis()}.mp4") } }
    var discard by remember { mutableStateOf(false) }
    val done by rememberUpdatedState(onDone)
    val cancel by rememberUpdatedState(onCancel)
    val fail by rememberUpdatedState(onError)

    DisposableEffect(state.front) {
        // Both the listener and onDispose run on the main thread: a listener that comes after the
        // screen closed must not bind the camera to the activity (it would stay on).
        var disposed = false
        var bound: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed) return@addListener
            val provider = runCatching { future.get() }.getOrNull() ?: return@addListener fail("Камера недоступна")
            bound = provider
            val selector = if (state.front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val surface = Preview.Builder().build().also { it.setSurfaceProvider(state.preview.surfaceProvider) }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, surface, capture)
            }.onFailure { fail("Не удалось открыть камеру") }
        }, ContextCompat.getMainExecutor(context))
        onDispose { disposed = true; runCatching { bound?.unbindAll() } }
    }
    // Leaving the recorder in any other way than «Отправить» drops the clip.
    DisposableEffect(Unit) { onDispose { if (!state.processing) { discard = true; state.recording?.close(); raw.delete() } } }

    state.finishImpl = finish@{ canceled ->
        val active = state.recording ?: return@finish
        discard = canceled
        if (!canceled) state.processing = true
        active.stop()
        state.recording = null
    }
    state.startImpl = begin@{
        if (state.recording != null || state.processing) return@begin
        VoicePlayer.stop()
        discard = false
        startedAt = System.currentTimeMillis()
        state.elapsedMs = 0
        state.recording = capture.output.prepareRecording(context, FileOutputOptions.Builder(raw).build())
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event !is VideoRecordEvent.Finalize) return@start
                val durationMs = event.recordingStats.recordedDurationNanos / 1_000_000
                when {
                    discard -> { raw.delete(); cancel() }
                    event.hasError() && event.error != VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED -> { raw.delete(); state.processing = false; fail("Запись прервалась") }
                    durationMs < 1_000 -> { raw.delete(); state.processing = false; fail("Слишком короткое видеосообщение") }
                    else -> cropToSquare(context, raw, durationMs, onDone = { note -> state.processing = false; done(note) }, onError = { state.processing = false; fail(it) })
                }
            }
    }

    LaunchedEffect(state.recording) {
        while (state.recording != null) {
            state.elapsedMs = System.currentTimeMillis() - startedAt
            if (state.elapsedMs >= MAX_NOTE_MS) state.finish(canceled = false)
            delay(100.milliseconds)
        }
    }
    return state
}

/**
 * Full-screen recorder for round video messages (old design): circular camera preview, tap the button to start
 * and again to send, flip between cameras before recording, cancel at any time. Recording stops by
 * itself after a minute. The clip is cropped to a [NOTE_SIDE] square before sending.
 */
@Composable
internal fun VideoNoteRecorder(onDone: (RecordedVideoNote) -> Unit, onCancel: () -> Unit, onError: (String) -> Unit) {
    val note = rememberVideoNoteCapture(onDone, onCancel, onError)
    val recording = note.recording
    val processing = note.processing
    val elapsed = note.elapsedMs
    fun finish(canceled: Boolean) = note.finish(canceled)

    Dialog(onDismissRequest = { if (recording != null) finish(canceled = true) else if (!processing) onCancel() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(contentAlignment = Alignment.Center) {
                // Ring fills as the minute runs out.
                CircularProgressIndicator(
                    progress = { (elapsed.toFloat() / MAX_NOTE_MS).coerceIn(0f, 1f) },
                    modifier = Modifier.size(300.dp), color = Fusion.colors.danger, strokeWidth = 4.dp, trackColor = Color(0x33FFFFFF)
                )
                AndroidView({ note.preview }, Modifier.size(284.dp).clip(CircleShape))
                if (processing) CircularProgressIndicator(Modifier.size(36.dp), color = Color.White, strokeWidth = 3.dp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                when {
                    processing -> "Обработка…"
                    recording != null -> "%d:%02d".format(elapsed / 60_000, elapsed / 1000 % 60)
                    else -> "Нажмите, чтобы записать"
                },
                color = Color.White, fontSize = 16.sp
            )
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Отмена", color = Color.White, fontSize = 15.sp,
                    modifier = Modifier.clip(CircleShape).clickable(enabled = !processing) { if (recording != null) finish(canceled = true) else onCancel() }.padding(12.dp)
                )
                Box(
                    Modifier.size(76.dp).border(4.dp, Color.White, CircleShape).padding(8.dp).clip(CircleShape)
                        .background(if (recording != null) Fusion.colors.danger else Color.White)
                        .clickable(enabled = !processing, onClickLabel = if (recording != null) "Отправить" else "Записать") {
                            if (recording != null) finish(canceled = false) else note.start()
                        }
                )
                Text(
                    "↻", color = if (recording == null && !processing) Color.White else Color(0x55FFFFFF), fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).clickable(enabled = recording == null && !processing, onClickLabel = "Сменить камеру") { note.flip() }.padding(12.dp)
                )
            }
        }
    }
}

/** Crops the recording to a centered square and re-encodes it as H.264, as video notes must be. */
@OptIn(UnstableApi::class)
private fun cropToSquare(context: Context, raw: File, durationMs: Long, onDone: (RecordedVideoNote) -> Unit, onError: (String) -> Unit) {
    val out = File(raw.parentFile, raw.name.replace("note_raw_", "note_"))
    val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(raw)))
        .setEffects(Effects(emptyList(), listOf(Presentation.createForWidthAndHeight(NOTE_SIDE, NOTE_SIDE, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))))
        .build()
    val transformer = Transformer.Builder(context.applicationContext)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                raw.delete()
                onDone(RecordedVideoNote(out, ((durationMs + 500) / 1000).toInt().coerceIn(1, 60), NOTE_SIDE))
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                raw.delete()
                out.delete()
                onError("Не удалось подготовить видеосообщение")
            }
        })
        .build()
    transformer.start(item, out.absolutePath)
}
