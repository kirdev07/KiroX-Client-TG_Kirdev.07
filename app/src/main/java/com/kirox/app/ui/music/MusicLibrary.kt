package com.kirox.app.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirox.app.KiroXApplication
import com.kirox.app.settings.Playlist
import com.kirox.app.settings.PlaylistItem
import com.kirox.app.settings.Playlists
import com.kirox.app.telegram.TelegramAudio
import com.kirox.app.ui.chat.MusicPlayerUi
import com.kirox.app.ui.chat.Track
import com.kirox.app.ui.chat.VoicePlayer
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Whether the music library is open; settings and the player open it. */
object MusicLibraryUi {
    private val _open = MutableStateFlow(false)
    val isOpen = _open.asStateFlow()
    fun open() { _open.value = true }
    fun close() { _open.value = false }
}

fun TelegramAudio.toTrack() = Track(file, title, performer.ifBlank { chatTitle.ifBlank { "kiroX" } }, duration, chatId, messageId)
fun TelegramAudio.toItem() = PlaylistItem(chatId, messageId, title, performer, duration)
fun Track.toItem() = PlaylistItem(chatId, messageId, title, artist, durationSec)

private fun time(seconds: Int) = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)

private enum class Tab(val title: String) { All("Вся музыка"), Playlists("Плейлисты"), Chats("По чатам") }

/** Shows the library over everything while it is open. [onOpenChat] goes to the chat a track is from. */
@Composable
fun MusicLibraryHost(onOpenChat: (Long) -> Unit) {
    val open by MusicLibraryUi.isOpen.collectAsState()
    if (open) Dialog(onDismissRequest = MusicLibraryUi::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MusicLibraryScreen(onOpenChat = { MusicLibraryUi.close(); onOpenChat(it) })
    }
}

/**
 * «Музыка»: all music sent in your chats (with search), your playlists with «Любимое», and music by
 * chat. A tap plays the list from that track; ⋮ adds to a playlist, likes, or opens the chat.
 */
@Composable
private fun MusicLibraryScreen(onOpenChat: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val music = remember { (context.applicationContext as KiroXApplication).telegram.music }
    var tab by remember { mutableStateOf(Tab.All) }
    var playlistOpen by remember { mutableStateOf<Long?>(null) }
    var chatOpen by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(notice) { if (notice != null) { kotlinx.coroutines.delay(2500); notice = null } }
    // All music: pages of the server search, or the newest chats when it gives nothing.
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<TelegramAudio>>(emptyList()) }
    var next by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var loadedOnce by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun loadMore(reset: Boolean) {
        if (loading) return
        loading = true
        scope.launch {
            val q = query.trim()
            val result = runCatching { music.all(q, if (reset) "" else next) }
            error = result.exceptionOrNull()?.message
            val page = result.getOrNull()
            var list = page?.first.orEmpty()
            if (reset && list.isEmpty() && q.isEmpty()) list = runCatching { music.fromRecentChats() }.getOrDefault(emptyList())
            tracks = if (reset) list else (tracks + list).distinctBy { it.chatId to it.messageId }
            next = if (page == null || list.isEmpty()) "" else page.second
            loading = false
            loadedOnce = true
        }
    }
    LaunchedEffect(query) { kotlinx.coroutines.delay(if (query.isEmpty()) 0 else 450); loadMore(reset = true) }

    BackHandler {
        when {
            playlistOpen != null -> playlistOpen = null
            chatOpen != null -> chatOpen = null
            else -> MusicLibraryUi.close()
        }
    }
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(Fusion.colors.backdrop ?: listOf(Fusion.colors.background, Fusion.colors.background)))) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            val playlist = playlistOpen?.let { id -> Playlists.all.collectAsState().value.firstOrNull { it.id == id } }
            val chat = chatOpen
            when {
                playlist != null -> PlaylistScreen(playlist, onBack = { playlistOpen = null }, onNotice = { notice = it }, onOpenChat = onOpenChat)
                chat != null -> ChatMusicScreen(chat.first, chat.second, onBack = { chatOpen = null }, onNotice = { notice = it }, onOpenChat = onOpenChat)
                else -> {
                    Header("Музыка", onBack = MusicLibraryUi::close) {
                        val current by VoicePlayer.track.collectAsState()
                        if (current != null) Text("Плеер", color = Fusion.colors.accent, fontSize = 14.sp,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { MusicPlayerUi.open() }.padding(8.dp))
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tab.entries.forEach { t -> Pill(t.title, t == tab) { tab = t } }
                    }
                    when (tab) {
                        Tab.All -> {
                            SearchField(query, "Поиск музыки во всех чатах") { query = it }
                            PlayRow(tracks.size, onPlay = { play(context, tracks.map { it.toTrack() }, 0) }, onShuffle = { play(context, tracks.map { it.toTrack() }, -1) })
                            TrackList(tracks, loading = loading, emptyText = if (!loadedOnce) null else error?.let { "Не удалось загрузить музыку: $it" } ?: "Музыки не найдено", onEnd = { if (next.isNotEmpty()) loadMore(reset = false) },
                                onPlay = { i -> play(context, tracks.map { it.toTrack() }, i) }, onNotice = { notice = it }, onOpenChat = onOpenChat)
                        }
                        Tab.Playlists -> PlaylistsTab(onOpen = { playlistOpen = it }, onNotice = { notice = it })
                        Tab.Chats -> {
                            val chats = remember(tracks) { tracks.groupBy { it.chatId }.map { (id, list) -> Triple(id, list.first().chatTitle, list.size) }.sortedByDescending { it.third } }
                            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                                if (chats.isEmpty()) item { Text(if (loading) "Ищем музыку…" else "Музыки в чатах пока нет", color = Fusion.colors.textSecondary, modifier = Modifier.padding(20.dp)) }
                                itemsIndexed(chats) { _, (id, title, count) ->
                                    Row(Modifier.fillMaxWidth().clickable { chatOpen = id to title }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(44.dp).clip(CircleShape).background(Fusion.colors.surface2), contentAlignment = Alignment.Center) {
                                            Text(title.take(1).uppercase(), color = Fusion.colors.textPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(title.ifBlank { "Чат" }, color = Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Треков найдено: $count", color = Fusion.colors.textTertiary, fontSize = 12.sp)
                                        }
                                        KiroXIcon(KiroXIcon.Chevron, null, Modifier.size(18.dp), Fusion.colors.textTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        notice?.let {
            Text(it, color = Fusion.colors.textPrimary, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).clip(RoundedCornerShape(14.dp)).background(Fusion.colors.surface2).padding(horizontal = 16.dp, vertical = 10.dp))
        }
    }
}

/** Plays [tracks] from [index]; -1 shuffles. */
private fun play(context: android.content.Context, tracks: List<Track>, index: Int) {
    if (tracks.isEmpty()) return
    if (index < 0) {
        if (!VoicePlayer.shuffle.value) VoicePlayer.toggleShuffle()
        VoicePlayer.playQueue(context, tracks, tracks.indices.random())
    } else VoicePlayer.playQueue(context, tracks, index)
}

@Composable
private fun Header(title: String, subtitle: String? = null, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            KiroXIcon(KiroXIcon.Back, "Назад", Modifier.size(22.dp), Fusion.colors.textPrimary)
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(title, color = Fusion.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = Fusion.colors.textTertiary, fontSize = 12.sp)
        }
        actions()
    }
}

@Composable
private fun Pill(label: String, on: Boolean, onClick: () -> Unit) {
    Text(label, color = if (on) Fusion.colors.onAccent else Fusion.colors.textSecondary, fontSize = 13.sp,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) Fusion.colors.accent else Fusion.colors.surface1).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp))
}

@Composable
private fun SearchField(value: String, hint: String, onChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(14.dp)).background(Fusion.colors.surface1).padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (value.isEmpty()) Text(hint, color = Fusion.colors.textTertiary, fontSize = 15.sp)
        BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(color = Fusion.colors.textPrimary, fontSize = 15.sp), cursorBrush = SolidColor(Fusion.colors.accent), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PlayRow(count: Int, onPlay: () -> Unit, onShuffle: () -> Unit) {
    if (count == 0) return
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("▶  Слушать", color = Fusion.colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Fusion.colors.accent).clickable(onClick = onPlay).padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("⇄  Перемешать", color = Fusion.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Fusion.colors.surface1).clickable(onClick = onShuffle).padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Tracks from chats; reaching the end asks for more. */
@Composable
private fun TrackList(
    tracks: List<TelegramAudio>, loading: Boolean, emptyText: String?, onEnd: () -> Unit,
    onPlay: (Int) -> Unit, onNotice: (String) -> Unit, onOpenChat: (Long) -> Unit
) {
    val state = rememberLazyListState()
    val atEnd by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= state.layoutInfo.totalItemsCount - 5 } == true } }
    LaunchedEffect(atEnd, tracks.size) { if (atEnd && tracks.isNotEmpty()) onEnd() }
    LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(bottom = 24.dp)) {
        itemsIndexed(tracks, key = { _, t -> "${t.chatId}:${t.messageId}" }) { i, t ->
            TrackRow(t.toItem(), subtitle = listOf(t.performer, t.chatTitle).filter { it.isNotBlank() }.joinToString(" · "), fileId = t.file.id, onClick = { onPlay(i) }) { close ->
                TrackMenuItems(t.toItem(), onNotice, onOpenChat, close)
            }
        }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), color = Fusion.colors.accent) } }
        else if (tracks.isEmpty() && emptyText != null) item { Text(emptyText, color = Fusion.colors.textSecondary, modifier = Modifier.padding(20.dp)) }
    }
}

/** One track: title, details, length, ⋮ with [menu]. The one playing now is highlighted. */
@Composable
private fun TrackRow(item: PlaylistItem, subtitle: String, fileId: Int?, onClick: () -> Unit, menu: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    val current by VoicePlayer.track.collectAsState()
    val here = current?.let { (fileId != null && it.file.id == fileId) || (it.chatId == item.chatId && it.messageId == item.messageId && it.chatId != 0L) } == true
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (here) Fusion.colors.accent else Fusion.colors.surface2), contentAlignment = Alignment.Center) {
            KiroXIcon(KiroXIcon.Music, null, Modifier.size(20.dp), if (here) Fusion.colors.onAccent else Fusion.colors.textSecondary)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.title, color = if (here) Fusion.colors.accent else Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle.ifBlank { "kiroX" }, color = Fusion.colors.textTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(time(item.duration), color = Fusion.colors.textTertiary, fontSize = 12.sp)
        Box {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { open = true }, contentAlignment = Alignment.Center) {
                KiroXIcon(KiroXIcon.More, "Ещё", Modifier.size(20.dp), Fusion.colors.textSecondary)
            }
            DropdownMenu(open, onDismissRequest = { open = false }, containerColor = Fusion.colors.surface1) { menu { open = false } }
        }
    }
}

@Composable
private fun TrackMenuItems(item: PlaylistItem, onNotice: (String) -> Unit, onOpenChat: (Long) -> Unit, close: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    val liked = Playlists.all.collectAsState().value.firstOrNull { it.id == Playlists.LIKED }?.has(item.chatId, item.messageId) == true
    DropdownMenuItem(text = { Text(if (liked) "Убрать из «Любимое»" else "В «Любимое» ♥", color = Fusion.colors.textPrimary) }, onClick = {
        Playlists.toggleLiked(item); onNotice(if (liked) "Убрано из «Любимое»" else "Добавлено в «Любимое»"); close()
    })
    DropdownMenuItem(text = { Text("В плейлист…", color = Fusion.colors.textPrimary) }, onClick = { adding = true })
    DropdownMenuItem(text = { Text("Открыть чат", color = Fusion.colors.textPrimary) }, onClick = { close(); onOpenChat(item.chatId) })
    if (adding) AddToPlaylistDialog(item, onDone = { message -> adding = false; close(); message?.let(onNotice) })
}

/** «В плейлист»: pick a playlist or make a new one. [onDone] gets the notice to show, or null when cancelled. */
@Composable
fun AddToPlaylistDialog(item: PlaylistItem, onDone: (String?) -> Unit) {
    val playlists by Playlists.all.collectAsState()
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        containerColor = Fusion.colors.surface1,
        title = { Text("В плейлист", color = Fusion.colors.textPrimary) },
        text = {
            Column {
                playlists.forEach { p ->
                    val has = p.has(item.chatId, item.messageId)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = !has) {
                        Playlists.add(p.id, item); onDone("Добавлено в «${p.name}»")
                    }.padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, color = if (has) Fusion.colors.textTertiary else Fusion.colors.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(if (has) "уже есть" else "${p.items.size}", color = Fusion.colors.textTertiary, fontSize = 12.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Fusion.colors.surface2).padding(10.dp)) {
                        if (name.isEmpty()) Text("Новый плейлист", color = Fusion.colors.textTertiary, fontSize = 15.sp)
                        BasicTextField(name, { name = it.take(40) }, singleLine = true, textStyle = TextStyle(color = Fusion.colors.textPrimary, fontSize = 15.sp), cursorBrush = SolidColor(Fusion.colors.accent), modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(enabled = name.isNotBlank(), onClick = {
                        val id = Playlists.create(name); Playlists.add(id, item); onDone("Создан плейлист «${name.trim()}»")
                    }) { Text("Создать", color = Fusion.colors.accent) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(null) }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

@Composable
private fun PlaylistsTab(onOpen: (Long) -> Unit, onNotice: (String) -> Unit) {
    val playlists by Playlists.all.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().clickable { creating = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Fusion.colors.surface2), contentAlignment = Alignment.Center) {
                    Text("+", color = Fusion.colors.accent, fontSize = 26.sp)
                }
                Text("Новый плейлист", color = Fusion.colors.accent, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
        itemsIndexed(playlists, key = { _, p -> p.id }) { _, p ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(p.id) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(if (p.id == Playlists.LIKED) Fusion.colors.accent else Fusion.colors.surface2), contentAlignment = Alignment.Center) {
                    if (p.id == Playlists.LIKED) Text("♥", color = Fusion.colors.onAccent, fontSize = 22.sp)
                    else KiroXIcon(KiroXIcon.Music, null, Modifier.size(22.dp), Fusion.colors.textSecondary)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(p.name, color = Fusion.colors.textPrimary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Треков: ${p.items.size} · ${time(p.totalSeconds)}", color = Fusion.colors.textTertiary, fontSize = 12.sp)
                }
                KiroXIcon(KiroXIcon.Chevron, null, Modifier.size(18.dp), Fusion.colors.textTertiary)
            }
        }
        item {
            Text("Плейлисты хранятся на этом телефоне и попадают в резервную копию настроек. Сама музыка остаётся в чатах Telegram.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(20.dp))
        }
    }
    if (creating) NameDialog("Новый плейлист", name, onName = { name = it }, onDismiss = { creating = false; name = "" }) {
        val id = Playlists.create(name); creating = false; name = ""; onNotice("Плейлист создан"); onOpen(id)
    }
}

@Composable
private fun NameDialog(title: String, value: String, onName: (String) -> Unit, onDismiss: () -> Unit, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text(title, color = Fusion.colors.textPrimary) },
        text = {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Fusion.colors.surface2).padding(12.dp)) {
                if (value.isEmpty()) Text("Название", color = Fusion.colors.textTertiary, fontSize = 16.sp)
                BasicTextField(value, { onName(it.take(40)) }, singleLine = true, textStyle = TextStyle(color = Fusion.colors.textPrimary, fontSize = 16.sp), cursorBrush = SolidColor(Fusion.colors.accent), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = value.isNotBlank(), onClick = onDone) { Text("Готово", color = Fusion.colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

/** A playlist: play / shuffle, its tracks with move up / down and remove; rename or delete it. */
@Composable
private fun PlaylistScreen(playlist: Playlist, onBack: () -> Unit, onNotice: (String) -> Unit, onOpenChat: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val music = remember { (context.applicationContext as KiroXApplication).telegram.music }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(playlist.name) }
    var deleting by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    // Tracks point at messages: find each again, skipping the deleted ones.
    fun start(index: Int) {
        if (starting) return
        starting = true
        scope.launch {
            val found = playlist.items.map { i -> async { music.get(i.chatId, i.messageId) } }.awaitAll()
            starting = false
            val tracks = found.filterNotNull().map { it.toTrack() }
            val missing = found.count { it == null }
            if (missing > 0) onNotice("Недоступно треков: $missing — сообщения удалены")
            if (tracks.isEmpty()) return@launch
            val chosen = playlist.items.getOrNull(index)
            val at = if (index < 0) -1 else tracks.indexOfFirst { it.chatId == chosen?.chatId && it.messageId == chosen.messageId }.coerceAtLeast(0)
            play(context, tracks, at)
        }
    }
    Header(playlist.name, "Треков: ${playlist.items.size} · ${time(playlist.totalSeconds)}", onBack) {
        if (playlist.id != Playlists.LIKED) Box {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { menu = true }, contentAlignment = Alignment.Center) {
                KiroXIcon(KiroXIcon.More, "Ещё", Modifier.size(22.dp), Fusion.colors.textPrimary)
            }
            DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = Fusion.colors.surface1) {
                DropdownMenuItem(text = { Text("Переименовать", color = Fusion.colors.textPrimary) }, onClick = { menu = false; renaming = true })
                DropdownMenuItem(text = { Text("Удалить плейлист", color = Fusion.colors.danger) }, onClick = { menu = false; deleting = true })
            }
        }
    }
    if (starting) Box(Modifier.fillMaxWidth().padding(4.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), color = Fusion.colors.accent, strokeWidth = 2.dp) }
    PlayRow(playlist.items.size, onPlay = { start(0) }, onShuffle = { start(-1) })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (playlist.items.isEmpty()) item {
            Text("Пока пусто. Добавляйте треки через ⋮ в «Вся музыка», долгим нажатием на музыку в чате или кнопкой в плеере.",
                color = Fusion.colors.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(20.dp))
        }
        itemsIndexed(playlist.items, key = { _, i -> "${i.chatId}:${i.messageId}" }) { index, item ->
            TrackRow(item, subtitle = item.artist, fileId = null, onClick = { start(index) }) { close ->
                if (index > 0) DropdownMenuItem(text = { Text("Выше", color = Fusion.colors.textPrimary) }, onClick = { Playlists.move(playlist.id, index, -1); close() })
                if (index < playlist.items.lastIndex) DropdownMenuItem(text = { Text("Ниже", color = Fusion.colors.textPrimary) }, onClick = { Playlists.move(playlist.id, index, 1); close() })
                DropdownMenuItem(text = { Text("Открыть чат", color = Fusion.colors.textPrimary) }, onClick = { close(); onOpenChat(item.chatId) })
                DropdownMenuItem(text = { Text("Убрать из плейлиста", color = Fusion.colors.danger) }, onClick = { Playlists.remove(playlist.id, item.chatId, item.messageId); close() })
            }
        }
    }
    if (renaming) NameDialog("Переименовать", name, onName = { name = it }, onDismiss = { renaming = false }) { Playlists.rename(playlist.id, name); renaming = false }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Удалить «${playlist.name}»?", color = Fusion.colors.textPrimary) },
        text = { Text("Удалится только список — музыка останется в чатах.", color = Fusion.colors.textSecondary) },
        confirmButton = { TextButton(onClick = { deleting = false; Playlists.delete(playlist.id); onBack() }) { Text("Удалить", color = Fusion.colors.danger) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Отмена", color = Fusion.colors.accent) } }
    )
}

/** One chat's music, newest first, loading more on scroll. */
@Composable
private fun ChatMusicScreen(chatId: Long, title: String, onBack: () -> Unit, onNotice: (String) -> Unit, onOpenChat: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val music = remember { (context.applicationContext as KiroXApplication).telegram.music }
    var tracks by remember { mutableStateOf<List<TelegramAudio>>(emptyList()) }
    var from by remember { mutableLongStateOf(0L) }
    var loading by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    fun more() {
        if (loading || done) return
        loading = true
        scope.launch {
            val page = runCatching { music.chat(chatId, from) }.getOrNull()
            tracks = (tracks + page?.first.orEmpty()).distinctBy { it.messageId }
            from = page?.second ?: 0
            done = page == null || from == 0L || page.first.isEmpty()
            loading = false
        }
    }
    LaunchedEffect(chatId) { more() }
    Header(title.ifBlank { "Чат" }, "Музыка чата", onBack)
    PlayRow(tracks.size, onPlay = { play(context, tracks.map { it.toTrack() }, 0) }, onShuffle = { play(context, tracks.map { it.toTrack() }, -1) })
    TrackList(tracks, loading, if (done) "Музыки нет" else null, onEnd = ::more, onPlay = { i -> play(context, tracks.map { it.toTrack() }, i) }, onNotice = onNotice, onOpenChat = onOpenChat)
}
