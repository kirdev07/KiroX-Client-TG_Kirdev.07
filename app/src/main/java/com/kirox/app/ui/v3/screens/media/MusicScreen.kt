package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

private class MusicTrack(val title: String, val artist: String, val secs: Int, val c1: Color, val c2: Color, val glow: Color, val full: Color)

// demo: needs store (music library: tracks from chats, playlists, favourites, player state)
private val MusicTracks = listOf(
    MusicTrack("Ночные огни", "Лето", 222, Color(0xFF3E7A5A), Color(0xFF0F1A14), Color(70, 160, 110).copy(alpha = .35f), Color(0xFF2C5A42)),
    MusicTrack("Северный ветер", "Полярная", 245, Color(0xFF2D4E73), Color(0xFF0F1A2A), Color(70, 120, 200).copy(alpha = .35f), Color(0xFF24456A)),
    MusicTrack("Город спит", "Неон", 178, Color(0xFF6E58C9), Color(0xFF1E1830), Color(130, 100, 220).copy(alpha = .35f), Color(0xFF4A3A8A)),
    MusicTrack("Тёплый вечер", "Солнце", 201, Color(0xFFD08A3C), Color(0xFF3A220A), Color(220, 150, 70).copy(alpha = .30f), Color(0xFF7A4E20)),
    MusicTrack("Дождь за окном", "Тихо", 233, Color(0xFFA24B5B), Color(0xFF3A1820), Color(200, 80, 100).copy(alpha = .30f), Color(0xFF6A2A36)),
)

private fun Modifier.cover(t: MusicTrack) = cssGrad(150f, t.c1, t.c2)

/** Board «Музыка»: library, track list with equalizer, mini player and the full player. */
@Composable
fun MusicScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → music from all chats + the app's player (VoicePlayer)
    // music: real account → the user's music channels, «Моя музыка», playlists (MusicLibrary.kt) and the full player (MusicPlayer.kt).
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) DemoMusicScreen(ctx) else RealMusicLibrary(ctx)
}

@Composable
private fun DemoMusicScreen(ctx: KxScreenCtx) {
    val L = MusicTracks
    var cur by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var pos by remember { mutableIntStateOf(52) }
    var expanded by remember { mutableStateOf(false) }
    val liked = remember { mutableStateMapOf<Int, Boolean>() }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableIntStateOf(0) }
    fun go(dir: Int) { cur = (cur + dir + L.size) % L.size; pos = 0; playing = true }
    LaunchedEffect(playing, cur) {
        while (playing) { delay(1000); if (pos + 1 >= L[cur].secs) go(1) else pos += 1 }
    }
    val c = L[cur]
    val glow by animateColorAsState(c.glow, tween(800, easing = Ease), label = "glow")
    val progress by animateFloatAsState(pos.toFloat() / c.secs, tween(500, easing = LinearEasing), label = "pos")
    val eqClock by rememberPlayClock(playing)

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Box(Modifier.fillMaxWidth().height(320.dp).ellipseGlow(glow, .3f, 0f, .9f, .7f, .7f))
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ctx.arg != "tab") MRoundBtn(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", highlight = Kx.w(.14f)) { ctx.back() }
                Column(Modifier.weight(1f)) {
                    T("Добрый вечер", 12.sp, color = Kx.text4)
                    T("Музыка", 22.sp, FontWeight.ExtraBold)
                }
                MRoundBtn(Icons.Outlined.Search, "Поиск", tint = Kx.text2)
                MRoundBtn(Icons.AutoMirrored.Outlined.PlaylistAdd, "Новый плейлист", tint = Kx.text2)
            }
            var filter by remember { mutableIntStateOf(0) }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Все", "Любимое", "Плейлисты", "Чаты").forEachIndexed { i, s ->
                    val on = i == filter
                    Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Kx.accent else Kx.w(.06f)).kxClick { filter = i }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        T(s, 13.sp, if (on) FontWeight.Bold else FontWeight.SemiBold, if (on) Kx.onAccent else Kx.text2)
                    }
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                T("Для вас", 16.sp, FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 10.dp))
                Row(Modifier.height(150.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1.3f).fillMaxHeight().kxRise(0, 500).clip(RoundedCornerShape(26.dp))
                        .cssGrad(150f, 0f to Color(0xFF3E7A5A), .75f to Color(0xFF10241A))
                        .kxClick { cur = (L.indices).random(); pos = 0; playing = true; shuffle = true }.padding(14.dp)) {
                        Icon(Icons.Rounded.GraphicEq, null, Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp).size(40.dp), tint = Color(200, 240, 216).copy(alpha = .35f))
                        Column(Modifier.align(Alignment.BottomStart).padding(end = 44.dp)) {
                            T("Вся музыка", 17.sp, FontWeight.ExtraBold)
                            T("48 треков · Перемешать", 12.sp, color = Color(0xFFBFDCCB))
                        }
                        Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(40.dp).clip(CircleShape).background(Kx.accent), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Shuffle, null, Modifier.size(24.dp), tint = Kx.onAccent)
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallMix("Любимое", "12 треков", Color(0xFFA24B5B), Color(0xFF3A1820), Color(0xFFFFD6DE), 50, Modifier.weight(1f))
                        SmallMix("Из чатов", "3 чата", Color(0xFF4B3F7A), Color(0xFF1E1830), Color(0xFFDCD0FF), 100, Modifier.weight(1f))
                    }
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Row(Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    T("Все треки", 16.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    T("Сначала новые", 12.sp, color = Kx.text4)
                }
                L.forEachIndexed { i, t ->
                    val active = i == cur
                    val rowBg by animateColorAsState(if (active) Kx.accent.copy(alpha = .08f) else Color.Transparent, tween(300), label = "row")
                    Row(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(16.dp)).background(rowBg)
                        .kxClick { cur = i; pos = 0; playing = true }.padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).cover(t), contentAlignment = Alignment.Center) {
                            if (active) Equalizer(eqClock)
                        }
                        Column(Modifier.weight(1f)) {
                            T(t.title, 15.sp, FontWeight.SemiBold, if (active) Kx.accent else Kx.text, maxLines = 1)
                            T("${t.artist} · ${fmtSec(t.secs)}", 12.sp, color = Kx.text4)
                        }
                        Icon(Icons.Outlined.MoreVert, null, Modifier.size(20.dp), tint = Kx.text4)
                    }
                }
            }
        }

        if (!expanded) MiniPlayer(c, playing, progress, eqClock, Modifier.align(Alignment.BottomCenter),
            onExpand = { expanded = true }, onToggle = { playing = !playing }, onNext = { go(1) })

        AnimatedVisibility(expanded, enter = slideInVertically(tween(450, easing = CubicBezierEasing(.2f, .9f, .25f, 1f))) { it },
            exit = slideOutVertically(tween(300)) { it }) {
            FullPlayer(c, playing, progress, pos, liked[cur] == true, shuffle, repeat,
                onCollapse = { expanded = false }, onToggle = { playing = !playing }, onNext = { go(1) },
                onPrev = { if (pos > 3) pos = 0 else go(-1) }, onLike = { liked[cur] = liked[cur] != true },
                onShuffle = { shuffle = !shuffle }, onRepeat = { repeat = (repeat + 1) % 3 })
        }
    }
}

@Composable
private fun SmallMix(title: String, sub: String, c1: Color, c2: Color, subColor: Color, delayMs: Int, modifier: Modifier) {
    Column(modifier.fillMaxWidth().kxRise(delayMs, 500).clip(RoundedCornerShape(20.dp)).cssGrad(150f, c1, c2).kxClick { }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Bottom) {
        T(title, 14.sp, FontWeight.Bold)
        T(sub, 11.sp, color = subColor)
    }
}

/** Three white bars, kxEq .8s ease-in-out infinite with .2s steps, frozen while paused. */
@Composable
private fun Equalizer(clock: Long) {
    Row(Modifier.height(18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            val s = if (LocalKxReducedMotion.current) 1f else pingPong(clock, 800, i * 200, .3f, 1f)
            Box(Modifier.width(3.dp).fillMaxHeight().graphicsLayer { scaleY = s; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, 1f) }.background(Color.White))
        }
    }
}

@Composable
private fun MiniPlayer(c: MusicTrack, playing: Boolean, progress: Float, clock: Long, modifier: Modifier, onExpand: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, coverPath: String? = null) {
    val shape = RoundedCornerShape(24.dp)
    // kxBreath 3s × 2, paused with the music.
    val breath = if (clock >= 6000) 1f else pingPong(clock, 3000, 0, 1f, 1.03f)
    Box(modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 20.dp).fillMaxWidth().height(66.dp)
        .shadow(34.dp, shape, ambientColor = Color.Black.copy(alpha = .55f), spotColor = Color.Black.copy(alpha = .55f))
        .clip(shape).background(Kx.surface).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
        .border(1.dp, Kx.w(.16f), shape).topHighlight(Kx.w(.35f)).kxClick("Открыть плеер", onClick = onExpand)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(50.dp).graphicsLayer { scaleX = breath; scaleY = breath }.clip(RoundedCornerShape(14.dp)).cover(c)) { KxImage(coverPath, Modifier.matchParentSize(), maxPx = 160) }
            Column(Modifier.weight(1f)) {
                T(c.title, 14.sp, FontWeight.Bold, maxLines = 1)
                T(c.artist, 12.sp, color = Color(0xFFA6B5AC))
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.accent).kxClick(if (playing) "Пауза" else "Играть", onClick = onToggle), contentAlignment = Alignment.Center) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(24.dp), tint = Kx.onAccent)
            }
            Box(Modifier.size(40.dp, 44.dp).kxClick("Следующий трек", onClick = onNext), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SkipNext, null, Modifier.size(24.dp), tint = Kx.text2)
            }
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(Kx.accent))
    }
}

@Composable
private fun FullPlayer(c: MusicTrack, playing: Boolean, progress: Float, pos: Int, liked: Boolean, shuffle: Boolean, repeat: Int,
                       onCollapse: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, onPrev: () -> Unit,
                       onLike: () -> Unit, onShuffle: () -> Unit, onRepeat: () -> Unit,
                       coverPath: String? = null, source: String = "Вся музыка", showLike: Boolean = true, onSeek: ((Float) -> Unit)? = null) {
    val coverScale by animateFloatAsState(if (playing) 1f else .9f, tween(500, easing = EaseOutBack), label = "cover")
    val playScale by animateFloatAsState(if (playing) 1f else .94f, tween(250, easing = CubicBezierEasing(.34f, 1.5f, .5f, 1f)), label = "play")
    val heart = remember { Animatable(1f) }
    LaunchedEffect(liked) {
        if (liked) { heart.snapTo(1f); heart.animateTo(1f, keyframes { durationMillis = 400; 1.35f at 160 using Ease; 1f at 400 using Ease }) }
    }
    val dim = Color.White.copy(alpha = .7f)
    Column(Modifier.fillMaxSize().background(Kx.bg).cssGrad(180f, 0f to c.full, .75f to Kx.bg).statusBarsPadding().navigationBarsPadding()
        .kxClick { }.padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MRoundBtn(Icons.Rounded.KeyboardArrowDown, "Свернуть", bg = Kx.w(.08f), border = null, onClick = onCollapse)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Text("ИГРАЕТ ИЗ", style = ts(11.sp, color = Color.White.copy(alpha = .65f)).copy(letterSpacing = 1.sp))
                T(source, 14.sp, FontWeight.Bold, maxLines = 1)
            }
            MRoundBtn(Icons.Rounded.MoreHoriz, "Ещё", bg = Kx.w(.08f), border = null)
        }
        Box(Modifier.padding(top = 30.dp).align(Alignment.CenterHorizontally).size(300.dp).graphicsLayer { scaleX = coverScale; scaleY = coverScale }
            .shadow(60.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black.copy(alpha = .55f), spotColor = Color.Black.copy(alpha = .55f))
            .clip(RoundedCornerShape(34.dp)).cover(c), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Album, null, Modifier.size(90.dp), tint = Color.White.copy(alpha = .25f))
            KxImage(coverPath, Modifier.matchParentSize(), maxPx = 600)
        }
        Row(Modifier.padding(top = 30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                T(c.title, 22.sp, FontWeight.ExtraBold, maxLines = 1)
                T(c.artist, 15.sp, color = dim)
            }
            if (showLike) Box(Modifier.size(46.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick("В любимое", onClick = onLike), contentAlignment = Alignment.Center) {
                Icon(if (liked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, null,
                    Modifier.size(24.dp).graphicsLayer { scaleX = heart.value; scaleY = heart.value }, tint = if (liked) Color(0xFFFF8A9A) else Color.White)
            }
        }
        Column(Modifier.padding(top = 20.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(14.dp).then(if (onSeek != null) Modifier.pointerInput(Unit) {
                detectTapGestures { o -> onSeek((o.x / size.width).coerceIn(0f, 1f)) } } else Modifier), contentAlignment = Alignment.CenterStart) {
                val p = progress.coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = .18f))) {
                    Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Color.White))
                }
                Box(Modifier.offset(maxWidth * p - 7.dp).size(14.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White))
            }
            Row(Modifier.padding(top = 2.dp)) {
                T(fmtSec(pos), 12.sp, color = dim, modifier = Modifier.weight(1f))
                T(fmtSec(c.secs), 12.sp, color = dim)
            }
        }
        Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).kxClick("Перемешать", onClick = onShuffle), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Shuffle, null, tint = if (shuffle) Kx.accent else dim)
            }
            Box(Modifier.size(52.dp).kxClick("Предыдущий трек", onClick = onPrev), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SkipPrevious, null, Modifier.size(36.dp), tint = Color.White)
            }
            Box(Modifier.size(78.dp).graphicsLayer { scaleX = playScale; scaleY = playScale }
                .shadow(30.dp, CircleShape, ambientColor = Color.Black.copy(alpha = .4f), spotColor = Color.Black.copy(alpha = .4f))
                .clip(CircleShape).background(Color.White).kxClick(if (playing) "Пауза" else "Играть", onClick = onToggle), contentAlignment = Alignment.Center) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(40.dp), tint = Kx.bg)
            }
            Box(Modifier.size(52.dp).kxClick("Следующий трек", onClick = onNext), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SkipNext, null, Modifier.size(36.dp), tint = Color.White)
            }
            Box(Modifier.size(44.dp).kxClick("Повтор", onClick = onRepeat), contentAlignment = Alignment.Center) {
                Icon(if (repeat == 2) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat, null, tint = if (repeat != 0) Kx.accent else dim)
            }
        }
        Spacer(Modifier.weight(1f))
        if (showLike) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.10f)).kxClick { }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.PhoneAndroid, null, Modifier.size(18.dp), tint = Kx.text)
                T("Слушаете на: Телефон", 13.sp, FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            MRoundBtn(Icons.Outlined.Bedtime, "Таймер сна", size = 40.dp, iconSize = 20.dp, bg = Kx.w(.10f), border = null)
            MRoundBtn(Icons.AutoMirrored.Outlined.QueueMusic, "Очередь", size = 40.dp, iconSize = 20.dp, bg = Kx.w(.10f), border = null)
        }
    }
}
