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

// Settings → Data and storage: usage, auto-download, network usage.

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
internal fun StorageCategory.color(): Color = when (this) {
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

internal fun NetworkDownload.summary(): String = when {
    !enabled -> "Выключено"
    else -> (if (photos) "Фото · " else "Без фото · ") + "видео: ${downloadLimit(maxVideoBytes)} · файлы: ${downloadLimit(maxFileBytes)}"
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
        item { ToggleRow("Автозагрузка медиа", "Выключите — фото, видео и файлы будут загружаться только по нажатию", value.enabled) { autoDownload.set(network, value.copy(enabled = it)) } }
        item { SmallCaption("Типы медиа") }
        item { ToggleRow("Фото", "Загружать фотографии в чатах сразу", value.photos, enabled = value.enabled) { autoDownload.set(network, value.copy(photos = it)) } }
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
