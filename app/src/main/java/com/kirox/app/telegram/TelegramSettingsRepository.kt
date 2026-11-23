package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi

/** Who may see or do something (base privacy rule; per-user exceptions are kept as they are). */
enum class PrivacyLevel(val title: String) { Everybody("Все"), Contacts("Мои контакты"), Nobody("Никто") }

/** Privacy settings shown in KiroX; [levels] are the choices Telegram allows for the item. */
enum class PrivacyItem(val title: String, val levels: List<PrivacyLevel> = PrivacyLevel.entries) {
    PhoneNumber("Номер телефона"),
    FindByPhone("Кто найдёт меня по номеру", listOf(PrivacyLevel.Everybody, PrivacyLevel.Contacts)),
    LastSeen("Время последней активности"),
    ProfilePhoto("Фото профиля"),
    Bio("О себе"),
    Birthdate("Дата рождения"),
    ProfileAudio("Музыка в профиле"),
    Gifts("Подарки в профиле"),
    ForwardLink("Ссылка при пересылке"),
    Calls("Звонки"),
    PeerToPeer("Прямое соединение в звонках"),
    VoiceNotes("Голосовые и видеосообщения"),
    ChatInvites("Приглашения в группы");

    internal fun toTd(): TdApi.UserPrivacySetting = when (this) {
        PhoneNumber -> TdApi.UserPrivacySettingShowPhoneNumber()
        FindByPhone -> TdApi.UserPrivacySettingAllowFindingByPhoneNumber()
        ProfileAudio -> TdApi.UserPrivacySettingShowProfileAudio()
        Gifts -> TdApi.UserPrivacySettingAutosaveGifts()
        PeerToPeer -> TdApi.UserPrivacySettingAllowPeerToPeerCalls()
        LastSeen -> TdApi.UserPrivacySettingShowStatus()
        ProfilePhoto -> TdApi.UserPrivacySettingShowProfilePhoto()
        Calls -> TdApi.UserPrivacySettingAllowCalls()
        ChatInvites -> TdApi.UserPrivacySettingAllowChatInvites()
        Bio -> TdApi.UserPrivacySettingShowBio()
        Birthdate -> TdApi.UserPrivacySettingShowBirthdate()
        ForwardLink -> TdApi.UserPrivacySettingShowLinkInForwardedMessages()
        VoiceNotes -> TdApi.UserPrivacySettingAllowPrivateVoiceAndVideoNoteMessages()
    }
}

/** Notification scopes: private chats, groups, channels. */
enum class NotifyScope(val title: String) {
    Private("Личные чаты"), Groups("Группы"), Channels("Каналы");

    internal fun toTd(): TdApi.NotificationSettingsScope = when (this) {
        Private -> TdApi.NotificationSettingsScopePrivateChats()
        Groups -> TdApi.NotificationSettingsScopeGroupChats()
        Channels -> TdApi.NotificationSettingsScopeChannelChats()
    }
}

/** Defaults for one chat type; [exceptions] is how many chats of the type differ from them. */
/** Base level of a privacy item and how many people or chats are exceptions to it. */
data class PrivacyState(val level: PrivacyLevel, val exceptions: Int)

/** «Удалить мой аккаунт, если меня не было»: Telegram's allowed periods. */
enum class AccountTtl(val title: String, val days: Int) {
    Month("1 месяц", 30), ThreeMonths("3 месяца", 91), HalfYear("6 месяцев", 182), Year("1 год", 365), YearHalf("18 месяцев", 548), TwoYears("2 года", 730);

    companion object { fun of(days: Int) = entries.minBy { kotlin.math.abs(it.days - days) } }
}

/** Message and chat privacy outside the per-item rules; null where Telegram did not answer. */
data class MessagePrivacy(
    val showReadDate: Boolean?,
    val newChatsFromStrangers: Boolean?,
    val archiveNewChats: Boolean?,
    val suggestFrequent: Boolean?
)

data class ScopeNotifications(
    val enabled: Boolean,
    val showPreview: Boolean,
    val pinned: Boolean = true,
    val mentions: Boolean = true,
    val exceptions: Int = 0,
    /** New stories of these chats (Telegram keeps this for private chats). */
    val stories: Boolean = true
)

/** Whose reactions to your messages and stories notify you. */
enum class ReactionSource(val title: String) { Nobody("Никто"), Contacts("Мои контакты"), Everybody("Все") }

data class ReactionNotifications(val messages: ReactionSource, val stories: ReactionSource, val showPreview: Boolean)

private fun TdApi.ReactionNotificationSource?.toSource() = when (this) {
    is TdApi.ReactionNotificationSourceAll -> ReactionSource.Everybody
    is TdApi.ReactionNotificationSourceContacts -> ReactionSource.Contacts
    else -> ReactionSource.Nobody
}

private fun ReactionSource.toTd(): TdApi.ReactionNotificationSource = when (this) {
    ReactionSource.Everybody -> TdApi.ReactionNotificationSourceAll()
    ReactionSource.Contacts -> TdApi.ReactionNotificationSourceContacts()
    ReactionSource.Nobody -> TdApi.ReactionNotificationSourceNone()
}

/** A blocked user or chat. */
data class BlockedSender(val id: Long, val isChat: Boolean, val name: String, val photo: TelegramFileRef?)

/** Cloud password (two-step verification) state; [pendingEmailPattern] while a recovery email waits for its code. */
data class CloudPasswordState(val hasPassword: Boolean, val hint: String, val hasRecoveryEmail: Boolean, val pendingEmailPattern: String?,
                              /** Unix time when a requested password reset happens; 0 = none pending. */
                              val pendingResetDate: Int = 0)

/** Result of «Сбросить пароль» without the recovery e-mail: done now, scheduled for [date], or refused until [date] (unix seconds). */
sealed interface PasswordResetResult {
    data object Done : PasswordResetResult
    data class Pending(val date: Int) : PasswordResetResult
    data class Declined(val retryDate: Int) : PasswordResetResult
}

/** A person in a privacy exception list. */
data class PrivacyPerson(val id: Long, val name: String, val photo: TelegramFileRef?)

/** «Всегда разрешать» / «Никогда не разрешать» people of a privacy rule; [chatRules] = group-member exceptions kept as they are. */
data class PrivacyExceptions(val allow: List<PrivacyPerson>, val never: List<PrivacyPerson>, val chatRules: Int)

/** A chat whose notifications differ from the defaults. */
data class NotificationException(val chatId: Long, val title: String, val muted: Boolean)

enum class ProxyKind(val title: String) { Socks5("SOCKS5"), Http("HTTP"), MtProto("MTProto") }

/** A proxy added to TDLib. */
data class TelegramProxy(val id: Int, val server: String, val port: Int, val kind: ProxyKind, val enabled: Boolean)

/** Proxy from a tg://proxy, tg://socks or t.me/proxy link; null when the text is not one. */
fun parseProxyLink(link: String): Triple<String, Int, Pair<ProxyKind, String>>? {
    val text = link.trim()
    val kind = when {
        text.startsWith("tg://proxy", true) || text.contains("t.me/proxy", true) -> ProxyKind.MtProto
        text.startsWith("tg://socks", true) || text.contains("t.me/socks", true) -> ProxyKind.Socks5
        else -> return null
    }
    val params = text.substringAfter('?', "").split('&').mapNotNull { part ->
        part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].lowercase() to java.net.URLDecoder.decode(it[1], "UTF-8") }
    }.toMap()
    val server = params["server"]?.takeIf { it.isNotBlank() } ?: return null
    val port = params["port"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
    return Triple(server, port, kind to (if (kind == ProxyKind.MtProto) params["secret"].orEmpty() else listOf(params["user"].orEmpty(), params["pass"].orEmpty()).joinToString(":")))
}

data class StorageUsage(val filesBytes: Long, val fileCount: Int, val databaseBytes: Long)

/** What the cache holds, grouped as in Telegram's storage usage; [types] are what clearing deletes. */
enum class StorageCategory(val title: String, val types: () -> Array<TdApi.FileType>) {
    Photos("Фото", { arrayOf(TdApi.FileTypePhoto(), TdApi.FileTypeSelfDestructingPhoto()) }),
    Videos("Видео", { arrayOf(TdApi.FileTypeVideo(), TdApi.FileTypeSelfDestructingVideo(), TdApi.FileTypeLivePhotoVideo(), TdApi.FileTypeSelfDestructingLivePhotoVideo()) }),
    Files("Файлы", { arrayOf(TdApi.FileTypeDocument(), TdApi.FileTypeSecure()) }),
    Music("Музыка", { arrayOf(TdApi.FileTypeAudio(), TdApi.FileTypeNotificationSound()) }),
    Voice("Голосовые и кружки", { arrayOf(TdApi.FileTypeVoiceNote(), TdApi.FileTypeVideoNote(), TdApi.FileTypeSelfDestructingVoiceNote(), TdApi.FileTypeSelfDestructingVideoNote()) }),
    Stories("Истории", { arrayOf(TdApi.FileTypePhotoStory(), TdApi.FileTypeVideoStory()) }),
    Stickers("Стикеры и GIF", { arrayOf(TdApi.FileTypeSticker(), TdApi.FileTypeAnimation()) }),
    Other("Прочее", { arrayOf(TdApi.FileTypeThumbnail(), TdApi.FileTypeProfilePhoto(), TdApi.FileTypeWallpaper(), TdApi.FileTypeSecret(), TdApi.FileTypeSecretThumbnail(), TdApi.FileTypeUnknown(), TdApi.FileTypeNone()) });

    companion object {
        fun of(type: TdApi.FileType): StorageCategory =
            entries.firstOrNull { category -> category.types().any { it.constructor == type.constructor } } ?: Other
    }
}

/** Cached bytes of one chat; [chatId] 0 is files that belong to no chat (avatars, wallpapers). */
data class ChatStorage(val chatId: Long, val title: String, val bytes: Long)

/** Full breakdown of the cache by type and by the chats that take the most. */
data class StorageBreakdown(val filesBytes: Long, val databaseBytes: Long, val byCategory: Map<StorageCategory, Long>, val chats: List<ChatStorage>)

/** Sent and received bytes per network since [sinceDate] (unix seconds). */
data class NetworkUsage(val sinceDate: Int, val bytes: Map<DownloadNetwork, Pair<Long, Long>>)

/** Account-level settings stored in Telegram: notifications, privacy, local storage. */
class TelegramSettingsRepository(private val client: TelegramClient) {
    /** TDLib has no getter for reaction notifications: they arrive in an update after sign-in. */
    @Volatile private var reactionSettings: TdApi.ReactionNotificationSettings? = null

    init {
        client.addUpdateHandler { update -> if (update is TdApi.UpdateReactionNotificationSettings) reactionSettings = update.notificationSettings }
    }

    fun reactionNotifications(): ReactionNotifications? = reactionSettings?.let {
        ReactionNotifications(it.messageReactionSource.toSource(), it.storyReactionSource.toSource(), it.showPreview)
    }

    /** Keeps poll votes and the sound as they are; only what kiroX shows changes. */
    suspend fun setReactionNotifications(value: ReactionNotifications) {
        val current = reactionSettings
        val next = TdApi.ReactionNotificationSettings(
            value.messages.toTd(), value.stories.toTd(), current?.pollVoteSource ?: TdApi.ReactionNotificationSourceAll(), current?.soundId ?: 0, value.showPreview
        )
        client.query(TdApi.SetReactionNotificationSettings(next))
        reactionSettings = next
    }

    suspend fun notifications(scope: NotifyScope): ScopeNotifications {
        val settings = client.query(TdApi.GetScopeNotificationSettings(scope.toTd()))
        val exceptions = runCatching { client.query(TdApi.GetChatNotificationSettingsExceptions(scope.toTd(), false)).chatIds.size }.getOrDefault(0)
        return ScopeNotifications(
            settings.muteFor == 0, settings.showPreview, !settings.disablePinnedMessageNotifications, !settings.disableMentionNotifications, exceptions,
            stories = settings.useDefaultMuteStories || !settings.muteStories
        )
    }

    suspend fun setNotifications(scope: NotifyScope, value: ScopeNotifications) {
        val current = client.query(TdApi.GetScopeNotificationSettings(scope.toTd()))
        current.muteFor = if (value.enabled) 0 else MUTE_FOREVER_S
        current.showPreview = value.showPreview
        current.disablePinnedMessageNotifications = !value.pinned
        current.disableMentionNotifications = !value.mentions
        if (value.stories != (current.useDefaultMuteStories || !current.muteStories)) {
            current.useDefaultMuteStories = false
            current.muteStories = !value.stories
        }
        client.query(TdApi.SetScopeNotificationSettings(scope.toTd(), current))
    }

    /** «Контакт присоединился к Telegram»: a message when someone from the contacts signs up. */
    suspend fun contactJoinedNotifications(): Boolean =
        (client.query(TdApi.GetOption("disable_contact_registered_notifications")) as? TdApi.OptionValueBoolean)?.value != true

    suspend fun setContactJoinedNotifications(enabled: Boolean) {
        client.query(TdApi.SetOption("disable_contact_registered_notifications", TdApi.OptionValueBoolean(!enabled)))
    }

    /** Back to Telegram's defaults for every chat type and chat; exceptions are removed. */
    suspend fun resetAllNotifications() { client.query(TdApi.ResetAllNotificationSettings()) }

    suspend fun privacy(item: PrivacyItem): PrivacyState {
        val rules = client.query(TdApi.GetUserPrivacySettingRules(item.toTd())).rules
        val level = when {
            rules.any { it is TdApi.UserPrivacySettingRuleAllowAll } -> PrivacyLevel.Everybody
            rules.any { it is TdApi.UserPrivacySettingRuleAllowContacts } -> PrivacyLevel.Contacts
            else -> PrivacyLevel.Nobody
        }
        val exceptions = rules.sumOf { rule ->
            when (rule) {
                is TdApi.UserPrivacySettingRuleAllowUsers -> rule.userIds.size
                is TdApi.UserPrivacySettingRuleRestrictUsers -> rule.userIds.size
                is TdApi.UserPrivacySettingRuleAllowChatMembers -> rule.chatIds.size
                is TdApi.UserPrivacySettingRuleRestrictChatMembers -> rule.chatIds.size
                else -> 0
            }
        }
        return PrivacyState(level, exceptions)
    }

    suspend fun messagePrivacy(): MessagePrivacy = MessagePrivacy(
        runCatching { client.query(TdApi.GetReadDatePrivacySettings()).showReadDate }.getOrNull(),
        runCatching { client.query(TdApi.GetNewChatPrivacySettings()).allowNewChatsFromUnknownUsers }.getOrNull(),
        runCatching { client.query(TdApi.GetArchiveChatListSettings()).archiveAndMuteNewChatsFromUnknownUsers }.getOrNull(),
        runCatching { (client.query(TdApi.GetOption("disable_top_chats")) as? TdApi.OptionValueBoolean)?.value != true }.getOrNull()
    )

    /** «Время прочтения»: off hides when you read messages, and you no longer see it for others. */
    suspend fun setShowReadDate(value: Boolean) { client.query(TdApi.SetReadDatePrivacySettings(TdApi.ReadDatePrivacySettings(value))) }

    /** Off: only contacts and people you wrote to can start a chat (Telegram Premium). Paid messages stay as they are. */
    suspend fun setNewChatsFromStrangers(value: Boolean) {
        val current = client.query(TdApi.GetNewChatPrivacySettings())
        current.allowNewChatsFromUnknownUsers = value
        client.query(TdApi.SetNewChatPrivacySettings(current))
    }

    /** New chats from people outside the contacts go to the archive without sound. */
    suspend fun setArchiveNewChats(value: Boolean) {
        val current = client.query(TdApi.GetArchiveChatListSettings())
        current.archiveAndMuteNewChatsFromUnknownUsers = value
        client.query(TdApi.SetArchiveChatListSettings(current))
    }

    /** Frequent contacts suggested in search and when sharing. */
    suspend fun setSuggestFrequent(value: Boolean) { client.query(TdApi.SetOption("disable_top_chats", TdApi.OptionValueBoolean(!value))) }

    suspend fun accountTtl(): AccountTtl = AccountTtl.of(client.query(TdApi.GetAccountTtl()).days)

    suspend fun setAccountTtl(value: AccountTtl) { client.query(TdApi.SetAccountTtl(TdApi.AccountTtl(value.days))) }

    /** Removes the contacts synced from phone books; your Telegram contacts list stays. */
    suspend fun clearImportedContacts() { client.query(TdApi.ClearImportedContacts()) }

    suspend fun blockedCount(): Int = client.query(TdApi.GetBlockedMessageSenders(TdApi.BlockListMain(), 0, 1)).totalCount

    /** Replaces the base rule and keeps exceptions for specific users and chats. */
    suspend fun setPrivacy(item: PrivacyItem, level: PrivacyLevel) {
        val exceptions = client.query(TdApi.GetUserPrivacySettingRules(item.toTd())).rules.filter {
            it is TdApi.UserPrivacySettingRuleAllowUsers || it is TdApi.UserPrivacySettingRuleRestrictUsers ||
                it is TdApi.UserPrivacySettingRuleAllowChatMembers || it is TdApi.UserPrivacySettingRuleRestrictChatMembers
        }
        val base: List<TdApi.UserPrivacySettingRule> = when (level) {
            PrivacyLevel.Everybody -> listOf(TdApi.UserPrivacySettingRuleAllowAll())
            PrivacyLevel.Contacts -> listOf(TdApi.UserPrivacySettingRuleAllowContacts(), TdApi.UserPrivacySettingRuleRestrictAll())
            PrivacyLevel.Nobody -> listOf(TdApi.UserPrivacySettingRuleRestrictAll())
        }
        // Exceptions go first: Telegram applies the first matching rule.
        client.query(TdApi.SetUserPrivacySettingRules(item.toTd(), TdApi.UserPrivacySettingRules((exceptions + base).toTypedArray())))
    }

    suspend fun storage(): StorageUsage =
        client.query(TdApi.GetStorageStatisticsFast()).let { StorageUsage(it.filesSize, it.fileCount, it.databaseSize) }

    /** Cache by type and the [chatLimit] chats that take the most; TDLib scans the files, so it takes a moment. */
    suspend fun storageBreakdown(chatLimit: Int = 20): StorageBreakdown {
        val stats = client.query(TdApi.GetStorageStatistics(chatLimit))
        val database = runCatching { client.query(TdApi.GetStorageStatisticsFast()).databaseSize }.getOrDefault(0)
        val byCategory = HashMap<StorageCategory, Long>()
        stats.byChat.forEach { chat -> chat.byFileType.forEach { byCategory.merge(StorageCategory.of(it.fileType), it.size, Long::plus) } }
        val chats = stats.byChat.filter { it.size > 0 }.sortedByDescending { it.size }.map { chat ->
            val title = if (chat.chatId == 0L) "Прочие файлы" else runCatching { client.query(TdApi.GetChat(chat.chatId)).title }.getOrNull()?.ifBlank { null } ?: "Чат"
            ChatStorage(chat.chatId, title, chat.size)
        }
        return StorageBreakdown(stats.size, database, byCategory, chats)
    }

    /** Deletes cached files of [categories], only in [chatId] when given; messages stay in the cloud. */
    suspend fun clearStorage(categories: Set<StorageCategory>, chatId: Long? = null) {
        if (categories.isEmpty()) return
        val types = categories.flatMap { it.types().toList() }.toTypedArray()
        client.query(TdApi.OptimizeStorage(0, 0, 0, 0, types, chatId?.let { longArrayOf(it) }, null, false, 0))
    }

    /** Deletes all cached media and files; messages stay in the cloud and download again when opened. */
    suspend fun clearCache() {
        client.query(TdApi.OptimizeStorage(0, 0, 0, 0, null, null, null, false, 0))
    }

    /** Traffic since the last reset, summed per network (files, messages and calls). */
    suspend fun networkUsage(): NetworkUsage {
        val stats = client.query(TdApi.GetNetworkStatistics(false))
        val totals = HashMap<DownloadNetwork, Pair<Long, Long>>()
        stats.entries.forEach { entry ->
            val (type, sent, received) = when (entry) {
                is TdApi.NetworkStatisticsEntryFile -> Triple(entry.networkType, entry.sentBytes, entry.receivedBytes)
                is TdApi.NetworkStatisticsEntryCall -> Triple(entry.networkType, entry.sentBytes, entry.receivedBytes)
                else -> return@forEach
            }
            val network = when (type) {
                is TdApi.NetworkTypeMobile -> DownloadNetwork.Mobile
                is TdApi.NetworkTypeMobileRoaming -> DownloadNetwork.Roaming
                is TdApi.NetworkTypeWiFi, is TdApi.NetworkTypeOther -> DownloadNetwork.WiFi
                else -> return@forEach
            }
            val old = totals[network] ?: (0L to 0L)
            totals[network] = (old.first + sent) to (old.second + received)
        }
        return NetworkUsage(stats.sinceDate, totals)
    }

    suspend fun resetNetworkUsage() { client.query(TdApi.ResetNetworkStatistics()) }

    private companion object { const val MUTE_FOREVER_S = 400 * 24 * 60 * 60 }

    // ---------- Blocked users ----------

    suspend fun blocked(): List<BlockedSender> {
        val senders = client.query(TdApi.GetBlockedMessageSenders(TdApi.BlockListMain(), 0, 100)).senders
        return senders.mapNotNull { sender ->
            runCatching {
                when (sender) {
                    is TdApi.MessageSenderUser -> client.query(TdApi.GetUser(sender.userId)).let { u ->
                        BlockedSender(u.id, false, listOf(u.firstName, u.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Удалённый аккаунт" }, u.profilePhoto?.small?.toRef())
                    }
                    is TdApi.MessageSenderChat -> client.query(TdApi.GetChat(sender.chatId)).let { c -> BlockedSender(c.id, true, c.title, c.photo?.small?.toRef()) }
                    else -> null
                }
            }.getOrNull()
        }
    }

    suspend fun unblock(sender: BlockedSender) {
        val id: TdApi.MessageSender = if (sender.isChat) TdApi.MessageSenderChat(sender.id) else TdApi.MessageSenderUser(sender.id)
        client.query(TdApi.SetMessageSenderBlockList(id, null))
    }

    // ---------- Cloud password ----------

    /**
     * Checks the cloud password: TDLib has no direct check, but GetRecoveryEmailAddress needs the
     * password and fails with PASSWORD_HASH_INVALID when it is wrong (throws TdException then).
     */
    suspend fun verifyPassword(password: String) { client.query(TdApi.GetRecoveryEmailAddress(password)) }

    suspend fun passwordState(): CloudPasswordState = client.query(TdApi.GetPasswordState()).toState()

    /**
     * Sets, changes or (with an empty [newPassword]) removes the cloud password. [oldPassword] is
     * needed when one is set. A new [recoveryEmail] waits for a code, see [confirmRecoveryEmail].
     */
    suspend fun setPassword(oldPassword: String, newPassword: String, hint: String, recoveryEmail: String?): CloudPasswordState =
        client.query(TdApi.SetPassword(oldPassword, newPassword, hint, recoveryEmail != null, recoveryEmail.orEmpty())).toState()

    suspend fun confirmRecoveryEmail(code: String): CloudPasswordState = client.query(TdApi.CheckRecoveryEmailAddressCode(code)).toState()

    private fun TdApi.PasswordState.toState() = CloudPasswordState(
        hasPassword, passwordHint.orEmpty(), hasRecoveryEmailAddress, recoveryEmailAddressCodeInfo?.emailAddressPattern, pendingResetDate
    )

    /** New recovery e-mail for an existing password; the address waits for its code ([confirmRecoveryEmail]). */
    suspend fun setRecoveryEmail(password: String, email: String): CloudPasswordState =
        client.query(TdApi.SetRecoveryEmailAddress(password, email)).toState()

    suspend fun resendRecoveryEmailCode(): CloudPasswordState = client.query(TdApi.ResendRecoveryEmailAddressCode()).toState()

    /** «Забыли пароль?» while signed in: a code goes to the recovery e-mail; returns its pattern («a•••@mail.com»). */
    suspend fun requestPasswordRecovery(): String = client.query(TdApi.RequestPasswordRecovery()).emailAddressPattern.orEmpty()

    /** The e-mail code; [newPassword] empty removes the password. */
    suspend fun recoverPassword(code: String, newPassword: String, newHint: String): CloudPasswordState =
        client.query(TdApi.RecoverPassword(code, newPassword, newHint)).toState()

    /** Reset without the e-mail: Telegram removes the password after a waiting period (or refuses for now). */
    suspend fun resetPassword(): PasswordResetResult = when (val r = client.query(TdApi.ResetPassword())) {
        is TdApi.ResetPasswordResultPending -> PasswordResetResult.Pending(r.pendingResetDate)
        is TdApi.ResetPasswordResultDeclined -> PasswordResetResult.Declined(r.retryDate)
        else -> PasswordResetResult.Done
    }

    suspend fun cancelPasswordReset() { client.query(TdApi.CancelPasswordReset()) }

    // ---------- Privacy exceptions ----------

    private suspend fun person(userId: Long): PrivacyPerson? = runCatching {
        client.query(TdApi.GetUser(userId)).let { u ->
            PrivacyPerson(u.id, listOf(u.firstName, u.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Удалённый аккаунт" }, u.profilePhoto?.small?.toRef())
        }
    }.getOrNull()

    suspend fun privacyExceptions(item: PrivacyItem): PrivacyExceptions {
        val rules = client.query(TdApi.GetUserPrivacySettingRules(item.toTd())).rules
        val allow = rules.filterIsInstance<TdApi.UserPrivacySettingRuleAllowUsers>().flatMap { it.userIds.toList() }.distinct()
        val never = rules.filterIsInstance<TdApi.UserPrivacySettingRuleRestrictUsers>().flatMap { it.userIds.toList() }.distinct()
        val chats = rules.sumOf { r ->
            when (r) {
                is TdApi.UserPrivacySettingRuleAllowChatMembers -> r.chatIds.size
                is TdApi.UserPrivacySettingRuleRestrictChatMembers -> r.chatIds.size
                else -> 0
            }
        }
        return PrivacyExceptions(allow.mapNotNull { person(it) }, never.mapNotNull { person(it) }, chats)
    }

    /** Replaces the people exceptions of [item]; the base rule and group-member exceptions stay. */
    suspend fun setPrivacyExceptions(item: PrivacyItem, allow: List<Long>, never: List<Long>) {
        val rules = client.query(TdApi.GetUserPrivacySettingRules(item.toTd())).rules
        val chatRules = rules.filter { it is TdApi.UserPrivacySettingRuleAllowChatMembers || it is TdApi.UserPrivacySettingRuleRestrictChatMembers }
        val base = rules.filter {
            it is TdApi.UserPrivacySettingRuleAllowAll || it is TdApi.UserPrivacySettingRuleAllowContacts || it is TdApi.UserPrivacySettingRuleRestrictAll ||
                it is TdApi.UserPrivacySettingRuleRestrictContacts || it is TdApi.UserPrivacySettingRuleAllowBots || it is TdApi.UserPrivacySettingRuleRestrictBots ||
                it is TdApi.UserPrivacySettingRuleAllowPremiumUsers
        }
        val people = buildList<TdApi.UserPrivacySettingRule> {
            val n = never.distinct(); val a = allow.distinct().filter { it !in n }
            if (a.isNotEmpty()) add(TdApi.UserPrivacySettingRuleAllowUsers(a.toLongArray()))
            if (n.isNotEmpty()) add(TdApi.UserPrivacySettingRuleRestrictUsers(n.toLongArray()))
        }
        // Exceptions go first: Telegram applies the first matching rule.
        client.query(TdApi.SetUserPrivacySettingRules(item.toTd(), TdApi.UserPrivacySettingRules((people + chatRules + base).toTypedArray())))
    }

    // ---------- Notification exceptions ----------

    suspend fun notificationExceptions(): List<NotificationException> {
        val ids = client.query(TdApi.GetChatNotificationSettingsExceptions(null, false)).chatIds
        return ids.toList().mapNotNull { id ->
            runCatching { client.query(TdApi.GetChat(id)) }.getOrNull()?.let { chat ->
                NotificationException(chat.id, chat.title, chat.notificationSettings.muteFor > 0)
            }
        }
    }

    // ---------- Proxy ----------

    suspend fun proxies(): List<TelegramProxy> = client.query(TdApi.GetProxies()).proxies.map { added ->
        TelegramProxy(
            added.id, added.proxy.server, added.proxy.port,
            when (added.proxy.type) { is TdApi.ProxyTypeHttp -> ProxyKind.Http; is TdApi.ProxyTypeMtproto -> ProxyKind.MtProto; else -> ProxyKind.Socks5 },
            added.isEnabled
        )
    }

    /** Adds and enables a proxy; [secret] is the MTProto secret or "user:password" for SOCKS5/HTTP. */
    suspend fun addProxy(server: String, port: Int, kind: ProxyKind, secret: String) {
        val user = secret.substringBefore(':', secret)
        val pass = secret.substringAfter(':', "")
        val type: TdApi.ProxyType = when (kind) {
            ProxyKind.Socks5 -> TdApi.ProxyTypeSocks5(user, pass)
            ProxyKind.Http -> TdApi.ProxyTypeHttp(user, pass, false)
            ProxyKind.MtProto -> TdApi.ProxyTypeMtproto(secret)
        }
        client.query(TdApi.AddProxy(TdApi.Proxy(server, port, type), true, ""))
    }

    suspend fun enableProxy(id: Int) { client.query(TdApi.EnableProxy(id)) }
    suspend fun disableProxy() { client.query(TdApi.DisableProxy()) }
    suspend fun removeProxy(id: Int) { client.query(TdApi.RemoveProxy(id)) }

}
