@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kirox.app.ui.home

import com.kirox.app.ui.components.fusionPanel
import androidx.compose.material3.MaterialTheme
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import com.kirox.app.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.BuildConfig
import com.kirox.app.telegram.TelegramContact
import com.kirox.app.telegram.TelegramUserProfile
import com.kirox.app.ui.components.SeasonalLayer
import com.kirox.app.ui.components.TelegramAvatar
import com.kirox.app.ui.format.TelegramFormat
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.icons.KiroXIcon as IconGlyph
import com.kirox.app.ui.theme.Fusion
import com.kirox.app.ui.theme.FusionPalette

// Settings → Updates and Settings → Devices.

/**
 * Settings → «О приложении»: who kiroX is, the update card (check, download, install from GitHub
 * releases, beta channel, what's new) and every project link — the Telegram channel, releases,
 * source code, bug reports.
 */
@Composable
internal fun UpdatesScreen(updates: com.kirox.app.update.UpdateCenter, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    val linkOpener = com.kirox.app.ui.chat.LocalLinkOpener.current
    val state by updates.state.collectAsStateWithLifecycle()
    var beta by remember { mutableStateOf(updates.betaChannel) }
    // t.me links open inside kiroX, the rest in the browser.
    fun open(url: String) {
        if (linkOpener != null && url.contains("t.me/")) linkOpener(url)
        else runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri()).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    val release = when (val s = state) {
        is com.kirox.app.update.UpdateState.Available -> s.release
        is com.kirox.app.update.UpdateState.Downloading -> s.release
        is com.kirox.app.update.UpdateState.Ready -> s.release
        is com.kirox.app.update.UpdateState.Failed -> s.release
        else -> null
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { BackTitle("О приложении", onBack) }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.kirox.app.R.drawable.kirox_logo), "kiroX", Modifier.size(88.dp))
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Text("kiroX", color = Fusion.colors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Неофициальный клиент Telegram", color = Fusion.colors.textSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                val channel = if (BuildConfig.VERSION_NAME.contains('-')) "бета" else "стабильная"
                Text(
                    "Версия ${BuildConfig.VERSION_NAME} · $channel",
                    color = Fusion.colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Fusion.colors.accent.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        item { SmallCaption("Обновления") }
        item { UpdateCard(updates, state) }
        if (release != null) item {
            SmallCaption("Что нового в ${release.version}")
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    if (release.notes.isNotBlank()) Text(releaseNotesText(release.notes, Fusion.colors.textPrimary), color = Fusion.colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                    else Text("Описание к этой версии не добавлено.", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        listOfNotNull(
                            release.apkSize.takeIf { it > 0 }?.let { "Размер: ${formatBytes(it)}" },
                            if (release.prerelease) "бета-версия" else null,
                            if (release.sha256 != null) "контрольная сумма проверяется" else null
                        ).joinToString(" · "),
                        color = Fusion.colors.textTertiary, fontSize = 12.sp
                    )
                    TextButton(onClick = { open(release.pageUrl) }, contentPadding = PaddingValues(0.dp)) { Text("Страница версии на GitHub", color = Fusion.colors.accent) }
                }
            }
        }
        item {
            SettingsCard {
                Row(Modifier.fillMaxWidth().clickable { beta = !beta; updates.betaChannel = beta }.heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Бета-версии", color = Fusion.colors.textPrimary, fontSize = 16.sp)
                        Text("Новые функции раньше, но возможны ошибки", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                    }
                    Switch(beta, { beta = it; updates.betaChannel = it })
                }
            }
        }
        item {
            Text(
                "kiroX сам проверяет обновления раз в день. Файл скачивается с GitHub и устанавливается, только если его контрольная сумма и подпись совпали.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        item { SmallCaption("Проект") }
        item {
            SettingsCard {
                IconRow(KiroXIcon.Channel, androidx.compose.ui.graphics.Color(0xFF2AABEE), "Telegram-канал kiroX", "Новости и версии клиента — t.me/kiroxclient") { open(com.kirox.app.update.UpdateCenter.CHANNEL_URL) }
                CardDivider()
                IconRow(KiroXIcon.Profile, androidx.compose.ui.graphics.Color(0xFF6C4CE0), "Канал разработчика", "t.me/kirdev_07") { open(com.kirox.app.update.UpdateCenter.DEVELOPER_URL) }
                CardDivider()
                IconRow(KiroXIcon.Refresh, androidx.compose.ui.graphics.Color(0xFF9B6BFF), "Все версии", "Список выпусков на GitHub") { open(com.kirox.app.update.UpdateCenter.RELEASES_URL) }
                CardDivider()
                IconRow(KiroXIcon.Link, androidx.compose.ui.graphics.Color(0xFF34C38F), "Исходный код и лицензии", "github.com/${com.kirox.app.update.UpdateCenter.REPO}") { open(com.kirox.app.update.UpdateCenter.REPO_URL) }
                CardDivider()
                IconRow(KiroXIcon.Info, androidx.compose.ui.graphics.Color(0xFFFFA94D), "Сообщить об ошибке", "Описать проблему на GitHub") { open(com.kirox.app.update.UpdateCenter.ISSUES_URL) }
            }
        }
        item { SmallCaption("Сведения") }
        item {
            SettingsCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    InfoPair("Версия", BuildConfig.VERSION_NAME)
                    InfoPair("Сборка", "${BuildConfig.VERSION_CODE} · ${if (BuildConfig.DEBUG) "отладочная" else "релиз"}")
                    InfoPair("Основа", "TDLib — официальная библиотека Telegram")
                    InfoPair("Android", "${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                }
            }
        }
        item {
            Text(
                "kiroX — неофициальное приложение, оно не связано с Telegram и не одобрено им. Название Telegram принадлежит его правообладателям.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp)
            )
        }
    }
}

/** Status of updates with the one action that fits it: check, download, install or retry. */
@Composable
private fun UpdateCard(updates: com.kirox.app.update.UpdateCenter, s: com.kirox.app.update.UpdateState) {
    val (title, subtitle) = when (s) {
        com.kirox.app.update.UpdateState.Idle -> "Проверить обновления" to "Нажмите кнопку, чтобы узнать о новой версии"
        com.kirox.app.update.UpdateState.Checking -> "Проверяю…" to "Смотрю выпуски на GitHub"
        com.kirox.app.update.UpdateState.UpToDate -> "У вас последняя версия" to "kiroX ${updates.currentVersion}"
        is com.kirox.app.update.UpdateState.Available -> "Доступна версия ${s.release.version}" to "Можно скачать и установить"
        is com.kirox.app.update.UpdateState.Downloading -> "Скачиваю ${s.release.version}" to "${(s.progress * 100).toInt()}%"
        is com.kirox.app.update.UpdateState.Ready -> "Версия ${s.release.version} готова" to "Скачана и проверена"
        is com.kirox.app.update.UpdateState.Failed -> "Не получилось" to s.message
    }
    val (icon, tint) = when (s) {
        com.kirox.app.update.UpdateState.UpToDate -> KiroXIcon.Check to androidx.compose.ui.graphics.Color(0xFF34C38F)
        is com.kirox.app.update.UpdateState.Available, is com.kirox.app.update.UpdateState.Ready -> KiroXIcon.Star to Fusion.colors.accent
        is com.kirox.app.update.UpdateState.Failed -> KiroXIcon.Info to Fusion.colors.danger
        else -> KiroXIcon.Refresh to androidx.compose.ui.graphics.Color(0xFF4F8DF7)
    }
    SettingsCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tint), contentAlignment = Alignment.Center) {
                    IconGlyph(icon, null, Modifier.size(24.dp), androidx.compose.ui.graphics.Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Fusion.colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = if (s is com.kirox.app.update.UpdateState.Failed) Fusion.colors.danger else Fusion.colors.textSecondary, fontSize = 13.sp)
                }
            }
            if (s is com.kirox.app.update.UpdateState.Downloading) {
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth(), color = Fusion.colors.accent, trackColor = Fusion.colors.surface2)
            }
            val (label, action) = when (s) {
                is com.kirox.app.update.UpdateState.Available -> "Скачать ${s.release.version}" to { updates.download(s.release) }
                is com.kirox.app.update.UpdateState.Ready -> "Установить" to { updates.install(s.file) }
                is com.kirox.app.update.UpdateState.Failed -> (if (s.release != null) "Повторить" else "Проверить снова") to { s.release?.let(updates::download) ?: updates.check() }
                is com.kirox.app.update.UpdateState.Checking, is com.kirox.app.update.UpdateState.Downloading -> "Подождите…" to {}
                else -> "Проверить обновления" to { updates.check() }
            }
            Spacer(Modifier.height(14.dp))
            com.kirox.app.ui.components.FusionButton(
                label, onClick = action,
                enabled = s !is com.kirox.app.update.UpdateState.Checking && s !is com.kirox.app.update.UpdateState.Downloading
            )
            val last = updates.lastCheck
            if (last > 0) Text(
                "Последняя проверка: ${checkTime(last)}",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

/** «30 окт., 23:55» for the last update check. */
private fun checkTime(millis: Long): String = java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))

@Composable
private fun InfoPair(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Fusion.colors.textSecondary, fontSize = 14.sp, modifier = Modifier.width(96.dp))
        androidx.compose.material3.Text(value, color = Fusion.colors.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

/** Settings → Devices: active sessions of the account; others can be terminated. */
@Composable
internal fun DevicesScreen(sessions: com.kirox.app.telegram.TelegramSessionsRepository, onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<com.kirox.app.telegram.TelegramSession>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<com.kirox.app.telegram.TelegramSession?>(null) }
    var confirmAll by remember { mutableStateOf(false) }
    fun reload() {
        scope.launch { runCatching { sessions.sessions() }.onSuccess { list = it; failed = false }.onFailure { failed = true } }
    }
    LaunchedEffect(Unit) { reload() }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Устройства", onBack) }
        val current = list?.firstOrNull { it.isCurrent }
        val others = list?.filterNot { it.isCurrent }.orEmpty()
        when {
            failed -> item { Text("Не удалось загрузить список устройств", color = Fusion.colors.textSecondary, modifier = Modifier.padding(20.dp)) }
            list == null -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = Fusion.colors.textPrimary) }
            }
            else -> {
                if (current != null) {
                    item { SmallCaption("Это устройство") }
                    item { SessionRow(current, onTerminate = null) }
                }
                if (others.isNotEmpty()) {
                    item {
                        Text(
                            "Завершить все другие сеансы", color = Fusion.colors.danger, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth().clickable { confirmAll = true }.padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                    }
                    item { SmallCaption("Активные сеансы") }
                    items(others, key = { it.id }) { session -> SessionRow(session, onTerminate = { confirm = session }) }
                } else {
                    item { Text("Других активных сеансов нет", color = Fusion.colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                }
            }
        }
    }
    confirm?.let { session ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Завершить сеанс?", color = Fusion.colors.textPrimary) },
            text = { Text("«${session.device.ifBlank { session.application }}» выйдет из аккаунта.", color = Fusion.colors.textSecondary) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirm = null
                    scope.launch { runCatching { sessions.terminate(session.id) }.onSuccess { reload() }.onFailure { onNotice("Не удалось завершить сеанс") } }
                }) { Text("Завершить", color = Fusion.colors.danger) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirm = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    if (confirmAll) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmAll = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Завершить все другие сеансы?", color = Fusion.colors.textPrimary) },
        text = { Text("Все устройства, кроме этого, выйдут из аккаунта.", color = Fusion.colors.textSecondary) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                confirmAll = false
                scope.launch { runCatching { sessions.terminateOthers() }.onSuccess { reload() }.onFailure { onNotice("Не удалось завершить сеансы") } }
            }) { Text("Завершить", color = Fusion.colors.danger) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmAll = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

@Composable
private fun SessionRow(session: com.kirox.app.telegram.TelegramSession, onTerminate: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Fusion.colors.surface2), contentAlignment = Alignment.Center) {
            IconGlyph(KiroXIcon.Devices, null, Modifier.size(22.dp), Fusion.colors.textPrimary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(session.device.ifBlank { "Устройство" }, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(session.application, color = Fusion.colors.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(
                session.location.takeIf { it.isNotBlank() },
                if (session.isCurrent) "в сети" else com.kirox.app.ui.format.TelegramFormat.chatTime(session.lastActive)
            ).joinToString(" · ")
            Text(details, color = Fusion.colors.textTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onTerminate != null) Text(
            "Завершить", color = Fusion.colors.danger, fontSize = 14.sp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onTerminate).padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

/** The small Markdown subset used in release notes: headings, "- " bullets and **bold**. The top "## KiroX …" title is dropped. */
private fun releaseNotesText(markdown: String, strong: androidx.compose.ui.graphics.Color): androidx.compose.ui.text.AnnotatedString =
    androidx.compose.ui.text.buildAnnotatedString {
        var lines = markdown.replace("\r", "").lines().dropWhile { it.isBlank() }
        if (lines.firstOrNull()?.startsWith("## ") == true) lines = lines.drop(1).dropWhile { it.isBlank() }
        lines.forEachIndexed { index, raw ->
            if (index > 0) append('\n')
            if (raw.startsWith("#")) {
                withStyle(androidx.compose.ui.text.SpanStyle(color = strong, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)) { append(raw.trimStart('#').trim()) }
                return@forEachIndexed
            }
            val line = if (raw.startsWith("- ")) "•  " + raw.drop(2) else raw
            line.split("**").forEachIndexed { i, part ->
                if (i % 2 == 1) withStyle(androidx.compose.ui.text.SpanStyle(color = strong, fontWeight = FontWeight.SemiBold)) { append(part) } else append(part)
            }
        }
    }
