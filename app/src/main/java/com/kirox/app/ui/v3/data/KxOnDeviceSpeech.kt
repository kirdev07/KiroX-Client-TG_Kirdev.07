package com.kirox.app.ui.v3.data

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kirox.app.telegram.TelegramClient
import com.kirox.app.ui.v3.model.KxFeatureLangs
import com.kirox.app.ui.v3.model.KxTextResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.nio.ByteOrder
import kotlin.coroutines.resume

/*
 * features: «В текст» for voice notes and round videos without Telegram Premium. The downloaded OGG/Opus (or the
 * MP4 audio of a round video) is decoded on the phone (MediaExtractor + MediaCodec) to 16 kHz mono PCM and fed to
 * the system's on-device recognizer (SpeechRecognizer.createOnDeviceSpeechRecognizer with EXTRA_AUDIO_SOURCE,
 * Android 13+). Nothing is sent anywhere. Without an offline model for the language the user gets the system's
 * download (triggerModelDownload) from the chat layer.
 */

/** Pure audio helpers (unit-tested). */
internal object KxPcm {
    const val RATE = 16_000

    /** Interleaved [channels] → mono (average). */
    fun mono(samples: ShortArray, channels: Int): ShortArray {
        if (channels <= 1) return samples
        val n = samples.size / channels
        return ShortArray(n) { i -> var s = 0; for (c in 0 until channels) s += samples[i * channels + c]; (s / channels).toShort() }
    }

    /** Linear resampling of mono PCM from [from] Hz to [to] Hz. */
    fun resample(samples: ShortArray, from: Int, to: Int = RATE): ShortArray {
        if (from == to || samples.isEmpty() || from <= 0) return samples
        val n = (samples.size.toLong() * to / from).toInt().coerceAtLeast(1)
        val step = from.toDouble() / to
        return ShortArray(n) { i ->
            val x = i * step; val a = x.toInt().coerceAtMost(samples.lastIndex); val b = (a + 1).coerceAtMost(samples.lastIndex); val f = x - a
            (samples[a] * (1 - f) + samples[b] * f).toInt().toShort()
        }
    }

    fun bytes(samples: ShortArray): ByteArray = ByteArray(samples.size * 2).also { out ->
        samples.forEachIndexed { i, s -> out[2 * i] = (s.toInt() and 0xFF).toByte(); out[2 * i + 1] = (s.toInt() shr 8 and 0xFF).toByte() }
    }

    /** The recognised text with the on-device mark. */
    fun marked(text: String) = "$text\n· распознано на телефоне"
}

object KxOnDeviceSpeech {
    /** A language whose offline model is missing (the chat layer offers the system download), BCP-47. */
    var needsModel by mutableStateOf<String?>(null)
    const val MAX_SECONDS = 15 * 60

    fun langTag(index: Int) = KxFeatureLangs.speech.getOrElse(index) { KxFeatureLangs.speech[0] }.first

    /** What the settings screen says about this phone; null = ready to try. */
    fun problem(context: Context): String? = when {
        Build.VERSION.SDK_INT < 33 -> "Нужен Android 13 или новее: старые версии не умеют распознавать речь из файла"
        !SpeechRecognizer.isOnDeviceRecognitionAvailable(context) -> "На телефоне нет офлайн-распознавания речи (сервис распознавания Google или производителя)"
        else -> null
    }

    /** Opens the system download of the offline model for [tag] (Android 13+). */
    fun downloadModel(context: Context, tag: String): String? {
        if (Build.VERSION.SDK_INT < 33) return "Нужен Android 13 или новее"
        return runCatching {
            val r = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            try { r.triggerModelDownload(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)) } finally { r.destroy() }
            null
        }.getOrElse { "Не удалось начать загрузку: скачайте язык в настройках системы → Распознавание речи" }
    }

    suspend fun transcribe(context: Context, client: TelegramClient, chatId: Long, messageId: Long, langIndex: Int, onPartial: (String) -> Unit): KxTextResult {
        val app = context.applicationContext
        problem(app)?.let { return KxTextResult(error = "$it. Telegram Premium расшифровывает на серверах Telegram.") }
        val tag = langTag(langIndex)
        // 1. The file (downloaded now if needed).
        val file = runCatching {
            when (val c = client.query(TdApi.GetMessage(chatId, messageId)).content) {
                is TdApi.MessageVoiceNote -> c.voiceNote.voice
                is TdApi.MessageVideoNote -> c.videoNote.video
                else -> null
            }
        }.getOrNull() ?: return KxTextResult(error = "Это не голосовое и не кружок")
        val path = file.local?.path?.takeIf { file.local.isDownloadingCompleted && it.isNotEmpty() }
            ?: runCatching { client.query(TdApi.DownloadFile(file.id, 16, 0, 0, true)).local?.path }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: return KxTextResult(error = "Не удалось скачать запись")
        // 2. Decoded to 16 kHz mono PCM.
        val pcm = withContext(Dispatchers.Default) { runCatching { decode(path) }.getOrNull() }
            ?: return KxTextResult(error = "Не удалось прочитать звук записи")
        if (pcm.isEmpty()) return KxTextResult(error = "В записи нет звука")
        // 3. Recognised by the system's on-device recognizer.
        return if (Build.VERSION.SDK_INT >= 33) recognize(app, pcm, tag, onPartial) else KxTextResult(error = "Нужен Android 13 или новее")
    }

    /** Whole file → 16 kHz mono PCM (at most [MAX_SECONDS]). */
    private fun decode(path: String): ShortArray? {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(path)
            val track = (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true } ?: return null
            ex.selectTrack(track)
            val format = ex.getTrackFormat(track)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var float = false
            val chunks = ArrayList<ShortArray>()
            var total = 0L
            try {
                codec.configure(format, null, null, 0); codec.start()
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                while (true) {
                    if (!inputDone) {
                        val i = codec.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val n = ex.readSampleData(codec.getInputBuffer(i)!!, 0)
                            if (n < 0) { codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                            else { codec.queueInputBuffer(i, 0, n, ex.sampleTime, 0); ex.advance() }
                        }
                    }
                    val o = codec.dequeueOutputBuffer(info, 10_000)
                    if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val f = codec.outputFormat
                        rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE); channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        float = f.containsKey(MediaFormat.KEY_PCM_ENCODING) && f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    } else if (o >= 0) {
                        val buf = codec.getOutputBuffer(o)!!.apply { position(info.offset); limit(info.offset + info.size) }.order(ByteOrder.LITTLE_ENDIAN)
                        val samples = if (float) buf.asFloatBuffer().let { fb -> ShortArray(fb.remaining()) { (fb.get(it).coerceIn(-1f, 1f) * 32767).toInt().toShort() } }
                            else buf.asShortBuffer().let { sb -> ShortArray(sb.remaining()).also { sb.get(it) } }
                        codec.releaseOutputBuffer(o, false)
                        if (samples.isNotEmpty()) { chunks += samples; total += samples.size }
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                        if (total > rate.toLong() * channels * MAX_SECONDS) break
                    }
                }
            } finally { runCatching { codec.stop() }; codec.release() }
            val all = ShortArray(total.toInt()); var at = 0
            chunks.forEach { System.arraycopy(it, 0, all, at, it.size); at += it.size }
            return KxPcm.resample(KxPcm.mono(all, channels), rate)
        } finally { ex.release() }
    }

    @RequiresApi(33)
    private suspend fun supported(context: Context, tag: String): Boolean? = withContext(Dispatchers.Main) {
        withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { c ->
                val r = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                c.invokeOnCancellation { r.destroy() }
                r.checkRecognitionSupport(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag), context.mainExecutor,
                    object : RecognitionSupportCallback {
                        override fun onSupportResult(s: RecognitionSupport) {
                            r.destroy()
                            val lang = tag.substringBefore('-')
                            fun has(l: List<String>) = l.any { it.equals(tag, true) || it.substringBefore('-').equals(lang, true) }
                            if (c.isActive) c.resume(if (has(s.installedOnDeviceLanguages)) true else if (has(s.supportedOnDeviceLanguages) || has(s.pendingOnDeviceLanguages)) false else null)
                        }
                        override fun onError(error: Int) { r.destroy(); if (c.isActive) c.resume(null) }
                    })
            }
        }
    }

    @RequiresApi(33)
    private suspend fun recognize(context: Context, pcm: ShortArray, tag: String, onPartial: (String) -> Unit): KxTextResult {
        // A language the recognizer can install but has not yet: offer the download first.
        if (supported(context, tag) == false) {
            needsModel = tag
            return KxTextResult(error = "Скачайте язык для распознавания на телефоне")
        }
        val seconds = pcm.size / KxPcm.RATE
        val timeout = (seconds * 2_000L + 30_000L).coerceAtMost(30 * 60_000L)
        val result = withTimeoutOrNull(timeout) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<KxTextResult> { c ->
                    val pipe = ParcelFileDescriptor.createPipe()
                    val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    val segments = ArrayList<String>()
                    fun text() = segments.joinToString(" ").trim()
                    fun finish(r: KxTextResult) {
                        if (!c.isActive) return
                        runCatching { recognizer.destroy() }; runCatching { pipe[0].close() }
                        c.resume(r)
                    }
                    c.invokeOnCancellation { runCatching { recognizer.destroy() }; runCatching { pipe[0].close() }; runCatching { pipe[1].close() } }
                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                        override fun onPartialResults(partial: Bundle?) {
                            val p = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                            onPartial((text() + " " + p).trim())
                        }
                        override fun onSegmentResults(segment: Bundle) {
                            segment.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { segments += it; onPartial(text()) }
                        }
                        override fun onEndOfSegmentedSession() {
                            finish(if (text().isBlank()) KxTextResult(error = "Речь не распознана") else KxTextResult(text = KxPcm.marked(text())))
                        }
                        override fun onResults(results: Bundle?) {
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { segments += it }
                            finish(if (text().isBlank()) KxTextResult(error = "Речь не распознана") else KxTextResult(text = KxPcm.marked(text())))
                        }
                        override fun onError(error: Int) {
                            when {
                                text().isNotBlank() && error in setOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_CLIENT) ->
                                    finish(KxTextResult(text = KxPcm.marked(text())))
                                error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                                    needsModel = tag; finish(KxTextResult(error = "Скачайте язык для распознавания на телефоне"))
                                }
                                else -> finish(KxTextResult(error = KxSpeechErrors.text(error)))
                            }
                        }
                    })
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pipe[0])
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, KxPcm.RATE)
                        // One session over the whole file: pauses do not end it, the end of the audio does.
                        .putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
                    recognizer.startListening(intent)
                    // The audio is written in real-time-ish chunks on a worker; closing the pipe ends the session.
                    Thread {
                        runCatching {
                            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { out ->
                                val bytes = KxPcm.bytes(pcm)
                                var at = 0
                                while (at < bytes.size) { val n = minOf(8_192, bytes.size - at); out.write(bytes, at, n); at += n }
                            }
                        }
                    }.apply { name = "kx-speech-feed"; isDaemon = true }.start()
                }
            }
        }
        return result ?: KxTextResult(error = "Распознавание заняло слишком много времени")
    }
}

/** SpeechRecognizer error codes → Russian (unit-tested). */
internal object KxSpeechErrors {
    fun text(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Речь не распознана"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Система просит разрешение «Микрофон» для распознавания (запись не ведётся)"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознавание занято, попробуйте через минуту"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Скачайте язык для распознавания на телефоне"
        SpeechRecognizer.ERROR_AUDIO -> "Не удалось передать звук распознаванию"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
            "Офлайн-распознавание недоступно на этом телефоне"
        else -> "Не удалось распознать на телефоне (код $code)"
    }
}
