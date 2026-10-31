package com.kirox.app.telegram

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.drinkless.tdlib.TdApi

/** Largest files that download by themselves on the current network; 0 means never. */
data class AutoDownloadRule(val maxVideoBytes: Long = 0, val maxOtherBytes: Long = 0, val preloadStories: Boolean = false, val gifs: Boolean = true, val photos: Boolean = true)

/** Network a set of auto-download limits is for, as in Telegram's «Автозагрузка медиа». */
enum class DownloadNetwork(val title: String) { Mobile("Через мобильную сеть"), WiFi("Через Wi-Fi"), Roaming("В роуминге") }

/** Auto-download limits for one network; a limit of 0 means that kind never downloads by itself. */
data class NetworkDownload(val enabled: Boolean, val maxVideoBytes: Long, val maxFileBytes: Long, val preloadStories: Boolean, val photos: Boolean = true)

internal fun DeviceNetwork.downloadNetwork(): DownloadNetwork? = when (this) {
    DeviceNetwork.WiFi, DeviceNetwork.Other -> DownloadNetwork.WiFi
    DeviceNetwork.Mobile -> DownloadNetwork.Mobile
    DeviceNetwork.MobileRoaming -> DownloadNetwork.Roaming
    DeviceNetwork.None -> null
}

private fun TdApi.AutoDownloadSettingsPresets.forNetwork(network: DownloadNetwork) = when (network) {
    DownloadNetwork.WiFi -> high
    DownloadNetwork.Mobile -> medium
    DownloadNetwork.Roaming -> low
}

private fun TdApi.AutoDownloadSettings.toNetworkDownload() = NetworkDownload(isAutoDownloadEnabled, maxVideoFileSize, maxOtherFileSize, preloadStories)

/** Picks Telegram's preset for the network: high on Wi-Fi, medium on mobile, low in roaming, none offline. */
internal fun autoDownloadRuleFor(network: DeviceNetwork, presets: TdApi.AutoDownloadSettingsPresets?): AutoDownloadRule {
    val settings = when (network) {
        DeviceNetwork.WiFi, DeviceNetwork.Other -> presets?.high
        DeviceNetwork.Mobile -> presets?.medium
        DeviceNetwork.MobileRoaming -> presets?.low
        DeviceNetwork.None -> null
    } ?: return AutoDownloadRule()
    if (!settings.isAutoDownloadEnabled) return AutoDownloadRule()
    return AutoDownloadRule(settings.maxVideoFileSize, settings.maxOtherFileSize, settings.preloadStories)
}

/**
 * Auto-download limits: Telegram's presets for the account (the same the official apps use) until
 * the user sets their own for a network in «Данные и память»; those are kept on this device.
 * Follows the device network.
 */
class AutoDownloadPolicy(context: Context, private val client: TelegramClient, monitor: ConnectionMonitor) {
    private val prefs = context.getSharedPreferences("auto_download", Context.MODE_PRIVATE)
    private val presets = MutableStateFlow<TdApi.AutoDownloadSettingsPresets?>(null)
    private val custom = MutableStateFlow(loadCustom())
    private val gifs = MutableStateFlow(prefs.getBoolean(GIFS_KEY, true))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** GIFs download and play by themselves in chats. */
    val autoplayGifs: StateFlow<Boolean> = gifs.asStateFlow()

    /** Limits shown for each network: the user's own, otherwise the account's preset. */
    val settings: StateFlow<Map<DownloadNetwork, NetworkDownload>> = combine(presets, custom) { loaded, own ->
        DownloadNetwork.entries.associateWith { network -> own[network] ?: loaded?.forNetwork(network)?.toNetworkDownload() ?: NetworkDownload(false, 0, 0, false) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val rule: StateFlow<AutoDownloadRule> = combine(monitor.network, presets, custom, gifs) { network, loaded, own, gifsOn ->
        val mine = network.downloadNetwork()?.let { own[it] }
        val base = when {
            mine == null -> autoDownloadRuleFor(network, loaded)
            mine.enabled -> AutoDownloadRule(mine.maxVideoBytes, mine.maxFileBytes, mine.preloadStories, photos = mine.photos)
            // Turned off by the user: photos wait for a tap too.
            else -> AutoDownloadRule(photos = false)
        }
        base.copy(gifs = gifsOn && network != DeviceNetwork.None)
    }.stateIn(scope, SharingStarted.Eagerly, AutoDownloadRule())

    init {
        client.addUpdateHandler { update ->
            if (update !is TdApi.UpdateAuthorizationState) return@addUpdateHandler
            when (update.authorizationState) {
                is TdApi.AuthorizationStateReady -> client.send(TdApi.GetAutoDownloadSettingsPresets()) { result -> if (result is TdApi.AutoDownloadSettingsPresets) presets.value = result }
                // Another account has its own presets; nothing downloads by itself until they load.
                is TdApi.AuthorizationStateClosed -> if (!client.paused) presets.value = null
            }
        }
    }

    fun set(network: DownloadNetwork, value: NetworkDownload) {
        custom.value = custom.value + (network to value)
        prefs.edit()
            .putBoolean("${network.name}_enabled", value.enabled)
            .putLong("${network.name}_video", value.maxVideoBytes)
            .putLong("${network.name}_file", value.maxFileBytes)
            .putBoolean("${network.name}_stories", value.preloadStories)
            .putBoolean("${network.name}_photos", value.photos)
            .apply()
    }

    fun setAutoplayGifs(enabled: Boolean) {
        gifs.value = enabled
        prefs.edit().putBoolean(GIFS_KEY, enabled).apply()
    }

    /** Back to Telegram's presets for every network. */
    fun reset() {
        prefs.edit().clear().apply()
        custom.value = emptyMap()
        gifs.value = true
    }

    private fun loadCustom(): Map<DownloadNetwork, NetworkDownload> = DownloadNetwork.entries.mapNotNull { network ->
        if (!prefs.contains("${network.name}_enabled")) return@mapNotNull null
        network to NetworkDownload(
            prefs.getBoolean("${network.name}_enabled", true),
            prefs.getLong("${network.name}_video", 0),
            prefs.getLong("${network.name}_file", 0),
            prefs.getBoolean("${network.name}_stories", false),
            prefs.getBoolean("${network.name}_photos", true)
        )
    }.toMap()

    private companion object { const val GIFS_KEY = "gifs" }
}
