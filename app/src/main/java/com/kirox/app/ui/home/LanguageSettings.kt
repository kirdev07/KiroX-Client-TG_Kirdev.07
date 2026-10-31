package com.kirox.app.ui.home

import androidx.core.net.toUri
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import com.kirox.app.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.BuildConfig
import com.kirox.app.KiroXApplication
import com.kirox.app.telegram.BlockedSender
import com.kirox.app.telegram.CloudPasswordState
import com.kirox.app.telegram.NotificationException
import com.kirox.app.telegram.NotifyScope
import com.kirox.app.telegram.ProxyKind
import com.kirox.app.telegram.TelegramProxy
import com.kirox.app.telegram.PrivacyItem
import com.kirox.app.telegram.PrivacyState
import com.kirox.app.telegram.AccountTtl
import com.kirox.app.telegram.MessagePrivacy
import com.kirox.app.telegram.ReactionNotifications
import com.kirox.app.telegram.ReactionSource
import com.kirox.app.telegram.PrivacyLevel
import com.kirox.app.telegram.ScopeNotifications
import com.kirox.app.telegram.ChatStorage
import com.kirox.app.telegram.StorageBreakdown
import com.kirox.app.telegram.StorageCategory
import com.kirox.app.telegram.CacheLimit
import com.kirox.app.telegram.CallDataSaving
import com.kirox.app.telegram.TelegramChatKind
import com.kirox.app.telegram.DownloadNetwork
import com.kirox.app.telegram.KeepMedia
import com.kirox.app.telegram.NetworkDownload
import com.kirox.app.telegram.NetworkUsage
import com.kirox.app.telegram.TelegramUserProfile
import com.kirox.app.ui.icons.KiroXIcon as IconGlyph
import com.kirox.app.ui.icons.KiroXIcon as IconGlyphs
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.launch

// Settings → Language: languages and own names for texts.

/** Settings → Language: interface language and the user's own names for any text. */
@Composable
internal fun LanguageScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    val appContext = LocalContext.current.applicationContext
    /** The language for the app and for the system notification channels. */
    fun chooseLanguage(language: com.kirox.app.i18n.AppLanguage) {
        com.kirox.app.i18n.I18n.chooseLanguage(language)
        com.kirox.app.notify.MessageNotifications.refreshChannels(appContext)
        com.kirox.app.voip.CallNotifications.ensureChannels(appContext)
    }
    var renaming by remember { mutableStateOf(false) }
    if (renaming) { RenameTextsScreen(onBack = { renaming = false }, onNotice = onNotice); return }
    BackHandler(onBack = onBack)
    val i18n = com.kirox.app.i18n.I18n
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Язык", onBack) }
        item { SmallCaption("Язык интерфейса") }
        item {
            SettingsCard {
                com.kirox.app.i18n.AppLanguage.entries.forEachIndexed { index, language ->
                    if (index > 0) CardDivider()
                    val selected = i18n.language == language
                    Row(
                        Modifier.fillMaxWidth().clickable { chooseLanguage(language) }.heightIn(min = 56.dp).padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            // Each language is named in itself and never translated.
                            androidx.compose.material3.Text(language.title, color = Fusion.colors.textPrimary, fontSize = 16.sp)
                            androidx.compose.material3.Text(
                                if (language == com.kirox.app.i18n.AppLanguage.Russian) "Russian" else "Английский",
                                color = Fusion.colors.textSecondary, fontSize = 13.sp
                            )
                        }
                        androidx.compose.material3.RadioButton(selected, onClick = { chooseLanguage(language) })
                    }
                }
            }
        }
        item {
            Text(
                "Сообщения и названия чатов не переводятся — меняется только интерфейс kiroX.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        item { SmallCaption("Свои названия") }
        item { NamePacksCard(onEdit = { renaming = true }, onNotice = onNotice) }
        item {
            Text(
                "Набор — это ваши названия разделов и кнопок. Можно держать несколько наборов и переключаться между ними, делиться ими с друзьями и загружать чужие.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Installed name packs with the one in use, «Обычные названия» to use none, and per pack: edit,
 * share as text, save as a file, rename, delete. New packs are made here or loaded from a file or text.
 */
@Composable
private fun NamePacksCard(onEdit: () -> Unit, onNotice: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val i18n = com.kirox.app.i18n.I18n
    var menuFor by remember { mutableStateOf<String?>(null) }
    var renameFor by remember { mutableStateOf<com.kirox.app.i18n.NamePack?>(null) }
    var deleteFor by remember { mutableStateOf<com.kirox.app.i18n.NamePack?>(null) }
    var creating by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var pasting by remember { mutableStateOf(false) }
    fun installed(pack: com.kirox.app.i18n.NamePack?) {
        if (pack == null) { onNotice("Это не набор названий kiroX"); return }
        i18n.activatePack(pack.id)
        onNotice("Набор «${pack.title}» загружен и выбран")
    }
    val pickFile = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().take(PACK_FILE_MAX).toByteArray().decodeToString() } }.getOrNull()
            }
            installed(text?.let(i18n::importPack))
        }
    }
    SettingsCard {
        PackRow("Обычные названия", "Как задумано в kiroX", i18n.activePackId == null, onSelect = { i18n.activatePack(null) }, onMenu = null)
        i18n.packs.forEach { pack ->
            CardDivider()
            Box {
                PackRow(pack.title, packCount(pack.names.size), i18n.activePackId == pack.id, onSelect = { i18n.activatePack(pack.id) }, onMenu = { menuFor = pack.id })
                androidx.compose.material3.DropdownMenu(menuFor == pack.id, onDismissRequest = { menuFor = null }, modifier = Modifier.background(Fusion.colors.surface2)) {
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Изменить названия") }, onClick = { menuFor = null; i18n.activatePack(pack.id); onEdit() })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Поделиться") }, onClick = {
                        menuFor = null
                        val text = i18n.exportPack(pack.id) ?: return@DropdownMenuItem
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, pack.title).putExtra(Intent.EXTRA_TEXT, text)
                        runCatching { context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Сохранить в Загрузки") }, onClick = {
                        menuFor = null
                        val text = i18n.exportPack(pack.id) ?: return@DropdownMenuItem
                        scope.launch {
                            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                java.io.File(context.cacheDir, "packs").apply { mkdirs() }.resolve("pack.json").apply { writeText(text) }
                            }
                            val saved = com.kirox.app.ui.chat.saveToDownloads(context, file.path, "${pack.title}.kirox-names.json", "application/json")
                            onNotice(if (saved) "Сохранено в «Загрузки/kiroX»" else "Не удалось сохранить файл")
                        }
                    })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Переименовать набор") }, onClick = { menuFor = null; renameFor = pack })
                    androidx.compose.material3.DropdownMenuItem(text = { Text("Удалить набор", color = Fusion.colors.danger) }, onClick = { menuFor = null; deleteFor = pack })
                }
            }
        }
    }
    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.material3.OutlinedButton(onClick = { creating = true }, modifier = Modifier.weight(1f)) { Text("Новый набор") }
        androidx.compose.material3.OutlinedButton(onClick = { loading = true }, modifier = Modifier.weight(1f)) { Text("Загрузить набор") }
    }
    if (i18n.activePackId != null) SettingsCard {
        IconRow(IconGlyphs.Edit, Color(0xFF9B6BFF), "Изменить названия", "В наборе «${i18n.packs.firstOrNull { it.id == i18n.activePackId }?.title.orEmpty()}»", onClick = onEdit)
    }
    if (creating) PackTitleDialog("Новый набор", "Мои названия", onDismiss = { creating = false }) { title ->
        creating = false
        i18n.createPack(title)
        onEdit()
    }
    renameFor?.let { pack ->
        PackTitleDialog("Переименовать набор", pack.title, onDismiss = { renameFor = null }) { title -> renameFor = null; i18n.renamePack(pack.id, title) }
    }
    deleteFor?.let { pack ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Удалить набор «${pack.title}»?", color = Fusion.colors.textPrimary) },
            text = { Text("Его названия пропадут с этого телефона. Если вы им делились, у друзей он останется.", color = Fusion.colors.textSecondary) },
            confirmButton = { TextButton(onClick = { deleteFor = null; i18n.deletePack(pack.id); onNotice("Набор удалён") }) { Text("Удалить", color = Fusion.colors.danger) } },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    if (loading) AlertDialog(
        onDismissRequest = { loading = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Загрузить набор", color = Fusion.colors.textPrimary) },
        text = {
            Column {
                Text("Откуда взять набор названий, который вам прислали?", color = Fusion.colors.textSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LinkRow("Из файла", "Файл .json, например из «Загрузок»") { loading = false; pickFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                LinkRow("Вставить текст", "Если набор прислали сообщением") { loading = false; pasting = true }
            }
        },
        confirmButton = { TextButton(onClick = { loading = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    if (pasting) {
        var text by remember { mutableStateOf("") }
        val clipboard = remember { context.getSystemService(android.content.ClipboardManager::class.java) }
        AlertDialog(
            onDismissRequest = { pasting = false },
            containerColor = Fusion.colors.surface1,
            title = { Text("Вставить набор", color = Fusion.colors.textPrimary) },
            text = {
                Column {
                    Text("Скопируйте сообщение с набором и вставьте его сюда.", color = Fusion.colors.textSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { text = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty().take(PACK_FILE_MAX) }) {
                        Text("Вставить из буфера", color = Fusion.colors.accent)
                    }
                    Box(Modifier.heightIn(max = 220.dp)) { EditField(text, "{ \"kirox_names\": 1, … }", singleLine = false) { text = it.take(PACK_FILE_MAX) } }
                }
            },
            confirmButton = { TextButton(onClick = { pasting = false; installed(i18n.importPack(text)) }, enabled = text.isNotBlank()) { Text("Загрузить", color = Fusion.colors.accent) } },
            dismissButton = { TextButton(onClick = { pasting = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
}

/** Largest pack file read; a real pack is a few kilobytes. */
private const val PACK_FILE_MAX = 64 * 1024

private fun packCount(n: Int): String = when {
    n == 0 -> "Пока без своих названий"
    n % 10 == 1 && n % 100 != 11 -> "$n название"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "$n названия"
    else -> "$n названий"
}

@Composable
private fun PackRow(title: String, subtitle: String, selected: Boolean, onSelect: () -> Unit, onMenu: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onSelect).heightIn(min = 60.dp).padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.RadioButton(selected, onClick = onSelect)
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            androidx.compose.material3.Text(title, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Text(subtitle, color = Fusion.colors.textSecondary, fontSize = 13.sp)
        }
        if (onMenu != null) androidx.compose.material3.IconButton(onClick = onMenu) { IconGlyph(IconGlyphs.More, "Действия с набором", Modifier.size(22.dp), Fusion.colors.textSecondary) }
    }
}

@Composable
private fun PackTitleDialog(title: String, initial: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text(title, color = Fusion.colors.textPrimary) },
        text = { EditField(value, "Название набора") { value = it.replace("\n", " ").take(40) } },
        confirmButton = { TextButton(onClick = { onDone(value.trim()) }, enabled = value.isNotBlank()) { Text("Готово", color = Fusion.colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

/** A name the user may change: the source text, where it is shown and what it does. */
private class RenameItem(val key: String, val about: String)

/** The main names of kiroX, grouped by where they are; everything else keeps its normal text. */
private val renameGroups: List<Pair<String, List<RenameItem>>> = listOf(
    "Нижняя панель" to listOf(
        RenameItem("Чаты", "Вкладка со списком всех ваших переписок"),
        RenameItem("Контакты", "Вкладка с людьми из вашей телефонной книги в Telegram"),
        RenameItem("Звонки", "Вкладка с историей звонков"),
        RenameItem("Истории", "Вкладка с историями контактов и каналов"),
        RenameItem("Настройки", "Вкладка с настройками kiroX и аккаунта")
    ),
    "Список чатов" to listOf(
        RenameItem("Все", "Первая вкладка над чатами — показывает все чаты сразу"),
        RenameItem("Личные", "Фильтр: только переписки с людьми и ботами"),
        RenameItem("Группы", "Фильтр: только группы"),
        RenameItem("Каналы", "Фильтр: только каналы"),
        RenameItem("Архив", "Раздел со скрытыми чатами вверху списка"),
        RenameItem("Сообщения", "Раздел с обычными чатами рядом с «Архивом»"),
        RenameItem("Избранное", "Ваш личный чат с самим собой для заметок и файлов")
    ),
    "Создание" to listOf(
        RenameItem("Новая группа", "Кнопка в меню ✏️ — создать группу"),
        RenameItem("Новый канал", "Кнопка в меню ✏️ — создать канал"),
        RenameItem("Секретный чат", "Кнопка в меню ✏️ — зашифрованный чат только на двух устройствах"),
        RenameItem("Новый контакт", "Добавить человека по номеру телефона")
    ),
    "Настройки" to listOf(
        RenameItem("Изменить профиль", "Имя, фото, имя пользователя и «О себе»"),
        RenameItem("Уведомления и звуки", "Уведомления о сообщениях, звуки, баннеры"),
        RenameItem("Конфиденциальность", "Кто что видит, код-пароль, чёрный список"),
        RenameItem("Данные и память", "Кэш, автозагрузка медиа, трафик, прокси"),
        RenameItem("Оформление", "Тема, пузыри сообщений, обои, анимации"),
        RenameItem("Язык", "Язык интерфейса и свои названия"),
        RenameItem("Устройства", "Где выполнен вход в ваш аккаунт"),
        RenameItem("kiroX Настройки", "Журнал ошибок, сброс настроек, выход"),
        RenameItem("О приложении", "Версия kiroX и обновления")
    ),
    "В чате" to listOf(
        RenameItem("Сообщение", "Подсказка в поле ввода внизу чата"),
        RenameItem("Ответить", "Пункт меню сообщения — ответить на него"),
        RenameItem("Переслать", "Пункт меню сообщения — отправить в другой чат"),
        RenameItem("Копировать", "Пункт меню сообщения — скопировать текст"),
        RenameItem("Закрепить", "Пункт меню сообщения — закрепить вверху чата"),
        RenameItem("Удалить", "Пункт меню сообщения — удалить его")
    )
)

/**
 * «Свои названия»: the main names of kiroX with what each one is; tap one to name it your way.
 * The new name replaces it everywhere in the app, in any language.
 */
@Composable
private fun RenameTextsScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val i18n = com.kirox.app.i18n.I18n
    var editing by remember { mutableStateOf<RenameItem?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val overrides = i18n.overrides
    val mine = renameGroups.sumOf { (_, items) -> items.count { it.key in overrides } }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle(i18n.packs.firstOrNull { it.id == i18n.activePackId }?.title ?: "Свои названия", onBack) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF9B6BFF)), contentAlignment = Alignment.Center) {
                    IconGlyph(IconGlyphs.Edit, null, Modifier.size(32.dp), Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Назовите главные разделы и кнопки kiroX по-своему. Например, «Избранное» — «Мои заметки», а «Чаты» — «Переписки». Новое название появится везде, где есть этот пункт, на любом языке.",
                    color = Fusion.colors.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center
                )
            }
        }
        renameGroups.forEach { (group, items) ->
            item(key = group) {
                SmallCaption(group)
                SettingsCard {
                    items.forEachIndexed { index, rename ->
                        if (index > 0) androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 20.dp), color = Fusion.colors.outline)
                        val own = overrides[rename.key]
                        Row(
                            Modifier.fillMaxWidth().clickable { editing = rename }.heightIn(min = 60.dp).padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                // Raw material3 Text: the list shows names as they are, not run through the engine again.
                                androidx.compose.material3.Text(own ?: i18n.base(rename.key), color = if (own != null) Fusion.colors.accent else Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(rename.about, color = Fusion.colors.textSecondary, fontSize = 13.sp, lineHeight = 17.sp)
                                if (own != null) androidx.compose.material3.Text("было: ${i18n.base(rename.key)}", color = Fusion.colors.textTertiary, fontSize = 12.sp)
                            }
                            IconGlyph(IconGlyphs.Edit, null, Modifier.size(18.dp), Fusion.colors.textTertiary)
                        }
                    }
                }
            }
        }
        if (mine > 0) item {
            Spacer(Modifier.height(8.dp))
            SettingsCard { LinkRow("Вернуть все названия", "Своих названий: $mine", Fusion.colors.danger) { confirmReset = true } }
        }
    }
    editing?.let { rename ->
        var value by remember(rename.key) { mutableStateOf(overrides[rename.key] ?: i18n.base(rename.key)) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = Fusion.colors.surface1,
            title = { androidx.compose.material3.Text(i18n.base(rename.key), color = Fusion.colors.textPrimary) },
            text = {
                Column {
                    Text(rename.about, color = Fusion.colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    EditField(value, "Своё название") { value = it.replace("\n", " ").take(40) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    i18n.setOverride(rename.key, value.trim().takeIf { it.isNotEmpty() && it != i18n.base(rename.key) })
                    editing = null
                }) { Text("Сохранить", color = Fusion.colors.accent) }
            },
            dismissButton = {
                Row {
                    if (rename.key in overrides) TextButton(onClick = { i18n.setOverride(rename.key, null); editing = null }) { Text("Вернуть", color = Fusion.colors.danger) }
                    TextButton(onClick = { editing = null }) { Text("Отмена", color = Fusion.colors.textSecondary) }
                }
            }
        )
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Вернуть все названия?", color = Fusion.colors.textPrimary) },
        text = { Text("Разделы и кнопки снова будут называться как обычно.", color = Fusion.colors.textSecondary) },
        confirmButton = { TextButton(onClick = { confirmReset = false; i18n.clearOverrides(); onNotice("Названия возвращены") }) { Text("Вернуть", color = Fusion.colors.danger) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}
