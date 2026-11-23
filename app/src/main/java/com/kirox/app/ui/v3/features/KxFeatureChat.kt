package com.kirox.app.ui.v3.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
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
import com.kirox.app.ui.v3.components.KxConfirm
import com.kirox.app.ui.v3.components.KxConfirmDialog
import com.kirox.app.ui.v3.components.KxSwitch
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.data.KxFeatureJump
import com.kirox.app.ui.v3.data.KxOnDeviceSpeech
import com.kirox.app.ui.v3.data.KxOnDeviceTranslate
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.screens.ChatMenuExtra
import com.kirox.app.ui.v3.screens.chat.*
import com.kirox.app.ui.v3.theme.Kx
import kotlinx.coroutines.delay
import java.util.Calendar

/*
 * features: everything the kiroX-only tools add to an open chat, behind three small hooks in ChatScreen
 * (message menu items, ⋮ menu rows, and [KxFeatureChatLayer] drawn over the chat):
 *  - «/» in the composer → reply templates (KxTemplateVars), and «Шаблоны» in the ⋮ menu;
 *  - «В закладки» and «Напомнить прочитать» in the message menu; «Заметка» in the ⋮ menu;
 *  - the questions of the on-device tools (download a translation model / a speech language);
 *  - scrolling to a message opened from a bookmark or a reminder.
 */

/** Sheets of the open chat (one chat at a time). */
internal object KxFeatureUi {
    var remindFor by mutableStateOf<KxMessage?>(null)
    var noteFor by mutableStateOf<Long?>(null)
    var templatesFor by mutableStateOf<Long?>(null)
}

/** One-line text of a message for lists and notifications. */
internal fun kxSnippet(m: KxMessage): String = m.text.replace('\n', ' ').trim().take(300).ifBlank {
    when (m.kind) {
        KxMsgKind.Photo -> "Фото"; KxMsgKind.Video -> "Видео"; KxMsgKind.Gif -> "GIF"; KxMsgKind.Voice -> "Голосовое сообщение"
        KxMsgKind.VideoNote -> "Видеосообщение"; KxMsgKind.Sticker -> "Стикер"; KxMsgKind.File -> m.fileName.ifBlank { "Файл" }
        KxMsgKind.Music -> m.fileName.ifBlank { "Музыка" }; KxMsgKind.Location -> "Геопозиция"; KxMsgKind.Poll -> "Опрос"; KxMsgKind.Contact -> "Контакт"
        else -> "Сообщение"
    }
}

/** Message menu items (hook in ChatScreen's `more` list). [close] hides the menu. */
internal fun kxFeatureMessageItems(store: KxStore, chatId: Long, header: KxChatHeader, m: KxMessage, close: () -> Unit, toast: (String) -> Unit): List<Triple<ImageVector, String, () -> Unit>> {
    if (m.kind == KxMsgKind.Service) return emptyList()
    val f = store.features
    val marked = f.isBookmarked(chatId, m.id)
    return listOf(
        Triple(if (marked) Icons.Outlined.StarOutline else Icons.Outlined.StarBorder, if (marked) "Убрать из закладок" else "В закладки") {
            close()
            if (marked) { f.removeBookmark(chatId, m.id); toast("Убрано из закладок") }
            else {
                f.addBookmark(KxBookmark(chatId, m.id, header.title, if (m.out) "Вы" else m.sender ?: header.title, kxSnippet(m), (System.currentTimeMillis() / 1000).toInt(), System.currentTimeMillis()))
                toast("Добавлено в закладки")
            }
        },
        Triple(Icons.Outlined.Alarm, "Напомнить прочитать") { close(); KxFeatureUi.remindFor = m },
    )
}

/** ⋮ menu rows of the chat (hook in ChatScreen's extra list). */
internal fun kxFeatureChatExtras(store: KxStore, chatId: Long, header: KxChatHeader): List<ChatMenuExtra> = buildList {
    add(ChatMenuExtra(Icons.AutoMirrored.Outlined.StickyNote2, if (store.features.note(chatId).isBlank()) "Заметка о чате" else "Моя заметка") { KxFeatureUi.noteFor = chatId })
    if (header.composer == KxComposer.Write) add(ChatMenuExtra(Icons.Outlined.Bolt, "Шаблоны ответов") { KxFeatureUi.templatesFor = chatId })
}

/**
 * Drawn over the open chat (hook at the end of ChatScreen's root Box): template suggestions above the composer,
 * the remind / note / templates sheets, and the on-device questions. [text] / [onText] = the composer.
 */
@Composable
internal fun KxFeatureChatLayer(store: KxStore, chatId: Long, header: KxChatHeader, text: String, onText: (String) -> Unit,
                                replyName: String?, onOpen: (String) -> Unit, toast: (String) -> Unit) {
    // Its own full-size box (no touch handling): the popups align like the chat's own overlays.
    Box(Modifier.fillMaxSize()) { LayerContent(store, chatId, header, text, onText, replyName, onOpen, toast) }
}

@Composable
private fun BoxScope.LayerContent(store: KxStore, chatId: Long, header: KxChatHeader, text: String, onText: (String) -> Unit,
                                  replyName: String?, onOpen: (String) -> Unit, toast: (String) -> Unit) {
    val f = store.features
    val context = LocalContext.current
    // A bookmark / reminder opened this chat: scroll to its message once the history is there.
    LaunchedEffect(chatId) {
        val target = KxFeatureJump.consume(chatId) ?: return@LaunchedEffect
        repeat(20) { if (store.messages(chatId).isNotEmpty()) return@repeat; delay(150) }
        store.jumpTo(chatId, target)
    }
    DisposableEffect(Unit) { KxOnDeviceTranslate.hosts++; onDispose { KxOnDeviceTranslate.hosts-- } }

    val vars = KxTemplateVars.Vars(peerName = replyName ?: if (header.kind == KxChatKind.Private || header.kind == KxChatKind.Secret) header.title else "",
        myName = store.me.name, chatTitle = header.title, now = Calendar.getInstance())
    fun insert(t: KxTemplate, replace: Boolean) {
        val body = KxTemplateVars.expand(t.text, vars)
        onText(if (replace || text.isBlank()) body else text.trimEnd() + " " + body)
        f.templateUsed(t.id)
    }

    // «/» suggestions above the composer.
    val hits = if (f.settings[KxFeatureKeys.SLASH] != 0 && header.composer == KxComposer.Write && header.kind != KxChatKind.Bot) KxTemplateVars.matches(text, f.templates) else null
    if (hits != null && (hits.isNotEmpty() || text == "/")) Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding()
        .padding(start = 10.dp, end = 10.dp, bottom = 92.dp).fillMaxWidth().heightIn(max = 260.dp).shadow(16.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
        .background(Color(0xFF161B18)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).verticalScroll(rememberScrollState()).padding(6.dp)) {
        hits.forEach { t ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).kxClick("/${t.key}") { insert(t, true) }.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("/${t.key}", style = ts(13, W800, Kx.accent), modifier = Modifier.widthIn(min = 64.dp, max = 110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(KxTemplateVars.expand(t.text, vars), style = ts(13, color = Kx.text2), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).kxClick { onText(""); onOpen("KxTemplates") }.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = Kx.text3)
            Text(if (f.templates.isEmpty()) "Создать шаблон ответа" else "Шаблоны…", style = ts(13, W700, Kx.text3))
        }
    }

    // On-device model download in progress.
    val dl = KxOnDeviceTranslate.downloading
    AnimatedVisibility(dl.isNotEmpty(), Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp), enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Text("Скачивается модель перевода…", style = ts(12, W700, Kx.text2), modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xF21E2823))
            .border(1.dp, Kx.w(.12f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 7.dp))
    }

    // Sheets.
    BSheet(KxFeatureUi.templatesFor == chatId, { KxFeatureUi.templatesFor = null }) {
        Text("Шаблоны ответов", style = ts(18, W800), modifier = Modifier.padding(bottom = 10.dp))
        if (f.templates.isEmpty()) Text("Шаблонов пока нет. Создайте ответ один раз — и вставляйте его в любом чате через «/».", style = ts(14, color = Kx.text3, lh = 20.sp))
        Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            f.templates.forEach { t ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.045f)).kxClick("/${t.key}") { insert(t, false); KxFeatureUi.templatesFor = null }
                    .padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("/${t.key}", style = ts(13, W800, Kx.accent))
                    Text(KxTemplateVars.expand(t.text, vars), style = ts(14, color = Kx.text2, lh = 19.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
        ChPrimary(if (f.templates.isEmpty()) "Создать шаблон" else "Управлять шаблонами", modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), icon = Icons.Outlined.Bolt) {
            KxFeatureUi.templatesFor = null; onOpen("KxTemplates")
        }
    }

    val noteOpen = KxFeatureUi.noteFor == chatId
    var noteText by remember(chatId, noteOpen) { mutableStateOf(f.note(chatId)) }
    BSheet(noteOpen, { KxFeatureUi.noteFor = null }) {
        Column(Modifier.imePadding()) {
            Text("Заметка · ${header.title}", style = ts(18, W800), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Видите только вы. Хранится на этом телефоне, в Telegram не отправляется.", style = ts(12, color = Kx.text4, lh = 17.sp), modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            BTextArea(noteText, { noteText = it.take(4000) }, "Например: день рождения 12 марта, любит кофе без сахара", 140.dp)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (f.note(chatId).isNotBlank()) Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Kx.danger.copy(alpha = .1f))
                    .kxClick("Удалить заметку") { f.setNote(chatId, header.title, ""); KxFeatureUi.noteFor = null; toast("Заметка удалена") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, null, tint = Kx.danger)
                }
                ChPrimary("Сохранить", modifier = Modifier.weight(1f)) { f.setNote(chatId, header.title, noteText); KxFeatureUi.noteFor = null; toast(if (noteText.isBlank()) "Заметка удалена" else "Заметка сохранена") }
            }
        }
    }

    KxFeatureUi.remindFor?.let { m ->
        KxRemindSheet(onDismiss = { KxFeatureUi.remindFor = null }) { at, label ->
            f.addReminder(KxReminder(0, chatId, m.id, header.title, kxSnippet(m), at))
            KxFeatureUi.remindFor = null
            toast("Напомню $label")
        }
    }

    // Questions of the on-device tools.
    KxOnDeviceTranslate.ask?.let { a ->
        var wifi by remember(a) { mutableStateOf(f.settings[KxFeatureKeys.TR_WIFI_ONLY] != 0) }
        BSheet(true, { KxOnDeviceTranslate.answer(false) }) {
            Text("Перевод на телефоне", style = ts(18, W800))
            Text("Без Telegram Premium kiroX переводит сам, прямо на телефоне: текст никуда не отправляется. Для этого один раз нужно скачать " +
                a.langs.joinToString(" и ") { "модель " + KxFeatureLangs.name(it) } + " — около ${30 * a.langs.size} МБ.",
                style = ts(14, color = Kx.text2, lh = 20.sp), modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).kxClick { wifi = !wifi; f.putSetting(KxFeatureKeys.TR_WIFI_ONLY, if (wifi) 1 else 0) }
                .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Скачивать только по Wi-Fi", style = ts(15), modifier = Modifier.weight(1f)); KxSwitch(wifi)
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.08f)).kxClick { KxOnDeviceTranslate.answer(false) }, contentAlignment = Alignment.Center) {
                    Text("Не сейчас", style = ts(15, W700))
                }
                ChPrimary("Скачать", modifier = Modifier.weight(1f)) { KxOnDeviceTranslate.answer(true) }
            }
        }
    }
    KxOnDeviceSpeech.needsModel?.let { tag ->
        val name = KxFeatureLangs.speech.firstOrNull { it.first == tag }?.second ?: tag
        KxConfirmDialog(KxConfirm("Распознавание на телефоне",
            "Чтобы превращать голосовые в текст без интернета и без Premium, телефону нужен офлайн-пакет языка «$name». Его скачает система (Google / производитель телефона), один раз.",
            "Скачать", danger = false) {
            KxOnDeviceSpeech.downloadModel(context, tag)?.let(toast) ?: toast("Загрузка началась — попробуйте «В текст» чуть позже")
        }) { KxOnDeviceSpeech.needsModel = null }
    }
}

/** «Напомнить прочитать»: quick times and an own day + time. [onPick] gets ms and «через час»-like text. */
@Composable
internal fun KxRemindSheet(onDismiss: () -> Unit, onPick: (Long, String) -> Unit) {
    val now = remember { Calendar.getInstance() }
    fun at(days: Int, h: Int, m: Int) = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, days); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val presets = remember { KxRemindPresets.of(now) }
    var own by remember { mutableStateOf(false) }
    var day by remember { mutableIntStateOf(0) }
    var hh by remember { mutableIntStateOf((now.get(Calendar.HOUR_OF_DAY) + 1) % 24) }
    var mm by remember { mutableIntStateOf(0) }
    BSheet(true, onDismiss) {
        Text("Напомнить прочитать", style = ts(18, W800))
        Text("Придёт уведомление, нажмите — откроется это сообщение.", style = ts(12, color = Kx.text4), modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        if (!own) {
            presets.forEach { p ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).kxClick(p.title) { onPick(p.at, p.label) }.padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(20.dp), tint = Kx.accent)
                    Text(p.title, style = ts(15), modifier = Modifier.weight(1f))
                    Text(p.hint, style = ts(13, color = Kx.text4))
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).kxClick("Выбрать время") { own = true }.padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.EditCalendar, null, Modifier.size(20.dp), tint = Kx.text2); Text("Выбрать день и время", style = ts(15))
            }
        } else {
            val days = remember { listOf("Сегодня", "Завтра") + (2..6).map { d -> KxRemindPresets.weekday((now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, d) }) } }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEachIndexed { i, d ->
                    val on = day == i
                    Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f))
                        .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Color.Transparent, RoundedCornerShape(18.dp)).kxClick(d) { day = i }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center) { Text(d, style = ts(13, W700, if (on) Kx.accent else Kx.text)) }
                }
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stepper("Час", "%02d".format(hh), Modifier.weight(1f), { hh = (hh + 23) % 24 }) { hh = (hh + 1) % 24 }
                Stepper("Минуты", "%02d".format(mm), Modifier.weight(1f), { mm = (mm + 55) % 60 }) { mm = (mm + 5) % 60 }
            }
            val t = at(day, hh, mm)
            val label = "${days[day].lowercase()} в %02d:%02d".format(hh, mm)
            ChPrimary(if (t > System.currentTimeMillis() + 30_000) "Напомнить $label" else "Выберите время в будущем", enabled = t > System.currentTimeMillis() + 30_000,
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth()) { if (t > System.currentTimeMillis() + 30_000) onPick(t, label) }
        }
    }
}

@Composable
private fun Stepper(title: String, value: String, modifier: Modifier, minus: () -> Unit, plus: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = ts(12, color = Kx.text4))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).kxClick("$title меньше", onClick = minus), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Remove, null, tint = Kx.text2) }
            Text(value, style = ts(24, W800), modifier = Modifier.padding(horizontal = 8.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).kxClick("$title больше", onClick = plus), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, tint = Kx.text2) }
        }
    }
}

/** Quick reminder times (unit-tested through [of]). */
internal object KxRemindPresets {
    data class Preset(val title: String, val hint: String, val label: String, val at: Long)
    private val wd = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
    fun weekday(c: Calendar) = wd[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " + c.get(Calendar.DAY_OF_MONTH)

    fun of(now: Calendar): List<Preset> {
        val ms = now.timeInMillis
        fun day(d: Int, h: Int) = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, d); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        return buildList {
            add(Preset("Через час", "%02d:%02d".format((now.get(Calendar.HOUR_OF_DAY) + 1) % 24, now.get(Calendar.MINUTE)), "через час", ms + 3_600_000))
            add(Preset("Через 3 часа", "%02d:%02d".format((now.get(Calendar.HOUR_OF_DAY) + 3) % 24, now.get(Calendar.MINUTE)), "через 3 часа", ms + 3 * 3_600_000))
            if (now.get(Calendar.HOUR_OF_DAY) < 19) add(Preset("Сегодня вечером", "20:00", "сегодня в 20:00", day(0, 20)))
            add(Preset("Завтра утром", "9:00", "завтра в 9:00", day(1, 9)))
            add(Preset("Через неделю", weekday((now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }), "через неделю", day(7, 9)))
        }
    }
}

