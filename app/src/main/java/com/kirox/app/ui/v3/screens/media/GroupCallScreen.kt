package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class GcPerson(val name: String, val short: String, val c1: Color, val c2: Color, val ink: Color, val bg: Color,
                       val face: Pair<Color, Color>? = null, val body: Color? = null, val video: Boolean = false, val hand: Boolean = false, val me: Boolean = false)

// demo: needs store (group call participants, speaking state, raised hands)
private val GcPeople = listOf(
    GcPerson("Вы", "Вы", Color(0xFF3F9A6C), Color(0xFF1F4D36), Color(0xFFDFF5E8), Color(0xFF151A1C), Color(0xFF7C8496) to Color(0xFF3E4452), Color(0xFF353B48), me = true),
    GcPerson("Марина · ведущая", "Марина", Color(0xFF4B3F7A), Color(0xFF2A2240), Color(0xFFDCD0FF), Color(0xFF1B1826), Color(0xFFB08C7A) to Color(0xFF5E4438), Color(0xFF5A4B7A), video = true),
    GcPerson("Олег", "Олег", Color(0xFF2F6E9E), Color(0xFF14243A), Color(0xFFCFE6FF), Color(0xFF141B22)),
    GcPerson("Дана", "Дана", Color(0xFF9E3F5C), Color(0xFF2E1420), Color(0xFFFFD0DD), Color(0xFF22161B), Color(0xFFC9A089) to Color(0xFF6E4E3E), Color(0xFF7A3F52), video = true),
    GcPerson("Тимур", "Тимур", Color(0xFF7A5A2E), Color(0xFF3A2A14), Color(0xFFFFE2B8), Color(0xFF1E1A14)),
    GcPerson("Алина", "Алина", Color(0xFF3A6E7A), Color(0xFF14303A), Color(0xFFCFF4FF), Color(0xFF13201F), hand = true),
)

private class GcTile(val p: GcPerson, val muted: Boolean, val talking: Boolean, val video: Boolean, val hand: Boolean)

/** Russian plural: 1 участник, 2 участника, 5 участников. */
fun ruPlural(n: Int, one: String, few: String, many: String): String {
    val m = kotlin.math.abs(n) % 100; val d = m % 10
    return if (m in 11..19) many else if (d == 1) one else if (d in 2..4) few else many
}

/**
 * kxTalk 1.2s: 2px #8FD3AC ring + a soft outer ring 6px@.25 → 12px@.08, drawn outside the shape
 * (box-shadow spread). [radius] = corner radius of the shape, or null for a circle.
 */
fun Modifier.talkRing(active: Boolean, clock: Long, radius: Dp?): Modifier = if (!active) this else drawBehind {
    val v = pingPong(clock, 1200, 0, 0f, 1f)
    val spread = (6f + 6f * v).dp.toPx(); val a = .25f - .17f * v
    fun ring(grow: Float, color: Color) {
        if (radius == null) drawCircle(color, size.minDimension / 2 + grow)
        else drawRoundRect(color, Offset(-grow, -grow), Size(size.width + 2 * grow, size.height + 2 * grow), CornerRadius(radius.toPx() + grow))
    }
    ring(spread, Kx.accent.copy(alpha = a))
    ring(2.dp.toPx(), Kx.accent)
}

/** Board «Видеочат» (group call): tiles grid or voice view, speaker line, controls, participants sheet. */
@Composable
fun GroupCallScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → the video chat this account is in (store.groupCall)
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) DemoGroupCallScreen(ctx) else RealGroupCall(ctx)
}

@Composable
private fun DemoGroupCallScreen(ctx: KxScreenCtx) {
    var sec by remember { mutableIntStateOf(754) }
    var speaker by remember { mutableIntStateOf(1) }
    var mic by remember { mutableStateOf(false) }
    var cam by remember { mutableStateOf(false) }
    var hand by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(true) }
    var people by remember { mutableStateOf(false) }
    var gone by remember { mutableStateOf(false) }
    var speakerOut by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val pool = listOf(1, 2, 3, 4) + if (mic) listOf(0) else emptyList()
            if (sec % 3 == 0) speaker = pool.random()
            sec += 1
        }
    }
    val tiles = GcPeople.mapIndexed { i, p ->
        val muted = if (p.me) !mic else i == 4 || i == 5
        GcTile(p, muted, speaker == i && !muted, if (p.me) cam else p.video, if (p.me) hand else p.hand)
    }
    val sp = if (tiles.getOrNull(speaker)?.talking == true) speaker else 1
    val count = "${GcPeople.size} ${ruPlural(GcPeople.size, "участник", "участника", "участников")}"
    val clock by rememberLoopClock()
    val glass = Kx.w(.12f); val on = Color(0xFFEEF3EF)

    Box(Modifier.fillMaxSize().background(Kx.bg).ellipseGlow(Kx.accentDeep.copy(alpha = .22f), .5f, 0f, .9f, .5f, .6f)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MRoundBtn(Icons.Rounded.KeyboardArrowDown, "Свернуть", size = 42.dp, bg = Kx.w(.07f), border = Kx.w(.1f)) { ctx.back() }
                Column(Modifier.weight(1f)) {
                    T("Курс английского", 17.sp, FontWeight.ExtraBold, maxLines = 1)
                    T("Видеочат · $count · ${sec / 60}:${(sec % 60).toString().padStart(2, '0')}", 12.sp, color = Kx.text3, maxLines = 1)
                }
                MRoundBtn(if (grid) Icons.Outlined.GraphicEq else Icons.Outlined.GridView, "Вид", size = 42.dp, bg = Kx.w(.07f), border = Kx.w(.1f)) { grid = !grid }
                MRoundBtn(Icons.Outlined.Group, "Участники", size = 42.dp, bg = Kx.w(.07f), border = Kx.w(.1f)) { people = true }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp).height(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpeakBars(height = 12.dp, max = 12.dp, periodMs = 800, stepMs = 150)
                T("Говорит: ${GcPeople[sp].name.substringBefore(" ·")}", 13.sp, color = Kx.accent)
            }
            if (grid) LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(tiles) { _, t -> GcGridTile(t, clock) }
            } else VoiceView(tiles, sp, clock, Modifier.weight(1f))
        }

        // Controls.
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 24.dp).fillMaxWidth()
            .shadow(40.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f))
            .clip(RoundedCornerShape(34.dp)).background(Kx.surface).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
            .border(1.dp, Kx.w(.16f), RoundedCornerShape(34.dp)).topHighlight(Kx.w(.18f)).padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
            GcBtn(if (cam) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff, "Камера", if (cam) on else glass, if (cam) Kx.bg else on) { cam = !cam }
            GcBtn(if (speakerOut) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.PhoneInTalk, if (speakerOut) "Динамик" else "Телефон",
                if (speakerOut) on else glass, if (speakerOut) Kx.bg else on) { speakerOut = !speakerOut }
            val micLabel = if (mic) "В эфире" else "Микрофон"
            Column(Modifier.width(76.dp).kxClick(micLabel) { mic = !mic; hand = false }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(62.dp, 50.dp).drawBehind { if (mic) drawRoundRect(Kx.accent.copy(alpha = .18f), Offset(-4.dp.toPx(), -4.dp.toPx()), Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), CornerRadius(29.dp.toPx())) }
                    .clip(RoundedCornerShape(25.dp)).then(if (mic) Modifier.cssGrad(140f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.16f))), contentAlignment = Alignment.Center) {
                    Icon(if (mic) Icons.Rounded.Mic else Icons.Rounded.MicOff, null, Modifier.size(28.dp), tint = if (mic) Kx.onAccent else on)
                }
                Text(micLabel, style = ts(11.sp, FontWeight.Bold, if (mic) Kx.accent else Kx.text2), maxLines = 1, softWrap = false)
            }
            GcBtn(Icons.Rounded.FrontHand, if (hand) "Опустить" else "Поднять руку", if (hand) Kx.warning else glass, if (hand) Color(0xFF1A1206) else on) { hand = !hand }
            GcBtn(Icons.Rounded.CallEnd, "Выйти", Color(0xFFE5534B), Color.White) { gone = true; people = false }
        }

        MSheet(people, { people = false }, dim = .5f, top = Color(0xFF121614), bottom = Color(0xFF121614), border = Kx.w(.08f), highlight = null,
            handleColor = Color(0xFF3A4540), handleWidth = 36.dp, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp), modifier = Modifier.heightIn(max = 620.dp)) {
            Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                T("Участники · $count", 19.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .12f)).kxClick { }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.PersonAdd, null, Modifier.size(18.dp), tint = Kx.accent)
                    T("Пригласить", 13.sp, FontWeight.Bold, Kx.accent)
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                tiles.forEach { t ->
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GcAvatar(t.p, 44.dp, 17.sp, Modifier.talkRing(t.talking, clock, null))
                        Column(Modifier.weight(1f)) {
                            T(t.p.name, 15.sp, FontWeight.Bold)
                            T(if (t.talking) "говорит" else if (t.hand) "хочет сказать" else if (t.muted) "микрофон выключен" else "слушает", 12.sp,
                                color = if (t.talking) Kx.accent else if (t.hand) Kx.warning else Kx.text4)
                        }
                        if (t.hand) Icon(Icons.Rounded.FrontHand, null, Modifier.size(20.dp), tint = Kx.warning)
                        Icon(if (t.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, null, Modifier.size(20.dp), tint = if (t.muted) Kx.text4 else if (t.talking) Kx.accent else Kx.text)
                    }
                }
            }
        }

        AnimatedVisibility(gone, enter = fadeIn(tween(400)), exit = fadeOut(tween(200))) {
            Column(Modifier.fillMaxSize().background(Kx.bg).ellipseGlow(Kx.accentDeep.copy(alpha = .18f), .5f, .42f, .7f, .45f, .7f).kxClick { },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                Box(Modifier.size(88.dp).clip(RoundedCornerShape(30.dp)).cssGrad(145f, Color(0xFF1D2A23), Color(0xFF121714)).border(1.dp, Color(0xFF2A3A31), RoundedCornerShape(30.dp)),
                    contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CallEnd, null, Modifier.size(44.dp), tint = Kx.accent) }
                T("Вы вышли из видеочата", 22.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
                T("Чат продолжается без вас", 14.sp, color = Kx.text3)
                Box(Modifier.padding(top = 16.dp).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.accent).kxClick { gone = false }.padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                    T("Вернуться", 14.sp, FontWeight.Bold, Kx.onAccent)
                }
            }
        }
    }
}

@Composable
private fun GcAvatar(p: GcPerson, size: Dp, font: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).cssGrad(140f, p.c1, p.c2), contentAlignment = Alignment.Center) { T(p.name.take(1), font, FontWeight.Bold, p.ink) }
}

@Composable
private fun GcGridTile(t: GcTile, clock: Long) {
    Box(Modifier.height(168.dp).talkRing(t.talking, clock, 24.dp).clip(RoundedCornerShape(24.dp)).background(t.p.bg)) {
        if (t.video && t.p.face != null) {
            val drift by rememberPlayClock(true, 14000)
            val v = if (drift >= 14000) 0f else pingPong(drift, 7000, 0, 0f, 1f)
            Box(Modifier.matchParentSize().graphicsLayer { translationX = -6.dp.toPx() * v; translationY = 5.dp.toPx() * v }
                .personPlaceholder(t.p.face.first, t.p.face.second, t.p.body!!, t.p.body, 30f, 64f, 72f, 102f, 130f, 100f))
        } else GcAvatar(t.p, 74.dp, 28.sp, Modifier.align(Alignment.Center))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(60.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .6f)))))
        Row(Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            T(t.p.name, 13.sp, FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            if (t.hand) HandWave(clock)
            Icon(if (t.muted) Icons.Rounded.MicOff else Icons.Rounded.Mic, null, Modifier.size(18.dp), tint = if (t.muted) Kx.text4 else if (t.talking) Kx.accent else Kx.text)
        }
    }
}

/** kxHand 1.2s ×2: 0 → -14° → 14° → 0. */
@Composable
private fun HandWave(@Suppress("UNUSED_PARAMETER") clock: Long) {
    val c by rememberPlayClock(true, 2400)
    val p = (c % 1200) / 1200f
    val r = if (c >= 2400) 0f else when {
        p < .25f -> -14f * EaseInOut.transform(p / .25f)
        p < .75f -> -14f + 28f * EaseInOut.transform((p - .25f) / .5f)
        else -> 14f - 14f * EaseInOut.transform((p - .75f) / .25f)
    }
    Icon(Icons.Rounded.FrontHand, null, Modifier.size(18.dp).graphicsLayer { rotationZ = r }, tint = Kx.warning)
}

@Composable
private fun VoiceView(tiles: List<GcTile>, sp: Int, clock: Long, modifier: Modifier) {
    val p = tiles[sp].p
    Column(modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            // kxBlob 2.4s ×2: wobbling soft blob, scale 1 → 1.12.
            val bc by rememberPlayClock(true, 4800)
            val v = if (bc >= 4800) 0f else pingPong(bc, 2400, 0, 0f, 1f)
            Box(Modifier.matchParentSize().graphicsLayer { scaleX = 1f + .12f * v + .03f * (1 - v); scaleY = 1f + .12f * v - .02f * v; rotationZ = 8f * v }
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
                .background(Brush.radialGradient(0f to Color(111, 196, 151).copy(alpha = .5f), .7f to Kx.accentDeep.copy(alpha = .15f), 1f to Kx.accentDeep.copy(alpha = .15f))))
            val pop = remember(sp) { Animatable(0f) }
            LaunchedEffect(sp) { pop.animateTo(1f, tween(400, easing = Ease)) }
            val sc = if (pop.value < .7f) .6f + .48f * pop.value / .7f else 1.08f - .08f * (pop.value - .7f) / .3f
            GcAvatar(p, 132.dp, 48.sp, Modifier.graphicsLayer { scaleX = sc; scaleY = sc; alpha = (pop.value / .7f).coerceAtMost(1f) })
        }
        T(p.name.substringBefore(" ·"), 20.sp, FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp))
        FlowRow(Modifier.padding(top = 22.dp, start = 20.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            tiles.forEach { t ->
                Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box {
                        GcAvatar(t.p, 52.dp, 19.sp, Modifier.talkRing(t.talking, clock, null))
                        if (t.hand) Icon(Icons.Rounded.FrontHand, null, Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).size(18.dp), tint = Kx.warning)
                    }
                    T(t.p.short, 11.sp, color = Kx.text2, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun GcBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, bg: Color, ink: Color, onClick: () -> Unit) {
    val b by animateColorAsState(bg, tween(200), label = "b"); val i by animateColorAsState(ink, tween(200), label = "i")
    Column(Modifier.width(60.dp).kxClick(label, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(b), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(24.dp), tint = i) }
        Text(label, style = ts(11.sp, color = Kx.text2), maxLines = 1, softWrap = false, modifier = Modifier.wrapContentWidth(unbounded = true))
    }
}

@Suppress("unused") private val keepStroke = Stroke(1f)
