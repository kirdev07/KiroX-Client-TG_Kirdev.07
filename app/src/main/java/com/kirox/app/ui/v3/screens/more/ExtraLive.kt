package com.kirox.app.ui.v3.screens.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxConfirm
import com.kirox.app.ui.v3.components.KxConfirmDialog
import com.kirox.app.ui.v3.components.KxImage
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.model.KxChatPrefs
import com.kirox.app.ui.v3.model.KxEntityType
import com.kirox.app.ui.v3.model.KxFound
import com.kirox.app.ui.v3.model.KxPublicChat
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * wave3-chat: real-account versions of the extra chat boards. Each opens from its board when the store is not
 * KxDemoStore; the demo keeps the mockup. Arg conventions as in Chat3Live.kt.
 */

@Composable
private fun LiveFrame(ctx: KxScreenCtx, title: String, sub: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            MoTitleHeader(title, ctx.back, sub = sub)
            content()
        }
    }
}

@Composable
private fun Field(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier = Modifier, single: Boolean = true) {
    Box(modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart) { MoInput(value, onValue, hint, Modifier.fillMaxWidth(), singleLine = single) }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) =
    MoPill(text, height = 34.dp, size = 12, bg = if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.06f), ink = if (on) Kx.accent else Kx.text2, onClick = onClick)

@Composable
private fun FoundList(items: List<KxFound>, modifier: Modifier, onTap: (KxFound) -> Unit) {
    LazyColumn(modifier, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { "${it.chatId}:${it.messageId}" }) { f ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick { onTap(f) }.padding(14.dp)) {
                Row { Text(f.chatTitle, style = mts(13, W8, Kx.accent), modifier = Modifier.weight(1f), maxLines = 1); Text(f.time, style = mts(11, color = MoMuted)) }
                Text(f.text, style = mts(14), maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ---- Checklist ---------------------------------------------------------------------------------

/** Checklist: with chat:message → that checklist (mark tasks); with a chat only → create one (Premium in Telegram). */
@Composable
internal fun ChecklistLive(ctx: KxScreenCtx, chatId: Long, messageId: Long?) {
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    if (messageId != null) {
        val list = c3Load(messageId, reload) { ctx.store.checklist(chatId, messageId) }
        LiveFrame(ctx, list?.title ?: "Список задач", list?.let { "${it.tasks.count { t -> t.done }} из ${it.tasks.size} готово" }) {
            if (list == null) C3Empty("Загрузка… (или это не список задач)", Modifier.weight(1f))
            else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(list.tasks, key = { it.id }) { t ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).kxClick {
                        if (!list.canMark) { ctx.toast("Автор не разрешил отмечать задачи"); return@kxClick }
                        scope.launch { ctx.store.markChecklist(chatId, messageId, if (t.done) emptyList() else listOf(t.id), if (t.done) listOf(t.id) else emptyList())?.let { ctx.toast(it) }; reload++ }
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (t.done) "☑" else "☐", style = mts(20, color = if (t.done) Kx.accent else MoSub))
                        Column(Modifier.weight(1f)) {
                            Text(t.text, style = mts(15))
                            if (t.doneBy.isNotBlank()) Text("отметил(а): ${t.doneBy}", style = mts(11, color = MoMuted))
                        }
                    }
                }
            }
        }
        return
    }
    var title by remember { mutableStateOf("") }
    val tasks = remember { mutableStateListOf("", "") }
    var add by remember { mutableStateOf(true) }
    var mark by remember { mutableStateOf(true) }
    LiveFrame(ctx, "Новый список задач", "в «${ctx.store.header(chatId).title}»") {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!ctx.store.me.premium) Text("Telegram разрешает создавать списки задач только с Premium.", style = mts(12, color = Kx.premium))
            Field(title, { title = it }, "Название списка")
            tasks.forEachIndexed { i, t -> Field(t, { tasks[i] = it }, "Задача ${i + 1}") }
            MoPill("+ задача", height = 36.dp) { if (tasks.size < 30) tasks.add("") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(if (add) "✓ Другие добавляют задачи" else "Другие добавляют задачи", add) { add = !add }
                Chip(if (mark) "✓ Другие отмечают" else "Другие отмечают", mark) { mark = !mark }
            }
        }
        MoPill("Отправить список", Modifier.fillMaxWidth().padding(16.dp), height = 50.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent, size = 15) {
            val list = tasks.filter { it.isNotBlank() }
            if (title.isBlank() || list.isEmpty()) { ctx.toast("Нужны название и хотя бы одна задача"); return@MoPill }
            scope.launch { val e = ctx.store.sendChecklist(chatId, title.trim(), list, add, mark); ctx.toast(e ?: "Список отправлен"); if (e == null) ctx.back() }
        }
    }
}

// ---- Memories ----------------------------------------------------------------------------------

/** Memories: photos and videos sent on this day in earlier years (store.memories); a tap opens the chat at that message. */
@Composable
internal fun MemoriesLive(ctx: KxScreenCtx) {
    val list = c3Load(Unit) { ctx.store.memories() }
    LiveFrame(ctx, "В этот день", "Фото и видео из прошлых лет") {
        when {
            list == null -> C3Empty("Ищем воспоминания…", Modifier.weight(1f))
            list.isEmpty() -> C3Empty("В этот день в прошлые годы фото и видео не было", Modifier.weight(1f))
            else -> FoundList(list, Modifier.weight(1f)) { f -> ctx.openChat(f.chatId); ctx.store.jumpTo(f.chatId, f.messageId) }
        }
    }
}

// ---- Explore -----------------------------------------------------------------------------------

/** Explore: recommended channels, public search and (with a chat arg) similar channels; join / open. */
@Composable
internal fun ExploreLive(ctx: KxScreenCtx, chatId: Long?) {
    var tab by remember { mutableIntStateOf(if (chatId != null) 2 else 0) }
    var query by remember { mutableStateOf("") }
    var list by remember { mutableStateOf<List<KxPublicChat>?>(null) }
    val joined = remember { mutableStateListOf<Long>() }
    LaunchedEffect(tab, query) {
        list = null
        if (tab == 1) delay(400)
        list = when (tab) { 0 -> ctx.store.recommendedChats(); 1 -> ctx.store.searchPublicChats(query); else -> chatId?.let { ctx.store.similarChats(it) }.orEmpty() }
    }
    LiveFrame(ctx, "Обзор", "Публичные каналы и группы") {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Рекомендации", tab == 0) { tab = 0 }
            Chip("Поиск", tab == 1) { tab = 1 }
            if (chatId != null) Chip("Похожие", tab == 2) { tab = 2 }
        }
        if (tab == 1) Field(query, { query = it }, "Название или @username", Modifier.padding(horizontal = 16.dp))
        val l = list
        when {
            tab == 1 && query.isBlank() -> C3Empty("Введите запрос", Modifier.weight(1f))
            l == null -> C3Empty("Загрузка…", Modifier.weight(1f))
            l.isEmpty() -> C3Empty("Ничего не нашлось", Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(l, key = { it.id }) { c ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).kxClick { ctx.openChat(c.id) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.accentDeep), contentAlignment = Alignment.Center) {
                            Text(c.title.take(1).uppercase(), style = mts(17, W8, MoInk)); KxImage(c.avatar, Modifier.matchParentSize(), maxPx = 160)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(c.title + if (c.verified) " ✓" else "", style = mts(15, W7), maxLines = 1)
                            Text(listOfNotNull(c.username.takeIf { it.isNotBlank() }?.let { "@$it" },
                                if (c.members > 0) "${c.members} ${if (c.channel) moPlural(c.members, "подписчик", "подписчика", "подписчиков") else moPlural(c.members, "участник", "участника", "участников")}" else null)
                                .joinToString(" · "), style = mts(12, color = MoMuted), maxLines = 1)
                        }
                        if (c.joined || c.id in joined) Text("вы там", style = mts(12, color = MoMuted))
                        else MoPill(if (c.channel) "Подписаться" else "Вступить", height = 34.dp, size = 12, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent) {
                            ctx.store.joinChat(c.id); joined += c.id
                        }
                    }
                }
            }
        }
    }
}

// ---- Birthdays ---------------------------------------------------------------------------------

/** Birthdays: contacts with a birthday (Telegram's close birthdays + a small scan); «Поздравить» opens the chat. */
@Composable
internal fun BirthdaysLive(ctx: KxScreenCtx) {
    val list = c3Load(Unit) { ctx.store.birthdays() }
    val scope = rememberCoroutineScope()
    val months = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    LiveFrame(ctx, "Дни рождения", "Ваши контакты") {
        when {
            list == null -> C3Empty("Загрузка…", Modifier.weight(1f))
            list.isEmpty() -> C3Empty("Сегодня дней рождения нет, и ближайших Telegram не показывает", Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(list, key = { it.userId }) { b ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (b.daysLeft == 0) Kx.accent.copy(alpha = .1f) else Kx.w(.04f)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.accentDeep), contentAlignment = Alignment.Center) {
                            Text(b.name.take(1).uppercase(), style = mts(16, W8, MoInk)); KxImage(b.avatar, Modifier.matchParentSize(), maxPx = 160)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(b.name, style = mts(15, W7), maxLines = 1)
                            Text("${b.day} ${months.getOrElse(b.month - 1) { "" }}" + when (b.daysLeft) { 0 -> " · сегодня 🎉"; 1 -> " · завтра"; else -> " · через ${b.daysLeft} ${moPlural(b.daysLeft, "день", "дня", "дней")}" },
                                style = mts(12, color = MoMuted))
                        }
                        MoPill("Поздравить", height = 34.dp, size = 12, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent) {
                            scope.launch { val chat = ctx.store.privateChat(b.userId); if (chat != null) ctx.openChat(chat) else ctx.toast("Не получилось открыть чат") }
                        }
                    }
                }
            }
        }
    }
}

// ---- Activity ----------------------------------------------------------------------------------

/** Activity: who is typing / recording right now (KxChat.typing) and kiroX's «hide my typing» (tweak ghostNoTyping). */
@Composable
internal fun ActivityLive(ctx: KxScreenCtx) {
    val active = (ctx.store.chats + ctx.store.archivedChats).filter { it.typing != null }
    val hide = ctx.store.tweaks["ghostNoTyping"] == true
    LiveFrame(ctx, "Активность", "Кто сейчас печатает") {
        Row(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick { ctx.store.setTweak("ghostNoTyping", !hide) }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Скрывать, что я печатаю", style = mts(15, W7))
                Text("Собеседники не увидят «печатает…»", style = mts(12, color = MoMuted))
            }
            Text(if (hide) "Вкл" else "Выкл", style = mts(14, W8, if (hide) Kx.accent else MoSub))
        }
        if (active.isEmpty()) C3Empty("Сейчас никто не печатает", Modifier.weight(1f))
        else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(active, key = { it.id }) { c ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).kxClick { ctx.openChat(c.id) }.padding(12.dp)) {
                    Text(c.title, style = mts(15, W7), modifier = Modifier.weight(1f), maxLines = 1)
                    Text("${c.typing}…", style = mts(13, color = Kx.accent))
                }
            }
        }
    }
}

// ---- Sensitive / LinkWarn ----------------------------------------------------------------------

/** Sensitive: Telegram's «show sensitive content» (18+), offered only when Telegram lets this account change it. */
@Composable
internal fun SensitiveLive(ctx: KxScreenCtx) {
    val allowed = ctx.store.sensitiveAllowed
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    LiveFrame(ctx, "Чувствительное", "Медиа 18+ в каналах") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (allowed == null) Text("Telegram не даёт менять эту настройку в этом аккаунте или приложении (например, из-за правил магазина). Её можно изменить в веб-версии Telegram.",
                style = mts(14, color = MoSub, lh = 1.4f))
            else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick {
                if (allowed) ctx.store.allowSensitive(false)
                else confirm = KxConfirm("Показывать медиа 18+?", "Подтвердите, что вам есть 18 лет. Такие фото и видео будут открываться без размытия.", "Мне есть 18", danger = false) { ctx.store.allowSensitive(true) }
            }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Показывать без размытия", style = mts(15, W7)); Text("Фото и видео, отмеченные как 18+", style = mts(12, color = MoMuted)) }
                Text(if (allowed) "Вкл" else "Выкл", style = mts(14, W8, if (allowed) Kx.accent else MoSub))
            }
        }
    }
    confirm?.let { c -> KxConfirmDialog(c) { confirm = null } }
}

/** LinkWarn: the kiroX switch «спрашивать перед подозрительной ссылкой» (KxChatPrefs.LINK_WARN) and a checker for any link. */
@Composable
internal fun LinkWarnLive(ctx: KxScreenCtx) {
    val on = (ctx.store.prefs[KxChatPrefs.LINK_WARN] ?: 1) != 0
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val uri = LocalUriHandler.current
    val verdict = if (url.isBlank()) null else com.kirox.app.ui.v3.data.KxChat3Mapping.linkWarning(text, url)
    LiveFrame(ctx, "Проверка ссылок") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick { ctx.store.putPref(KxChatPrefs.LINK_WARN, if (on) 0 else 1) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Спрашивать перед подозрительной ссылкой", style = mts(15, W7)); Text("Когда текст и адрес не совпадают или адрес похож на подделку", style = mts(12, color = MoMuted)) }
                Text(if (on) "Вкл" else "Выкл", style = mts(14, W8, if (on) Kx.accent else MoSub))
            }
            Text("Проверить ссылку", style = mts(13, W7, Kx.accent))
            Field(text, { text = it }, "Текст ссылки (что видно в сообщении)")
            Field(url, { url = it }, "Адрес (куда ведёт)")
            if (url.isNotBlank()) Text(verdict ?: "Подозрительного не видно", style = mts(14, W7, if (verdict != null) Kx.danger else Kx.accent))
            if (url.isNotBlank()) MoPill("Открыть ${com.kirox.app.ui.v3.data.KxChat3Mapping.host(url) ?: url}", height = 40.dp) {
                runCatching { uri.openUri(if (url.contains("://")) url.trim() else "https://" + url.trim()) }
            }
        }
    }
}

// ---- TypeTranslate / ClipPaste / FilePicker / SmartText / BigEmoji / ViewOnce / MeetTime --------

/** TypeTranslate: write in your language, Telegram translates (store.translateText); send the translation or the original. */
@Composable
internal fun TypeTranslateLive(ctx: KxScreenCtx, chatId: Long) {
    val langs = listOf("en" to "English", "ru" to "Русский", "kk" to "Қазақша", "de" to "Deutsch", "es" to "Español", "tr" to "Türkçe", "zh" to "中文")
    var lang by remember { mutableStateOf("en") }
    var text by remember { mutableStateOf("") }
    var out by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(text, lang) { out = null; if (text.isBlank()) return@LaunchedEffect; busy = true; delay(600); out = ctx.store.translateText(text, lang); busy = false }
    LiveFrame(ctx, "Перевод при наборе", "в «${ctx.store.header(chatId).title}»") {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            langs.forEach { (c, n) -> Chip(n, lang == c) { lang = c } }
        }
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(text, { text = it }, "Напишите сообщение", single = false)
            Box(Modifier.fillMaxWidth().heightIn(min = 80.dp).clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .08f)).padding(14.dp)) {
                Text(when { text.isBlank() -> "Перевод появится здесь"; busy -> "Переводим…"; out == null -> "Перевод не получился"; else -> out!! },
                    style = mts(15, color = if (out != null) Kx.text else MoMuted))
            }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoPill("Отправить оригинал", Modifier.weight(1f), height = 48.dp) { if (text.isNotBlank()) { ctx.store.send(chatId, text.trim()); text = ""; ctx.toast("Отправлено") } }
            MoPill("Отправить перевод", Modifier.weight(1f), height = 48.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent) {
                val t = out; if (t.isNullOrBlank()) ctx.toast("Перевода ещё нет") else { ctx.store.send(chatId, t); text = ""; ctx.toast("Перевод отправлен") }
            }
        }
    }
}

/** ClipPaste: what is on the clipboard now (text, link or a copied image) and sending it to the chat. */
@Composable
internal fun ClipPasteLive(ctx: KxScreenCtx, chatId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    val clip = remember(tick) {
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
    }
    val uri = clip?.uri
    val isImage = uri != null && context.contentResolver.getType(uri)?.startsWith("image/") == true
    val text = clip?.text?.toString().orEmpty()
    LiveFrame(ctx, "Вставить из буфера", "в «${ctx.store.header(chatId).title}»") {
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                clip == null || (uri == null && text.isBlank()) -> C3Empty("Буфер обмена пуст (или Android не даёт его прочитать, пока приложение не в фокусе)")
                isImage -> { Text("Картинка из буфера", style = mts(14, W7)) }
                else -> Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(14.dp)) { Text(text, style = mts(15), maxLines = 12) }
            }
            MoPill("Обновить", height = 38.dp) { tick++ }
        }
        MoPill(if (isImage) "Отправить картинку" else "Отправить", Modifier.fillMaxWidth().padding(16.dp), height = 50.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent, size = 15) {
            when {
                isImage -> scope.launch {
                    val path = withContext(Dispatchers.IO) { c3CopyUri(context, uri!!, "clip") }
                    if (path == null) ctx.toast("Не удалось прочитать картинку") else { ctx.store.sendPhoto(chatId, path); ctx.toast("Картинка отправлена") }
                }
                text.isNotBlank() -> { ctx.store.send(chatId, text); ctx.toast("Отправлено") }
                else -> ctx.toast("Нечего вставлять")
            }
        }
    }
}

/** FilePicker: the system file picker (SAF) → files go to the chat as documents (store.sendFile). */
@Composable
internal fun FilePickerLive(ctx: KxScreenCtx, chatId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    val sent = remember { mutableStateListOf<String>() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            uris.take(10).forEachIndexed { i, u ->
                val path = withContext(Dispatchers.IO) { c3CopyUri(context, u, "file") }
                if (path == null) ctx.toast("Не удалось прочитать файл") else { ctx.store.sendFile(chatId, path, if (i == 0) caption.trim() else ""); sent += path.substringAfterLast('/') }
            }
            caption = ""
        }
    }
    LiveFrame(ctx, "Отправить файл", "в «${ctx.store.header(chatId).title}»") {
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(caption, { caption = it }, "Подпись к файлу (необязательно)")
            sent.forEach { Text("✓ $it", style = mts(13, color = Kx.accent), maxLines = 1) }
            if (sent.isEmpty()) C3Empty("Файлы уходят как документы, без сжатия. До 10 за раз.")
        }
        MoPill("Выбрать файлы", Modifier.fillMaxWidth().padding(16.dp), height = 50.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent, size = 15) { picker.launch(arrayOf("*/*")) }
    }
}

/** SmartText: links, phones, e-mails, mentions and hashtags Telegram finds in a message (store.textEntities). */
@Composable
internal fun SmartTextLive(ctx: KxScreenCtx, chatId: Long?, messageId: Long?) {
    val peek = c3Peek(ctx.store, chatId, 30)
    val msg = if (messageId != null) peek.firstOrNull { it.id == messageId } else peek.lastOrNull { it.text.isNotBlank() }
    var text by remember(msg?.id) { mutableStateOf(msg?.text.orEmpty()) }
    var found by remember { mutableStateOf<List<com.kirox.app.ui.v3.model.KxEntity>>(emptyList()) }
    LaunchedEffect(text) { delay(300); found = if (text.isBlank()) emptyList() else ctx.store.textEntities(text) }
    val uri = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    LiveFrame(ctx, "Умный текст", "Что Telegram нашёл в сообщении") {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(text, { text = it }, "Текст сообщения", single = false)
            if (found.isEmpty()) C3Empty(if (text.isBlank()) "Вставьте текст или откройте из сообщения" else "Ссылок, телефонов и адресов не найдено")
            found.forEach { e ->
                val part = runCatching { text.substring(e.offset, e.offset + e.length) }.getOrDefault("")
                val kind = when { e.url?.startsWith("tel:") == true -> "Телефон"; e.url?.startsWith("mailto:") == true -> "Почта"; e.type == KxEntityType.Mention -> "Упоминание"
                    e.type == KxEntityType.Hashtag -> "Хэштег"; else -> "Ссылка" }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { Text(kind, style = mts(11, W7, Kx.accent)); Text(part, style = mts(14), maxLines = 2) }
                    MoPill("Копировать", height = 32.dp, size = 12) { clipboard.setText(AnnotatedString(part)); ctx.toast("Скопировано") }
                    if (e.url != null) MoPill("Открыть", height = 32.dp, size = 12, ink = Kx.accent) { runCatching { uri.openUri(e.url) } }
                }
            }
        }
    }
}

/** BigEmoji: the emoji of a message (arg chat:message) and a tap that plays Telegram's effect on both sides (store.clickBigEmoji). */
@Composable
internal fun BigEmojiLive(ctx: KxScreenCtx, chatId: Long, messageId: Long?) {
    val peek = c3Peek(ctx.store, chatId, 40)
    val msg = if (messageId != null) peek.firstOrNull { it.id == messageId } else peek.lastOrNull { m -> m.text.isNotBlank() && m.text.length <= 8 && m.text.none { it.isLetterOrDigit() } }
    LiveFrame(ctx, "Большой эмодзи", ctx.store.header(chatId).title) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (msg == null) C3Empty(if (peek.isEmpty()) "Загрузка…" else "В последних сообщениях нет одиночного эмодзи")
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(msg.text, style = mts(96), modifier = Modifier.kxClick("Эффект") { ctx.store.clickBigEmoji(chatId, msg.id); ctx.toast("Эффект отправлен — собеседник тоже его увидит") })
                Text("Нажмите на эмодзи", style = mts(13, color = MoMuted))
            }
        }
    }
}

/** ViewOnce: pick a photo from the gallery and send it as «просмотр один раз» (private chats only). */
@Composable
internal fun ViewOnceLive(ctx: KxScreenCtx, chatId: Long) {
    val header = ctx.store.header(chatId)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val private = header.kind == KxChatKind.Private
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        if (u == null) return@rememberLauncherForActivityResult
        scope.launch {
            val path = withContext(Dispatchers.IO) { c3CopyUri(context, u, "once") }
            ctx.toast(if (path == null) "Не удалось прочитать фото" else ctx.store.sendViewOnce(chatId, path) ?: "Фото отправлено — его можно открыть один раз")
        }
    }
    LiveFrame(ctx, "Просмотр один раз", header.title) {
        C3Empty(if (private) "Фото исчезнет сразу после того, как собеседник его откроет. Скриншот он всё равно может сделать." else "Telegram разрешает такие фото только в личных чатах.", Modifier.weight(1f))
        if (private) MoPill("Выбрать фото", Modifier.fillMaxWidth().padding(16.dp), height = 50.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent, size = 15) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

/** MeetTime: a non-anonymous multi-answer poll «Когда встречаемся?» built from picked days × times (store.sendPoll). */
@Composable
internal fun MeetTimeLive(ctx: KxScreenCtx, chatId: Long) {
    val days = remember {
        val f = java.text.SimpleDateFormat("EE d MMM", java.util.Locale("ru"))
        (0 until 7).map { d -> f.format(java.util.Date(System.currentTimeMillis() + d * 86_400_000L)) }
    }
    val times = listOf("10:00", "13:00", "16:00", "19:00")
    val picked = remember { mutableStateListOf<String>() }
    var question by remember { mutableStateOf("Когда встречаемся?") }
    LiveFrame(ctx, "Выбрать время", "Опрос в «${ctx.store.header(chatId).title}»") {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(question, { question = it }, "Вопрос")
            days.forEach { d ->
                Text(d, style = mts(13, W7, Kx.text2))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    times.forEach { t -> val k = "$d, $t"; Chip(t, k in picked) { if (k in picked) picked.remove(k) else if (picked.size < 12) picked.add(k) else ctx.toast("В опросе не больше 12 вариантов") } }
                }
            }
        }
        MoPill("Отправить опрос (${picked.size})", Modifier.fillMaxWidth().padding(16.dp), height = 50.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent, size = 15) {
            if (picked.size < 2) { ctx.toast("Выберите хотя бы 2 варианта"); return@MoPill }
            ctx.store.sendPoll(chatId, question.ifBlank { "Когда встречаемся?" }, picked.toList(), anonymous = false, multiple = true)
            ctx.toast("Опрос отправлен — голоса видны в чате"); ctx.back()
        }
    }
}
