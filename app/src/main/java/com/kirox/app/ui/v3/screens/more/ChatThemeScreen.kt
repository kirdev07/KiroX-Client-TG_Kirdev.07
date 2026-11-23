package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.cssAngleEnd
import com.kirox.app.ui.v3.components.cssAngleStart
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// Demo themes. Real chat (arg = chat id): Telegram's emoji themes (store.chatThemes) set for both sides with store.applyChatTheme;
// Telegram has no «only for me» theme, so that switch is hidden on the real account.
private data class CTheme(val e: String, val name: String, val bgA: Color, val bgB: Color, val outA: Color, val outB: Color, val ink: Color,
                          val inBg: Color, val acc: Color, val motif: List<String>)

private val CT = listOf(
    CTheme("❌", "Без темы", Color(0xFF0A0C0B), Color(0xFF0A0C0B), Kx.accentMid, Kx.accentDeep, MoInk, Kx.w(.07f), Kx.accent, emptyList()),
    CTheme("🌸", "Сакура", Color(0xFF3A1424), Color(0xFF1C0E16), Color(0xFFF5A3C0), Color(0xFFD0648E), Color(0xFF2A0A18), Color(0x1FFFC8DC), Color(0xFFF5A3C0), listOf("🌸", "🌸", "✿")),
    CTheme("🐳", "Океан", Color(0xFF0B2A44), Color(0xFF071624), Color(0xFF7FC4E8), Color(0xFF2F6E9E), Color(0xFF04172A), Color(0x1F7FC4E8), Color(0xFF7FC4E8), listOf("🫧", "🐳", "🫧")),
    CTheme("🌿", "Лес", Color(0xFF12301F), Color(0xFF08140D), Kx.accentMid, Kx.accentDeep, MoInk, Color(0x1F8FD3AC), Kx.accent, listOf("🍃", "🌿", "🍃")),
    CTheme("🔥", "Огонь", Color(0xFF3A1A0A), Color(0xFF160904), Color(0xFFF2B866), Color(0xFFC0443C), Color(0xFF2A0E04), Color(0x1FF2B866), Color(0xFFF2B866), listOf("🔥", "✨", "🔥")),
    CTheme("🌙", "Ночь", Color(0xFF1A1033), Color(0xFF0A0718), Color(0xFFB9A3E8), Color(0xFF6A4C9C), Color(0xFF140A28), Color(0x1FB9A3E8), Color(0xFFB9A3E8), listOf("⭐", "✨", "🌙")),
    CTheme("🎄", "Праздник", Color(0xFF0F2A1C), Color(0xFF2A0E0E), Color(0xFFE05A50), Color(0xFFA02E28), Color.White, Kx.w(.1f), MoAmber, listOf("❄️", "✨", "❄️")),
)

/** ChatTheme board: pick a theme for one chat (live preview + floating motif), for both or only for me, apply. */
@Composable
fun ChatThemeScreen(ctx: KxScreenCtx) {
    val chatId = ctx.c3Chat
    if (ctx.c3Live && chatId == null) { C3NeedChat(ctx, "Тема чата"); return }
    val live = chatId != null
    val header = chatId?.let { ctx.store.header(it) }
    val peek = c3Peek(ctx.store, chatId)
    val scope = rememberCoroutineScope()
    val names = if (live) ctx.store.chatThemes else emptyList()
    val looks = CT.drop(1)
    val themes = if (!live) CT else listOf(CT[0]) + names.mapIndexed { i, n -> CT.firstOrNull { it.e == n }?.copy(name = "Тема $n") ?: looks[i % looks.size].copy(e = n, name = "Тема $n", motif = listOf(n, "✨", n)) }
    if (live && names.isEmpty()) { C3NeedChat(ctx, "Тема чата", "Telegram пока не прислал список тем. Откройте позже."); return }
    var t by remember { mutableIntStateOf(0) }
    var cur by remember { mutableIntStateOf(if (live) -1 else 0) }
    var both by remember { mutableStateOf(true) }
    var curBoth by remember { mutableStateOf(true) }
    var k by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var tk by remember { mutableIntStateOf(0) }
    val th = themes[t.coerceIn(0, themes.lastIndex)]
    val changed = t != cur
    val bgA by animateColorAsState(th.bgA, tween(500), label = "bgA")
    val bgB by animateColorAsState(th.bgB, tween(500), label = "bgB")

    LaunchedEffect(tk) { if (tk > 0) { delay(2200); toast = "" } }

    Box(Modifier.fillMaxSize().background(Kx.bg).drawBehind {
        drawRect(Brush.linearGradient(listOf(bgA, bgB), cssAngleStart(170f, size), cssAngleEnd(170f, size)))
    }) {
        if (th.motif.isNotEmpty()) key(t) { CtMotif(th.motif) }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Color(0xFF0D100E)).statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", bg = Kx.w(.08f), border = null, onClick = ctx.back)
                // fixer-misc4: the real chat's avatar (photo when downloaded), not a red letter.
                if (header != null) com.kirox.app.ui.v3.components.KxAvatar(header.title, header.chatId, size = 40.dp, colorIndex = header.colorIndex, photo = header.avatar)
                else MoLetter("А", Color(0xFFC0443C))
                Column(Modifier.weight(1f)) {
                    Text(header?.title ?: "Анна", style = mts(16, W8), maxLines = 1)
                    Text(header?.status ?: "в сети", style = mts(12, color = th.acc), maxLines = 1)
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds().padding(14.dp)) {
                val w = maxWidth
                val f = moEnter(400, key = k)
                // Anchored to the bottom: with several real messages / photos the oldest ones slide out at the top instead of squeezing.
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().wrapContentHeight(Alignment.Bottom, unbounded = true).graphicsLayer { alpha = .2f + .8f * f },
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    if (cur > 0 && curBoth) Box(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .3f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)) { Text("Вы поставили тему ${themes[cur].e} «${themes[cur].name}»", style = mts(12, color = Color(0xFFDDE5E0))) }
                    if (!live) {
                        MoIncoming("Смотри, можно поставить тему на наш чат 😍", bg = th.inBg, modifier = Modifier.widthIn(max = w * .78f))
                        Box(Modifier.align(Alignment.End).widthIn(max = w * .78f).clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).moLin(135f, th.outA, th.outB)
                            .padding(horizontal = 12.dp, vertical = 8.dp)) { Text("Давай что-нибудь весёлое!", style = mts(15, color = th.ink)) }
                        MoIncoming("Выбирай ты 🙂", bg = th.inBg, modifier = Modifier.widthIn(max = w * .78f))
                    } else {
                        // fixer-misc4: the chat's own last messages (already loaded by the open chat; the peek otherwise), photos as images.
                        val loaded = chatId?.let { ctx.store.messages(it) }.orEmpty().filter { it.kind != com.kirox.app.ui.v3.model.KxMsgKind.Service }
                        val src = (if (loaded.isNotEmpty()) loaded else peek).filter { it.text.isNotBlank() || it.thumb != null || it.media != null }.takeLast(6)
                        src.forEach { m ->
                            val pic = (m.thumb ?: m.media)?.takeIf { m.kind == com.kirox.app.ui.v3.model.KxMsgKind.Photo || m.kind == com.kirox.app.ui.v3.model.KxMsgKind.Video }
                            val outShape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp); val inShape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
                            if (pic != null && !m.spoiler) {
                                val ar = if (m.aspect > 0f) m.aspect.coerceIn(.6f, 1.8f) else 1.3f
                                Box(Modifier.align(if (m.out) Alignment.End else Alignment.Start).width(w * .6f).aspectRatio(ar).clip(if (m.out) outShape else inShape).background(th.inBg)) {
                                    com.kirox.app.ui.v3.components.KxImage(pic, Modifier.matchParentSize(), maxPx = 480)
                                }
                            } else if (m.out) Box(Modifier.align(Alignment.End).widthIn(max = w * .78f).clip(outShape).moLin(135f, th.outA, th.outB)
                                .padding(horizontal = 12.dp, vertical = 8.dp)) { Text(m.text, style = mts(15, color = th.ink), maxLines = 4) }
                            else MoIncoming(m.text, m.time, bg = th.inBg, modifier = Modifier.widthIn(max = w * .78f))
                        }
                    }
                }
            }
            // sheet (part of the layout)
            val sp = moEnter(400, easing = KxMotion.rise)
            Column(Modifier.fillMaxWidth().graphicsLayer { translationY = (1f - sp) * size.height }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Kx.bg)
                .background(Brush.verticalGradient(listOf(Color(0xF51E2421), Color(0xFC0E110F))))
                .drawBehind { drawLine(Kx.w(.14f), Offset(0f, .5f), Offset(size.width, .5f), 1.dp.toPx()) }
                .navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 22.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Тема чата", style = mts(17, W8), modifier = Modifier.weight(1f))
                    Text(th.name, style = mts(12, color = MoSub))
                }
                Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    themes.forEachIndexed { i, x ->
                        val on = t == i
                        val ty by animateDpAsState(if (on) (-4).dp else 0.dp, tween(200), label = "ty")
                        val bd by animateColorAsState(if (on) Kx.text else Color.Transparent, tween(200), label = "bd")
                        Column(Modifier.offset(y = ty).size(70.dp, 92.dp).clip(RoundedCornerShape(18.dp)).moLin(170f, x.bgA, x.bgB)
                            .border(2.5.dp, bd, RoundedCornerShape(18.dp)).kxClick(x.name) { t = i; k++ }.padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                            verticalArrangement = Arrangement.SpaceBetween) {
                            Box(Modifier.align(Alignment.End).size(34.dp, 10.dp).clip(RoundedCornerShape(5.dp)).moLin(135f, x.outA, x.outB))
                            Box(Modifier.align(Alignment.Start).size(26.dp, 10.dp).clip(RoundedCornerShape(5.dp)).background(Kx.w(.35f)))
                            Text(x.e, style = mts(24), modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("У обоих · тема Telegram" to true, "Только у меня" to false).filter { !live || it.second }.forEach { (label, v) ->
                        val on = both == v
                        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .18f) else Color.Transparent)
                            .kxClick { both = v }, contentAlignment = Alignment.Center) { Text(label, style = mts(13, W7, if (on) Kx.accent else MoSub), maxLines = 1) }
                    }
                }
                Text(if (both) "${header?.title?.substringBefore(' ') ?: "Анна"} увидит ту же тему и сообщение о смене" else "Тема будет только на этом телефоне", style = mts(12, color = MoSub),
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp))
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
                    .then(if (changed) Modifier.moAccent() else Modifier.background(Kx.w(.07f))).kxClick {
                        if (changed && live) {
                            val pick = t
                            scope.launch {
                                val err = ctx.store.applyChatTheme(chatId!!, if (pick == 0) "" else themes[pick].e)
                                if (err == null) { cur = pick; curBoth = true; toast = if (pick == 0) "Тема убрана у вас обоих" else "Тема изменена у вас обоих" } else toast = err
                                tk++
                            }
                        } else if (changed) { cur = t; curBoth = both; toast = if (both) "Тема изменена у вас обоих" else "Тема изменена только у вас"; tk++ }
                    }, contentAlignment = Alignment.Center) {
                    Text(if (changed) (if (t == 0) "Убрать тему" else "Поставить «${th.name}»") else "Эта тема уже стоит", style = mts(16, W8, if (changed) MoInk else MoMuted))
                }
            }
        }

        if (toast.isNotEmpty()) key(tk) {
            val p = moEnter(2200, easing = LinearEasing)
            Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 74.dp).graphicsLayer {
                val a = when { p < .12f -> p / .12f; p < .85f -> 1f; else -> 1f - (p - .85f) / .15f }
                alpha = a; translationY = (1f - a) * (-10).dp.toPx()
            }.clip(RoundedCornerShape(16.dp)).background(Color(0xF51E2421)).border(1.dp, Kx.w(.14f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(toast, style = mts(13, W7), maxLines = 1)
            }
        }
    }
}

/** kxFloat: 9 emoji rise 420 px with a 40° turn, fading in to .5 and out; 2 runs, staggered by negative delays. */
@Composable
private fun CtMotif(motif: List<String>) {
    if (LocalKxReducedMotion.current) return
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { clock.animateTo(30f, tween(30000, easing = LinearEasing)) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth; val h = maxHeight
        repeat(9) { i ->
            val dur = 9f + (i % 4) * 2f
            val runs = (clock.value + i * 1.3f) / dur
            if (runs >= 2f) return@repeat
            val p = runs - kotlin.math.floor(runs)
            val sz = 16 + (i * 7) % 14
            Text(motif[i % motif.size], style = mts(sz), modifier = Modifier.offset(x = w * (((i * 37) % 92 + 2) / 100f), y = h + 30.dp - sz.dp * 1.2f).graphicsLayer {
                translationY = -420.dp.toPx() * p; rotationZ = 40f * p
                alpha = if (p < .15f) .5f * p / .15f else .5f * (1f - (p - .15f) / .85f)
            })
        }
    }
}
