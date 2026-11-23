package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxAvatarPalette
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType
import kotlinx.coroutines.launch

/*
 * fixer-chat4 (lead exhaustive walk f2bbba4): chat actions that opened separate boards with a fake mini-chat on the real
 * account now work in place, over the real ChatScreen: forward sheet, quote picker, reaction picker, chat translation,
 * emoji / sticker / GIF panel under the composer, dice picker, checklist editor. The boards stay for KxDemoStore.
 */

private val SheetBg = Color(0xFF121614)

/** Opaque bottom sheet over the chat: dim scrim (tap closes), handle, optional title, slide-up. */
@Composable
internal fun InChatSheet(onDismiss: () -> Unit, title: String? = null, sub: String? = null, content: @Composable ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val y = remember { Animatable(1f) }
    LaunchedEffect(Unit) { y.animateTo(0f, tween(280, easing = KxMotion.rise)) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x99050706)).kxClick("Закрыть", onClick = onDismiss))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = y.value * size.height }
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(SheetBg)
            .drawBehind { drawLine(Kx.w(.12f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
            .clickable(interactionSource = null, indication = null) {}.navigationBarsPadding().imePadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
            if (title != null) Row(Modifier.padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = KxType.style(18.sp, FontWeight.ExtraBold), maxLines = 1)
                    if (sub != null) Text(sub, style = KxType.style(12.sp, color = Kx.text3), maxLines = 1)
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.fillHi).kxClick("Закрыть", onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, Modifier.size(20.dp), tint = Kx.text)
                }
            }
            content()
        }
    }
}

@Composable
private fun SheetField(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier = Modifier, single: Boolean = true) {
    Box(modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, style = KxType.style(15.sp, color = Kx.text4))
        BasicTextField(value, onValue, singleLine = single, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
    }
}

// ---- Forward -------------------------------------------------------------------------------------------------------

/** «Переслать» over the chat: real chats with their photos (multi-select), author / caption options, comment, link. */
@Composable
internal fun ChatForwardSheet(store: KxStore, fromChat: Long, ids: List<Long>, onDismiss: () -> Unit, toast: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val sel = remember { mutableStateListOf<Long>() }
    var q by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var hide by remember { mutableStateOf(false) }
    var noCap by remember { mutableStateOf(false) }
    val channelIds = store.chats.filter { it.kind == KxChatKind.Channel }.map { it.id }
    val postable by produceState(emptySet<Long>(), channelIds) { value = channelIds.filter { runCatching { store.canPost(it) }.getOrDefault(false) }.toSet() }
    val targets = store.chats.filter { it.kind != KxChatKind.Channel || it.id in postable }
    val shown = targets.filter { q.isBlank() || it.title.lowercase().contains(q.trim().lowercase()) }
    InChatSheet(onDismiss, if (ids.size > 1) "Переслать ${ids.size} ${kxPlural(ids.size, "сообщение", "сообщения", "сообщений")}" else "Переслать") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(Kx.w(.06f)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text4)
                Box(Modifier.weight(1f)) {
                    if (q.isEmpty()) Text("Кому переслать?", style = KxType.style(15.sp, color = Kx.text4))
                    BasicTextField(q, { q = it }, singleLine = true, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
                }
            }
            if (ids.size == 1) Box(Modifier.size(42.dp).clip(CircleShape).background(Kx.w(.06f)).kxClick("Скопировать ссылку") {
                scope.launch {
                    val link = runCatching { store.messageLink(fromChat, ids.first()) }.getOrNull()
                    if (link != null) { clipboard.setText(androidx.compose.ui.text.AnnotatedString(link)); toast("Ссылка скопирована"); onDismiss() }
                    else toast("У этого сообщения нет публичной ссылки")
                }
            }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Link, null, tint = Kx.text2) }
        }
        Box(Modifier.fillMaxWidth().height(330.dp)) {
            LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 14.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(shown, key = { it.id }) { c ->
                    val on = c.id in sel
                    Column(Modifier.kxClick(c.title) { if (on) sel.remove(c.id) else sel.add(c.id) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(62.dp)) {
                            val square = c.kind == KxChatKind.Group || c.kind == KxChatKind.Channel
                            if (c.kind == KxChatKind.Saved) KxAvatar(c.title, c.id, size = 62.dp, icon = Icons.Rounded.Bookmark, swatch = KxAvatarPalette.saved)
                            else KxAvatar(c.title, c.id, size = 62.dp, square = square, colorIndex = c.colorIndex, photo = c.avatar)
                            if (on) Box(Modifier.matchParentSize().border(2.5.dp, Kx.accent, if (square) RoundedCornerShape(19.dp) else CircleShape))
                            if (on) Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(22.dp).clip(CircleShape).background(SheetBg).padding(2.dp).clip(CircleShape)
                                .background(Kx.accent), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Kx.onAccent) }
                        }
                        Text(if (c.kind == KxChatKind.Saved) "Избранное" else c.title, style = KxType.style(12.sp, color = if (on) Kx.accent else Kx.text2), maxLines = 2,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 80.dp))
                    }
                }
            }
            if (shown.isEmpty()) Text("Чаты не найдены", style = KxType.style(14.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
        }
        if (sel.isNotEmpty()) {
            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxChip(if (hide) "Без автора" else "С автором", selected = hide) { hide = !hide }
                if (hide) KxChip("Без подписи", selected = noCap) { noCap = !noCap }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetField(note, { note = it }, "Добавить комментарий", Modifier.weight(1f))
                Box(Modifier.size(48.dp).clip(CircleShape).background(Kx.accent).kxClick("Отправить") {
                    val names = sel.mapNotNull { id -> targets.firstOrNull { it.id == id } }
                    names.forEach { t ->
                        if (note.isNotBlank()) store.send(t.id, note.trim())
                        store.forward(fromChat, ids, t.id, hideSender = hide, dropCaption = hide && noCap)
                    }
                    toast(if (names.size == 1) (if (names[0].kind == KxChatKind.Saved) "Сохранено в «Избранное»" else "Переслано: ${names[0].title}")
                        else "Переслано в ${names.size} ${kxPlural(names.size, "чат", "чата", "чатов")}")
                    onDismiss()
                }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(22.dp), tint = Kx.onAccent) }
            }
        }
    }
}

internal fun kxPlural(n: Int, one: String, few: String, many: String): String {
    val a = n % 100; val b = n % 10
    return if (a in 11..14) many else when (b) { 1 -> one; 2, 3, 4 -> few; else -> many }
}

// ---- Quote ---------------------------------------------------------------------------------------------------------

/**
 * «Цитировать» over the chat: the real message (author Вы / sender, real time); tap the first and the last word.
 * The quote is the exact part of the text (TDLib checks it), handed to the composer's reply bar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatQuoteSheet(m: KxMessage, author: String, onDismiss: () -> Unit, onQuote: (String) -> Unit) {
    val words = remember(m.text) { Regex("\\S+").findAll(m.text).map { it.range }.toList() }
    var a by remember { mutableIntStateOf(-1) }
    var b by remember { mutableIntStateOf(-1) }
    val lo = if (b >= 0) minOf(a, b) else a
    val hi = if (b >= 0) maxOf(a, b) else a
    val quote = if (a >= 0) m.text.substring(words[lo].first, words[hi].last + 1) else ""
    InChatSheet(onDismiss, "Цитата", "Нажмите на первое и последнее слово") {
        Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Column(Modifier.align(if (m.out) Alignment.End else Alignment.Start).fillMaxWidth(.9f).clip(RoundedCornerShape(18.dp))
                .background(if (m.out) Kx.accentDark else Color(0xFF1B211E)).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)) {
                Text(author, style = KxType.style(13.sp, FontWeight.ExtraBold, if (m.out) Color(0xFFA6E3C1) else Kx.accent), modifier = Modifier.padding(bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    words.forEachIndexed { i, r ->
                        val on = a >= 0 && i in lo..hi
                        val edge = on && (i == a || i == b)
                        Text(m.text.substring(r.first, r.last + 1), style = KxType.style(15.sp, color = if (edge) Kx.onAccent else Kx.text, lineHeight = 26.sp),
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (on) (if (edge) Kx.accent else Kx.accent.copy(alpha = .38f)) else Color.Transparent)
                                .kxClick { if (a < 0 || b >= 0) { a = i; b = -1 } else b = i }.padding(horizontal = 1.dp))
                    }
                }
                Text((if (m.edited) "изменено " else "") + m.time, style = KxType.style(11.sp, color = Kx.text4), modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            }
        }
        if (quote.isNotEmpty()) Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.05f)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.FormatQuote, null, tint = Kx.accent)
            Text("«$quote»", style = KxType.style(13.sp, color = Kx.text2), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxButton("Всё сообщение", Modifier.weight(1f), KxButtonKind.Quiet, height = 48.dp) { onQuote(m.text.trim()) }
            KxButton("Ответить с цитатой", Modifier.weight(1.3f), height = 48.dp, enabled = quote.isNotEmpty()) { onQuote(quote) }
        }
    }
}

// ---- Reactions -----------------------------------------------------------------------------------------------------

/** «Все реакции» over the chat: what Telegram allows for this message (top / recent / popular), Premium-only locked. */
@Composable
internal fun ChatReactionSheet(store: KxStore, chatId: Long, m: KxMessage, onDismiss: () -> Unit, onPick: (String) -> Unit, toast: (String) -> Unit) {
    val panel by produceState<KxReactionPanel?>(null, chatId, m.id) { value = runCatching { store.reactionPanel(chatId, m.id) }.getOrNull() ?: KxReactionPanel(emptyList()) }
    val mine = m.reactions.filter { it.mine }.map { it.emoji }.toSet()
    InChatSheet(onDismiss, "Реакции") {
        val p = panel
        Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            when {
                p == null -> Text("Загрузка…", style = KxType.style(14.sp, color = Kx.text3), modifier = Modifier.padding(vertical = 24.dp))
                p.top.isEmpty() && p.recent.isEmpty() && p.popular.isEmpty() -> Text("Реакции в этом чате выключены", style = KxType.style(14.sp, color = Kx.text3), modifier = Modifier.padding(vertical = 24.dp))
                else -> listOf("Частые" to p.top, "Недавние" to (p.recent - p.top.toSet()), "Все" to (p.popular - p.top.toSet() - p.recent.toSet())).forEach { (label, list) ->
                    if (list.isNotEmpty()) {
                        Text(label, style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp))
                        list.chunked(7).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                row.forEach { e ->
                                    val locked = e in p.premiumOnly && !store.me.premium
                                    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(if (e in mine) Kx.accent.copy(alpha = .18f) else Color.Transparent)
                                        .kxClick("Реакция $e") { if (locked) toast("Эта реакция — с Telegram Premium") else onPick(e) }, contentAlignment = Alignment.Center) {
                                        Text(e, style = KxType.style(26.sp), modifier = if (locked) Modifier.graphicsLayer { alpha = .5f } else Modifier)
                                        if (locked) Icon(Icons.Rounded.Lock, null, Modifier.align(Alignment.BottomEnd).padding(2.dp).size(12.dp), tint = Kx.premium)
                                    }
                                }
                                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Translate chat ------------------------------------------------------------------------------------------------

internal val KxTranslateLangs = listOf(Triple("ru", "🇷🇺", "Русский"), Triple("en", "🇬🇧", "Английский"), Triple("kk", "🇰🇿", "Казахский"))

/** A message is «foreign» when its letters are in another script than the target language (Telegram has no detection call). */
internal fun kxForeignTo(text: String, lang: String): Boolean {
    val letters = text.filter { it.isLetter() }
    if (letters.length < 2) return false
    val cyr = letters.count { it in 'Ѐ'..'ӿ' }
    return if (lang == "en") cyr > 0 else cyr * 2 < letters.length
}

/** «Перевести чат» over the chat: translation on / off for incoming messages of this chat and the target language. */
@Composable
internal fun ChatTranslateSheet(on: Boolean, lang: Int, secret: Boolean, onSet: (Boolean, Int) -> Unit, onDismiss: () -> Unit) {
    InChatSheet(onDismiss, "Перевод чата", "Переводит Telegram, входящие сообщения на другом языке") {
        if (secret) Text("В секретных чатах Telegram не переводит сообщения.", style = KxType.style(14.sp, color = Kx.text3), modifier = Modifier.padding(vertical = 12.dp))
        else {
            Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).kxClick(if (on) "Показать оригинал" else "Переводить чат") { onSet(!on, lang) }
                .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Translate, null, tint = Kx.accent)
                Text(if (on) "Показать оригинал" else "Переводить сообщения", style = KxType.style(15.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                KxSwitchDot(on)
            }
            Text("Переводить на", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp))
            KxTranslateLangs.forEachIndexed { i, (_, flag, name) ->
                Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).kxClick(name) { onSet(on, i) }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(flag, style = KxType.style(20.sp)); Text(name, style = KxType.style(15.sp), modifier = Modifier.weight(1f))
                    if (i == lang) Icon(Icons.Rounded.Check, null, tint = Kx.accent)
                }
            }
        }
    }
}

@Composable
private fun KxSwitchDot(on: Boolean) {
    Box(Modifier.size(46.dp, 28.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent else Kx.w(.15f)).padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(if (on) Kx.onAccent else Kx.text3))
    }
}

// ---- Dice ----------------------------------------------------------------------------------------------------------

private val Dice = listOf("🎲" to "Кубик", "🎯" to "Дартс", "🏀" to "Баскетбол", "⚽" to "Футбол", "🎳" to "Боулинг", "🎰" to "Автомат")

/** Attach «Игры»: a small picker; Telegram rolls the dice (sendDice) and the result shows in the chat. */
@Composable
internal fun ChatDiceSheet(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    InChatSheet(onDismiss, "Игры", "Результат выбирает Telegram, его видят все в чате") {
        Dice.chunked(3).forEach { row ->
            Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (e, label) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick("$label $e") { onPick(e) }.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(e, style = KxType.style(34.sp)); Text(label, style = KxType.style(12.sp, color = Kx.text2))
                    }
                }
            }
        }
    }
}

// ---- Checklist -----------------------------------------------------------------------------------------------------

/** Attach «Список»: the checklist editor (title, tasks, who may add / mark) → store.sendChecklist. */
@Composable
internal fun ChatChecklistSheet(store: KxStore, chatId: Long, onDismiss: () -> Unit, toast: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    val tasks = remember { mutableStateListOf("", "") }
    var add by remember { mutableStateOf(true) }
    var mark by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    InChatSheet(onDismiss, "Список задач", if (!store.me.premium) "Telegram разрешает создавать списки с Premium" else null) {
        Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetField(title, { title = it }, "Название списка")
            tasks.forEachIndexed { i, t ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("☐", style = KxType.style(18.sp, color = Kx.text3))
                    SheetField(t, { tasks[i] = it }, "Задача ${i + 1}", Modifier.weight(1f))
                    if (tasks.size > 1) Box(Modifier.size(34.dp).clip(CircleShape).kxClick("Убрать задачу") { tasks.removeAt(i) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, null, Modifier.size(18.dp), tint = Kx.text4)
                    }
                }
            }
            if (tasks.size < 30) Text("+ Добавить задачу", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).kxClick { tasks.add("") }.padding(horizontal = 8.dp, vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxChip("Другие добавляют", selected = add) { add = !add }
                KxChip("Другие отмечают", selected = mark) { mark = !mark }
            }
        }
        KxButton(if (sending) "Отправка…" else "Отправить список", Modifier.padding(top = 12.dp).fillMaxWidth(), height = 50.dp, icon = Icons.AutoMirrored.Rounded.Send, enabled = !sending) {
            val list = tasks.map { it.trim() }.filter { it.isNotEmpty() }
            if (title.isBlank() || list.isEmpty()) { toast("Нужны название и хотя бы одна задача"); return@KxButton }
            sending = true
            scope.launch {
                val e = store.sendChecklist(chatId, title.trim(), list, add, mark)
                sending = false
                if (e != null) toast(e) else { toast("Список отправлен"); onDismiss() }
            }
        }
    }
}

// ---- Emoji / stickers / GIF panel ------------------------------------------------------------------------------------

private class EmojiCat(val emoji: String, val label: String, val chars: String)
private val EmojiCats = listOf(
    EmojiCat("😀", "Смайлы", "😀😃😄😁😆😅😂🤣🥲🥹😊😇🙂😉😌😍🥰😘😋😛😜🤪😎🤩🥳😏😒😔😢😭😤😡🤯😱🤗🤔🫡🤫😴🙄😬🥺😮🤭🙃"),
    EmojiCat("👍", "Жесты", "👍👎👌✌️🤞🤟🤘🤙👋🙌👏🙏🤝💪👀🫶❤️‍🔥💋👊✊🫡"),
    EmojiCat("🐱", "Животные", "🐶🐱🐭🐹🐰🦊🐻🐼🐨🐯🦁🐮🐷🐸🐵🐔🐧🐦🦆🦉🐺🐴🦄🐝🦋🐢🐍🐙🦀🐬🐳"),
    EmojiCat("🍕", "Еда", "🍏🍎🍊🍋🍌🍉🍇🍓🍒🍑🥭🍍🥥🥝🍅🥑🥦🌽🥕🥐🍞🧀🍳🥞🍔🍟🍕🌭🌮🍣🍩🍪🎂☕🍵"),
    EmojiCat("⚽", "Активности", "⚽🏀🏈⚾🎾🏐🎱🏓🥊🎯🎳🎮🎲🧩🎸🎹🎤🎧🎬🎨🏆🥇🎉🎁🎈"),
    EmojiCat("❤️", "Символы", "❤️🧡💛💚💙💜🖤🤍🤎💔❣️💕💞💓💗💖💘💝✨⭐🌟💫🔥💯✅❌⚠️🔔🎵"),
)

/** A sticker / set icon: the sticker's still image (WEBP, or the thumbnail of an animated one); TGS as a still frame. */
@Composable
internal fun KxStickerCell(st: KxSticker, store: KxStore, size: Dp) {
    val path = st.path ?: store.takeIf { st.fileId != 0 && !st.gif }?.fileState(st.fileId)?.path
    if (st.path == null && path == null && st.fileId != 0 && !st.gif) LaunchedEffect(st.fileId) { store.download(st.fileId) }
    val type = remember(path) { path?.let(::stickerType) ?: -1 }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        when (type) {
            0 -> KxImage(path, Modifier.fillMaxSize(), maxPx = 256, contentScale = ContentScale.Fit)
            1 -> KxLottie(path!!, Modifier.fillMaxSize(), play = false)
            else -> Text(st.emoji.ifEmpty { "🙂" }, style = KxType.style((size.value * .55f).sp))
        }
    }
}

/**
 * ☺ panel under the real composer: emoji (into the text), stickers (favourites / recent + installed sets, real images,
 * set icons = set covers) and saved GIFs; a tap on a sticker / GIF sends it to this chat (as a reply when one is set).
 */
@Composable
internal fun ChatEmojiPanel(store: KxStore, chatId: Long, replyTo: Long?, height: Dp, onEmoji: (String) -> Unit, onBackspace: () -> Unit, onSent: () -> Unit, onOpen: (String) -> Unit) {
    var mode by rememberSaveableInt(0)
    var cat by remember(mode) { mutableIntStateOf(0) }
    val sets by produceState(emptyList<KxStickerSet>(), mode) { if (mode == 1 && value.isEmpty()) value = runCatching { store.stickerSets() }.getOrDefault(emptyList()) }
    val stickers by produceState<List<KxSticker>?>(null, mode, cat, sets.size) {
        value = null
        if (mode == 1) value = runCatching {
            if (cat == 0) store.favoriteStickers() + store.recentStickers() else sets.getOrNull(cat - 1)?.let { store.stickers(it.id) }.orEmpty()
        }.getOrDefault(emptyList()).distinctBy { it.id }
    }
    val gifs by produceState<List<KxSticker>?>(null, mode) { value = null; if (mode == 2) value = runCatching { store.savedGifs() }.getOrDefault(emptyList()) }
    Column(Modifier.fillMaxWidth().height(height).background(Color(0xFF0F1311)).drawBehind { drawLine(Kx.w(.08f), Offset(0f, 0f), Offset(size.width, 0f), 1f) }) {
        // Category / set strip.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            when (mode) {
                0 -> EmojiCats.forEachIndexed { i, c -> PanelTab(i == cat, c.label, { cat = i }) { Text(c.emoji, style = KxType.style(21.sp)) } }
                1 -> {
                    PanelTab(cat == 0, "Избранные и недавние", { cat = 0 }) { Icon(Icons.Rounded.Star, null, Modifier.size(21.dp), tint = if (cat == 0) Kx.accent else Kx.text4) }
                    sets.forEachIndexed { i, s -> PanelTab(cat == i + 1, s.title, { cat = i + 1 }) {
                        val cover = s.cover
                        if (cover != null) KxStickerCell(cover, store, 28.dp) else Text(s.title.take(1), style = KxType.style(15.sp, FontWeight.Bold))
                    } }
                    PanelTab(false, "Наборы стикеров", { onOpen("StickerManage") }) { Icon(Icons.Outlined.Settings, null, Modifier.size(20.dp), tint = Kx.text4) }
                }
                else -> Text("Сохранённые GIF", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp))
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (mode) {
                0 -> LazyVerticalGrid(GridCells.Fixed(8), Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                    items(emojiSplitAll(EmojiCats[cat].chars)) { e ->
                        Box(Modifier.height(42.dp).clip(RoundedCornerShape(10.dp)).kxClick(e) { onEmoji(e) }, contentAlignment = Alignment.Center) { Text(e, style = KxType.style(26.sp)) }
                    }
                }
                1 -> {
                    val list = stickers
                    if (list == null) PanelNote("Загрузка…")
                    else if (list.isEmpty()) PanelNote(if (cat == 0) "Здесь появятся избранные и недавние стикеры" else "В наборе нет стикеров")
                    else LazyVerticalGrid(GridCells.Fixed(5), Modifier.fillMaxSize().padding(horizontal = 6.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                        items(list, key = { it.id }) { s ->
                            Box(Modifier.height(72.dp).clip(RoundedCornerShape(12.dp)).kxClick("Стикер ${s.emoji}") { store.sendSticker(chatId, s.id, replyTo); onSent() }, contentAlignment = Alignment.Center) {
                                KxStickerCell(s, store, 62.dp)
                            }
                        }
                    }
                }
                else -> {
                    val list = gifs
                    if (list == null) PanelNote("Загрузка…")
                    else if (list.isEmpty()) PanelNote("Сохранённых GIF пока нет")
                    else LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                        items(list, key = { it.id }) { g ->
                            Box(Modifier.height(96.dp).clip(RoundedCornerShape(12.dp)).background(Kx.w(.06f)).kxClick("GIF") { store.sendSticker(chatId, g.id, replyTo); onSent() },
                                contentAlignment = Alignment.Center) {
                                Text("GIF", style = KxType.style(12.sp, FontWeight.Bold, Kx.text4))
                                KxImage(g.path, Modifier.matchParentSize(), maxPx = 320)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                listOf("Эмодзи", "Стикеры", "GIF").forEachIndexed { i, label ->
                    Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (i == mode) Kx.accent.copy(alpha = .18f) else Color.Transparent)
                        .kxClick(label) { mode = i }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text(label, style = KxType.style(13.sp, FontWeight.Bold, if (i == mode) Kx.accent else Kx.text3))
                    }
                }
            }
            if (mode == 0) Box(Modifier.size(40.dp).clip(CircleShape).kxClick("Стереть", onClick = onBackspace), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(20.dp), tint = Kx.text3)
            }
        }
    }
}

@Composable
private fun rememberSaveableInt(v: Int) = androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(v) }

@Composable
private fun PanelTab(on: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(42.dp, 38.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Color.Transparent).kxClick(label, onClick = onClick),
        contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun PanelNote(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(text, style = KxType.style(14.sp, color = Kx.text4), textAlign = TextAlign.Center) }
}

/** Grapheme clusters of [s] (one emoji each, ZWJ sequences and flags kept whole). */
internal fun emojiSplitAll(s: String): List<String> {
    val it = java.text.BreakIterator.getCharacterInstance(); it.setText(s)
    val out = ArrayList<String>(); var start = it.first(); var end = it.next()
    while (end != java.text.BreakIterator.DONE) { val g = s.substring(start, end); if (g.isNotBlank()) out += g; start = end; end = it.next() }
    return out
}

/** fixer-chat4: [s] without its last grapheme (emoji backspace in the panel). */
internal fun kxDropLastGrapheme(s: String): String {
    if (s.isEmpty()) return s
    val it = java.text.BreakIterator.getCharacterInstance(); it.setText(s)
    val prev = it.preceding(s.length)
    return if (prev == java.text.BreakIterator.DONE) "" else s.substring(0, prev)
}
