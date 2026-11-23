package com.kirox.app.ui.v3.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kirox.app.telegram.CallEndReason
import com.kirox.app.telegram.CallPhase
import com.kirox.app.telegram.GroupCallPhase
import com.kirox.app.telegram.TelegramActiveCall
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.telegram.TelegramGroupCall
import com.kirox.app.telegram.TelegramViewModel
import com.kirox.app.telegram.decodeWaveform
import com.kirox.app.telegram.mediaOf
import com.kirox.app.telegram.toStory
import com.kirox.app.ui.chat.RepeatMode
import com.kirox.app.ui.chat.Track
import com.kirox.app.ui.chat.VoicePlayer
import com.kirox.app.ui.v3.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

/*
 * wave3-media: the TDLib side of the media, music, files, voice, story, call and network boards.
 * Reuses the old app layer (TelegramMusic, TelegramFileRepository, VoicePlayer, TelegramVoipRepository,
 * TelegramGroupCallRepository, TelegramExtras, TelegramStoryRepository, MediaFiles helpers); the few
 * TDLib requests without a repository (download list, story archive, traffic per category, sounds,
 * ping) are made here. Wired from TelegramKxStore with one-line delegates.
 */

/** Pure mappings of the media boards (unit-tested). */
internal object KxMediaMapping {
    /** «0:00 Вступление» / «12:40 — Итоги» lines of a caption → (seconds, title), sorted; fewer than 2 = no chapters. */
    fun chapters(caption: String): List<Pair<Int, String>> {
        val re = Regex("""^\s*(?:(\d{1,2}):)?(\d{1,2}):(\d{2})\s*[-—–:·]?\s*(.+)$""")
        val found = caption.lines().mapNotNull { line ->
            val m = re.find(line) ?: return@mapNotNull null
            val h = m.groupValues[1].toIntOrNull() ?: 0
            val min = m.groupValues[2].toInt(); val s = m.groupValues[3].toInt()
            if (s >= 60) return@mapNotNull null
            (h * 3600 + min * 60 + s) to m.groupValues[4].trim()
        }.distinctBy { it.first }.sortedBy { it.first }
        return if (found.size >= 2 && found.first().first == 0) found else emptyList()
    }

    /** TDLib file type of a traffic entry → board category key. */
    fun category(type: TdApi.FileType?): String = when (type) {
        is TdApi.FileTypePhoto, is TdApi.FileTypeProfilePhoto, is TdApi.FileTypeThumbnail, is TdApi.FileTypeWallpaper -> "photo"
        is TdApi.FileTypeVideo, is TdApi.FileTypeAnimation, is TdApi.FileTypeVideoNote, is TdApi.FileTypeVideoStory -> "video"
        is TdApi.FileTypeVoiceNote -> "voice"
        is TdApi.FileTypeAudio -> "music"
        is TdApi.FileTypeDocument, is TdApi.FileTypeSecret -> "files"
        is TdApi.FileTypeSticker -> "stickers"
        null, is TdApi.FileTypeNone -> "messages"
        else -> "other"
    }

    /** 0 mobile, 1 Wi-Fi, 2 roaming, -1 skip. */
    fun network(type: TdApi.NetworkType?): Int = when (type) {
        is TdApi.NetworkTypeMobile -> 0
        is TdApi.NetworkTypeWiFi, is TdApi.NetworkTypeOther -> 1
        is TdApi.NetworkTypeMobileRoaming -> 2
        else -> -1
    }

    fun problems(keys: List<String>): Array<TdApi.CallProblem> = keys.mapNotNull {
        when (it) {
            "echo" -> TdApi.CallProblemEcho(); "noise" -> TdApi.CallProblemNoise(); "interruptions" -> TdApi.CallProblemInterruptions()
            "distorted" -> TdApi.CallProblemDistortedSpeech(); "silentLocal" -> TdApi.CallProblemSilentLocal()
            "silentRemote" -> TdApi.CallProblemSilentRemote(); "dropped" -> TdApi.CallProblemDropped()
            "video" -> TdApi.CallProblemDistortedVideo(); "pixelated" -> TdApi.CallProblemPixelatedVideo()
            else -> null
        }
    }.toTypedArray()

    fun endReason(r: CallEndReason) = when (r) {
        CallEndReason.HungUp -> "Звонок завершён"; CallEndReason.Declined -> "Звонок отклонён"
        CallEndReason.Missed -> "Нет ответа"; CallEndReason.Disconnected -> "Соединение прервано"
    }

    fun call(c: TelegramActiveCall, avatar: String?): KxCallState {
        val (phase, since, ended) = when (val p = c.phase) {
            is CallPhase.Calling -> Triple(if (p.ringing) KxCallPhase.Ringing else KxCallPhase.Calling, 0L, 0)
            CallPhase.Incoming -> Triple(KxCallPhase.Incoming, 0L, 0)
            CallPhase.Connecting -> Triple(KxCallPhase.Connecting, 0L, 0)
            is CallPhase.Active -> Triple(KxCallPhase.Active, p.sinceMillis, 0)
            is CallPhase.Ended -> Triple(KxCallPhase.Ended, 0L, p.durationSeconds)
            is CallPhase.Failed -> Triple(KxCallPhase.Failed, 0L, 0)
        }
        return KxCallState(
            id = c.id, peerId = c.userId, name = c.name, avatar = avatar, video = c.isVideo, outgoing = c.isOutgoing, phase = phase,
            sinceMs = since, endedSeconds = ended, emojis = c.emojis, micMuted = c.micMuted, cameraOn = c.cameraOn,
            remoteVideo = c.remoteVideo, remoteMuted = c.remoteMuted, signalBars = c.signalBars,
            reconnecting = (c.phase as? CallPhase.Active)?.reconnecting == true,
            endReason = (c.phase as? CallPhase.Ended)?.reason?.let(::endReason).orEmpty(),
            failure = (c.phase as? CallPhase.Failed)?.message.orEmpty(),
        )
    }

    fun sizeLine(bytes: Long) = KxFormat.fileSize(bytes)
}

internal class KxMediaBinder(
    private val app: Application,
    private val vm: TelegramViewModel,
    private val scope: CoroutineScope,
    /** Path of a small file, starting its download (avatars, photos, thumbnails). */
    private val autoPath: (TelegramFileRef?) -> String?,
) {
    private val services get() = (app as com.kirox.app.KiroXApplication).telegram
    private val client get() = services.client

    // ---- names ----
    private val names = ConcurrentHashMap<Long, String>()
    private suspend fun senderName(sender: TdApi.MessageSender?): String = when (sender) {
        is TdApi.MessageSenderUser -> names[sender.userId] ?: orNull { client.query(TdApi.GetUser(sender.userId)) }
            ?.let { listOf(it.firstName, it.lastName).filter { s -> s.isNotBlank() }.joinToString(" ") }.orEmpty().also { if (it.isNotEmpty()) names[sender.userId] = it }
        is TdApi.MessageSenderChat -> chatTitle(sender.chatId)
        else -> ""
    }
    private suspend fun chatTitle(chatId: Long): String = names[-chatId - 1] ?: orNull { client.query(TdApi.GetChat(chatId)).title }.orEmpty().also { if (it.isNotEmpty()) names[-chatId - 1] = it }

    // ---- Viewer / Album ----
    private suspend fun item(m: TdApi.Message): KxMediaItem? {
        val video = when (m.content) { is TdApi.MessagePhoto -> false; is TdApi.MessageVideo -> true; else -> return null }
        val (media, thumb) = mediaOf(m.content)
        media ?: return null
        val full = if (video) fileState(media.file.id).path ?: media.file.localPath else autoPath(media.file)
        return KxMediaItem(
            chatId = m.chatId, messageId = m.id, video = video, media = full, thumb = autoPath(thumb), fileId = media.file.id,
            duration = media.duration, aspect = if (media.height > 0) media.width.toFloat() / media.height else 0f, size = media.size,
            sender = senderName(m.senderId), time = KxFormat.dateTime(m.date),
            caption = com.kirox.app.telegram.formattedTextOf(m.content)?.text.orEmpty(), albumId = m.mediaAlbumId,
            chapters = if (video) KxMediaMapping.chapters(com.kirox.app.telegram.formattedTextOf(m.content)?.text.orEmpty()) else emptyList(),
            canDelete = true, canForward = m.content !is TdApi.MessageExpiredPhoto,
        )
    }

    suspend fun chatMedia(chatId: Long, around: Long): List<KxMediaItem> {
        val found = orNull {
            client.query(TdApi.SearchChatMessages(chatId, null, "", null, around, if (around == 0L) 0 else -40, 80, TdApi.SearchMessagesFilterPhotoAndVideo()))
        }?.messages?.toMutableList() ?: mutableListOf()
        if (around != 0L && found.none { it.id == around }) orNull { client.query(TdApi.GetMessage(chatId, around)) }?.let { found += it }
        return withRights(chatId, found.distinctBy { it.id }.sortedBy { it.id }.mapNotNull { item(it) })
    }

    /** Delete / forward / save rights of each item (GetMessageProperties), asked in parallel. */
    private suspend fun withRights(chatId: Long, items: List<KxMediaItem>): List<KxMediaItem> = kotlinx.coroutines.coroutineScope {
        items.map { i ->
            async {
                val r = orNull { client.query(TdApi.GetMessageProperties(chatId, i.messageId)) }
                if (r == null) i.copy(canDelete = false, canForward = false, canSave = false)
                else i.copy(canDelete = r.canBeDeletedOnlyForSelf || r.canBeDeletedForAllUsers, canDeleteForAll = r.canBeDeletedForAllUsers,
                    canForward = r.canBeForwarded, canSave = r.canBeSaved)
            }
        }.awaitAll()
    }

    suspend fun albumOf(chatId: Long, messageId: Long): List<KxMediaItem> {
        val msg = orNull { client.query(TdApi.GetMessage(chatId, messageId)) } ?: return emptyList()
        if (msg.mediaAlbumId == 0L) return listOfNotNull(item(msg))
        // Albums are at most 10 messages next to each other. TDLib's first answer is often only what is cached:
        // ask again from the oldest / newest album message found until a page brings nothing of the album.
        val found = LinkedHashMap<Long, TdApi.Message>().apply { put(msg.id, msg) }
        /** True while it is worth asking again: the page had only known album messages (TDLib answers from its cache first). */
        suspend fun page(from: Long, offset: Int, limit: Int): Boolean {
            val got = orNull { client.query(TdApi.GetChatHistory(chatId, from, offset, limit, false)) }?.messages.orEmpty()
            if (got.isEmpty()) return false
            got.filter { it.mediaAlbumId == msg.mediaAlbumId }.forEach { found[it.id] = it }
            return got.all { it.mediaAlbumId == msg.mediaAlbumId } // reached outside the album → done
        }
        for (k in 0 until 6) if (!page(found.keys.min(), 0, 11) || found.size >= 10) break        // older (from_message included)
        for (k in 0 until 6) if (!page(found.keys.max(), -10, 11) || found.size >= 10) break      // newer
        return withRights(chatId, found.values.sortedBy { it.id }.take(10).mapNotNull { item(it) }.ifEmpty { listOfNotNull(item(msg)) })
    }

    // ---- file states ----
    private val fileStates = mutableStateMapOf<Int, KxFileState>()
    private val watching = HashSet<Int>()
    fun fileState(fileId: Int): KxFileState {
        if (fileId == 0) return KxFileState()
        if (watching.add(fileId)) scope.launch {
            // Ends once the file is on the phone, so finished files do not keep a collector each.
            vm.files.state(TelegramFileRef(fileId, null)).first { s -> fileStates[fileId] = KxFileState(s.path, s.downloaded, s.total, s.downloading); s.path != null }
        }
        return fileStates[fileId] ?: KxFileState()
    }
    fun cancelDownload(fileId: Int) = vm.files.cancel(TelegramFileRef(fileId, null))

    private suspend fun pathOf(fileId: Int): String? = orNull { client.query(TdApi.GetFile(fileId)) }?.local?.takeIf { it.isDownloadingCompleted && it.path.isNotBlank() }?.path

    suspend fun saveToGallery(fileId: Int, video: Boolean): String? {
        val path = pathOf(fileId) ?: return "Файл ещё не скачан"
        return if (com.kirox.app.ui.chat.saveToGallery(app, path, video)) null else "Не получилось сохранить"
    }

    suspend fun shareFile(fileId: Int): String? {
        val file = orNull { client.query(TdApi.GetFile(fileId)) } ?: return "Файл не найден"
        val path = file.local?.takeIf { it.isDownloadingCompleted }?.path ?: return "Файл ещё не скачан"
        val name = java.io.File(path).name
        return if (com.kirox.app.ui.chat.shareFile(app, path, name, com.kirox.app.ui.chat.mimeTypeOf(name, "application/octet-stream"))) null else "Нет приложения, чтобы поделиться"
    }

    suspend fun openFile(fileId: Int): String? {
        val path = pathOf(fileId) ?: return "Файл ещё не скачан"
        val name = java.io.File(path).name
        return if (com.kirox.app.ui.chat.openFile(app, path, name, com.kirox.app.ui.chat.mimeTypeOf(name, "application/octet-stream"))) null else "Нет приложения, чтобы открыть файл"
    }

    // ---- Music ----
    var player by mutableStateOf(KxPlayerState())
        private set
    private var trackMap = HashMap<Int, KxTrack>()

    private fun TelegramAudioToKx(a: com.kirox.app.telegram.TelegramAudio) = KxTrack(
        a.chatId, a.messageId, a.title, a.performer, a.chatTitle, a.duration, a.file.id, a.size, autoPath(a.cover), KxFormat.chatTime(a.date),
    )

    suspend fun musicLibrary(query: String): List<KxTrack> {
        val first = orNull { services.music.all(query.trim(), "", 100).first }.orEmpty()
        val list = if (first.isEmpty() && query.isBlank()) orNull { services.music.fromRecentChats(40, 20) }.orEmpty() else first
        return list.map { TelegramAudioToKx(it) }.also { tracks -> tracks.forEach { trackMap[it.fileId] = it } }
    }

    private fun KxTrack.toTrack() = Track(TelegramFileRef(fileId, null), title, performer.ifBlank { "kiroX" }, duration, chatId, messageId)

    fun playTracks(tracks: List<KxTrack>, index: Int) {
        tracks.forEach { trackMap[it.fileId] = it }
        VoicePlayer.playQueue(app, tracks.map { it.toTrack() }, index, "Музыка")
    }
    fun playerToggle() = VoicePlayer.playPause()
    fun playerNext() = VoicePlayer.next(app)
    fun playerPrevious() = VoicePlayer.previous(app)
    fun playerSeek(fraction: Float) = VoicePlayer.seekTo(fraction)
    fun playerShuffle() = VoicePlayer.toggleShuffle()
    fun playerRepeat() = VoicePlayer.cycleRepeat()

    fun playVoice(fileId: Int) {
        scope.launch {
            val ref = TelegramFileRef(fileId, null)
            vm.files.download(ref)
            val path = withTimeoutOrNull(60_000) { vm.files.state(ref).first { it.path != null }.path } ?: return@launch
            VoicePlayer.toggle(app, path)
        }
    }

    private fun startPlayer() {
        scope.launch {
            combine(VoicePlayer.track, VoicePlayer.queue, VoicePlayer.shuffle, VoicePlayer.repeat, VoicePlayer.loading) { t, q, s, r, l ->
                arrayOf<Any?>(t, q, s, r, l)
            }.collect { (t, q, s, r, l) ->
                @Suppress("UNCHECKED_CAST")
                val track = t as Track?
                @Suppress("UNCHECKED_CAST")
                val queue = (q as List<Track>).map { trackMap[it.file.id] ?: KxTrack(it.chatId, it.messageId, it.title, it.artist, duration = it.durationSec, fileId = it.file.id) }
                player = player.copy(
                    track = track?.let { trackMap[it.file.id] ?: KxTrack(it.chatId, it.messageId, it.title, it.artist, duration = it.durationSec, fileId = it.file.id) },
                    queue = queue, shuffle = s as Boolean, repeat = (r as RepeatMode).ordinal, loading = l as Boolean,
                    source = VoicePlayer.source.value,
                )
            }
        }
        // Position is not a flow: sampled twice a second while something is loaded.
        scope.launch {
            while (isActive) {
                val playing = VoicePlayer.isPlaying
                val pos = VoicePlayer.positionMs.toLong(); val dur = VoicePlayer.durationMs
                if (playing != player.playing || pos != player.positionMs || dur != player.durationMs) player = player.copy(playing = playing, positionMs = pos, durationMs = dur)
                delay(if (player.track != null || playing) 500 else 1500)
            }
        }
    }

    // ---- Downloads ----
    suspend fun downloads(query: String): List<KxDownload> {
        val all = ArrayList<TdApi.FileDownload>()
        var offset = ""
        for (k in 0 until 10) {
            val page = orNull { client.query(TdApi.SearchFileDownloads(query.trim(), false, false, offset, 100)) } ?: break
            all += page.files
            offset = page.nextOffset.orEmpty()
            if (offset.isEmpty() || page.files.isEmpty()) break
        }
        return all.distinctBy { it.fileId }.map { d ->
            val m = d.message
            val (media, thumb) = mediaOf(m.content)
            val file = orNull { client.query(TdApi.GetFile(d.fileId)) }
            val local = file?.local
            val kind = when (m.content) {
                is TdApi.MessagePhoto -> KxMsgKind.Photo; is TdApi.MessageVideo -> KxMsgKind.Video; is TdApi.MessageAnimation -> KxMsgKind.Gif
                is TdApi.MessageAudio -> KxMsgKind.Music; is TdApi.MessageVoiceNote -> KxMsgKind.Voice; is TdApi.MessageVideoNote -> KxMsgKind.VideoNote
                else -> KxMsgKind.File
            }
            val name = media?.title?.takeIf { it.isNotBlank() } ?: when (kind) { KxMsgKind.Photo -> "Фото"; KxMsgKind.Video -> "Видео"; KxMsgKind.Music -> "Аудио"; else -> "Файл" }
            val size = file?.let { if (it.size > 0) it.size else it.expectedSize } ?: media?.size ?: 0
            KxDownload(
                fileId = d.fileId, chatId = m.chatId, messageId = m.id, name = name, kind = kind, size = size,
                downloaded = local?.downloadedSize ?: 0, paused = d.isPaused, done = d.completeDate > 0 || local?.isDownloadingCompleted == true,
                path = local?.takeIf { it.isDownloadingCompleted && it.path.isNotBlank() }?.path, thumb = autoPath(thumb),
                chatTitle = chatTitle(m.chatId), time = KxFormat.chatTime(if (d.completeDate > 0) d.completeDate else d.addDate), addedAt = d.addDate,
            )
        }
    }
    fun pauseDownload(fileId: Int, paused: Boolean) { client.send(TdApi.ToggleDownloadIsPaused(fileId, paused)) }
    fun removeDownloads(fileIds: List<Int>) {
        fileIds.forEach { id ->
            client.send(TdApi.RemoveFileFromDownloads(id, true))
            vm.files.deleteLocal(TelegramFileRef(id, null))
            fileStates.remove(id); watching.remove(id)
        }
    }

    // ---- Voice ----
    suspend fun voiceMessages(chatId: Long): List<KxVoiceItem> {
        val found = orNull { client.query(TdApi.SearchChatMessages(chatId, null, "", null, 0, 0, 50, TdApi.SearchMessagesFilterVoiceAndVideoNote())) }?.messages.orEmpty()
        return found.mapNotNull { m ->
            val (file, seconds, wave, listened, transcript, video) = when (val c = m.content) {
                is TdApi.MessageVoiceNote -> VoiceParts(c.voiceNote.voice, c.voiceNote.duration, decodeWaveform(c.voiceNote.waveform), c.isListened,
                    (c.voiceNote.speechRecognitionResult as? TdApi.SpeechRecognitionResultText)?.text, false)
                is TdApi.MessageVideoNote -> VoiceParts(c.videoNote.video, c.videoNote.duration, emptyList(), c.isViewed,
                    (c.videoNote.speechRecognitionResult as? TdApi.SpeechRecognitionResultText)?.text, true)
                else -> return@mapNotNull null
            }
            KxVoiceItem(
                chatId = m.chatId, messageId = m.id, sender = if (m.isOutgoing) "Вы" else senderName(m.senderId), out = m.isOutgoing,
                time = KxFormat.dateTime(m.date), seconds = seconds, waveform = wave.map { it.coerceIn(0, 31) / 31f },
                listened = m.isOutgoing || listened, fileId = file.id, path = file.local?.takeIf { it.isDownloadingCompleted && it.path.isNotBlank() }?.path,
                transcript = transcript, video = video, date = m.date,
            )
        }
    }
    private data class VoiceParts(val file: TdApi.File, val seconds: Int, val wave: List<Int>, val listened: Boolean, val transcript: String?, val video: Boolean)

    /** Not in kiroX ghost-read mode (the sender would see «прослушано»). */
    fun markListened(chatId: Long, messageId: Long) {
        if (com.kirox.app.settings.KiroxTweaks.current.ghostRead) return
        client.send(TdApi.OpenMessageContent(chatId, messageId))
    }

    suspend fun transcribe(chatId: Long, messageId: Long, onPartial: (String) -> Unit): KxTextResult {
        val props = orNull { client.query(TdApi.GetMessageProperties(chatId, messageId)) }
        if (props != null && !props.canRecognizeSpeech) return KxTextResult(error = "Расшифровка недоступна: нужен Telegram Premium (или бесплатные попытки этой недели закончились)")
        return transcribeNow(chatId, messageId, onPartial)
    }

    private suspend fun transcribeNow(chatId: Long, messageId: Long, onPartial: (String) -> Unit): KxTextResult = try {
        KxTextResult(text = services.extras.transcribe(chatId, messageId) { p -> scope.launch { onPartial(p) } })
    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { KxTextResult(error = KxExtrasMapping.errorText(e)) }

    // ---- Stories ----
    private fun storyItem(s: TdApi.Story, active: Boolean): KxStoryItem {
        val t = toStory(s)
        val months = java.text.SimpleDateFormat("LLLL yyyy", java.util.Locale.forLanguageTag("ru"))
        return KxStoryItem(
            id = s.id, date = KxFormat.dayHeader(s.date), media = autoPath(t.image), video = t.isVideo, caption = t.caption,
            views = t.viewCount, reactions = t.reactionCount, onProfile = s.isPostedToChatPage, active = active,
            month = months.format(java.util.Date(s.date * 1000L)).replaceFirstChar { it.uppercase() }, unix = s.date, fileId = t.video?.id ?: 0,
        )
    }
    private fun myChat() = services.users.myId()

    suspend fun storyArchive(): List<KxStoryItem> {
        val now = System.currentTimeMillis() / 1000
        val out = ArrayList<KxStoryItem>()
        var from = 0
        repeat(5) {
            val page = orNull { client.query(TdApi.GetChatArchivedStories(myChat(), from, 100)) }?.stories.orEmpty()
            if (page.isEmpty()) return out
            val fresh = page.filter { st -> out.none { it.id == st.id } }
            if (fresh.isEmpty()) return out
            out += fresh.map { storyItem(it, now - it.date < 86400) }
            from = page.minOf { it.id }
        }
        return out
    }

    suspend fun profileStories(): List<KxStoryItem> {
        val now = System.currentTimeMillis() / 1000
        return orNull { client.query(TdApi.GetChatPostedToChatPageStories(myChat(), 0, 100)) }?.stories.orEmpty().map { storyItem(it, now - it.date < 86400) }
    }

    suspend fun setStoryOnProfile(storyId: Int, on: Boolean): String? = attempt { client.query(TdApi.ToggleStoryIsPostedToChatPage(myChat(), storyId, on)) }

    suspend fun pinnedStoryIds(): List<Int> = orNull { client.query(TdApi.GetChatPostedToChatPageStories(myChat(), 0, 100)) }?.pinnedStoryIds?.toList().orEmpty()

    suspend fun pinnedStoryMax(): Int =
        (orNull { client.query(TdApi.GetOption("pinned_story_count_max")) } as? TdApi.OptionValueInteger)?.value?.toInt()?.takeIf { it > 0 } ?: 3

    suspend fun pinStories(ids: List<Int>): String? = attempt {
        val onProfile = orNull { client.query(TdApi.GetChatPostedToChatPageStories(myChat(), 0, 100)) }?.stories.orEmpty().map { it.id }.toSet()
        ids.filter { it !in onProfile }.forEach { client.query(TdApi.ToggleStoryIsPostedToChatPage(myChat(), it, true)) }
        client.query(TdApi.SetChatPinnedStories(myChat(), ids.take(pinnedStoryMax()).toIntArray()))
    }

    suspend fun deviceMedia(limit: Int): List<KxDeviceMedia> = withContext(Dispatchers.IO) {
        runCatching {
            val collection = android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(android.provider.MediaStore.Files.FileColumns._ID, android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE, android.provider.MediaStore.Video.VideoColumns.DURATION)
            val image = android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            val videoType = android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            val args = android.os.Bundle().apply {
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} IN ($image, $videoType)")
                putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(android.provider.MediaStore.Files.FileColumns.DATE_ADDED))
                putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
            }
            app.contentResolver.query(collection, projection, args, null)?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val video = c.getInt(1) == videoType
                        val base = if (video) android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI else android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        add(KxDeviceMedia(android.content.ContentUris.withAppendedId(base, c.getLong(0)).toString(), video, if (video) c.getLong(2) else 0L))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    /** TDLib uploads local files only: the picked item is copied into the app cache first. */
    private suspend fun localCopy(media: KxDeviceMedia): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = java.io.File(app.cacheDir, "v3-picked").apply { mkdirs() }
            dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 3600_000L }?.forEach { it.delete() }
            val out = java.io.File(dir, "${media.uri.hashCode().toUInt()}.${if (media.video) "mp4" else "jpg"}")
            app.contentResolver.openInputStream(android.net.Uri.parse(media.uri))!!.use { input -> out.outputStream().use { input.copyTo(it) } }
            out.absolutePath
        }.getOrNull()
    }

    suspend fun postStory(media: KxDeviceMedia, caption: String, audience: Int): String? {
        val path = localCopy(media) ?: return "Не удалось открыть файл"
        val aud = com.kirox.app.telegram.StoryAudience.entries.getOrElse(audience) { com.kirox.app.telegram.StoryAudience.Everyone }
        return attempt { services.stories.postStory(path, media.video, ((media.durationMs + 999) / 1000).toInt().coerceIn(1, 60).takeIf { media.video } ?: 0, caption, aud) }
    }

    suspend fun sendDeviceMedia(chatId: Long, media: KxDeviceMedia, caption: String): String? {
        val path = localCopy(media) ?: return "Не удалось открыть файл"
        val text = caption.takeIf { it.isNotBlank() }?.let { TdApi.FormattedText(it, emptyArray()) }
        val seconds = ((media.durationMs + 999) / 1000).toInt()
        val content: TdApi.InputMessageContent = if (media.video) TdApi.InputMessageVideo().apply {
            video = TdApi.InputVideo().apply { video = TdApi.InputFileLocal(path); addedStickerFileIds = IntArray(0); duration = seconds; supportsStreaming = true }
            this.caption = text
        } else TdApi.InputMessagePhoto().apply {
            photo = TdApi.InputPhoto().apply { photo = TdApi.InputFileLocal(path); addedStickerFileIds = IntArray(0) }
            this.caption = text
        }
        return attempt { client.query(TdApi.SendMessage(chatId, null, null, null, null, content)) }
    }

    suspend fun closeFriends(): Set<Long> = services.extras.closeFriends()
    suspend fun privateChat(userId: Long): Long? = orNull { vm.chats.privateChatId(userId) }

    // ---- Calls ----
    var activeCall by mutableStateOf<KxCallState?>(null)
        private set
    var groupCall by mutableStateOf<KxGroupCallState?>(null)
        private set
    var lastCall by mutableStateOf<KxCallState?>(null)
        private set

    private fun startCalls() {
        scope.launch {
            vm.voip.call.collect { c ->
                val mapped = c?.let { KxMediaMapping.call(it, autoPath(it.photo)) }
                if (mapped != null && (mapped.phase == KxCallPhase.Ended || mapped.phase == KxCallPhase.Failed)) lastCall = mapped
                activeCall = mapped?.takeIf { it.phase != KxCallPhase.Ended && it.phase != KxCallPhase.Failed }
            }
        }
        scope.launch { vm.groupCalls.call.collect { g -> groupCall = g?.let(::group) } }
    }

    private fun group(g: TelegramGroupCall) = KxGroupCallState(
        chatId = g.chatId, title = g.title, count = g.count, muted = g.muted, canUnmute = g.canUnmute, cameraOn = g.cameraOn,
        connected = g.phase == GroupCallPhase.Connected, reconnecting = g.phase == GroupCallPhase.Reconnecting,
        failure = (g.phase as? GroupCallPhase.Failed)?.message.orEmpty(),
        members = g.members.map { m ->
            KxCallMember(m.key, if (m.isSelf) "Вы" else m.name, autoPath(m.photo), m.isSelf, m.speaking, m.muted, m.handRaised, m.videoEndpoint != null)
        },
    )

    fun answerCall() = vm.voip.accept()
    fun hangUp() = vm.voip.hangUp()
    fun muteCallMic(muted: Boolean) = vm.voip.setMuted(muted)
    fun turnCallCamera(on: Boolean) = vm.voip.setCamera(on)
    fun flipCallCamera() = vm.voip.switchCamera()
    fun attachCallVideo(remote: Any?, local: Any?) = vm.voip.setVideoSinks(remote as? org.webrtc.VideoSink, local as? org.webrtc.VideoSink)
    fun joinGroupCall(chatId: Long) = vm.groupCalls.join(chatId)
    fun leaveGroupCall() = vm.groupCalls.leave()
    fun muteGroupMic(muted: Boolean) = vm.groupCalls.setMuted(muted)
    fun turnGroupCamera(on: Boolean) = vm.groupCalls.setCamera(on)

    suspend fun rateCall(callId: Int, rating: Int, comment: String, problems: List<String>): String? =
        attempt { client.query(TdApi.SendCallRating(TdApi.InputCallDiscarded(callId), rating.coerceIn(1, 5), comment.trim(), KxMediaMapping.problems(problems))) }

    suspend fun planVideoChat(chatId: Long, title: String, startAt: Int, rtmp: Boolean): String? =
        attempt { client.query(TdApi.CreateVideoChat(chatId, title.trim(), startAt, rtmp)) }

    // ---- Traffic / space / net / sounds ----
    suspend fun networkStats(): KxTraffic? {
        val stats = orNull { client.query(TdApi.GetNetworkStatistics(false)) } ?: return null
        val rows = stats.entries.mapNotNull { e ->
            when (e) {
                is TdApi.NetworkStatisticsEntryFile -> KxMediaMapping.network(e.networkType).takeIf { it >= 0 }?.let { KxTrafficRow(it, KxMediaMapping.category(e.fileType), e.sentBytes, e.receivedBytes) }
                is TdApi.NetworkStatisticsEntryCall -> KxMediaMapping.network(e.networkType).takeIf { it >= 0 }?.let { KxTrafficRow(it, "calls", e.sentBytes, e.receivedBytes) }
                else -> null
            }
        }.groupBy { it.network to it.category }.map { (k, v) -> KxTrafficRow(k.first, k.second, v.sumOf { it.sent }, v.sumOf { it.received }) }
        return KxTraffic(if (stats.sinceDate > 0) KxFormat.dayHeader(stats.sinceDate) else "", rows)
    }
    suspend fun resetNetworkStats(): String? = attempt { client.query(TdApi.ResetNetworkStatistics()) }

    suspend fun deviceSpace(): Pair<Long, Long>? = withContext(Dispatchers.IO) {
        runCatching { val s = android.os.StatFs(app.filesDir.absolutePath); s.availableBytes to s.totalBytes }.getOrNull()
    }

    suspend fun netCheck(): KxNetCheck {
        val network = when (services.network.network.value) {
            com.kirox.app.telegram.DeviceNetwork.WiFi -> "Wi-Fi"; com.kirox.app.telegram.DeviceNetwork.Mobile -> "Мобильная сеть"
            com.kirox.app.telegram.DeviceNetwork.MobileRoaming -> "Роуминг"; com.kirox.app.telegram.DeviceNetwork.None -> "Нет сети"
            com.kirox.app.telegram.DeviceNetwork.Other -> "Другая сеть"
        }
        val conn = when (vm.connection.state.value) {
            com.kirox.app.telegram.TelegramConnection.Ready -> "Подключено"
            com.kirox.app.telegram.TelegramConnection.WaitingForNetwork -> "Ожидание сети"
            com.kirox.app.telegram.TelegramConnection.ConnectingToProxy -> "Подключение к прокси"
            com.kirox.app.telegram.TelegramConnection.Connecting -> "Соединение"
            com.kirox.app.telegram.TelegramConnection.Updating -> "Обновление"
        }
        val online = services.network.network.value != com.kirox.app.telegram.DeviceNetwork.None
        val ping = if (online) withTimeoutOrNull(15_000) { orNull { client.query(TdApi.PingProxy(null)).seconds } }?.let { (it * 1000).toInt() } else null
        val proxy = orNull { client.query(TdApi.GetProxies()) }?.proxies?.firstOrNull { it.isEnabled }
        val proxyPing = if (proxy != null && online) withTimeoutOrNull(15_000) { orNull { client.query(TdApi.PingProxy(proxy.proxy)).seconds } }?.let { (it * 1000).toInt() } else null
        val dns = if (!online) null else withContext(Dispatchers.IO) {
            withTimeoutOrNull(5_000) { runCatching { java.net.InetAddress.getByName("telegram.org"); true }.getOrDefault(false) } ?: false
        }
        return KxNetCheck(online, network, conn, ping, proxy?.proxy?.let { "${it.server}:${it.port}" }, proxyPing, dns)
    }

    private fun scope(key: String): TdApi.NotificationSettingsScope? = when (key) {
        "private" -> TdApi.NotificationSettingsScopePrivateChats(); "groups" -> TdApi.NotificationSettingsScopeGroupChats()
        "channels" -> TdApi.NotificationSettingsScopeChannelChats(); else -> null
    }
    private val soundFiles = ConcurrentHashMap<Long, TdApi.File>()
    suspend fun notificationSounds(): List<KxSound> {
        val saved = orNull { client.query(TdApi.GetSavedNotificationSounds()) }?.notificationSounds.orEmpty()
        saved.forEach { soundFiles[it.id] = it.sound }
        return listOf(KxSound(1, "По умолчанию"), KxSound(0, "Без звука")) + saved.map { KxSound(it.id, it.title.ifBlank { "Звук" }, it.duration, it.sound.id) }
    }
    suspend fun scopeSound(key: String): Long {
        val s = scope(key) ?: return -1
        return orNull { client.query(TdApi.GetScopeNotificationSettings(s)) }?.soundId?.let { if (it == -1L || it == 1L) 1L else it } ?: -1
    }
    suspend fun changeScopeSound(key: String, soundId: Long): String? {
        val s = scope(key) ?: return "Неизвестный раздел"
        return attempt {
            val cur = client.query(TdApi.GetScopeNotificationSettings(s))
            cur.soundId = if (soundId == 1L) -1L else soundId // -1: Telegram's default sound
            client.query(TdApi.SetScopeNotificationSettings(s, cur))
        }
    }
    fun previewSound(soundId: Long) {
        if (soundId == 0L) return
        if (soundId == 1L) {
            runCatching {
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                android.media.RingtoneManager.getRingtone(app, uri)?.play()
            }
            return
        }
        val f = soundFiles[soundId] ?: return
        scope.launch {
            val ref = TelegramFileRef(f.id, null)
            vm.files.download(ref)
            val path = withTimeoutOrNull(20_000) { vm.files.state(ref).first { it.path != null }.path } ?: return@launch
            runCatching { android.media.MediaPlayer().apply { setDataSource(path); setOnCompletionListener { it.release() }; prepare(); start() } }
        }
    }

    fun start() { startPlayer(); startCalls() }
}
