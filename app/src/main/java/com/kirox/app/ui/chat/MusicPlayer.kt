package com.kirox.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirox.app.telegram.TelegramMedia
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The open chat's music files, oldest first: the queue a tap on one of them plays. */
val LocalChatTracks = compositionLocalOf { emptyList<Track>() }

internal fun TelegramMedia.toTrack() = Track(file, title.ifBlank { "Аудио" }, performer.ifBlank { "kiroX" }, duration)

/** Whether the full music player is open; any screen can open it. */
object MusicPlayerUi {
    private val _open = MutableStateFlow(false)
    val isOpen = _open.asStateFlow()
    fun open() { _open.value = true }
    fun close() { _open.value = false }
}

private fun time(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

/** Re-reads the player position a few times a second while shown. */
@Composable
private fun rememberPosition(): Pair<Long, Long> {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            position = VoicePlayer.positionMs.toLong(); duration = VoicePlayer.durationMs
            kotlinx.coroutines.delay(250)
        }
    }
    return position to duration
}

@Composable
private fun RoundButton(label: String, size: Int = 48, filled: Boolean = false, active: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(if (filled) Fusion.colors.accent else androidx.compose.ui.graphics.Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (filled) Fusion.colors.onAccent else if (active) Fusion.colors.textPrimary else Fusion.colors.textTertiary, fontSize = (size / 2.6f).sp, fontWeight = FontWeight.Bold)
    }
}

/** A thin bar over a chat while music plays: title, play / pause, next; a tap opens the player. */
@Composable
fun MiniPlayerBar() {
    val track by VoicePlayer.track.collectAsState()
    val playingPath by VoicePlayer.playingPath.collectAsState()
    val loading by VoicePlayer.loading.collectAsState()
    val current = track ?: return
    val context = LocalContext.current
    val (position, duration) = rememberPosition()
    Column(Modifier.fillMaxWidth().background(Fusion.colors.surface1.copy(alpha = .96f)).clickable { MusicPlayerUi.open() }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (loading) Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), color = Fusion.colors.accent, strokeWidth = 2.dp) }
            else RoundButton(if (playingPath != null) "❚❚" else "▶", size = 40) { VoicePlayer.playPause() }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(current.title, color = Fusion.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(current.artist, color = Fusion.colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            RoundButton("⏭", size = 40) { VoicePlayer.next(context) }
            RoundButton("✕", size = 40) { VoicePlayer.stop() }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Fusion.colors.surface3)) {
            Box(Modifier.fillMaxWidth(if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f).fillMaxHeight().background(Fusion.colors.accent))
        }
    }
}

/** The full music player: cover, seek bar, controls, repeat, shuffle, speed, sleep timer and the queue. */
@Composable
fun MusicPlayerSheet() {
    val open by MusicPlayerUi.isOpen.collectAsState()
    val track by VoicePlayer.track.collectAsState()
    if (!open) return
    val current = track ?: run { MusicPlayerUi.close(); return }
    val context = LocalContext.current
    val playingPath by VoicePlayer.playingPath.collectAsState()
    val loading by VoicePlayer.loading.collectAsState()
    val cover by VoicePlayer.cover.collectAsState()
    val repeat by VoicePlayer.repeat.collectAsState()
    val shuffle by VoicePlayer.shuffle.collectAsState()
    val speed by VoicePlayer.speed.collectAsState()
    val sleepAt by VoicePlayer.sleepAt.collectAsState()
    val queue by VoicePlayer.queue.collectAsState()
    val (position, duration) = rememberPosition()
    var dragging by remember { mutableStateOf<Float?>(null) }
    var sleepMenu by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = MusicPlayerUi::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler { if (showQueue) showQueue = false else MusicPlayerUi.close() }
        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(Fusion.colors.backdrop ?: listOf(Fusion.colors.background, Fusion.colors.background))).systemBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = MusicPlayerUi::close), contentAlignment = Alignment.Center) {
                    com.kirox.app.ui.icons.KiroXIcon(KiroXIcon.Back, "Закрыть", Modifier.size(22.dp), Fusion.colors.textPrimary)
                }
                Text(if (showQueue) "Очередь" else "Музыка", color = Fusion.colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Text("Библиотека", color = Fusion.colors.accent, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { MusicPlayerUi.close(); com.kirox.app.ui.music.MusicLibraryUi.open() }.padding(8.dp))
                if (queue.size > 1) Text(
                    if (showQueue) "Плеер" else "Очередь (${queue.size})", color = Fusion.colors.accent, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { showQueue = !showQueue }.padding(8.dp)
                )
            }
            if (showQueue) {
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(queue, key = { _, t -> t.file.id }) { i, t ->
                        val here = t.file.id == current.file.id
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { VoicePlayer.playQueue(context, queue, i) }.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (here) "▶" else "${i + 1}", color = if (here) Fusion.colors.accent else Fusion.colors.textTertiary, fontSize = 13.sp, modifier = Modifier.width(32.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.title, color = if (here) Fusion.colors.accent else Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(t.artist, color = Fusion.colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(time(t.durationSec * 1000L), color = Fusion.colors.textTertiary, fontSize = 12.sp)
                        }
                    }
                }
                return@Column
            }
            Spacer(Modifier.weight(.6f))
            Box(
                Modifier.fillMaxWidth(.82f).aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(Fusion.colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = cover
                if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else com.kirox.app.ui.icons.KiroXIcon(KiroXIcon.Music, null, Modifier.size(96.dp), Fusion.colors.textTertiary)
                if (loading) CircularProgressIndicator(Modifier.size(48.dp), color = Fusion.colors.accent)
            }
            Spacer(Modifier.height(28.dp))
            Text(current.title, color = Fusion.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(current.artist, color = Fusion.colors.textSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            // ♥ and «В плейлист» for tracks that come from a message.
            if (current.chatId != 0L) {
                val playlists by com.kirox.app.settings.Playlists.all.collectAsState()
                val liked = playlists.firstOrNull { it.id == com.kirox.app.settings.Playlists.LIKED }?.has(current.chatId, current.messageId) == true
                var adding by remember { mutableStateOf(false) }
                var added by remember { mutableStateOf<String?>(null) }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Chip(if (liked) "♥ В любимом" else "♡ В любимое", active = liked) { com.kirox.app.settings.Playlists.toggleLiked(current.toItemForPlaylist()) }
                    Chip(added ?: "+ В плейлист") { adding = true }
                }
                if (adding) com.kirox.app.ui.music.AddToPlaylistDialog(current.toItemForPlaylist()) { result -> adding = false; added = result?.let { "Добавлено" } }
            }
            Spacer(Modifier.height(20.dp))
            val fraction = dragging ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            Slider(
                value = fraction,
                onValueChange = { dragging = it },
                onValueChangeFinished = { dragging?.let(VoicePlayer::seekTo); dragging = null },
                colors = SliderDefaults.colors(thumbColor = Fusion.colors.accent, activeTrackColor = Fusion.colors.accent, inactiveTrackColor = Fusion.colors.surface3)
            )
            Row(Modifier.fillMaxWidth()) {
                Text(time((fraction * duration).toLong()), color = Fusion.colors.textTertiary, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(time(duration), color = Fusion.colors.textTertiary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                RoundButton("⇄", active = shuffle) { VoicePlayer.toggleShuffle() }
                RoundButton("⏮", size = 56) { VoicePlayer.previous(context) }
                RoundButton(if (playingPath != null) "❚❚" else "▶", size = 72, filled = true) { VoicePlayer.playPause() }
                RoundButton("⏭", size = 56) { VoicePlayer.next(context) }
                RoundButton(if (repeat == RepeatMode.One) "↻¹" else "↻", active = repeat != RepeatMode.Off) { VoicePlayer.cycleRepeat() }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Chip("−15 с") { VoicePlayer.skipBy(-15_000) }
                Chip("${if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()}×") { VoicePlayer.cycleSpeed() }
                Box {
                    val minutesLeft = if (sleepAt > 0) ((sleepAt - System.currentTimeMillis()) / 60_000 + 1).coerceAtLeast(1) else 0
                    Chip(if (sleepAt > 0) "Сон: $minutesLeft мин" else "Таймер сна", active = sleepAt > 0) { sleepMenu = true }
                    androidx.compose.material3.DropdownMenu(sleepMenu, onDismissRequest = { sleepMenu = false }, containerColor = Fusion.colors.surface1) {
                        listOf(15, 30, 45, 60, 90).forEach { m ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text("$m мин", color = Fusion.colors.textPrimary) }, onClick = { sleepMenu = false; VoicePlayer.sleepIn(m) })
                        }
                        if (sleepAt > 0) androidx.compose.material3.DropdownMenuItem(text = { Text("Выключить таймер", color = Fusion.colors.danger) }, onClick = { sleepMenu = false; VoicePlayer.sleepIn(0) })
                    }
                }
                Chip("+15 с") { VoicePlayer.skipBy(15_000) }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean = false, onClick: () -> Unit) {
    Text(
        label, color = if (active) Fusion.colors.accent else Fusion.colors.textSecondary, fontSize = 13.sp,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Fusion.colors.surface2).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

internal fun Track.toItemForPlaylist() = com.kirox.app.settings.PlaylistItem(chatId, messageId, title, artist, durationSec)
