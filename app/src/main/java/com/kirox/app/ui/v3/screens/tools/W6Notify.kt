package com.kirox.app.ui.v3.screens.tools

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.kirox.app.telegram.NotifyScope
import com.kirox.app.telegram.ReactionSource
import com.kirox.app.telegram.ScopeNotifications
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.screens.settings.SSwitchRow
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

// Wave 6: everything the old «Уведомления и звуки» screen had beyond the v3 Notify board, drawn in v3
// (per-scope pinned / mentions / stories, Android channel sounds, reactions, calls, badge, events, delivery, reset),
// and the «Исключения» list whose rows open the per-chat ChatNotify board.

/** Android channel of a chat type: its sound and vibration are set there. */
private fun NotifyScope.channel(): String = when (this) {
    NotifyScope.Private -> com.kirox.app.notify.MessageNotifications.CHANNEL
    NotifyScope.Groups -> com.kirox.app.notify.MessageNotifications.CHANNEL_GROUPS
    NotifyScope.Channels -> com.kirox.app.notify.MessageNotifications.CHANNEL_CHANNELS
}

/** Store scope key («private» / «groups» / «channels», the board's «users» too) → Telegram scope. */
internal fun w6Scope(key: String): NotifyScope? = when (key) {
    "private", "users" -> NotifyScope.Private
    "groups" -> NotifyScope.Groups
    "channels" -> NotifyScope.Channels
    else -> null
}

internal fun w6OpenAppNotifications(context: android.content.Context) =
    w6Start(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))

internal fun w6OpenChannel(context: android.content.Context, channel: String) =
    w6Start(context, Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, channel))

/** Small «label · value» line of an opened Notify category (13sp, value accent and tappable). */
@Composable
private fun ExtraLine(label: String, value: String, on: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().kxClick(label, onClick = onClick)) {
        Text(label, style = KxType.style(13.sp, color = Kx.text2), modifier = Modifier.weight(1f))
        Text(value, style = KxType.style(13.sp, color = if (on) Kx.accent else Kx.text3))
    }
}

/**
 * Extra lines inside an opened category of the Notify board on the real account: pinned messages,
 * mentions (groups), new stories (private chats) and the Android channel with its sound and vibration.
 * Each change re-reads the scope from Telegram and writes it back (the board's own switches do the same).
 */
@Composable
internal fun W6ScopeExtras(ctx: KxScreenCtx, key: String) {
    val services = w6Services(ctx) ?: return
    val scope = w6Scope(key) ?: return
    val context = LocalContext.current
    val co = rememberCoroutineScope()
    var value by remember(key) { mutableStateOf<ScopeNotifications?>(null) }
    LaunchedEffect(key) { value = runCatching { services.settings.notifications(scope) }.getOrNull() }
    fun change(edit: (ScopeNotifications) -> ScopeNotifications) {
        val old = value ?: return
        value = edit(old)
        co.launch {
            runCatching { val fresh = services.settings.notifications(scope); services.settings.setNotifications(scope, edit(fresh)) }
                .onFailure { value = old; ctx.toast("Не удалось сохранить") }
        }
    }
    val v = value
    if (v != null) {
        ExtraLine("Закреплённые сообщения", if (v.pinned) "Уведомлять" else "Нет", v.pinned) { change { it.copy(pinned = !it.pinned) } }
        if (scope == NotifyScope.Groups) ExtraLine("Упоминания и ответы", if (v.mentions) "Даже без звука" else "Как у группы", v.mentions) { change { it.copy(mentions = !it.mentions) } }
        if (scope == NotifyScope.Private) ExtraLine("Новые истории", if (v.stories) "Уведомлять" else "Нет", v.stories) { change { it.copy(stories = !it.stories) } }
    }
    ExtraLine("Звук и вибрация", "Настройки Android ›") { w6OpenChannel(context, scope.channel()) }
}

/** Overlay state of [W6NotifyExtras]: the open picker sheet and the reset confirm. */
@Stable
internal class W6NotifyOverlay {
    var picking by mutableStateOf<String?>(null)
    var confirm by mutableStateOf<KxConfirm?>(null)
    /** Reaction notifications (Telegram sends them after sign-in; null until then) and how a pick is saved. */
    var reactions by mutableStateOf<com.kirox.app.telegram.ReactionNotifications?>(null)
    var saveReactions: (com.kirox.app.telegram.ReactionNotifications) -> Unit = {}
}

/**
 * Sections under «В приложении» on the real account: reactions, calls, badge, events, sounds per chat type,
 * reliable delivery and «Сбросить все уведомления» (the old screen's full set). Nothing in the demo.
 */
@Composable
internal fun W6NotifyExtras(ctx: KxScreenCtx, overlay: W6NotifyOverlay) {
    val services = w6Services(ctx) ?: return
    val context = LocalContext.current
    val co = rememberCoroutineScope()
    val repo = services.settings
    val prefs = services.notificationPrefs
    val local by prefs.settings.collectAsState()
    LaunchedEffect(Unit) { overlay.reactions = repo.reactionNotifications() }
    var joined by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { joined = runCatching { repo.contactJoinedNotifications() }.getOrNull() }
    var resumes by remember { mutableIntStateOf(0) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { resumes++; onPauseOrDispose { } }
    fun saveReactions(value: com.kirox.app.telegram.ReactionNotifications) {
        val old = overlay.reactions; overlay.reactions = value
        co.launch { runCatching { repo.setReactionNotifications(value) }.onFailure { overlay.reactions = old; ctx.toast("Не удалось сохранить") } }
    }
    overlay.saveReactions = ::saveReactions

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TCap("Реакции", top = 8.dp)
        val r = overlay.reactions
        if (r == null) THint("Настройки реакций появятся, когда kiroX получит их от Telegram.", top = 0.dp)
        else {
            W6ValueRow("На сообщения", r.messages.title, groupShape(0, 3, 20.dp), icon = Icons.Outlined.AddReaction) { overlay.picking = "reactMsg" }
            W6ValueRow("На истории", r.stories.title, groupShape(1, 3, 20.dp), icon = Icons.Outlined.AutoStories) { overlay.picking = "reactStory" }
            SSwitchRow(Icons.Outlined.EmojiEmotions, "Показывать реакцию", r.showPreview, groupShape(2, 3, 20.dp), sub = "Какой эмодзи поставили — в тексте уведомления", small = true) {
                saveReactions(r.copy(showPreview = !r.showPreview))
            }
        }

        TCap("Звонки")
        val fullScreen = remember(resumes) { android.os.Build.VERSION.SDK_INT < 34 || context.getSystemService(android.app.NotificationManager::class.java)?.canUseFullScreenIntent() == true }
        val n = if (android.os.Build.VERSION.SDK_INT >= 34) 2 else 1
        W6ValueRow("Мелодия и вибрация", "Android ›", groupShape(0, n, 20.dp), icon = Icons.Outlined.RingVolume) { w6OpenChannel(context, com.kirox.app.voip.CallNotifications.CHANNEL_INCOMING) }
        if (android.os.Build.VERSION.SDK_INT >= 34) W6ValueRow("Звонки на весь экран", if (fullScreen) "Разрешено" else "Выключено", groupShape(1, n, 20.dp),
            sub = if (fullScreen) "Входящий открывается поверх блокировки" else "Входящий звонок будет только уведомлением", icon = Icons.Outlined.Fullscreen,
            valueColor = if (fullScreen) Kx.accent else Kx.danger) {
            w6Start(context, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri()))
        }

        TCap("Значок и события")
        SSwitchRow(Icons.Outlined.Numbers, "Считать сообщения", local.badgeCountsMessages && local.badge, groupShape(0, 2, 20.dp),
            sub = if (!local.badge) "Включите «Счётчик на иконке» выше" else if (local.badgeCountsMessages) "Каждое новое сообщение" else "Один на каждый чат", small = true) {
            if (local.badge) prefs.update(local.copy(badgeCountsMessages = !local.badgeCountsMessages))
        }
        val j = joined
        SSwitchRow(Icons.Outlined.PersonAdd, "Контакт в Telegram", j ?: true, groupShape(1, 2, 20.dp), sub = "Когда кто-то из контактов заводит Telegram", small = true) {
            if (j != null) { joined = !j; co.launch { runCatching { repo.setContactJoinedNotifications(!j) }.onFailure { joined = j; ctx.toast("Не удалось сохранить") } } }
        }

        TCap("Звук по типам чатов")
        NotifyScope.entries.forEachIndexed { i, s ->
            W6ValueRow(s.title, "Android ›", groupShape(i, NotifyScope.entries.size + 1, 20.dp), sub = "Мелодия, вибрация, всплывающие", icon = Icons.Outlined.MusicNote) { w6OpenChannel(context, s.channel()) }
        }
        W6ValueRow("Все уведомления kiroX", "Android ›", groupShape(NotifyScope.entries.size, NotifyScope.entries.size + 1, 20.dp), icon = Icons.Outlined.Settings) { w6OpenAppNotifications(context) }

        TCap("Надёжная доставка")
        val power = remember { context.getSystemService(android.os.PowerManager::class.java) }
        val unrestricted = remember(resumes) { power?.isIgnoringBatteryOptimizations(context.packageName) == true }
        W6ValueRow("Без ограничений батареи", if (unrestricted) "Включено" else "Выключено", groupShape(0, 2, 20.dp),
            sub = if (unrestricted) "Система не усыпляет kiroX" else "Чтобы звонки и сообщения не задерживались", icon = Icons.Outlined.BatteryChargingFull,
            valueColor = if (unrestricted) Kx.accent else Kx.warning) {
            if (!unrestricted) {
                @android.annotation.SuppressLint("BatteryLife")
                val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
                w6Start(context, request)
            }
        }
        W6ValueRow("Работа в фоне", "Android ›", groupShape(1, 2, 20.dp), sub = "Чтобы уведомления приходили, когда kiroX свёрнут", icon = Icons.Outlined.Layers) {
            w6Start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(20.dp)).background(Kx.dangerSolid.copy(alpha = .1f)).kxClick {
            overlay.confirm = KxConfirm("Сбросить все уведомления?", "Для всех чатов вернутся настройки по умолчанию, отдельные настройки чатов удалятся.", "Сбросить") {
                prefs.reset()
                co.launch {
                    runCatching { repo.resetAllNotifications() }
                        .onSuccess {
                            // The board's scope switches show what Telegram has now (the defaults).
                            ctx.store.notifyScopes.forEach { s -> ctx.store.setNotifyScope(s.key, true, true) }
                            overlay.reactions = repo.reactionNotifications(); ctx.toast("Уведомления сброшены")
                        }
                        .onFailure { ctx.toast("Не удалось сбросить") }
                }
            }
        }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.RestartAlt, null, Modifier.size(20.dp), tint = Kx.danger)
            Text("Сбросить все уведомления", style = KxType.style(15.sp, FontWeight.SemiBold, Kx.danger))
        }
        THint("Настройки Telegram по умолчанию; исключения отдельных чатов удалятся.")
    }
}

/** Sheets and the confirm of [W6NotifyExtras], drawn over the whole board. */
@Composable
internal fun BoxScope.W6NotifyOverlayHost(overlay: W6NotifyOverlay) {
    val r = overlay.reactions
    when (val p = overlay.picking) {
        "reactMsg", "reactStory" -> if (r != null) W6ChoiceSheet(if (p == "reactMsg") "Реакции на сообщения" else "Реакции на истории", ReactionSource.entries.map { it.title },
            (if (p == "reactMsg") r.messages else r.stories).ordinal, hint = "Чьи реакции присылают уведомление", onClose = { overlay.picking = null }) { i ->
            val pick = ReactionSource.entries[i]
            overlay.saveReactions(if (p == "reactMsg") r.copy(messages = pick) else r.copy(stories = pick))
        }
    }
    overlay.confirm?.let { c -> KxConfirmDialog(c) { overlay.confirm = null } }
}

private class W6Exc(val chatId: Long, val title: String, val muted: Boolean)

private val W6DemoExc = listOf(W6Exc(1, "Kotlin-клуб", true), W6Exc(2, "Мама", false), W6Exc(3, "Новости IT", true))

/** «Исключения»: chats with their own notification settings; a row opens that chat's ChatNotify board. */
@Composable
fun NotifyExceptionsScreen(ctx: KxScreenCtx) {
    val services = w6Services(ctx)
    var list by remember { mutableStateOf(if (services == null) W6DemoExc else null) }
    var failed by remember { mutableStateOf(false) }
    if (services != null) LaunchedEffect(Unit) {
        runCatching { services.settings.notificationExceptions() }
            .onSuccess { l -> list = l.map { W6Exc(it.chatId, it.title, it.muted) } }
            .onFailure { failed = true; list = emptyList() }
    }
    TScreen("Исключения", ctx.back, sub = list?.let { "${it.size} ${tPlural(it.size, "чат", "чата", "чатов")} со своими настройками" } ?: "Загрузка…") {
        val l = list
        when {
            l == null -> THint("Загружаем исключения…", top = 16.dp)
            l.isEmpty() -> Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.NotificationsNone, null, Modifier.size(44.dp), tint = Kx.text4)
                Text(if (failed) "Не удалось загрузить исключения" else "Исключений нет", style = KxType.style(15.sp, FontWeight.Bold, Kx.text2), modifier = Modifier.padding(top = 8.dp))
                Text(if (failed) "Проверьте соединение и откройте экран снова" else "Все чаты следуют общим настройкам", style = KxType.style(13.sp, color = Kx.text3),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                l.forEachIndexed { i, e ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(groupShape(i, l.size, 20.dp)).background(TTile).kxClick(e.title) { ctx.open("ChatNotify:${e.chatId}") }
                        .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KxAvatar(e.title, e.chatId, size = 44.dp)
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = KxType.style(15.sp, FontWeight.SemiBold), maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (e.muted) Icon(Icons.Rounded.NotificationsOff, null, Modifier.size(14.dp), tint = Kx.text4)
                                Text(if (e.muted) "Без звука" else "Уведомления включены", style = KxType.style(12.sp, color = if (e.muted) Kx.text4 else Kx.accent))
                            }
                        }
                        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = Kx.text4)
                    }
                }
                THint("Нажмите на чат, чтобы изменить или сбросить его настройки.")
            }
        }
    }
}
