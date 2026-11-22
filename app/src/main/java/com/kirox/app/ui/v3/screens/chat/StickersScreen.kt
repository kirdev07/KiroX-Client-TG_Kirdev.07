package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import kotlinx.coroutines.delay
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.model.KxSticker
import com.kirox.app.ui.v3.model.KxStickerSet
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

/** Splits a string into emoji (grapheme clusters). */
internal fun emojiSplit(s: String): List<String> {
    val it = java.text.BreakIterator.getCharacterInstance(); it.setText(s)
    val out = ArrayList<String>(); var start = it.first(); var end = it.next()
    while (end != java.text.BreakIterator.DONE) { val g = s.substring(start, end); if (g.isNotBlank()) out += g; start = end; end = it.next() }
    return out
}

private class Cat(val icon: ImageVector?, val emoji: String, val label: String, val chars: String, val keys: String)

// Opened from a chat ("Stickers:<chatId>") with the real store: sticker sets / favourites / saved GIFs / GIF search come from the store
// (stickers are drawn by their emoji) and a tap sends with store.sendSticker(chatId, sticker.id).
// demo: needs store (emoji categories / recent emoji come from the board; sticker search by name is not in the store yet)
private val EmojiCats = listOf(
    Cat(Icons.Rounded.Schedule, "", "Недавние", "", ""),
    Cat(null, "😀", "Смайлы", "😀😃😄😁😆😅😂🤣🥲🥹😊😇🙂😉😌😍🥰😘😋😛😜🤪😎🤩🥳😏😒😔😢😭😤😡🤯😱🤗🤔🫡🤫😴", "улыбка смех"),
    Cat(null, "🐱", "Животные", "🐶🐱🐭🐹🐰🦊🐻🐼🐨🐯🦁🐮🐷🐸🐵🐔🐧🐦🦆🦉🐺🐗🐴🦄🐝🦋🐌🐞🐢🐍🦖🐙🦀🐬🐳🦈", "кот собака"),
    Cat(null, "🍕", "Еда", "🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍒🍑🥭🍍🥥🥝🍅🥑🥦🌽🥕🥐🍞🧀🍳🥞🍔🍟🍕🌭🌮🍣🍩🍪🎂☕🍵🧃", "еда пицца"),
    Cat(null, "⚽", "Активности", "⚽🏀🏈⚾🎾🏐🎱🏓🏸🥊🎯🎳🎮🎲🧩🎸🎹🎺🎻🥁🎤🎧🎬🎨🏆🥇🎫🎪🛹🚴🏊🧗", "спорт игра"),
    Cat(null, "💡", "Предметы", "⌚📱💻⌨️🖥️🖨️📷📹🎥📺📻⏰🔋🔌💡🔦🕯️💸💰💳💎🔧🔨🧲🔑🗝️🚪🛋️🎁🎈📦✉️📌✂️📎", "вещи"),
    Cat(null, "❤️", "Символы", "❤️🧡💛💚💙💜🖤🤍🤎💔❣️💕💞💓💗💖💘💝✨⭐🌟💫🔥💯✅❌⚠️♻️🔔🎵➕➖✔️", "сердце огонь"),
)
private val StickerCats = listOf(
    Cat(Icons.Rounded.Star, "", "Избранные", "🐼🦊🐸🐱", ""),
    Cat(null, "🐼", "Панда", "🐼🎍🍃🎋🥟🍜🧋😴🙈🫶🎉💤", "панда"),
    Cat(null, "🦊", "Лис", "🦊🍂🌙🔥😏🎒🧣☕📚🏕️🎸👋", "лис"),
    Cat(null, "🐸", "Лягушка", "🐸🪷💧🌿🎶😐🤌☔🍵🦟👑🫠", "лягушка"),
    Cat(Icons.Rounded.Add, "", "Свой набор", "", ""),
)
private val GifNames = listOf("Тренды", "Смех", "Ура", "Привет", "Котики", "Да", "Нет", "Танец")
private val GifGrads = listOf(
    Color(0xFF3F9A6C) to Color(0xFF1B2E3A), Color(0xFF6A4C9C) to Color(0xFF2A2240), Color(0xFFC77D3A) to Color(0xFF3A2414),
    Color(0xFF2F6E9E) to Color(0xFF14243A), Color(0xFF9E3F5C) to Color(0xFF2E1420), Color(0xFF5C9E3F) to Color(0xFF1E2E14),
)

private data class Sent(val id: Long, val type: Int, val text: String = "", val big: Boolean = false, val grad: Int = 0)

/** Board «Стикеры»: emoji / sticker / GIF panel under a chat composer (lens switch, search, recent emoji). */
@Composable
fun StickersScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = ctx.arg?.toLongOrNull() ?: store.chats.firstOrNull { it.kind == KxChatKind.Private }?.id ?: 0L
    val h = store.header(chatId)
    var panel by remember { mutableStateOf(true) }
    var mode by remember { mutableIntStateOf(0) }
    var cat by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    var recent by remember { mutableStateOf(listOf("😂", "❤️", "👍", "🔥", "🥹", "😎", "🎉", "🙏")) }
    val sent = remember { mutableStateListOf<Sent>() }
    fun push(m: Sent) { sent += m; while (sent.size > 4) sent.removeAt(0) }
    val qq = q.trim().lowercase()
    val hasDraft = draft.isNotEmpty()
    val live = store !is com.kirox.app.ui.v3.demo.KxDemoStore && ctx.arg?.toLongOrNull() != null
    val realSets by produceState(emptyList<KxStickerSet>(), live) { if (live) value = store.stickerSets() }
    val stickerCats = if (!live) StickerCats else listOf(StickerCats.first()) +
        realSets.map { s -> Cat(null, s.cover?.emoji?.ifEmpty { null } ?: "🗂", s.title, "", s.title.lowercase()) } + listOf(StickerCats.last())
    val liveStickers by produceState(emptyList<KxSticker>(), live, mode, cat, realSets.size) {
        if (live && mode == 1) value = if (cat == 0) store.favoriteStickers().ifEmpty { store.recentStickers() } else realSets.getOrNull(cat - 1)?.let { store.stickers(it.id) }.orEmpty()
    }
    val liveGifs by produceState(emptyList<KxSticker>(), live, mode, qq) {
        if (live && mode == 2) { if (qq.isNotEmpty()) delay(300); value = if (qq.isEmpty()) store.savedGifs() else store.searchGifs(chatId, qq) }
    }

    Column(Modifier.fillMaxSize().background(Kx.bg).ellipseGlow(1f, 0f, 1.2f * .6f, .6f * .6f, Kx.accentDeep.copy(alpha = .22f)).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 42.dp, onClick = ctx.back)
            KxAvatar(h.title, chatId, size = 38.dp, colorIndex = h.colorIndex)
            Column { Text(h.title, style = ts(16, W800), maxLines = 1); Text(h.status, style = ts(12, color = if (h.online) Kx.accent else Kx.text4), maxLines = 1) }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
            Text("Скинь что-нибудь смешное", style = ts(15), modifier = Modifier.fillMaxWidth(.78f).wrapContentWidth(Alignment.Start)
                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)).background(Color(0xFF161B18)).padding(horizontal = 12.dp, vertical = 8.dp))
            sent.forEach { m ->
                key(m.id) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        when (m.type) {
                            0 -> Text(m.text, style = ts(if (m.big) 40 else 15, color = Color(0xFFE6F6EC), lh = if (m.big) 52.sp else 19.5.sp),
                                modifier = Modifier.fillMaxWidth(.78f).wrapContentWidth(Alignment.End).chPop(.6f, 300).clip(RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp))
                                    .cssGradient(160f, Kx.accentDark, Color(0xFF1F4D36)).padding(horizontal = 12.dp, vertical = 8.dp))
                            1 -> Text(m.text, style = ts(96, lh = 96.sp), modifier = Modifier.chPop(.4f, 400))
                            else -> GifTile(m.grad, "GIF", Modifier.size(200.dp, 130.dp).chPop(.6f, 300), 3000, small = true)
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(23.dp))
                .padding(start = 4.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).kxClick(if (panel) "Клавиатура" else "Эмодзи и стикеры") { panel = !panel }, contentAlignment = Alignment.Center) {
                    Icon(if (panel) Icons.Outlined.Keyboard else Icons.Outlined.Mood, null, tint = if (panel) Kx.accent else Kx.text3)
                }
                if (panel) Text(if (hasDraft) draft else "Сообщение", style = ts(15, color = if (hasDraft) Kx.text else Kx.text4), maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                else ChInput(draft, { draft = it }, "Сообщение", Modifier.weight(1f).padding(horizontal = 4.dp))
                if (hasDraft) Box(Modifier.size(34.dp).clip(CircleShape).kxClick("Стереть") { draft = emojiSplit(draft).dropLast(1).joinToString("") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(20.dp), tint = Kx.text3)
                }
            }
            Box(Modifier.size(46.dp).clip(CircleShape).cssGradient(140f, Kx.accentMid, Kx.accentDeep).kxClick(if (hasDraft) "Отправить" else "Голосовое") {
                if (!hasDraft) return@kxClick
                val parts = emojiSplit(draft)
                val onlyEmoji = parts.size <= 3 && parts.all { p -> p.codePointAt(0) > 0x2000 }
                push(Sent(System.nanoTime(), 0, draft, onlyEmoji))
                if (chatId != 0L) store.send(chatId, draft)
                draft = ""
            }, contentAlignment = Alignment.Center) {
                Icon(if (hasDraft) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic, null, tint = Kx.onAccent)
            }
        }
        AnimatedVisibility(panel, enter = fadeIn(tween(300)) + slideInVertically(tween(300, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) { 40 }) {
            Column(Modifier.fillMaxWidth().height(392.dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color(0xFF111513))
                .drawBehind { drawLine(Kx.w(.08f), androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx()) }) {
                Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp).fillMaxWidth().height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.06f))
                    .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text4)
                    ChInput(q, { q = it }, listOf("Поиск эмодзи", "Поиск стикеров", "Поиск GIF")[mode], Modifier.weight(1f), size = 14)
                }
                val cats = when (mode) { 0 -> EmojiCats; 1 -> stickerCats; else -> emptyList() }
                if (cats.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    cats.forEachIndexed { i, c ->
                        val on = i == cat
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .16f) else Color.Transparent, tween(200), label = "cat")
                        Box(Modifier.size(40.dp, 36.dp).clip(RoundedCornerShape(12.dp)).background(bg).kxClick(c.label) {
                            if (!(mode == 1 && c.icon == Icons.Rounded.Add)) { cat = i; q = "" } else ctx.open("StickerPack")
                        }, contentAlignment = Alignment.Center) {
                            if (c.icon != null) Icon(if (on) c.icon else outlinedOf(c.icon), null, Modifier.size(21.dp), tint = if (on) Kx.accent else Kx.text4)
                            else Text(c.emoji, style = ts(22))
                        }
                    }
                }
                var title = ""
                val emojis: List<String>; val stickers: List<String>; val gifs: List<Int>
                when (mode) {
                    0 -> {
                        emojis = when {
                            qq.isNotEmpty() -> { title = "Поиск"; EmojiCats.filter { it.keys.isNotEmpty() && (it.keys.contains(qq) || it.label.lowercase().contains(qq)) }.flatMap { emojiSplit(it.chars) } }
                            cat == 0 -> { title = "Недавние"; recent }
                            else -> { title = EmojiCats[cat].label; emojiSplit(EmojiCats[cat].chars) }
                        }; stickers = emptyList(); gifs = emptyList()
                    }
                    1 -> {
                        stickers = if (live) { title = stickerCats.getOrNull(cat)?.label ?: ""; liveStickers.map { it.emoji.ifEmpty { "🙂" } } }
                            else if (qq.isNotEmpty()) { title = "Поиск"; StickerCats.filter { it.keys.isNotEmpty() && it.keys.contains(qq) }.flatMap { emojiSplit(it.chars) } }
                            else { title = StickerCats[cat].label; emojiSplit(StickerCats[cat].chars) }
                        emojis = emptyList(); gifs = emptyList()
                    }
                    else -> {
                        title = if (qq.isNotEmpty()) "Поиск" else if (live) "Сохранённые GIF" else "Популярные GIF"
                        gifs = if (live) liveGifs.indices.toList() else GifNames.indices.filter { qq.isEmpty() || GifNames[it].lowercase().contains(qq) }; emojis = emptyList(); stickers = emptyList()
                    }
                }
                val empty = (mode == 0 && emojis.isEmpty()) || (mode == 1 && stickers.isEmpty() && qq.isNotEmpty()) || (mode == 2 && gifs.isEmpty())
                val cols = when (mode) { 0 -> 8; 1 -> 4; else -> 2 }
                LazyVerticalGrid(GridCells.Fixed(cols), Modifier.weight(1f).padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (mode == 0) 2.dp else 6.dp), verticalArrangement = Arrangement.spacedBy(if (mode == 0) 2.dp else 6.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text(title, style = ts(12, W800, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)) }
                    if (mode == 0) itemsIndexed(emojis, key = { i, e -> "e$mode$cat$i$e" }) { _, e ->
                        Box(Modifier.height(42.dp).clip(RoundedCornerShape(12.dp)).kxClick(e) {
                            draft += e; recent = (listOf(e) + recent.filter { it != e }).take(16)
                        }, contentAlignment = Alignment.Center) { Text(e, style = ts(27)) }
                    }
                    if (mode == 1) itemsIndexed(stickers, key = { i, e -> "s$cat$i$e" }) { i, e ->
                        Box(Modifier.height(80.dp).clip(RoundedCornerShape(16.dp)).kxClick("Стикер $e") {
                            push(Sent(System.nanoTime(), 1, e))
                            if (live) liveStickers.getOrNull(i)?.let { store.sendSticker(chatId, it.id) }
                        }, contentAlignment = Alignment.Center) {
                            Text(e, style = ts(54), modifier = if (i % 3 == 0) Modifier.wobble() else Modifier)
                        }
                    }
                    if (mode == 2) itemsIndexed(gifs, key = { _, g -> "g$g" }) { i, g ->
                        val gName = if (live) "GIF" else GifNames[g]
                        GifTile(g % GifGrads.size, gName, Modifier.fillMaxWidth().height(if (i % 3 == 0) 130.dp else 100.dp)
                            .kxClick(gName) {
                                push(Sent(System.nanoTime(), 2, grad = g % GifGrads.size))
                                if (live) liveGifs.getOrNull(g)?.let { store.sendSticker(chatId, it.id) }
                            }, (2 + i % 3) * 1000)
                    }
                    if (empty) item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Ничего не нашлось", style = ts(14, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 30.dp))
                    }
                }
                Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 14.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.clip(RoundedCornerShape(22.dp)).background(Kx.w(.06f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(4.dp)) {
                        val lens by animateDpAsState((mode * 92).dp, tween(400, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)), label = "lens")
                        Box(Modifier.offset(x = lens).size(92.dp, 34.dp).clip(RoundedCornerShape(17.dp))
                            .background(Brush.verticalGradient(listOf(Kx.accent.copy(alpha = .28f), Kx.accent.copy(alpha = .14f)))).border(1.dp, Kx.accent.copy(alpha = .4f), RoundedCornerShape(17.dp)))
                        Row {
                            listOf("Эмодзи", "Стикеры", "GIF").forEachIndexed { i, label ->
                                val ink by animateColorAsState(if (i == mode) Kx.text else Kx.text4, tween(200), label = "mi")
                                Box(Modifier.size(92.dp, 34.dp).clip(RoundedCornerShape(17.dp)).kxClick { mode = i; cat = 0; q = "" }, contentAlignment = Alignment.Center) {
                                    Text(label, style = ts(13, W700, ink))
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!panel) Spacer(Modifier.height(18.dp))
    }
}

private fun outlinedOf(icon: ImageVector): ImageVector = when (icon) {
    Icons.Rounded.Schedule -> Icons.Outlined.Schedule
    Icons.Rounded.Star -> Icons.Outlined.StarOutline
    else -> icon
}

/** kxWob: ±4° rock, 2.4s, two times. */
@Composable
private fun Modifier.wobble(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { repeat(2) { a.animateTo(1f, tween(2400, easing = LinearEasing)); a.snapTo(0f) } }
    return graphicsLayer { val t = a.value; rotationZ = -4f + 8f * (if (t < .5f) t * 2 else (1 - t) * 2) }
}

/** GIF placeholder: a moving 200% gradient (kxGif, linear, two runs) with a label chip. */
@Composable
private fun GifTile(grad: Int, label: String, modifier: Modifier, speedMs: Int, small: Boolean = false) {
    val (a, b) = GifGrads[grad]
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(speedMs, easing = LinearEasing)) } }
    Box(modifier.clip(RoundedCornerShape(18.dp)).drawBehind {
        val w = size.width * 2; val shift = -p.value * w
        drawRect(Brush.horizontalGradient(listOf(a, b, a, b, a), startX = shift, endX = shift + w * 2))
    }.padding(8.dp), contentAlignment = Alignment.BottomStart) {
        Text(label, style = ts(if (small) 11 else 12, if (small) W800 else W700), modifier = Modifier.clip(RoundedCornerShape(if (small) 6.dp else 8.dp))
            .background(Color.Black.copy(alpha = if (small) .45f else .4f)).padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 3.dp))
    }
}

