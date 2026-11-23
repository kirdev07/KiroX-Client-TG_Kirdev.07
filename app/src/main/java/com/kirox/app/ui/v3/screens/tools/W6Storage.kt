package com.kirox.app.ui.v3.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.telegram.CacheLimit
import com.kirox.app.telegram.ChatStorage
import com.kirox.app.telegram.DownloadNetwork
import com.kirox.app.telegram.NetworkDownload
import com.kirox.app.telegram.StorageCategory
import com.kirox.app.telegram.TelegramChatKind
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.screens.settings.SPills
import com.kirox.app.ui.v3.screens.settings.SSegment
import com.kirox.app.ui.v3.screens.settings.SSwitchRow
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

// Wave 6: the old «Данные и память» extras (auto-download per network with size limits, cache limit, GIF autoplay,
// save to gallery, reset, the chats that take the most) drawn in v3 inside the Storage board on the real account.

/** Size limits offered for videos and files (as the old screen and Telegram); 0 is «не загружать». */
private val W6Limits = listOf(0L, 1L shl 20, 5L shl 20, 10L shl 20, 25L shl 20, 50L shl 20, 100L shl 20, 500L shl 20, 1L shl 30, 2L shl 30)

private fun w6Limit(bytes: Long): String = when {
    bytes <= 0 -> "Только вручную"
    bytes % (1L shl 30) == 0L -> "До ${bytes shr 30} ГБ"
    bytes % (1L shl 20) == 0L -> "До ${bytes shr 20} МБ"
    else -> "До ${w6Bytes(bytes)}"
}

/** Overlay state of [W6StorageReal] (picker sheet, confirm). */
@Stable
internal class W6StorageOverlay {
    /** «video:<network>» / «file:<network>». */
    var picking by mutableStateOf<String?>(null)
    var confirm by mutableStateOf<KxConfirm?>(null)
}

/** The real account's auto-download and cache rules (replaces the old «Автозагрузка и кэш» page). */
@Composable
internal fun W6StorageReal(ctx: KxScreenCtx, overlay: W6StorageOverlay) {
    val services = w6Services(ctx) ?: return
    val co = rememberCoroutineScope()
    val auto = services.autoDownload
    val all by auto.settings.collectAsState()
    val gifs by auto.autoplayGifs.collectAsState()
    val limit by services.storage.cacheLimit.collectAsState()
    val gallery by services.media.saveToGallery.collectAsState()
    var net by remember { mutableIntStateOf(1) }
    val networks = listOf(DownloadNetwork.WiFi, DownloadNetwork.Mobile, DownloadNetwork.Roaming)
    val network = networks[net]
    val v = all[network] ?: NetworkDownload(false, 0, 0, false)
    var chats by remember { mutableStateOf<List<ChatStorage>?>(null) }
    var reloadChats by remember { mutableIntStateOf(0) }
    LaunchedEffect(reloadChats) { chats = runCatching { services.settings.storageBreakdown(10).chats }.getOrNull() }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        com.kirox.app.ui.v3.screens.settings.GroupLabel("Автозагрузка медиа")
        SSegment(listOf("Wi-Fi", "Мобильная", "Роуминг"), net, listOf(Icons.Outlined.Wifi, Icons.Outlined.SignalCellularAlt, Icons.Outlined.Luggage)) { net = it }
        Spacer(Modifier.height(8.dp))
        SSwitchRow(Icons.Outlined.Download, "Загружать сами", v.enabled, groupShape(0, 5, 20.dp), sub = if (v.enabled) "Медиа загружаются, пока вы листаете чат" else "Только по нажатию", small = true) {
            auto.set(network, v.copy(enabled = !v.enabled))
        }
        SSwitchRow(Icons.Outlined.Image, "Фото", v.photos && v.enabled, groupShape(1, 5, 20.dp), small = true) { if (v.enabled) auto.set(network, v.copy(photos = !v.photos)) }
        W6ValueRow("Видео", w6Limit(v.maxVideoBytes), groupShape(2, 5, 20.dp), enabled = v.enabled, icon = Icons.Outlined.Movie) { overlay.picking = "video:${network.name}" }
        W6ValueRow("Файлы", w6Limit(v.maxFileBytes), groupShape(3, 5, 20.dp), enabled = v.enabled, icon = Icons.Outlined.Description) { overlay.picking = "file:${network.name}" }
        SSwitchRow(Icons.Outlined.AutoStories, "Истории", v.preloadStories && v.enabled, groupShape(4, 5, 20.dp), sub = "Заранее загружать истории контактов", small = true) {
            if (v.enabled) auto.set(network, v.copy(preloadStories = !v.preloadStories))
        }
        THint("Файлы больше выбранного размера загружаются только по нажатию. Настройки хранятся на этом телефоне.")

        Spacer(Modifier.height(10.dp))
        com.kirox.app.ui.v3.screens.settings.GroupLabel("Кэш")
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TTile).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Максимальный размер", style = KxType.style(14.sp), modifier = Modifier.weight(1f))
                Text(limit.title, style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
            }
            Spacer(Modifier.height(10.dp))
            SPills(listOf("5 ГБ", "16 ГБ", "32 ГБ", "∞"), CacheLimit.entries.indexOf(limit)) { services.storage.setCacheLimit(CacheLimit.entries[it]) }
        }
        THint("Когда кэш больше, первыми удаляются файлы, которые вы дольше всего не открывали.")
        Spacer(Modifier.height(8.dp))
        SSwitchRow(Icons.Outlined.Gif, "Автовоспроизведение GIF", gifs, groupShape(0, 1, 20.dp), sub = "Загружать и проигрывать GIF в чатах сами", small = true) { auto.setAutoplayGifs(!gifs) }

        Spacer(Modifier.height(10.dp))
        com.kirox.app.ui.v3.screens.settings.GroupLabel("Сохранять в галерею")
        val kinds = listOf(Triple(TelegramChatKind.Private, "Личные чаты", Icons.Outlined.Person), Triple(TelegramChatKind.Group, "Группы", Icons.Outlined.Groups),
            Triple(TelegramChatKind.Channel, "Каналы", Icons.Outlined.Campaign))
        kinds.forEachIndexed { i, (kind, title, icon) ->
            SSwitchRow(icon, title, kind in gallery, groupShape(i, kinds.size, 20.dp), small = true) { services.media.setSaveToGallery(kind, kind !in gallery) }
        }
        THint("Полученные фото и видео копируются в «Pictures/kiroX» и «Movies/kiroX» после загрузки.")

        val list = chats.orEmpty().filter { it.bytes > 0 }
        if (list.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            com.kirox.app.ui.v3.screens.settings.GroupLabel("Больше всего занимают")
            list.forEachIndexed { i, c ->
                Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).clip(groupShape(i, list.size, 20.dp)).background(TTile).kxClick(c.title) {
                    overlay.confirm = KxConfirm("Очистить кэш «${c.title}»?", "Будет освобождено около ${w6Bytes(c.bytes)}. Сообщения останутся, файлы загрузятся снова, когда вы их откроете.", "Очистить") {
                        co.launch {
                            runCatching { services.settings.clearStorage(StorageCategory.entries.toSet(), c.chatId) }
                                .onSuccess { ctx.toast("Освобождено ${w6Bytes(c.bytes)}") }.onFailure { ctx.toast("Не удалось очистить кэш") }
                            reloadChats++
                        }
                    }
                }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KxAvatar(c.title, c.chatId, size = 40.dp)
                    Text(c.title, style = KxType.style(15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(w6Bytes(c.bytes), style = KxType.style(13.sp, FontWeight.Bold, Kx.text3))
                    Icon(Icons.Rounded.CleaningServices, null, Modifier.size(18.dp), tint = Kx.text4)
                }
            }
            THint("Нажмите на чат, чтобы удалить с телефона только его файлы.")
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(20.dp)).background(Kx.dangerSolid.copy(alpha = .1f)).kxClick {
            overlay.confirm = KxConfirm("Сбросить автозагрузку?", "Для всех сетей вернутся настройки Telegram по умолчанию, автовоспроизведение GIF включится.", "Сбросить") {
                auto.reset(); ctx.toast("Настройки автозагрузки сброшены")
            }
        }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.RestartAlt, null, Modifier.size(20.dp), tint = Kx.danger)
            Text("Сбросить настройки автозагрузки", style = KxType.style(15.sp, FontWeight.SemiBold, Kx.danger))
        }
    }
}

/** Sheets and the confirm of [W6StorageReal], over the whole board. */
@Composable
internal fun BoxScope.W6StorageOverlayHost(ctx: KxScreenCtx, overlay: W6StorageOverlay) {
    val services = w6Services(ctx)
    val p = overlay.picking
    if (services != null && p != null) {
        val all by services.autoDownload.settings.collectAsState()
        val network = DownloadNetwork.entries.firstOrNull { it.name == p.substringAfter(':') }
        val v = network?.let { all[it] }
        if (network != null && v != null) {
            val video = p.startsWith("video")
            val current = if (video) v.maxVideoBytes else v.maxFileBytes
            val opts = (W6Limits + current).distinct().sorted()
            W6ChoiceSheet(if (video) "Видео" else "Файлы", opts.map(::w6Limit), opts.indexOf(current), hint = network.title, onClose = { overlay.picking = null }) { i ->
                services.autoDownload.set(network, if (video) v.copy(maxVideoBytes = opts[i]) else v.copy(maxFileBytes = opts[i]))
            }
        }
    }
    overlay.confirm?.let { c -> KxConfirmDialog(c) { overlay.confirm = null } }
}
