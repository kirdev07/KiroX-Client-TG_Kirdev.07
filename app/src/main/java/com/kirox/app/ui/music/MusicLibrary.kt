package com.kirox.app.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.kirox.app.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirox.app.KiroXApplication
import com.kirox.app.settings.PlaylistItem
import com.kirox.app.settings.Playlists
import com.kirox.app.telegram.TelegramAudio
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.ui.chat.Track
import com.kirox.app.ui.chat.VoicePlayer
import com.kirox.app.ui.components.FusionToast
import com.kirox.app.ui.components.rememberTelegramBitmap
import com.kirox.app.ui.components.rememberToast
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

fun TelegramAudio.toTrack() = Track(file, title, performer.ifBlank { chatTitle.ifBlank { "kiroX" } }, duration, chatId, messageId, cover)
fun TelegramAudio.toItem() = PlaylistItem(chatId, messageId, title, performer, duration)
fun Track.toItem() = PlaylistItem(chatId, messageId, title, artist, durationSec)

private fun time(seconds: Int) = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)

// ---- The music look: dark, covers first, the theme's accent ----

/** Colors of the music screens: a near-black backdrop with soft greys, as in music apps. */
internal object MusicLook {
    val backdrop = Brush.verticalGradient(listOf(Color(0xFF16171E), Color(0xFF0B0B0F)))
    val surface = Color(0xFF1E1F27)
    val surfaceHigh = Color(0xFF2A2B34)
    val text = Color.White
    val muted = Color(0xFF9A9BA6)
}

/** A track or collection cover: Telegram's thumbnail, else a gradient with a note. */
@Composable
internal fun TrackArt(cover: TelegramFileRef?, size: Dp, corner: Dp, modifier: Modifier = Modifier, file: TelegramFileRef? = null, iconLift: Dp = 0.dp) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val image = rememberAudioCover(cover, file, px)
    Box(
        modifier.size(size).clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(Fusion.colors.accent.copy(alpha = .75f), Color(0xFF2B1D4A)))),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else KiroXIcon(KiroXIcon.Music, null, Modifier.offset(y = -iconLift).size(size * .4f), Color.White.copy(alpha = .9f))
    }
}

@Composable
private fun RoundIcon(icon: KiroXIcon, description: String, size: Dp = 40.dp, background: Color = Color.Transparent, tint: Color = MusicLook.text, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(background).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        KiroXIcon(icon, description, Modifier.size(size * .55f), tint)
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, color = if (selected) Color.White else MusicLook.muted, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.clip(CircleShape).background(if (selected) Fusion.colors.accent else MusicLook.surface).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = MusicLook.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = MusicLook.muted, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(8.dp))
    }
}

/** A big card for «Для вас»: cover, title, line under it and a white play button. */
@Composable
private fun MixCard(title: String, subtitle: String, cover: TelegramFileRef?, onOpen: () -> Unit, file: TelegramFileRef? = null, onPlay: () -> Unit) {
    Box(Modifier.width(158.dp).height(196.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onOpen)) {
        TrackArt(cover, 196.dp, 0.dp, Modifier.fillMaxSize(), file = file, iconLift = 34.dp)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .75f)), startY = 200f)))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 48.dp, bottom = 12.dp)) {
            androidx.compose.material3.Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            androidx.compose.material3.Text(subtitle, color = Color.White.copy(alpha = .75f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(34.dp).clip(CircleShape).background(Color.White).clickable(onClick = onPlay), contentAlignment = Alignment.Center) {
            KiroXIcon(KiroXIcon.Play, "Играть", Modifier.size(20.dp), Color(0xFF111111))
        }
    }
}

/** A track line: cover, title, artist, ⋯; the playing one shows an equalizer over its cover. */
@Composable
private fun TrackLine(title: String, artist: String, cover: TelegramFileRef?, fileId: Int?, chatId: Long, messageId: Long, onClick: () -> Unit, file: TelegramFileRef? = null, menu: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    val current by VoicePlayer.track.collectAsState()
    val here = current?.let { (fileId != null && it.file.id == fileId) || (chatId != 0L && it.chatId == chatId && it.messageId == messageId) } == true
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 8.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            TrackArt(cover, 48.dp, 10.dp, file = file)
            if (here) Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .5f)), contentAlignment = Alignment.Center) {
                KiroXIcon(KiroXIcon.Equalizer, null, Modifier.size(22.dp), Fusion.colors.accent)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            androidx.compose.material3.Text(title, color = if (here) Fusion.colors.accent else MusicLook.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            androidx.compose.material3.Text(artist.ifBlank { "kiroX" }, color = MusicLook.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            RoundIcon(KiroXIcon.MoreHoriz, "Ещё", tint = MusicLook.muted) { open = true }
            DropdownMenu(open, onDismissRequest = { open = false }, containerColor = MusicLook.surface) { menu { open = false } }
        }
    }
}

/** Shows the library over everything while it is open. [onOpenChat] goes to the chat a track is from. */
@Composable
fun MusicLibraryHost(onOpenChat: (Long) -> Unit) {
    val open by MusicLibraryUi.isOpen.collectAsState()
    if (open) Dialog(onDismissRequest = MusicLibraryUi::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        MusicLibraryScreen(onOpenChat = { MusicLibraryUi.close(); onOpenChat(it) })
    }
}

private enum class Tab(val title: String) { All("Все"), Playlists("Плейлисты"), Chats("Чаты"), Liked("Любимое") }
private enum class Sort(val title: String) { Newest("Сначала новые"), Title("По названию"), Artist("По исполнителю") }

/** What a collection page shows: a playlist, a chat's music or «Любимое». */
private sealed interface Page {
    data class OfPlaylist(val id: Long) : Page
    data class OfChat(val chatId: Long, val title: String) : Page
}

/** Plays [tracks] from [index] (-1 shuffles); [source] is shown as «Играет из». */
private fun play(context: android.content.Context, tracks: List<Track>, index: Int, source: String) {
    if (tracks.isEmpty()) return
    if (index < 0) {
        if (!VoicePlayer.shuffle.value) VoicePlayer.toggleShuffle()
        VoicePlayer.playQueue(context, tracks, tracks.indices.random(), source)
    } else VoicePlayer.playQueue(context, tracks, index, source)
}

private fun greeting(): String = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Доброе утро"
    in 12..17 -> "Добрый день"
    in 18..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

/**
 * «Музыка»: a greeting, search over all chats' music, chips (Все · Плейлисты · Чаты · Любимое),
 * «Для вас» cards of playlists and chats with music, and every track. The playing track's bar is
 * at the bottom.
 */
@Composable
private fun MusicLibraryScreen(onOpenChat: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val music = remember { (context.applicationContext as KiroXApplication).telegram.music }
    val playlists by Playlists.all.collectAsState()
    var tab by remember { mutableStateOf(Tab.All) }
    var page by remember { mutableStateOf<Page?>(null) }
    var sort by remember { mutableStateOf(Sort.Newest) }
    var sortMenu by remember { mutableStateOf(false) }
    val toast = rememberToast()
    var notice by toast
    // All music: pages of the server search, or the newest chats when it gives nothing.
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<TelegramAudio>>(emptyList()) }
    var next by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var loadedOnce by remember { mutableStateOf(false) }
    fun loadMore(reset: Boolean) {
        if (loading) return
        loading = true
        scope.launch {
            val q = query.trim()
            val page1 = runCatching { music.all(q, if (reset) "" else next) }.getOrNull()
            var list = page1?.first.orEmpty()
            if (reset && list.isEmpty() && q.isEmpty()) list = runCatching { music.fromRecentChats() }.getOrDefault(emptyList())
            tracks = if (reset) list else (tracks + list).distinctBy { it.chatId to it.messageId }
            next = if (page1 == null || list.isEmpty()) "" else page1.second
            loading = false
            loadedOnce = true
        }
    }
    LaunchedEffect(query) { kotlinx.coroutines.delay(if (query.isEmpty()) 0 else 450); loadMore(reset = true) }
    val sorted = remember(tracks, sort) {
        when (sort) {
            Sort.Newest -> tracks
            Sort.Title -> tracks.sortedBy { it.title.lowercase() }
            Sort.Artist -> tracks.sortedBy { it.performer.lowercase() }
        }
    }
    val chats = remember(tracks) { tracks.groupBy { it.chatId }.map { (_, list) -> list }.sortedByDescending { it.size } }

    BackHandler { if (page != null) page = null else MusicLibraryUi.close() }
    Box(Modifier.fillMaxSize().background(MusicLook.backdrop)) {
        when (val p = page) {
            is Page.OfPlaylist -> PlaylistPage(p.id, onBack = { page = null }, onNotice = { notice = it }, onOpenChat = onOpenChat)
            is Page.OfChat -> ChatPage(p.chatId, p.title, onBack = { page = null }, onNotice = { notice = it }, onOpenChat = onOpenChat)
            null -> {
                val state = rememberLazyListState()
                val atEnd by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= state.layoutInfo.totalItemsCount - 5 } == true } }
                LaunchedEffect(atEnd, tracks.size) { if (atEnd && tracks.isNotEmpty() && next.isNotEmpty() && tab == Tab.All) loadMore(reset = false) }
                LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(bottom = 110.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(greeting(), color = MusicLook.muted, fontSize = 14.sp)
                                Text("Музыка\nиз ваших чатов", color = MusicLook.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            RoundIcon(KiroXIcon.Down, "Закрыть", size = 44.dp, background = MusicLook.surface) { MusicLibraryUi.close() }
                        }
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).clip(CircleShape).background(MusicLook.surface).padding(start = 16.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KiroXIcon(KiroXIcon.Search, null, Modifier.size(20.dp), MusicLook.muted)
                            Box(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 14.dp)) {
                                if (query.isEmpty()) Text("Поиск треков и исполнителей", color = MusicLook.muted, fontSize = 14.sp)
                                BasicTextField(query, { query = it }, singleLine = true, textStyle = TextStyle(color = MusicLook.text, fontSize = 14.sp), cursorBrush = SolidColor(Fusion.colors.accent), modifier = Modifier.fillMaxWidth())
                            }
                            Box {
                                RoundIcon(KiroXIcon.Tune, "Сортировка", tint = if (sort != Sort.Newest) Fusion.colors.accent else MusicLook.muted) { sortMenu = true }
                                DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }, containerColor = MusicLook.surface) {
                                    Sort.entries.forEach { s ->
                                        DropdownMenuItem(text = { Text(s.title, color = if (s == sort) Fusion.colors.accent else MusicLook.text) }, onClick = { sort = s; sortMenu = false })
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tab.entries.forEach { t ->
                                Chip(t.title, t == tab) { if (t == Tab.Liked) page = Page.OfPlaylist(Playlists.LIKED) else tab = t }
                            }
                        }
                    }
                    when {
                        query.isNotBlank() || tab == Tab.All -> {
                            if (query.isBlank()) {
                                val cards = buildList<@Composable () -> Unit> {
                                    playlists.filter { it.items.isNotEmpty() }.forEach { pl ->
                                        add {
                                            MixCard(pl.name, "Треков: ${pl.items.size}", null, onOpen = { page = Page.OfPlaylist(pl.id) }) {
                                                scope.launch { val list = pl.items.map { i -> async { music.get(i.chatId, i.messageId) } }.awaitAll().filterNotNull(); play(context, list.map { it.toTrack() }, 0, pl.name) }
                                            }
                                        }
                                    }
                                    chats.take(8).forEach { list ->
                                        val first = list.first()
                                        add {
                                            MixCard(first.chatTitle.ifBlank { "Чат" }, "Треков: ${list.size}", list.firstNotNullOfOrNull { it.cover }, onOpen = { page = Page.OfChat(first.chatId, first.chatTitle) }, file = first.file) {
                                                play(context, list.map { it.toTrack() }, 0, first.chatTitle)
                                            }
                                        }
                                    }
                                }
                                if (cards.isNotEmpty()) {
                                    item { SectionTitle("Для вас") }
                                    item {
                                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(cards.size) { cards[it]() } }
                                    }
                                }
                            }
                            item { SectionTitle(if (query.isBlank()) "Все треки" else "Найдено", if (sorted.isNotEmpty()) "Перемешать" else null) { play(context, sorted.map { it.toTrack() }, -1, "Вся музыка") } }
                            itemsIndexed(sorted, key = { _, t -> "${t.chatId}:${t.messageId}" }) { i, t ->
                                TrackLine(t.title, listOf(t.performer, t.chatTitle).filter { it.isNotBlank() }.joinToString(" · "), t.cover, t.file.id, t.chatId, t.messageId,
                                    onClick = { play(context, sorted.map { it.toTrack() }, i, if (query.isBlank()) "Вся музыка" else "Поиск") }, file = t.file) { close ->
                                    TrackMenuItems(t.toItem(), { notice = it }, onOpenChat, close, file = t.file)
                                }
                            }
                            if (loading) item { Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), color = Fusion.colors.accent) } }
                            else if (sorted.isEmpty() && loadedOnce) item { Text("Музыки не найдено. Отправьте или перешлите себе трек — он появится здесь.", color = MusicLook.muted, fontSize = 14.sp, modifier = Modifier.padding(20.dp)) }
                        }
                        tab == Tab.Playlists -> playlistsSection(this, playlists, onOpen = { page = Page.OfPlaylist(it) }, onNotice = { notice = it })
                        else -> {
                            item { SectionTitle("Чаты с музыкой") }
                            if (chats.isEmpty()) item { Text(if (loading) "Ищем музыку…" else "Музыки в чатах пока нет", color = MusicLook.muted, modifier = Modifier.padding(20.dp)) }
                            items(chats, key = { it.first().chatId }) { list ->
                                val first = list.first()
                                Row(Modifier.fillMaxWidth().clickable { page = Page.OfChat(first.chatId, first.chatTitle) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TrackArt(list.firstNotNullOfOrNull { it.cover }, 56.dp, 12.dp, file = first.file)
                                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                        androidx.compose.material3.Text(first.chatTitle.ifBlank { "Чат" }, color = MusicLook.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Треков: ${list.size}", color = MusicLook.muted, fontSize = 13.sp)
                                    }
                                    KiroXIcon(KiroXIcon.Chevron, null, Modifier.size(20.dp), MusicLook.muted)
                                }
                            }
                        }
                    }
                }
            }
        }
        // The playing track, always at the bottom.
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) { com.kirox.app.ui.chat.MiniPlayerBar(floating = true) }
        FusionToast(toast)
    }
}

/** The playlists tab: new, load a friend's, and every playlist with its count. */
private fun playlistsSection(scope: androidx.compose.foundation.lazy.LazyListScope, playlists: List<com.kirox.app.settings.Playlist>, onOpen: (Long) -> Unit, onNotice: (String) -> Unit) = with(scope) {
    item { NewPlaylistRows(onOpen, onNotice) }
    items(playlists, key = { it.id }) { p ->
        Row(Modifier.fillMaxWidth().clickable { onOpen(p.id) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(if (p.id == Playlists.LIKED) Brush.linearGradient(listOf(Fusion.colors.accent, Color(0xFF2B1D4A))) else Brush.linearGradient(listOf(MusicLook.surfaceHigh, MusicLook.surface))), contentAlignment = Alignment.Center) {
                KiroXIcon(if (p.id == Playlists.LIKED) KiroXIcon.Heart else KiroXIcon.Queue, null, Modifier.size(24.dp), Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                androidx.compose.material3.Text(com.kirox.app.i18n.I18n.t(p.name), color = MusicLook.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Треков: ${p.items.size} · ${time(p.totalSeconds)}", color = MusicLook.muted, fontSize = 13.sp)
            }
            KiroXIcon(KiroXIcon.Chevron, null, Modifier.size(20.dp), MusicLook.muted)
        }
    }
    item {
        Text("Плейлисты хранятся на этом телефоне и попадают в резервную копию настроек. Сама музыка остаётся в чатах Telegram.",
            color = MusicLook.muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun NewPlaylistRows(onOpen: (Long) -> Unit, onNotice: (String) -> Unit) {
    val context = LocalContext.current
    val pickFile = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching { context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText().take(2_000_000) } }.getOrNull()
        val id = text?.let(Playlists::import)
        if (id == null) onNotice("Это не плейлист kiroX") else { onNotice("Плейлист загружен"); onOpen(id) }
    }
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.weight(1f).clip(CircleShape).background(Fusion.colors.accent).clickable { creating = true }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            KiroXIcon(KiroXIcon.Add, null, Modifier.size(20.dp), Color.White)
            Text(" Новый", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.weight(1f).clip(CircleShape).background(MusicLook.surface).clickable { runCatching { pickFile.launch("*/*") } }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            KiroXIcon(KiroXIcon.Download, null, Modifier.size(20.dp), MusicLook.text)
            Text(" Загрузить", color = MusicLook.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    if (creating) NameDialog("Новый плейлист", name, onName = { name = it }, onDismiss = { creating = false; name = "" }) {
        val id = Playlists.create(name); creating = false; name = ""; onNotice("Плейлист создан"); onOpen(id)
    }
}

/**
 * A collection page as in music apps: a big cover with the title over it, «Перемешать», download
 * everything, then the tracks. [tracks] null while loading.
 */
@Composable
private fun CollectionPage(
    title: String, subtitle: String, tracks: List<TelegramAudio>?, source: String, onBack: () -> Unit,
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)?,
    rowMenu: @Composable ColumnScope.(index: Int, track: TelegramAudio, close: () -> Unit) -> Unit,
    empty: String,
    onEnd: () -> Unit = {}
) {
    val context = LocalContext.current
    val files = com.kirox.app.ui.components.LocalTelegramFiles.current
    var open by remember { mutableStateOf(false) }
    val list = tracks.orEmpty()
    val state = rememberLazyListState()
    val atEnd by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= state.layoutInfo.totalItemsCount - 4 } == true } }
    LaunchedEffect(atEnd, list.size) { if (atEnd) onEnd() }
    LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(bottom = 110.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(330.dp)) {
                val cover = list.firstNotNullOfOrNull { it.cover }
                BoxWithConstraints(Modifier.fillMaxSize()) { TrackArt(cover, maxWidth, 0.dp, Modifier.fillMaxSize(), file = list.firstOrNull()?.file) }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent, Color(0xFF14151B)))))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(KiroXIcon.Back, "Назад", size = 40.dp, background = Color.Black.copy(alpha = .35f), onClick = onBack)
                    Spacer(Modifier.weight(1f))
                    if (menu != null) Box {
                        RoundIcon(KiroXIcon.MoreHoriz, "Ещё", size = 40.dp, background = Color.Black.copy(alpha = .35f)) { open = true }
                        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = MusicLook.surface) { menu { open = false } }
                    }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 12.dp)) {
                    androidx.compose.material3.Text(com.kirox.app.i18n.I18n.t(title), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 36.sp)
                    Text(subtitle, color = Color.White.copy(alpha = .75f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clip(CircleShape).clickable(enabled = list.isNotEmpty()) { play(context, list.map { it.toTrack() }, -1, source) }, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Fusion.colors.accent), contentAlignment = Alignment.Center) {
                        KiroXIcon(KiroXIcon.Play, "Играть", Modifier.size(30.dp), Color.White)
                    }
                    Text("Перемешать", color = MusicLook.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp))
                }
                Spacer(Modifier.weight(1f))
                // Download everything for listening offline.
                Box(Modifier.size(44.dp).clip(CircleShape).border(1.5.dp, MusicLook.muted, CircleShape).clickable(enabled = list.isNotEmpty() && files != null) {
                    list.forEach { files?.download(it.file) }
                }, contentAlignment = Alignment.Center) {
                    KiroXIcon(KiroXIcon.Download, "Скачать всё", Modifier.size(22.dp), MusicLook.text)
                }
            }
        }
        if (tracks == null) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp), color = Fusion.colors.accent) } }
        else if (list.isEmpty()) item { Text(empty, color = MusicLook.muted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(20.dp)) }
        itemsIndexed(list, key = { _, t -> "${t.chatId}:${t.messageId}" }) { index, t ->
            TrackLine(t.title, t.performer.ifBlank { t.chatTitle }, t.cover, t.file.id, t.chatId, t.messageId, onClick = { play(context, list.map { it.toTrack() }, index, source) }, file = t.file) { close ->
                rowMenu(index, t, close)
            }
        }
    }
}

/** A playlist (or «Любимое»): its tracks found again in their chats, with share, rename, delete. */
@Composable
private fun PlaylistPage(id: Long, onBack: () -> Unit, onNotice: (String) -> Unit, onOpenChat: (Long) -> Unit) {
    val context = LocalContext.current
    val music = remember { (context.applicationContext as KiroXApplication).telegram.music }
    val playlists by Playlists.all.collectAsState()
    val playlist = playlists.firstOrNull { it.id == id } ?: run { LaunchedEffect(Unit) { onBack() }; return }
    var renaming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(playlist.name) }
    var deleting by remember { mutableStateOf(false) }
    // Tracks point at messages: find each again, keeping the order; deleted ones drop out.
    var resolved by remember { mutableStateOf<List<TelegramAudio>?>(null) }
    LaunchedEffect(playlist.items) {
        val found = kotlinx.coroutines.coroutineScope { playlist.items.map { i -> async { music.get(i.chatId, i.messageId) } }.awaitAll() }
        resolved = found.filterNotNull()
        val missing = found.count { it == null }
        if (missing > 0) onNotice("Недоступно треков: $missing — сообщения удалены")
    }
    CollectionPage(
        title = playlist.name, subtitle = "Треков: ${playlist.items.size} · ${time(playlist.totalSeconds)}", tracks = resolved, source = com.kirox.app.i18n.I18n.t(playlist.name), onBack = onBack,
        menu = { close ->
            if (playlist.items.isNotEmpty()) DropdownMenuItem(text = { Text("Поделиться плейлистом", color = MusicLook.text) }, onClick = { close(); sharePlaylist(context, playlist.id, playlist.name) })
            if (playlist.id != Playlists.LIKED) {
                DropdownMenuItem(text = { Text("Переименовать", color = MusicLook.text) }, onClick = { close(); renaming = true })
                DropdownMenuItem(text = { Text("Удалить плейлист", color = Fusion.colors.danger) }, onClick = { close(); deleting = true })
            }
        },
        rowMenu = { _, t, close ->
            val at = playlist.items.indexOfFirst { it.chatId == t.chatId && it.messageId == t.messageId }
            if (at > 0) DropdownMenuItem(text = { Text("Выше", color = MusicLook.text) }, onClick = { Playlists.move(playlist.id, at, -1); close() })
            if (at in 0 until playlist.items.lastIndex) DropdownMenuItem(text = { Text("Ниже", color = MusicLook.text) }, onClick = { Playlists.move(playlist.id, at, 1); close() })
            DownloadItems(t.file, onNotice, close)
            DropdownMenuItem(text = { Text("Открыть чат", color = MusicLook.text) }, onClick = { close(); onOpenChat(t.chatId) })
            DropdownMenuItem(text = { Text("Убрать из плейлиста", color = Fusion.colors.danger) }, onClick = { Playlists.remove(playlist.id, t.chatId, t.messageId); close() })
        },
        empty = "Пока пусто. Добавляйте треки через ⋯ в списке, долгим нажатием на музыку в чате или кнопкой в плеере."
    )
    if (renaming) NameDialog("Переименовать", name, onName = { name = it }, onDismiss = { renaming = false }) { Playlists.rename(playlist.id, name); renaming = false }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        containerColor = MusicLook.surface,
        title = { Text("Удалить «${playlist.name}»?", color = MusicLook.text) },
        text = { Text("Удалится только список — музыка останется в чатах.", color = MusicLook.muted) },
        confirmButton = { TextButton(onClick = { deleting = false; Playlists.delete(playlist.id); onBack() }) { Text("Удалить", color = Fusion.colors.danger) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Отмена", color = Fusion.colors.accent) } }
    )
}

/** One chat's music, newest first, loading more on scroll. */
@Composable
private fun ChatPage(chatId: Long, title: String, onBack: () -> Unit, onNotice: (String) -> Unit, onOpenChat: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val music = remember { (context.applicationContext as KiroXApplication).telegram.music }
    var tracks by remember { mutableStateOf<List<TelegramAudio>?>(null) }
    var from by remember { mutableLongStateOf(0L) }
    var loading by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    fun more() {
        if (loading || done) return
        loading = true
        scope.launch {
            val p = runCatching { music.chat(chatId, from) }.getOrNull()
            tracks = (tracks.orEmpty() + p?.first.orEmpty()).distinctBy { it.messageId }
            from = p?.second ?: 0
            done = p == null || from == 0L || p.first.isEmpty()
            loading = false
        }
    }
    LaunchedEffect(chatId) { more() }
    CollectionPage(
        title = title.ifBlank { "Чат" }, subtitle = "Музыка чата · треков: ${tracks?.size ?: 0}", tracks = tracks, source = title.ifBlank { "Чат" }, onBack = onBack,
        menu = { close -> DropdownMenuItem(text = { Text("Открыть чат", color = MusicLook.text) }, onClick = { close(); onOpenChat(chatId) }) },
        rowMenu = { _, t, close -> TrackMenuItems(t.toItem(), onNotice, onOpenChat, close, file = t.file) },
        empty = "Музыки нет",
        onEnd = ::more
    )
}

/** «Скачать» or «Удалить с телефона» for a track's file (the track stays in the chat). */
@Composable
private fun DownloadItems(file: TelegramFileRef, onNotice: (String) -> Unit, close: () -> Unit) {
    val files = com.kirox.app.ui.components.LocalTelegramFiles.current ?: return
    val state by remember(file.id) { files.state(file) }.collectAsState()
    if (state.path != null || state.downloaded > 0) DropdownMenuItem(text = { Text("Удалить с телефона", color = Fusion.colors.danger) }, onClick = {
        if (VoicePlayer.track.value?.file?.id == file.id) VoicePlayer.stop()
        files.deleteLocal(file); onNotice("Файл удалён с телефона — трек остался в чате"); close()
    })
    else DropdownMenuItem(text = { Text("Скачать", color = MusicLook.text) }, onClick = { files.download(file); onNotice("Скачиваю…"); close() })
}

@Composable
private fun TrackMenuItems(item: PlaylistItem, onNotice: (String) -> Unit, onOpenChat: (Long) -> Unit, close: () -> Unit, file: TelegramFileRef? = null) {
    var adding by remember { mutableStateOf(false) }
    if (file != null) DownloadItems(file, onNotice, close)
    val liked = Playlists.all.collectAsState().value.firstOrNull { it.id == Playlists.LIKED }?.has(item.chatId, item.messageId) == true
    DropdownMenuItem(text = { Text(if (liked) "Убрать из «Любимое»" else "В «Любимое» ♥", color = MusicLook.text) }, onClick = {
        Playlists.toggleLiked(item); onNotice(if (liked) "Убрано из «Любимое»" else "Добавлено в «Любимое»"); close()
    })
    DropdownMenuItem(text = { Text("В плейлист…", color = MusicLook.text) }, onClick = { adding = true })
    DropdownMenuItem(text = { Text("Открыть чат", color = MusicLook.text) }, onClick = { close(); onOpenChat(item.chatId) })
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

/**
 * Sends a playlist as a small file. Tracks from channels and public groups open for anyone;
 * tracks from private chats only for those who are in them.
 */
private fun sharePlaylist(context: android.content.Context, id: Long, name: String) {
    val json = Playlists.export(id) ?: return
    runCatching {
        val dir = java.io.File(context.cacheDir, "shared/playlists").apply { mkdirs() }
        val safe = name.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().ifBlank { "playlist" }
        val file = java.io.File(dir, "$safe.kirox-playlist.json").apply { writeText(json) }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/json").putExtra(android.content.Intent.EXTRA_STREAM, uri)
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
