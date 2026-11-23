package com.kirox.app.ui.v3.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.kirox.app.settings.BubbleShape
import com.kirox.app.settings.ChatDensity
import com.kirox.app.settings.ChatTextSize
import com.kirox.app.settings.ChatWallpaper
import com.kirox.app.settings.DoubleTapAction
import com.kirox.app.settings.PowerSaving
import com.kirox.app.telegram.CallDataSaving
import com.kirox.app.ui.v3.model.KxPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * kiroX-local settings of the v3 boards (KxPrefs keys) on top of the app's existing settings, so the
 * old screens and the repositories keep seeing the same values. Keys without an app setting live
 * in the "kirox_v3_prefs" shared preferences.
 */

/** Message request card texts (unit-tested). */
internal object KxRequestMapping {
    /** «@erlan_s · нет фото · 2 общие группы», «нет общих чатов · нет фото». */
    fun meta(username: String, hasPhoto: Boolean, common: Int): String = listOfNotNull(
        username.takeIf { it.isNotBlank() }?.let { "@$it" },
        if (common == 0) "нет общих чатов" else "$common ${KxFormat.plural(common, "общая группа", "общие группы", "общих групп")}",
        if (hasPhoto) null else "нет фото",
    ).joinToString(" · ")

    fun hiddenMedia(kind: com.kirox.app.telegram.TelegramMessageKind): String? = when (kind) {
        com.kirox.app.telegram.TelegramMessageKind.Photo -> "Фото скрыто"
        com.kirox.app.telegram.TelegramMessageKind.Video, com.kirox.app.telegram.TelegramMessageKind.Animation -> "Видео скрыто"
        com.kirox.app.telegram.TelegramMessageKind.Document -> "Файл скрыт"
        else -> null
    }
}

/** Pure board index ↔ app setting conversions (unit-tested). */
internal object KxLocalMapping {
    /** Board order: Острые, Мягкие, Круглые. */
    private val bubbles = listOf(BubbleShape.Strict, BubbleShape.Medium, BubbleShape.Soft)
    fun bubbles(b: BubbleShape) = bubbles.indexOf(b).coerceAtLeast(0)
    fun bubbles(i: Int) = bubbles.getOrElse(i) { BubbleShape.Medium }

    fun textSize(t: ChatTextSize) = t.ordinal
    fun textSize(i: Int) = ChatTextSize.entries.getOrElse(i) { ChatTextSize.Normal }
    fun density(d: ChatDensity) = d.ordinal
    fun density(i: Int) = ChatDensity.entries.getOrElse(i) { ChatDensity.Normal }

    /** Board wallpapers Тема, Узор, Градиент, Ночь, Нет; «Ночь» has no app wallpaper (null = leave it). */
    fun wallpaper(i: Int): ChatWallpaper? = when (i) { 0 -> ChatWallpaper.Theme; 1 -> ChatWallpaper.Pattern; 2 -> ChatWallpaper.Gradient; 4 -> ChatWallpaper.None; else -> null }
    fun wallpaper(w: ChatWallpaper): Int = when (w) { ChatWallpaper.Theme -> 0; ChatWallpaper.Pattern, ChatWallpaper.Dots -> 1; ChatWallpaper.Gradient -> 2; ChatWallpaper.None -> 4 }

    /** Features board: Как в системе, Всегда, Никогда = PowerSaving order. */
    fun powerMode(p: PowerSaving) = when (p) { PowerSaving.Auto -> 0; PowerSaving.On -> 1; PowerSaving.Off -> 2 }
    fun powerMode(i: Int) = when (i) { 1 -> PowerSaving.On; 2 -> PowerSaving.Off; else -> PowerSaving.Auto }

    /** Power board threshold: Never → 0, Always → 100, Auto → own threshold (15 when it follows only the system). */
    fun threshold(p: PowerSaving, own: Int) = when (p) { PowerSaving.Off -> 0; PowerSaving.On -> 100; PowerSaving.Auto -> own.takeIf { it in 1..99 } ?: 15 }

    fun doubleTap(d: DoubleTapAction) = d.ordinal
    fun doubleTap(i: Int) = DoubleTapAction.entries.getOrElse(i) { DoubleTapAction.None }

    /** Call data cards: Высокое (never save), Сбалансированно (mobile network), Экономно (always). */
    fun callData(c: CallDataSaving) = when (c) { CallDataSaving.Never -> 0; CallDataSaving.Mobile -> 1; CallDataSaving.Always -> 2 }
    fun callData(i: Int) = when (i) { 0 -> CallDataSaving.Never; 2 -> CallDataSaving.Always; else -> CallDataSaving.Mobile }

    /** Night mode «По расписанию» (1) is the app's auto theme: night from [from] to [to]. */
    fun nightMode(autoTheme: Boolean, stored: Int) = if (autoTheme) 1 else stored.takeIf { it != 1 } ?: 3

    /** Dock tab keys for I18n own names, by the board's tab index. */
    val tabKeys = listOf("Контакты", "Звонки", "Чаты", "Истории", "Настройки")
}

internal class KxLocalSettings(private val app: Context, private val scope: CoroutineScope) {
    private val settings = (app as com.kirox.app.KiroXApplication).settings
    private val media = (app as com.kirox.app.KiroXApplication).telegram.media
    private val prefs = app.getSharedPreferences("kirox_v3_prefs", Context.MODE_PRIVATE)
    private val own = setOf(KxPrefs.THEME, KxPrefs.CHAT_WALLPAPER, KxPrefs.TRANSLATE, KxPrefs.TRANSLATE_TO, KxPrefs.POWER_OFF, KxPrefs.NOISE, KxPrefs.ECHO,
        KxPrefs.RINGTONE, KxPrefs.NIGHT_MODE, KxPrefs.NIGHT_THEME, KxPrefs.WALL, KxPrefs.WALL_BLUR, KxPrefs.WALL_MOTION, KxPrefs.WALL_DIM,
        KxPrefs.ANIM_ON, KxPrefs.ANIM_SCENE, KxPrefs.ANIM_SPEED, KxPrefs.ANIM_QUAL)

    var values by mutableStateOf(read())
        private set

    private fun stored(key: String) = prefs.getInt(key, KxPrefs.defaults[key] ?: 0)

    private fun read(): Map<String, Int> {
        val tweaks = com.kirox.app.settings.KiroxTweaks.current
        val auto = autoTheme()
        val perChat = prefs.all.filter { (k, v) -> ':' in k && v is Int }.mapValues { it.value as Int }
        return own.associateWith { stored(it) } + perChat + mapOf(
            KxPrefs.OWN_THEMES to com.kirox.app.settings.CustomThemes.themes.value.size,
            KxPrefs.CHAT_LOOKS to com.kirox.app.settings.ChatWallpapers.count(app),
            KxPrefs.BUBBLES to KxLocalMapping.bubbles(settings.bubbleShape.value),
            KxPrefs.TEXT_SIZE to KxLocalMapping.textSize(settings.chatTextSize.value),
            KxPrefs.DENSITY to KxLocalMapping.density(settings.chatDensity.value),
            KxPrefs.LANGUAGE to if (com.kirox.app.i18n.I18n.language == com.kirox.app.i18n.AppLanguage.English) 1 else 0,
            KxPrefs.POWER_THRESHOLD to KxLocalMapping.threshold(tweaks.powerSaving, com.kirox.app.settings.PowerSaver.threshold.value),
            KxPrefs.POWER_MODE to KxLocalMapping.powerMode(tweaks.powerSaving),
            KxPrefs.BATTERY to com.kirox.app.settings.PowerSaver.battery.value,
            KxPrefs.POWER_ACTIVE to if (com.kirox.app.settings.PowerSaver.active.value) 1 else 0,
            KxPrefs.DOUBLE_TAP to KxLocalMapping.doubleTap(tweaks.doubleTap),
            KxPrefs.FILTER_WORDS to com.kirox.app.settings.MessageFilters.state.value.words.size,
            KxPrefs.CALL_DATA to KxLocalMapping.callData(media.callDataSaving.value),
            KxPrefs.NIGHT_MODE to KxLocalMapping.nightMode(auto.enabled, stored(KxPrefs.NIGHT_MODE)),
            KxPrefs.NIGHT_FROM to auto.nightStart,
            KxPrefs.NIGHT_TO to auto.dayStart,
        ).let { mapped ->
            // A wallpaper the board can show keeps the board's own pick («Ночь» is kiroX-only).
            val wall = stored(KxPrefs.CHAT_WALLPAPER)
            val appWall = KxLocalMapping.wallpaper(settings.chatWallpaper.value)
            mapped + (KxPrefs.CHAT_WALLPAPER to if (KxLocalMapping.wallpaper(wall) == null) wall else appWall)
        }
    }

    /** Re-reads when any app setting behind the keys changes (also from the old screens). */
    fun start() {
        scope.launch {
            combine(settings.bubbleShape, settings.chatTextSize, settings.chatDensity, settings.chatWallpaper, settings.autoTheme) { _, _, _, _, _ -> }
                .collect { values = read() }
        }
        scope.launch {
            combine(com.kirox.app.settings.KiroxTweaks.state, com.kirox.app.settings.PowerSaver.battery, com.kirox.app.settings.PowerSaver.threshold,
                media.callDataSaving, com.kirox.app.settings.CustomThemes.themes) { _, _, _, _, _ -> }
                .collect { values = read() }
        }
        scope.launch { com.kirox.app.settings.MessageFilters.state.collect { values = read() } }
        scope.launch { com.kirox.app.settings.PowerSaver.active.collect { values = read() } }
        // The DataStore value caught up with what was written: drop the pending copy.
        scope.launch { settings.autoTheme.collect { if (it == pendingAuto) pendingAuto = null } }
    }

    fun put(key: String, value: Int) {
        when (key) {
            KxPrefs.BUBBLES -> settings.setBubbleShape(KxLocalMapping.bubbles(value))
            KxPrefs.TEXT_SIZE -> settings.setChatTextSize(KxLocalMapping.textSize(value))
            KxPrefs.DENSITY -> settings.setChatDensity(KxLocalMapping.density(value))
            KxPrefs.CHAT_WALLPAPER -> { store(key, value); KxLocalMapping.wallpaper(value)?.let { settings.setChatWallpaper(it) } }
            KxPrefs.LANGUAGE -> com.kirox.app.i18n.I18n.chooseLanguage(if (value == 1) com.kirox.app.i18n.AppLanguage.English else com.kirox.app.i18n.AppLanguage.Russian)
            KxPrefs.POWER_THRESHOLD -> {
                val mode = when { value <= 0 -> PowerSaving.Off; value >= 100 -> PowerSaving.On; else -> PowerSaving.Auto }
                if (mode == PowerSaving.Auto) com.kirox.app.settings.PowerSaver.setThreshold(value)
                tweak { it.copy(powerSaving = mode) }
            }
            KxPrefs.POWER_MODE -> tweak { it.copy(powerSaving = KxLocalMapping.powerMode(value)) }
            KxPrefs.DOUBLE_TAP -> tweak { it.copy(doubleTap = KxLocalMapping.doubleTap(value)) }
            KxPrefs.CALL_DATA -> media.setCallDataSaving(KxLocalMapping.callData(value))
            KxPrefs.NIGHT_MODE -> { store(key, value); changeAutoTheme { it.copy(enabled = value == 1) } }
            KxPrefs.NIGHT_FROM -> changeAutoTheme { it.copy(nightStart = value.mod(24)) }
            KxPrefs.NIGHT_TO -> changeAutoTheme { it.copy(dayStart = value.mod(24)) }
            in own -> store(key, value)
            else -> if (':' in key && key.substringBefore(':').startsWith("wall.")) store(key, value) else return // read-only or unknown
        }
        // Optimistic: the flows confirm a moment later.
        values = values + (key to value)
    }

    private fun store(key: String, value: Int) = prefs.edit { putInt(key, value) }

    /** The auto theme written last: DataStore updates its flow only after the async edit, so two quick
     *  changes in a row (from + to) must build on what was just written, not on the stale flow value. */
    private var pendingAuto: com.kirox.app.settings.AutoTheme? = null
    private fun autoTheme() = pendingAuto ?: settings.autoTheme.value
    private fun changeAutoTheme(change: (com.kirox.app.settings.AutoTheme) -> com.kirox.app.settings.AutoTheme) {
        val next = change(autoTheme())
        pendingAuto = next
        settings.setAutoTheme(next)
    }
    private fun tweak(change: (com.kirox.app.settings.Tweaks) -> com.kirox.app.settings.Tweaks) =
        com.kirox.app.settings.KiroxTweaks.update(change(com.kirox.app.settings.KiroxTweaks.current))

    // Dock tab names: the app's own-names pack (I18n), keyed by the Russian tab title.
    val tabNames: Map<Int, String>
        get() = com.kirox.app.i18n.I18n.overrides.let { o -> KxLocalMapping.tabKeys.withIndex().mapNotNull { (i, k) -> o[k]?.let { i to it } }.toMap() }
    fun renameTab(index: Int, name: String) {
        val key = KxLocalMapping.tabKeys.getOrNull(index) ?: return
        // Removing a name that is not there would make I18n create an empty pack.
        if (name.isBlank() && com.kirox.app.i18n.I18n.overrides[key] == null) return
        com.kirox.app.i18n.I18n.setOverride(key, name.trim().ifEmpty { null })
    }

    // Passcode: security/PasscodeLock (the same code the old lock screen asks for).
    val passcodeSet: Boolean get() = com.kirox.app.security.PasscodeLock.settings.value.enabled
    val passcodeBiometric: Boolean get() = com.kirox.app.security.PasscodeLock.settings.value.biometric
    /** PBKDF hashing takes tens of ms: off the main thread. */
    suspend fun checkPasscode(code: String): Boolean = withContext(Dispatchers.Default) { com.kirox.app.security.PasscodeLock.unlock(code) }
    fun biometricOk() = com.kirox.app.security.PasscodeLock.unlockByBiometric()
    fun passcodeWait(): Int = ((com.kirox.app.security.PasscodeLock.blockedUntil - android.os.SystemClock.elapsedRealtime() + 999) / 1000).toInt().coerceAtLeast(0)
}
