package com.kirox.app.telegram

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

/**
 * Chat lists (main, archive, folders) built from TDLib updates. All state is touched only on the
 * TDLib dispatcher; bursts of updates are coalesced into one publish per frame-ish window.
 */
class TelegramChatRepository(private val client: TelegramClient) {
    private val chatsById = HashMap<Long, TdApi.Chat>()
    private var folders: List<TelegramFolder> = emptyList()
    private val loading = HashSet<TelegramChatListKey>()
    private val complete = HashSet<TelegramChatListKey>()
    private val wanted = HashSet<TelegramChatListKey>()
    private val failures = HashMap<TelegramChatListKey, Int>()
    private var publishScheduled = false
    private var myId = 0L
    /** Users seen in updates, for list facts of private chats (online, bot, verified) and group senders' names. */
    private val peerUsers = HashMap<Long, TdApi.User>()
    private val verifiedSupergroups = HashSet<Long>()
    /** Scam / fake marks of supergroups and channels, from their verification status. */
    private val scamSupergroups = HashSet<Long>()
    private val fakeSupergroups = HashSet<Long>()
    /** Mute-for seconds of the private / group / channel scope defaults, for chats on "use default". */
    private val scopeMuteFor = IntArray(3)

    private val _onlineMembers = MutableStateFlow<Map<Long, Int>>(emptyMap())
    /** Members online right now, per chat (UpdateChatOnlineMemberCount comes only for opened chats). */
    val onlineMembers: StateFlow<Map<Long, Int>> = _onlineMembers.asStateFlow()

    private val _folderUnread = MutableStateFlow<Map<Int, Int>>(emptyMap())
    /** Unread unmuted chats per folder, as Telegram counts them (UpdateUnreadChatCount of ChatListFolder). */
    val folderUnread: StateFlow<Map<Int, Int>> = _folderUnread.asStateFlow()

    private val _unreadUnmutedChats = MutableStateFlow(0)
    /** Chats-tab badge as Telegram counts it: unread unmuted chats of the main list (UpdateUnreadChatCount). */
    val unreadUnmutedChats: StateFlow<Int> = _unreadUnmutedChats.asStateFlow()

    private val _lists = MutableStateFlow(TelegramChatLists())
    val lists: StateFlow<TelegramChatLists> = _lists.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    /** Last failed chat action, for a toast; cleared by [clearError]. */
    val error: StateFlow<String?> = _error.asStateFlow()

    init { client.addUpdateHandler(::handle) }

    /** Dispatcher-only lookup for other repositories. */
    internal fun chat(id: Long): TdApi.Chat? = chatsById[id]

    /** Id of the private chat with [userId], creating it locally if needed. */
    suspend fun privateChatId(userId: Long): Long = client.query(TdApi.CreatePrivateChat(userId, false)).id

    fun setArchived(chatId: Long, archived: Boolean) =
        act(TdApi.AddChatToList(chatId, if (archived) TdApi.ChatListArchive() else TdApi.ChatListMain()))

    fun setPinned(key: TelegramChatListKey, chatId: Long, pinned: Boolean) = act(TdApi.ToggleChatIsPinned(key.toTd(), chatId, pinned))

    fun markUnread(chatId: Long) = act(TdApi.ToggleChatIsMarkedAsUnread(chatId, true))

    /** End-to-end encrypted chat with [userId]; works only between this device and the peer's device. */
    suspend fun createSecretChat(userId: Long): Long = client.query(TdApi.CreateNewSecretChat(userId)).id

    /** New folder with the given chats; returns its id. */
    /** [icon]: TDLib ChatFolderIcon name («Work», «Love»…), empty lets Telegram choose. */
    suspend fun createFolder(name: String, chatIds: List<Long>, icon: String = ""): Int {
        val folder = TdApi.ChatFolder(
            TdApi.ChatFolderName(TdApi.FormattedText(name, emptyArray()), false), icon.takeIf { it.isNotBlank() }?.let { TdApi.ChatFolderIcon(it) }, -1, false,
            LongArray(0), chatIds.toLongArray(), LongArray(0), false, false, false, false, false, false, false, false
        )
        return client.query(TdApi.CreateChatFolder(folder)).id
    }

    /** Removes the folder only; its chats stay in the main list. */
    /**
     * «Поделиться папкой»: a t.me/addlist link with the folder's groups and channels (Telegram
     * shares no private chats). Null when the folder has none.
     */
    suspend fun shareFolder(id: Int): String? {
        val ids = client.query(TdApi.GetChatFolder(id)).includedChatIds.filter { chatId ->
            val type = runCatching { client.query(TdApi.GetChat(chatId)).type }.getOrNull()
            type is TdApi.ChatTypeSupergroup || type is TdApi.ChatTypeBasicGroup
        }
        if (ids.isEmpty()) return null
        return client.query(TdApi.CreateChatFolderInviteLink(id, "", ids.toLongArray())).inviteLink
    }

    suspend fun checkFolderLink(link: String): FolderInvite? = runCatching {
        val info = client.query(TdApi.CheckChatFolderInviteLink(link))
        val chats = info.missingChatIds.map { id -> id to (runCatching { client.query(TdApi.GetChat(id)).title }.getOrNull() ?: "Чат") }
        FolderInvite(link, info.chatFolderInfo.name.text.text, chats, info.addedChatIds.size)
    }.getOrNull()

    suspend fun addFolderByLink(link: String, chatIds: List<Long>) { client.query(TdApi.AddChatFolderByInviteLink(link, chatIds.toLongArray())) }

    /**
     * The chat as plain text, oldest first: «[31.10.2026 18:30] Имя: текст». Media become their
     * label («Фото», «Видео»). Up to [limit] messages; [onProgress] gets the count so far.
     */
    suspend fun exportText(chatId: Long, limit: Int = 20_000, onProgress: (Int) -> Unit = {}): String {
        val names = HashMap<Long, String>()
        suspend fun sender(message: TdApi.Message): String = when (val s = message.senderId) {
            is TdApi.MessageSenderUser -> names.getOrPut(s.userId) { runCatching { client.query(TdApi.GetUser(s.userId)).displayName() }.getOrDefault("") }
            is TdApi.MessageSenderChat -> names.getOrPut(-s.chatId) { runCatching { client.query(TdApi.GetChat(s.chatId)).title }.getOrDefault("") }
            else -> ""
        }
        val all = ArrayList<TdApi.Message>()
        var from = 0L
        while (all.size < limit) {
            val page = client.query(TdApi.GetChatHistory(chatId, from, 0, 100, false)).messages
            if (page.isEmpty()) break
            all += page
            from = page.last().id
            onProgress(all.size)
        }
        val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        val title = runCatching { client.query(TdApi.GetChat(chatId)).title }.getOrDefault("")
        return buildString {
            appendLine("kiroX — экспорт переписки «$title»")
            appendLine("Сообщений: ${all.size}")
            appendLine()
            all.asReversed().forEach { message ->
                append('[').append(format.format(java.util.Date(message.date * 1000L))).append("] ")
                sender(message).takeIf { it.isNotBlank() }?.let { append(it).append(": ") }
                appendLine(messagePreview(message))
            }
        }
    }

    /** New order of the folders; the main list keeps its place (0 = first). */
    suspend fun reorderFolders(ids: List<Int>, mainListPosition: Int = 0) {
        client.query(TdApi.ReorderChatFolders(ids.toIntArray(), mainListPosition))
    }

    suspend fun deleteFolder(id: Int) { client.query(TdApi.DeleteChatFolder(id, LongArray(0))) }

    /** Chats added to folder [id] by hand (not the ones it takes by type). */
    suspend fun folderChatIds(id: Int): List<Long> = client.query(TdApi.GetChatFolder(id)).includedChatIds.toList()

    /** Renames folder [id] and sets its hand-picked chats; type rules and pinned chats stay as they are. */
    suspend fun editFolder(id: Int, name: String, chatIds: List<Long>) {
        val folder = client.query(TdApi.GetChatFolder(id))
        folder.name = TdApi.ChatFolderName(TdApi.FormattedText(name, emptyArray()), false)
        folder.includedChatIds = chatIds.toLongArray()
        folder.pinnedChatIds = folder.pinnedChatIds.filter { it in chatIds }.toLongArray()
        client.query(TdApi.EditChatFolder(id, folder))
    }

    /** New basic group with [userIds]; returns its chat id. */
    suspend fun createGroup(title: String, userIds: List<Long>, description: String = ""): Long {
        val id = client.query(TdApi.CreateNewBasicGroupChat(userIds.toLongArray(), title, 0)).chatId
        if (description.isNotBlank()) runCatching { client.query(TdApi.SetChatDescription(id, description)) }
        return id
    }

    /** Why [username] cannot be a new public link, or null when it is free. */
    suspend fun publicLinkProblem(username: String): String? =
        when (runCatching { client.query(TdApi.CheckChatUsername(0, username)) }.getOrNull()) {
            is TdApi.CheckChatUsernameResultOk -> null
            is TdApi.CheckChatUsernameResultUsernameInvalid -> "Ссылка может содержать латиницу, цифры и «_», от 5 символов"
            is TdApi.CheckChatUsernameResultUsernameOccupied, is TdApi.CheckChatUsernameResultUsernamePurchasable -> "Эта ссылка уже занята"
            is TdApi.CheckChatUsernameResultPublicChatsTooMany -> "Слишком много публичных каналов и групп"
            else -> "Не удалось проверить ссылку"
        }

    /** New broadcast channel, public under t.me/[username] when it is given; returns its chat id. */
    suspend fun createChannel(title: String, description: String, username: String? = null): Long {
        val chat = client.query(TdApi.CreateNewSupergroupChat(title, false, true, description, null, 0, false))
        val supergroupId = (chat.type as? TdApi.ChatTypeSupergroup)?.supergroupId
        if (!username.isNullOrBlank() && supergroupId != null) client.query(TdApi.SetSupergroupUsername(supergroupId, username))
        return chat.id
    }

    suspend fun isMuted(chatId: Long): Boolean =
        client.query(TdApi.GetChat(chatId)).notificationSettings.let { !it.useDefaultMuteFor && it.muteFor > 0 }

    /** Mutes "forever" (Telegram treats a year and more as forever) or unmutes the chat. */
    suspend fun setMuted(chatId: Long, muted: Boolean) {
        val settings = client.query(TdApi.GetChat(chatId)).notificationSettings
        settings.useDefaultMuteFor = false
        settings.muteFor = if (muted) MUTE_FOREVER_S else 0
        client.query(TdApi.SetChatNotificationSettings(chatId, settings))
    }

    /** A channel or group: whether this account is in it, and whether it may post (channel admins). */
    suspend fun membership(chatId: Long): Pair<Boolean, Boolean> {
        val type = client.query(TdApi.GetChat(chatId)).type as? TdApi.ChatTypeSupergroup ?: return true to true
        return when (val status = client.query(TdApi.GetSupergroup(type.supergroupId)).status) {
            is TdApi.ChatMemberStatusLeft, is TdApi.ChatMemberStatusBanned -> false to false
            is TdApi.ChatMemberStatusCreator -> status.isMember to status.isMember
            is TdApi.ChatMemberStatusAdministrator -> true to (!type.isChannel || status.rights.canPostMessages)
            is TdApi.ChatMemberStatusRestricted -> status.isMember to false
            else -> true to !type.isChannel
        }
    }

    /**
     * What the bottom of a chat may show (v3 UI), as Telegram decides it: the member status plus the
     * chat's default permissions (an admins-only group), media-only restrictions still allow text.
     */
    suspend fun writeAccess(chatId: Long): TelegramWriteAccess = runCatching {
        val chat = client.query(TdApi.GetChat(chatId))
        val defaults = chat.permissions?.canSendBasicMessages != false
        when (val type = chat.type) {
            is TdApi.ChatTypePrivate -> {
                val user = runCatching { client.query(TdApi.GetUser(type.userId)) }.getOrNull()
                when {
                    chat.blockList != null -> TelegramWriteAccess.Blocked
                    user?.type is TdApi.UserTypeBot && chat.lastMessage == null -> TelegramWriteAccess.StartBot
                    else -> TelegramWriteAccess.Write
                }
            }
            is TdApi.ChatTypeBasicGroup -> when (val s = client.query(TdApi.GetBasicGroup(type.basicGroupId)).status) {
                is TdApi.ChatMemberStatusCreator, is TdApi.ChatMemberStatusAdministrator -> TelegramWriteAccess.Write
                is TdApi.ChatMemberStatusRestricted -> if (s.permissions.canSendBasicMessages) TelegramWriteAccess.Write else TelegramWriteAccess.Restricted
                is TdApi.ChatMemberStatusLeft, is TdApi.ChatMemberStatusBanned -> TelegramWriteAccess.Restricted
                else -> if (defaults) TelegramWriteAccess.Write else TelegramWriteAccess.Restricted
            }
            is TdApi.ChatTypeSupergroup -> when (val s = client.query(TdApi.GetSupergroup(type.supergroupId)).status) {
                is TdApi.ChatMemberStatusCreator -> if (s.isMember) TelegramWriteAccess.Write else TelegramWriteAccess.Join
                is TdApi.ChatMemberStatusAdministrator -> if (!type.isChannel || s.rights.canPostMessages) TelegramWriteAccess.Write else TelegramWriteAccess.ReadOnly
                is TdApi.ChatMemberStatusRestricted -> when {
                    !s.isMember -> TelegramWriteAccess.Join
                    type.isChannel -> TelegramWriteAccess.ReadOnly
                    s.permissions.canSendBasicMessages -> TelegramWriteAccess.Write
                    else -> TelegramWriteAccess.Restricted
                }
                is TdApi.ChatMemberStatusLeft -> TelegramWriteAccess.Join
                is TdApi.ChatMemberStatusBanned -> TelegramWriteAccess.Restricted
                else -> if (type.isChannel) TelegramWriteAccess.ReadOnly else if (defaults) TelegramWriteAccess.Write else TelegramWriteAccess.Restricted
            }
            else -> TelegramWriteAccess.Write
        }
    }.getOrDefault(TelegramWriteAccess.Write)

    /** Subscribes to a channel or joins a public group. */
    suspend fun join(chatId: Long) { client.query(TdApi.JoinChat(chatId)) }

    /** A stranger wrote first: the chat's action bar offers «Заблокировать / Добавить в контакты» (or «Спам»). */
    suspend fun fromStranger(chatId: Long): Boolean = runCatching {
        val bar = client.query(TdApi.GetChat(chatId)).actionBar
        bar is TdApi.ChatActionBarReportAddBlock || bar is TdApi.ChatActionBarReportSpam
    }.getOrDefault(false)

    /** «Принять переписку»: hides the stranger bar for good. */
    suspend fun removeActionBar(chatId: Long) { client.query(TdApi.RemoveChatActionBar(chatId)) }

    /** What an invite link (t.me/+…, t.me/joinchat/…) leads to, before joining. */
    suspend fun inviteLinkInfo(link: String): InviteLinkInfo {
        val i = client.query(TdApi.CheckChatInviteLink(link))
        val names = i.memberUserIds.take(3).mapNotNull { id -> runCatching { client.query(TdApi.GetUser(id)).firstName }.getOrNull()?.takeIf { it.isNotBlank() } }
        return InviteLinkInfo(i.chatId, i.title, i.type is TdApi.InviteLinkChatTypeChannel, i.isPublic, i.memberCount, names, i.description,
            i.createsJoinRequest, i.photo?.small?.toRef(),
            // chatId is also set for public chats one can only read: a member is one that needs no «Вступить».
            member = i.chatId != 0L && i.accessibleFor == 0 && writeAccess(i.chatId) != TelegramWriteAccess.Join)
    }

    /** Joins by an invite link; true when only a join request was sent (the admins decide). */
    suspend fun joinByInviteLink(link: String): Boolean = try {
        client.query(TdApi.JoinChatByInviteLink(link)); false
    } catch (e: TdException) {
        if (e.error.message.contains("INVITE_REQUEST_SENT")) true else throw e
    }

    /** New messages disappear [seconds] after they are read (secret chats) or sent; 0 turns it off. */
    suspend fun setAutoDelete(chatId: Long, seconds: Int) { client.query(TdApi.SetChatMessageAutoDeleteTime(chatId, seconds)) }

    /** Deletes the chat history for this account only ([forEveryone]: for both sides, where Telegram allows); the chat stays in the list. */
    suspend fun clearHistory(chatId: Long, forEveryone: Boolean = false) { client.query(TdApi.DeleteChatHistory(chatId, false, forEveryone)) }

    /** Private chats: delete for this account and remove from the list. Groups and channels: leave. */
    suspend fun deleteChat(chatId: Long, forEveryone: Boolean = false) {
        val chat = client.query(TdApi.GetChat(chatId))
        when (val type = chat.type) {
            // forEveryone: «Также удалить для собеседника».
            is TdApi.ChatTypePrivate -> client.query(TdApi.DeleteChatHistory(chatId, true, forEveryone))
            // A secret chat is closed first, otherwise it stays open on the other device.
            is TdApi.ChatTypeSecret -> {
                runCatching { client.query(TdApi.CloseSecretChat(type.secretChatId)) }
                client.query(TdApi.DeleteChatHistory(chatId, true, false))
            }
            // A creator may delete the whole group/channel for all members.
            else -> if (forEveryone && chat.canBeDeletedForAllUsers) client.query(TdApi.DeleteChat(chatId)) else {
                client.query(TdApi.LeaveChat(chatId))
                // A basic group stays in the list after leaving until its history is removed.
                if (type is TdApi.ChatTypeBasicGroup) runCatching { client.query(TdApi.DeleteChatHistory(chatId, true, false)) }
            }
        }
    }

    fun markRead(chatId: Long) = client.post {
        val chat = chatsById[chatId] ?: return@post
        chat.lastMessage?.let { act(TdApi.ViewMessages(chatId, longArrayOf(it.id), TdApi.MessageSourceChatList(), true)) }
        if (chat.isMarkedAsUnread) act(TdApi.ToggleChatIsMarkedAsUnread(chatId, false))
    }

    /** fixer-final: opening a chat clears its manual «unread» mark, as Telegram does (only the mark: no read receipts). */
    fun clearMarkedUnread(chatId: Long) = client.post {
        if (chatsById[chatId]?.isMarkedAsUnread == true) act(TdApi.ToggleChatIsMarkedAsUnread(chatId, false))
    }

    fun clearError() { _error.value = null }

    /** «Прочитать» from the list as Telegram does it: the messages, and also the unread mentions and reactions. */
    fun markReadAll(chatId: Long) = client.post {
        markRead(chatId)
        val chat = chatsById[chatId] ?: return@post
        if (chat.unreadMentionCount > 0) act(TdApi.ReadAllChatMentions(chatId))
        if (chat.unreadReactionCount > 0) act(TdApi.ReadAllChatReactions(chatId))
    }

    /**
     * Members of a group or subscribers of a channel; 0 for private chats or when Telegram hides it.
     * Supergroup.memberCount is 0 until known, then the full info has it.
     */
    suspend fun memberCount(chatId: Long): Int = runCatching {
        when (val type = client.query(TdApi.GetChat(chatId)).type) {
            is TdApi.ChatTypeBasicGroup -> client.query(TdApi.GetBasicGroup(type.basicGroupId)).memberCount
            is TdApi.ChatTypeSupergroup -> client.query(TdApi.GetSupergroup(type.supergroupId)).memberCount.takeIf { it > 0 }
                ?: client.query(TdApi.GetSupergroupFullInfo(type.supergroupId)).memberCount
            else -> 0
        }
    }.getOrDefault(0)

    /** Supergroup ids that are forums (topics). Dispatcher-confined. */
    private val forumSupergroups = HashSet<Long>()

    /** Whether [chatId] is a forum supergroup; call on the TDLib dispatcher. */
    internal fun isForum(chatId: Long): Boolean = (chatsById[chatId]?.type as? TdApi.ChatTypeSupergroup)?.supergroupId?.let { it in forumSupergroups } == true

    /**
     * Chats matching [query]: the user's own chats first (title and username), then chats found on
     * the server and public channels, groups and bots by username. Each appears once.
     */
    suspend fun search(query: String): List<TelegramChatItem> {
        val local = runCatching { client.query(TdApi.SearchChats(query, null, SEARCH_LIMIT)).chatIds.toList() }.getOrDefault(emptyList())
        val server = runCatching { client.query(TdApi.SearchChatsOnServer(query, null, SEARCH_LIMIT)).chatIds.toList() }.getOrDefault(emptyList())
        // Public search needs a few letters; Telegram rejects very short queries.
        val public = if (query.trim().length >= 4) runCatching { client.query(TdApi.SearchPublicChats(query, null)).chatIds.toList() }.getOrDefault(emptyList()) else emptyList()
        return items((local + server + public).distinct())
    }

    /** Username, about text and phone shown on a chat's profile; empty strings when there are none. */
    suspend fun profileInfo(chatId: Long): TelegramProfileInfo {
        val type = onDispatcher { chatsById[chatId]?.type }
        return runCatching {
            when (type) {
                is TdApi.ChatTypePrivate, is TdApi.ChatTypeSecret -> {
                    val userId = (type as? TdApi.ChatTypePrivate)?.userId ?: (type as TdApi.ChatTypeSecret).userId
                    val user = client.query(TdApi.GetUser(userId))
                    val full = runCatching { client.query(TdApi.GetUserFullInfo(userId)) }.getOrNull()
                    TelegramProfileInfo(user.usernames?.activeUsernames?.firstOrNull().orEmpty(), full?.bio?.text.orEmpty(), user.phoneNumber.orEmpty(), user.emojiStatusId())
                }
                is TdApi.ChatTypeSupergroup -> {
                    val group = client.query(TdApi.GetSupergroup(type.supergroupId))
                    val full = runCatching { client.query(TdApi.GetSupergroupFullInfo(type.supergroupId)) }.getOrNull()
                    TelegramProfileInfo(group.usernames?.activeUsernames?.firstOrNull().orEmpty(), full?.description.orEmpty(), "")
                }
                is TdApi.ChatTypeBasicGroup -> TelegramProfileInfo("", runCatching { client.query(TdApi.GetBasicGroupFullInfo(type.basicGroupId)).description }.getOrNull().orEmpty(), "")
                else -> TelegramProfileInfo()
            }
        }.getOrDefault(TelegramProfileInfo())
    }

    /** Members of a group with their role; null for chats without a member list (users, channels you do not run). */
    suspend fun members(chatId: Long): List<TelegramMember>? {
        val type = onDispatcher { chatsById[chatId]?.type }
        val raw: List<TdApi.ChatMember> = runCatching {
            when (type) {
                is TdApi.ChatTypeBasicGroup -> client.query(TdApi.GetBasicGroupFullInfo(type.basicGroupId)).members.toList()
                is TdApi.ChatTypeSupergroup -> if (type.isChannel) return null else client.query(TdApi.GetSupergroupMembers(type.supergroupId, TdApi.SupergroupMembersFilterRecent(), 0, MEMBERS_LIMIT)).members.toList()
                else -> return null
            }
        }.getOrElse { return null }
        return raw.mapNotNull { member ->
            val userId = (member.memberId as? TdApi.MessageSenderUser)?.userId ?: return@mapNotNull null
            val user = runCatching { client.query(TdApi.GetUser(userId)) }.getOrNull() ?: return@mapNotNull null
            val role = when (member.status) {
                is TdApi.ChatMemberStatusCreator -> "Создатель"
                is TdApi.ChatMemberStatusAdministrator -> "Администратор"
                else -> ""
            }
            TelegramMember(userId, listOf(user.firstName, user.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Удалённый аккаунт" }, user.profilePhoto?.small?.toRef(), user.status.toStatus(), role)
        }.sortedWith(compareBy({ if (it.role.isBlank()) 1 else 0 }, { it.name }))
    }

    /** Newest photos/videos, files, voice notes or links of a chat, for the profile tabs. */
    /** GIFs of a chat (v3 «GIF» tab); a separate call so SharedMediaKind — the old profile's tabs — stays as it is. */
    suspend fun sharedGifs(chatId: Long): List<TelegramSharedItem> = sharedMedia(chatId, SharedMediaKind.Media, TdApi.SearchMessagesFilterAnimation())

    suspend fun sharedMedia(chatId: Long, kind: SharedMediaKind, filterOverride: TdApi.SearchMessagesFilter? = null): List<TelegramSharedItem> {
        val filter: TdApi.SearchMessagesFilter = filterOverride ?: when (kind) {
            SharedMediaKind.Media -> TdApi.SearchMessagesFilterPhotoAndVideo()
            SharedMediaKind.Files -> TdApi.SearchMessagesFilterDocument()
            SharedMediaKind.Music -> TdApi.SearchMessagesFilterAudio()
            SharedMediaKind.Voice -> TdApi.SearchMessagesFilterVoiceNote()
            SharedMediaKind.Links -> TdApi.SearchMessagesFilterUrl()
        }
        val found = runCatching { client.query(TdApi.SearchChatMessages(chatId, null, "", null, 0, 0, SHARED_LIMIT, filter)) }.getOrNull() ?: return emptyList()
        return found.messages.filterNotNull().map { message ->
            when (val content = message.content) {
                is TdApi.MessagePhoto -> TelegramSharedItem(message.id, message.date, thumb = content.photo.sizes.minByOrNull { kotlin.math.abs(it.width - 320) }?.photo?.toRef(), full = content.photo.sizes.maxByOrNull { it.width }?.photo?.toRef())
                is TdApi.MessageVideo -> TelegramSharedItem(message.id, message.date, thumb = content.video.thumbnail?.file?.toRef(), subtitle = durationText(content.video.duration), isVideo = true)
                is TdApi.MessageAnimation -> TelegramSharedItem(message.id, message.date, thumb = content.animation.thumbnail?.file?.toRef(), subtitle = durationText(content.animation.duration),
                    isVideo = true, file = content.animation.animation.toRef(), title = content.animation.fileName.orEmpty())
                is TdApi.MessageDocument -> content.document.let { d ->
                    TelegramSharedItem(message.id, message.date, thumb = d.thumbnail?.file?.toRef(), title = d.fileName.ifBlank { "Файл" }, subtitle = sizeText(d.document.size),
                        file = d.document.toRef(), size = d.document.size.takeIf { it > 0 } ?: d.document.expectedSize, mimeType = d.mimeType.orEmpty())
                }
                is TdApi.MessageAudio -> content.audio.let { a ->
                    TelegramSharedItem(message.id, message.date, thumb = a.albumCoverThumbnail?.file?.toRef(), title = a.title.ifBlank { a.fileName.substringBeforeLast('.') }.ifBlank { "Аудио" },
                        subtitle = listOf(a.performer, durationText(a.duration)).filter { it.isNotBlank() }.joinToString(" · "),
                        file = a.audio.toRef(), size = a.audio.size, mimeType = a.mimeType.orEmpty(), performer = a.performer.orEmpty(), duration = a.duration)
                }
                is TdApi.MessageVoiceNote -> TelegramSharedItem(message.id, message.date, title = "Голосовое сообщение", subtitle = durationText(content.voiceNote.duration), file = content.voiceNote.voice.toRef(), duration = content.voiceNote.duration)
                is TdApi.MessageText -> TelegramSharedItem(message.id, message.date, title = Regex("https?://\\S+|t\\.me/\\S+|[\\w.-]+\\.[a-z]{2,}/\\S*").find(content.text.text)?.value ?: content.text.text.take(80), subtitle = content.text.text.take(120))
                else -> TelegramSharedItem(message.id, message.date, title = messagePreview(message))
            }
        }
    }

    private fun durationText(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)
    private fun sizeText(bytes: Long) = when { bytes >= 1_048_576 -> "%.1f МБ".format(bytes / 1_048_576.0); bytes >= 1024 -> "${bytes / 1024} КБ"; else -> "$bytes Б" }

    /** Messages matching [query] in all chats of the main list, newest first. */
    suspend fun searchMessages(query: String, filter: TdApi.SearchMessagesFilter? = null): List<TelegramFoundMessage> {
        val found = runCatching { client.query(TdApi.SearchMessages(TdApi.ChatListMain(), query, "", SEARCH_LIMIT, filter, null, 0, 0)) }.getOrNull() ?: return emptyList()
        return onDispatcher {
            found.messages.filterNotNull().mapNotNull { message ->
                val chat = chatsById[message.chatId] ?: return@mapNotNull null
                TelegramFoundMessage(message.chatId, message.id, chat.title, messagePreview(message), message.date, chat.photo?.small?.toRef())
            }
        }
    }

    /** Chat list items for [ids], in that order; TDLib reports every chat before returning its id. */
    private suspend fun items(ids: List<Long>): List<TelegramChatItem> = onDispatcher {
        ids.mapNotNull { id ->
            chatsById[id]?.let { chat ->
                val position = chat.positions.firstOrNull { it.list is TdApi.ChatListMain } ?: chat.positions.firstOrNull() ?: TdApi.ChatPosition(TdApi.ChatListMain(), 0, false, null)
                toChatItem(chat, position, archived = position.list is TdApi.ChatListArchive, myId = myId, peer = peerUsers::get, verifiedSupergroup = { it in verifiedSupergroups }, scopeMuteFor = scopeMuteFor, supergroupFlags = { id -> Triple(id in forumSupergroups, id in scamSupergroups, id in fakeSupergroups) }, chatTitle = { chatsById[it]?.title })
            }
        }
    }

    /** Runs [block] on the TDLib dispatcher, where chatsById may be read. */
    private suspend fun <T> onDispatcher(block: () -> T): T = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        client.post { continuation.resumeWith(runCatching(block)) }
    }

    private fun act(function: TdApi.Function<*>) {
        client.send(function) { result -> if (result is TdApi.Error) _error.value = humanError(result) }
    }

    /** Requests the next page of [key]; safe to call repeatedly from UI (e.g. on scroll to the end). */
    fun loadMore(key: TelegramChatListKey) = client.post { requestPage(key) }

    private fun handle(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> when (update.authorizationState) {
                is TdApi.AuthorizationStateReady -> {
                    requestPage(TelegramChatListKey.Main)
                    requestPage(TelegramChatListKey.Archive)
                }
                is TdApi.AuthorizationStateClosed -> {
                    // A background pause keeps the list on screen; it is refreshed on Ready.
                    // Logout and account switch start from nothing.
                    if (!client.paused) { chatsById.clear(); folders = emptyList(); forumSupergroups.clear(); peerUsers.clear(); verifiedSupergroups.clear(); scamSupergroups.clear(); fakeSupergroups.clear() }
                    loading.clear(); complete.clear(); wanted.clear(); failures.clear()
                    schedulePublish()
                }
            }
            is TdApi.UpdateNewChat -> { chatsById[update.chat.id] = update.chat; schedulePublish() }
            is TdApi.UpdateSupergroup -> {
                if (update.supergroup.isForum) forumSupergroups += update.supergroup.id else forumSupergroups -= update.supergroup.id
                val status = update.supergroup.verificationStatus
                if (status?.isVerified == true) verifiedSupergroups += update.supergroup.id else verifiedSupergroups -= update.supergroup.id
                if (status?.isScam == true) scamSupergroups += update.supergroup.id else scamSupergroups -= update.supergroup.id
                if (status?.isFake == true) fakeSupergroups += update.supergroup.id else fakeSupergroups -= update.supergroup.id
            }
            // A private chat's id equals its user's id, so only users with a chat here republish the list.
            is TdApi.UpdateUser -> { peerUsers[update.user.id] = update.user; if (update.user.id in chatsById) schedulePublish() }
            is TdApi.UpdateUserStatus -> peerUsers[update.userId]?.let { it.status = update.status; if (update.userId in chatsById) schedulePublish() }
            is TdApi.UpdateChatOnlineMemberCount -> _onlineMembers.value = _onlineMembers.value + (update.chatId to update.onlineMemberCount)
            // "Unread" already includes chats marked as unread (TDLib docs), so the marked count is not added.
            is TdApi.UpdateUnreadChatCount -> when (val list = update.chatList) {
                is TdApi.ChatListMain -> _unreadUnmutedChats.value = update.unreadUnmutedCount
                is TdApi.ChatListFolder -> _folderUnread.value = _folderUnread.value + (list.chatFolderId to update.unreadUnmutedCount)
                else -> Unit
            }
            is TdApi.UpdateScopeNotificationSettings -> {
                val index = when (update.scope) { is TdApi.NotificationSettingsScopePrivateChats -> 0; is TdApi.NotificationSettingsScopeGroupChats -> 1; else -> 2 }
                scopeMuteFor[index] = update.notificationSettings.muteFor
                schedulePublish()
            }
            is TdApi.UpdateChatUnreadReactionCount -> mutate(update.chatId) { it.unreadReactionCount = update.unreadReactionCount }
            is TdApi.UpdateMessageUnreadReactions -> mutate(update.chatId) { it.unreadReactionCount = update.unreadReactionCount }
            is TdApi.UpdateOption -> if (update.name == "my_id") { myId = (update.value as? TdApi.OptionValueInteger)?.value ?: 0L; schedulePublish() }
            is TdApi.UpdateChatFolders -> {
                folders = update.chatFolders.map { TelegramFolder(it.id, it.name?.text?.text.orEmpty(), it.icon?.name.orEmpty()) }
                schedulePublish()
            }
            is TdApi.UpdateChatTitle -> mutate(update.chatId) { it.title = update.title }
            is TdApi.UpdateChatPhoto -> mutate(update.chatId) { it.photo = update.photo }
            is TdApi.UpdateChatLastMessage -> mutate(update.chatId) { chat ->
                chat.lastMessage = update.lastMessage
                update.positions.forEach { chat.positions = applyChatPosition(chat.positions, it) }
            }
            is TdApi.UpdateChatPosition -> mutate(update.chatId) { it.positions = applyChatPosition(it.positions, update.position) }
            is TdApi.UpdateChatDraftMessage -> mutate(update.chatId) { chat ->
                chat.draftMessage = update.draftMessage
                update.positions.forEach { chat.positions = applyChatPosition(chat.positions, it) }
            }
            is TdApi.UpdateChatReadInbox -> mutate(update.chatId) { it.unreadCount = update.unreadCount; it.lastReadInboxMessageId = update.lastReadInboxMessageId }
            is TdApi.UpdateChatReadOutbox -> mutate(update.chatId) { it.lastReadOutboxMessageId = update.lastReadOutboxMessageId }
            is TdApi.UpdateChatUnreadMentionCount -> mutate(update.chatId) { it.unreadMentionCount = update.unreadMentionCount }
            is TdApi.UpdateMessageMentionRead -> mutate(update.chatId) { it.unreadMentionCount = update.unreadMentionCount }
            is TdApi.UpdateChatHasScheduledMessages -> mutate(update.chatId) { it.hasScheduledMessages = update.hasScheduledMessages }
            is TdApi.UpdateChatAccentColors -> mutate(update.chatId) { it.accentColorId = update.accentColorId }
            is TdApi.UpdateChatIsMarkedAsUnread -> mutate(update.chatId) { it.isMarkedAsUnread = update.isMarkedAsUnread }
            is TdApi.UpdateChatNotificationSettings -> mutate(update.chatId) { it.notificationSettings = update.notificationSettings }
        }
    }

    private fun requestPage(key: TelegramChatListKey) {
        if (key in complete) return
        // Asked again while a page is loading: load the next one right after it.
        if (key in loading) { wanted += key; return }
        loading += key
        schedulePublish()
        val sent = client.send(TdApi.LoadChats(key.toTd(), PAGE_SIZE)) { result ->
            loading -= key
            val again = wanted.remove(key)
            when {
                // 404: every chat of the list is already known.
                result is TdApi.Error && result.code == 404 -> complete += key
                result is TdApi.Error -> {
                    // Network or flood errors: try again a little later, a few times.
                    val tries = (failures[key] ?: 0) + 1
                    failures[key] = tries
                    if (tries <= 3) client.post(2_000L * tries) { requestPage(key) }
                }
                else -> {
                    failures.remove(key)
                    // TDLib may answer with a short page (only pinned chats): fill the screen anyway.
                    if (again || countIn(key) < MIN_FILL) client.post { requestPage(key) }
                }
            }
            schedulePublish()
        }
        if (!sent) loading -= key
    }

    private fun countIn(key: TelegramChatListKey): Int {
        val list = key.toTd()
        return chatsById.values.count { chat -> chat.positions.any { sameChatList(it.list, list) && it.order != 0L } }
    }

    private fun mutate(id: Long, block: (TdApi.Chat) -> Unit) {
        chatsById[id]?.let { block(it); schedulePublish() }
    }

    private fun schedulePublish() {
        if (publishScheduled) return
        publishScheduled = true
        client.post(PUBLISH_DELAY_MS) {
            publishScheduled = false
            publish()
        }
    }

    private fun publish() {
        _lists.value = TelegramChatLists(
            main = state(TelegramChatListKey.Main),
            archive = state(TelegramChatListKey.Archive),
            folders = folders,
            folderChats = folders.associate { it.id to state(TelegramChatListKey.Folder(it.id)) }
        )
    }

    private fun state(key: TelegramChatListKey): TelegramChatListState {
        val list = key.toTd()
        val chats = chatsById.values.mapNotNull { chat ->
            val position = chat.positions.firstOrNull { sameChatList(it.list, list) }?.takeIf { it.order != 0L } ?: return@mapNotNull null
            toChatItem(chat, position, archived = key == TelegramChatListKey.Archive, myId = myId, peer = peerUsers::get, verifiedSupergroup = { it in verifiedSupergroups }, scopeMuteFor = scopeMuteFor, supergroupFlags = { id -> Triple(id in forumSupergroups, id in scamSupergroups, id in fakeSupergroups) }, chatTitle = { chatsById[it]?.title }) to position.order
        }.sortedWith(compareByDescending<Pair<TelegramChatItem, Long>> { it.second }.thenByDescending { it.first.id }).map { it.first }
        return TelegramChatListState(chats, loading = key in loading, complete = key in complete)
    }

    private companion object {
        const val MIN_FILL = 30
        const val PAGE_SIZE = 50
        const val PUBLISH_DELAY_MS = 32L
        const val SEARCH_LIMIT = 50
    }
}

internal fun TelegramChatListKey.toTd(): TdApi.ChatList = when (this) {
    TelegramChatListKey.Main -> TdApi.ChatListMain()
    TelegramChatListKey.Archive -> TdApi.ChatListArchive()
    is TelegramChatListKey.Folder -> TdApi.ChatListFolder(id)
}

internal fun toChatItem(
    chat: TdApi.Chat, position: TdApi.ChatPosition, archived: Boolean, myId: Long = 0L,
    peer: (Long) -> TdApi.User? = { null }, verifiedSupergroup: (Long) -> Boolean = { false }, scopeMuteFor: IntArray? = null,
    /** Supergroup id -> (forum, scam, fake). */
    supergroupFlags: (Long) -> Triple<Boolean, Boolean, Boolean> = { Triple(false, false, false) },
    chatTitle: (Long) -> String? = { null }
): TelegramChatItem {
    val saved = myId != 0L && (chat.type as? TdApi.ChatTypePrivate)?.userId == myId
    val last = chat.lastMessage
    val type = chat.type
    val peerUserId = (type as? TdApi.ChatTypePrivate)?.userId ?: (type as? TdApi.ChatTypeSecret)?.userId ?: 0L
    val peerUser = if (peerUserId != 0L) peer(peerUserId) else null
    val isChannel = (type as? TdApi.ChatTypeSupergroup)?.isChannel == true
    val isGroup = type is TdApi.ChatTypeBasicGroup || (type is TdApi.ChatTypeSupergroup && !type.isChannel)
    val groupFlags = (type as? TdApi.ChatTypeSupergroup)?.let { supergroupFlags(it.supergroupId) } ?: Triple(false, false, false)
    val serviceLine = last?.let { message ->
        val who = when (val sender = message.senderId) {
            is TdApi.MessageSenderUser -> if (sender.userId == myId) "Вы" else peer(sender.userId)?.displayName() ?: "Пользователь"
            is TdApi.MessageSenderChat -> chatTitle(sender.chatId) ?: chat.title.orEmpty()
            else -> ""
        }
        serviceText(message, who, isChannel) { id -> if (id == myId) "Вы" else peer(id)?.displayName() ?: "Пользователь" }
    }
    val draft = (chat.draftMessage?.content as? TdApi.DraftMessageContentText)?.text?.text?.takeIf { it.isNotBlank() }
    val notifications = chat.notificationSettings
    return TelegramChatItem(
        id = chat.id,
        title = if (saved) "Избранное" else chat.title.orEmpty(),
        preview = messagePreview(last).let { if ((chat.type as? TdApi.ChatTypePrivate)?.userId == TELEGRAM_SERVICE_USER) hideLoginCode(it) else it },
        timestamp = maxOf(last?.date ?: 0, if (draft != null) chat.draftMessage?.date ?: 0 else 0),
        unreadCount = chat.unreadCount,
        unreadMentionCount = chat.unreadMentionCount,
        isMarkedUnread = chat.isMarkedAsUnread,
        isPinned = position.isPinned,
        // Chats on the scope default are shown unmuted; scope defaults arrive with notifications work.
        isMuted = notifications != null && !notifications.useDefaultMuteFor && notifications.muteFor > 0,
        draft = draft,
        outgoingState = when {
            saved || last == null || !last.isOutgoing -> LastMessageState.None
            last.id <= chat.lastReadOutboxMessageId -> LastMessageState.Read
            else -> LastMessageState.Sent
        },
        photo = if (saved) null else chat.photo?.small?.toRef(),
        archived = archived,
        isSaved = saved,
        kind = when (type) {
            is TdApi.ChatTypeSupergroup -> if (type.isChannel) TelegramChatKind.Channel else TelegramChatKind.Group
            is TdApi.ChatTypeBasicGroup -> TelegramChatKind.Group
            else -> TelegramChatKind.Private
        },
        lastSender = when {
            last == null || saved || isChannel || serviceLine != null -> null
            last.isOutgoing -> "Вы"
            !isGroup -> null
            else -> when (val sender = last.senderId) {
                is TdApi.MessageSenderUser -> peer(sender.userId)?.let { it.firstName.ifBlank { it.lastName } }?.takeIf { it.isNotBlank() }
                // A linked channel posting in the group shows its title; an anonymous admin (the group itself) nothing.
                is TdApi.MessageSenderChat -> if (sender.chatId == chat.id) null else chatTitle(sender.chatId)
                else -> null
            }
        },
        lastServiceText = serviceLine,
        lastPollQuestion = (last?.content as? TdApi.MessagePoll)?.poll?.question?.text,
        lastDelivery = when {
            saved || last == null || !last.isOutgoing -> TelegramDelivery.None
            last.sendingState is TdApi.MessageSendingStatePending -> TelegramDelivery.Sending
            last.sendingState is TdApi.MessageSendingStateFailed -> TelegramDelivery.Failed
            last.id <= chat.lastReadOutboxMessageId -> TelegramDelivery.Read
            else -> TelegramDelivery.Sent
        },
        lastKind = kindOf(last?.content),
        lastDuration = when (val content = last?.content) {
            is TdApi.MessageVoiceNote -> content.voiceNote.duration
            is TdApi.MessageVideoNote -> content.videoNote.duration
            else -> 0
        },
        peerUserId = peerUserId,
        isBot = peerUser?.type is TdApi.UserTypeBot,
        isSecret = type is TdApi.ChatTypeSecret,
        isVerified = peerUser?.verificationStatus?.isVerified == true || ((type as? TdApi.ChatTypeSupergroup)?.supergroupId?.let(verifiedSupergroup) == true),
        // TDLib may not send the offline status when "online" expires: check the expiry too.
        isOnline = !saved && (peerUser?.status as? TdApi.UserStatusOnline)?.let { it.expires > System.currentTimeMillis() / 1000 } == true,
        unreadReactionCount = chat.unreadReactionCount,
        // "Use default" follows the scope of this kind of chat (channels are often muted that way).
        isMutedResolved = notifications != null && if (!notifications.useDefaultMuteFor) notifications.muteFor > 0
            else (scopeMuteFor?.get(if (peerUserId != 0L) 0 else if (isChannel) 2 else 1) ?: 0) > 0,
        accentColorId = chat.accentColorId,
        isPremium = peerUser?.isPremium == true,
        isScam = peerUser?.verificationStatus?.isScam == true || groupFlags.second,
        isFake = peerUser?.verificationStatus?.isFake == true || groupFlags.third,
        hasScheduled = chat.hasScheduledMessages,
        isForum = groupFlags.first,
        canDeleteForAll = chat.canBeDeletedForAllUsers && peerUserId == 0L
    )
}

/** [member]: the user is already in the chat ([chatId] then opens it). */
data class InviteLinkInfo(val chatId: Long, val title: String, val channel: Boolean, val public: Boolean, val members: Int, val knownNames: List<String>,
                          val description: String, val byRequest: Boolean, val photo: TelegramFileRef?, val member: Boolean)

internal fun TdApi.File.toRef() = TelegramFileRef(id, local?.path?.takeIf { local.isDownloadingCompleted && it.isNotBlank() })

/** Telegram's service account, which sends sign-in codes. */
internal const val TELEGRAM_SERVICE_USER = 777000L

/**
 * Sign-in codes from Telegram are hidden in the chat list, as the official app does: anyone glancing
 * at the screen could read them there. Inside the chat the code is shown as sent.
 */
internal fun hideLoginCode(preview: String): String = preview.replace(Regex("""(?<![\d-])\d{5,6}(?![\d-])""")) { "•".repeat(it.value.length) }

internal fun messagePreview(message: TdApi.Message?): String = when (val content = message?.content) {
    is TdApi.MessageText -> content.text.text
    is TdApi.MessagePhoto -> content.caption.text.ifBlank { "Фотография" }
    is TdApi.MessageVideo -> content.caption.text.ifBlank { "Видео" }
    is TdApi.MessageDocument -> content.caption.text.ifBlank { "Документ" }
    is TdApi.MessageVoiceNote -> content.caption.text.ifBlank { "Голосовое сообщение" }
    is TdApi.MessageVideoNote -> "Видеосообщение"
    is TdApi.MessageSticker -> "Стикер ${content.sticker.emoji}".trim()
    is TdApi.MessageDice -> diceText(content)
    is TdApi.MessageAnimation -> content.caption.text.ifBlank { "GIF" }
    is TdApi.MessageAudio -> content.caption.text.ifBlank { "Аудио" }
    is TdApi.MessageCall -> "Звонок"
    is TdApi.MessageContact -> "Контакт"
    is TdApi.MessageLocation -> "Геопозиция"
    is TdApi.MessagePoll -> "Опрос"
    null -> ""
    else -> "Сообщение"
}

private const val MUTE_FOREVER_S = 400 * 24 * 60 * 60

data class TelegramProfileInfo(val username: String = "", val about: String = "", val phone: String = "", val emojiStatusId: Long = 0)

/** Custom emoji id of a user's emoji status, 0 when none or expired. */
internal fun TdApi.User.emojiStatusId(): Long {
    val status = emojiStatus ?: return 0L
    if (status.expirationDate != 0 && status.expirationDate <= System.currentTimeMillis() / 1000) return 0L
    return (status.type as? TdApi.EmojiStatusTypeCustomEmoji)?.customEmojiId ?: 0L
}

enum class SharedMediaKind(val title: String) { Media("Медиа"), Files("Файлы"), Music("Музыка"), Voice("Голосовые"), Links("Ссылки") }

data class TelegramSharedItem(
    val messageId: Long,
    val date: Int,
    val thumb: TelegramFileRef? = null,
    val title: String = "",
    val subtitle: String = "",
    val isVideo: Boolean = false,
    /** Largest size of a photo, for the full-screen viewer. */
    val full: TelegramFileRef? = null,
    /** The file itself (documents, music, voice notes), to download and open from the profile. */
    val file: TelegramFileRef? = null,
    val size: Long = 0,
    val mimeType: String = "",
    /** Music: performer and length in seconds. */
    val performer: String = "",
    val duration: Int = 0
)

private const val SHARED_LIMIT = 60

data class TelegramMember(val userId: Long, val name: String, val photo: TelegramFileRef?, val status: TelegramUserStatus, val role: String)

private const val MEMBERS_LIMIT = 200
