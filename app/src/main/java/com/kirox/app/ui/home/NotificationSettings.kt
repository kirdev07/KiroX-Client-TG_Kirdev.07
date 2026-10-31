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

// Settings → Notifications and sounds, with exceptions.

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

internal fun NotifyScope.icon(): Pair<com.kirox.app.ui.icons.KiroXIcon, Color> = when (this) {
    NotifyScope.Private -> com.kirox.app.ui.icons.KiroXIcon.Profile to Color(0xFF4F8DF7)
    NotifyScope.Groups -> com.kirox.app.ui.icons.KiroXIcon.Contacts to Color(0xFF34C38F)
    NotifyScope.Channels -> com.kirox.app.ui.icons.KiroXIcon.Channel to Color(0xFFFFA94D)
}

internal fun ScopeNotifications.summary(): String = buildString {
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
