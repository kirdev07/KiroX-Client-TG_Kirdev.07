package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * music: the full player (board «Музыка», the expanded player) as its own screen, so in the Music tab
 * it covers the dock. Play / pause, previous / next, a draggable seek bar, shuffle, repeat off/all/one,
 * ♡ = «Моя музыка», queue (reorder / remove), speed, sleep timer, output with Bluetooth battery and volume.
 */

private val SleepChoices = listOf(15, 30, 45, 60, 90)
private val SpeedChoices = listOf(.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private fun speedText(s: Float) = (if (s % 1f == 0f) s.toInt().toString() else s.toString().trimEnd('0')).replace('.', ',') + "×"

@Composable
fun MusicPlayerScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val music = store.music
    val p = store.player
    val t = p.track
    // Nothing loaded (the queue ended with a voice note, the player was stopped): back to the library.
    LaunchedEffect(t == null) { if (t == null) { delay(300); if (store.player.track == null) ctx.back() } }
    if (t == null) { Box(Modifier.fillMaxSize().background(Kx.bg)); return }
    DisposableEffect(Unit) { music.watchOutput(true); onDispose { music.watchOutput(false) } }

    var sheet by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()   // "", queue, sleep, speed, output, more, playlist
    val mine = music.isMine(t.chatId, t.messageId)
    val (c1, c2) = coverColors(t.messageId + t.chatId)
    val coverScale by animateFloatAsState(if (p.playing) 1f else .92f, tween(500, easing = EaseOutBack), label = "cover")
    val heart = remember { Animatable(1f) }
    LaunchedEffect(mine) { if (mine) heart.animateTo(1f, keyframes { durationMillis = 400; 1.35f at 160 using Ease; 1f at 400 using Ease }) }
    val dim = Color.White.copy(alpha = .7f)

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().cssGrad(180f, 0f to c2.copy(alpha = 1f).compositeOverBlack(c1), .75f to Kx.bg).statusBarsPadding().navigationBarsPadding()
            .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Rounded.KeyboardArrowDown, "Свернуть", bg = Kx.w(.08f), border = null) { ctx.back() }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Text("ИГРАЕТ ИЗ", style = ts(11.sp, color = Color.White.copy(alpha = .65f)).copy(letterSpacing = 1.sp))
                    T(p.source.ifBlank { t.chatTitle.ifBlank { "Музыка" } }, 14.sp, FontWeight.Bold, maxLines = 1)
                }
                MRoundBtn(Icons.Rounded.MoreHoriz, "Ещё", bg = Kx.w(.08f), border = null) { sheet = "more" }
            }
            // Cover: as big as the screen allows (small phones keep the controls visible).
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight - 24.dp).coerceIn(140.dp, 340.dp)
                Box(Modifier.size(side).graphicsLayer { scaleX = coverScale; scaleY = coverScale }
                    .shadow(60.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black.copy(alpha = .55f), spotColor = Color.Black.copy(alpha = .55f))) {
                    TrackCover(t, side, 34.dp, image = music.cover)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    T(t.title, 22.sp, FontWeight.ExtraBold, maxLines = 1)
                    T(t.performer.ifBlank { t.chatTitle }, 15.sp, color = dim, maxLines = 1)
                }
                Box(Modifier.size(46.dp).clip(CircleShape).background(Kx.w(.08f)).kxToggleState(mine)
                    .kxClick(if (mine) "Убрать из «Моя музыка»" else "В «Моя музыка»") {
                        music.toggleMine(t); ctx.toast(if (mine) "Убрано из «Моя музыка»" else "Добавлено в «Моя музыка»")
                    }, contentAlignment = Alignment.Center) {
                    Icon(if (mine) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, null,
                        Modifier.size(24.dp).graphicsLayer { scaleX = heart.value; scaleY = heart.value }, tint = if (mine) Color(0xFFFF8A9A) else Color.White)
                }
            }
            SeekBar(p.positionMs, p.durationMs, dim, onSeek = { music.seekMs(it) })
            Row(Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).kxToggleState(p.shuffle).kxClick("Перемешать") { store.playerShuffle() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Shuffle, null, tint = if (p.shuffle) Kx.accent else dim)
                }
                Box(Modifier.size(52.dp).kxClick("Предыдущий трек") { store.playerPrevious() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.SkipPrevious, null, Modifier.size(36.dp), tint = Color.White)
                }
                val playScale by animateFloatAsState(if (p.playing) 1f else .94f, tween(250, easing = CubicBezierEasing(.34f, 1.5f, .5f, 1f)), label = "play")
                Box(Modifier.size(76.dp).graphicsLayer { scaleX = playScale; scaleY = playScale }
                    .shadow(30.dp, CircleShape, ambientColor = Color.Black.copy(alpha = .4f), spotColor = Color.Black.copy(alpha = .4f))
                    .clip(CircleShape).background(Color.White).kxClick(if (p.playing) "Пауза" else "Играть") { store.playerToggle() }, contentAlignment = Alignment.Center) {
                    if (p.loading) CircularProgressIndicator(Modifier.size(30.dp), color = Kx.bg, strokeWidth = 3.dp)
                    else Icon(if (p.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(40.dp), tint = Kx.bg)
                }
                Box(Modifier.size(52.dp).kxClick("Следующий трек") { store.playerNext() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.SkipNext, null, Modifier.size(36.dp), tint = Color.White)
                }
                val repeatLabel = when (p.repeat) { 1 -> "Повтор: все"; 2 -> "Повтор: один трек"; else -> "Повтор выключен" }
                Box(Modifier.size(44.dp).kxClick(repeatLabel) { store.playerRepeat() }, contentAlignment = Alignment.Center) {
                    Icon(if (p.repeat == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, null, tint = if (p.repeat != 0) Kx.accent else dim)
                }
            }
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val out = music.output
                val route = out.current
                Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.10f)).kxClick("Куда выводить звук") { sheet = "output" }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(routeIcon(route?.kind ?: 0), null, Modifier.size(18.dp), tint = if ((route?.kind ?: 0) != 0) Kx.accent else Kx.text)
                    T(route?.name ?: "Телефон", 13.sp, FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    if (route != null && route.battery >= 0) BatteryBadge(route.battery)
                }
                Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (music.speed != 1f) Kx.accent.copy(alpha = .22f) else Kx.w(.10f))
                    .kxClick("Скорость ${speedText(music.speed)}") { sheet = "speed" }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    T(speedText(music.speed), 13.sp, FontWeight.Bold, if (music.speed != 1f) Kx.accent else Kx.text)
                }
                MRoundBtn(Icons.Outlined.Bedtime, if (music.sleepAt > 0) "Таймер сна включён" else "Таймер сна", size = 40.dp, iconSize = 20.dp,
                    bg = if (music.sleepAt > 0) Kx.accent.copy(alpha = .22f) else Kx.w(.10f), border = null, tint = if (music.sleepAt > 0) Kx.accent else Kx.text) { sheet = "sleep" }
                MRoundBtn(Icons.AutoMirrored.Outlined.QueueMusic, "Очередь", size = 40.dp, iconSize = 20.dp, bg = Kx.w(.10f), border = null) { sheet = "queue" }
            }
        }

        MSheet(sheet == "more", onClose = { sheet = "" }) {
            MoreRow(Icons.AutoMirrored.Outlined.PlaylistAdd, "Добавить в плейлист") { sheet = "playlist" }
            when (music.offlineState(t.chatId, t.messageId)) {
                0, 5 -> MoreRow(Icons.Outlined.DownloadForOffline, "Скачать для офлайна") { music.downloadOffline(listOf(t)); sheet = ""; ctx.toast("Скачиваем для офлайна") }
                4 -> MoreRow(Icons.Rounded.DownloadDone, "Скачан — играет без интернета", Kx.accent) { sheet = "" }
                else -> MoreRow(Icons.Outlined.Downloading, "Скачивается… (отменить)") { music.cancelOffline(t.chatId, t.messageId); sheet = "" }
            }
            MoreRow(Icons.Outlined.LibraryMusic, "Сохранить в Музыку телефона") {
                sheet = ""; ctx.toast("Сохраняем в Музыку телефона…")
                scope.launch { ctx.toast(music.exportToPhone(listOf(t), null) { _, _ -> }) }
            }
            MoreRow(Icons.Outlined.Speed, "Скорость: ${speedText(music.speed)}") { sheet = "speed" }
            MoreRow(Icons.Outlined.Bedtime, if (music.sleepAt > 0) "Таймер сна: ${sleepLeft(music.sleepAt)}" else "Таймер сна") { sheet = "sleep" }
            MoreRow(Icons.AutoMirrored.Outlined.QueueMusic, "Очередь") { sheet = "queue" }
            if (t.chatId != 0L) MoreRow(Icons.Outlined.ChatBubbleOutline, "Открыть чат") { sheet = ""; ctx.openChat(t.chatId) }
        }
        MSheet(sheet == "playlist", onClose = { sheet = "" }) {
            T("Добавить в плейлист", 17.sp, FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
            val lists = music.playlists.filterNot { it.mine }
            if (lists.isEmpty()) T("Плейлистов пока нет — создайте его в разделе «Плейлисты»", 13.sp, color = Kx.text4, modifier = Modifier.padding(8.dp))
            lists.forEach { pl ->
                val has = pl.tracks.any { it.chatId == t.chatId && it.messageId == t.messageId }
                MoreRow(if (has) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Outlined.QueueMusic, pl.name) {
                    if (!has) music.addToPlaylist(pl.id, t)
                    sheet = ""; ctx.toast(if (has) "Уже в «${pl.name}»" else "Добавлено в «${pl.name}»")
                }
            }
        }
        MSheet(sheet == "speed", onClose = { sheet = "" }) {
            T("Скорость", 17.sp, FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpeedChoices.forEach { s ->
                    val on = s == music.speed
                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent else Kx.w(.08f)).kxSelectedState(on)
                        .kxClick(speedText(s)) { music.setSpeed(s); sheet = "" }, contentAlignment = Alignment.Center) {
                        T(speedText(s), 14.sp, FontWeight.Bold, if (on) Kx.onAccent else Kx.text)
                    }
                }
            }
        }
        MSheet(sheet == "sleep", onClose = { sheet = "" }) {
            T("Таймер сна", 17.sp, FontWeight.ExtraBold)
            T(if (music.sleepAt > 0) "Музыка остановится через ${sleepLeft(music.sleepAt)}" else "Музыка остановится сама", 13.sp, color = Kx.text4, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            SleepChoices.forEach { m -> MoreRow(Icons.Outlined.Timer, "Через $m мин") { music.sleepIn(m); sheet = ""; ctx.toast("Таймер сна: $m мин") } }
            if (music.sleepAt > 0) MoreRow(Icons.Outlined.TimerOff, "Выключить таймер", Kx.danger) { music.sleepIn(0); sheet = "" }
        }
        MSheet(sheet == "queue", onClose = { sheet = "" }) { QueueSheet(store) }
        OutputSheet(sheet == "output", music, onClose = { sheet = "" })
    }
}

private fun Color.compositeOverBlack(tint: Color): Color = Color(
    red = red * .55f + tint.red * .45f, green = green * .55f + tint.green * .45f, blue = blue * .55f + tint.blue * .45f, alpha = 1f)

private fun sleepLeft(at: Long): String {
    val min = ((at - System.currentTimeMillis()) / 60_000L + 1).coerceAtLeast(1)
    return "$min мин"
}

private fun routeIcon(kind: Int) = when (kind) {
    1 -> Icons.Rounded.Headphones
    2 -> Icons.Rounded.BluetoothAudio
    3 -> Icons.Rounded.Usb
    else -> Icons.Rounded.PhoneAndroid
}

@Composable
private fun BatteryBadge(level: Int) {
    val low = level <= 20
    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(if (low) Color(0x33E5534B) else Kx.w(.10f)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(if (low) Icons.Rounded.BatteryAlert else Icons.Rounded.BatteryFull, null, Modifier.size(14.dp), tint = if (low) Color(0xFFFF8A80) else Kx.text2)
        T("$level%", 11.sp, FontWeight.Bold, if (low) Color(0xFFFF8A80) else Kx.text2)
    }
}

@Composable
private fun MoreRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color = Kx.text, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).kxClick(text, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = color); T(text, 15.sp, FontWeight.SemiBold, color, maxLines = 1)
    }
}

/** Thin seek bar with a knob: tap to jump, drag to scrub (the time follows the finger, the jump happens on release). */
@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, dim: Color, onSeek: (Long) -> Unit) {
    var width by remember { mutableIntStateOf(1) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    val p = dragging ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Column(Modifier.padding(top = 18.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(28.dp).onSizeChanged { width = it.width.coerceAtLeast(1) }
            .pointerInput(durationMs) { detectTapGestures { o -> if (durationMs > 0) onSeek((durationMs * (o.x / width).coerceIn(0f, 1f)).toLong()) } }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragging = (o.x / width).coerceIn(0f, 1f) },
                    onDragEnd = { dragging?.let { if (durationMs > 0) onSeek((durationMs * it).toLong()) }; dragging = null },
                    onDragCancel = { dragging = null },
                ) { change, dx -> change.consume(); dragging = ((dragging ?: 0f) + dx / width).coerceIn(0f, 1f) }
            }
            .seekSemantics(p, durationMs, onSeek), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(if (dragging != null) 8.dp else 6.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = .18f))) {
                Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color.White))
            }
            val knob = if (dragging != null) 18.dp else 14.dp
            Box(Modifier.offset(maxWidth * p - knob / 2).size(knob).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White))
        }
        Row {
            T(fmtTime(((dragging?.let { it * durationMs } ?: positionMs.toFloat()) / 1000).toInt()), 12.sp, color = dim, modifier = Modifier.weight(1f))
            T(if (durationMs > 0) fmtTime((durationMs / 1000).toInt()) else "–:––", 12.sp, color = dim)
        }
    }
}

/** Seek bar for TalkBack: a progress value it can set. */
private fun Modifier.seekSemantics(fraction: Float, durationMs: Long, onSeek: (Long) -> Unit): Modifier = this.semantics {
    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
    setProgress("Перемотка") { v -> if (durationMs > 0) onSeek((durationMs * v.coerceIn(0f, 1f)).toLong()); true }
}

private fun fmtTime(s: Int): String = if (s >= 3600) "${s / 3600}:${(s / 60 % 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}" else fmtSec(s)

/** «Очередь»: the play order, the current track marked; tap plays, ↑↓ move, × removes. */
@Composable
private fun QueueSheet(store: KxStore) {
    val music = store.music
    val q = store.player.queue
    val cur = music.queueIndex
    T("Очередь", 17.sp, FontWeight.ExtraBold)
    T("${q.size} ${musicPlural(q.size)}${if (store.player.shuffle) " · перемешано" else ""}", 13.sp, color = Kx.text4, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (cur - 1).coerceAtLeast(0))
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), state = state) {
        items(q.size, key = { "q:$it:${q[it].chatId}:${q[it].messageId}" }) { i ->
            val t = q[i]
            val on = i == cur
            Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .10f) else Color.Transparent)
                .kxClick("${t.title}, ${t.performer}") { if (!on) music.playQueueAt(i) }.padding(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TrackCover(t, 40.dp, 10.dp) { if (on) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .35f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.GraphicEq, null, Modifier.size(20.dp), tint = Color.White) } }
                Column(Modifier.weight(1f)) {
                    T(t.title, 14.sp, FontWeight.SemiBold, if (on) Kx.accent else if (i < cur) Kx.text3 else Kx.text, maxLines = 1)
                    T(t.performer.ifBlank { t.chatTitle }, 12.sp, color = Kx.text4, maxLines = 1)
                }
                MRoundBtn(Icons.Rounded.KeyboardArrowUp, "Выше", size = 34.dp, iconSize = 20.dp, bg = Color.Transparent, border = null, tint = if (i > 0) Kx.text2 else Kx.text4) { if (i > 0) music.moveInQueue(i, i - 1) }
                MRoundBtn(Icons.Rounded.KeyboardArrowDown, "Ниже", size = 34.dp, iconSize = 20.dp, bg = Color.Transparent, border = null, tint = if (i < q.lastIndex) Kx.text2 else Kx.text4) { if (i < q.lastIndex) music.moveInQueue(i, i + 1) }
                MRoundBtn(Icons.Rounded.Close, "Убрать из очереди", size = 34.dp, iconSize = 18.dp, bg = Color.Transparent, border = null, tint = Kx.text3) { music.removeFromQueue(i) }
            }
        }
    }
}

/** Where the sound goes: Android's output switcher, the outputs with battery, and the media volume. */
@Composable
private fun OutputSheet(visible: Boolean, music: KxMusic, onClose: () -> Unit) {
    val context = LocalContext.current
    val askBt = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { music.refreshOutput() }
    val out = music.output
    // BLUETOOTH_CONNECT only when this sheet opens (Android 12+): names and battery of the headphones.
    LaunchedEffect(visible) {
        if (visible) {
            music.refreshOutput()
            if (android.os.Build.VERSION.SDK_INT >= 31 && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) runCatching { askBt.launch(android.Manifest.permission.BLUETOOTH_CONNECT) }
        }
    }
    MSheet(visible, onClose) {
        T("Вывод звука", 17.sp, FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
        out.routes.forEach { r ->
            val on = out.current?.id == r.id
            Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .12f) else Color.Transparent)
                .kxSelectedState(on).kxClick(r.name) { music.pickOutput(if (r.kind == 0 && out.routes.size == 1) null else r.id) }.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(routeIcon(r.kind), null, Modifier.size(22.dp), tint = if (on) Kx.accent else Kx.text2)
                Column(Modifier.weight(1f)) {
                    T(r.name, 15.sp, FontWeight.SemiBold, if (on) Kx.accent else Kx.text, maxLines = 1)
                    val kind = when (r.kind) { 1 -> "Проводные"; 2 -> "Bluetooth"; 3 -> "USB"; else -> "Динамик телефона" }
                    T(if (r.battery >= 0) "$kind · заряд ${r.battery}%" else kind, 12.sp, color = Kx.text4)
                }
                if (r.battery >= 0) BatteryBadge(r.battery)
                if (on) Icon(Icons.Rounded.Check, null, Modifier.size(20.dp), tint = Kx.accent)
            }
        }
        if (out.chosen != null) MoreRow(Icons.Outlined.AutoMode, "Как решит система") { music.pickOutput(null) }
        if (out.needsBluetoothPermission) MoreRow(Icons.Outlined.Bluetooth, "Разрешить доступ к Bluetooth — имя и заряд наушников") {
            runCatching { askBt.launch(android.Manifest.permission.BLUETOOTH_CONNECT) }
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) MoreRow(Icons.Outlined.Cast, "Другие устройства…") {
            if (!music.openSystemOutputSwitcher(context)) music.refreshOutput()
        }
        Row(Modifier.padding(top = 10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (out.volume == 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(22.dp), tint = Kx.text2)
            // Follows the volume keys (the store listens to the system volume while the player is open).
            Slider(out.volume.toFloat(), { music.setVolume(it.toInt()) }, Modifier.weight(1f), valueRange = 0f..out.maxVolume.coerceAtLeast(1).toFloat(),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Kx.accent, inactiveTrackColor = Kx.w(.15f), activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent))
            T("${if (out.maxVolume > 0) out.volume * 100 / out.maxVolume else 0}%", 12.sp, color = Kx.text3, modifier = Modifier.widthIn(min = 36.dp))
        }
    }
}
