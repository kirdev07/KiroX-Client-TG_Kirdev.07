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

// Settings → Privacy and security: privacy rules, passcode, app icon dialog.

/**
 * Settings → Privacy and security, laid out as in Telegram: security first (cloud password, devices,
 * blocked), then who sees and can do what, message privacy, contacts and the account's self-destruct.
 * Everything here is stored in the Telegram account.
 */
@Composable
internal fun PrivacyScreen(onBack: () -> Unit, onNotice: (String) -> Unit, onOpenDevices: () -> Unit = {}) {
    var page by remember { mutableStateOf("") }
    val repo = settingsRepo()
    val scope = rememberCoroutineScope()
    val states = remember { mutableStateMapOf<PrivacyItem, PrivacyState>() }
    var choosing by remember { mutableStateOf<PrivacyItem?>(null) }
    var password by remember { mutableStateOf<CloudPasswordState?>(null) }
    var blockedCount by remember { mutableStateOf<Int?>(null) }
    var messages by remember { mutableStateOf<MessagePrivacy?>(null) }
    var ttl by remember { mutableStateOf<AccountTtl?>(null) }
    var deletingAccount by remember { mutableStateOf(false) }
    if (deletingAccount) DeleteAccountDialog(onDismiss = { deletingAccount = false }) { deletingAccount = false; onNotice("Аккаунт удалён") }
    var choosingTtl by remember { mutableStateOf(false) }
    var confirmClearContacts by remember { mutableStateOf(false) }
    fun load() {
        scope.launch { PrivacyItem.entries.forEach { item -> runCatching { repo.privacy(item) }.onSuccess { states[item] = it } } }
        scope.launch { runCatching { repo.passwordState() }.onSuccess { password = it } }
        scope.launch { runCatching { repo.blockedCount() }.onSuccess { blockedCount = it } }
        scope.launch { runCatching { repo.messagePrivacy() }.onSuccess { messages = it } }
        scope.launch { runCatching { repo.accountTtl() }.onSuccess { ttl = it } }
    }
    LaunchedEffect(Unit) { load() }
    when (page) {
        "password" -> { CloudPasswordScreen(onBack = { page = ""; load() }, onNotice = onNotice); return }
        "blocked" -> { BlockedUsersScreen(onBack = { page = ""; load() }, onNotice = onNotice); return }
        "passcode" -> { PasscodeSettingsScreen(onBack = { page = "" }, onNotice = onNotice); return }
    }
    val passcode by com.kirox.app.security.PasscodeLock.settings.collectAsState()
    BackHandler(onBack = onBack)
    /** Saves one message-privacy switch; a refusal (often Telegram Premium only) puts it back. */
    fun saveMessages(next: MessagePrivacy, save: suspend () -> Unit) {
        val old = messages
        messages = next
        scope.launch {
            runCatching { save() }.onFailure { error ->
                messages = old
                onNotice(if ((error as? com.kirox.app.telegram.TdException)?.message.orEmpty().contains("PREMIUM", true)) "Доступно с Telegram Premium" else "Не удалось сохранить")
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Конфиденциальность", onBack) }
        item { SmallCaption("Безопасность") }
        item {
            SettingsCard {
                IconRow(IconGlyphs.Lock, Color(0xFF9B6BFF), "Код-пароль", if (passcode.enabled) "Включён · ${passcode.autoLock.title.lowercase()}" else "Выключен: kiroX открывается без кода") { page = "passcode" }
                CardDivider()
                IconRow(IconGlyphs.Shield, Color(0xFF34C38F), "Облачный пароль", when (password?.hasPassword) { true -> "Включён"; false -> "Выключен"; null -> "…" }) { page = "password" }
                CardDivider()
                IconRow(IconGlyphs.Devices, Color(0xFF4F8DF7), "Устройства", "Где выполнен вход в ваш аккаунт", onClick = onOpenDevices)
                CardDivider()
                IconRow(IconGlyphs.Shield, Color(0xFFFF6B6B), "Чёрный список", blockedCount?.let { if (it == 0) "Пусто" else "Заблокировано: $it" } ?: "…") { page = "blocked" }
            }
        }
        listOf(
            "Профиль" to listOf(PrivacyItem.PhoneNumber, PrivacyItem.FindByPhone, PrivacyItem.LastSeen, PrivacyItem.ProfilePhoto, PrivacyItem.Bio, PrivacyItem.Birthdate, PrivacyItem.ProfileAudio, PrivacyItem.Gifts),
            "Сообщения и звонки" to listOf(PrivacyItem.ForwardLink, PrivacyItem.Calls, PrivacyItem.PeerToPeer, PrivacyItem.VoiceNotes, PrivacyItem.ChatInvites)
        ).forEach { (caption, group) ->
            item(key = caption) {
                SmallCaption(caption)
                SettingsCard {
                    group.forEachIndexed { index, item ->
                        if (index > 0) CardDivider()
                        val state = states[item]
                        IconRow(item.icon(), item.tint(), item.title, null, trailing = state?.let { it.level.title + if (it.exceptions > 0) " (+${it.exceptions})" else "" } ?: "…") {
                            if (state != null) choosing = item
                        }
                    }
                }
            }
        }
        item {
            Text(
                "«+N» — исключения для отдельных людей и чатов. Их можно настроить в официальном приложении, kiroX их сохраняет.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        item { SmallCaption("Сообщения") }
        item {
            val m = messages
            SettingsCard {
                if (m == null) Loading() else {
                    ToggleRow("Время прочтения", "Показывать, когда вы прочитали сообщение; выключив, вы не увидите его и у других", m.showReadDate ?: true, enabled = m.showReadDate != null) { on ->
                        saveMessages(m.copy(showReadDate = on)) { repo.setShowReadDate(on) }
                    }
                    ToggleRow("Новые чаты от всех", "Выключите, чтобы писать вам могли только контакты", m.newChatsFromStrangers ?: true, enabled = m.newChatsFromStrangers != null) { on ->
                        saveMessages(m.copy(newChatsFromStrangers = on)) { repo.setNewChatsFromStrangers(on) }
                    }
                    ToggleRow("Новые чаты — в архив", "Чаты от людей не из контактов попадают в архив без звука", m.archiveNewChats ?: false, enabled = m.archiveNewChats != null) { on ->
                        saveMessages(m.copy(archiveNewChats = on)) { repo.setArchiveNewChats(on) }
                    }
                }
            }
        }
        item { SmallCaption("Контакты") }
        item {
            val m = messages
            SettingsCard {
                ToggleRow("Частые собеседники", "Подсказывать их в поиске и при пересылке", m?.suggestFrequent ?: true, enabled = m?.suggestFrequent != null) { on ->
                    m?.let { saveMessages(it.copy(suggestFrequent = on)) { repo.setSuggestFrequent(on) } }
                }
                CardDivider()
                LinkRow("Удалить синхронизированные контакты", "Контакты из телефонной книги, загруженные в Telegram", Fusion.colors.danger) { confirmClearContacts = true }
            }
        }
        item { SmallCaption("Удаление аккаунта") }
        item {
            SettingsCard {
                IconRow(IconGlyphs.Delete, Color(0xFF8E8E93), "Если меня не было", null, trailing = ttl?.title ?: "…") { if (ttl != null) choosingTtl = true }
                CardDivider()
                IconRow(IconGlyphs.Delete, Fusion.colors.danger, "Удалить аккаунт сейчас", "Навсегда, со всеми сообщениями и контактами") { deletingAccount = true }
            }
        }
        item {
            Text(
                "Если вы ни разу не зайдёте в Telegram за этот срок, аккаунт удалится вместе со всеми сообщениями и контактами.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
    }
    choosing?.let { item ->
        val current = states[item] ?: return@let
        ChoiceDialog(item.title, item.levels, current.level, { it.title }, onDismiss = { choosing = null }) { level ->
            choosing = null
            states[item] = current.copy(level = level)
            scope.launch { runCatching { repo.setPrivacy(item, level) }.onFailure { states[item] = current; onNotice("Не удалось сохранить") } }
        }
    }
    if (choosingTtl) ttl?.let { current ->
        ChoiceDialog("Удалить аккаунт, если меня не было", AccountTtl.entries, current, { it.title }, onDismiss = { choosingTtl = false }) { value ->
            choosingTtl = false
            ttl = value
            scope.launch { runCatching { repo.setAccountTtl(value) }.onFailure { ttl = current; onNotice("Не удалось сохранить") } }
        }
    }
    if (confirmClearContacts) AlertDialog(
        onDismissRequest = { confirmClearContacts = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Удалить синхронизированные контакты?", color = Fusion.colors.textPrimary) },
        text = { Text("Telegram удалит загруженные номера из телефонной книги. Ваш список контактов в Telegram останется.", color = Fusion.colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = {
                confirmClearContacts = false
                scope.launch { runCatching { repo.clearImportedContacts() }.onSuccess { onNotice("Синхронизированные контакты удалены") }.onFailure { onNotice("Не удалось удалить") } }
            }) { Text("Удалить", color = Fusion.colors.danger) }
        },
        dismissButton = { TextButton(onClick = { confirmClearContacts = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

/**
 * Код-пароль: turn the local app lock on (code twice), change it, auto-lock delay, fingerprint,
 * hiding the content, lock now, turn it off (current code first).
 */
@Composable
private fun PasscodeSettingsScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    val context = LocalContext.current
    val settings by com.kirox.app.security.PasscodeLock.settings.collectAsState()
    // "" settings; "new" / "repeat" setting a code; "off" / "change" checking the current one first.
    var step by remember { mutableStateOf("") }
    // Kind of the code being set: digits or a graphic key.
    var newPattern by remember { mutableStateOf(false) }
    var choosingKind by remember { mutableStateOf(false) }
    var firstCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shake by remember { mutableIntStateOf(0) }
    var choosingAuto by remember { mutableStateOf(false) }
    val canBiometric = remember { com.kirox.app.ui.auth.biometricAvailable(context) }
    fun reset() { step = ""; firstCode = ""; error = null }
    if (step.isNotEmpty()) {
        BackHandler { reset() }
        val (title, subtitle) = when (step) {
            "new" -> if (newPattern) "Нарисуйте графический ключ" to "Соедините хотя бы 4 точки. Ключ хранится только на этом телефоне"
                else "Придумайте код-пароль" to "4 цифры. Код хранится только на этом телефоне"
            "repeat" -> if (newPattern) "Повторите графический ключ" to null else "Повторите код-пароль" to null
            else -> "Введите текущий код" to null
        }
        val drawing = if (step == "new" || step == "repeat") newPattern else settings.pattern
        com.kirox.app.ui.auth.PasscodeEntry(title, subtitle, error, shake, pattern = drawing) { code ->
            when (step) {
                "new" -> { firstCode = code; error = null; step = "repeat" }
                "repeat" -> if (code == firstCode) {
                    val changing = settings.enabled
                    com.kirox.app.security.PasscodeLock.setCode(code, newPattern)
                    reset()
                    onNotice(if (changing) "Код-пароль изменён" else "Код-пароль включён")
                } else { error = "Коды не совпадают, придумайте заново"; firstCode = ""; step = "new"; shake++ }
                "off" -> if (com.kirox.app.security.PasscodeLock.matches(code)) { com.kirox.app.security.PasscodeLock.disable(); reset(); onNotice("Код-пароль выключен") } else { error = "Неверный код"; shake++ }
                "change" -> if (com.kirox.app.security.PasscodeLock.matches(code)) { error = null; step = ""; choosingKind = true } else { error = "Неверный код"; shake++ }
            }
        }
        return
    }
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Код-пароль", onBack) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF9B6BFF)), contentAlignment = Alignment.Center) {
                    IconGlyph(IconGlyphs.Lock, null, Modifier.size(36.dp), Color.White)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (settings.enabled) "kiroX просит код при запуске и после того, как побыл в фоне."
                    else "Защитите переписку на этом телефоне: kiroX будет просить 4-значный код при открытии. Это не облачный пароль Telegram.",
                    color = Fusion.colors.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center
                )
            }
        }
        if (!settings.enabled) {
            item { com.kirox.app.ui.components.FusionButton("Включить код-пароль", onClick = { choosingKind = true }, modifier = Modifier.padding(16.dp)) }
        } else {
            item {
                SettingsCard {
                    LinkRow("Сменить код-пароль", if (settings.pattern) "Сейчас: графический ключ" else "Сейчас: PIN-код") { step = "change" }
                    LinkRow("Автоблокировка", settings.autoLock.title) { choosingAuto = true }
                }
            }
            item { SmallCaption("Вход") }
            item {
                SettingsCard {
                    ToggleRow(
                        "Вход по отпечатку", if (canBiometric) "Вместо кода; код тоже всегда подходит" else "На телефоне не настроен отпечаток или лицо",
                        settings.biometric && canBiometric, enabled = canBiometric
                    ) { com.kirox.app.security.PasscodeLock.setBiometric(it) }
                    ToggleRow("Скрывать содержимое", "Пустой экран в списке недавних приложений, скриншоты запрещены", settings.hideContent) {
                        com.kirox.app.security.PasscodeLock.setHideContent(it)
                    }
                }
            }
            item {
                SettingsCard {
                    LinkRow("Заблокировать сейчас") { com.kirox.app.security.PasscodeLock.lockNow() }
                    LinkRow("Выключить код-пароль", null, Fusion.colors.danger) { step = "off" }
                }
            }
            item {
                Text(
                    "Если забудете код, его не восстановить: придётся выйти из аккаунта и войти снова по номеру телефона.",
                    color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }
    if (choosingKind) AlertDialog(
        onDismissRequest = { choosingKind = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Чем закрывать kiroX", color = Fusion.colors.textPrimary) },
        text = {
            Column {
                LinkRow("PIN-код", "4 цифры") { choosingKind = false; newPattern = false; step = "new" }
                LinkRow("Графический ключ", "Соединить точки на экране") { choosingKind = false; newPattern = true; step = "new" }
            }
        },
        confirmButton = { TextButton(onClick = { choosingKind = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    if (choosingAuto) ChoiceDialog("Автоблокировка", com.kirox.app.security.AutoLock.entries, settings.autoLock, { it.title }, onDismiss = { choosingAuto = false }) {
        com.kirox.app.security.PasscodeLock.setAutoLock(it); choosingAuto = false
    }
}

/** «Иконка приложения»: the four launcher icons drawn as they look on the home screen. */
@Composable
internal fun AppIconDialog(current: com.kirox.app.settings.AppIcon, onDismiss: () -> Unit, onCustom: () -> Unit = {}, onPick: (com.kirox.app.settings.AppIcon) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text("Иконка приложения", color = Fusion.colors.textPrimary) },
        text = {
            Column {
                Text("Так kiroX будет выглядеть на рабочем столе. Если иконка пропадёт, найдите kiroX в списке приложений — лаунчер обновит её.", color = Fusion.colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
                com.kirox.app.settings.AppIcon.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { icon ->
                            val selected = icon == current
                            Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable { onPick(icon) }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val background = when (icon) {
                                    com.kirox.app.settings.AppIcon.Violet -> androidx.compose.ui.graphics.SolidColor(Color(0xFF6C4CE0))
                                    com.kirox.app.settings.AppIcon.Dark -> androidx.compose.ui.graphics.SolidColor(Color(0xFF121214))
                                    com.kirox.app.settings.AppIcon.Light, com.kirox.app.settings.AppIcon.White -> androidx.compose.ui.graphics.SolidColor(Color.White)
                                    com.kirox.app.settings.AppIcon.Cosmos -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF7B4CE0), Color(0xFF1B2A6B)))
                                    com.kirox.app.settings.AppIcon.Green -> androidx.compose.ui.graphics.SolidColor(Color(0xFF1FA463))
                                }
                                val plane = when (icon) {
                                    com.kirox.app.settings.AppIcon.Light -> com.kirox.app.R.drawable.ic_launcher_foreground_violet
                                    com.kirox.app.settings.AppIcon.White -> com.kirox.app.R.drawable.ic_launcher_foreground_green
                                    else -> com.kirox.app.R.drawable.ic_launcher_foreground
                                }
                                Box(
                                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(background)
                                        .then(if (selected) Modifier.border(2.5.dp, Fusion.colors.accent, RoundedCornerShape(16.dp)) else Modifier),
                                    contentAlignment = Alignment.Center
                                ) { androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(plane), null, Modifier.size(84.dp)) }
                                Spacer(Modifier.height(6.dp))
                                Text(icon.title, color = if (selected) Fusion.colors.accent else Fusion.colors.textPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "Зелёная и белая иконки красят в свой цвет и нижнюю панель — выбранную вкладку и кнопки.",
                    color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp
                )
                androidx.compose.material3.OutlinedButton(onClick = onCustom, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Создать свою иконку") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = Fusion.colors.textSecondary) } }
    )
}

internal fun PrivacyItem.icon(): IconGlyphs = when (this) {
    PrivacyItem.PhoneNumber -> IconGlyphs.Phone
    PrivacyItem.FindByPhone -> IconGlyphs.Search
    PrivacyItem.LastSeen -> IconGlyphs.Eye
    PrivacyItem.ProfilePhoto -> IconGlyphs.Photo
    PrivacyItem.Bio -> IconGlyphs.Info
    PrivacyItem.Birthdate -> IconGlyphs.Cake
    PrivacyItem.ProfileAudio -> IconGlyphs.Music
    PrivacyItem.Gifts -> IconGlyphs.Star
    PrivacyItem.ForwardLink -> IconGlyphs.Forward
    PrivacyItem.Calls -> IconGlyphs.Call
    PrivacyItem.PeerToPeer -> IconGlyphs.Link
    PrivacyItem.VoiceNotes -> IconGlyphs.Voice
    PrivacyItem.ChatInvites -> IconGlyphs.Contacts
}

internal fun PrivacyItem.tint(): Color = when (this) {
    PrivacyItem.PhoneNumber, PrivacyItem.FindByPhone -> Color(0xFF34C38F)
    PrivacyItem.LastSeen, PrivacyItem.ProfilePhoto -> Color(0xFF4F8DF7)
    PrivacyItem.Bio, PrivacyItem.Birthdate -> Color(0xFFFFA94D)
    PrivacyItem.ProfileAudio, PrivacyItem.Gifts -> Color(0xFFF06595)
    PrivacyItem.ForwardLink, PrivacyItem.ChatInvites -> Color(0xFF9B6BFF)
    PrivacyItem.Calls, PrivacyItem.PeerToPeer -> Color(0xFF34C38F)
    PrivacyItem.VoiceNotes -> Color(0xFFFF6B6B)
}
