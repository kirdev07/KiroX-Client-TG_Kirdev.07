package com.kirox.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.kirox.app.i18n.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.telegram.TelegramMedia
import com.kirox.app.ui.components.FusionFullscreen
import com.kirox.app.ui.components.fusionScreenBackground
import com.kirox.app.ui.components.rememberTelegramBitmap
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kirox.app.ui.icons.KiroXIcon as Glyph

/** The open chat's voice notes, oldest first: after one ends the next one plays. */
val LocalChatVoices = compositionLocalOf { emptyList<com.kirox.app.telegram.TelegramFileRef>() }

/** The open chat's music files, oldest first: the queue a tap on one of them plays. */
val LocalChatTracks = compositionLocalOf { emptyList<Track>() }

/** The open chat's title, shown in the player as «Играет из». */
val LocalChatTitle = compositionLocalOf { "" }

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

/** The cover of [track]: the picture inside the file when it plays, else Telegram's album thumbnail, else a note. */
@Composable
private fun trackCover(track: Track?, playing: Boolean, sizePx: Int): ImageBitmap? {
    val embedded by VoicePlayer.cover.collectAsState()
    val thumbnail = com.kirox.app.ui.music.rememberAudioCover(track?.cover, track?.file, sizePx)
    val own = if (playing) embedded else null
    return remember(own, thumbnail) { own?.asImageBitmap() } ?: thumbnail
}

@Composable
private fun CoverArt(track: Track?, size: Dp, corner: Dp, modifier: Modifier = Modifier, playing: Boolean = true) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val image = trackCover(track, playing, px)
    Box(
        modifier.size(size).clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(Fusion.colors.accent.copy(alpha = .55f), Fusion.colors.surface2))),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else KiroXIcon(Glyph.Music, null, Modifier.size(size * .42f), Color.White.copy(alpha = .85f))
    }
}

/** A round icon button; [filled] is the big accent play button. */
@Composable
private fun PlayerButton(icon: Glyph, description: String, size: Dp = 48.dp, iconSize: Dp = 26.dp, filled: Boolean = false, active: Boolean = true, accent: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(if (filled) Fusion.colors.accent else Color.Transparent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val tint = when {
            filled -> Fusion.colors.onAccent
            accent -> Fusion.colors.accent
            active -> Fusion.colors.textPrimary
            else -> Fusion.colors.textTertiary
        }
        KiroXIcon(icon, description, Modifier.size(iconSize), tint)
    }
}

/** Left / right swipe over [content] switches to the next / previous track. */
private fun Modifier.swipeTracks(context: android.content.Context): Modifier = pointerInput(Unit) {
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragEnd = {
            if (total < -120f) VoicePlayer.next(context) else if (total > 120f) VoicePlayer.previous(context)
        },
        onHorizontalDrag = { _, amount -> total += amount }
    )
}

/**
 * The bar of the playing track: cover, title, play / pause, next, close and a thin progress line.
 * A tap opens the player, a swipe switches tracks. [floating] — over the tabs of the main screen.
 */
@Composable
fun MiniPlayerBar(floating: Boolean = false) {
    val track by VoicePlayer.track.collectAsState()
    val playingPath by VoicePlayer.playingPath.collectAsState()
    val loading by VoicePlayer.loading.collectAsState()
    val current = track ?: return
    val context = LocalContext.current
    val (position, duration) = rememberPosition()
    val shape = RoundedCornerShape(if (floating) 18.dp else 0.dp)
    Column(
        Modifier.fillMaxWidth().then(if (floating) Modifier.padding(horizontal = 10.dp, vertical = 4.dp) else Modifier)
            .clip(shape).background(Fusion.colors.surface1.copy(alpha = .97f)).clickable { MusicPlayerUi.open() }.swipeTracks(context)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverArt(current, 40.dp, 10.dp)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                AnimatedContent(current, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "mini_track") { t ->
                    Column {
                        androidx.compose.material3.Text(t.title, color = Fusion.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        androidx.compose.material3.Text(t.artist, color = Fusion.colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (loading) Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), color = Fusion.colors.accent, strokeWidth = 2.dp) }
            else PlayerButton(if (playingPath != null) Glyph.Pause else Glyph.Play, if (playingPath != null) "Пауза" else "Играть", size = 44.dp) { VoicePlayer.playPause() }
            PlayerButton(Glyph.SkipNext, "Следующий трек", size = 44.dp, iconSize = 24.dp) { VoicePlayer.next(context) }
            PlayerButton(Glyph.Close, "Закрыть плеер", size = 40.dp, iconSize = 20.dp, active = false) { VoicePlayer.stop() }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Fusion.colors.surface3)) {
            Box(Modifier.fillMaxWidth(if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f).fillMaxHeight().background(Fusion.colors.accent))
        }
    }
}

/** An output kiroX can play to: the phone's speaker, Bluetooth or wired headphones, a USB device. */
private data class Output(val device: android.media.AudioDeviceInfo, val name: String, val headphones: Boolean)

/** Outputs to choose from now, the phone first. */
private fun outputs(context: android.content.Context): List<Output> {
    val audio = context.getSystemService(android.media.AudioManager::class.java) ?: return emptyList()
    val bluetooth = buildSet {
        add(android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        // Bluetooth LE audio exists from Android 12.
        if (android.os.Build.VERSION.SDK_INT >= 31) { add(android.media.AudioDeviceInfo.TYPE_BLE_HEADSET); add(android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER) }
    }
    val wired = setOf(android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES, android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET, android.media.AudioDeviceInfo.TYPE_USB_HEADSET)
    return audio.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { d ->
        when (d.type) {
            android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> Output(d, "Телефон", false)
            in bluetooth -> Output(d, d.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth", true)
            in wired -> Output(d, "Проводные наушники", true)
            else -> null
        }
    }.distinctBy { it.name to it.headphones }.sortedBy { if (it.headphones) 1 else 0 }
}

/** Where the sound goes now: the output chosen in the player, else what Android picks (headphones before the speaker). */
private fun currentOutput(list: List<Output>, chosen: Int?): Output? =
    list.firstOrNull { it.device.id == chosen } ?: list.firstOrNull { it.headphones } ?: list.firstOrNull()

/** A thin seek bar with a round knob: tap or drag to move through the track. */
@Composable
private fun SeekBar(fraction: Float, onSeeking: (Float) -> Unit, onSeek: (Float) -> Unit) {
    var width by remember { mutableIntStateOf(1) }
    Box(
        Modifier.fillMaxWidth().height(28.dp).onSizeChanged { width = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                var at = 0f
                detectHorizontalDragGestures(
                    onDragStart = { at = (it.x / width).coerceIn(0f, 1f); onSeeking(at) },
                    onDragEnd = { onSeek(at) },
                    onHorizontalDrag = { change, _ -> at = (change.position.x / width).coerceIn(0f, 1f); onSeeking(at) }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = .18f)))
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(Fusion.colors.accent))
        val knob = 14.dp
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Box(Modifier.offset(x = (maxWidth - knob) * fraction.coerceIn(0f, 1f)).size(knob).clip(CircleShape).background(Fusion.colors.accent))
        }
    }
}

/**
 * «Сейчас играет»: where it plays from, a big cover (swipe it to switch tracks), title with ♥, a thin
 * seek bar, shuffle / previous / play / next / repeat, and at the bottom where the sound goes, which
 * also opens the queue. ⋯ holds speed, sleep timer, ±10 s, playlists, the chat and deleting the file.
 */
@Composable
fun MusicPlayerSheet() {
    val open by MusicPlayerUi.isOpen.collectAsState()
    val track by VoicePlayer.track.collectAsState()
    if (!open) return
    val current = track ?: run { MusicPlayerUi.close(); return }
    val context = LocalContext.current
    val files = com.kirox.app.ui.components.LocalTelegramFiles.current
    val playingPath by VoicePlayer.playingPath.collectAsState()
    val loading by VoicePlayer.loading.collectAsState()
    val repeat by VoicePlayer.repeat.collectAsState()
    val shuffle by VoicePlayer.shuffle.collectAsState()
    val speed by VoicePlayer.speed.collectAsState()
    val sleepAt by VoicePlayer.sleepAt.collectAsState()
    val source by VoicePlayer.source.collectAsState()
    val queue by VoicePlayer.queue.collectAsState()
    val playlists by com.kirox.app.settings.Playlists.all.collectAsState()
    val (position, duration) = rememberPosition()
    var seeking by remember { mutableStateOf<Float?>(null) }
    var menu by remember { mutableStateOf(false) }
    var sleepMenu by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    // Outputs come and go (headphones connect); the list is read again every few seconds.
    var outputList by remember { mutableStateOf(outputs(context)) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(2500); outputList = outputs(context) } }
    val chosenOutput by VoicePlayer.output.collectAsState()
    val output = currentOutput(outputList, chosenOutput)
    var choosingOutput by remember { mutableStateOf(false) }
    val fromMessage = current.chatId != 0L
    val liked = fromMessage && playlists.firstOrNull { it.id == com.kirox.app.settings.Playlists.LIKED }?.has(current.chatId, current.messageId) == true
    val dark = Brush.verticalGradient(listOf(Color(0xFF16171E), Color(0xFF0B0B0F)))
    val muted = Color(0xFF9A9BA6)
    FusionFullscreen(onDismiss = MusicPlayerUi::close, onBack = { if (showQueue) showQueue = false else MusicPlayerUi.close() }) {
        Column(Modifier.fillMaxSize().background(dark).systemBarsPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerButton(Glyph.Down, "Свернуть", size = 44.dp, iconSize = 28.dp) { MusicPlayerUi.close() }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (showQueue) "ОЧЕРЕДЬ" else "ИГРАЕТ ИЗ", color = muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                    androidx.compose.material3.Text(
                        if (showQueue) "${(queue.indexOfFirst { it.file.id == current.file.id } + 1).coerceAtLeast(1)} / ${queue.size}" else source.ifBlank { com.kirox.app.i18n.I18n.t("Музыка") },
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    PlayerButton(Glyph.MoreHoriz, "Ещё", size = 44.dp) { menu = true }
                    androidx.compose.material3.DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = Color(0xFF1E1F27)) {
                        @Composable fun Item(label: String, color: Color = Color.White, onClick: () -> Unit) =
                            androidx.compose.material3.DropdownMenuItem(text = { Text(label, color = color) }, onClick = { menu = false; onClick() })
                        Item("Скорость: ${if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()}×") { VoicePlayer.cycleSpeed() }
                        Item(if (sleepAt > 0) "Таймер сна: ${((sleepAt - System.currentTimeMillis()) / 60_000 + 1).coerceAtLeast(1)} мин" else "Таймер сна") { sleepMenu = true }
                        Item("Назад на 10 секунд") { VoicePlayer.skipBy(-10_000) }
                        Item("Вперёд на 10 секунд") { VoicePlayer.skipBy(10_000) }
                        Item("Очередь") { showQueue = true }
                        if (fromMessage) Item("В плейлист…") { adding = true }
                        Item("Библиотека") { MusicPlayerUi.close(); com.kirox.app.ui.music.MusicLibraryUi.open() }
                        if (files != null) Item("Удалить с телефона", Fusion.colors.danger) { VoicePlayer.stop(); files.deleteLocal(current.file) }
                    }
                    androidx.compose.material3.DropdownMenu(sleepMenu, onDismissRequest = { sleepMenu = false }, containerColor = Color(0xFF1E1F27)) {
                        listOf(15, 30, 45, 60, 90).forEach { m ->
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Через $m мин", color = Color.White) }, onClick = { sleepMenu = false; VoicePlayer.sleepIn(m) })
                        }
                        if (sleepAt > 0) androidx.compose.material3.DropdownMenuItem(text = { Text("Выключить таймер", color = Fusion.colors.danger) }, onClick = { sleepMenu = false; VoicePlayer.sleepIn(0) })
                    }
                }
            }
            if (showQueue) QueueList(queue, current, Modifier.weight(1f))
            else {
                Spacer(Modifier.weight(.35f))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).swipeTracks(context), contentAlignment = Alignment.Center) {
                    AnimatedContent(current, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "cover") { t ->
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            CoverArt(t, maxWidth, 24.dp, modifier = Modifier.shadow(28.dp, RoundedCornerShape(24.dp)))
                        }
                    }
                    if (loading) CircularProgressIndicator(Modifier.size(48.dp), color = Color.White)
                }
                Spacer(Modifier.height(30.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        androidx.compose.material3.Text(current.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        androidx.compose.material3.Text(current.artist, color = muted, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (fromMessage) PlayerButton(if (liked) Glyph.Heart else Glyph.HeartOutline, if (liked) "Убрать из любимого" else "В любимое", iconSize = 28.dp, accent = liked, active = liked) {
                        com.kirox.app.settings.Playlists.toggleLiked(current.toItemForPlaylist())
                    }
                }
                Spacer(Modifier.height(18.dp))
                val fraction = seeking ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                SeekBar(fraction, onSeeking = { seeking = it }) { VoicePlayer.seekTo(it); seeking = null }
                Row(Modifier.fillMaxWidth()) {
                    Text(time((fraction * duration).toLong()), color = muted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("−" + time((duration - (fraction * duration).toLong()).coerceAtLeast(0)), color = muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    PlayerButton(Glyph.Shuffle, "Перемешать", accent = shuffle, active = shuffle) { VoicePlayer.toggleShuffle() }
                    PlayerButton(Glyph.SkipPrevious, "Предыдущий трек", size = 56.dp, iconSize = 36.dp) { VoicePlayer.previous(context) }
                    Box(Modifier.size(76.dp).clip(CircleShape).background(Color(0xFF2A2B34)).clickable { VoicePlayer.playPause() }, contentAlignment = Alignment.Center) {
                        KiroXIcon(if (playingPath != null) Glyph.Pause else Glyph.Play, if (playingPath != null) "Пауза" else "Играть", Modifier.size(40.dp), Color.White)
                    }
                    PlayerButton(Glyph.SkipNext, "Следующий трек", size = 56.dp, iconSize = 36.dp) { VoicePlayer.next(context) }
                    PlayerButton(if (repeat == RepeatMode.One) Glyph.RepeatOne else Glyph.Repeat, "Повтор", accent = repeat != RepeatMode.Off, active = repeat != RepeatMode.Off) { VoicePlayer.cycleRepeat() }
                }
                Spacer(Modifier.weight(1f))
            }
            // Where the sound goes; a tap chooses between the phone and connected headphones.
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(CircleShape).background(Color(0xFF1E1F27)).clickable { outputList = outputs(context); choosingOutput = true }.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headphones = output?.headphones == true
                KiroXIcon(if (headphones) Glyph.Headphones else Glyph.Speaker, null, Modifier.size(20.dp), if (headphones) Fusion.colors.accent else Color.White)
                Text("Слушаете на: ", color = muted, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                androidx.compose.material3.Text(com.kirox.app.i18n.I18n.t(output?.name ?: "Телефон"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (outputList.size > 1) Text("Сменить", color = Fusion.colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (adding) com.kirox.app.ui.music.AddToPlaylistDialog(current.toItemForPlaylist()) { adding = false }
        if (choosingOutput) OutputSheet(outputList, output) { picked -> choosingOutput = false; picked?.let { VoicePlayer.setOutput(it.device) } }
    }
}

/** «Куда выводить звук»: the phone and connected headphones; [onPick] gets the choice, or null when closed. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun OutputSheet(list: List<Output>, current: Output?, onPick: (Output?) -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = { onPick(null) }, containerColor = Color(0xFF1E1F27)) {
        Text("Куда выводить звук", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        list.forEach { o ->
            val selected = o.device.id == current?.device?.id
            Row(Modifier.fillMaxWidth().clickable { onPick(o) }.padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                KiroXIcon(if (o.headphones) Glyph.Headphones else Glyph.Phone, null, Modifier.size(24.dp), if (selected) Fusion.colors.accent else Color.White)
                androidx.compose.material3.Text(com.kirox.app.i18n.I18n.t(o.name), color = if (selected) Fusion.colors.accent else Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 16.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (selected) KiroXIcon(Glyph.Check, null, Modifier.size(22.dp), Fusion.colors.accent)
            }
        }
        if (list.size < 2) Text("Подключите Bluetooth-наушники или колонку — они появятся здесь.", color = Color(0xFF9A9BA6), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

/** The queue: covers, titles, the playing one marked; a tap plays that track. */
@Composable
private fun QueueList(queue: List<Track>, current: Track, modifier: Modifier) {
    val context = LocalContext.current
    val playingPath by VoicePlayer.playingPath.collectAsState()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = queue.indexOfFirst { it.file.id == current.file.id }.coerceAtLeast(0))
    LazyColumn(modifier.fillMaxWidth(), state = state, contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(queue, key = { _, t -> t.file.id }) { i, t ->
            val here = t.file.id == current.file.id
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (here) Color.White.copy(alpha = .08f) else Color.Transparent)
                    .clickable { VoicePlayer.playQueue(context, queue, i) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CoverArt(t, 46.dp, 10.dp, playing = here)
                    if (here) Box(Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
                        KiroXIcon(if (playingPath != null) Glyph.Pause else Glyph.Play, null, Modifier.size(22.dp), Color.White)
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    androidx.compose.material3.Text(t.title, color = if (here) Fusion.colors.accent else Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = if (here) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    androidx.compose.material3.Text(t.artist, color = Fusion.colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(time(t.durationSec * 1000L), color = Fusion.colors.textTertiary, fontSize = 12.sp)
            }
        }
    }
}

/**
 * A music file in a chat: the cover with play / pause (a ring while it downloads), title,
 * performer and length; the playing one shows its progress. A tap plays the chat's music from
 * here; a tap on the title of the playing one opens the player.
 */
@Composable
internal fun AudioMessage(media: TelegramMedia, cover: com.kirox.app.telegram.TelegramFileRef? = null) {
    val context = LocalContext.current
    val files = com.kirox.app.ui.components.LocalTelegramFiles.current
    val state by remember(media.file.id, files) {
        files?.state(media.file) ?: MutableStateFlow(com.kirox.app.telegram.TelegramFileState(path = media.file.localPath))
    }.collectAsState()
    val playingPath by VoicePlayer.playingPath.collectAsState()
    val current by VoicePlayer.track.collectAsState()
    val chatTracks = LocalChatTracks.current
    val chatTitle = LocalChatTitle.current
    val isCurrent = current?.file?.id == media.file.id
    val playing = isCurrent && playingPath != null
    val track = remember(media, cover) { media.toTrack().copy(cover = cover) }
    Column(Modifier.widthIn(min = 220.dp, max = 280.dp)) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                if (state.downloading && !isCurrent && current == null) files?.cancel(media.file)
                else {
                    val queue = chatTracks.ifEmpty { listOf(track) }
                    val index = queue.indexOfFirst { it.file.id == media.file.id }
                    if (index >= 0) VoicePlayer.playQueue(context, queue, index, chatTitle) else VoicePlayer.playQueue(context, listOf(track), 0, chatTitle)
                }
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CoverArt(track, 48.dp, 12.dp, playing = isCurrent)
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .35f)), contentAlignment = Alignment.Center) {
                    when {
                        state.downloading && !isCurrent -> CircularProgressIndicator(progress = { state.fraction.coerceAtLeast(0.02f) }, modifier = Modifier.size(34.dp), color = Color.White, strokeWidth = 2.dp)
                        else -> KiroXIcon(if (playing) Glyph.Pause else Glyph.Play, if (playing) "Пауза" else "Играть", Modifier.size(26.dp), Color.White)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).then(if (isCurrent) Modifier.clickable { MusicPlayerUi.open() } else Modifier)) {
                androidx.compose.material3.Text(media.title.ifBlank { "Аудио" }, color = if (isCurrent) Fusion.colors.accent else Fusion.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val details = listOf(media.performer, time(media.duration * 1000L)).filter { it.isNotBlank() }.joinToString(" · ")
                androidx.compose.material3.Text(details, color = Fusion.colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (isCurrent) {
            val (position, duration) = rememberPosition()
            Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Fusion.colors.surface3)) {
                Box(Modifier.fillMaxWidth(if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f).fillMaxHeight().background(Fusion.colors.accent))
            }
        }
    }
}

internal fun Track.toItemForPlaylist() = com.kirox.app.settings.PlaylistItem(chatId, messageId, title, artist, durationSec)
