package com.kirox.app.ui.v3.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.kirox.app.telegram.TelegramServices
import com.kirox.app.ui.v3.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

/*
 * wave3-account: the account / tools boards on the real account. TDLib calls go through the shared
 * TelegramClient (services.client.query); kiroX-local settings of these boards live in the same
 * "kirox_v3_prefs" file as TelegramKxLocal, mapped onto the app's MessageFilters where one exists.
 */

/** Pure conversions of the account boards (unit-tested). */
internal object KxAccountMapping {
    fun folderInfo(f: TdApi.ChatFolder) = KxFolderInfo(
        title = f.name?.text?.text.orEmpty(), icon = f.icon?.name.orEmpty(), colorId = f.colorId,
        chatIds = (f.pinnedChatIds.toList() + f.includedChatIds.toList()).distinct(), excludedIds = f.excludedChatIds.toList(),
        contacts = f.includeContacts, nonContacts = f.includeNonContacts, groups = f.includeGroups, channels = f.includeChannels, bots = f.includeBots,
        excludeMuted = f.excludeMuted, excludeRead = f.excludeRead, excludeArchived = f.excludeArchived, shareable = f.isShareable,
    )

    /** [base] keeps what the board does not edit (pinned order, animated emoji). */
    fun folder(info: KxFolderInfo, base: TdApi.ChatFolder?): TdApi.ChatFolder {
        val f = base ?: TdApi.ChatFolder().apply { pinnedChatIds = LongArray(0); includedChatIds = LongArray(0); excludedChatIds = LongArray(0) }
        val pinned = f.pinnedChatIds.filter { it in info.chatIds }
        return TdApi.ChatFolder(
            // An unchanged title keeps its entities (custom emoji in the folder name).
            f.name?.takeIf { it.text?.text == info.title.trim() } ?: TdApi.ChatFolderName(TdApi.FormattedText(info.title.trim(), emptyArray()), f.name?.animateCustomEmoji ?: false),
            info.icon.takeIf { it.isNotBlank() }?.let { TdApi.ChatFolderIcon(it) }, info.colorId, f.isShareable,
            pinned.toLongArray(), info.chatIds.filter { it !in pinned }.toLongArray(), info.excludedIds.toLongArray(),
            info.excludeMuted, info.excludeRead, info.excludeArchived, info.contacts, info.nonContacts, info.bots, info.groups, info.channels,
        )
    }

    /** Built-in accent colours 0..6 are free; the rest need Premium. ARGB of the dark theme. */
    fun accent(c: TdApi.AccentColor) = KxAccent(c.id, c.darkThemeColors.toList().ifEmpty { c.lightThemeColors.toList() }, premium = c.id > 6)
    fun profileAccent(c: TdApi.ProfileAccentColor) = KxAccent(c.id, (c.darkThemeColors?.paletteColors?.toList()).orEmpty() + (c.darkThemeColors?.backgroundColors?.toList()).orEmpty(), premium = true)

    fun country(c: TdApi.CountryInfo) = KxCountry(c.countryCode, c.name.ifBlank { c.englishName }, c.callingCodes.firstOrNull().orEmpty(), flag(c.countryCode))
    /** Regional indicator letters → flag emoji. */
    fun flag(code: String): String = if (code.length != 2 || !code.all { it in 'A'..'Z' }) "" else
        code.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")

    fun codeSent(type: TdApi.AuthenticationCodeType?): String = when (type) {
        is TdApi.AuthenticationCodeTypeTelegramMessage -> "Код отправлен в Telegram"
        is TdApi.AuthenticationCodeTypeSms -> "Код отправлен по SMS"
        is TdApi.AuthenticationCodeTypeCall -> "Код продиктуют звонком"
        is TdApi.AuthenticationCodeTypeFlashCall, is TdApi.AuthenticationCodeTypeMissedCall -> "Код придёт звонком"
        else -> "Код отправлен"
    }

    /** Words the real filter uses: own words plus the chosen ready-made sets, without repeats. */
    fun filterWords(own: List<String>, presetBits: Int, presets: List<List<String>>): List<String> =
        (own + presets.filterIndexed { i, _ -> presetBits and (1 shl i) != 0 }.flatten()).distinctBy { it.lowercase() }

    /**
     * Do-not-disturb: the switch (until [until], 0 = until off) or the nightly window [from]..[to] (hours).
     * Chats in [people] still alert.
     */
    fun silenced(on: Boolean, until: Long, sched: Boolean, from: Int, to: Int, nowSec: Long, hour: Int, chatId: Long, people: Set<Long>): Boolean {
        if (chatId in people) return false
        val manual = on && (until == 0L || nowSec < until)
        val night = sched && from != to && if (from < to) hour in from until to else hour >= from || hour < to
        return manual || night
    }

    /**
     * Sunrise and sunset as local hours (NOAA approximation, ±few minutes) for [lat]/[lon] on [dayOfYear];
     * [tzHours] is the local offset from UTC. Null at polar day / night.
     */
    fun sunTimes(lat: Double, lon: Double, dayOfYear: Int, tzHours: Double): Pair<Double, Double>? {
        val g = 2 * Math.PI / 365 * (dayOfYear - 1)
        val eqTime = 229.18 * (0.000075 + 0.001868 * kotlin.math.cos(g) - 0.032077 * kotlin.math.sin(g) - 0.014615 * kotlin.math.cos(2 * g) - 0.040849 * kotlin.math.sin(2 * g))
        val decl = 0.006918 - 0.399912 * kotlin.math.cos(g) + 0.070257 * kotlin.math.sin(g) - 0.006758 * kotlin.math.cos(2 * g) + 0.000907 * kotlin.math.sin(2 * g) -
            0.002697 * kotlin.math.cos(3 * g) + 0.00148 * kotlin.math.sin(3 * g)
        val latR = Math.toRadians(lat)
        val cosH = kotlin.math.cos(Math.toRadians(90.833)) / (kotlin.math.cos(latR) * kotlin.math.cos(decl)) - kotlin.math.tan(latR) * kotlin.math.tan(decl)
        if (cosH < -1 || cosH > 1) return null
        val ha = Math.toDegrees(kotlin.math.acos(cosH))
        val rise = (720 - 4 * (lon + ha) - eqTime) / 60.0 + tzHours
        val set = (720 - 4 * (lon - ha) - eqTime) / 60.0 + tzHours
        return ((rise % 24 + 24) % 24) to ((set % 24 + 24) % 24)
    }

    /** When «Не беспокоить» started now with duration [dur] ends (unix s, 0 = until turned off). */
    fun dndUntil(dur: Int, nowSec: Long, hour: Int, minute: Int, morning: Int): Long = when (dur) {
        0 -> nowSec + 3600
        1 -> nowSec + 3 * 3600
        2 -> { val left = ((morning - hour + 24) % 24).let { if (it == 0) 24 else it } * 3600L - minute * 60L; nowSec + left }
        else -> 0L
    }
}

/** Do-not-disturb check for kiroX's own notifications (notify/MessageNotifications asks before alerting). */
object KxDnd {
    fun silenced(context: Context, chatId: Long): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val d = KxPrefsAccount.defaults
        fun i(k: String) = p.getInt(k, d[k] ?: 0)
        val cal = java.util.Calendar.getInstance()
        val people = p.getString(KxPrefsAccount.DND_PEOPLE, "").orEmpty().split('\n').mapNotNull { it.trim().toLongOrNull() }.toSet()
        return KxAccountMapping.silenced(i(KxPrefsAccount.DND_ON) != 0, p.getLong(UNTIL_LONG, 0L), i(KxPrefsAccount.DND_SCHED) != 0 && p.getBoolean(SCHED_SET, false),
            i(KxPrefsAccount.DND_FROM), i(KxPrefsAccount.DND_TO), System.currentTimeMillis() / 1000, cal.get(java.util.Calendar.HOUR_OF_DAY), chatId, people)
    }
    internal const val PREFS = "kirox_v3_prefs"
    /** The end time as a long (the int pref would overflow in 2038). */
    internal const val UNTIL_LONG = "dnd.untilLong"
    /** The nightly schedule only works once the user touched the board (the default «on» is the board's look). */
    internal const val SCHED_SET = "dnd.schedSet"
}

internal class KxAccountBinder(private val app: Context, private val services: TelegramServices, private val scope: CoroutineScope, private val onError: (String) -> Unit) {
    private val client get() = services.client
    private val prefs = app.getSharedPreferences(KxDnd.PREFS, Context.MODE_PRIVATE)

    // ---- local settings ----
    private val intKeys = KxPrefsAccount.defaults.keys
    private val textKeys = setOf(KxPrefsAccount.FILTER_LIST, KxPrefsAccount.DND_PEOPLE, KxPrefsAccount.SEARCH_RECENT, KxPrefsAccount.HIDDEN_CHATS)
    private val usageKeys = setOf(KxPrefsAccount.USAGE_DAYS, KxPrefsAccount.USAGE_SINCE)
    private val readOnly = setOf(KxPrefsAccount.USAGE_TODAY, KxPrefsAccount.USAGE_WEEK)
    fun owns(key: String) = key in intKeys
    fun ownsText(key: String) = key in textKeys || key in usageKeys

    var values by mutableStateOf(readInts())
        private set
    var texts by mutableStateOf(readTexts())
        private set

    private fun readInts(): Map<String, Int> {
        val filters = com.kirox.app.settings.MessageFilters.state.value
        val usage = usageDays()
        return intKeys.associateWith { prefs.getInt(it, KxPrefsAccount.defaults[it] ?: 0) } + mapOf(
            KxPrefsAccount.USAGE_TODAY to usage.last(), KxPrefsAccount.USAGE_WEEK to usage.sum(),
            KxPrefsAccount.FILTER_ON to if (filters.enabled) 1 else 0,
            KxPrefsAccount.DND_UNTIL to prefs.getLong(KxDnd.UNTIL_LONG, 0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            KxPrefsAccount.DND_SCHED to if (prefs.getBoolean(KxDnd.SCHED_SET, false)) prefs.getInt(KxPrefsAccount.DND_SCHED, 0) else 0,
            KxPrefsAccount.SHORTCUT_BADGE to if (services.notificationPrefs.current.badge) 1 else 0,
        )
    }

    private fun readTexts(): Map<String, String> {
        val own = prefs.getString(KxPrefsAccount.FILTER_LIST, null)
            ?: com.kirox.app.settings.MessageFilters.state.value.words.joinToString("\n") // first open: the app's words are the own words
        return textKeys.associateWith { prefs.getString(it, "").orEmpty() } + (KxPrefsAccount.FILTER_LIST to own) +
            (KxPrefsAccount.USAGE_DAYS to usageDays().joinToString(",")) + (KxPrefsAccount.USAGE_SINCE to prefs.getString(USAGE_SINCE_KEY, "").orEmpty())
    }

    fun start() {
        scope.launch { com.kirox.app.settings.MessageFilters.state.collect { values = readInts() } }
        // Archive flags as Telegram has them (another device may have changed them).
        scope.launch {
            runCatching { client.query(org.drinkless.tdlib.TdApi.GetArchiveChatListSettings()) }.getOrNull()?.let { a ->
                prefs.edit { putInt(KxPrefsAccount.ARCHIVE_KEEP, if (a.keepUnmutedChatsArchived) 1 else 0); putInt(KxPrefsAccount.ARCHIVE_FOLDERS, if (a.keepChatsFromFoldersArchived) 1 else 0) }
                values = readInts()
            }
        }
        // Wellbeing: a minute counts when kiroX is on screen (local only, from the first start of this version).
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                if (com.kirox.app.notify.AppVisibility.visible) {
                    val key = USAGE_DAY_PREFIX + day(0)
                    prefs.edit { putInt(key, prefs.getInt(key, 0) + 1); if (!prefs.contains(USAGE_SINCE_KEY)) putString(USAGE_SINCE_KEY, day(0)) }
                    texts = readTexts(); values = readInts()
                }
            }
        }
        scope.launch { services.notificationPrefs.settings.collect { values = readInts() } }
    }

    fun put(key: String, value: Int) {
        if (key in readOnly) return
        when (key) {
            KxPrefsAccount.FILTER_ON -> filters { it.copy(enabled = value != 0) }
            KxPrefsAccount.FILTER_PRESETS -> { prefs.edit { putInt(key, value) }; syncFilterWords(value, prefs.getString(KxPrefsAccount.FILTER_LIST, null)) }
            KxPrefsAccount.DND_UNTIL -> prefs.edit { putLong(KxDnd.UNTIL_LONG, value.toLong()) }
            KxPrefsAccount.DND_SCHED -> prefs.edit { putInt(key, value); putBoolean(KxDnd.SCHED_SET, true) }
            // «Счётчик на иконке» is the app's notification badge setting.
            KxPrefsAccount.SHORTCUT_BADGE -> services.notificationPrefs.update(services.notificationPrefs.current.copy(badge = value != 0))
            // «Тише после 23:00» = the do-not-disturb night window 23–07.
            KxPrefsAccount.WELL_QUIET -> prefs.edit {
                putInt(key, value); putInt(KxPrefsAccount.DND_SCHED, value); putBoolean(KxDnd.SCHED_SET, true)
                if (value != 0) { putInt(KxPrefsAccount.DND_FROM, 23); putInt(KxPrefsAccount.DND_TO, 7) }
            }
            // Archive: the two Telegram flags go to TDLib too (local copy = what the sheet shows offline).
            KxPrefsAccount.ARCHIVE_KEEP, KxPrefsAccount.ARCHIVE_FOLDERS -> {
                prefs.edit { putInt(key, value) }
                scope.launch {
                    try {
                        val cur = client.query(org.drinkless.tdlib.TdApi.GetArchiveChatListSettings())
                        if (key == KxPrefsAccount.ARCHIVE_KEEP) cur.keepUnmutedChatsArchived = value != 0 else cur.keepChatsFromFoldersArchived = value != 0
                        client.query(org.drinkless.tdlib.TdApi.SetArchiveChatListSettings(cur))
                    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { onError(KxExtrasMapping.errorText(e)) }
                }
            }
            else -> prefs.edit { putInt(key, value) }
        }
        values = values + (key to value)
    }

    private fun day(back: Int): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        .format(java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -back) }.time)
    private fun usageDays(): List<Int> = (6 downTo 0).map { prefs.getInt(USAGE_DAY_PREFIX + day(it), 0) }

    fun putText(key: String, value: String) {
        if (key !in textKeys) return
        prefs.edit { putString(key, value) }
        if (key == KxPrefsAccount.FILTER_LIST) syncFilterWords(prefs.getInt(KxPrefsAccount.FILTER_PRESETS, 0), value)
        texts = texts + (key to value)
    }

    /** The app's filter gets own words + chosen sets (the board's sets, same order). */
    private fun syncFilterWords(presetBits: Int, ownText: String?) {
        val own = (ownText ?: texts[KxPrefsAccount.FILTER_LIST].orEmpty()).split('\n').filter { it.isNotBlank() }
        filters { it.copy(words = KxAccountMapping.filterWords(own, presetBits, FILTER_PRESET_WORDS)) }
    }
    private fun filters(change: (com.kirox.app.settings.Filters) -> com.kirox.app.settings.Filters) =
        com.kirox.app.settings.MessageFilters.update(change(com.kirox.app.settings.MessageFilters.state.value))

    // ---- folders ----
    suspend fun folderInfo(id: Int): KxFolderInfo? = if (id <= 0) null else orNull { KxAccountMapping.folderInfo(client.query(TdApi.GetChatFolder(id))) }
    suspend fun saveFolder(id: Int, info: KxFolderInfo): String? {
        if (info.title.isBlank()) return "Введите название папки"
        if (info.chatIds.isEmpty() && !(info.contacts || info.nonContacts || info.groups || info.channels || info.bots)) return "Добавьте в папку хотя бы один чат или тип чатов"
        return attempt {
            if (id > 0) client.query(TdApi.EditChatFolder(id, KxAccountMapping.folder(info, client.query(TdApi.GetChatFolder(id)))))
            else client.query(TdApi.CreateChatFolder(KxAccountMapping.folder(info, null)))
        }
    }
    suspend fun folderShareableChats(id: Int): List<Long> = if (id <= 0) emptyList() else orNull { client.query(TdApi.GetChatsForChatFolderInviteLink(id)).chatIds.toList() }.orEmpty()
    suspend fun folderLinks(id: Int): List<KxInviteLink> = if (id <= 0) emptyList() else orNull {
        client.query(TdApi.GetChatFolderInviteLinks(id)).inviteLinks.map { KxInviteLink(it.inviteLink, it.name, members = it.chatIds.size) }
    }.orEmpty()
    suspend fun createFolderLink(id: Int, chatIds: List<Long>): String? = try {
        client.query(TdApi.CreateChatFolderInviteLink(id, "", chatIds.toLongArray())).inviteLink
    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { onError(KxExtrasMapping.errorText(e)); null }
    fun deleteFolderLink(id: Int, link: String) = act { client.query(TdApi.DeleteChatFolderInviteLink(id, link)) }

    // ---- personal channel ----
    suspend fun personalChannels(meId: Long): KxPersonalChannels {
        val current = orNull { client.query(TdApi.GetUserFullInfo(meId)).personalChatId } ?: 0L
        val ids = orNull { client.query(TdApi.GetSuitablePersonalChats()).chatIds.toList() }.orEmpty()
        val list = (ids + listOfNotNull(current.takeIf { it != 0L && it !in ids })).mapNotNull { id ->
            val chat = orNull { client.query(TdApi.GetChat(id)) } ?: return@mapNotNull null
            val subs = (chat.type as? TdApi.ChatTypeSupergroup)?.let { t -> orNull { client.query(TdApi.GetSupergroup(t.supergroupId)).memberCount } } ?: 0
            KxPersonalChannel(id, chat.title, subs, lastPost(chat.lastMessage), colorIndex = chat.accentColorId)
        }
        return KxPersonalChannels(current, list)
    }
    private fun lastPost(m: TdApi.Message?): String = when (val c = m?.content) {
        null -> ""
        is TdApi.MessageText -> c.text.text
        is TdApi.MessagePhoto -> c.caption.text.ifBlank { "Фото" }
        is TdApi.MessageVideo -> c.caption.text.ifBlank { "Видео" }
        is TdApi.MessageDocument -> c.caption.text.ifBlank { "Файл" }
        is TdApi.MessageAudio -> c.caption.text.ifBlank { "Музыка" }
        is TdApi.MessagePoll -> "Опрос"
        else -> "Сообщение"
    }.replace('\n', ' ').take(120)
    suspend fun choosePersonalChannel(chatId: Long): String? = attempt { services.account.setPersonalChannel(chatId) }

    // ---- change number ----
    suspend fun changeNumberSendCode(phone: String): String = try {
        KxAccountMapping.codeSent(client.query(TdApi.SendPhoneNumberCode(phone.filter { it.isDigit() || it == '+' }, TdApi.PhoneNumberAuthenticationSettings(), TdApi.PhoneNumberCodeTypeChange())).type)
    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { "!" + KxExtrasMapping.errorText(e) }
    suspend fun changeNumberResend(): String = try {
        KxAccountMapping.codeSent(client.query(TdApi.ResendPhoneNumberCode(TdApi.ResendCodeReasonUserRequest())).type)
    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { "!" + KxExtrasMapping.errorText(e) }
    suspend fun changeNumberCheck(code: String): String? = attempt { services.account.confirmChangeNumber(code.trim()) }

    // ---- login e-mail ----
    suspend fun loginEmailPattern(): String = orNull { client.query(TdApi.GetPasswordState()).loginEmailAddressPattern }.orEmpty()
    suspend fun loginEmailSendCode(email: String): String? = attempt { client.query(TdApi.SetLoginEmailAddress(email.trim())) }
    suspend fun loginEmailResend(): String? = attempt { client.query(TdApi.ResendLoginEmailAddressCode()) }
    suspend fun loginEmailCheck(code: String): String? = attempt { client.query(TdApi.CheckLoginEmailAddressCode(TdApi.EmailAddressAuthenticationCode(code.trim()))) }

    fun confirmSession(id: Long, done: suspend () -> Unit) = act { client.query(TdApi.ConfirmSession(id)); done() }

    // ---- terms ----
    /** Only terms Telegram wants shown as a popup (showPopup); the others are accepted silently by the official apps too. */
    val termsUpdate: KxTerms? get() = services.accountUpdates.terms.value?.takeIf { it.termsOfService.showPopup }?.let { KxTerms(it.termsOfServiceId, it.termsOfService.text.text, it.termsOfService.minUserAge) }
    suspend fun acceptTerms(id: String): String? = attempt { client.query(TdApi.AcceptTermsOfService(id)); services.accountUpdates.termsDone() }

    // ---- groups ----
    private suspend fun supergroupId(chatId: Long): Long? = (orNull { client.query(TdApi.GetChat(chatId)) }?.type as? TdApi.ChatTypeSupergroup)?.supergroupId
    suspend fun antiSpam(chatId: Long): Boolean? {
        val id = supergroupId(chatId) ?: return null
        val full = orNull { client.query(TdApi.GetSupergroupFullInfo(id)) } ?: return null
        return if (full.canToggleAggressiveAntiSpam || full.hasAggressiveAntiSpamEnabled) full.hasAggressiveAntiSpamEnabled else null
    }
    fun switchAntiSpam(chatId: Long, on: Boolean) = act { client.query(TdApi.ToggleSupergroupHasAggressiveAntiSpamEnabled(supergroupId(chatId) ?: error("not a supergroup"), on)) }
    fun reportAntiSpamMistake(chatId: Long, messageId: Long) = act { client.query(TdApi.ReportSupergroupAntiSpamFalsePositive(supergroupId(chatId) ?: error("not a supergroup"), messageId)) }
    /** Only supergroups have the switch; a basic group shows its whole history to new members anyway. */
    fun switchHistoryForNew(chatId: Long, on: Boolean) {
        scope.launch {
            // Basic groups have no switch in TDLib: say so instead of a silent no-op.
            val id = supergroupId(chatId) ?: run { onError("Эта настройка есть только у супергрупп"); return@launch }
            attempt { client.query(TdApi.ToggleSupergroupIsAllHistoryAvailable(id, on)) }?.let(onError)
        }
    }
    suspend fun upgradeGroup(chatId: Long): Long? = try {
        val chat = client.query(TdApi.GetChat(chatId))
        if (chat.type is TdApi.ChatTypeSupergroup) chatId else client.query(TdApi.UpgradeBasicGroupChatToSupergroupChat(chatId)).id
    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { onError(KxExtrasMapping.errorText(e)); null }
    suspend fun publicChat(username: String): Long? = orNull { client.query(TdApi.SearchPublicChat(username.removePrefix("@"))).id }

    // ---- contacts ----
    suspend fun importedContactCount(): Int? = orNull { client.query(TdApi.GetImportedContactCount()).count }
    suspend fun clearImportedContacts(): String? = attempt { client.query(TdApi.ClearImportedContacts()) }

    // ---- colours ----
    val accentColors: List<KxAccent> get() = services.accountUpdates.accentColors.value.map { KxAccountMapping.accent(it) }
    val profileColors: List<KxAccent> get() = services.accountUpdates.profileColors.value.map { KxAccountMapping.profileAccent(it) }
    var myColors by mutableStateOf(0 to -1)
        private set
    private var colorsAsked = false
    fun loadMyColors() {
        if (colorsAsked) return
        colorsAsked = true
        scope.launch { orNull { client.query(TdApi.GetMe()) }?.let { myColors = it.accentColorId to it.profileAccentColorId } ?: run { colorsAsked = false } }
    }
    // The second argument is the (Premium) background emoji: keep the one the user has.
    suspend fun applyNameColor(id: Int): String? = attempt {
        client.query(TdApi.SetAccentColor(id, client.query(TdApi.GetMe()).backgroundCustomEmojiId)); myColors = id to myColors.second
    }
    suspend fun applyProfileColor(id: Int): String? = attempt {
        client.query(TdApi.SetProfileAccentColor(id, client.query(TdApi.GetMe()).profileBackgroundCustomEmojiId)); myColors = myColors.first to id
    }

    // ---- emoji status ----
    suspend fun emojiStatuses(): List<KxEmojiStatus> {
        val ids = orNull { client.query(TdApi.GetDefaultEmojiStatuses()).customEmojiIds.toList() }.orEmpty().take(40)
        if (ids.isEmpty()) return emptyList()
        val stickers = orNull { client.query(TdApi.GetCustomEmojiStickers(ids.toLongArray())).stickers.toList() }.orEmpty()
        return stickers.mapNotNull { s ->
            val id = (s.fullType as? TdApi.StickerFullTypeCustomEmoji)?.customEmojiId ?: return@mapNotNull null
            KxEmojiStatus(id, s.emoji.ifBlank { "⭐" })
        }
    }
    suspend fun applyEmojiStatus(customEmojiId: Long, until: Int): String? = attempt {
        client.query(TdApi.SetEmojiStatus(if (customEmojiId == 0L) null else TdApi.EmojiStatus(TdApi.EmojiStatusTypeCustomEmoji(customEmojiId), until)))
    }

    // ---- countries ----
    suspend fun countries(): List<KxCountry> = orNull { client.query(TdApi.GetCountries()).countries.filter { !it.isHidden }.map { KxAccountMapping.country(it) } }.orEmpty()
        .sortedWith(compareBy(java.text.Collator.getInstance(java.util.Locale("ru"))) { it.name })
    suspend fun myCountry(): String = orNull { client.query(TdApi.GetCountryCode()).text }.orEmpty()

    private fun act(block: suspend () -> Unit) { scope.launch { attempt(block)?.let(onError) } }

    companion object {
        private const val USAGE_DAY_PREFIX = "usage.d."
        private const val USAGE_SINCE_KEY = "usage.sinceDay"
        /** The Word filter board's ready-made sets, in its order (Реклама, Крипта, Спойлеры). */
        val FILTER_PRESET_WORDS = listOf(listOf("скидка", "промокод", "подпишись"), listOf("airdrop", "x100", "памп"), listOf("финал", "спойлер"))
    }
}
