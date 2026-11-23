package com.kirox.app.media.music

/*
 * music offline: pure parts (unit-tested) — names for files saved to the phone's Music folder,
 * the duplicate check, the downloaded-tracks index and the download queue.
 */

/** File and folder names for «Сохранить в Музыку телефона». */
object MusicNaming {
    const val ROOT = "Music/kiroX/"
    private val bad = Regex("""[\\/:*?"<>|\p{Cntrl}]""")

    /** A name every file system accepts: no reserved characters, no leading/trailing dots or spaces, at most [max] chars. */
    fun safe(s: String, max: Int = 80): String =
        s.replace(bad, " ").replace(Regex("\\s+"), " ").trim().trim('.', ' ').take(max).trim()

    /** «Исполнитель - Название.mp3», or just the title without a performer; «Трек» when there is nothing. */
    fun fileName(title: String, performer: String, ext: String): String {
        val t = safe(title).ifBlank { "Трек" }
        val p = safe(performer)
        val e = ext.trim().trimStart('.').lowercase().ifBlank { "mp3" }
        val base = if (p.isBlank() || t.startsWith(p, ignoreCase = true)) t else "$p - $t"
        return "${safe(base, 120)}.$e"
    }

    /** MediaStore RELATIVE_PATH: Music/kiroX/ or Music/kiroX/<folder>/. */
    fun folder(name: String?): String {
        val f = safe(name.orEmpty(), 60)
        return if (f.isBlank()) ROOT else "$ROOT$f/"
    }

    /** Extension of a local file, else from its MIME type. */
    fun extension(path: String, mime: String? = null): String {
        val fromPath = path.substringAfterLast('/').substringAfterLast('.', "").lowercase()
        if (fromPath.length in 1..5 && fromPath.all { it.isLetterOrDigit() }) return fromPath
        return when (mime?.lowercase()) {
            "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac" -> "m4a"
            "audio/ogg", "audio/opus" -> "ogg"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/wav", "audio/x-wav" -> "wav"
            else -> "mp3"
        }
    }

    fun mime(ext: String): String = when (ext.lowercase()) {
        "m4a", "mp4", "aac" -> "audio/mp4"
        "ogg", "oga", "opus" -> "audio/ogg"
        "flac" -> "audio/flac"
        "wav" -> "audio/x-wav"
        else -> "audio/mpeg"
    }

    /** Whether [name] is in the folder already (file systems on phones ignore case). */
    fun isDuplicate(existing: Collection<String>, name: String): Boolean = existing.any { it.equals(name, ignoreCase = true) }

    /**
     * Names for a batch: a name already in the folder is a duplicate (null = skip it); two tracks of
     * the batch with the same name get « (2)», « (3)».
     */
    fun batchNames(names: List<String>, existing: Collection<String>): List<String?> {
        val inFolder = existing.map { it.lowercase() }.toSet()
        val used = HashSet<String>()
        return names.map { n ->
            if (n.lowercase() in inFolder) return@map null
            val base = n.substringBeforeLast('.'); val ext = n.substringAfterLast('.', "")
            var candidate = n; var k = 2
            while (candidate.lowercase() in used || candidate.lowercase() in inFolder) {
                candidate = "$base ($k)" + if (ext.isNotEmpty()) ".$ext" else ""; k++
            }
            used += candidate.lowercase(); candidate
        }
    }
}

/** A track kept for offline listening: its message, names, and the copy in kiroX's own storage. */
data class OfflineEntry(
    val chatId: Long, val messageId: Long, val fileId: Int, val title: String, val performer: String,
    val duration: Int, val size: Long, val path: String, val chatTitle: String = "", val addedAt: Long = 0,
) {
    val key: Pair<Long, Long> get() = chatId to messageId

    companion object {
        private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ')

        fun encode(list: List<OfflineEntry>): String = list.joinToString("\n") { e ->
            listOf(e.chatId, e.messageId, e.fileId, e.duration, e.size, e.addedAt, clean(e.path), clean(e.chatTitle), clean(e.performer), clean(e.title)).joinToString("\t")
        }

        fun decode(text: String?): List<OfflineEntry> = text.orEmpty().lineSequence().mapNotNull { line ->
            val p = line.split('\t', limit = 10)
            if (p.size < 10) return@mapNotNull null
            OfflineEntry(p[0].toLongOrNull() ?: return@mapNotNull null, p[1].toLongOrNull() ?: return@mapNotNull null,
                p[2].toIntOrNull() ?: 0, p[9], p[8], p[3].toIntOrNull() ?: 0, p[4].toLongOrNull() ?: 0, p[6], p[7], p[5].toLongOrNull() ?: 0)
        }.distinctBy { it.key }.toList()
    }
}

enum class DlState { Queued, Running, Paused, Done, Failed }

data class DlItem(val key: Pair<Long, Long>, val size: Long = 0, val downloaded: Long = 0, val state: DlState = DlState.Queued)

/**
 * The offline download queue as a value (unit-tested): at most [maxActive] run at once, the rest
 * wait in order; pause / resume / cancel; totals for the progress line («3 из 10 · 12 МБ»).
 */
data class DownloadQueue(val items: List<DlItem> = emptyList(), val maxActive: Int = 2) {
    val active: Int get() = items.count { it.state == DlState.Running }
    val waiting: Int get() = items.count { it.state == DlState.Queued }
    val done: Int get() = items.count { it.state == DlState.Done }
    val failed: Int get() = items.count { it.state == DlState.Failed }
    /** Paused by the user (all of the unfinished ones are). */
    val paused: Boolean get() = items.any { it.state == DlState.Paused } && items.none { it.state == DlState.Running || it.state == DlState.Queued }
    /** Something still to do (running, waiting or paused). */
    val busy: Boolean get() = items.any { it.state == DlState.Running || it.state == DlState.Queued || it.state == DlState.Paused }
    val totalBytes: Long get() = items.sumOf { it.size }
    val downloadedBytes: Long get() = items.sumOf { if (it.state == DlState.Done) it.size else it.downloaded.coerceAtMost(if (it.size > 0) it.size else Long.MAX_VALUE) }
    val fraction: Float get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else if (items.isNotEmpty()) done.toFloat() / items.size else 0f

    fun state(key: Pair<Long, Long>): DlItem? = items.firstOrNull { it.key == key }

    /** Adds tracks not in the queue yet (failed ones are tried again). [have] = already offline: skipped. */
    fun enqueue(add: List<Pair<Pair<Long, Long>, Long>>, have: Set<Pair<Long, Long>> = emptySet()): DownloadQueue {
        val list = items.map { if (it.state == DlState.Failed && add.any { a -> a.first == it.key }) it.copy(state = DlState.Queued, downloaded = 0) else it }.toMutableList()
        val known = list.map { it.key }.toMutableSet()
        add.forEach { (key, size) -> if (key !in known && key !in have) { list += DlItem(key, size); known += key } }
        return copy(items = list)
    }

    /** Items to start now (first waiting ones, up to [maxActive] running). */
    fun toStart(): List<Pair<Long, Long>> = items.filter { it.state == DlState.Queued }.take((maxActive - active).coerceAtLeast(0)).map { it.key }

    private fun update(key: Pair<Long, Long>, f: (DlItem) -> DlItem) = copy(items = items.map { if (it.key == key) f(it) else it })

    fun started(key: Pair<Long, Long>) = update(key) { it.copy(state = DlState.Running) }
    fun progress(key: Pair<Long, Long>, downloaded: Long, size: Long) = update(key) { it.copy(downloaded = downloaded, size = if (size > 0) size else it.size) }
    fun finished(key: Pair<Long, Long>) = update(key) { it.copy(state = DlState.Done, downloaded = it.size) }
    fun failed(key: Pair<Long, Long>) = update(key) { it.copy(state = DlState.Failed) }

    /** Pause: running and waiting ones stop (what is on disk stays for the resume). */
    fun pauseAll() = copy(items = items.map { if (it.state == DlState.Running || it.state == DlState.Queued) it.copy(state = DlState.Paused) else it })
    fun resumeAll() = copy(items = items.map { if (it.state == DlState.Paused) it.copy(state = DlState.Queued) else it })
    fun cancel(key: Pair<Long, Long>) = copy(items = items.filterNot { it.key == key })
    fun cancelAll() = copy(items = items.filter { it.state == DlState.Done })
    /** Finished batches leave the progress line. */
    fun clearFinished() = if (busy) this else copy(items = emptyList())
}
