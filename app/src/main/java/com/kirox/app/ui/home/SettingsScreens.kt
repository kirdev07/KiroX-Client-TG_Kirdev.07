package com.kirox.app.ui.home

import androidx.core.net.toUri
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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

@Composable
private fun settingsRepo() = (LocalContext.current.applicationContext as KiroXApplication).telegram.settings

@Composable
internal fun ToggleRow(title: String, subtitle: String? = null, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    // Material 3 list item with a switch; the whole row toggles.
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked, onChange, enabled = enabled) },
        colors = fusionListColors(),
        modifier = Modifier.clickable(enabled = enabled) { onChange(!checked) }
    )
}

@Composable
private fun LinkRow(title: String, subtitle: String? = null, color: Color = Fusion.colors.textPrimary, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title, color = color) },
        supportingContent = subtitle?.let { { Text(it) } },
        colors = fusionListColors(),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.textPrimary) }
}

// ---------- Notifications ----------

/**
 * Settings → Notifications and sounds, laid out as in Telegram: one row per chat type with its switch
 * (stored in Telegram), events, calls, system sound settings and what keeps delivery reliable.
 */
@Composable
internal fun NotificationSettingsScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    var page by remember { mutableStateOf<String?>(null) }
    val repo = settingsRepo()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { mutableStateMapOf<NotifyScope, ScopeNotifications>() }
    var contactJoined by remember { mutableStateOf<Boolean?>(null) }
    var reactions by remember { mutableStateOf(repo.reactionNotifications()) }
    var choosingReactions by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val localPrefs = (context.applicationContext as KiroXApplication).telegram.notificationPrefs
    val local by localPrefs.settings.collectAsState()
    fun load() {
        scope.launch { NotifyScope.entries.forEach { s -> runCatching { repo.notifications(s) }.onSuccess { state[s] = it } } }
        scope.launch { runCatching { repo.contactJoinedNotifications() }.onSuccess { contactJoined = it } }
        reactions = repo.reactionNotifications()
    }
    fun updateReactions(value: ReactionNotifications) {
        val old = reactions
        reactions = value
        scope.launch { runCatching { repo.setReactionNotifications(value) }.onFailure { reactions = old; onNotice("Не удалось сохранить") } }
    }
    LaunchedEffect(Unit) { load() }
    fun update(s: NotifyScope, value: ScopeNotifications) {
        val old = state[s]
        state[s] = value
        scope.launch { runCatching { repo.setNotifications(s, value) }.onFailure { old?.let { state[s] = it }; onNotice("Не удалось сохранить") } }
    }
    when (val current = page) {
        "exceptions" -> { NotificationExceptionsScreen(onBack = { page = null; load() }); return }
        null -> Unit
        else -> NotifyScope.entries.firstOrNull { it.name == current }?.let { s ->
            NotifyScopeScreen(s, state[s], onChange = { update(s, it) }, onExceptions = { page = "exceptions" }, onSound = { openChannelSettings(context, s.channel()) }, onBack = { page = null })
            return
        }
    }
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Уведомления и звуки", onBack) }
        item { SmallCaption("Уведомления о сообщениях") }
        item {
            SettingsCard {
                NotifyScope.entries.forEachIndexed { index, s ->
                    if (index > 0) androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 72.dp), color = Fusion.colors.outline)
                    ScopeRow(s, state[s], onOpen = { page = s.name }) { value -> update(s, value.copy(enabled = !value.enabled)) }
                }
            }
        }
        item { LinkRow("Все исключения", "Чаты, где уведомления настроены отдельно") { page = "exceptions" } }
        item { SmallCaption("Реакции") }
        item {
            val r = reactions
            SettingsCard {
                if (r == null) Text("Настройки реакций появятся, когда kiroX получит их от Telegram.", color = Fusion.colors.textTertiary, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                else {
                    LinkRow("На сообщения", r.messages.title) { choosingReactions = "messages" }
                    LinkRow("На истории", r.stories.title) { choosingReactions = "stories" }
                    ToggleRow("Показывать реакцию", "Какой эмодзи поставили, в тексте уведомления", r.showPreview) { updateReactions(r.copy(showPreview = it)) }
                }
            }
        }
        item { SmallCaption("В приложении") }
        item {
            SettingsCard {
                ToggleRow("Звук", "Короткий сигнал о новом сообщении, пока kiroX открыт", local.inAppSound) { localPrefs.update(local.copy(inAppSound = it)) }
                ToggleRow("Вибрация", checked = local.inAppVibrate) { localPrefs.update(local.copy(inAppVibrate = it)) }
                ToggleRow("Баннер сверху", "Отправитель и текст поверх экрана; нажмите, чтобы открыть чат", local.inAppPreview) { localPrefs.update(local.copy(inAppPreview = it)) }
            }
        }
        item {
            Text("В открытом сейчас чате ни звука, ни баннера не будет.", color = Fusion.colors.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        item { SmallCaption("Значок на иконке") }
        item {
            SettingsCard {
                ToggleRow("Счётчик непрочитанных", "Число на значке kiroX, если его показывает ваш лаунчер", local.badge) { localPrefs.update(local.copy(badge = it)) }
                ToggleRow("Считать сообщения", if (local.badgeCountsMessages) "Каждое новое сообщение" else "Один на каждый чат", local.badgeCountsMessages, enabled = local.badge) {
                    localPrefs.update(local.copy(badgeCountsMessages = it))
                }
            }
        }
        item { SmallCaption("События") }
        item {
            ToggleRow("Контакт присоединился к Telegram", "Сообщение, когда кто-то из ваших контактов заводит Telegram", contactJoined ?: true, enabled = contactJoined != null) { on ->
                contactJoined = on
                scope.launch { runCatching { repo.setContactJoinedNotifications(on) }.onFailure { contactJoined = !on; onNotice("Не удалось сохранить") } }
            }
        }
        item { SmallCaption("Звук и вибрация") }
        item {
            SettingsCard {
                NotifyScope.entries.forEach { s -> LinkRow(s.title, "Мелодия, вибрация, всплывающие") { openChannelSettings(context, s.channel()) } }
                LinkRow("Входящие звонки", "Мелодия звонка и вибрация") { openChannelSettings(context, com.kirox.app.voip.CallNotifications.CHANNEL_INCOMING) }
            }
        }
        item {
            LinkRow("Все уведомления kiroX", "Системные настройки приложения") {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        item {
            // Re-read when coming back from the system screens below.
            var resumes by remember { mutableIntStateOf(0) }
            androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { resumes++; onPauseOrDispose { } }
            // Android 14+: full-screen incoming calls need a separate permission the user may have turned off.
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                SmallCaption("Звонки")
                val notifications = remember { context.getSystemService(android.app.NotificationManager::class.java) }
                val fullScreen = remember(resumes) { notifications?.canUseFullScreenIntent() == true }
                LinkRow(
                    "Звонки на весь экран",
                    if (fullScreen) "Разрешено: входящий звонок открывается поверх блокировки" else "Выключено: входящий звонок будет только уведомлением"
                ) {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
            SmallCaption("Надёжная доставка")
            val power = remember { context.getSystemService(android.os.PowerManager::class.java) }
            val unrestricted = remember(resumes) { power?.isIgnoringBatteryOptimizations(context.packageName) == true }
            LinkRow(
                "Без ограничений батареи",
                if (unrestricted) "Включено: система не усыпляет kiroX" else "Чтобы звонки и сообщения не задерживались в режиме экономии"
            ) {
                if (!unrestricted) runCatching {
                    @android.annotation.SuppressLint("BatteryLife")
                    val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
                    context.startActivity(request.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            LinkRow("Работа в фоне", "Чтобы уведомления и звонки приходили, когда kiroX свёрнут") {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
        item { SmallCaption("Сброс") }
        item { LinkRow("Сбросить все уведомления", "Настройки Telegram по умолчанию, исключения удалятся", Fusion.colors.danger) { confirmReset = true } }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Сбросить все уведомления?", color = Fusion.colors.textPrimary) },
        text = { Text("Для всех чатов вернутся настройки по умолчанию, отдельные настройки чатов удалятся.", color = Fusion.colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = {
                confirmReset = false
                localPrefs.reset()
                scope.launch { runCatching { repo.resetAllNotifications() }.onSuccess { onNotice("Уведомления сброшены"); load() }.onFailure { onNotice("Не удалось сбросить") } }
            }) { Text("Сбросить", color = Fusion.colors.danger) }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    reactions?.let { r ->
        when (choosingReactions) {
            "messages" -> ChoiceDialog("Реакции на сообщения", ReactionSource.entries, r.messages, { it.title }, onDismiss = { choosingReactions = null }) { updateReactions(r.copy(messages = it)); choosingReactions = null }
            "stories" -> ChoiceDialog("Реакции на истории", ReactionSource.entries, r.stories, { it.title }, onDismiss = { choosingReactions = null }) { updateReactions(r.copy(stories = it)); choosingReactions = null }
        }
    }
}

/** Android channel of a chat type: its sound and vibration are set there. */
private fun NotifyScope.channel(): String = when (this) {
    NotifyScope.Private -> com.kirox.app.notify.MessageNotifications.CHANNEL
    NotifyScope.Groups -> com.kirox.app.notify.MessageNotifications.CHANNEL_GROUPS
    NotifyScope.Channels -> com.kirox.app.notify.MessageNotifications.CHANNEL_CHANNELS
}

private fun openChannelSettings(context: android.content.Context, channel: String) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, channel).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Rounded group of rows on the screen background, as Telegram's settings sections. */
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Fusion.colors.surface1), content = content)
}

private fun NotifyScope.icon(): Pair<com.kirox.app.ui.icons.KiroXIcon, Color> = when (this) {
    NotifyScope.Private -> com.kirox.app.ui.icons.KiroXIcon.Profile to Color(0xFF4F8DF7)
    NotifyScope.Groups -> com.kirox.app.ui.icons.KiroXIcon.Contacts to Color(0xFF34C38F)
    NotifyScope.Channels -> com.kirox.app.ui.icons.KiroXIcon.Channel to Color(0xFFFFA94D)
}

private fun ScopeNotifications.summary(): String = buildString {
    append(if (enabled) "Включены" else "Выключены")
    if (enabled && !showPreview) append(" · без текста")
    if (exceptions > 0) append(" · исключений: $exceptions")
}

/** Chat type with a colored icon, its state and a switch; the row opens the type's settings. */
@Composable
private fun ScopeRow(scope: NotifyScope, value: ScopeNotifications?, onOpen: () -> Unit, onToggle: (ScopeNotifications) -> Unit) {
    val (icon, tint) = scope.icon()
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).heightIn(min = 68.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint), contentAlignment = Alignment.Center) {
            IconGlyph(icon, null, Modifier.size(22.dp), Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(scope.title, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(value?.summary() ?: "…", color = Fusion.colors.textSecondary, fontSize = 13.sp)
        }
        // A thin line splits the row: left opens the settings, the switch only turns them on or off.
        Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(32.dp).background(Fusion.colors.outline))
        Switch(value?.enabled == true, { if (value != null) onToggle(value) }, enabled = value != null)
    }
}

/** One chat type: on/off, message text, pinned messages, mentions (groups) and its exceptions. */
@Composable
private fun NotifyScopeScreen(scope: NotifyScope, value: ScopeNotifications?, onChange: (ScopeNotifications) -> Unit, onExceptions: () -> Unit, onSound: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle(scope.title, onBack) }
        if (value == null) item { Loading() } else {
            item {
                SettingsCard { ToggleRow("Показывать уведомления", checked = value.enabled) { onChange(value.copy(enabled = it)) } }
            }
            item { SmallCaption("Настройки") }
            item {
                SettingsCard {
                    ToggleRow("Текст сообщения", "Имя и текст в уведомлении; без него — только «Новое сообщение»", value.showPreview, enabled = value.enabled) { onChange(value.copy(showPreview = it)) }
                    ToggleRow("Закреплённые сообщения", "Уведомлять, когда в чате что-то закрепили", value.pinned, enabled = value.enabled) { onChange(value.copy(pinned = it)) }
                    if (scope == NotifyScope.Groups) ToggleRow("Упоминания и ответы", "Уведомлять об упоминаниях, даже если группа без звука", value.mentions) { onChange(value.copy(mentions = it)) }
                    if (scope == NotifyScope.Private) ToggleRow("Новые истории", "Когда контакт публикует историю", value.stories) { onChange(value.copy(stories = it)) }
                }
            }
            item { SmallCaption("Звук") }
            item { SettingsCard { LinkRow("Звук и вибрация", "Мелодия, вибрация и всплывающие для «${scope.title}»", onClick = onSound) } }
            item { SmallCaption("Исключения") }
            item { LinkRow(if (value.exceptions > 0) "Чатов с отдельными настройками: ${value.exceptions}" else "Исключений нет", "Открыть список исключений", onClick = onExceptions) }
        }
    }
}

// ---------- Privacy ----------

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
    }
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
                IconRow(IconGlyphs.Lock, Color(0xFF34C38F), "Облачный пароль", when (password?.hasPassword) { true -> "Включён"; false -> "Выключен"; null -> "…" }) { page = "password" }
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

@Composable
private fun CardDivider() = androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 72.dp), color = Fusion.colors.outline)

/** Settings row with a colored square icon, as in Telegram: title, optional subtitle, value on the right. */
@Composable
private fun IconRow(icon: IconGlyphs, tint: Color, title: String, subtitle: String?, trailing: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tint), contentAlignment = Alignment.Center) {
            IconGlyph(icon, null, Modifier.size(20.dp), Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Fusion.colors.textPrimary, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, color = Fusion.colors.textSecondary, fontSize = 13.sp)
        }
        if (trailing != null) Text(trailing, color = Fusion.colors.accent, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun PrivacyItem.icon(): IconGlyphs = when (this) {
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

private fun PrivacyItem.tint(): Color = when (this) {
    PrivacyItem.PhoneNumber, PrivacyItem.FindByPhone -> Color(0xFF34C38F)
    PrivacyItem.LastSeen, PrivacyItem.ProfilePhoto -> Color(0xFF4F8DF7)
    PrivacyItem.Bio, PrivacyItem.Birthdate -> Color(0xFFFFA94D)
    PrivacyItem.ProfileAudio, PrivacyItem.Gifts -> Color(0xFFF06595)
    PrivacyItem.ForwardLink, PrivacyItem.ChatInvites -> Color(0xFF9B6BFF)
    PrivacyItem.Calls, PrivacyItem.PeerToPeer -> Color(0xFF34C38F)
    PrivacyItem.VoiceNotes -> Color(0xFFFF6B6B)
}

// ---------- Data and storage ----------

/**
 * Settings → Data and storage, as in Telegram: memory use and cache rules, traffic, auto-download
 * per network, GIF autoplay and the proxy.
 */
@Composable
internal fun StorageScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    var page by remember { mutableStateOf<String?>(null) }
    when (val current = page) {
        "proxy" -> { ProxyScreen(onBack = { page = null }, onNotice = onNotice); return }
        "traffic" -> { NetworkUsageScreen(onBack = { page = null }, onNotice = onNotice); return }
        null -> Unit
        else -> DownloadNetwork.entries.firstOrNull { it.name == current }?.let { network -> AutoDownloadScreen(network, onBack = { page = null }); return }
    }
    BackHandler(onBack = onBack)
    val repo = settingsRepo()
    val services = (LocalContext.current.applicationContext as KiroXApplication).telegram
    val autoDownload = services.autoDownload
    val storage = services.storage
    val downloads by autoDownload.settings.collectAsState()
    val gifs by autoDownload.autoplayGifs.collectAsState()
    val keepMedia by storage.keepMedia.collectAsState()
    val cacheLimit by storage.cacheLimit.collectAsState()
    val media = services.media
    val gallery by media.saveToGallery.collectAsState()
    val callSaving by media.callDataSaving.collectAsState()
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf<StorageBreakdown?>(null) }
    var selected by remember { mutableStateOf(StorageCategory.entries.toSet()) }
    var clearing by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var chatToClear by remember { mutableStateOf<ChatStorage?>(null) }
    var choosing by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    fun reload() { scope.launch { runCatching { repo.storageBreakdown() }.onSuccess { usage = it } } }
    val selectedBytes = usage?.let { u -> selected.sumOf { u.byCategory[it] ?: 0L } } ?: 0L
    LaunchedEffect(Unit) { reload() }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Данные и память", onBack) }
        item { SmallCaption("Использование памяти") }
        item {
            val u = usage
            if (u == null) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Loading()
                Text("Считаем, сколько занимает кэш…", color = Fusion.colors.textSecondary, fontSize = 13.sp)
            } else StorageUsageCard(u, selected) { category -> selected = if (category in selected) selected - category else selected + category }
        }
        item {
            Box(
                Modifier.padding(16.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp))
                    .background(Fusion.colors.surface2).clickable(enabled = !clearing && selectedBytes > 0) { confirm = true },
                contentAlignment = Alignment.Center
            ) {
                if (clearing) CircularProgressIndicator(Modifier.size(22.dp), color = Fusion.colors.textPrimary, strokeWidth = 2.dp)
                else Text(
                    if (selectedBytes > 0) "Очистить выбранное · ${formatBytes(selectedBytes)}" else "Нечего очищать",
                    color = if (selectedBytes > 0) Fusion.colors.danger else Fusion.colors.textTertiary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
        item {
            Text(
                "Сообщения остаются в облаке Telegram: удалённые фото, видео и файлы загрузятся снова, когда вы их откроете.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        item { SmallCaption("Автоочистка") }
        item { LinkRow("Хранить медиа", keepMedia.title) { choosing = "keep" } }
        item { LinkRow("Максимальный размер кэша", cacheLimit.title) { choosing = "limit" } }
        item {
            Text(
                "Фото, видео и файлы, которые вы не открывали дольше выбранного срока, удаляются с устройства.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        val chats = usage?.chats.orEmpty()
        if (chats.isNotEmpty()) {
            item { SmallCaption("Больше всего занимают") }
            items(chats, key = { "chat_${it.chatId}" }) { chat ->
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(chat.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                    trailingContent = { Text(formatBytes(chat.bytes), color = Fusion.colors.textSecondary, fontSize = 14.sp) },
                    colors = fusionListColors(),
                    modifier = Modifier.clickable { chatToClear = chat }
                )
            }
        }
        item { SmallCaption("Использование трафика") }
        item { LinkRow("Использование сети", "Сколько отправлено и получено через мобильную сеть, Wi-Fi и в роуминге") { page = "traffic" } }
        item { SmallCaption("Автозагрузка медиа") }
        DownloadNetwork.entries.forEach { network ->
            item(key = network.name) { LinkRow(network.title, downloads[network]?.summary() ?: "…") { page = network.name } }
        }
        item { LinkRow("Сбросить настройки автозагрузки", null, Fusion.colors.danger) { confirmReset = true } }
        item { SmallCaption("Автовоспроизведение") }
        item { ToggleRow("GIF", "Загружать и проигрывать GIF в чатах автоматически", gifs) { autoDownload.setAutoplayGifs(it) } }
        item { SmallCaption("Сохранять в галерею") }
        listOf(TelegramChatKind.Private to "Личные чаты", TelegramChatKind.Group to "Группы", TelegramChatKind.Channel to "Каналы").forEach { (kind, title) ->
            item(key = "gallery_$kind") { ToggleRow(title, checked = kind in gallery) { media.setSaveToGallery(kind, it) } }
        }
        item {
            Text(
                "Полученные фото и видео копируются в «Pictures/kiroX» и «Movies/kiroX» после загрузки.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        item { SmallCaption("Звонки") }
        item { LinkRow("Экономия трафика", callSaving.title) { choosing = "calls" } }
        item { SmallCaption("Сеть") }
        item { LinkRow("Прокси", "SOCKS5, HTTP или MTProto, если Telegram недоступен напрямую") { page = "proxy" } }
    }
    when (choosing) {
        "keep" -> ChoiceDialog("Хранить медиа", KeepMedia.entries, keepMedia, { it.title }, onDismiss = { choosing = null }) { storage.setKeepMedia(it); choosing = null; reload() }
        "limit" -> ChoiceDialog("Максимальный размер кэша", CacheLimit.entries, cacheLimit, { it.title }, onDismiss = { choosing = null }) { storage.setCacheLimit(it); choosing = null; reload() }
        "calls" -> ChoiceDialog("Экономия трафика в звонках", CallDataSaving.entries, callSaving, { it.title }, onDismiss = { choosing = null }) { media.setCallDataSaving(it); choosing = null }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Очистить кэш?", color = Fusion.colors.textPrimary) },
        text = {
            Text(
                "Будет освобождено около ${formatBytes(selectedBytes)}: " + selected.filter { (usage?.byCategory?.get(it) ?: 0L) > 0 }.joinToString { it.title.lowercase() } + ".",
                color = Fusion.colors.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = {
                confirm = false; clearing = true
                scope.launch {
                    runCatching { repo.clearStorage(selected) }.onSuccess { onNotice("Освобождено ${formatBytes(selectedBytes)}") }.onFailure { onNotice("Не удалось очистить кэш") }
                    clearing = false; reload()
                }
            }) { Text("Очистить", color = Fusion.colors.danger) }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    chatToClear?.let { chat ->
        AlertDialog(
            onDismissRequest = { chatToClear = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Очистить кэш «${chat.title}»?", color = Fusion.colors.textPrimary) },
            text = { Text("Будет освобождено около ${formatBytes(chat.bytes)}. Сообщения останутся.", color = Fusion.colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    chatToClear = null; clearing = true
                    scope.launch {
                        runCatching { repo.clearStorage(StorageCategory.entries.toSet(), chat.chatId) }.onSuccess { onNotice("Освобождено ${formatBytes(chat.bytes)}") }.onFailure { onNotice("Не удалось очистить кэш") }
                        clearing = false; reload()
                    }
                }) { Text("Очистить", color = Fusion.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { chatToClear = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Сбросить автозагрузку?", color = Fusion.colors.textPrimary) },
        text = { Text("Для всех сетей вернутся настройки Telegram по умолчанию, автовоспроизведение GIF включится.", color = Fusion.colors.textSecondary) },
        confirmButton = { TextButton(onClick = { confirmReset = false; autoDownload.reset(); onNotice("Настройки автозагрузки сброшены") }) { Text("Сбросить", color = Fusion.colors.danger) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

/** Colors of the storage chart, one per category; fixed, so the same type keeps its color in every theme. */
private fun StorageCategory.color(): Color = when (this) {
    StorageCategory.Photos -> Color(0xFF4F8DF7)
    StorageCategory.Videos -> Color(0xFF9B6BFF)
    StorageCategory.Files -> Color(0xFF34C38F)
    StorageCategory.Music -> Color(0xFFFF6B6B)
    StorageCategory.Voice -> Color(0xFFFFA94D)
    StorageCategory.Stories -> Color(0xFFF06595)
    StorageCategory.Stickers -> Color(0xFFFFD43B)
    StorageCategory.Other -> Color(0xFF8E8E93)
}

/**
 * Telegram-style storage usage: a ring split by type with the cache size inside, and below it every
 * type with its share and size; a checked type is cleared by «Очистить выбранное».
 */
@Composable
private fun StorageUsageCard(usage: StorageBreakdown, selected: Set<StorageCategory>, onToggle: (StorageCategory) -> Unit) {
    val parts = StorageCategory.entries.map { it to (usage.byCategory[it] ?: 0L) }.filter { it.second > 0 }
    val total = parts.sumOf { it.second }.coerceAtLeast(1)
    val track = Fusion.colors.surface3
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Fusion.colors.surface1).padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(184.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 22.dp.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, topLeft, arc, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                // Small gaps between the parts, as in Telegram; a lone part is a full ring.
                val gap = if (parts.size > 1) 2f else 0f
                var start = -90f
                parts.forEach { (category, bytes) ->
                    val sweep = 360f * bytes / total
                    val color = category.color().copy(alpha = if (category in selected) 1f else 0.3f)
                    drawArc(color, start + gap / 2, (sweep - gap).coerceAtLeast(0.6f), false, topLeft, arc, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatBytes(usage.filesBytes), color = Fusion.colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("кэш на устройстве", color = Fusion.colors.textSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (parts.isEmpty()) Text("Кэш пуст", color = Fusion.colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
        parts.forEach { (category, bytes) ->
            val percent = (bytes * 100 / total).toInt()
            Row(
                Modifier.fillMaxWidth().clickable { onToggle(category) }.padding(start = 8.dp, end = 20.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Checkbox(
                    checked = category in selected, onCheckedChange = { onToggle(category) },
                    colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = category.color(), uncheckedColor = category.color(), checkmarkColor = Color.White)
                )
                Text(category.title, color = Fusion.colors.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(if (percent < 1) "<1%" else "$percent%", color = Fusion.colors.textSecondary, fontSize = 14.sp)
                Spacer(Modifier.width(12.dp))
                Text(formatBytes(bytes), color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
        androidx.compose.material3.HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = Fusion.colors.outline)
        Text(
            "База данных: ${formatBytes(usage.databaseBytes)} — сообщения и списки чатов. Очистка кэша её не затрагивает.",
            color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

private fun NetworkDownload.summary(): String = when {
    !enabled -> "Выключено"
    else -> "Видео: ${downloadLimit(maxVideoBytes)} · файлы: ${downloadLimit(maxFileBytes)}"
}

private fun downloadLimit(bytes: Long): String = when {
    bytes <= 0 -> "не загружать"
    bytes % (1L shl 30) == 0L -> "до ${bytes shr 30} ГБ"
    bytes % (1L shl 20) == 0L -> "до ${bytes shr 20} МБ"
    else -> "до ${formatBytes(bytes)}"
}

/** Size limits offered for videos and files, as in Telegram; 0 is «не загружать». */
private val downloadLimits = listOf(0L, 1L shl 20, 5L shl 20, 10L shl 20, 25L shl 20, 50L shl 20, 100L shl 20, 500L shl 20, 1L shl 30, 2L shl 30)

/** Auto-download for one network: on/off, video and file size limits, stories. */
@Composable
private fun AutoDownloadScreen(network: DownloadNetwork, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val autoDownload = (LocalContext.current.applicationContext as KiroXApplication).telegram.autoDownload
    val all by autoDownload.settings.collectAsState()
    val value = all[network] ?: NetworkDownload(false, 0, 0, false)
    var choosing by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle(network.title, onBack) }
        item { ToggleRow("Автозагрузка медиа", "Фото всегда загружаются в чатах, остальное — по ограничениям ниже", value.enabled) { autoDownload.set(network, value.copy(enabled = it)) } }
        item { SmallCaption("Типы медиа") }
        item { LinkRow("Видео", downloadLimit(value.maxVideoBytes), if (value.enabled) Fusion.colors.textPrimary else Fusion.colors.textTertiary) { if (value.enabled) choosing = "video" } }
        item { LinkRow("Файлы", downloadLimit(value.maxFileBytes), if (value.enabled) Fusion.colors.textPrimary else Fusion.colors.textTertiary) { if (value.enabled) choosing = "file" } }
        item { ToggleRow("Истории", "Заранее загружать истории контактов", value.preloadStories, enabled = value.enabled) { autoDownload.set(network, value.copy(preloadStories = it)) } }
        item {
            Text(
                "Файлы больше выбранного размера загружаются только по нажатию. Настройки хранятся на этом устройстве.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
    when (choosing) {
        "video" -> ChoiceDialog("Видео", (downloadLimits + value.maxVideoBytes).distinct().sorted(), value.maxVideoBytes, ::downloadLimit, onDismiss = { choosing = null }) {
            autoDownload.set(network, value.copy(maxVideoBytes = it)); choosing = null
        }
        "file" -> ChoiceDialog("Файлы", (downloadLimits + value.maxFileBytes).distinct().sorted(), value.maxFileBytes, ::downloadLimit, onDismiss = { choosing = null }) {
            autoDownload.set(network, value.copy(maxFileBytes = it)); choosing = null
        }
    }
}

/** Traffic per network since the last reset, from TDLib's network statistics. */
@Composable
private fun NetworkUsageScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val repo = settingsRepo()
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf<NetworkUsage?>(null) }
    var confirm by remember { mutableStateOf(false) }
    fun reload() { scope.launch { runCatching { repo.networkUsage() }.onSuccess { usage = it }.onFailure { onNotice("Не удалось получить статистику") } } }
    LaunchedEffect(Unit) { reload() }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Использование сети", onBack) }
        val u = usage
        if (u == null) item { Loading() } else {
            DownloadNetwork.entries.forEach { network ->
                item(key = network.name) {
                    val (sent, received) = u.bytes[network] ?: (0L to 0L)
                    SmallCaption(network.title)
                    LinkRow("Отправлено", formatBytes(sent)) {}
                    LinkRow("Получено", formatBytes(received)) {}
                }
            }
            item {
                val since = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.forLanguageTag("ru")).format(java.util.Date(u.sinceDate * 1000L))
                Text("Статистика с $since.", color = Fusion.colors.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            item { LinkRow("Сбросить статистику", null, Fusion.colors.danger) { confirm = true } }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Сбросить статистику?", color = Fusion.colors.textPrimary) },
        confirmButton = {
            TextButton(onClick = {
                confirm = false
                scope.launch { runCatching { repo.resetNetworkUsage() }.onSuccess { reload() }.onFailure { onNotice("Не удалось сбросить") } }
            }) { Text("Сбросить", color = Fusion.colors.danger) }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

/** One choice out of [options], in the same dialog style as the privacy levels. */
@Composable
private fun <T> ChoiceDialog(title: String, options: List<T>, selected: T, label: (T) -> String, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text(title, color = Fusion.colors.textPrimary) },
        text = {
            LazyColumn {
                items(options) { option ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(option) }.heightIn(min = 48.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (option == selected) "●" else "○", color = Fusion.colors.textPrimary, fontSize = 16.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(label(option), color = Fusion.colors.textPrimary, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f ГБ".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.1f МБ".format(bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> "%d КБ".format(bytes / (1L shl 10))
    else -> "$bytes Б"
}

// ---------- Premium ----------

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

// ---------- Advanced ----------

/** Settings → Advanced: app info, reset of local KiroX settings, log out. */
@Composable
internal fun AdvancedScreen(onBack: () -> Unit, onLogOut: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Дополнительно", onBack) }
        item { SmallCaption("О приложении") }
        item { LinkRow("kiroX ${BuildConfig.VERSION_NAME}", "Неофициальный клиент Telegram на TDLib") {} }
        item {
            LinkRow("Исходный код и лицензии", "github.com/kirdev07/KiroX-Client-TG_Kirdev.07") {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/kirdev07/KiroX-Client-TG_Kirdev.07".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        item {
            LinkRow("Язык", "Русский · системные настройки языка") {
                // Per-app language settings exist from Android 13.
                if (android.os.Build.VERSION.SDK_INT < 33) onNotice("Пока доступен только русский язык")
                else runCatching { context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, "package:${context.packageName}".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    .onFailure { onNotice("Пока доступен только русский язык") }
            }
        }
        item { SmallCaption("Тестирование") }
        item {
            var crashes by remember { mutableStateOf(com.kirox.app.CrashLog.count(context)) }
            LinkRow("Журнал ошибок", if (crashes == 0) "Ошибок не записано" else "Записей: $crashes · нажмите, чтобы отправить разработчику") {
                if (crashes > 0) com.kirox.app.CrashLog.share(context) else onNotice("Журнал пуст")
            }
            if (crashes > 0) LinkRow("Очистить журнал", null, Fusion.colors.textSecondary) { com.kirox.app.CrashLog.clear(context); crashes = 0; onNotice("Журнал очищен") }
        }
        item { SmallCaption("Сброс") }
        item { LinkRow("Сбросить настройки kiroX", "Тема, анимации, чаты — к значениям по умолчанию. Telegram не затрагивается", Fusion.colors.danger) { confirmReset = true } }
        item { LinkRow("Выйти из аккаунта", null, Fusion.colors.danger) { confirmLogout = true } }
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

// ---------- QR profile ----------

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


// ---------- Cloud password ----------

/** Settings → Privacy → Cloud password: set, change or remove it, recovery email with its code. */
@Composable
internal fun CloudPasswordScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val repo = settingsRepo()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<CloudPasswordState?>(null) }
    var busy by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { repo.passwordState() }.onSuccess { state = it }.onFailure { onNotice("Не удалось загрузить настройки пароля") } }
    fun run(block: suspend () -> CloudPasswordState, done: String) {
        busy = true
        scope.launch {
            runCatching { block() }
                .onSuccess { state = it; current = ""; password = ""; repeat = ""; code = ""; onNotice(done) }
                .onFailure { onNotice(com.kirox.app.telegram.passwordError(it)) }
            busy = false
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Облачный пароль", onBack) }
        val s = state
        if (s == null) { item { Loading() }; return@LazyColumn }
        item {
            Text(
                if (s.hasPassword) "Пароль включён. Его спросят при входе на новом устройстве." + (if (s.hasRecoveryEmail) " Почта для восстановления указана." else "")
                else "Пароль не задан. С ним войти в аккаунт по одному коду из SMS не получится.",
                color = Fusion.colors.textSecondary, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        s.pendingEmailPattern?.let { pattern ->
            item { SmallCaption("Подтверждение почты") }
            item { PasswordField("Код из письма на $pattern", code, secret = false) { code = it } }
            item { ActionButton("Подтвердить", enabled = !busy && code.isNotBlank()) { run({ repo.confirmRecoveryEmail(code.trim()) }, "Почта подтверждена") } }
        }
        item { SmallCaption(if (s.hasPassword) "Сменить пароль" else "Новый пароль") }
        if (s.hasPassword) item { PasswordField("Текущий пароль" + (if (s.hint.isNotBlank()) " (подсказка: ${s.hint})" else ""), current) { current = it } }
        item { PasswordField("Новый пароль", password) { password = it } }
        item { PasswordField("Повторите пароль", repeat) { repeat = it } }
        item { PasswordField("Подсказка (необязательно)", hint, secret = false) { hint = it } }
        if (!s.hasRecoveryEmail) item { PasswordField("Почта для восстановления (необязательно)", email, secret = false) { email = it } }
        item {
            val valid = password.isNotEmpty() && password == repeat && hint != password && (!s.hasPassword || current.isNotEmpty())
            ActionButton(if (s.hasPassword) "Сменить пароль" else "Включить пароль", enabled = !busy && valid) {
                run({ repo.setPassword(current, password, hint.trim(), email.trim().takeIf { it.isNotEmpty() && !s.hasRecoveryEmail }) }, "Пароль сохранён")
            }
            if (password.isNotEmpty() && repeat.isNotEmpty() && password != repeat) Text("Пароли не совпадают", color = Fusion.colors.danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }
        if (s.hasPassword) item {
            LinkRow("Отключить пароль", "Нужен текущий пароль", color = Fusion.colors.danger) { if (current.isEmpty()) onNotice("Введите текущий пароль") else confirmRemove = true }
        }
    }
    if (confirmRemove) AlertDialog(
        onDismissRequest = { confirmRemove = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Отключить облачный пароль?", color = Fusion.colors.textPrimary) },
        text = { Text("Войти в аккаунт можно будет только по коду.", color = Fusion.colors.textSecondary) },
        confirmButton = { TextButton(onClick = { confirmRemove = false; run({ repo.setPassword(current, "", "", null) }, "Пароль отключён") }) { Text("Отключить", color = Fusion.colors.danger) } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

@Composable
private fun PasswordField(label: String, value: String, secret: Boolean = true, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedTextColor = Fusion.colors.textPrimary, unfocusedTextColor = Fusion.colors.textPrimary,
            focusedBorderColor = Fusion.colors.textPrimary, unfocusedBorderColor = Fusion.colors.outlineStrong,
            focusedLabelColor = Fusion.colors.textPrimary, unfocusedLabelColor = Fusion.colors.textSecondary, cursorColor = Fusion.colors.textPrimary
        )
    )
}

@Composable
private fun ActionButton(title: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.padding(16.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp))
            .background(if (enabled) Fusion.colors.textPrimary else Fusion.colors.surface2).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(title, color = if (enabled) Fusion.colors.background else Fusion.colors.textTertiary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

// ---------- Blocked users ----------

@Composable
internal fun BlockedUsersScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val repo = settingsRepo()
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<BlockedSender>?>(null) }
    var confirm by remember { mutableStateOf<BlockedSender?>(null) }
    LaunchedEffect(Unit) { list = runCatching { repo.blocked() }.getOrElse { onNotice("Не удалось загрузить список"); emptyList() } }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Чёрный список", onBack) }
        val current = list
        when {
            current == null -> item { Loading() }
            current.isEmpty() -> item { Text("Никто не заблокирован", color = Fusion.colors.textSecondary, modifier = Modifier.padding(20.dp)) }
            else -> items(current, key = { "${it.isChat}${it.id}" }) { sender ->
                Row(Modifier.fillMaxWidth().clickable { confirm = sender }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.kirox.app.ui.components.TelegramAvatar(sender.name, sender.photo, 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(sender.name, color = Fusion.colors.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text("Разблокировать", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                }
            }
        }
    }
    confirm?.let { sender ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Разблокировать ${sender.name}?", color = Fusion.colors.textPrimary) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        runCatching { repo.unblock(sender) }
                            .onSuccess { list = list?.filterNot { it == sender }; onNotice("Разблокирован") }
                            .onFailure { onNotice("Не удалось разблокировать") }
                    }
                }) { Text("Разблокировать", color = Fusion.colors.textPrimary) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
}

// ---------- Notification exceptions ----------

@Composable
internal fun NotificationExceptionsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val repo = settingsRepo()
    var list by remember { mutableStateOf<List<NotificationException>?>(null) }
    LaunchedEffect(Unit) { list = runCatching { repo.notificationExceptions() }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Исключения", onBack) }
        val current = list
        when {
            current == null -> item { Loading() }
            current.isEmpty() -> item { Text("Все чаты следуют общим настройкам", color = Fusion.colors.textSecondary, modifier = Modifier.padding(20.dp)) }
            else -> items(current, key = { it.chatId }) { e -> LinkRow(e.title, if (e.muted) "Без звука" else "Уведомления включены") {} }
        }
        item {
            Text(
                "Изменить исключение можно в меню чата (⋮ → «Без звука»).",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

// ---------- Proxy ----------

/** Settings → Data → Proxy: list, switch, add by fields or by pasting a tg://proxy link, remove. */
@Composable
internal fun ProxyScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val repo = settingsRepo()
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<TelegramProxy>?>(null) }
    var adding by remember { mutableStateOf(false) }
    fun reload() { scope.launch { list = runCatching { repo.proxies() }.getOrDefault(emptyList()) } }
    fun act(block: suspend () -> Unit, failure: String) { scope.launch { runCatching { block() }.onFailure { onNotice(failure) }; reload() } }
    LaunchedEffect(Unit) { reload() }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Прокси", onBack) }
        val current = list
        if (current == null) { item { Loading() }; return@LazyColumn }
        item { ToggleRow("Использовать прокси", if (current.none { it.enabled }) "Сейчас прямое подключение" else null, checked = current.any { it.enabled }, enabled = current.isNotEmpty()) { on ->
            if (on) current.firstOrNull()?.let { p -> act({ repo.enableProxy(p.id) }, "Не удалось включить прокси") } else act({ repo.disableProxy() }, "Не удалось отключить прокси")
        } }
        if (current.isNotEmpty()) item { SmallCaption("Сохранённые") }
        items(current, key = { it.id }) { proxy ->
            Row(Modifier.fillMaxWidth().clickable { act({ repo.enableProxy(proxy.id) }, "Не удалось включить прокси") }.heightIn(min = 56.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (proxy.enabled) "●" else "○", color = Fusion.colors.textPrimary, fontSize = 16.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${proxy.server}:${proxy.port}", color = Fusion.colors.textPrimary, fontSize = 16.sp)
                    Text(proxy.kind.title, color = Fusion.colors.textSecondary, fontSize = 12.sp)
                }
                Text("Удалить", color = Fusion.colors.danger, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { act({ repo.removeProxy(proxy.id) }, "Не удалось удалить") }.padding(8.dp))
            }
        }
        item { LinkRow("Добавить прокси") { adding = true } }
    }
    if (adding) AddProxyDialog(onDismiss = { adding = false }) { server, port, kind, secret ->
        adding = false
        act({ repo.addProxy(server, port, kind, secret) }, "Не удалось добавить прокси")
    }
}

@Composable
private fun AddProxyDialog(onDismiss: () -> Unit, onAdd: (String, Int, ProxyKind, String) -> Unit) {
    var link by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(ProxyKind.MtProto) }
    var secret by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text("Новый прокси", color = Fusion.colors.textPrimary) },
        text = {
            Column {
                PasswordField("Ссылка tg://proxy (необязательно)", link, secret = false) { value ->
                    link = value
                    com.kirox.app.telegram.parseProxyLink(value)?.let { (s, p, k) -> server = s; port = p.toString(); kind = k.first; secret = k.second }
                }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProxyKind.entries.forEach { k ->
                        Text(
                            k.title, color = if (kind == k) Fusion.colors.background else Fusion.colors.textPrimary, fontSize = 13.sp,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (kind == k) Fusion.colors.textPrimary else Fusion.colors.surface3).clickable { kind = k }.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                PasswordField("Сервер", server, secret = false) { server = it.trim() }
                PasswordField("Порт", port, secret = false) { port = it.filter(Char::isDigit).take(5) }
                PasswordField(if (kind == ProxyKind.MtProto) "Секрет" else "Логин:пароль (необязательно)", secret) { secret = it.trim() }
            }
        },
        confirmButton = {
            val portNumber = port.toIntOrNull()
            TextButton(
                enabled = server.isNotBlank() && portNumber != null && portNumber in 1..65535 && (kind != ProxyKind.MtProto || secret.isNotBlank()),
                onClick = { onAdd(server, portNumber!!, kind, secret) }
            ) { Text("Добавить", color = Fusion.colors.textPrimary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}
