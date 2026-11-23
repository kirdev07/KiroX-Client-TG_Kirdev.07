package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

/** Full-screen viewer: progress bars, 5s per story, tap left/right, reply field, flying reactions. */
@Composable
fun StoryViewer(store: KxStore, startIndex: Int, onClose: () -> Unit) {
    val peers = store.stories.filter { !it.mine && it.slides.isNotEmpty() }
    var open by remember { mutableIntStateOf(startIndex.coerceIn(0, (peers.size - 1).coerceAtLeast(0))) }
    var slide by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    val flying = remember { mutableStateListOf<Pair<Long, String>>() }
    var paused by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf("") }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val toast = LocalKxToast.current
    if (peers.isEmpty()) { LaunchedEffect(Unit) { onClose() }; return }
    val peer = peers[open]
    val s = peer.slides[slide.coerceIn(0, peer.slides.lastIndex)]
    fun advance(dir: Int) {
        var sl = slide + dir; var op = open
        if (sl >= peer.slides.size) { op += 1; sl = 0 }
        if (sl < 0) { if (op > 0) { op -= 1; sl = peers[op].slides.lastIndex } else sl = 0 }
        if (op >= peers.size) { onClose(); return }
        open = op; slide = sl
    }
    LaunchedEffect(open, slide) { store.viewStory(peer.id, s.id); progress.snapTo(0f) }
    DisposableEffect(Unit) { onDispose { store.closeStory() } }
    LaunchedEffect(open, slide, paused) {
        if (!paused) { progress.animateTo(1f, tween(((1f - progress.value) * 5000).toInt().coerceAtLeast(1), easing = LinearEasing)); advance(1) }
        else progress.stop()
    }
    val zoom = remember { Animatable(.88f) }
    LaunchedEffect(Unit) { zoom.animateTo(1f, tween(300, easing = KxMotion.rise)) }
    val ken = remember(open, slide) { Animatable(1f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(open, slide) { if (!reduced) ken.animateTo(1.12f, tween(6000, easing = LinearOutSlowInEasing)) }

    Box(Modifier.fillMaxSize().background(Color.Black).graphicsLayer { scaleX = zoom.value; scaleY = zoom.value; alpha = ((zoom.value - .88f) / .12f).coerceIn(0f, 1f) }) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))) {
            Box(Modifier.fillMaxSize().graphicsLayer { scaleX = ken.value; scaleY = ken.value }.look(s.look)) { if (s.media != null) KxImage(s.media, Modifier.matchParentSize()) }
            Icon(Looks[s.look % Looks.size].third, null, Modifier.align(BiasAlignment(0f, -.1f)).size(90.dp), tint = Color.White.copy(alpha = .22f))
            Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .55f), Color.Transparent))))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(200.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .6f)))))
            Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    peer.slides.indices.forEach { i ->
                        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.3f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(when { i < slide -> 1f; i == slide -> progress.value; else -> 0f }).background(Color.White))
                        }
                    }
                }
                Row(Modifier.padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KxAvatar(peer.name, peer.id, size = 38.dp, colorIndex = peer.colorIndex, photo = peer.avatar)
                    Column(Modifier.weight(1f)) {
                        Text(peer.name, style = KxType.style(15.sp, FontWeight.Bold))
                        Text(peer.time, style = KxType.style(12.sp, color = Color.White.copy(alpha = .75f)))
                    }
                    Box(Modifier.size(40.dp).kxClick("Ещё") { paused = !paused }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MoreHoriz, null, tint = Color.White) }
                    Box(Modifier.size(40.dp).kxClick("Закрыть", onClick = onClose), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, null, tint = Color.White) }
                }
            }
            Text(s.caption, style = KxType.style(18.sp, FontWeight.Bold).copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = .5f), blurRadius = 20f)),
                modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 120.dp))
            Row(Modifier.fillMaxSize().padding(top = 80.dp, bottom = 100.dp)) {
                Box(Modifier.weight(.35f).fillMaxHeight().clickable(interactionSource = null, indication = null) { advance(-1) }.semantics { contentDescription = "Назад" })
                Box(Modifier.weight(.65f).fillMaxHeight().clickable(interactionSource = null, indication = null) { advance(1) }.semantics { contentDescription = "Дальше" })
            }
            flying.forEach { (id, e) -> key(id) { FlyingEmoji(e) { flying.removeAll { it.first == id } } } }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Reply field: typing pauses the story; «Отправить» sends the reply to the author (store.replyStory).
            Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.w(.12f)).border(1.dp, Kx.w(.2f), RoundedCornerShape(25.dp))
                .padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                if (reply.isEmpty()) Text("Ответить…", style = KxType.style(15.sp, color = Color.White.copy(alpha = .8f)), maxLines = 1)
                androidx.compose.foundation.text.BasicTextField(reply, { reply = it; if (it.isNotEmpty()) paused = true }, Modifier.fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) paused = true },
                    singleLine = true, textStyle = KxType.style(15.sp, color = Color.White), cursorBrush = androidx.compose.ui.graphics.SolidColor(Kx.accent))
            }
            if (reply.isNotBlank()) Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.accent).kxClick("Отправить") {
                store.replyStory(peer.id, s.id, reply.trim()); reply = ""; paused = false; focus.clearFocus(); toast("Ответ отправлен")
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
