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

// Cloud password, blocked users and proxy.

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
