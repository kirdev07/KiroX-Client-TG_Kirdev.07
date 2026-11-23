package com.kirox.app.ui.v3.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import com.kirox.app.media.music.AudioOutput
import com.kirox.app.media.music.MusicSource
import com.kirox.app.media.music.MusicSourceKind
import com.kirox.app.media.music.MusicSourceRules
import com.kirox.app.media.music.MusicSourceStore
import com.kirox.app.media.music.OutputKind
import com.kirox.app.settings.PlaylistItem
import com.kirox.app.settings.Playlists
import com.kirox.app.telegram.TelegramAudio
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.telegram.toRef
import com.kirox.app.ui.chat.Track
import com.kirox.app.ui.chat.VoicePlayer
import com.kirox.app.ui.v3.model.KxAudioOutput
import com.kirox.app.ui.v3.model.KxAudioRoute
import com.kirox.app.ui.v3.model.KxMusic
import com.kirox.app.ui.v3.model.KxMusicChannel
import com.kirox.app.ui.v3.model.KxPlaylist
import com.kirox.app.ui.v3.model.KxTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.drinkless.tdlib.TdApi

/**
 * The Music tab on the real account: the user's music channels (found with Telegram's audio
 * counter on their chats, plus chats added by hand), «Моя музыка» and playlists (settings/Playlists,
 * the same store the old library used), and the player extras on top of [VoicePlayer].
 */
internal class TelegramKxMusic(
    private val app: Application,
    private val scope: CoroutineScope,
    private val media: KxMediaBinder,
    /** Path of a small file, starting its download (avatars, covers). */
    private val autoPath: (TelegramFileRef?) -> String?,
) : KxMusic {
    private val services get() = (app as com.kirox.app.KiroXApplication).telegram
    private val client get() = services.client
    private val sources = MusicSourceStore(app)
    private val audioOut by lazy { AudioOutput.get(app) }

    // ---- Sources ----
    private var channelList by mutableStateOf<List<KxMusicChannel>?>(null)
    override val channels: List<KxMusicChannel>? get() = channelList
    private var scanningNow by mutableStateOf(false)
    override val scanning: Boolean get() = scanningNow
    private val avatars = mutableStateMapOf<Long, String>()
    private var scanJob: Job? = null
    private val avatarAsked = HashSet<Long>()

    private fun MusicSourceKind.code() = when (this) { MusicSourceKind.Channel -> 0; MusicSourceKind.Group -> 1; MusicSourceKind.Saved -> 2; MusicSourceKind.Private -> 3 }
    private fun kindOf(code: Int) = when (code) { 0 -> MusicSourceKind.Channel; 1 -> MusicSourceKind.Group; 2 -> MusicSourceKind.Saved; else -> MusicSourceKind.Private }

    private fun publishChannels(scanned: List<MusicSource> = sources.scanned) {
        val added = sources.added
        val known = (scanned + added).associateBy { it.chatId }
        val visible = MusicSourceRules.visible(scanned, added.map { it.chatId }, sources.removed, known)
        val addedIds = added.map { it.chatId }.toSet()
        channelList = visible.map { KxMusicChannel(it.chatId, it.title, it.audioCount, avatars[it.chatId], it.chatId in addedIds, it.kind.code()) }
        visible.forEach { s -> if (avatars[s.chatId] == null && avatarAsked.add(s.chatId)) scope.launch { avatar(s.chatId)?.let { p -> avatars[s.chatId] = p; refreshAvatars() } } }
    }
    private fun refreshAvatars() { channelList = channelList?.map { c -> avatars[c.chatId]?.let { c.copy(avatar = it) } ?: c } }

    private suspend fun avatar(chatId: Long): String? = orNull { client.query(TdApi.GetChat(chatId)) }?.photo?.small?.let { autoPath(it.toRef()) }

    private var meId = 0L
    private suspend fun kindOf(chat: TdApi.Chat): MusicSourceKind? = when (val t = chat.type) {
        is TdApi.ChatTypeSupergroup -> if (t.isChannel) MusicSourceKind.Channel else MusicSourceKind.Group
        is TdApi.ChatTypeBasicGroup -> MusicSourceKind.Group
        is TdApi.ChatTypePrivate -> {
            if (meId == 0L) meId = orNull { client.query(TdApi.GetMe()).id } ?: 0L
            if (t.userId == meId) MusicSourceKind.Saved else MusicSourceKind.Private
        }
        else -> null
    }

    private suspend fun audioCount(chatId: Long): Int =
        orNull { client.query(TdApi.GetChatMessageCount(chatId, null, TdApi.SearchMessagesFilterAudio(), false)).count } ?: 0

    private suspend fun sourceOf(chatId: Long, countPrivate: Boolean): MusicSource? {
        val chat = orNull { client.query(TdApi.GetChat(chatId)) } ?: return null
        val kind = kindOf(chat) ?: return null
        if (kind == MusicSourceKind.Private && !countPrivate) return null
        return MusicSource(chatId, if (kind == MusicSourceKind.Saved) "Избранное" else chat.title, kind, audioCount(chatId))
    }

    override fun scanChannels(force: Boolean) {
        val scannedAt = sources.scannedAt
        // Before the first scan the list stays null («ищем музыку»), unless the user added chats already.
        if (channelList == null && (scannedAt > 0 || sources.added.isNotEmpty())) publishChannels()
        val fresh = scannedAt > 0 && System.currentTimeMillis() - scannedAt < SCAN_TTL_MS
        if ((fresh && !force) || scanJob?.isActive == true) return
        scanJob = scope.launch {
            scanningNow = true
            try {
                val ids = LinkedHashSet<Long>()
                orNull { client.query(TdApi.GetChats(TdApi.ChatListMain(), 300)) }?.chatIds?.forEach { ids += it }
                orNull { client.query(TdApi.GetChats(TdApi.ChatListArchive(), 150)) }?.chatIds?.forEach { ids += it }
                // A few requests at a time: hundreds of chats must not run into Telegram's flood limits.
                val gate = Semaphore(4)
                val first = sources.scannedAt == 0L
                val found = ArrayList<MusicSource>()
                ids.toList().chunked(24).forEach { chunk ->
                    found += chunk.map { id -> async { gate.withPermit { sourceOf(id, countPrivate = false) } } }.awaitAll().filterNotNull()
                    // The first scan shows channels as they are found (a few hundred chats take a while).
                    if (first) publishChannels(found.filter { it.audioCount > 0 })
                }
                if (ids.isNotEmpty()) sources.scanned = found.filter { it.audioCount > 0 }
                // Counts of hand-added chats follow too.
                sources.added = sources.added.map { a -> found.firstOrNull { it.chatId == a.chatId } ?: a }
            } finally {
                scanningNow = false
                publishChannels()
            }
        }
    }

    override suspend fun channelTracks(chatId: Long, fromMessageId: Long): Pair<List<KxTrack>, Long> {
        val (list, next) = orNull { services.music.chat(chatId, fromMessageId, 50) } ?: return emptyList<KxTrack>() to 0L
        return list.map { it.kx() }.also(media::rememberTracks) to next
    }

    override suspend fun channelCandidates(query: String): List<KxMusicChannel> {
        val addedIds = sources.added.map { it.chatId }.toSet()
        val visibleIds = channelList.orEmpty().map { it.chatId }.toSet()
        val list: List<MusicSource> = if (query.isBlank()) {
            // Chats seen in the scan (any music at all), hidden ones included, so they can come back.
            sources.scanned.filter { it.chatId !in visibleIds }.sortedByDescending { it.audioCount }
        } else {
            val ids = orNull { client.query(TdApi.SearchChats(query.trim(), null, 30)) }?.chatIds?.toList().orEmpty() +
                orNull { client.query(TdApi.SearchChatsOnServer(query.trim(), null, 20)) }?.chatIds?.toList().orEmpty()
            val gate = Semaphore(4)
            ids.distinct().take(30).map { id -> scope.async { gate.withPermit { sourceOf(id, countPrivate = true) } } }.awaitAll().filterNotNull()
        }
        return list.map { KxMusicChannel(it.chatId, it.title, it.audioCount, avatars[it.chatId] ?: avatar(it.chatId)?.also { p -> avatars[it.chatId] = p }, it.chatId in addedIds, it.kind.code()) }
    }

    override fun addChannel(channel: KxMusicChannel) {
        val s = MusicSource(channel.chatId, channel.title, kindOf(channel.kind), channel.count)
        sources.added = sources.added.filterNot { it.chatId == s.chatId } + s
        sources.removed = sources.removed - s.chatId
        publishChannels()
    }

    override fun removeChannel(chatId: Long) {
        sources.added = sources.added.filterNot { it.chatId == chatId }
        sources.removed = sources.removed + chatId
        publishChannels()
    }

    override suspend fun search(query: String): List<KxTrack> {
        if (query.isBlank()) return emptyList()
        return orNull { services.music.all(query.trim(), "", 100).first }.orEmpty().map { it.kx() }.also(media::rememberTracks)
    }

    // ---- Моя музыка and playlists ----
    private val resolved = mutableStateMapOf<Pair<Long, Long>, KxTrack>()
    /** Messages that are gone (deleted, no access): not looked up again in this run. */
    private val missing = HashSet<Pair<Long, Long>>()
    private var playlistState by mutableStateOf<List<com.kirox.app.settings.Playlist>>(Playlists.all.value)
    private val resolving = HashSet<Pair<Long, Long>>()

    private fun PlaylistItem.kx(): KxTrack = resolved[chatId to messageId] ?: KxTrack(chatId, messageId, title, artist, duration = duration)

    override val playlists: List<KxPlaylist>
        get() = playlistState.map { p -> KxPlaylist(p.id, if (p.id == Playlists.LIKED) "Моя музыка" else p.name, p.items.map { it.kx() }) }

    /** Looks up playlist tracks once (file id, cover, chat name), a few at a time. */
    private fun resolveItems(items: List<PlaylistItem>) {
        val todo = items.map { it.chatId to it.messageId }.filter { it !in resolved && it !in missing && resolving.add(it) }
        if (todo.isEmpty()) return
        scope.launch {
            val gate = Semaphore(4)
            todo.map { key -> async { gate.withPermit { key to orNull { services.music.get(key.first, key.second) } } } }.awaitAll().forEach { (key, a) ->
                resolving.remove(key)
                if (a == null) missing += key else resolved[key] = a.kx()
            }
            media.rememberTracks(resolved.values.toList())
        }
    }

    override fun isMine(chatId: Long, messageId: Long): Boolean = playlistState.firstOrNull { it.id == Playlists.LIKED }?.has(chatId, messageId) == true

    private fun KxTrack.item() = PlaylistItem(chatId, messageId, title, performer, duration)
    private fun remember(t: KxTrack) { if (t.fileId != 0) resolved[t.chatId to t.messageId] = t }

    override fun toggleMine(track: KxTrack) { remember(track); Playlists.toggleLiked(track.item()) }
    override fun createPlaylist(name: String): Long = Playlists.create(name)
    override fun renamePlaylist(id: Long, name: String) = Playlists.rename(id, name)
    override fun deletePlaylist(id: Long) = Playlists.delete(id)
    override fun addToPlaylist(id: Long, track: KxTrack) { remember(track); Playlists.add(id, track.item()) }
    override fun removeFromPlaylist(id: Long, chatId: Long, messageId: Long) = Playlists.remove(id, chatId, messageId)
    override fun movePlaylistItem(id: Long, from: Int, to: Int) = Playlists.moveTo(id, from, to)

    override fun play(tracks: List<KxTrack>, index: Int, source: String, shuffle: Boolean) {
        if (tracks.isEmpty()) return
        val tapped = tracks.getOrNull(index)
        scope.launch {
            val need = tracks.filter { it.fileId == 0 && (it.chatId to it.messageId) !in missing }
            if (need.isNotEmpty()) {
                val gate = Semaphore(4)
                need.map { t -> async { gate.withPermit { t to orNull { services.music.get(t.chatId, t.messageId) } } } }.awaitAll().forEach { (t, a) ->
                    if (a == null) missing += t.chatId to t.messageId else resolved[t.chatId to t.messageId] = a.kx()
                }
            }
            val list = tracks.mapNotNull { t -> if (t.fileId != 0) t else resolved[t.chatId to t.messageId] }
            if (list.isEmpty()) return@launch
            val at = tapped?.let { tp -> list.indexOfFirst { it.chatId == tp.chatId && it.messageId == tp.messageId } }?.takeIf { it >= 0 }
                ?: if (shuffle) list.indices.random() else 0
            if (shuffle && !VoicePlayer.shuffle.value) VoicePlayer.toggleShuffle()
            media.playTracks(list, at, source)
        }
    }

    // ---- Player extras ----
    private var speedNow by mutableStateOf(VoicePlayer.speed.value)
    override val speed: Float get() = speedNow
    override fun setSpeed(speed: Float) = VoicePlayer.setSpeed(speed)
    private var sleepAtNow by mutableStateOf(0L)
    override val sleepAt: Long get() = sleepAtNow
    override fun sleepIn(minutes: Int) = VoicePlayer.sleepIn(minutes)
    private var indexNow by mutableStateOf(-1)
    override val queueIndex: Int get() = indexNow
    override fun playQueueAt(index: Int) = VoicePlayer.playIndex(app, index)
    override fun moveInQueue(from: Int, to: Int) = VoicePlayer.moveInQueue(from, to)
    override fun removeFromQueue(index: Int) = VoicePlayer.removeFromQueue(app, index)
    override fun playNext(track: KxTrack) {
        if (track.fileId == 0) return
        media.rememberTracks(listOf(track))
        VoicePlayer.playNext(app, track.track())
    }
    override fun seekMs(ms: Long) {
        val d = VoicePlayer.durationMs
        if (d > 0) VoicePlayer.seekTo((ms.toFloat() / d).coerceIn(0f, 1f))
    }
    private var coverNow by mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    override val cover: androidx.compose.ui.graphics.ImageBitmap? get() = coverNow

    // ---- Output ----
    private var outputNow by mutableStateOf(KxAudioOutput())
    override val output: KxAudioOutput get() = outputNow
    private var outputJob: Job? = null
    override fun watchOutput(on: Boolean) {
        if (on) {
            audioOut.start()
            if (outputJob == null) outputJob = scope.launch {
                audioOut.state.collect { s ->
                    fun kind(k: OutputKind) = when (k) { OutputKind.Phone -> 0; OutputKind.Wired -> 1; OutputKind.Bluetooth -> 2; OutputKind.Usb -> 3 }
                    val routes = s.routes.map { KxAudioRoute(it.id, it.name, kind(it.kind), it.battery) }
                    outputNow = KxAudioOutput(routes, s.current?.let { c -> routes.firstOrNull { it.id == c.id } }, s.chosen, s.volume, s.maxVolume, s.needsBluetoothPermission)
                }
            }
        } else audioOut.stop()
    }
    override fun refreshOutput() = audioOut.refresh()
    override fun pickOutput(id: Int?) = audioOut.choose(id)
    override fun setVolume(volume: Int) = audioOut.setVolume(volume)
    override fun openSystemOutputSwitcher(context: android.content.Context): Boolean = audioOut.openSystemSwitcher(context)

    // ---- mapping ----
    private fun TelegramAudio.kx() = KxTrack(
        chatId, messageId, title, performer, chatTitle, duration, file.id, size, autoPath(cover), KxFormat.chatTime(date), coverFileId = cover?.id ?: 0,
    )
    private fun KxTrack.track() = Track(TelegramFileRef(fileId, null), title, performer.ifBlank { "kiroX" }, duration, chatId, messageId,
        coverFileId.takeIf { it != 0 }?.let { TelegramFileRef(it, null) })

    fun start() {
        scope.launch { Playlists.all.collect { all -> playlistState = all; all.forEach { resolveItems(it.items) } } }
        scope.launch { VoicePlayer.speed.collect { speedNow = it } }
        scope.launch { VoicePlayer.sleepAt.collect { sleepAtNow = it } }
        scope.launch { VoicePlayer.index.collect { indexNow = it } }
        scope.launch { VoicePlayer.cover.collect { b -> coverNow = b?.asImageBitmap() } }
        restoreSession()
    }

    /** The queue of the last run comes back paused at the same place (nothing downloads until play). */
    private fun restoreSession() {
        if (VoicePlayer.track.value != null) return
        val session = VoicePlayer.savedSession(app) ?: return
        scope.launch {
            val gate = Semaphore(6)
            val found = session.tracks.map { t -> async { gate.withPermit { t to orNull { services.music.get(t.chatId, t.messageId) } } } }.awaitAll()
            val current = session.tracks[session.index]
            val list = found.mapNotNull { (_, a) -> a?.kx() }
            val at = list.indexOfFirst { it.chatId == current.chatId && it.messageId == current.messageId }
            if (at < 0) return@launch
            media.rememberTracks(list)
            list.forEach { resolved[it.chatId to it.messageId] = it }
            VoicePlayer.restore(app, list.map { it.track() }, at, session.positionMs, session.source)
        }
    }

    private companion object { const val SCAN_TTL_MS = 24 * 60 * 60_000L }
}
