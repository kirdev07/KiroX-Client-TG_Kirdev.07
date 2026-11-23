package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.model.KxStoryPeer
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

/** Demo story looks (board «Истории»): 160° gradient + a big faded icon. */
private val Looks: List<Triple<Color, Color, ImageVector>> = listOf(
    Triple(Color(0xFF6E58C9), Color(0xFF2A2240), Icons.Rounded.Theaters), Triple(Color(0xFFC97A58), Color(0xFF3A2014), Icons.Rounded.LocalCafe),
    Triple(Color(0xFF58A6C9), Color(0xFF14303A), Icons.Rounded.NightsStay), Triple(Color(0xFF3E7A5A), Color(0xFF0F1A14), Icons.Rounded.Terrain),
    Triple(Color(0xFF5A8A3E), Color(0xFF141F0F), Icons.Rounded.Forest), Triple(Color(0xFF2E8A6B), Color(0xFF0B2018), Icons.Rounded.RocketLaunch),
    Triple(Color(0xFF3C7BD0), Color(0xFF0F1E3A), Icons.Rounded.BeachAccess), Triple(Color(0xFFD08A3C), Color(0xFF3A220A), Icons.Rounded.SportsEsports),
)

private fun Modifier.look(i: Int) = drawBehind {
    val (a, b, _) = Looks[i % Looks.size]
    drawRect(Brush.linearGradient(0f to a, .7f to b, start = cssAngleStart(160f, size), end = cssAngleEnd(160f, size)))
}

/** Stories tab: «Моя история» + 2-column cards; tap opens the viewer. */
@Composable
fun StoriesScreen(store: KxStore, onOpenViewer: (Int) -> Unit, onOpen: (String) -> Unit, bottomPadding: Dp = 110.dp) {
    val peers = store.stories.filter { !it.mine && it.slides.isNotEmpty() }
    Box(Modifier.fillMaxSize()) {
        KxGlow(diameter = 380.dp, x = (-90).dp, y = (-110).dp)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Истории", style = KxType.style(30.sp, FontWeight.ExtraBold).copy(letterSpacing = (-.4).sp), modifier = Modifier.weight(1f))
                KxIconButton(Icons.Outlined.VisibilityOff, "Скрытые истории", size = 42.dp, glass = true) { onOpen("HiddenStories") }
                KxIconButton(Icons.Outlined.AddAPhoto, "Новая история", size = 42.dp, accent = true) { onOpen("StoryPost") }
            }
            LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(Modifier.height(214.dp).clip(RoundedCornerShape(24.dp))
                        .drawBehind { drawRect(Brush.linearGradient(listOf(Color(0xFF1E3A2A), Color(0xFF0F1A14)), cssAngleStart(160f, size), cssAngleEnd(160f, size))) }
                        .dashedBorder(Kx.accent.copy(alpha = .35f), 24.dp).kxClick("Добавить историю") { onOpen("StoryPost") },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                        Box(Modifier.size(58.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(30.dp), tint = Kx.accent)
                        }
                        Text("Моя история", style = KxType.style(15.sp, FontWeight.Bold))
                        Text("Фото или видео на 24 часа", style = KxType.style(12.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                itemsIndexed(peers, key = { _, p -> p.id }) { i, p -> StoryCard(p, Modifier.kxRise(i * 60, 500)) { onOpenViewer(i) } }
            }
        }
    }
}

@Composable
private fun StoryCard(p: KxStoryPeer, modifier: Modifier, onClick: () -> Unit) {
    val first = p.slides.first()
    val spin = remember { Animatable(0f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(p.unseen, reduced) { if (p.unseen && !reduced) spin.animateTo(720f, tween(12000, easing = LinearEasing)) }
    Box(modifier.height(214.dp).clip(RoundedCornerShape(24.dp)).look(first.look).then(Modifier)
        .then(if (p.unseen) Modifier.border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(24.dp)) else Modifier)
        .kxClick("История: ${p.name}", onClick = onClick)) {
        if (first.media != null) KxImage(first.media, Modifier.matchParentSize(), maxPx = 600)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(.45f to Color.Transparent, 1f to Color.Black.copy(alpha = .65f))))
        Icon(Looks[first.look % Looks.size].third, null, Modifier.align(BiasAlignment(0f, -.16f)).size(44.dp), tint = Color.White.copy(alpha = .28f))
        Box(Modifier.padding(10.dp).size(40.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.matchParentSize().graphicsLayer { rotationZ = -90f + spin.value }.clip(CircleShape).background(if (p.unseen) Kx.storyRing else Brush.linearGradient(listOf(Kx.w(.3f), Kx.w(.3f)))))
            KxAvatar(p.name, p.id, Modifier.border(2.dp, Kx.bg, CircleShape), size = 34.dp, colorIndex = p.colorIndex, photo = p.avatar)
        }
        if (p.slides.size > 1) Text(p.slides.size.toString(), style = KxType.style(11.sp, FontWeight.Bold), modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 10.dp)
            .clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .4f)).padding(horizontal = 8.dp, vertical = 2.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
            KxLine(p.name, KxType.style(15.sp, FontWeight.Bold))
            Text(p.time, style = KxType.style(12.sp, color = Color.White.copy(alpha = .75f)))
        }
    }
}

private class BiasAlignment(h: Float, v: Float) : Alignment by androidx.compose.ui.BiasAlignment(h, v)

/**
 * Full-screen viewer, Telegram-like (fixer-final): a photo stays ~6 s, a video for its own length (the bar follows
 * playback); the bar starts only once the photo is decoded / the video is ready (a real story whose file is still
 * downloading waits with a spinner instead of being skipped); press and hold pauses and, after a moment, hides the
 * UI; tap left/right = previous/next; swipe down closes; the reply field pauses. No auto-zoom of the photo. The
 * person order is taken when the viewer opens, so a story turning «seen» (TDLib moves the person down the list)
 * does not make the viewer jump to someone else.
 */
@Composable
fun StoryViewer(store: KxStore, startIndex: Int, onClose: () -> Unit) {
    val order = remember { store.stories.filter { !it.mine && it.slides.isNotEmpty() }.map { it.id } }
    val peers = order.mapNotNull { id -> store.stories.firstOrNull { it.id == id && it.slides.isNotEmpty() } }
    var openId by remember { mutableStateOf(order.getOrNull(startIndex.coerceIn(0, (order.size - 1).coerceAtLeast(0)))) }
    var slide by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    val flying = remember { mutableStateListOf<Pair<Long, String>>() }
    var paused by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }
    var hideUi by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf("") }
    var replyFocused by remember { mutableStateOf(false) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val toast = LocalKxToast.current
    val open = peers.indexOfFirst { it.id == openId }
    if (open < 0) { LaunchedEffect(Unit) { onClose() }; return }
    val peer = peers[open]
    val slideAt = slide.coerceIn(0, peer.slides.lastIndex)
    val s = peer.slides[slideAt]
    fun advance(dir: Int) {
        var sl = slideAt + dir; var op = open
        if (sl >= peer.slides.size) { op += 1; sl = 0 }
        if (sl < 0) { if (op > 0) { op -= 1; sl = peers[op].slides.lastIndex } else sl = 0 }
        if (op >= peers.size) { onClose(); return }
        openId = peers[op].id; slide = sl
    }
    LaunchedEffect(openId, slideAt) { store.viewStory(peer.id, s.id); progress.snapTo(0f) }
    DisposableEffect(Unit) { onDispose { store.closeStory() } }
    LaunchedEffect(held) { if (held) { delay(250); hideUi = true } else hideUi = false }
    // wave5 (fixer-media): a video story plays (with sound) once its file is on the phone; the bar follows the video's length.
    val videoPath = s.media?.takeIf { s.video }
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(videoPath) {
        videoPath?.let { p ->
            com.kirox.app.ui.chat.VoicePlayer.stop()
            androidx.media3.exoplayer.ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
                .setHandleAudioBecomingNoisy(true).build().apply {
                    setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(p)))); playWhenReady = false; prepare()
                }
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }
    val photo = if (player == null) rememberKxBitmap(s.media) else null
    // Waiting for the media: nothing loaded yet, or a photo still decoding. Demo slides (no media, not pending) run at once.
    val waiting = s.pending || (player == null && s.media != null && photo == null)
    val running = !paused && !held && !replyFocused && !waiting
    LaunchedEffect(openId, slideAt, running, player) {
        val p = player
        if (p != null) {
            p.playWhenReady = running
            if (running) while (true) {
                if (p.playerError != null) { // unplayable file: behave like a photo
                    progress.animateTo(1f, tween(((1f - progress.value) * 6000).toInt().coerceAtLeast(1), easing = LinearEasing)); advance(1); break
                }
                val d = p.duration
                if (d > 0 && p.playbackState != androidx.media3.common.Player.STATE_IDLE) progress.snapTo((p.currentPosition.toFloat() / d).coerceIn(0f, 1f))
                if (p.playbackState == androidx.media3.common.Player.STATE_ENDED) { progress.snapTo(1f); advance(1); break }
                delay(50)
            }
        }
        else if (running) { progress.animateTo(1f, tween(((1f - progress.value) * 6000).toInt().coerceAtLeast(1), easing = LinearEasing)); advance(1) }
        else progress.stop()
    }
    val zoom = remember { Animatable(.88f) }
    LaunchedEffect(Unit) { zoom.animateTo(1f, tween(300, easing = KxMotion.rise)) }
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val uiAlpha by animateFloatAsState(if (hideUi) 0f else 1f, tween(200), label = "storyUi")

    Box(Modifier.fillMaxSize().background(Color.Black).graphicsLayer {
        val d = (drag.value / size.height.coerceAtLeast(1f)).coerceIn(0f, 1f)
        scaleX = zoom.value * (1f - d * .15f); scaleY = scaleX; translationY = drag.value
        alpha = ((zoom.value - .88f) / .12f).coerceIn(0f, 1f) * (1f - d * .5f)
    }) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))) {
            Box(Modifier.fillMaxSize().look(s.look)) {
                if (player != null) androidx.compose.ui.viewinterop.AndroidView(factory = { c -> androidx.media3.ui.PlayerView(c).apply { useController = false; keepScreenOn = true } },
                    update = { it.player = player }, modifier = Modifier.matchParentSize())
                else if (photo != null) androidx.compose.foundation.Image(photo, null, Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            }
            if (s.media == null && !s.pending) Icon(Looks[s.look % Looks.size].third, null, Modifier.align(BiasAlignment(0f, -.1f)).size(90.dp), tint = Color.White.copy(alpha = .22f))
            if (waiting) androidx.compose.material3.CircularProgressIndicator(Modifier.align(Alignment.Center).size(44.dp), color = Color.White, strokeWidth = 3.dp)
            // Gestures over the story (under the header / reply row): tap left/right, hold = pause, drag down = close.
            Box(Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { scope.launch { if (drag.value > 140.dp.toPx()) onClose() else drag.animateTo(0f, tween(200)) } },
                        onDragCancel = { scope.launch { drag.animateTo(0f, tween(200)) } },
                    ) { change, dy -> change.consume(); scope.launch { drag.snapTo((drag.value + dy).coerceAtLeast(0f)) } }
                }
                .pointerInput(replyFocused) {
                    detectTapGestures(
                        onPress = { held = true; try { tryAwaitRelease() } finally { held = false } },
                        onTap = { o -> if (replyFocused) focus.clearFocus() else advance(if (o.x < size.width * .35f) -1 else 1) },
                    )
                }
                .semantics { contentDescription = "История: нажмите слева — назад, справа — дальше" })
            Box(Modifier.fillMaxWidth().height(140.dp).graphicsLayer { alpha = uiAlpha }.background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .55f), Color.Transparent))))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(200.dp).graphicsLayer { alpha = uiAlpha }.background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .6f)))))
            Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp).graphicsLayer { alpha = uiAlpha }) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    peer.slides.indices.forEach { i ->
                        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.3f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(when { i < slideAt -> 1f; i == slideAt -> progress.value; else -> 0f }).background(Color.White))
                        }
                    }
                }
                Row(Modifier.padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KxAvatar(peer.name, peer.id, size = 38.dp, colorIndex = peer.colorIndex, photo = peer.avatar)
                    Column(Modifier.weight(1f)) {
                        Text(peer.name, style = KxType.style(15.sp, FontWeight.Bold))
                        Text(peer.time, style = KxType.style(12.sp, color = Color.White.copy(alpha = .75f)))
                    }
                    Box(Modifier.size(40.dp).kxClick(if (paused) "Продолжить" else "Пауза") { paused = !paused }, contentAlignment = Alignment.Center) {
                        Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = Color.White)
                    }
                    Box(Modifier.size(40.dp).kxClick("Закрыть", onClick = onClose), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, null, tint = Color.White) }
                }
            }
            Text(s.caption, style = KxType.style(18.sp, FontWeight.Bold).copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = .5f), blurRadius = 20f)),
                modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 120.dp).graphicsLayer { alpha = uiAlpha })
            flying.forEach { (id, e) -> key(id) { FlyingEmoji(e) { flying.removeAll { it.first == id } } } }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(start = 10.dp, end = 10.dp, bottom = 22.dp).graphicsLayer { alpha = uiAlpha }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Reply field: typing pauses the story; «Отправить» sends the reply to the author (store.replyStory).
            Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.w(.12f)).border(1.dp, Kx.w(.2f), RoundedCornerShape(25.dp))
                .padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                if (reply.isEmpty()) Text("Ответить…", style = KxType.style(15.sp, color = Color.White.copy(alpha = .8f)), maxLines = 1)
                // The field pauses the story while it has focus (replyFocused); leaving it resumes.
                androidx.compose.foundation.text.BasicTextField(reply, { reply = it }, Modifier.fillMaxWidth()
                    .onFocusChanged { replyFocused = it.isFocused },
                    singleLine = true, textStyle = KxType.style(15.sp, color = Color.White), cursorBrush = androidx.compose.ui.graphics.SolidColor(Kx.accent))
            }
            if (reply.isNotBlank()) Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.accent).kxClick("Отправить") {
                store.replyStory(peer.id, s.id, reply.trim()); reply = ""; focus.clearFocus(); toast("Ответ отправлен")
            }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp), tint = Kx.onAccent) }
            else listOf("❤️", "🔥", "😂").forEach { e ->
                Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.w(.12f)).border(1.dp, Kx.w(.2f), CircleShape)
                    .kxClick("Реакция $e") { store.reactStory(peer.id, s.id, e); flying.add(System.nanoTime() to e) }, contentAlignment = Alignment.Center) { Text(e, style = KxType.style(20.sp)) }
            }
        }
    }
}


@Composable
private fun BoxScope.FlyingEmoji(e: String, onDone: () -> Unit) {
    val p = remember { Animatable(0f) }
    val x = remember { (30..70).random() / 100f }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)); onDone() }
    Text(e, style = KxType.style(34.sp), modifier = Modifier.align(BiasAlignment(x * 2 - 1, 1f)).padding(bottom = 100.dp)
        .graphicsLayer { translationY = -p.value * 260.dp.toPx(); alpha = 1f - p.value; scaleX = 1f + p.value * .4f; scaleY = scaleX })
}
