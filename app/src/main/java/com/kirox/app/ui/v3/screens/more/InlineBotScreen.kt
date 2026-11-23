package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.GifBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// Demo bots and results. Real chat (arg = chat id): InlineBotLive asks the real bot (store.inlineQuery / sendInlineResult).
private val IB_GRADS = listOf(
    listOf(Color(0xFF3F9A6C), Color(0xFF2F6E9E), Color(0xFF6A4C9C)), listOf(Color(0xFFC0443C), Color(0xFFE8B04A), Color(0xFFC0443C)),
    listOf(Color(0xFF2F6E9E), Color(0xFF7FB8E0), Color(0xFF1B2A3A)), listOf(Color(0xFF6A4C9C), Color(0xFFC0443C), Color(0xFF2A1630)),
    listOf(Color(0xFFB07A2E), Color(0xFFF2B866), Color(0xFF6B4A2E)), listOf(Color(0xFF3A403D), Color(0xFF9AA59F), Color(0xFF1B201E)),
)
private data class IbBot(val name: String, val q: String, val label: String, val icon: ImageVector, val bg: Color, val items: List<String>, val rows: List<Triple<String, String, String>>)

private val IB_BOTS = linkedMapOf(
    "gif" to IbBot("@gif", "понедельник", "GIF по запросу «понедельник»", Icons.Rounded.GifBox, Color(0xFFC0443C), listOf("😴", "☕", "🐱", "🙃", "🧟", "📅"), emptyList()),
    "sticker" to IbBot("@sticker", "кофе", "Стикеры «кофе»", Icons.Rounded.EmojiEmotions, Color(0xFFB07A2E), listOf("☕", "🫖", "🥐", "😵‍💫", "🧋", "🍪"), emptyList()),
    "wiki" to IbBot("@wiki", "понедельник", "Статьи из Википедии", Icons.AutoMirrored.Rounded.MenuBook, Color(0xFF2F6E9E), emptyList(), listOf(
        Triple("📅", "Понедельник", "День недели между воскресеньем и вторником"), Triple("🎵", "Понедельник (песня)", "Музыкальная композиция"),
        Triple("🧠", "Синдром понедельника", "Термин из популярной психологии"))),
)
private data class IbSent(val id: Int, val via: String, val e: String = "", val grad: Int = 0, val text: String = "")

/** InlineBot board: «@gif понедельник» in the composer shows inline results above it; tap one to send «через @bot». */
@Composable
fun InlineBotScreen(ctx: KxScreenCtx) {
    val chatId = ctx.c3Chat
    if (ctx.c3Live) { if (chatId == null) C3NeedChat(ctx, "Инлайн-боты") else InlineBotLive(ctx, chatId); return }
    var bot by remember { mutableStateOf("gif") }
    var open by remember { mutableStateOf(true) }
    val sent = remember { mutableStateListOf<IbSent>() }
    var pk by remember { mutableIntStateOf(0) }
    var nid by remember { mutableIntStateOf(0) }
    val b = IB_BOTS.getValue(bot)
    fun send(m: IbSent) { open = false; sent.add(m.copy(id = nid++)); while (sent.size > 2) sent.removeAt(0) }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "Л", Color(0xFF6A4C9C), null, "Лиза", "в сети", Kx.accent)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val w = maxWidth
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    MoIncoming("Понедельник опять 😩", modifier = Modifier.widthIn(max = w * .78f))
                    sent.forEach { m -> key(m.id) { IbSentBubble(m, Modifier.align(Alignment.End)) } }
                }
            }

            if (open) key(pk) {
                val p = moEnter(300, easing = KxMotion.rise)
                val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                Column(Modifier.padding(horizontal = 8.dp).fillMaxWidth().graphicsLayer { alpha = p; translationY = (1f - p) * 20.dp.toPx() }
                    .clip(shape).background(Color(0xFF181D1B)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.06f))))
                    .drawBehind {
                        val c = Kx.w(.14f); val s = 1.dp.toPx()
                        drawLine(c, Offset(22.dp.toPx(), s / 2), Offset(size.width - 22.dp.toPx(), s / 2), s)
                        drawLine(c, Offset(s / 2, 22.dp.toPx()), Offset(s / 2, size.height), s)
                        drawLine(c, Offset(size.width - s / 2, 22.dp.toPx()), Offset(size.width - s / 2, size.height), s)
                    }.padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)) {
                    Row(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(b.bg), contentAlignment = Alignment.Center) {
                            Icon(b.icon, null, Modifier.size(15.dp), tint = Color.White)
                        }
                        Text(b.label, style = mts(12, W7, Kx.text2), modifier = Modifier.weight(1f), maxLines = 1)
                        Text(if (b.rows.isEmpty()) "листайте →" else "${b.rows.size} результата", style = mts(11, color = MoMuted))
                    }
                    if (b.rows.isEmpty()) {
                        val pan = moPanProgress(4000, 2)
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            b.items.forEachIndexed { i, e ->
                                val gif = bot == "gif"
                                val cw: Dp = if (gif && i % 2 == 0) 130.dp else 96.dp
                                val cp = moEnter(250, delayMs = i * 40)
                                Box(Modifier.size(cw, 110.dp).graphicsLayer { alpha = cp; val s = .85f + .15f * cp; scaleX = s; scaleY = s }.clip(RoundedCornerShape(14.dp))
                                    .then(if (gif) Modifier.moPan(110f, IB_GRADS[i % IB_GRADS.size], pan) else Modifier.background(Color.Black.copy(alpha = .2f)))
                                    .kxClick((if (gif) "GIF " else "Стикер ") + e) { send(IbSent(0, b.name, e, i % IB_GRADS.size)) }, contentAlignment = Alignment.Center) {
                                    Text(e, style = mts(34, color = Color.White))
                                    Box(Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = .45f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)) { Text(if (gif) "GIF" else "STK", style = mts(11, W8, Color.White)) }
                                }
                            }
                        }
                    } else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        b.rows.forEachIndexed { i, (e, t, sub) ->
                            val cp = moEnter(250, delayMs = i * 50)
                            Row(Modifier.fillMaxWidth().graphicsLayer { alpha = cp; val s = .85f + .15f * cp; scaleX = s; scaleY = s }.clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = .2f)).kxClick { send(IbSent(0, "@wiki", text = "$t — ${sub.lowercase()}. ru.wikipedia.org")) }
                                .padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x297FB8E0)), contentAlignment = Alignment.Center) { Text(e, style = mts(20)) }
                                Column(Modifier.weight(1f)) {
                                    Text(t, style = mts(14, W7), maxLines = 1)
                                    Text(sub, style = mts(12, color = MoSub), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().moTopLine().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(b.name, style = mts(15, W8, Kx.accent))
                    Text(" " + b.q, style = mts(15))
                    val c = moLoop(1000, label = "caret")
                    Box(Modifier.padding(start = 1.dp).size(2.dp, 18.dp).graphicsLayer { alpha = if (c < .5f) 1f else 0f }.background(Kx.accent))
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                IB_BOTS.forEach { (k, x) ->
                    val on = bot == k
                    Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                        .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(16.dp))
                        .kxClick { bot = k; open = true; pk++ }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text(x.name, style = mts(12, W8, if (on) Kx.accent else Kx.text2))
                    }
                }
            }
        }
    }
}

@Composable
private fun IbSentBubble(m: IbSent, modifier: Modifier) {
    val p = moEnter(300, easing = KxMotion.rise)
    val pan = moPanProgress(4000)
    val shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
    Column(modifier.graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s },
        horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        when (m.via) {
            "@gif" -> Box(Modifier.size(200.dp, 140.dp).clip(shape).moPan(110f, IB_GRADS[m.grad], pan), contentAlignment = Alignment.Center) { Text(m.e, style = mts(58, color = Color.White)) }
            "@sticker" -> Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) { Text(m.e, style = mts(84, color = Color.White)) }
            else -> Box(Modifier.width(240.dp).clip(shape).moAccent().padding(horizontal = 12.dp, vertical = 10.dp)) { Text(m.text, style = mts(14, color = MoInk)) }
        }
        Text("через ${m.via}", style = mts(11, color = MoMuted))
    }
}
