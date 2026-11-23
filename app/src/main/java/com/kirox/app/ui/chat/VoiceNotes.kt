package com.kirox.app.ui.chat

import android.content.Context
import android.media.MediaRecorder
import androidx.core.content.edit
import com.kirox.app.voip.PlaybackService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Records a voice note as OGG/Opus — the format Telegram expects — with the platform MediaRecorder
 * (Android 10+). Keeps a rough level history for the waveform Telegram shows.
 */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L
    private val levels = ArrayList<Int>()

    val isRecording: Boolean get() = recorder != null
    var isPaused = false
        private set
    private var pausedAt = 0L
    private var pausedTotal = 0L

    /** Pauses recording; the time paused is not counted in the note. */
    fun pause() {
        val active = recorder ?: return
        if (isPaused || !runCatching { active.pause() }.isSuccess) return
        isPaused = true
        pausedAt = android.os.SystemClock.elapsedRealtime()
    }

    fun resume() {
        val active = recorder ?: return
        if (!isPaused || !runCatching { active.resume() }.isSuccess) return
        pausedTotal += android.os.SystemClock.elapsedRealtime() - pausedAt
        isPaused = false
    }
    /** Recorded time, without pauses. */
    val elapsedMs: Long get() {
        if (startedAt == 0L) return 0
        val now = if (isPaused) pausedAt else android.os.SystemClock.elapsedRealtime()
        return now - startedAt - pausedTotal
    }

    fun start(): Boolean {
        stop(discard = true)
        val target = File(context.cacheDir, "voice").apply { mkdirs() }.let { File(it, "voice_${System.currentTimeMillis()}.ogg") }
        val created = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return runCatching {
            created.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            created.setOutputFormat(MediaRecorder.OutputFormat.OGG)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            created.setAudioSamplingRate(48_000)
            created.setAudioEncodingBitRate(32_000)
            created.setAudioChannels(1)
            created.setOutputFile(target.path)
            created.prepare()
            created.start()
            recorder = created
            file = target
            startedAt = android.os.SystemClock.elapsedRealtime()
            isPaused = false
            pausedTotal = 0
            levels.clear()
            true
        }.getOrElse {
            created.release()
            target.delete()
            false
        }
    }

    /** Samples the input level (call ~10 times a second while recording). */
    fun sampleLevel(): Float {
        if (isPaused) return 0f
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        levels += amplitude
        return (amplitude / 32767f).coerceIn(0f, 1f)
    }

    /** Stops recording; returns the file and duration, or null when discarded or too short. */
    fun stop(discard: Boolean = false): Recording? {
        val active = recorder ?: return null
        val durationMs = elapsedMs
        recorder = null
        startedAt = 0
        isPaused = false
        pausedTotal = 0
        val ok = runCatching { active.stop() }.isSuccess
        active.release()
        val out = file
        file = null
        if (discard || !ok || out == null || durationMs < MIN_DURATION_MS) {
            out?.delete()
            return null
        }
        return Recording(out, (durationMs / 1000).toInt().coerceAtLeast(1), waveform())
    }

    /** Telegram's waveform: 100 five-bit samples packed into bytes. */
    private fun waveform(): ByteArray {
        if (levels.isEmpty()) return ByteArray(0)
        val count = 100
        val peak = levels.maxOrNull()?.coerceAtLeast(1) ?: 1
        val samples = IntArray(count) { i ->
            val from = i * levels.size / count
            val to = ((i + 1) * levels.size / count).coerceAtLeast(from + 1).coerceAtMost(levels.size)
            val max = levels.subList(from, to).maxOrNull() ?: 0
            (max * 31 / peak).coerceIn(0, 31)
        }
        val bytes = ByteArray((count * 5 + 7) / 8)
        samples.forEachIndexed { i, value ->
            val bit = i * 5
            val index = bit / 8
            val shift = bit % 8
            val packed = value shl shift
            bytes[index] = (bytes[index].toInt() or (packed and 0xFF)).toByte()
            if (index + 1 < bytes.size) bytes[index + 1] = (bytes[index + 1].toInt() or (packed shr 8)).toByte()
        }
        return bytes
    }

    class Recording(val file: File, val durationSeconds: Int, val waveform: ByteArray)

    private companion object { const val MIN_DURATION_MS = 700L }
}

/** A music file in the player's queue; it is downloaded when its turn comes. */
data class Track(
    val file: com.kirox.app.telegram.TelegramFileRef, val title: String, val artist: String, val durationSec: Int,
    /** The message it was sent in, for playlists; 0 when unknown. */
    val chatId: Long = 0, val messageId: Long = 0,
    /** The album cover Telegram has for it; the file's own cover is used once it is downloaded. */
    val cover: com.kirox.app.telegram.TelegramFileRef? = null
)

/** «Повтор» in the music player: off, the whole queue, or one track. */
enum class RepeatMode { Off, All, One }

/**
 * Plays one voice note or music file at a time (main thread). Media3 ExoPlayer handles audio focus
 * (music pauses, a call pauses us) and headphones being unplugged; [PlaybackService] puts the player in a
 * media session with a notification, so playback goes on when KiroX leaves the screen, on Android 10–17.
 * Every row observes [playingPath]: starting one note shows the previous one as stopped.
 *
 * Music plays as a queue — the chat's music files in order — with next / previous, repeat, shuffle,
 * a sleep timer and the cover from the file in the notification. Long files (podcasts, audiobooks)
 * continue from where they were left.
 */
object VoicePlayer {
    private var player: androidx.media3.exoplayer.ExoPlayer? = null
    private var appContext: Context? = null
    private var currentPath: String? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    private var loadJob: kotlinx.coroutines.Job? = null
    private var sleepJob: kotlinx.coroutines.Job? = null

    private val _playingPath = MutableStateFlow<String?>(null)
    val playingPath: StateFlow<String?> = _playingPath.asStateFlow()

    /** Playback speed for voice notes and music, kept between notes like in Telegram. */
    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()
    private val speeds = listOf(1f, 1.5f, 2f)

    /** The output chosen in the player (an AudioDeviceInfo id); null leaves it to the system. */
    private val _output = MutableStateFlow<Int?>(null)
    val output: StateFlow<Int?> = _output.asStateFlow()

    /** Sends kiroX's sound to [device] (the phone's speaker, Bluetooth, wired), or back to the system's choice with null. */
    @androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
    fun setOutput(device: android.media.AudioDeviceInfo?) {
        _output.value = device?.id
        player?.setPreferredAudioDevice(device)
    }

    private val _source = MutableStateFlow("")
    /** Where the queue comes from: a chat, a playlist, «Вся музыка» — shown as «Играет из». */
    val source: StateFlow<String> = _source.asStateFlow()
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()
    private val _index = MutableStateFlow(-1)
    /** The music track playing or paused now; null for voice notes and when nothing is loaded. */
    private val _track = MutableStateFlow<Track?>(null)
    val track: StateFlow<Track?> = _track.asStateFlow()
    private val _cover = MutableStateFlow<android.graphics.Bitmap?>(null)
    /** The current track's embedded cover, when the file has one. */
    val cover: StateFlow<android.graphics.Bitmap?> = _cover.asStateFlow()
    private val _repeat = MutableStateFlow(RepeatMode.Off)
    val repeat: StateFlow<RepeatMode> = _repeat.asStateFlow()
    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()
    /** Wall-clock time the sleep timer stops playback, 0 when it is off. */
    private val _sleepAt = MutableStateFlow(0L)
    val sleepAt: StateFlow<Long> = _sleepAt.asStateFlow()
    /** True while the next track is being downloaded. */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** The player for [PlaybackService]'s media session; null when nothing was played yet. */
    internal val sessionPlayer: androidx.media3.common.Player? get() = player

    /** 0–1 position of the playing file; 0 when nothing plays. */
    val progress: Float get() = player?.let { p -> if (p.duration > 0) p.currentPosition.toFloat() / p.duration else 0f } ?: 0f

    /** Milliseconds played of the current file. */
    val positionMs: Int get() = player?.currentPosition?.toInt() ?: 0

    /** Length of the current file in milliseconds; 0 while unknown. */
    val durationMs: Long get() = player?.duration?.takeIf { it > 0 } ?: 0L

    /** Whether something plays (or is about to) right now. */
    val isPlaying: Boolean get() = player?.playWhenReady == true && currentPath != null

    fun cycleSpeed() {
        _speed.value = speeds[(speeds.indexOf(_speed.value) + 1) % speeds.size]
        player?.setPlaybackSpeed(_speed.value)
    }

    fun cycleRepeat() { _repeat.value = RepeatMode.entries[(_repeat.value.ordinal + 1) % RepeatMode.entries.size] }

    fun toggleShuffle() { _shuffle.value = !_shuffle.value }

    /** Jumps to [fraction] of [path] if it is the file playing now. */
    fun seek(path: String, fraction: Float) {
        val p = player ?: return
        if (currentPath != path || p.duration <= 0) return
        p.seekTo((p.duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    /** Jumps to [fraction] of whatever is loaded. */
    fun seekTo(fraction: Float) { currentPath?.let { seek(it, fraction) } }

    /** Moves by [deltaMs] within the current file. */
    fun skipBy(deltaMs: Long) {
        val p = player ?: return
        p.seekTo((p.currentPosition + deltaMs).coerceIn(0L, p.duration.coerceAtLeast(0L)))
    }

    /** Pauses or resumes whatever is loaded. */
    fun playPause() {
        val p = player ?: return
        if (p.playWhenReady) p.pause() else { if (p.playbackState == androidx.media3.common.Player.STATE_ENDED) p.seekTo(0); p.play() }
    }

    /** Stops playback after [minutes]; 0 turns the timer off. */
    fun sleepIn(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) { _sleepAt.value = 0; return }
        _sleepAt.value = System.currentTimeMillis() + minutes * 60_000L
        sleepJob = scope.launch {
            kotlinx.coroutines.delay(minutes * 60_000L)
            _sleepAt.value = 0
            player?.pause()
        }
    }

    @androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
    private fun player(context: Context): androidx.media3.exoplayer.ExoPlayer {
        player?.let { return it }
        val app = context.applicationContext
        appContext = app
        val attributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        return androidx.media3.exoplayer.ExoPlayer.Builder(app)
            .setMediaSourceFactory(com.kirox.app.media.TelegramStream.mediaSourceFactory(app))
            .setAudioAttributes(attributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build().also { created ->
                created.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { publish(); if (!isPlaying) rememberPosition() }
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = publish()
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == androidx.media3.common.Player.STATE_ENDED) { forgetPosition(); onEnded() } else publish()
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) = stop()
                })
                player = created
                // An output chosen earlier still applies to the new player.
                _output.value?.let { id ->
                    app.getSystemService(android.media.AudioManager::class.java)?.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                        ?.firstOrNull { it.id == id }?.let(created::setPreferredAudioDevice)
                }
            }
    }

    /** Shown as playing while it plays or is about to (buffering); paused (focus lost, notification) shows as stopped. */
    private fun publish() {
        val p = player
        _playingPath.value = if (p != null && p.playWhenReady && p.playbackState != androidx.media3.common.Player.STATE_IDLE) currentPath else null
    }

    /** A music track ended: the same again, the next one, or the end of the queue. */
    private fun onEnded() {
        val queue = _queue.value
        val context = appContext
        if (_track.value == null && context != null && playNextVoice(context)) return
        if (_track.value == null || queue.isEmpty() || context == null) { stop(); return }
        when {
            _repeat.value == RepeatMode.One -> { player?.seekTo(0); player?.play() }
            else -> {
                val next = nextIndex(forward = true)
                if (next == null) { player?.pause(); player?.seekTo(0); publish() } else playAt(context, next)
            }
        }
    }

    private fun nextIndex(forward: Boolean): Int? {
        val size = _queue.value.size
        if (size == 0) return null
        val i = _index.value
        if (_shuffle.value && size > 1) return (0 until size).filter { it != i }.random()
        val next = if (forward) i + 1 else i - 1
        return when {
            next in 0 until size -> next
            _repeat.value == RepeatMode.All -> (next + size) % size
            else -> null
        }
    }

    fun next(context: Context) { nextIndex(forward = true)?.let { playAt(context, it) } }

    /** Whether next / previous make sense now (a music queue is loaded). */
    val hasQueue: Boolean get() = _track.value != null && _queue.value.isNotEmpty()

    /** Next and previous from the notification, the lock screen and headphone buttons. */
    internal fun nextFromSession() { appContext?.let { if (hasQueue) next(it) else playNextVoice(it) } }
    internal fun previousFromSession() { appContext?.let { if (hasQueue) previous(it) else player?.seekTo(0) } }

    /** Back to the start of the track after 3 seconds of it, otherwise the previous track. */
    fun previous(context: Context) {
        if ((player?.currentPosition ?: 0) > 3000) { player?.seekTo(0); return }
        nextIndex(forward = false)?.let { playAt(context, it) } ?: player?.seekTo(0)
    }

    /**
     * Plays [tracks] as a queue from [index] (the chat's music in order); a tap on the track that
     * is already loaded pauses or resumes it instead. Tracks download on their turn.
     */
    fun playQueue(context: Context, tracks: List<Track>, index: Int, source: String = "") {
        if (index !in tracks.indices) return
        if (source.isNotBlank()) _source.value = source
        if (_track.value?.file?.id == tracks[index].file.id && currentPath != null) {
            _queue.value = tracks; _index.value = index
            playPause(); return
        }
        _queue.value = tracks
        playAt(context, index)
    }

    private fun playAt(context: Context, index: Int) {
        val track = _queue.value.getOrNull(index) ?: return
        val files = (context.applicationContext as? com.kirox.app.KiroXApplication)?.telegram?.files ?: return
        _index.value = index
        _track.value = track
        loadJob?.cancel()
        _cover.value = null
        loadJob = scope.launch {
            val state = files.state(track.file)
            val key = "tg:${track.file.id}"
            val path = state.value.path
            if (path != null) {
                val cover = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { embeddedCover(path) }
                _cover.value = cover?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
                start(context, android.net.Uri.fromFile(File(path)), key, track.title, track.artist, cover)
                return@launch
            }
            // Not on the phone yet: play online while it downloads; the cover appears once it is saved.
            files.download(track.file)
            start(context, com.kirox.app.media.TelegramStream.uri(track.file.id), key, track.title, track.artist, null)
            val saved = runCatching { kotlinx.coroutines.withTimeout(30 * 60_000L) { state.first { it.path != null }.path } }.getOrNull() ?: return@launch
            val cover = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { embeddedCover(saved) }
            if (_track.value?.file?.id == track.file.id) _cover.value = cover?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
    }

    // ---- Voice notes play one after another, like in Telegram ----

    /** The chat's voice notes in order, and the one playing now. */
    private var voices: List<com.kirox.app.telegram.TelegramFileRef> = emptyList()
    private var voiceId = 0

    /** A voice note from a chat: plays it and remembers [chatVoices] to go on with the next one. */
    fun playVoice(context: Context, path: String, fileId: Int, chatVoices: List<com.kirox.app.telegram.TelegramFileRef>) {
        voices = chatVoices
        voiceId = fileId
        toggle(context, path)
    }

    /** After a voice note ends: the next one in the chat, downloaded first if needed. False when there is none. */
    private fun playNextVoice(context: Context): Boolean {
        val at = voices.indexOfFirst { it.id == voiceId }
        val next = voices.getOrNull(at + 1) ?: return false.also { voices = emptyList() }
        val files = (context.applicationContext as? com.kirox.app.KiroXApplication)?.telegram?.files ?: return false
        voiceId = next.id
        loadJob?.cancel()
        loadJob = scope.launch {
            val state = files.state(next)
            val path = state.value.path ?: run {
                files.download(next)
                runCatching { kotlinx.coroutines.withTimeout(60_000) { state.first { it.path != null }.path } }.getOrNull()
            }
            if (path == null) stop() else start(context, android.net.Uri.fromFile(File(path)), path, "Голосовое сообщение", "kiroX", null)
        }
        return true
    }

    /** The picture stored inside a music file (ID3 / MP4 cover), or null. */
    private fun embeddedCover(path: String): ByteArray? = runCatching {
        android.media.MediaMetadataRetriever().run {
            try { setDataSource(path); embeddedPicture } finally { release() }
        }
    }.getOrNull()

    /**
     * Plays [path], or pauses it when it is the one playing; a paused file resumes where it was.
     * [title] and [artist] are shown in the media notification. For voice notes; music goes
     * through [playQueue].
     */
    fun toggle(context: Context, path: String, title: String = "Голосовое сообщение", artist: String = "kiroX") {
        val p = player(context)
        if (currentPath == path) { playPause(); return }
        if (voices.none { it.localPath == path || it.id == voiceId }) voices = emptyList()
        loadJob?.cancel()
        _track.value = null
        _queue.value = emptyList()
        _cover.value = null
        start(context, android.net.Uri.fromFile(File(path)), path, title, artist, null)
    }

    /** Plays [source]; [key] names it for the rows ([playingPath]) and for «продолжить с места». */
    private fun start(context: Context, source: android.net.Uri, key: String, title: String, artist: String, cover: ByteArray?) {
        val p = player(context)
        rememberPosition()
        currentPath = key
        val metadata = androidx.media3.common.MediaMetadata.Builder().setTitle(title).setArtist(artist)
        if (cover != null) metadata.setArtworkData(cover, androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        val item = androidx.media3.common.MediaItem.Builder()
            .setUri(source)
            .setMediaMetadata(metadata.build())
            .build()
        p.setMediaItem(item, savedPosition(context, key))
        p.setPlaybackSpeed(_speed.value)
        p.prepare()
        p.play()
        // Started while KiroX is on screen, so the media service may go to the foreground and keep playing.
        PlaybackService.start(context)
        publish()
    }

    // ---- «Продолжить с места»: only for files longer than 10 minutes ----

    private const val RESUME_PREFS = "player_positions"
    private const val RESUME_MIN_MS = 10 * 60_000L

    private fun positions(context: Context) = context.getSharedPreferences(RESUME_PREFS, Context.MODE_PRIVATE)

    private fun savedPosition(context: Context, path: String): Long = positions(context).getLong(path, 0L)

    private fun rememberPosition() {
        val p = player ?: return
        val path = currentPath ?: return
        val context = appContext ?: return
        if (p.duration < RESUME_MIN_MS) return
        val at = p.currentPosition
        positions(context).edit {
            if (at < 5_000 || at > p.duration - 10_000) remove(path) else putLong(path, at)
        }
    }

    private fun forgetPosition() {
        val path = currentPath ?: return
        appContext?.let { positions(it).edit { remove(path) } }
    }

    fun stop() {
        rememberPosition()
        loadJob?.cancel()
        player?.let { it.stop(); it.clearMediaItems() }
        currentPath = null
        _track.value = null
        _cover.value = null
        _loading.value = false
        _playingPath.value = null
        appContext?.let(PlaybackService::stop)
    }
}
