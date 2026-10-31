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

// kiroX settings (Advanced), Premium and the QR profile.

@Composable
internal fun PremiumScreen(profile: TelegramUserProfile?, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Telegram Premium", onBack) }
        item {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("★", color = Fusion.colors.textPrimary, fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (profile?.isPremium == true) "Telegram Premium активен" else "Telegram Premium не подключён",
                    color = Fusion.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Подписка оформляется и управляется в официальном приложении Telegram. Возможности Premium работают и в kiroX там, где их поддерживает TDLib.",
                    color = Fusion.colors.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center
                )
            }
        }
        item {
            LinkRow("Открыть Telegram Premium", "telegram.org/premium") {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://telegram.org/faq_premium".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

/** Settings → Advanced: app info, reset of local KiroX settings, log out. */
@Composable
internal fun AdvancedScreen(onBack: () -> Unit, onLogOut: () -> Unit, onNotice: (String) -> Unit) {
    var languagePage by remember { mutableStateOf(false) }
    if (languagePage) { LanguageScreen(onBack = { languagePage = false }, onNotice = onNotice); return }
    var stickersPage by remember { mutableStateOf(false) }
    if (stickersPage) { StickerSetsScreen(onBack = { stickersPage = false }, onNotice = onNotice); return }
    var customIconPage by remember { mutableStateOf(false) }
    if (customIconPage) { CustomIconScreen(onBack = { customIconPage = false }, onNotice = onNotice); return }
    var filtersPage by remember { mutableStateOf(false) }
    if (filtersPage) { MessageFiltersScreen(onBack = { filtersPage = false }); return }
    var powerPage by remember { mutableStateOf(false) }
    if (powerPage) { PowerSavingScreen(onBack = { powerPage = false }); return }
    var filesPage by remember { mutableStateOf(false) }
    if (filesPage) { com.kirox.app.ui.files.FilesExplorerScreen(onBack = { filesPage = false }, onNotice = onNotice); return }
    var browserPage by remember { mutableStateOf(false) }
    if (browserPage) { BrowserSettingsScreen(onBack = { browserPage = false }, onNotice = onNotice); return }
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tweaks by com.kirox.app.settings.KiroxTweaks.state.collectAsState()
    fun set(value: com.kirox.app.settings.Tweaks) = com.kirox.app.settings.KiroxTweaks.update(value)
    var choosingDoubleTap by remember { mutableStateOf(false) }
    var appIcon by remember { mutableStateOf(com.kirox.app.settings.AppIcons.current(context)) }
    var choosingIcon by remember { mutableStateOf(false) }
    var restoreText by remember { mutableStateOf<String?>(null) }
    val pickBackup = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            restoreText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().take(512 * 1024).toByteArray().decodeToString() } }.getOrNull()
            }
            if (restoreText == null) onNotice("Не удалось прочитать файл")
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("kiroX Настройки", onBack) }
        item { SmallCaption("Режим призрака") }
        item {
            val notifications = (context.applicationContext as KiroXApplication).telegram.notifications
            fun setGhost(value: com.kirox.app.settings.Tweaks) { set(value); notifications.refreshOnline() }
            SettingsCard {
                IconRow(IconGlyphs.Eye, if (tweaks.ghost) Color(0xFF9B6BFF) else Color(0xFF8E8E93), "Режим призрака", if (tweaks.ghost) "Включён" else "Выключен") {
                    setGhost(tweaks.copy(ghost = !tweaks.ghost))
                }
                if (tweaks.ghost) {
                    CardDivider()
                    ToggleRow("Не отмечать прочитанным", "Собеседник не увидит, что вы открыли чат", tweaks.ghostNoRead) { setGhost(tweaks.copy(ghostNoRead = it)) }
                    ToggleRow("Не показывать «печатает…»", "И «записывает голосовое», «выбирает стикер»", tweaks.ghostNoTyping) { setGhost(tweaks.copy(ghostNoTyping = it)) }
                    ToggleRow("Не появляться «в сети»", "Для других вы останетесь «был(а) недавно»", tweaks.ghostOffline) { setGhost(tweaks.copy(ghostOffline = it)) }
                    ToggleRow("Смотреть истории незаметно", "Автор не увидит вас среди просмотревших", tweaks.ghostStories) { setGhost(tweaks.copy(ghostStories = it)) }
                }
            }
        }
        item {
            Text(
                "Работает на стороне kiroX. Когда вы сами отправите сообщение в чат, Telegram отметит его прочитанным, а вас — «в сети» на минуту, — так устроен сервер.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        item { SmallCaption("Чаты") }
        item {
            SettingsCard {
                IconRow(IconGlyphs.Chat, Color(0xFF4F8DF7), "Двойной тап по сообщению", null, trailing = tweaks.doubleTap.title) { choosingDoubleTap = true }
                CardDivider()
                ToggleRow("Пересылать без автора", "В окне «Переслать» сразу включено «Без автора»: сообщение уйдёт как ваше", tweaks.forwardWithoutAuthor) { set(tweaks.copy(forwardWithoutAuthor = it)) }
                CardDivider()
                IconRow(IconGlyphs.Emoji, Color(0xFFFFA94D), "Наборы стикеров", "Добавить, убрать, поменять порядок") { stickersPage = true }
            }
        }
        item {
            Text(
                "Удерживайте кнопку «Отправить»: без звука, позже в выбранное время или когда собеседник будет в сети.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        item {
            val filters by com.kirox.app.settings.MessageFilters.state.collectAsState()
            val filtersInfo = if (filters.enabled) "Слов: ${filters.words.size} · людей: ${filters.people.size}" else "Выключены"
            SettingsCard {
                LinkRow("Фильтры сообщений", "Прятать в группах и каналах сообщения со словами или от людей · $filtersInfo") { filtersPage = true }
                LinkRow("Файлы Telegram", "Всё скачанное: документы, архивы, музыка, видео — просмотр и проверка") { filesPage = true }
                ToggleRow("Проверка файлов", "Перед открытием документ проверяется на программы, маскировку, макросы и опасные архивы", tweaks.fileGuard) { set(tweaks.copy(fileGuard = it)) }
            }
        }
        item { SmallCaption("Сохранение сообщений") }
        item {
            val archive = (context.applicationContext as KiroXApplication).telegram.archive
            var keptCount by remember { mutableStateOf<Int?>(null) }
            var reload by remember { mutableIntStateOf(0) }
            LaunchedEffect(reload) { keptCount = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { archive.count() }.getOrNull() } }
            SettingsCard {
                ToggleRow("Удалённые сообщения", "Если собеседник удалит сообщение, вы всё равно сможете его прочитать: ⋮ в чате → «Удалённые сообщения»", tweaks.keepDeleted) {
                    set(tweaks.copy(keepDeleted = it))
                }
                ToggleRow("История изменений", "Прежний текст изменённого сообщения: долгое нажатие → «История изменений»", tweaks.keepEdits) {
                    set(tweaks.copy(keepEdits = it))
                }
                if ((keptCount ?: 0) > 0) LinkRow("Очистить сохранённое", "Сохранено сообщений: $keptCount", Fusion.colors.danger) {
                    scope.launch {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { archive.clear() }
                        reload++
                        onNotice("Сохранённые сообщения удалены")
                    }
                }
            }
        }
        item {
            Text(
                "Копии текста хранятся только на этом телефоне 30 дней. Секретные чаты и исчезающие сообщения не сохраняются. Собеседник об этом не узнает — пользуйтесь честно.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        item { SmallCaption("Спрашивать перед отправкой") }
        item {
            SettingsCard {
                ToggleRow("Стикеры", checked = tweaks.confirmSticker) { set(tweaks.copy(confirmSticker = it)) }
                ToggleRow("GIF", checked = tweaks.confirmGif) { set(tweaks.copy(confirmGif = it)) }
                ToggleRow("Голосовые", checked = tweaks.confirmVoice) { set(tweaks.copy(confirmVoice = it)) }
                ToggleRow("Звонки", "Перед звонком и видеочатом", tweaks.confirmCall) { set(tweaks.copy(confirmCall = it)) }
            }
        }
        item { SmallCaption("Интерфейс") }
        item {
            SettingsCard {
                ToggleRow("Вкладка «Звонки»", "Показывать в нижней панели", !tweaks.hideCallsTab) { set(tweaks.copy(hideCallsTab = !it)) }
                ToggleRow("Вкладка «Истории»", "Показывать в нижней панели", !tweaks.hideStoriesTab) { set(tweaks.copy(hideStoriesTab = !it)) }
                ToggleRow("ID в профилях", "Номер пользователя или чата в Telegram; нажатие копирует его", tweaks.showIds) { set(tweaks.copy(showIds = it)) }
                ToggleRow("Скрывать клавиатуру при прокрутке", "Проведите по переписке — клавиатура закроется", tweaks.hideKeyboardOnScroll) { set(tweaks.copy(hideKeyboardOnScroll = it)) }
                LinkRow("Встроенный браузер", if (tweaks.inAppBrowser) "Включён" else "Выключен") { browserPage = true }
                LinkRow("Энергосбережение", tweaks.powerSaving.title) { powerPage = true }
                CardDivider()
                IconRow(IconGlyphs.Palette, Color(0xFF6C4CE0), "Иконка приложения", null, trailing = appIcon.title) { choosingIcon = true }
            }
        }
        item { SmallCaption("Резервная копия") }
        item {
            SettingsCard {
                LinkRow("Сохранить настройки в файл", "Оформление, функции kiroX, уведомления, язык и наборы названий — в «Загрузки/kiroX»") {
                    scope.launch {
                        val saved = runCatching {
                            val text = com.kirox.app.settings.SettingsBackup.export(context)
                            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                java.io.File(context.cacheDir, "backup").apply { mkdirs() }.resolve("settings.json").apply { writeText(text) }
                            }
                            val day = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
                            com.kirox.app.ui.chat.saveToDownloads(context, file.path, "kiroX-settings-$day.json", "application/json")
                        }.getOrDefault(false)
                        onNotice(if (saved) "Сохранено в «Загрузки/kiroX»" else "Не удалось сохранить файл")
                    }
                }
                LinkRow("Восстановить из файла", "Настройки из сохранённой копии заменят текущие") {
                    pickBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
            }
        }
        item {
            Text(
                "В копию не попадают переписка, аккаунт, код-пароль и ключи — только настройки kiroX.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        // Version, links and updates live in Settings → «О приложении».
        item {
            LinkRow("Язык", "${com.kirox.app.i18n.I18n.language.title} · свои названия") { languagePage = true }
        }
        item { SmallCaption("Тестирование") }
        item {
            var crashes by remember { mutableIntStateOf(com.kirox.app.CrashLog.count(context)) }
            LinkRow("Журнал ошибок", if (crashes == 0) "Ошибок не записано" else "Записей: $crashes · нажмите, чтобы отправить разработчику") {
                if (crashes > 0) com.kirox.app.CrashLog.share(context) else onNotice("Журнал пуст")
            }
            if (crashes > 0) LinkRow("Очистить журнал", null, Fusion.colors.textSecondary) { com.kirox.app.CrashLog.clear(context); crashes = 0; onNotice("Журнал очищен") }
        }
        item { SmallCaption("Сброс") }
        item { LinkRow("Сбросить настройки kiroX", "Тема, анимации, чаты — к значениям по умолчанию. Telegram не затрагивается", Fusion.colors.danger) { confirmReset = true } }
        item { LinkRow("Выйти из аккаунта", null, Fusion.colors.danger) { confirmLogout = true } }
    }
    if (choosingIcon) AppIconDialog(appIcon, onDismiss = { choosingIcon = false }, onCustom = { choosingIcon = false; customIconPage = true }) { icon ->
        choosingIcon = false
        if (icon != appIcon) {
            com.kirox.app.settings.AppIcons.set(context, icon)
            appIcon = icon
            onNotice("Иконка изменится на рабочем столе через несколько секунд")
        }
    }
    if (choosingDoubleTap) ChoiceDialog("Двойной тап по сообщению", com.kirox.app.settings.DoubleTapAction.entries, tweaks.doubleTap, { it.title }, onDismiss = { choosingDoubleTap = false }) {
        set(tweaks.copy(doubleTap = it)); choosingDoubleTap = false
    }
    restoreText?.let { text ->
        AlertDialog(
            onDismissRequest = { restoreText = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Восстановить настройки?", color = Fusion.colors.textPrimary) },
            text = { Text("Текущие настройки kiroX заменятся настройками из файла, и kiroX перезапустится.", color = Fusion.colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    restoreText = null
                    scope.launch {
                        if (com.kirox.app.settings.SettingsBackup.restore(context, text)) com.kirox.app.settings.SettingsBackup.restart(context)
                        else onNotice("Это не файл настроек kiroX")
                    }
                }) { Text("Восстановить", color = Fusion.colors.accent) }
            },
            dismissButton = { TextButton(onClick = { restoreText = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Сбросить настройки kiroX?", color = Fusion.colors.textPrimary) },
        text = { Text("Тема, пузыри, обои, анимации и размер текста вернутся к значениям по умолчанию.", color = Fusion.colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = {
                confirmReset = false
                (context.applicationContext as KiroXApplication).settings.resetAll()
                onNotice("Настройки сброшены")
            }) { Text("Сбросить", color = Fusion.colors.danger) }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Выйти из аккаунта?", color = Fusion.colors.textPrimary) },
        text = { Text("Сеанс kiroX на этом устройстве будет завершён.", color = Fusion.colors.textSecondary) },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onLogOut() }) { Text("Выйти", color = Fusion.colors.danger) } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

/** QR code with the t.me link of the profile. */
@Composable
internal fun ProfileQrDialog(profile: TelegramUserProfile?, onDismiss: () -> Unit) {
    val username = profile?.username
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text("QR-код профиля", color = Fusion.colors.textPrimary) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (username.isNullOrBlank()) {
                    Text("Чтобы получить QR-код, задайте имя пользователя в «Редактировать профиль».", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center)
                } else {
                    val link = "https://t.me/$username"
                    val matrix = remember(link) {
                        runCatching {
                            com.google.zxing.qrcode.QRCodeWriter().encode(link, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, mapOf(com.google.zxing.EncodeHintType.MARGIN to 0))
                        }.getOrNull()
                    }
                    Box(Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp)) {
                        if (matrix != null) Canvas(Modifier.fillMaxSize()) {
                            val cell = size.minDimension / matrix.width
                            for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                                if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("@$username", color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово", color = Fusion.colors.textPrimary) } }
    )
}
