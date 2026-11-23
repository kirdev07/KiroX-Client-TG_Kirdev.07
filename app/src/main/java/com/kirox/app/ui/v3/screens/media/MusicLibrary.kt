package com.kirox.app.ui.v3.screens.media

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll

/*
 * music: the Music tab / board on the real account. Three sections — «Каналы» (the user's music
 * channels and their tracks), «Моя музыка» and «Плейлисты» — with search over all chats, a mini
 * player and the full player as its own screen («MusicPlayer»).
 */

/** Cover colours by track: a stable pair from the message id, so a track keeps its colour. */
private val CoverPairs = listOf(
    Color(0xFF3E7A5A) to Color(0xFF0F1A14), Color(0xFF2D4E73) to Color(0xFF0F1A2A), Color(0xFF6E58C9) to Color(0xFF1E1830),
    Color(0xFFD08A3C) to Color(0xFF3A220A), Color(0xFFA24B5B) to Color(0xFF3A1820), Color(0xFF3C8A8A) to Color(0xFF0E2222),
)
internal fun coverColors(seed: Long): Pair<Color, Color> = CoverPairs[((seed % CoverPairs.size + CoverPairs.size) % CoverPairs.size).toInt()]

/** A square cover: the thumbnail when there is one, else a gradient with a note. */
@Composable
internal fun TrackCover(t: KxTrack?, size: Dp, corner: Dp, modifier: Modifier = Modifier, image: androidx.compose.ui.graphics.ImageBitmap? = null, content: @Composable BoxScope.() -> Unit = {}) {
    val (c1, c2) = coverColors((t?.messageId ?: 0L) + (t?.chatId ?: 0L))
    Box(modifier.size(size).clip(RoundedCornerShape(corner)).cssGrad(150f, c1, c2), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.MusicNote, null, Modifier.size(size * .42f), tint = Color.White.copy(alpha = .35f))
        if (image != null) androidx.compose.foundation.Image(image, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        else KxImage(t?.cover, Modifier.matchParentSize(), maxPx = (size.value * 3).toInt().coerceAtMost(900))
        content()
    }
}

/** Three white bars, kxEq .8s ease-in-out infinite with .2s steps, frozen while paused. */
@Composable
internal fun MusicEqualizer(clock: Long) {
    Row(Modifier.height(18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            val s = if (LocalKxReducedMotion.current) 1f else pingPong(clock, 800, i * 200, .3f, 1f)
            Box(Modifier.width(3.dp).fillMaxHeight().graphicsLayer { scaleY = s; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, 1f) }.background(Color.White))
        }
    }
}

internal fun musicPlural(n: Int): String = com.kirox.app.ui.v3.data.KxFormat.plural(n, "трек", "трека", "треков")

private fun durationText(seconds: Int): String = if (seconds >= 3600) "${seconds / 3600} ч ${seconds / 60 % 60} мин" else "${seconds / 60} мин"

/** Which sub-page of the library is open. */
private sealed interface MusicPage {
    data class Channel(val c: KxMusicChannel) : MusicPage
    data class Playlist(val id: Long) : MusicPage
}

@Composable
internal fun RealMusicLibrary(ctx: KxScreenCtx) {
    val store = ctx.store
    val music = store.music
    val asTab = ctx.arg == "tab"
    LaunchedEffect(Unit) { music.scanChannels() }
    var section by rememberSaveable { mutableIntStateOf(0) }
    var page by remember { mutableStateOf<MusicPage?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<Pair<KxTrack, Long?>?>(null) }
    var pickPlaylistFor by remember { mutableStateOf<KxTrack?>(null) }
    var addingChannel by remember { mutableStateOf(false) }
    var naming by remember { mutableStateOf<Pair<Long?, String>?>(null) }
    val confirm = remember { MConfirmState() }
    val exporter = rememberExporter(music, ctx.toast)
    val p = store.player
    val eqClock by rememberPlayClock(p.playing)
    val bottomSpace = if (asTab) 190.dp else 100.dp

    BackHandler(page != null || searching) { if (searching) { searching = false; query = "" } else page = null }

    fun play(list: List<KxTrack>, i: Int, source: String, shuffle: Boolean = false) = music.play(list, i, source, shuffle)
    val trackActions = TrackActions(
        current = p.track, playing = p.playing, clock = eqClock,
        isMine = { music.isMine(it.chatId, it.messageId) },
        onMine = { t -> val was = music.isMine(t.chatId, t.messageId); music.toggleMine(t); ctx.toast(if (was) "Убрано из «Моя музыка»" else "Добавлено в «Моя музыка»") },
        onMore = { t, pl -> menuFor = t to pl },
        offline = { music.offlineState(it.chatId, it.messageId) }, offlineFraction = { music.offlineFraction(it.chatId, it.messageId) },
    )

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        val (g1, _) = coverColors((p.track?.messageId ?: 0L) + (p.track?.chatId ?: 0L))
        Box(Modifier.fillMaxWidth().height(320.dp).ellipseGlow(g1.copy(alpha = .35f), .3f, 0f, .9f, .7f, .7f))
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sub = page
                if (!asTab || sub != null || searching) MRoundBtn(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", highlight = Kx.w(.14f)) {
                    when { searching -> { searching = false; query = "" }; sub != null -> page = null; else -> ctx.back() }
                }
                if (searching) {
                    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
                    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                    BasicTextField(query, { query = it },
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp, vertical = 12.dp)
                            .focusRequester(focus),
                        singleLine = true, textStyle = ts(15.sp), cursorBrush = SolidColor(Kx.accent),
                        decorationBox = { inner -> if (query.isEmpty()) T("Название или исполнитель", 15.sp, color = Kx.text4); inner() })
                } else Column(Modifier.weight(1f).padding(start = if (asTab && sub == null) 4.dp else 0.dp)) {
                    when (sub) {
                        is MusicPage.Channel -> { T("Канал", 12.sp, color = Kx.text4); T(sub.c.title, 20.sp, FontWeight.ExtraBold, maxLines = 1) }
                        is MusicPage.Playlist -> { T("Плейлист", 12.sp, color = Kx.text4); T(music.playlists.firstOrNull { it.id == sub.id }?.name ?: "", 20.sp, FontWeight.ExtraBold, maxLines = 1) }
                        null -> { T("Из ваших каналов", 12.sp, color = Kx.text4); T("Музыка", 22.sp, FontWeight.ExtraBold) }
                    }
                }
                if (!searching) MRoundBtn(Icons.Outlined.Search, "Поиск", tint = Kx.text2) { searching = true }
                when (val sub = page) {
                    null -> if (!searching) when (section) {
                        0 -> MRoundBtn(Icons.Rounded.Add, "Добавить канал", tint = Kx.text2) { addingChannel = true }
                        2 -> MRoundBtn(Icons.AutoMirrored.Outlined.PlaylistAdd, "Новый плейлист", tint = Kx.text2) { naming = null to "" }
                        else -> {}
                    }
                    is MusicPage.Channel -> if (!searching) MRoundBtn(Icons.Outlined.RemoveCircleOutline, "Убрать из источников", tint = Kx.text2) {
                        confirm.ask("Убрать канал?", "«${sub.c.title}» больше не будет в разделе «Каналы». Вернуть его можно через «+».", "Убрать") {
                            music.removeChannel(sub.c.chatId); page = null; ctx.toast("Канал убран")
                        }
                    }
                    is MusicPage.Playlist -> if (!searching && sub.id != KxPlaylist.MY_MUSIC) {
                        MRoundBtn(Icons.Outlined.Edit, "Переименовать", tint = Kx.text2) { naming = sub.id to (music.playlists.firstOrNull { it.id == sub.id }?.name ?: "") }
                        MRoundBtn(Icons.Outlined.DeleteOutline, "Удалить плейлист", tint = Kx.text2) {
                            confirm.ask("Удалить плейлист?", "Треки останутся в Telegram, пропадёт только список.", "Удалить") { music.deletePlaylist(sub.id); page = null }
                        }
                    }
                }
            }
            when {
                searching -> SearchResults(music, query, bottomSpace, trackActions) { list, i -> play(list, i, "Поиск") }
                page is MusicPage.Channel -> ChannelPage(music, (page as MusicPage.Channel).c, bottomSpace, trackActions, exporter) { list, i, sh -> play(list, i, (page as? MusicPage.Channel)?.c?.title ?: "Канал", sh) }
                page is MusicPage.Playlist -> {
                    val pl = music.playlists.firstOrNull { it.id == (page as MusicPage.Playlist).id }
                    if (pl == null) LaunchedEffect(Unit) { page = null }
                    else PlaylistPage(music, pl, bottomSpace, trackActions, exporter) { i, sh -> play(pl.tracks, i, pl.name, sh) }
                }
                else -> {
                    val chipScroll = androidx.compose.foundation.rememberScrollState() // A9: edge fade while more chips are off screen
                    Row(Modifier.padding(top = 10.dp, bottom = 4.dp).kxEdgeFade(chipScroll).horizontalScroll(chipScroll).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Каналы", "Моя музыка", "Плейлисты", "Скачанные").forEachIndexed { i, s ->
                            val on = i == section
                            Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Kx.accent else Kx.w(.06f))
                                .kxSelectedState(on).kxClick(s, androidx.compose.ui.semantics.Role.Tab) { section = i }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                T(s, 13.sp, if (on) FontWeight.Bold else FontWeight.SemiBold, if (on) Kx.onAccent else Kx.text2)
                            }
                        }
                    }
                    when (section) {
                        0 -> ChannelsSection(music, bottomSpace, onOpen = { page = MusicPage.Channel(it) }, onAdd = { addingChannel = true })
                        1 -> {
                            val mine = music.playlists.firstOrNull { it.mine }
                            if (mine == null || mine.tracks.isEmpty()) MusicEmpty(Icons.Rounded.FavoriteBorder, "Здесь пока пусто", "Нажмите ♡ у трека в канале — он появится в «Моя музыка»")
                            else PlaylistPage(music, mine, bottomSpace, trackActions, exporter) { i, sh -> play(mine.tracks, i, "Моя музыка", sh) }
                        }
                        3 -> DownloadedSection(music, bottomSpace, trackActions, exporter, confirm) { list, i -> play(list, i, "Скачанные") }
                        else -> PlaylistsSection(music, bottomSpace, onOpen = { page = MusicPage.Playlist(it) }, onCreate = { naming = null to "" })
                    }
                }
            }
        }

        if (p.track != null) MusicMiniPlayer(store, eqClock, Modifier.align(Alignment.BottomCenter).padding(bottom = if (asTab) 78.dp else 0.dp)) { ctx.open("MusicPlayer") }

        // Sheets and dialogs
        val menu = menuFor
        MSheet(menu != null, onClose = { menuFor = null }) {
            if (menu != null) TrackMenu(music, menu.first, menu.second, exporter, onClose = { menuFor = null }, toast = ctx.toast,
                onPickPlaylist = { pickPlaylistFor = it; menuFor = null })
        }
        val toAdd = pickPlaylistFor
        MSheet(toAdd != null, onClose = { pickPlaylistFor = null }) {
            if (toAdd != null) PlaylistPicker(music, toAdd, onNew = { naming = -1L to ""; }, onDone = { msg -> pickPlaylistFor = null; ctx.toast(msg) })
        }
        AddChannelSheet(addingChannel, music, onClose = { addingChannel = false }, toast = ctx.toast)
        NameDialog(naming, onDismiss = { naming = null }) { name ->
            val target = naming?.first
            when {
                target == null -> { val id = music.createPlaylist(name); ctx.toast("Плейлист создан"); page = MusicPage.Playlist(id) }
                target == -1L -> { val id = music.createPlaylist(name); pickPlaylistFor?.let { music.addToPlaylist(id, it); ctx.toast("Создан «${name.trim()}»") }; pickPlaylistFor = null }
                else -> music.renamePlaylist(target, name)
            }
            naming = null
        }
        MConfirmHost(confirm)
        ExportOverlay(exporter)
    }
}

/** Track row callbacks shared by every list. */
private class TrackActions(
    val current: KxTrack?, val playing: Boolean, val clock: Long,
    val isMine: (KxTrack) -> Boolean, val onMine: (KxTrack) -> Unit, val onMore: (KxTrack, Long?) -> Unit,
    /** KxMusic.offlineState and its progress. */
    val offline: (KxTrack) -> Int = { 0 }, val offlineFraction: (KxTrack) -> Float = { 0f },
)

@Composable
private fun TrackRow(t: KxTrack, a: TrackActions, playlistId: Long? = null, subtitle: String? = null, onClick: () -> Unit) {
    val active = a.current?.let { it.chatId == t.chatId && it.messageId == t.messageId } == true
    val rowBg by animateColorAsState(if (active) Kx.accent.copy(alpha = .08f) else Color.Transparent, tween(300), label = "row")
    val mine = a.isMine(t)
    Row(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(16.dp)).background(rowBg)
        .kxClick("${t.title}, ${t.performer}", onClick = onClick).padding(start = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TrackCover(t, 46.dp, 12.dp) { if (active && a.playing) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .35f)), contentAlignment = Alignment.Center) { MusicEqualizer(a.clock) } }
        Column(Modifier.weight(1f)) {
            T(t.title, 15.sp, FontWeight.SemiBold, if (active) Kx.accent else Kx.text, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OfflineMark(a.offline(t), a.offlineFraction(t))
                T(subtitle ?: listOf(t.performer, if (t.duration > 0) fmtSec(t.duration) else "").filter { it.isNotBlank() }.joinToString(" · "), 12.sp, color = Kx.text4, maxLines = 1)
            }
        }
        Box(Modifier.size(40.dp).clip(CircleShape).kxClick(if (mine) "Убрать из «Моя музыка»" else "В «Моя музыка»") { a.onMine(t) }, contentAlignment = Alignment.Center) {
            Icon(if (mine) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, null, Modifier.size(20.dp), tint = if (mine) Color(0xFFFF8A9A) else Kx.text4)
        }
        Box(Modifier.size(40.dp).clip(CircleShape).kxClick("Ещё") { a.onMore(t, playlistId) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.MoreVert, null, Modifier.size(20.dp), tint = Kx.text4)
        }
    }
}

@Composable
private fun MusicEmpty(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, action: String? = null, onAction: () -> Unit = {}) =
    KxEmpty(icon, title, text, Modifier.padding(top = 16.dp), action = action, onAction = if (action != null) onAction else null) // A4

/** «Слушать» + «Перемешать» over a list. */
@Composable
private fun PlayButtons(count: Int, seconds: Int, onPlay: () -> Unit, onShuffle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        T("$count ${musicPlural(count)}${if (seconds > 0) " · " + durationText(seconds) else ""}", 13.sp, color = Kx.text4, modifier = Modifier.weight(1f))
        Row(Modifier.height(44.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick("Перемешать", onClick = onShuffle).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp), tint = Kx.text); T("Перемешать", 13.sp, FontWeight.Bold)
        }
        Row(Modifier.height(44.dp).clip(CircleShape).background(Kx.accent).kxClick("Слушать", onClick = onPlay).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp), tint = Kx.onAccent); T("Слушать", 13.sp, FontWeight.ExtraBold, Kx.onAccent)
        }
    }
}

// ---- Каналы ----

@Composable
private fun ColumnScope.ChannelsSection(music: KxMusic, bottom: Dp, onOpen: (KxMusicChannel) -> Unit, onAdd: () -> Unit) {
    val list = music.channels
    when {
        list == null || (list.isEmpty() && music.scanning) -> Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = Kx.accent)
            T("Ищем музыку в ваших каналах…", 14.sp, color = Kx.text3)
        }
        list.isEmpty() -> MusicEmpty(Icons.Rounded.LibraryMusic, "Музыкальных каналов пока нет",
            "Подпишитесь на канал с музыкой или добавьте свой чат как источник", "Добавить канал", onAdd)
        else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom)) {
            if (music.scanning) item { Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(14.dp), color = Kx.accent, strokeWidth = 2.dp); T("Обновляем список…", 12.sp, color = Kx.text4)
            } }
            items(list.size, key = { "c:${list[it].chatId}" }) { i ->
                val c = list[i]
                Row(Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(18.dp)).kxClick(c.title) { onOpen(c) }.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KxAvatar(c.title, c.chatId, size = 50.dp, square = true, photo = c.avatar, icon = if (c.kind == 2) Icons.Rounded.Bookmark else null)
                    Column(Modifier.weight(1f)) {
                        T(c.title, 15.sp, FontWeight.SemiBold, maxLines = 1)
                        T("${c.count} ${musicPlural(c.count)}${if (c.added) " · добавлен вами" else ""}", 12.sp, color = Kx.text4, maxLines = 1)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(22.dp), tint = Kx.text4)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp).height(54.dp).clip(RoundedCornerShape(18.dp)).dashedBorder(Kx.w(.2f), 18.dp)
                    .kxClick("Добавить канал", onClick = onAdd).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Add, null, tint = Kx.accent); T("Добавить канал или чат", 14.sp, FontWeight.Bold, Kx.accent)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ChannelPage(music: KxMusic, c: KxMusicChannel, bottom: Dp, a: TrackActions, exporter: Exporter, onPlay: (List<KxTrack>, Int, Boolean) -> Unit) {
    var tracks by remember(c.chatId) { mutableStateOf<List<KxTrack>?>(null) }
    var next by remember(c.chatId) { mutableLongStateOf(0L) }
    var loadingMore by remember(c.chatId) { mutableStateOf(false) }
    LaunchedEffect(c.chatId) { val (l, n) = music.channelTracks(c.chatId, 0); tracks = l; next = n }
    val list = tracks
    val state = rememberLazyListState()
    // The next page loads near the end of the list.
    val nearEnd by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= state.layoutInfo.totalItemsCount - 6 } == true } }
    LaunchedEffect(nearEnd, next) {
        if (nearEnd && next != 0L && !loadingMore && list != null) {
            loadingMore = true
            val (more, n) = music.channelTracks(c.chatId, next)
            tracks = (tracks.orEmpty() + more).distinctBy { it.chatId to it.messageId }; next = if (more.isEmpty()) 0L else n
            loadingMore = false
        }
    }
    when {
        list == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Kx.accent) }
        list.isEmpty() -> MusicEmpty(Icons.Rounded.MusicOff, "Здесь нет музыки", "В этом чате не нашлось аудиофайлов")
        else -> LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottom)) {
            item {
                PlayButtons(maxOf(c.count, list.size), 0, onPlay = { onPlay(list, 0, false) }, onShuffle = { onPlay(list, -1, true) })
                // «Скачать всё» takes the tracks loaded so far (scroll on for more).
                OfflineBar(music, list, exporter, c.title)
            }
            items(list.size, key = { "t:${list[it].chatId}:${list[it].messageId}" }) { i ->
                val t = list[i]
                TrackRow(t, a, subtitle = listOf(t.performer, if (t.duration > 0) fmtSec(t.duration) else "", t.date).filter { it.isNotBlank() }.joinToString(" · ")) { onPlay(list, i, false) }
            }
            if (next != 0L) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), color = Kx.accent, strokeWidth = 2.dp) } }
        }
    }
}

// ---- Моя музыка / playlists ----

@Composable
private fun ColumnScope.PlaylistsSection(music: KxMusic, bottom: Dp, onOpen: (Long) -> Unit, onCreate: () -> Unit) {
    val lists = music.playlists
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom)) {
        item {
            Row(Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(18.dp)).kxClick("Новый плейлист", onClick = onCreate).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).dashedBorder(Kx.w(.25f), 14.dp), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null, tint = Kx.accent) }
                T("Новый плейлист", 15.sp, FontWeight.Bold, Kx.accent)
            }
        }
        items(lists.size, key = { "p:${lists[it].id}" }) { i ->
            val pl = lists[i]
            Row(Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(18.dp)).kxClick(pl.name) { onOpen(pl.id) }.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val first = pl.tracks.firstOrNull()
                Box(Modifier.size(50.dp)) {
                    if (pl.mine) Box(Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).cssGrad(150f, Color(0xFFA24B5B), Color(0xFF3A1820)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Favorite, null, Modifier.size(24.dp), tint = Color(0xFFFFD6DE))
                    } else TrackCover(first, 50.dp, 14.dp) { if (first == null) Icon(Icons.AutoMirrored.Outlined.QueueMusic, null, tint = Color.White) }
                }
                Column(Modifier.weight(1f)) {
                    T(pl.name, 15.sp, FontWeight.SemiBold, maxLines = 1)
                    T("${pl.tracks.size} ${musicPlural(pl.tracks.size)}", 12.sp, color = Kx.text4)
                }
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(22.dp), tint = Kx.text4)
            }
        }
    }
}

/** A playlist (or «Моя музыка»): play / shuffle, tracks with ↑↓ reorder in edit mode. */
@Composable
private fun ColumnScope.PlaylistPage(music: KxMusic, pl: KxPlaylist, bottom: Dp, a: TrackActions, exporter: Exporter, onPlay: (Int, Boolean) -> Unit) {
    var editing by remember(pl.id) { mutableStateOf(false) }
    if (pl.tracks.isEmpty()) { MusicEmpty(Icons.AutoMirrored.Outlined.QueueMusic, "Плейлист пуст", "Добавьте треки через ⋮ у трека в канале"); return }
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottom)) {
        item {
            PlayButtons(pl.tracks.size, pl.totalSeconds, onPlay = { onPlay(0, false) }, onShuffle = { onPlay(-1, true) })
            OfflineBar(music, pl.tracks, exporter, pl.name)
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.End) {
                T(if (editing) "Готово" else "Изменить порядок", 13.sp, FontWeight.Bold, Kx.accent,
                    Modifier.clip(RoundedCornerShape(10.dp)).kxClick { editing = !editing }.padding(horizontal = 8.dp, vertical = 6.dp))
            }
        }
        items(pl.tracks.size, key = { "pt:${pl.tracks[it].chatId}:${pl.tracks[it].messageId}" }) { i ->
            val t = pl.tracks[i]
            if (editing) Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackCover(t, 40.dp, 10.dp)
                Column(Modifier.weight(1f)) { T(t.title, 14.sp, FontWeight.SemiBold, maxLines = 1); T(t.performer, 12.sp, color = Kx.text4, maxLines = 1) }
                MusicDragHandle(i, pl.tracks.lastIndex) { from, to -> music.movePlaylistItem(pl.id, from, to) }
                Box(Modifier.kxHitClick("Убрать из плейлиста") { music.removeFromPlaylist(pl.id, t.chatId, t.messageId) }.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, Modifier.size(20.dp), tint = Kx.danger)
                }
            } else TrackRow(t, a, playlistId = pl.id) { onPlay(i, false) }
        }
    }
}

@Composable
private fun ColumnScope.SearchResults(music: KxMusic, query: String, bottom: Dp, a: TrackActions, onPlay: (List<KxTrack>, Int) -> Unit) {
    var found by remember { mutableStateOf<List<KxTrack>?>(emptyList()) }
    LaunchedEffect(query) {
        if (query.isBlank()) { found = emptyList(); return@LaunchedEffect }
        delay(400); found = null; found = music.search(query)
    }
    val list = found
    when {
        query.isBlank() -> MusicEmpty(Icons.Outlined.Search, "Поиск музыки", "Ищем по названию и исполнителю во всех ваших чатах и каналах")
        list == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Kx.accent) }
        list.isEmpty() -> MusicEmpty(Icons.Rounded.SearchOff, "Ничего не нашлось", "Попробуйте другое название")
        else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottom)) {
            items(list.size, key = { "s:${list[it].chatId}:${list[it].messageId}" }) { i ->
                val t = list[i]
                TrackRow(t, a, subtitle = listOf(t.performer, t.chatTitle).filter { it.isNotBlank() }.joinToString(" · ")) { onPlay(list, i) }
            }
        }
    }
}

// ---- Menus and sheets ----

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color = Kx.text, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).kxClick(text, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = color); T(text, 15.sp, FontWeight.SemiBold, color)
    }
}

@Composable
private fun TrackMenu(music: KxMusic, t: KxTrack, playlistId: Long?, exporter: Exporter, onClose: () -> Unit, toast: (String) -> Unit, onPickPlaylist: (KxTrack) -> Unit) {
    Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TrackCover(t, 48.dp, 12.dp)
        Column { T(t.title, 16.sp, FontWeight.Bold, maxLines = 1); T(listOf(t.performer, t.chatTitle).filter { it.isNotBlank() }.joinToString(" · "), 12.sp, color = Kx.text4, maxLines = 1) }
    }
    val mine = music.isMine(t.chatId, t.messageId)
    SheetRow(if (mine) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, if (mine) "Убрать из «Моя музыка»" else "В «Моя музыка»") {
        music.toggleMine(t); onClose(); toast(if (mine) "Убрано из «Моя музыка»" else "Добавлено в «Моя музыка»")
    }
    SheetRow(Icons.AutoMirrored.Outlined.PlaylistAdd, "В плейлист…") { onPickPlaylist(t) }
    if (t.fileId != 0) SheetRow(Icons.AutoMirrored.Outlined.QueueMusic, "Играть следующим") { music.playNext(t); onClose(); toast("Будет следующим") }
    when (music.offlineState(t.chatId, t.messageId)) {
        0, 5 -> SheetRow(Icons.Outlined.DownloadForOffline, "Слушать без интернета") { music.downloadOffline(listOf(t)); onClose(); toast("Скачиваем для офлайна") }
        4 -> {
            val toastEx = com.kirox.app.ui.v3.components.LocalKxToastEx.current
            // critique-C P2: red, with «Отменить» (downloads the file again).
            SheetRow(Icons.Outlined.DeleteOutline, "Удалить с телефона", Kx.danger) {
                music.removeOffline(t.chatId, t.messageId); onClose()
                toastEx("Удалено с телефона — в Telegram трек остался", com.kirox.app.ui.v3.components.KxToastKind.Info, "Отменить") { music.downloadOffline(listOf(t)) }
            }
        }
        else -> SheetRow(Icons.Outlined.Close, "Отменить загрузку") { music.cancelOffline(t.chatId, t.messageId); onClose() }
    }
    SheetRow(Icons.Outlined.LibraryMusic, "Сохранить файл в Музыку телефона") { onClose(); exporter.run(listOf(t), null) }
    if (playlistId != null && playlistId != KxPlaylist.MY_MUSIC) SheetRow(Icons.Outlined.RemoveCircleOutline, "Убрать из плейлиста", Kx.danger) {
        music.removeFromPlaylist(playlistId, t.chatId, t.messageId); onClose()
    }
}

@Composable
private fun PlaylistPicker(music: KxMusic, t: KxTrack, onNew: () -> Unit, onDone: (String) -> Unit) {
    T("Добавить в плейлист", 17.sp, FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
    SheetRow(Icons.Rounded.Add, "Новый плейлист", Kx.accent, onClick = onNew)
    Column(Modifier.heightIn(max = 360.dp).verticalScrollSafe()) {
        music.playlists.filterNot { it.mine }.forEach { pl ->
            val has = pl.tracks.any { it.chatId == t.chatId && it.messageId == t.messageId }
            SheetRow(if (has) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Outlined.QueueMusic, pl.name, if (has) Kx.accent else Kx.text) {
                if (has) onDone("Уже в «${pl.name}»") else { music.addToPlaylist(pl.id, t); onDone("Добавлено в «${pl.name}»") }
            }
        }
    }
}

@Composable
private fun Modifier.verticalScrollSafe(): Modifier = this.then(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))

/** Pick a chat to add as a music source: chats with music first, title search over all chats. */
@Composable
private fun AddChannelSheet(visible: Boolean, music: KxMusic, onClose: () -> Unit, toast: (String) -> Unit) {
    var query by remember(visible) { mutableStateOf("") }
    var found by remember(visible) { mutableStateOf<List<KxMusicChannel>?>(null) }
    LaunchedEffect(visible, query) {
        if (!visible) return@LaunchedEffect
        if (query.isNotBlank()) delay(400)
        found = null
        found = music.channelCandidates(query)
    }
    MSheet(visible, onClose) {
        T("Добавить источник", 18.sp, FontWeight.ExtraBold)
        T("Канал, группа или чат — его музыка появится в «Каналы»", 13.sp, color = Kx.text4, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
        BasicTextField(query, { query = it },
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp, vertical = 12.dp),
            singleLine = true, textStyle = ts(15.sp), cursorBrush = SolidColor(Kx.accent),
            decorationBox = { inner -> if (query.isEmpty()) T("Название канала или чата", 15.sp, color = Kx.text4); inner() })
        Column(Modifier.padding(top = 8.dp).heightIn(min = 120.dp, max = 380.dp).verticalScrollSafe()) {
            val list = found
            when {
                list == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Kx.accent) }
                list.isEmpty() -> T(if (query.isBlank()) "Введите название — найдём чат и посчитаем треки" else "Ничего не нашлось", 13.sp, color = Kx.text4, modifier = Modifier.padding(16.dp))
                else -> list.forEach { c ->
                    Row(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(14.dp)).kxClick(c.title) {
                        music.addChannel(c); toast("«${c.title}» добавлен"); onClose()
                    }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KxAvatar(c.title, c.chatId, size = 42.dp, square = true, photo = c.avatar, icon = if (c.kind == 2) Icons.Rounded.Bookmark else null)
                        Column(Modifier.weight(1f)) {
                            T(c.title, 15.sp, FontWeight.SemiBold, maxLines = 1)
                            T(if (c.count > 0) "${c.count} ${musicPlural(c.count)}" else "Музыки пока нет", 12.sp, color = Kx.text4)
                        }
                        Icon(Icons.Rounded.Add, null, tint = Kx.accent)
                    }
                }
            }
        }
    }
}

/** Name of a new / renamed playlist ([state] = target id (null new, -1 new for a track) to the starting name). */
@Composable
private fun NameDialog(state: Pair<Long?, String>?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    if (state == null) return
    var name by remember(state) { mutableStateOf(state.second) }
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).kxClick("Отмена", onClick = onDismiss).imePadding(), contentAlignment = Alignment.Center) {
        val shape = RoundedCornerShape(28.dp)
        Column(Modifier.padding(horizontal = 24.dp).fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF232A26), Color(0xFF161B18)))).border(1.dp, Kx.w(.14f), shape).kxClick { }.padding(20.dp)) {
            T(if (state.first == null || state.first == -1L) "Новый плейлист" else "Переименовать", 18.sp, FontWeight.ExtraBold)
            val focus = remember { androidx.compose.ui.focus.FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            BasicTextField(name, { if (it.length <= 40) name = it },
                Modifier.padding(top = 14.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp, vertical = 14.dp).focusRequester(focus),
                singleLine = true, textStyle = ts(15.sp), cursorBrush = SolidColor(Kx.accent),
                decorationBox = { inner -> if (name.isEmpty()) T("Название", 15.sp, color = Kx.text4); inner() })
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // critique-C P2: the same buttons as KxConfirmDialog (a prompt = confirm dialog + one field).
                KxButton("Отмена", Modifier.weight(1f), KxButtonKind.Quiet, height = 48.dp, onClick = onDismiss)
                KxButton("Сохранить", Modifier.weight(1f), height = 48.dp, enabled = name.isNotBlank()) { onSave(name.trim()) }
            }
        }
    }
}

// ---- Mini player ----

@Composable
internal fun MusicMiniPlayer(store: KxStore, clock: Long, modifier: Modifier, onExpand: () -> Unit) {
    val p = store.player
    val t = p.track ?: return
    val shape = RoundedCornerShape(24.dp)
    val progress = if (p.durationMs > 0) p.positionMs.toFloat() / p.durationMs else 0f
    Box(modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 20.dp).fillMaxWidth().height(66.dp)
        .shadow(34.dp, shape, ambientColor = Color.Black.copy(alpha = .55f), spotColor = Color.Black.copy(alpha = .55f))
        .clip(shape).background(Kx.surface).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
        .border(1.dp, Kx.w(.16f), shape).topHighlight(Kx.w(.35f)).kxClick("Открыть плеер", onClick = onExpand)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrackCover(t, 50.dp, 14.dp, image = store.music.cover) { if (p.playing) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .3f)), contentAlignment = Alignment.Center) { MusicEqualizer(clock) } }
            Column(Modifier.weight(1f)) {
                T(t.title, 14.sp, FontWeight.Bold, maxLines = 1)
                T(t.performer.ifBlank { t.chatTitle }, 12.sp, color = Color(0xFFA6B5AC), maxLines = 1)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White).kxClick(if (p.playing) "Пауза" else "Играть") { store.playerToggle() }, contentAlignment = Alignment.Center) {
                if (p.loading) CircularProgressIndicator(Modifier.size(22.dp), color = Kx.bg, strokeWidth = 2.dp)
                else Icon(if (p.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(24.dp), tint = Kx.bg)
            }
            Box(Modifier.size(40.dp, 44.dp).kxClick("Следующий трек") { store.playerNext() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SkipNext, null, Modifier.size(24.dp), tint = Kx.text2)
            }
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(Kx.accent))
    }
}

// ---- Offline and «Сохранить в Музыку телефона» ----

internal fun fmtBytes(b: Long): String = com.kirox.app.ui.v3.model.kxBytes(b) // critique-C P2: shared size format

/** Small mark before a track's subtitle: offline ✓, downloading (ring), waiting / paused / failed. */
@Composable
private fun OfflineMark(state: Int, fraction: Float) {
    when (state) {
        4 -> Icon(Icons.Rounded.DownloadDone, "Скачан", Modifier.size(14.dp), tint = Kx.accent)
        2 -> CircularProgressIndicator(progress = { fraction.coerceAtLeast(.05f) }, modifier = Modifier.size(12.dp), color = Kx.accent, strokeWidth = 2.dp, trackColor = Kx.w(.15f))
        1, 3 -> Icon(if (state == 3) Icons.Rounded.PauseCircle else Icons.Rounded.Schedule, if (state == 3) "Загрузка на паузе" else "В очереди на загрузку", Modifier.size(14.dp), tint = Kx.text4)
        5 -> Icon(Icons.Rounded.ErrorOutline, "Не скачался", Modifier.size(14.dp), tint = Kx.danger)
        else -> {}
    }
}

/** Runs an export with a progress card; the toast says where it went. */
internal class Exporter {
    var progress by mutableStateOf<Pair<Int, Int>?>(null)
    var run: (List<KxTrack>, String?) -> Unit = { _, _ -> }
}

@Composable
private fun rememberExporter(music: KxMusic, toast: (String) -> Unit): Exporter {
    val scope = rememberCoroutineScope()
    val e = remember { Exporter() }
    e.run = { tracks, folder ->
        if (e.progress == null && tracks.isNotEmpty()) scope.launch {
            e.progress = 0 to tracks.size
            val msg = try { music.exportToPhone(tracks, folder) { d, n -> e.progress = d to n } } finally { e.progress = null }
            toast(msg)
        }
    }
    return e
}

@Composable
private fun BoxScope.ExportOverlay(e: Exporter) {
    val p = e.progress ?: return
    Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp, start = 16.dp, end = 16.dp).fillMaxWidth()
        .clip(RoundedCornerShape(18.dp)).background(Kx.surface).border(1.dp, Kx.w(.14f), RoundedCornerShape(18.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(progress = { if (p.second > 0) p.first.toFloat() / p.second else 0f }, modifier = Modifier.size(26.dp), color = Kx.accent, strokeWidth = 3.dp, trackColor = Kx.w(.15f))
        Column(Modifier.weight(1f)) {
            T("Сохраняем в Музыку телефона", 14.sp, FontWeight.Bold)
            T("${p.first} из ${p.second} · Музыка/kiroX", 12.sp, color = Kx.text4)
        }
    }
}

/** Under a playlist / channel: «Скачать всё» (or how much is offline) and «В Музыку телефона». */
@Composable
private fun OfflineBar(music: KxMusic, tracks: List<KxTrack>, exporter: Exporter, folder: String) {
    val offlineCount = tracks.count { music.offlineState(it.chatId, it.messageId) == 4 }
    val pending = tracks.count { music.offlineState(it.chatId, it.messageId) in 1..3 }
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val all = offlineCount == tracks.size && tracks.isNotEmpty()
        val label = when { all -> "Всё скачано"; pending > 0 -> "Скачано $offlineCount из ${tracks.size}"; else -> "Скачать всё" }
        Row(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(if (all) Kx.accent.copy(alpha = .15f) else Kx.w(.08f))
            .kxClick(label, enabled = !all && pending == 0) { music.downloadOffline(tracks) }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(if (all) Icons.Rounded.DownloadDone else Icons.Outlined.DownloadForOffline, null, Modifier.size(18.dp), tint = if (all) Kx.accent else Kx.text)
            T(label, 13.sp, FontWeight.Bold, if (all) Kx.accent else Kx.text, maxLines = 1)
        }
        Row(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick("Сохранить в Музыку телефона") { exporter.run(tracks, folder) }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.LibraryMusic, null, Modifier.size(18.dp), tint = Kx.text)
            T("В Музыку телефона", 13.sp, FontWeight.Bold, maxLines = 1)
        }
    }
    DownloadProgressCard(music)
}

/** The running «Скачать всё»: progress, size, pause / resume, cancel. */
@Composable
private fun DownloadProgressCard(music: KxMusic) {
    val p = music.offlineProgress
    if (!p.busy) return
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).border(1.dp, Kx.w(.10f), RoundedCornerShape(18.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                T(if (p.paused) "Загрузка на паузе" else "Скачиваем для офлайна", 14.sp, FontWeight.Bold)
                T("${p.done} из ${p.total}" + (if (p.bytes > 0) " · ${fmtBytes(p.downloadedBytes)} из ${fmtBytes(p.bytes)}" else "") + (if (p.failed > 0) " · ошибок: ${p.failed}" else ""), 12.sp, color = Kx.text4, maxLines = 1)
            }
            MRoundBtn(if (p.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, if (p.paused) "Продолжить загрузку" else "Пауза загрузки", size = 36.dp, iconSize = 20.dp, border = null) { music.pauseOffline(!p.paused) }
            MRoundBtn(Icons.Rounded.Close, "Отменить загрузку", size = 36.dp, iconSize = 20.dp, border = null) { music.cancelAllOffline() }
        }
        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.12f))) {
            Box(Modifier.fillMaxWidth(p.fraction).fillMaxHeight().background(Kx.accent))
        }
    }
}

/** «Скачанные»: what plays without network, the space it takes, auto-download settings, delete all. */
@Composable
private fun ColumnScope.DownloadedSection(music: KxMusic, bottom: Dp, a: TrackActions, exporter: Exporter, confirm: MConfirmState, onPlay: (List<KxTrack>, Int) -> Unit) {
    val list = music.offlineTracks
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottom)) {
        item {
            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.09f), RoundedCornerShape(20.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.SdStorage, null, tint = Kx.accent)
                    Column(Modifier.weight(1f)) {
                        T("${list.size} ${musicPlural(list.size)} · ${fmtBytes(music.offlineBytes)}", 15.sp, FontWeight.Bold)
                        T("Играют без интернета; очистка кэша их не удаляет", 12.sp, color = Kx.text4)
                    }
                }
                OfflineSwitch("Скачивать «Моя музыка»", "Трек с ♡ сохраняется на телефон сам", music.autoDownloadLiked) { music.setAutoDownloadLiked(it) }
                if (music.autoDownloadLiked) OfflineSwitch("Только по Wi-Fi", "Не тратить мобильный интернет", music.offlineWifiOnly) { music.setOfflineWifiOnly(it) }
            }
            DownloadProgressCard(music)
            if (list.isNotEmpty()) {
                PlayButtons(list.size, list.sumOf { it.duration }, onPlay = { onPlay(list, 0) }, onShuffle = { music.play(list, -1, "Скачанные", true) })
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick("Сохранить все в Музыку телефона") { exporter.run(list, null) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.LibraryMusic, null, Modifier.size(18.dp), tint = Kx.text); T("В Музыку телефона", 13.sp, FontWeight.Bold, maxLines = 1)
                    }
                    Row(Modifier.weight(1f).height(44.dp).clip(CircleShape).background(Kx.danger.copy(alpha = .14f)).kxClick("Удалить скачанное") {
                        confirm.ask("Удалить скачанное?", "${list.size} ${musicPlural(list.size)} (${fmtBytes(music.offlineBytes)}) пропадут с телефона. В Telegram они останутся.", "Удалить") { music.removeAllOffline() }
                    }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp), tint = Kx.danger); T("Удалить скачанное", 13.sp, FontWeight.Bold, Kx.danger, maxLines = 1)
                    }
                }
            }
        }
        if (list.isEmpty()) item { MusicEmpty(Icons.Outlined.DownloadForOffline, "Пока ничего не скачано", "«Слушать без интернета» в меню ⋮ у трека или «Скачать всё» в плейлисте") }
        items(list.size, key = { "o:${list[it].chatId}:${list[it].messageId}" }) { i ->
            TrackRow(list[i], a, subtitle = listOf(list[i].performer, fmtBytes(list[i].size)).filter { it.isNotBlank() }.joinToString(" · ")) { onPlay(list, i) }
        }
    }
}

@Composable
private fun OfflineSwitch(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).kxToggleState(on).kxClick(title) { onChange(!on) },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) { T(title, 14.sp, FontWeight.SemiBold); T(sub, 12.sp, color = Kx.text4) }
        KxSwitch(on, trackOn = Kx.accent)
    }
}
