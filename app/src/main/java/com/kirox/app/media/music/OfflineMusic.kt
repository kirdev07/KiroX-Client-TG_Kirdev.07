package com.kirox.app.media.music

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.telegram.TelegramFileRepository
import com.kirox.app.ui.chat.VoicePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** What a download needs to know about a track. */
data class OfflineRequest(val chatId: Long, val messageId: Long, val fileId: Int, val title: String, val performer: String,
                          val duration: Int, val size: Long, val chatTitle: String) {
    val key: Pair<Long, Long> get() = chatId to messageId
}

/** Result of «Сохранить в Музыку телефона». */
data class ExportResult(val saved: Int, val skipped: Int, val failed: Int)

/**
 * Music kept for offline listening. A finished TDLib download is copied into kiroX's own folder
 * (files/music_offline), outside TDLib's files: «Очистить кэш», the cache limit (OptimizeStorage)
 * and «Хранить медиа» never remove it. The player plays that copy first (no network needed).
 * Also saves tracks into the phone's Music/kiroX through MediaStore (Android 10+, no permission).
 */
class OfflineMusic private constructor(context: Context) {
    private val app = context.applicationContext
    private val files: TelegramFileRepository? get() = (app as? com.kirox.app.KiroXApplication)?.telegram?.files
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dir = File(app.filesDir, "music_offline").apply { mkdirs() }
    private val indexFile = File(dir, "index.tsv")
    private val prefs = app.getSharedPreferences("music_offline", Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow(OfflineEntry.decode(runCatching { indexFile.readText() }.getOrNull()).filter { File(it.path).isFile })
    val entries: StateFlow<List<OfflineEntry>> = _entries.asStateFlow()
    private val _queue = MutableStateFlow(DownloadQueue())
    val queue: StateFlow<DownloadQueue> = _queue.asStateFlow()
    private val requests = HashMap<Pair<Long, Long>, OfflineRequest>()
    private val jobs = HashMap<Pair<Long, Long>, Job>()

    /** Liked tracks download by themselves. */
    var autoLiked: Boolean
        get() = prefs.getBoolean("autoLiked", false)
        set(v) { prefs.edit().putBoolean("autoLiked", v).apply(); _settings.value = v to wifiOnly }
    /** …only on Wi-Fi (unmetered networks), on by default. */
    var wifiOnly: Boolean
        get() = prefs.getBoolean("wifiOnly", true)
        set(v) { prefs.edit().putBoolean("wifiOnly", v).apply(); _settings.value = autoLiked to v }
    private val _settings = MutableStateFlow(autoLiked to wifiOnly)
    val settings: StateFlow<Pair<Boolean, Boolean>> = _settings.asStateFlow()

    init {
        // The player takes the offline copy first.
        VoicePlayer.offlinePath = { chatId, messageId -> _entries.value.firstOrNull { it.chatId == chatId && it.messageId == messageId }?.path?.takeIf { File(it).isFile } }
    }

    fun entry(chatId: Long, messageId: Long): OfflineEntry? = _entries.value.firstOrNull { it.chatId == chatId && it.messageId == messageId }

    val bytes: Long get() = _entries.value.sumOf { runCatching { File(it.path).length() }.getOrDefault(it.size) }

    fun unmetered(): Boolean = runCatching {
        val cm = app.getSystemService(android.net.ConnectivityManager::class.java)
        !cm.isActiveNetworkMetered
    }.getOrDefault(false)

    private fun saveIndex() { val text = OfflineEntry.encode(_entries.value); scope.launch(Dispatchers.IO) { runCatching { indexFile.writeText(text) } } }

    // ---- Downloads ----

    fun download(list: List<OfflineRequest>) {
        val have = _entries.value.map { it.key }.toSet()
        val add = list.filter { it.fileId != 0 && it.key !in have }
        add.forEach { requests[it.key] = it }
        _queue.value = _queue.value.clearFinished().enqueue(add.map { it.key to it.size }, have)
        pump()
    }

    private fun pump() {
        _queue.value.toStart().forEach { key ->
            val r = requests[key] ?: run { _queue.value = _queue.value.cancel(key); return@forEach }
            _queue.value = _queue.value.started(key)
            jobs[key] = scope.launch { run(r) }
        }
    }

    private suspend fun run(r: OfflineRequest) {
        val repo = files ?: run { fail(r.key); return }
        val ref = TelegramFileRef(r.fileId, null)
        repo.download(ref, PRIORITY)
        val state = repo.state(ref)
        // Progress follows TDLib; 3 minutes without new bytes counts as a failure (tried again on «Скачать»).
        var last = -1L; var lastAt = System.currentTimeMillis(); var askedAt = lastAt
        var path: String? = null
        while (true) {
            val s = state.value
            _queue.value = _queue.value.progress(r.key, s.downloaded, s.total)
            if (s.path != null) { path = s.path; break }
            val now = System.currentTimeMillis()
            if (s.downloaded != last) { last = s.downloaded; lastAt = now }
            else if (now - lastAt > 3 * 60_000L) break
            // TDLib may drop a background download (network change): ask again now and then.
            if (!s.downloading && now - askedAt > 20_000L) { repo.download(ref, PRIORITY); askedAt = now }
            kotlinx.coroutines.delay(500)
        }
        if (path == null) { fail(r.key); return }
        val ext = MusicNaming.extension(path)
        val target = File(dir, "${r.chatId}_${r.messageId}.$ext")
        val ok = withContext(Dispatchers.IO) { runCatching { File(path).copyTo(target, overwrite = true); true }.getOrDefault(false) }
        if (!ok) { fail(r.key); return }
        val e = OfflineEntry(r.chatId, r.messageId, r.fileId, r.title, r.performer, r.duration, target.length(), target.absolutePath, r.chatTitle, System.currentTimeMillis())
        _entries.value = _entries.value.filterNot { it.key == e.key } + e
        saveIndex()
        // The TDLib copy is not needed twice (unless it plays right now from that path).
        if (VoicePlayer.track.value?.file?.id != r.fileId) repo.deleteLocal(ref)
        jobs.remove(r.key)
        _queue.value = _queue.value.finished(r.key)
        pump()
    }

    private fun fail(key: Pair<Long, Long>) { jobs.remove(key); _queue.value = _queue.value.failed(key); pump() }

    fun pauseAll() {
        _queue.value.items.filter { it.state == DlState.Running }.forEach { item ->
            jobs.remove(item.key)?.cancel()
            requests[item.key]?.let { files?.cancel(TelegramFileRef(it.fileId, null)) }
        }
        _queue.value = _queue.value.pauseAll()
    }

    fun resumeAll() { _queue.value = _queue.value.resumeAll(); pump() }

    fun cancel(key: Pair<Long, Long>) {
        jobs.remove(key)?.cancel()
        requests.remove(key)?.let { files?.cancel(TelegramFileRef(it.fileId, null)) }
        _queue.value = _queue.value.cancel(key)
        pump()
    }

    fun cancelAll() { _queue.value.items.filter { it.state != DlState.Done }.forEach { cancel(it.key) }; _queue.value = DownloadQueue() }

    fun remove(chatId: Long, messageId: Long) {
        val e = entry(chatId, messageId) ?: return
        _entries.value = _entries.value.filterNot { it.key == e.key }
        saveIndex()
        scope.launch(Dispatchers.IO) { runCatching { File(e.path).delete() } }
    }

    fun removeAll() {
        cancelAll()
        val all = _entries.value
        _entries.value = emptyList()
        saveIndex()
        scope.launch(Dispatchers.IO) { all.forEach { runCatching { File(it.path).delete() } } }
    }

    // ---- «Сохранить в Музыку телефона» ----

    /** A local copy of the track: offline, else the finished TDLib download, else downloaded now (up to 10 min). */
    private suspend fun localFile(r: OfflineRequest): String? {
        entry(r.chatId, r.messageId)?.path?.takeIf { File(it).isFile }?.let { return it }
        val repo = files ?: return null
        if (r.fileId == 0) return null
        val ref = TelegramFileRef(r.fileId, null)
        repo.state(ref).value.path?.let { return it }
        repo.download(ref, PRIORITY)
        return withTimeoutOrNull(10 * 60_000L) { repo.state(ref).first { it.path != null }.path }
    }

    /** Names in Music/kiroX/<folder> written by kiroX (the ones this app can see without permission). */
    private fun existingNames(relative: String): Set<String> = runCatching {
        val out = HashSet<String>()
        app.contentResolver.query(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), "${MediaStore.MediaColumns.RELATIVE_PATH}=?", arrayOf(relative), null)?.use { c ->
            while (c.moveToNext()) c.getString(0)?.let(out::add)
        }
        out
    }.getOrDefault(emptySet())

    /**
     * Copies [tracks] into Music/kiroX/[folder] (MediaStore, IS_PENDING while writing) with title,
     * artist and album set, so the phone's own player lists them. Duplicates are skipped.
     * [progress] gets (done, total).
     */
    suspend fun exportToPhone(tracks: List<OfflineRequest>, folder: String?, album: String, progress: (Int, Int) -> Unit): ExportResult {
        val relative = MusicNaming.folder(folder)
        val existing = withContext(Dispatchers.IO) { existingNames(relative) }
        // Names first need the extension, which is known once the file is local: resolve as we go.
        var saved = 0; var skipped = 0; var failed = 0
        val used = existing.toMutableSet()
        tracks.distinctBy { it.key }.forEachIndexed { i, r ->
            progress(i, tracks.size)
            val path = localFile(r)
            if (path == null) { failed++; return@forEachIndexed }
            val ext = MusicNaming.extension(path)
            val name = MusicNaming.batchNames(listOf(MusicNaming.fileName(r.title, r.performer, ext)), used).single()
            if (name == null) { skipped++; return@forEachIndexed }
            val ok = withContext(Dispatchers.IO) { insert(path, relative, name, ext, r, album) }
            if (ok) { saved++; used += name } else failed++
        }
        progress(tracks.size, tracks.size)
        return ExportResult(saved, skipped, failed)
    }

    private fun insert(path: String, relative: String, name: String, ext: String, r: OfflineRequest, album: String): Boolean {
        val resolver = app.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, MusicNaming.mime(ext))
            put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            put(MediaStore.Audio.AudioColumns.TITLE, r.title)
            if (r.performer.isNotBlank()) put(MediaStore.Audio.AudioColumns.ARTIST, r.performer)
            put(MediaStore.Audio.AudioColumns.ALBUM, album.ifBlank { "kiroX" })
            put(MediaStore.Audio.AudioColumns.IS_MUSIC, 1)
            if (r.duration > 0) put(MediaStore.Audio.AudioColumns.DURATION, r.duration * 1000L)
        }
        val uri = runCatching { resolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values) }.getOrNull() ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out -> File(path).inputStream().use { it.copyTo(out) } } ?: error("no stream")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            true
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }

    companion object {
        /** Below what plays now and its next-track preload. */
        private const val PRIORITY = 1

        @Volatile private var instance: OfflineMusic? = null
        fun get(context: Context): OfflineMusic = instance ?: synchronized(this) { instance ?: OfflineMusic(context).also { instance = it } }
    }
}
