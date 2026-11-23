package com.kirox.app.ui.v3.model

import androidx.compose.runtime.Immutable

/** A chat the music comes from (a music channel, a group, Saved Messages). [added]: the user added it by hand. */
@Immutable
data class KxMusicChannel(val chatId: Long, val title: String, val count: Int, val avatar: String? = null, val added: Boolean = false,
                          /** 0 channel, 1 group, 2 Saved Messages, 3 private chat. */
                          val kind: Int = 0)

/** A playlist; [MY_MUSIC] is «Моя музыка» (always there, first). Tracks without a [KxTrack.fileId] are looked up on play. */
@Immutable
data class KxPlaylist(val id: Long, val name: String, val tracks: List<KxTrack>) {
    val mine: Boolean get() = id == MY_MUSIC
    val totalSeconds: Int get() = tracks.sumOf { it.duration }
    companion object { const val MY_MUSIC = 0L }
}

/** An audio output; [kind] 0 phone, 1 wired, 2 Bluetooth, 3 USB; [battery] 0–100 or -1. */
@Immutable
data class KxAudioRoute(val id: Int, val name: String, val kind: Int, val battery: Int = -1)

@Immutable
data class KxAudioOutput(val routes: List<KxAudioRoute> = emptyList(), val current: KxAudioRoute? = null, val chosen: Int? = null,
                         val volume: Int = 0, val maxVolume: Int = 15, val needsBluetoothPermission: Boolean = false)

/**
 * The music library and player extras of the Music tab / board. Reached as [KxStore.music]; every
 * member has a harmless default so the demo store (and tests) need nothing.
 */
interface KxMusic {
    // ---- Sources ----
    /** The user's music channels; null while the first scan runs. */
    val channels: List<KxMusicChannel>? get() = emptyList()
    /** A scan of the user's chats is running. */
    val scanning: Boolean get() = false
    /** Looks through the user's chats for music (cached for a day unless [force]). */
    fun scanChannels(force: Boolean = false) {}
    /** One page of a chat's music, newest first; the next page starts at the returned message id (0 = end). */
    suspend fun channelTracks(chatId: Long, fromMessageId: Long): Pair<List<KxTrack>, Long> = emptyList<KxTrack>() to 0L
    /** Chats to add as a source (title search; empty = the user's chats with music counts). */
    suspend fun channelCandidates(query: String): List<KxMusicChannel> = emptyList()
    fun addChannel(channel: KxMusicChannel) {}
    fun removeChannel(chatId: Long) {}
    /** Music found by title / performer in all the user's chats. */
    suspend fun search(query: String): List<KxTrack> = emptyList()

    // ---- Моя музыка and playlists ----
    /** «Моя музыка» first, then the user's playlists. */
    val playlists: List<KxPlaylist> get() = emptyList()
    fun isMine(chatId: Long, messageId: Long): Boolean = false
    /** Adds the track to «Моя музыка», or removes it when it is there. */
    fun toggleMine(track: KxTrack) {}
    /** A new playlist; returns its id. */
    fun createPlaylist(name: String): Long = -1
    fun renamePlaylist(id: Long, name: String) {}
    fun deletePlaylist(id: Long) {}
    fun addToPlaylist(id: Long, track: KxTrack) {}
    fun removeFromPlaylist(id: Long, chatId: Long, messageId: Long) {}
    fun movePlaylistItem(id: Long, from: Int, to: Int) {}
    /** Plays a list (playlist, channel page) from [index]; tracks without a file id are looked up first. */
    fun play(tracks: List<KxTrack>, index: Int, source: String, shuffle: Boolean = false) {}

    // ---- Player extras ----
    val speed: Float get() = 1f
    fun setSpeed(speed: Float) {}
    /** Wall-clock time the sleep timer stops the music; 0 = off. */
    val sleepAt: Long get() = 0L
    fun sleepIn(minutes: Int) {}
    /** Index of the current track in [KxPlayerState.queue]. */
    val queueIndex: Int get() = -1
    fun playQueueAt(index: Int) {}
    fun moveInQueue(from: Int, to: Int) {}
    fun removeFromQueue(index: Int) {}
    fun playNext(track: KxTrack) {}
    /** Seek to an exact place (the draggable seek bar). */
    fun seekMs(ms: Long) {}
    /** The current track's cover (the file's own picture or Telegram's album thumbnail); null = none yet. */
    val cover: androidx.compose.ui.graphics.ImageBitmap? get() = null

    // ---- Output ----
    val output: KxAudioOutput get() = KxAudioOutput()
    /** Starts / stops following outputs, battery and volume (while the player is on screen). */
    fun watchOutput(on: Boolean) {}
    /** Reads outputs again (after BLUETOOTH_CONNECT was granted). */
    fun refreshOutput() {}
    /** Sends the music to an output of [KxAudioOutput.routes]; null = the system decides. */
    fun pickOutput(id: Int?) {}
    fun setVolume(volume: Int) {}
    /** Android's own output switcher; false when there is none (the player's list is shown). */
    fun openSystemOutputSwitcher(context: android.content.Context): Boolean = false

    companion object { val None: KxMusic = object : KxMusic {} }
}
