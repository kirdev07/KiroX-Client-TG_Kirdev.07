package com.kirox.app.ui.v3.screens.media

import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxMediaItem
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * wave3-media: the Viewer / Album / VideoPlayer boards on the real account. ctx.arg = "chatId:messageId"
 * (ChatScreen opens them from a photo/video bubble). The demo boards stay in ViewerScreen / AlbumScreen /
 * VideoPlayerScreen for KxDemoStore.
 */

internal fun mediaArg(arg: String?): Pair<Long, Long>? {
    val chat = arg?.substringBefore(':')?.toLongOrNull() ?: return null
    return chat to (arg.substringAfter(':', "").toLongOrNull() ?: 0L)
}

@Composable
private fun RealEmpty(text: String, sub: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        MRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Закрыть", Modifier.statusBarsPadding().padding(12.dp), size = 42.dp, bg = Kx.w(.1f), border = null) { onBack() }
        Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(48.dp), tint = Kx.text4)
            T(text, 16.sp, FontWeight.Bold)
            T(sub, 13.sp, color = Kx.text3)
        }
    }
}

@Composable
private fun RealLoading() {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Kx.accent) }
}

/** Waits until a file is on the phone, starting its download. */
private suspend fun ensureFile(ctx: KxScreenCtx, fileId: Int): Boolean {
    if (ctx.store.fileState(fileId).path != null) return true
    ctx.store.download(fileId)
    repeat(240) { delay(250); if (ctx.store.fileState(fileId).path != null) return true }
    return false
}

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RealViewer(ctx: KxScreenCtx) {
    val arg = mediaArg(ctx.arg)
    var items by remember { mutableStateOf<List<KxMediaItem>?>(null) }
    LaunchedEffect(ctx.arg) { items = if (arg == null) emptyList() else ctx.store.chatMedia(arg.first, arg.second) }
    val list = items
    when {
        list == null -> RealLoading()
        list.isEmpty() -> RealEmpty("Нет фото и видео", "В этом чате пока нечего показать", ctx.back)
        else -> RealViewerPages(ctx, list, list.indexOfFirst { it.messageId == arg?.second }.coerceAtLeast(0)) { gone -> items = list.filter { it.messageId != gone } }
    }
}

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RealViewerPages(ctx: KxScreenCtx, list: List<KxMediaItem>, start: Int, onDeleted: (Long) -> Unit) {
    val pager = rememberPagerState(start) { list.size }
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf(true) }
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    val toast = remember { MToastState() }
    val i = pager.currentPage.coerceIn(0, list.lastIndex)
    val m = list[i]
    val strip = rememberLazyListState()
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    LaunchedEffect(i) { strip.animateScrollToItem((i - 3).coerceAtLeast(0)) }
    fun say(t: String) { menu = false; toast.show(t) }
    fun save(item: KxMediaItem) = scope.launch {
        if (!ensureFile(ctx, item.fileId)) { say("Файл не скачался"); return@launch }
        say(ctx.store.saveToGallery(item.fileId, item.video) ?: "Сохранено в галерею")
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), key = { list[it].messageId }) { k ->
            val p = list[k]
            Box(Modifier.fillMaxSize().kxClick("Показать или скрыть панели") { ui = !ui }, contentAlignment = Alignment.Center) {
                if (!p.video && p.media == null) LaunchedEffect(p.fileId) { ctx.store.download(p.fileId) }
                KxImage(if (p.video) p.thumb else (p.media ?: p.thumb), Modifier.fillMaxSize(), maxPx = 2048, contentScale = ContentScale.Fit)
                // fixer-chat4: a video plays right here (streamed / local file), the current page only; a tap still toggles the bars.
                if (p.video && k == pager.currentPage) InlineVideo(ctx, p, onTap = { ui = !ui }, onPlayer = { player = it })
                else if (p.video) Box(Modifier.size(72.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f)).border(1.dp, Kx.w(.25f), CircleShape)
                    .kxClick("Смотреть") { scope.launch { pager.animateScrollToPage(k) } }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(40.dp), tint = Color.White)
                }
            }
        }
        // fixer-chat4: dark scrims behind the top and bottom bars (Telegram) so the bars never collide with the picture's own text.
        if (ui) Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = .85f), .6f to Color.Black.copy(alpha = .55f), 1f to Color.Transparent)).statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Закрыть", size = 42.dp, bg = Kx.w(.1f), border = null) { ctx.back() }
            Column(Modifier.weight(1f)) {
                T(m.sender.ifBlank { "Медиа" }, 15.sp, FontWeight.Bold, maxLines = 1)
                T(m.time, 12.sp, color = Kx.text2)
            }
            T("${i + 1} из ${list.size}", 13.sp, color = Kx.text2, modifier = Modifier.padding(end = 4.dp))
            MRoundBtn(Icons.Outlined.MoreVert, "Ещё", size = 42.dp, bg = Color.Transparent, border = null) { menu = true }
        }
        if (ui) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Color.Transparent, .35f to Color.Black.copy(alpha = .6f), 1f to Color.Black.copy(alpha = .9f)))
            .navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 40.dp, bottom = 22.dp)) {
            val vp = player
            if (m.video && vp != null) VideoSeekRow(vp)
            if (m.caption.isNotEmpty()) Text(m.caption, style = ts(15.sp).copy(lineHeight = 21.sp), maxLines = 4, modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 12.dp))
            LazyRow(Modifier.fillMaxWidth().padding(bottom = 14.dp), state = strip, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(list, key = { _, it -> it.messageId }) { k, p ->
                    Box(Modifier.size(if (k == i) 64.dp else 34.dp, 44.dp).clip(RoundedCornerShape(10.dp)).background(Kx.w(.1f))
                        .border(2.dp, if (k == i) Kx.accent else Color.Transparent, RoundedCornerShape(10.dp))
                        .kxClick("Медиа ${k + 1}") { scope.launch { pager.animateScrollToPage(k) } }, contentAlignment = Alignment.Center) {
                        KxImage(p.thumb ?: p.media.takeIf { !p.video }, Modifier.matchParentSize(), maxPx = 160)
                        if (p.video) Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp), tint = Color.White)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(29.dp)).background(Kx.surface.copy(alpha = .6f)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(29.dp)).topHighlight(Kx.w(.18f)), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                RAct(Icons.Outlined.FavoriteBorder, "Реакция ❤") {
                    // wave5: only a reaction the chat allows (❤ when it can); failures come back as the store error, not a fake «поставлена».
                    val allowed = ctx.store.availableReactions(m.chatId, m.messageId)
                    val emoji = if ("❤" in allowed || allowed.isEmpty() && ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) "❤" else allowed.firstOrNull()
                    if (emoji == null) toast.show("Реакции в этом чате выключены") else { ctx.store.react(m.chatId, m.messageId, emoji); toast.show(if (emoji.startsWith("ce:")) "Реакция поставлена" else "Реакция $emoji") }
                }
                RAct(Icons.AutoMirrored.Outlined.Chat, "Показать в чате") { ctx.store.jumpTo(m.chatId, m.messageId); ctx.back() }
                RAct(Icons.AutoMirrored.Outlined.Forward, "Переслать", m.canForward) { ctx.open("Forward:${m.chatId}:${m.messageId}") }
                RAct(Icons.Outlined.Download, "Сохранить", m.canSave) { save(m) }
                RAct(Icons.Outlined.Share, "Поделиться", m.canSave) { scope.launch { if (ensureFile(ctx, m.fileId)) ctx.store.shareFile(m.fileId)?.let { say(it) } else say("Файл не скачался") } }
            }
        }
        MSheet(menu, { menu = false }, dim = .5f, top = Color(0xFF121614), bottom = Color(0xFF121614), border = Kx.w(.08f), highlight = null,
            handleColor = Color(0xFF3A4540), handleWidth = 36.dp, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp)) {
            MenuItem(Icons.AutoMirrored.Outlined.Chat, "Показать в чате", Kx.text) { menu = false; ctx.store.jumpTo(m.chatId, m.messageId); ctx.back() }
            // fixer-chat4: «Весь альбом» = this album; «Все медиа чата» = this chat's media grid (SharedMedia), not the phone's Медиатека.
            if (m.albumId != 0L) MenuItem(Icons.Outlined.PhotoLibrary, "Весь альбом", Kx.text) { menu = false; ctx.open("Album:${m.chatId}:${m.messageId}") }
            MenuItem(Icons.Outlined.GridView, "Все медиа чата", Kx.text) { menu = false; ctx.open("SharedMedia:${m.chatId}") }
            fun askDelete(forAll: Boolean) {
                menu = false
                confirm = KxConfirm(if (m.video) "Удалить видео?" else "Удалить фото?",
                    if (forAll) "Сообщение с этим медиа удалится из чата у всех." else "Сообщение удалится только у вас.", "Удалить") {
                    ctx.store.delete(m.chatId, listOf(m.messageId), forAll)
                    onDeleted(m.messageId)
                }
            }
            if (m.canDelete) MenuItem(Icons.Outlined.Delete, if (m.canDeleteForAll) "Удалить у меня" else "Удалить", Kx.danger) { askDelete(false) }
            if (m.canDeleteForAll) MenuItem(Icons.Outlined.DeleteForever, "Удалить у всех", Kx.danger) { askDelete(true) }
        }
        MToastHost(toast, top = 80.dp)
        confirm?.let { KxConfirmDialog(it) { confirm = null } }
    }
}

/** fixer-chat4: the viewer's video page — ExoPlayer under a transparent tap layer (bars), a centre play button when paused. */
@OptIn(UnstableApi::class)
@Composable
private fun InlineVideo(ctx: KxScreenCtx, p: KxMediaItem, onTap: () -> Unit, onPlayer: (ExoPlayer?) -> Unit) {
    val player = rememberTgPlayer(p.fileId, ctx.store.fileState(p.fileId).path ?: p.media)
    DisposableEffect(player) { onPlayer(player); onDispose { onPlayer(null) } }
    var playing by remember { mutableStateOf(true) }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(player) {
        while (true) {
            playing = player.playWhenReady && player.playbackState != Player.STATE_ENDED
            if (player.playbackState == Player.STATE_READY) started = true
            delay(200)
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(factory = { c -> PlayerView(c).apply { useController = false; this.player = player; keepScreenOn = true; setShutterBackgroundColor(android.graphics.Color.TRANSPARENT) } },
            modifier = Modifier.fillMaxSize())
        Box(Modifier.matchParentSize().kxClick("Показать или скрыть панели", onClick = onTap))
        if (!started && playing) CircularProgressIndicator(color = Kx.accent)
        else if (!playing) Box(Modifier.size(72.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f)).border(1.dp, Kx.w(.25f), CircleShape)
            .kxClick("Смотреть") { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(40.dp), tint = Color.White)
        }
    }
}

/** fixer-chat4: play / pause, position / length and a tap-to-seek bar for the viewer's current video. */
@Composable
private fun VideoSeekRow(player: ExoPlayer) {
    var pos by remember(player) { mutableLongStateOf(0L) }
    var dur by remember(player) { mutableLongStateOf(0L) }
    var playing by remember(player) { mutableStateOf(true) }
    LaunchedEffect(player) {
        while (true) { pos = player.currentPosition.coerceAtLeast(0); if (player.duration > 0) dur = player.duration; playing = player.playWhenReady && player.playbackState != Player.STATE_ENDED; delay(250) }
    }
    fun mm(ms: Long) = "${ms / 60000}:${((ms / 1000) % 60).toString().padStart(2, '0')}"
    val f = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Row(Modifier.padding(start = 2.dp, end = 2.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.w(.12f)).kxClick(if (playing) "Пауза" else "Смотреть") {
            if (playing) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }
        }, contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(22.dp), tint = Color.White) }
        T(mm(pos), 12.sp, color = Kx.text2)
        BoxWithConstraints(Modifier.weight(1f).height(18.dp), contentAlignment = Alignment.CenterStart) {
            val w = maxWidth
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.2f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(Kx.accent))
            }
            Box(Modifier.offset(x = w * f - 6.dp).size(12.dp).clip(CircleShape).background(Kx.text))
            Row(Modifier.matchParentSize()) { repeat(20) { k -> Box(Modifier.weight(1f).fillMaxHeight().clickableNoRipple("Перейти") { if (dur > 0) player.seekTo((dur * (k + .5f) / 20f).toLong()) }) } }
        }
        T(mm(dur), 12.sp, color = Kx.text2)
    }
}

@Composable
private fun RAct(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(52.dp, 46.dp).clip(RoundedCornerShape(23.dp)).then(if (enabled) Modifier.kxClick(label, onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = if (enabled) Kx.text else Kx.text4)
    }
}

/** Album board on the real account: the album's photos and videos, select, save, forward to Избранное. */
@Composable
internal fun RealAlbum(ctx: KxScreenCtx) {
    val arg = mediaArg(ctx.arg)
    var items by remember { mutableStateOf<List<KxMediaItem>?>(null) }
    LaunchedEffect(ctx.arg) { items = if (arg == null || arg.second == 0L) emptyList() else ctx.store.albumOf(arg.first, arg.second) }
    val list = items
    if (list == null) { RealLoading(); return }
    if (list.isEmpty()) { RealEmpty("Альбом не найден", "Откройте альбом из чата", ctx.back); return }
    var selecting by remember { mutableStateOf(false) }
    val sel = remember { mutableStateListOf<Long>() }
    val toast = remember { MPillToastState() }
    val scope = rememberCoroutineScope()
    val chosen = if (selecting && sel.isNotEmpty()) list.filter { it.messageId in sel } else list
    // Protected chats («запрет копирования»): TDLib says per message what may be saved / forwarded.
    val savable = chosen.filter { it.canSave }
    val forwardable = chosen.filter { it.canForward }
    Box(Modifier.fillMaxSize().background(Color(0xFF050606))) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.08f), border = null) { ctx.back() }
                Column(Modifier.weight(1f)) {
                    T(if (selecting) (if (sel.isNotEmpty()) "Выбрано: ${sel.size}" else "Выберите фото") else list.first().caption.lineSequence().firstOrNull().orEmpty().ifBlank { "Альбом" }, 16.sp, FontWeight.ExtraBold, maxLines = 1)
                    T("Альбом · ${list.size} · ${list.first().time}", 12.sp, color = Kx.text3, maxLines = 1)
                }
                Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (selecting) Kx.accent.copy(alpha = .16f) else Kx.w(.08f))
                    .kxClick { selecting = !selecting; sel.clear() }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    T(if (selecting) "Готово" else "Выбрать", 13.sp, FontWeight.Bold, if (selecting) Kx.accent else Kx.text)
                }
            }
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(list, key = { _, it -> it.messageId }) { k, p ->
                    val on = p.messageId in sel
                    if (!p.video && p.media == null) LaunchedEffect(p.fileId) { ctx.store.download(p.fileId) }
                    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Kx.w(.06f))
                        .kxClick("Фото ${k + 1}" + if (on) ", выбрано" else "") {
                            if (selecting) { if (on) sel.remove(p.messageId) else sel.add(p.messageId) } else ctx.open("Viewer:${p.chatId}:${p.messageId}")
                        }) {
                        KxImage(p.thumb.takeIf { p.video } ?: p.media ?: p.thumb, Modifier.matchParentSize(), maxPx = 480)
                        if (selecting) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Black.copy(alpha = .25f))
                            .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Kx.onAccent) }
                        if (p.video) T(fmtSec(p.duration), 11.sp, FontWeight.Bold, Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                            .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Color.Transparent, .4f to Color(0xFF050606)))
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.w(.08f)).kxClick {
                if (forwardable.isEmpty()) toast.show("В этом чате пересылка запрещена", Icons.AutoMirrored.Outlined.Forward)
                else { ctx.store.saveToFavorites(forwardable.first().chatId, forwardable.map { it.messageId }); toast.show("Переслано в «Избранное»: ${forwardable.size}", Icons.AutoMirrored.Outlined.Forward) }
            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                val ink = if (forwardable.isEmpty()) Kx.text4 else Kx.text
                Icon(Icons.AutoMirrored.Outlined.Forward, null, Modifier.size(20.dp), tint = ink); T("В Избранное", 14.sp, FontWeight.Bold, ink)
            }
            Row(Modifier.weight(1.3f).height(50.dp).clip(RoundedCornerShape(25.dp)).then(if (savable.isEmpty()) Modifier.background(Kx.w(.06f)) else Modifier.cssGrad(135f, Kx.accentMid, Kx.accentDeep)).kxClick {
                if (savable.isEmpty()) { toast.show("В этом чате сохранение запрещено", Icons.Rounded.Download); return@kxClick }
                scope.launch {
                    var ok = 0
                    savable.forEach { p -> if (ensureFile(ctx, p.fileId) && ctx.store.saveToGallery(p.fileId, p.video) == null) ok++ }
                    toast.show(if (ok == chosen.size) "Сохранено в галерею: $ok" else "Сохранено $ok из ${chosen.size}", Icons.Rounded.DownloadDone)
                }
            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                val ink = if (savable.isEmpty()) Kx.text4 else Kx.onAccent
                Icon(Icons.Rounded.Download, null, Modifier.size(20.dp), tint = ink)
                T(if (selecting && sel.isNotEmpty()) "Сохранить ${sel.size}" else "Сохранить все ${list.size}", 14.sp, FontWeight.ExtraBold, ink)
            }
        }
        MPillToastHost(toast, bottom = 96.dp)
    }
}

/** ExoPlayer for a Telegram video: the local file when downloaded, else streamed by file id. */
@OptIn(UnstableApi::class)
@Composable
private fun rememberTgPlayer(fileId: Int, path: String?): ExoPlayer {
    val context = LocalContext.current
    val player = remember(fileId) {
        com.kirox.app.ui.chat.VoicePlayer.stop()
        ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(com.kirox.app.media.TelegramStream.mediaSourceFactory(context))
            // wave5: takes audio focus (music pauses, a call pauses the video) and stops sound when headphones are pulled out.
            .setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                val uri = path?.let { android.net.Uri.fromFile(java.io.File(it)) } ?: com.kirox.app.media.TelegramStream.uri(fileId)
                setMediaItem(MediaItem.fromUri(uri)); playWhenReady = true; prepare()
            }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) { player.pause() }
    return player
}

/** VideoPlayer board on the real account: the video message, chapters from its caption timecodes, speed. */
@Composable
internal fun RealVideoPlayer(ctx: KxScreenCtx) {
    val arg = mediaArg(ctx.arg)
    var item by remember { mutableStateOf<KxMediaItem?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(ctx.arg) {
        item = if (arg == null || arg.second == 0L) null else ctx.store.albumOf(arg.first, arg.second).firstOrNull { it.messageId == arg.second && it.video }
        loaded = true
    }
    val v = item
    if (!loaded) { RealLoading(); return }
    if (v == null) { RealEmpty("Видео не найдено", "Откройте видео из чата", ctx.back); return }
    RealVideoBody(ctx, v)
}

@OptIn(UnstableApi::class)
@Composable
private fun RealVideoBody(ctx: KxScreenCtx, v: KxMediaItem) {
    val player = rememberTgPlayer(v.fileId, ctx.store.fileState(v.fileId).path ?: v.media)
    val scope = rememberCoroutineScope()
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(v.duration * 1000L) }
    var playing by remember { mutableStateOf(true) }
    var sp by remember { mutableIntStateOf(1) }
    val speeds = listOf(.5f, 1f, 1.25f, 1.5f, 2f)
    LaunchedEffect(player) {
        while (true) {
            pos = player.currentPosition.coerceAtLeast(0); if (player.duration > 0) dur = player.duration
            playing = player.playWhenReady && player.playbackState != Player.STATE_ENDED
            delay(250)
        }
    }
    val chapters = v.chapters
    val t = pos / 1000f
    val total = (dur / 1000f).coerceAtLeast(1f)
    val ci = chapters.indexOfLast { t >= it.first }.coerceAtLeast(0)
    fun seek(sec: Float) { player.seekTo((sec.coerceIn(0f, total) * 1000).toLong()) }
    fun mm(n: Float) = "${(n / 60).toInt().toString().padStart(2, '0')}:${(n % 60).toInt().toString().padStart(2, '0')}"

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.08f), border = null) { ctx.back() }
            Column(Modifier.weight(1f)) { T(v.sender.ifBlank { "Видео" }, 15.sp, FontWeight.ExtraBold, maxLines = 1); T(v.time, 12.sp, color = Kx.text3) }
            MRoundBtn(Icons.Outlined.Download, "Сохранить в галерею", bg = Kx.w(.08f), border = null, tint = if (v.canSave) Kx.text else Kx.text4) {
                if (!v.canSave) { ctx.toast("В этом чате сохранение запрещено"); return@MRoundBtn }
                scope.launch {
                    if (ctx.store.fileState(v.fileId).path == null) ctx.toast("Скачиваю видео…")
                    ctx.toast(if (!ensureFile(ctx, v.fileId)) "Видео не скачалось" else ctx.store.saveToGallery(v.fileId, true) ?: "Сохранено в галерею")
                }
            }
        }
        Box(Modifier.padding(top = 30.dp).fillMaxWidth().height(230.dp).background(Color.Black)) {
            AndroidView(factory = { c -> PlayerView(c).apply { useController = false; this.player = player; keepScreenOn = true } }, modifier = Modifier.matchParentSize())
            Row(Modifier.matchParentSize()) {
                Box(Modifier.weight(.35f).fillMaxHeight().clickableNoRipple("Назад на 10 секунд") { seek(t - 10) })
                Box(Modifier.weight(.30f).fillMaxHeight().clickableNoRipple(if (playing) "Пауза" else "Смотреть") { if (playing) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() } })
                Box(Modifier.weight(.35f).fillMaxHeight().clickableNoRipple("Вперёд на 10 секунд") { seek(t + 10) })
            }
            if (!playing) Box(Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(36.dp), tint = Color.White)
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(if (chapters.isNotEmpty()) "Глава ${ci + 1} · ${chapters[ci].second}" else "", 13.sp, FontWeight.Bold, Kx.accent, modifier = Modifier.weight(1f), maxLines = 1)
                T("${mm(t)} / ${mm(total)}", 13.sp, FontWeight.Bold, Kx.text2)
            }
            BoxWithConstraints(Modifier.padding(top = 10.dp).fillMaxWidth().height(18.dp)) {
                val w = maxWidth
                Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(3.dp)).background(Kx.w(.18f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((t / total).coerceIn(0f, 1f)).background(Kx.accent))
                }
                chapters.drop(1).forEach { c -> Box(Modifier.align(Alignment.CenterStart).offset(x = w * (c.first / total).coerceIn(0f, 1f)).size(3.dp, 8.dp).background(Color.Black)) }
                Row(Modifier.matchParentSize()) { repeat(20) { k -> Box(Modifier.weight(1f).fillMaxHeight().clickableNoRipple("Перейти") { seek(total * (k + .5f) / 20f) }) } }
                Box(Modifier.offset(x = w * (t / total).coerceIn(0f, 1f) - 7.dp, y = 2.dp).size(14.dp).clip(CircleShape).background(Kx.text))
            }
            Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                VBtn(Icons.Rounded.SkipPrevious, "Предыдущая глава", chapters.isNotEmpty()) { seek(if (t - chapters[ci].first > 3) chapters[ci].first.toFloat() else chapters[maxOf(0, ci - 1)].first.toFloat()) }
                VBtn(Icons.Outlined.Replay10, "Назад на 10 секунд") { seek(t - 10) }
                Box(Modifier.size(64.dp).clip(CircleShape).cssGrad(135f, Kx.accentMid, Kx.accentDeep).kxClick(if (playing) "Пауза" else "Смотреть") {
                    if (playing) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }
                }, contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(34.dp), tint = Kx.onAccent) }
                VBtn(Icons.Outlined.Forward10, "Вперёд на 10 секунд") { seek(t + 10) }
                VBtn(Icons.Rounded.SkipNext, "Следующая глава", chapters.isNotEmpty()) { seek(if (ci < chapters.lastIndex) chapters[ci + 1].first.toFloat() else total) }
            }
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                speeds.forEachIndexed { k, s ->
                    val on = sp == k
                    Box(Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Kx.accent.copy(alpha = .2f) else Color.Transparent)
                        .kxClick { sp = k; player.setPlaybackSpeed(s) }, contentAlignment = Alignment.Center) {
                        T((if (s == s.toInt().toFloat()) s.toInt().toString() else s.toString()).replace('.', ',') + "×", 13.sp, FontWeight.ExtraBold, if (on) Kx.accent else Kx.text3)
                    }
                }
            }
        }
        Column(Modifier.padding(top = 14.dp).weight(1f).fillMaxWidth().background(Kx.bg)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.08f)))
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp)) {
                if (v.caption.isNotBlank() && chapters.isEmpty()) Text(v.caption, style = ts(14.sp, color = Color(0xFFDDE5E0)).copy(lineHeight = 20.3.sp))
                if (v.caption.isBlank()) T("Без подписи", 13.sp, color = Kx.text4)
                chapters.forEachIndexed { k, c ->
                    val on = k == ci
                    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Kx.accent.copy(alpha = .08f) else Color.Transparent)
                        .kxClick { seek(c.first.toFloat()) }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        T(mm(c.first.toFloat()), 13.sp, FontWeight.ExtraBold, Kx.accent)
                        T(c.second, 14.sp, if (on) FontWeight.ExtraBold else FontWeight.Medium, if (on) Kx.text else Kx.text2, modifier = Modifier.weight(1f))
                        if (on) Icon(Icons.Rounded.GraphicEq, null, Modifier.size(16.dp), tint = Kx.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun VBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).then(if (enabled) Modifier.kxClick(label, onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = if (enabled) Kx.text else Kx.text4)
    }
}
