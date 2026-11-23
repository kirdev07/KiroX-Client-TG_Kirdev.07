package com.kirox.app.ui.v3.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.KxScreenFn
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.data.KxFeatureJump
import com.kirox.app.ui.v3.data.KxOnDeviceSpeech
import com.kirox.app.ui.v3.data.KxOnDeviceTranslate
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.screens.chat.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.screens.tools.TToast
import com.kirox.app.ui.v3.screens.tools.TToastHost
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * features: the «Функции kiroX» boards — templates, bookmarks, notes, reminders and the on-device recognition /
 * translation settings. Registered in KxRegistry (FeatureScreens); entry: Settings → «Функции kiroX».
 */

val FeatureScreens: Map<String, KxScreenFn> = mapOf(
    "KxTemplates" to { ctx -> TemplatesScreen(ctx) },
    "KxBookmarks" to { ctx -> BookmarksScreen(ctx) },
    "KxNotes" to { ctx -> NotesScreen(ctx) },
    "KxReminders" to { ctx -> RemindersScreen(ctx) },
    "KxOnDevice" to { ctx -> OnDeviceScreen(ctx) },
)

/** Section «Функции kiroX» of the Settings tab (hook in SettingsScreen): ONE group (design-critique B11), Музыка and the kiroX board included. */
@Composable
fun KxFeatureSettingsSection(store: KxStore, onOpen: (String) -> Unit) {
    val f = store.features
    Column(Modifier.kxRise(240)) {
        KxGroupCaption("Функции kiroX")
        val rows = listOf(
            FRow(Icons.Outlined.Bolt, "Режим призрака и другое", null, "Features"),
            FRow(Icons.Outlined.TextSnippet, "Шаблоны ответов", f.templates.size.takeIf { it > 0 }?.toString(), "KxTemplates"),
            FRow(Icons.Outlined.StarBorder, "Закладки", f.bookmarks.size.takeIf { it > 0 }?.toString(), "KxBookmarks"),
            FRow(Icons.AutoMirrored.Outlined.StickyNote2, "Заметки о чатах и людях", f.notes.size.takeIf { it > 0 }?.toString(), "KxNotes"),
            FRow(Icons.Outlined.Alarm, "Напоминания", f.reminders.size.takeIf { it > 0 }?.toString(), "KxReminders"),
            FRow(Icons.Outlined.Translate, "Расшифровка и перевод", if (store.me.premium) "Premium" else "на телефоне", "KxOnDevice"),
            FRow(Icons.Outlined.MusicNote, "Музыка", null, "Music"),
        )
        KxGroup {
            rows.forEach { r ->
                com.kirox.app.ui.v3.screens.settings.SNavRow(r.icon, r.title, null, RoundedCornerShape(0.dp), tint = KxTint.features, value = r.value) { onOpen(r.route) }
            }
        }
    }
}
private class FRow(val icon: ImageVector, val title: String, val value: String?, val route: String)

/** The personal note card in a person's profile (hook in ProfileScreen). */
@Composable
fun KxProfileNote(store: KxStore, chatId: Long, title: String) {
    val f = store.features
    var editing by remember { mutableStateOf(false) }
    val note = f.note(chatId)
    Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f))
        .kxClick(if (note.isBlank()) "Добавить заметку" else "Изменить заметку") { editing = true }.padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.AutoMirrored.Outlined.StickyNote2, null, Modifier.size(20.dp), tint = Kx.accent)
        Column(Modifier.weight(1f)) {
            Text(if (note.isBlank()) "Добавить заметку" else note, style = ts(14, color = if (note.isBlank()) Kx.text3 else Kx.text, lh = 19.sp))
            Text("Заметка kiroX · только на этом телефоне", style = ts(11, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
        }
    }
    if (editing) NoteEditor(title, note, onDismiss = { editing = false }) { f.setNote(chatId, title, it); editing = false }
}

@Composable
private fun NoteEditor(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var t by remember { mutableStateOf(initial) }
    // A dialog window: the profile is a scrolling column, a full-size sheet cannot sit inside it.
    androidx.compose.ui.window.Dialog(onDismiss, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
    Box(Modifier.fillMaxSize()) {
        BSheet(true, onDismiss) {
            Column(Modifier.imePadding()) {
                Text("Заметка kiroX · $title", style = ts(18, W800), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Видите только вы. Только на этом телефоне — в Telegram не отправляется.", style = ts(12, color = Kx.text4), modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                BTextArea(t, { t = it.take(4000) }, "Что важно помнить об этом чате", 140.dp)
                ChPrimary("Сохранить", modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) { onSave(t) }
            }
        }
    }
    }
}

// ---------------------------------------------------------------- Templates

@Composable
private fun TemplatesScreen(ctx: KxScreenCtx) {
    val f = ctx.store.features
    var editing by remember { mutableStateOf<KxTemplate?>(null) }
    var key by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val keyTaken = f.templates.any { it.key == key.trim().lowercase() && it.id != editing?.id }
    val canSave = KxTemplateVars.validKey(key.trim()) && body.isNotBlank() && !keyTaken
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            KxTopBar("Шаблоны ответов", onBack = ctx.back)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (f.templates.isNotEmpty()) item {
                    Row(Modifier.padding(bottom = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .07f)).border(1.dp, Kx.accent.copy(alpha = .18f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Bolt, null, Modifier.size(20.dp), tint = Kx.accent)
                        Text("Наберите «/» в чате. Нажмите на шаблон, чтобы изменить; порядок — перетаскиванием за ручку.",
                            style = ts(13, color = Kx.text2, lh = 18.sp))
                    }
                }
                // A4: the shared empty state, its action inside (the bottom button shows once there is a list).
                if (f.templates.isEmpty()) item {
                    KxEmpty(Icons.Outlined.Bolt, "Шаблонов пока нет", "Сохраните частый ответ и отправляйте его одной командой «/».", Modifier.padding(top = 24.dp),
                        action = "Новый шаблон", actionIcon = Icons.Outlined.Add) { editing = KxTemplate(0, "", ""); key = ""; body = "" }
                }
                // critique-B: tap = edit sheet (delete inside), order by dragging the handle (was ▲▼ per row).
                itemsIndexed(f.templates, key = { _, t -> t.id }) { _, t ->
                    var dy by remember { mutableFloatStateOf(0f) }
                    var rowH by remember { mutableFloatStateOf(1f) }
                    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
                    val dragging = dy != 0f
                    Row(Modifier.fillMaxWidth().zIndex(if (dragging) 1f else 0f).graphicsLayer { translationY = dy }
                        .onSizeChanged { rowH = it.height.toFloat() }
                        .shadow(if (dragging) 12.dp else 0.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(if (dragging) Kx.surface3 else Kx.w(.045f))
                        .kxClick("Изменить шаблон /${t.key}") { editing = t; key = t.key; body = t.text }
                        .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("/${t.key}", style = ts(13, W800, Kx.accent))
                            Text(t.text, style = ts(14, color = Kx.text2, lh = 19.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (t.uses > 0) Text("использован ${t.uses} " + com.kirox.app.ui.v3.data.KxFormat.plural(t.uses, "раз", "раза", "раз"), style = ts(11, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                        }
                        Box(Modifier.size(44.dp).clip(CircleShape).semantics { contentDescription = "Перетащите, чтобы изменить порядок" }
                            .pointerInput(t.id) {
                                detectDragGestures(onDragEnd = { dy = 0f }, onDragCancel = { dy = 0f }) { ch, d ->
                                    ch.consume()
                                    dy += d.y
                                    val step = rowH + gap
                                    val i = f.templates.indexOfFirst { it.id == t.id }
                                    if (dy > step / 2 && i < f.templates.lastIndex) { f.moveTemplate(t.id, i + 1); dy -= step }
                                    else if (dy < -step / 2 && i > 0) { f.moveTemplate(t.id, i - 1); dy += step }
                                }
                            }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.DragHandle, null, tint = Kx.text3)
                        }
                    }
                }
            }
        }
        if (f.templates.isNotEmpty()) ChPrimary("Новый шаблон", modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 22.dp).fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(18.dp), spotColor = Kx.accentDeep), icon = Icons.Outlined.Add) { editing = KxTemplate(0, "", ""); key = ""; body = "" }
        BSheet(editing != null, { editing = null }) {
            Column(Modifier.imePadding()) {
                Text(if (editing?.id == 0L) "Новый шаблон" else "Изменить шаблон", style = ts(18, W800), modifier = Modifier.padding(bottom = 12.dp))
                Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f))
                    .border(1.dp, if (key.isNotEmpty() && (!KxTemplateVars.validKey(key.trim()) || keyTaken)) Kx.danger.copy(alpha = .5f) else Color.Transparent, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("/", style = ts(16, W800, Kx.accent))
                    ChInput(key, { key = it.filterNot { c -> c.isWhitespace() || c == '/' }.take(24) }, "короткое имя", Modifier.weight(1f), size = 16)
                }
                if (keyTaken) Text("Такое имя уже есть", style = ts(12, color = Kx.danger), modifier = Modifier.padding(start = 6.dp, top = 4.dp))
                BTextArea(body, { body = it.take(4000) }, "Текст ответа", 120.dp, Modifier.padding(top = 8.dp))
                Row(Modifier.padding(top = 8.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KxTemplateVars.names.forEach { (v, hint) ->
                        Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).kxClick("Вставить $hint") { body = (body.trimEnd() + " " + v).trimStart() }
                            .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text(v, style = ts(12, W700, Kx.text2)) }
                    }
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ((editing?.id ?: 0L) != 0L) Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Kx.danger.copy(alpha = .1f)).kxClick("Удалить") { confirm = true },
                        contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Delete, null, tint = Kx.danger) }
                    ChPrimary("Сохранить", enabled = canSave, modifier = Modifier.weight(1f)) {
                        val e = editing ?: return@ChPrimary
                        if (canSave) { f.saveTemplate(e.copy(key = key.trim(), text = body)); editing = null }
                    }
                }
            }
        }
        ChConfirm(if (confirm) "Удалить шаблон?" else null, "Он пропадёт из подсказок «/».", "Удалить", { confirm = false }) {
            confirm = false; editing?.let { f.deleteTemplate(it.id) }; editing = null
        }
    }
}


// ---------------------------------------------------------------- Bookmarks

@Composable
private fun BookmarksScreen(ctx: KxScreenCtx) {
    val f = ctx.store.features
    var q by remember { mutableStateOf("") }
    val toast = remember { TToast() }
    val shown = f.bookmarks.filter { q.isBlank() || it.text.contains(q, true) || it.chatTitle.contains(q, true) || it.sender.contains(q, true) }
    val byChat = shown.groupBy { it.chatId }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            KxTopBar("Закладки", subtitle = "Из всех чатов · на телефоне", onBack = ctx.back)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.06f)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text3)
                ChInput(q, { q = it }, "Поиск по закладкам", Modifier.weight(1f))
            }
            if (f.bookmarks.isEmpty()) Empty(Icons.Outlined.StarBorder, "Закладок пока нет", "Удерживайте сообщение в любом чате → «В закладки». Закладки из всех чатов собираются здесь и хранятся только на этом телефоне.")
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                byChat.forEach { (chatId, list) ->
                    item(key = "h$chatId") { Text(list.first().chatTitle, style = ts(13, W800, Kx.accent), modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp)) }
                    items(list, key = { "${it.chatId}:${it.messageId}" }) { b ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.045f))
                            .kxClick("Открыть сообщение") { KxFeatureJump.request(b.chatId, b.messageId); ctx.openChat(b.chatId) }.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Star, null, Modifier.size(18.dp), tint = Color(0xFFFFD58A))
                            Column(Modifier.weight(1f)) {
                                Text(listOfNotNull(b.sender.takeIf { it.isNotBlank() && it != b.chatTitle }, when_(b.added)).joinToString(" · "), style = ts(12, W700, Kx.text3), maxLines = 1)
                                Text(b.text, style = ts(14, lh = 19.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Убрать из закладок") {
                                f.removeBookmark(b.chatId, b.messageId)
                                toast.show("Убрано из закладок", Icons.Outlined.StarBorder, act = "Вернуть") { f.addBookmark(b) }
                            }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(20.dp), tint = Kx.text4) }
                        }
                    }
                }
                if (shown.isEmpty()) item { KxEmpty(Icons.Outlined.Search, "Ничего не нашлось", "Попробуйте другое слово или имя чата.", Modifier.padding(top = 24.dp)) }
            }
        }
        TToastHost(toast, 34.dp)
    }
}

private fun when_(ms: Long): String = SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("ru")).format(Date(ms))

/** «Сегодня, 18:00» / «Завтра, 02:04» / «24 нояб., 02:04». */
private fun relWhen(ms: Long, now: Long = System.currentTimeMillis()): String {
    fun day(t: Long) = java.util.Calendar.getInstance().apply { timeInMillis = t }.let { it.get(java.util.Calendar.YEAR) * 400 + it.get(java.util.Calendar.DAY_OF_YEAR) }
    val time = SimpleDateFormat("HH:mm", Locale.forLanguageTag("ru")).format(Date(ms))
    return when (day(ms) - day(now)) { 0 -> "Сегодня, $time"; 1 -> "Завтра, $time"; -1 -> "Вчера, $time"; else -> when_(ms) }
}

/** A4: the shared [KxEmpty], filling the free height. */
@Composable
private fun ColumnScope.Empty(icon: ImageVector, title: String, text: String) = KxEmpty(icon, title, text, Modifier.weight(1f))

// ---------------------------------------------------------------- Notes

@Composable
private fun NotesScreen(ctx: KxScreenCtx) {
    val f = ctx.store.features
    var edit by remember { mutableStateOf<KxNote?>(null) }
    var pick by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            KxTopBar("Заметки kiroX", subtitle = "Только на этом телефоне", onBack = ctx.back)
            val list = f.notes.values.sortedByDescending { it.updated }
            if (list.isEmpty()) Column(Modifier.weight(1f)) {
                Empty(Icons.AutoMirrored.Outlined.StickyNote2, "Заметок пока нет", "Нажмите «Новая заметка» и выберите чат — или откройте чат → ⋮ → «Заметка о чате».")
            }
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.chatId }) { n ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f)).kxClick { edit = n }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(n.title.ifBlank { "Чат" }, style = ts(15, W800), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Открыть чат ${n.title}") { ctx.openChat(n.chatId) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(20.dp), tint = Kx.accent)
                            }
                        }
                        Text(n.text, style = ts(14, color = Kx.text2, lh = 19.sp), maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        Text(when_(n.updated), style = ts(11, color = Kx.text4), modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        ChPrimary("Новая заметка", modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 22.dp).fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(18.dp), spotColor = Kx.accentDeep), icon = Icons.Outlined.Add) { pick = true }
        // «+ Заметка»: pick a chat, then the same editor.
        BSheet(pick, { pick = false }) {
            var q by remember { mutableStateOf("") }
            Text("Заметка о чате", style = ts(18, W800), modifier = Modifier.padding(bottom = 10.dp))
            Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.06f)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text3)
                ChInput(q, { q = it }, "Найти чат", Modifier.weight(1f))
            }
            val chats = ctx.store.chats.filter { q.isBlank() || it.title.contains(q, true) }.take(40)
            Column(Modifier.padding(top = 8.dp).heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                chats.forEach { c ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).kxClick(c.title) {
                        pick = false; edit = f.notes[c.id] ?: KxNote(c.id, c.title, "", System.currentTimeMillis())
                    }.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(20.dp), tint = Kx.text3)
                        Text(c.title, style = ts(15), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (f.notes.containsKey(c.id)) Text("есть заметка", style = ts(12, color = Kx.text4))
                    }
                }
                if (chats.isEmpty()) Text("Ничего не нашлось", style = ts(14, color = Kx.text3), modifier = Modifier.padding(8.dp))
            }
        }
        edit?.let { n -> NoteEditor(n.title, n.text, onDismiss = { edit = null }) { f.setNote(n.chatId, n.title, it); edit = null } }
    }
}

// ---------------------------------------------------------------- Reminders

@Composable
internal fun RemindersScreen(ctx: KxScreenCtx) {
    val f = ctx.store.features
    val toast = remember { TToast() }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            KxTopBar("Напоминания", subtitle = "Прочитать позже · на этом телефоне", onBack = ctx.back)
            if (f.reminders.isEmpty()) Empty(Icons.Outlined.Alarm, "Напоминаний нет", "Удерживайте сообщение → «Напомнить прочитать» и выберите время.")
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(f.reminders, key = { it.id }) { r ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f))
                        .kxClick("Открыть сообщение") { KxFeatureJump.request(r.chatId, r.messageId); ctx.openChat(r.chatId) }.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Alarm, null, Modifier.size(20.dp), tint = Kx.accent)
                        Column(Modifier.weight(1f)) {
                            Text(relWhen(r.at) + " · " + r.chatTitle, style = ts(12, W700, Kx.accent), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(r.text, style = ts(14, lh = 19.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        // critique-B: snooze one hour; remove with «Вернуть».
                        Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Отложить на час") {
                            val made = f.addReminder(r.copy(at = maxOf(r.at, System.currentTimeMillis()) + 3_600_000L))
                            toast.show("Напомню " + relWhen(made.at).replaceFirstChar { it.lowercase() }, Icons.Outlined.Snooze)
                        }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Snooze, null, Modifier.size(20.dp), tint = Kx.text3) }
                        Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Отменить напоминание") {
                            f.removeReminder(r.id)
                            toast.show("Напоминание отменено", Icons.Outlined.Alarm, act = "Вернуть") { f.addReminder(r) }
                        }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(20.dp), tint = Kx.text4) }
                    }
                }
            }
        }
        TToastHost(toast, 34.dp)
    }
}

// ---------------------------------------------------------------- On-device recognition and translation

@Composable
private fun OnDeviceScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val f = store.features
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val premium = store.me.premium
    var models by remember { mutableStateOf<Set<String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { models = if (demo) setOf("en", "ru") else KxOnDeviceTranslate.downloaded() }
    val speechProblem = if (demo) null else remember { KxOnDeviceSpeech.problem(context) }
    fun sw(key: String) = f.settings[key] != 0
    fun flip(key: String) = f.putSetting(key, if (sw(key)) 0 else 1)
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            KxTopBar("Расшифровка и перевод", onBack = ctx.back)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
                Text(if (premium) "У вас Telegram Premium: голосовые и переводы обрабатывает Telegram, как в официальном приложении. Настройки ниже нужны только без Premium."
                    else "Без Telegram Premium kiroX сам превращает голосовые и кружки в текст и переводит сообщения — прямо на телефоне. Текст и звук никуда не отправляются; результат помечен «на телефоне».",
                    style = ts(13, color = Kx.text2, lh = 19.sp), modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .07f))
                        .border(1.dp, Kx.accent.copy(alpha = .18f), RoundedCornerShape(18.dp)).padding(14.dp))
                KxSectionTitle("Голосовые и кружки → текст", Modifier.padding(start = 0.dp))
                KxCard {
                    KxRow("Распознавать на телефоне", subtitle = speechProblem ?: "Распознавание Android, без интернета", icon = Icons.Outlined.GraphicEq,
                        trailing = { KxSwitch(sw(KxFeatureKeys.SPEECH_ON_DEVICE)) }) { flip(KxFeatureKeys.SPEECH_ON_DEVICE) }
                    val li = f.settings[KxFeatureKeys.SPEECH_LANG] ?: 0
                    KxRow("Язык голосовых", subtitle = KxFeatureLangs.speech.getOrElse(li) { KxFeatureLangs.speech[0] }.second, icon = Icons.Outlined.Language,
                        trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4) }) { f.putSetting(KxFeatureKeys.SPEECH_LANG, (li + 1) % KxFeatureLangs.speech.size) }
                    KxRow("Скачать язык для офлайн-распознавания", subtitle = "Пакет скачает система, один раз", icon = Icons.Outlined.Download) {
                        if (demo) ctx.toast("В демо ничего не скачивается")
                        else ctx.toast(KxOnDeviceSpeech.downloadModel(context, KxOnDeviceSpeech.langTag(li)) ?: "Загрузка началась — следите за уведомлением системы")
                    }
                }
                KxSectionTitle("Перевод сообщений", Modifier.padding(start = 0.dp))
                KxCard {
                    KxRow("Переводить на телефоне", subtitle = "Языковой пакет ~30 МБ на язык, скачивается один раз", icon = Icons.Outlined.Translate,
                        trailing = { KxSwitch(sw(KxFeatureKeys.TR_ON_DEVICE)) }) { flip(KxFeatureKeys.TR_ON_DEVICE) }
                    KxRow("Скачивать пакеты только по Wi-Fi", icon = Icons.Outlined.Wifi, trailing = { KxSwitch(sw(KxFeatureKeys.TR_WIFI_ONLY)) }) { flip(KxFeatureKeys.TR_WIFI_ONLY) }
                    val to = listOf("ru", "en", "kk").getOrElse(store.pref(KxPrefs.TRANSLATE_TO)) { "ru" }
                    KxRow(if (busy) "Скачивается…" else "Скачать пакет языка перевода", subtitle = "Язык перевода: ${KxFeatureLangs.nominative(to)}", icon = Icons.Outlined.Download) {
                        if (demo || busy) return@KxRow
                        busy = true
                        scope.launch {
                            ctx.toast(KxOnDeviceTranslate.prepare(to, sw(KxFeatureKeys.TR_WIFI_ONLY)) ?: "Модель готова")
                            models = KxOnDeviceTranslate.downloaded(); busy = false
                        }
                    }
                }
                KxSectionTitle("Скачанные языки перевода", Modifier.padding(start = 0.dp))
                KxCard {
                    val m = models
                    if (m == null) KxRow("Загрузка…")
                    else if (m.isEmpty()) KxRow("Пока ни одного", subtitle = "Пакет скачается при первом переводе, после вашего согласия")
                    else m.sorted().forEach { code ->
                        // critique-B: the name in the nominative; English is built into Google's on-phone translator and cannot be removed.
                        KxRow(KxFeatureLangs.nominative(code).replaceFirstChar { it.uppercase() }, subtitle = if (code == "en") "Встроен · не удаляется" else "≈ 30 МБ",
                            icon = Icons.Outlined.Translate, trailing = {
                                if (code != "en") Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Удалить язык") {
                                    if (!demo) scope.launch { KxOnDeviceTranslate.delete(code)?.let(ctx.toast); models = KxOnDeviceTranslate.downloaded() }
                                }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Delete, null, tint = Kx.text3) }
                            })
                    }
                }
            }
        }
    }
}
