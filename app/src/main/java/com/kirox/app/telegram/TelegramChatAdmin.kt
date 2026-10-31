package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi

/** What the signed-in user may do in a group or channel. */
data class ChatRights(
    val isChannel: Boolean,
    val isOwner: Boolean,
    val canChangeInfo: Boolean,
    val canRestrict: Boolean,
    val canPromote: Boolean,
    val canInvite: Boolean
) {
    /** Anything to manage at all: the «Управление» section shows only then. */
    val any get() = canChangeInfo || canRestrict || canPromote || canInvite
}

/** Editable facts of a group or channel. */
data class ChatDetails(val title: String, val description: String)

/** A member as the management screen shows it; [canBeEdited] — your rights reach them. */
data class ManagedMember(val userId: Long, val name: String, val photo: TelegramFileRef?, val role: MemberRole, val canBeEdited: Boolean)

enum class MemberRole(val title: String) { Owner("Создатель"), Admin("Администратор"), Member(""), Restricted("Только чтение"), Banned("Заблокирован") }

/** Group-wide permissions in plain words; everything else Telegram has keeps its current value. */
data class GroupPermissions(
    val sendMessages: Boolean,
    val sendMedia: Boolean,
    val sendStickers: Boolean,
    val sendPolls: Boolean,
    val addLinkPreviews: Boolean,
    val inviteUsers: Boolean,
    val pinMessages: Boolean,
    val changeInfo: Boolean
)

/** A channel Telegram suggests next to another. */
data class SimilarChat(val chatId: Long, val title: String, val photo: TelegramFileRef?)

/** Main numbers of a channel or group for its admins; each with the previous period's value. */
data class ChatStats(val isChannel: Boolean, val period: String, val values: List<Triple<String, Double, Double>>, val notificationsPercent: Double?)

/** Someone asking to join by a link that needs approval. */
data class JoinRequest(val userId: Long, val name: String, val photo: TelegramFileRef?, val bio: String, val date: Int)

data class InviteLink(val link: String, val name: String, val isPrimary: Boolean, val memberCount: Int, val expires: Int, val memberLimit: Int)

/** One step of a report: Telegram asks for a reason, then optionally for a comment. */
sealed interface ReportStep {
    data object Done : ReportStep
    data class ChooseReason(val title: String, val options: List<Pair<ByteArray, String>>) : ReportStep
    data class AddComment(val optionId: ByteArray, val optional: Boolean) : ReportStep
    data object Failed : ReportStep
}

/**
 * Managing groups and channels as their owner or admin: info, photo, members and their roles,
 * group permissions and invite links; plus reports and message links, which any member may use.
 */
class TelegramChatAdmin(private val client: TelegramClient) {

    suspend fun rights(chatId: Long): ChatRights? = runCatching {
        val chat = client.query(TdApi.GetChat(chatId))
        val isChannel = (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == true
        if (chat.type !is TdApi.ChatTypeSupergroup && chat.type !is TdApi.ChatTypeBasicGroup) return null
        val me = client.query(TdApi.GetMe()).id
        when (val status = client.query(TdApi.GetChatMember(chatId, TdApi.MessageSenderUser(me))).status) {
            is TdApi.ChatMemberStatusCreator -> ChatRights(isChannel, true, true, true, true, true)
            is TdApi.ChatMemberStatusAdministrator -> status.rights.let { ChatRights(isChannel, false, it.canChangeInfo, it.canRestrictMembers, it.canPromoteMembers, it.canInviteUsers) }
            // Plain members of a group may still change info or invite when the group allows it.
            else -> chat.permissions.let { ChatRights(isChannel, false, !isChannel && it.canChangeInfo, false, false, !isChannel && it.canInviteUsers) }
        }
    }.getOrNull()

    suspend fun details(chatId: Long): ChatDetails {
        val chat = client.query(TdApi.GetChat(chatId))
        val description = runCatching {
            when (val type = chat.type) {
                is TdApi.ChatTypeSupergroup -> client.query(TdApi.GetSupergroupFullInfo(type.supergroupId)).description
                is TdApi.ChatTypeBasicGroup -> client.query(TdApi.GetBasicGroupFullInfo(type.basicGroupId)).description
                else -> ""
            }
        }.getOrDefault("")
        return ChatDetails(chat.title.orEmpty(), description.orEmpty())
    }

    suspend fun setTitle(chatId: Long, title: String) { client.query(TdApi.SetChatTitle(chatId, title)) }
    suspend fun setDescription(chatId: Long, description: String) { client.query(TdApi.SetChatDescription(chatId, description)) }
    suspend fun setPhoto(chatId: Long, path: String) { client.query(TdApi.SetChatPhoto(chatId, TdApi.InputChatPhotoStatic(TdApi.InputFileLocal(path)))) }

    /** Members with roles: everyone recent, plus admins, restricted and banned when you can see them. */
    suspend fun members(chatId: Long): List<ManagedMember> {
        val chat = client.query(TdApi.GetChat(chatId))
        val raw: List<TdApi.ChatMember> = when (val type = chat.type) {
            is TdApi.ChatTypeBasicGroup -> client.query(TdApi.GetBasicGroupFullInfo(type.basicGroupId)).members.toList()
            is TdApi.ChatTypeSupergroup -> buildList {
                listOf<TdApi.SupergroupMembersFilter>(
                    TdApi.SupergroupMembersFilterAdministrators(), TdApi.SupergroupMembersFilterRecent(),
                    TdApi.SupergroupMembersFilterRestricted(""), TdApi.SupergroupMembersFilterBanned("")
                ).forEach { filter -> runCatching { addAll(client.query(TdApi.GetSupergroupMembers(type.supergroupId, filter, 0, LIMIT)).members) } }
            }
            else -> emptyList()
        }
        return raw.distinctBy { (it.memberId as? TdApi.MessageSenderUser)?.userId }.mapNotNull { member ->
            val userId = (member.memberId as? TdApi.MessageSenderUser)?.userId ?: return@mapNotNull null
            val user = runCatching { client.query(TdApi.GetUser(userId)) }.getOrNull() ?: return@mapNotNull null
            val (role, editable) = when (val s = member.status) {
                is TdApi.ChatMemberStatusCreator -> MemberRole.Owner to false
                is TdApi.ChatMemberStatusAdministrator -> MemberRole.Admin to s.canBeEdited
                is TdApi.ChatMemberStatusRestricted -> MemberRole.Restricted to true
                is TdApi.ChatMemberStatusBanned -> MemberRole.Banned to true
                else -> MemberRole.Member to true
            }
            ManagedMember(userId, user.displayName(), user.profilePhoto?.small?.toRef(), role, editable)
        }.sortedWith(compareBy({ it.role.ordinal }, { it.name }))
    }

    /** Admin with the usual rights (everything except adding other admins). */
    suspend fun promote(chatId: Long, userId: Long, isChannel: Boolean) {
        val rights = TdApi.ChatAdministratorRights(
            true, true, isChannel, isChannel, true, true, true, true, !isChannel, false, true, true, true, true, false, false, false, false
        )
        setStatus(chatId, userId, TdApi.ChatMemberStatusAdministrator(true, rights))
    }

    suspend fun demote(chatId: Long, userId: Long) = setStatus(chatId, userId, TdApi.ChatMemberStatusMember(0))

    /** Removes from the group; they can come back by an invite link. */
    suspend fun remove(chatId: Long, userId: Long) {
        setStatus(chatId, userId, TdApi.ChatMemberStatusBanned(0))
        runCatching { setStatus(chatId, userId, TdApi.ChatMemberStatusLeft()) }
    }

    /** Removes and keeps out until unblocked. */
    suspend fun ban(chatId: Long, userId: Long) = setStatus(chatId, userId, TdApi.ChatMemberStatusBanned(0))

    suspend fun unban(chatId: Long, userId: Long) = setStatus(chatId, userId, TdApi.ChatMemberStatusLeft())

    /** «Только чтение»: stays in the group but can't send anything. */
    suspend fun restrict(chatId: Long, userId: Long) =
        setStatus(chatId, userId, TdApi.ChatMemberStatusRestricted(true, 0, TdApi.ChatPermissions(false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false)))

    suspend fun unrestrict(chatId: Long, userId: Long) = setStatus(chatId, userId, TdApi.ChatMemberStatusMember(0))

    private suspend fun setStatus(chatId: Long, userId: Long, status: TdApi.ChatMemberStatus) {
        client.query(TdApi.SetChatMemberStatus(chatId, TdApi.MessageSenderUser(userId), status))
    }

    suspend fun permissions(chatId: Long): GroupPermissions = client.query(TdApi.GetChat(chatId)).permissions.let {
        GroupPermissions(it.canSendBasicMessages, it.canSendPhotos && it.canSendVideos && it.canSendDocuments, it.canSendOtherMessages, it.canSendPolls, it.canAddLinkPreviews, it.canInviteUsers, it.canPinMessages, it.canChangeInfo)
    }

    suspend fun setPermissions(chatId: Long, value: GroupPermissions) {
        val current = client.query(TdApi.GetChat(chatId)).permissions
        val media = value.sendMedia
        client.query(TdApi.SetChatPermissions(chatId, TdApi.ChatPermissions(
            value.sendMessages, media, media, media, media, media, media, value.sendPolls, value.sendStickers, value.addLinkPreviews,
            current.canReactToMessages, current.canEditTag, value.changeInfo, value.inviteUsers, value.pinMessages, current.canCreateTopics
        )))
    }

    suspend fun inviteLinks(chatId: Long): List<InviteLink> {
        val me = client.query(TdApi.GetMe()).id
        val links = client.query(TdApi.GetChatInviteLinks(chatId, me, false, 0, "", 50)).inviteLinks.toList()
        val all = if (links.none { it.isPrimary }) (runCatching { listOf(client.query(TdApi.ReplacePrimaryChatInviteLink(chatId))) }.getOrDefault(emptyList()) + links) else links
        return all.map { InviteLink(it.inviteLink, it.name.orEmpty(), it.isPrimary, it.memberCount, it.expirationDate, it.memberLimit) }
            .sortedByDescending { it.isPrimary }
    }

    /** [byApproval]: people send a request instead of joining at once; Telegram allows no member limit then. */
    suspend fun createInviteLink(chatId: Long, name: String, expiresInDays: Int, memberLimit: Int, byApproval: Boolean = false): InviteLink {
        val expires = if (expiresInDays > 0) (System.currentTimeMillis() / 1000 + expiresInDays * 86_400L).toInt() else 0
        return client.query(TdApi.CreateChatInviteLink(chatId, name, expires, if (byApproval) 0 else memberLimit, byApproval)).let {
            InviteLink(it.inviteLink, it.name.orEmpty(), it.isPrimary, it.memberCount, it.expirationDate, it.memberLimit)
        }
    }

    /** Revoking the main link makes Telegram give the chat a new one. */
    suspend fun revokeInviteLink(chatId: Long, link: String) { client.query(TdApi.RevokeChatInviteLink(chatId, link)) }

    /** «Похожие каналы» under a channel's profile. */
    suspend fun similarChannels(chatId: Long): List<SimilarChat> = runCatching {
        client.query(TdApi.GetChatSimilarChats(chatId)).chatIds.toList().mapNotNull { id ->
            runCatching { client.query(TdApi.GetChat(id)) }.getOrNull()?.let { SimilarChat(it.id, it.title, it.photo?.small?.toRef()) }
        }
    }.getOrDefault(emptyList())

    /** Statistics are open to admins of larger channels and groups (Telegram decides). */
    suspend fun canSeeStatistics(chatId: Long): Boolean = runCatching {
        val type = client.query(TdApi.GetChat(chatId)).type as? TdApi.ChatTypeSupergroup ?: return false
        client.query(TdApi.GetSupergroupFullInfo(type.supergroupId)).canGetStatistics
    }.getOrDefault(false)

    suspend fun statistics(chatId: Long): ChatStats? = runCatching {
        fun period(range: TdApi.DateRange) = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).let { f ->
            "${f.format(java.util.Date(range.startDate * 1000L))} — ${f.format(java.util.Date(range.endDate * 1000L))}"
        }
        fun v(title: String, value: TdApi.StatisticalValue) = Triple(title, value.value, value.previousValue)
        when (val s = client.query(TdApi.GetChatStatistics(chatId, true))) {
            is TdApi.ChatStatisticsChannel -> ChatStats(true, period(s.period), listOf(
                v("Подписчики", s.memberCount), v("Просмотров на пост", s.meanMessageViewCount),
                v("Репостов на пост", s.meanMessageShareCount), v("Реакций на пост", s.meanMessageReactionCount),
                v("Просмотров истории", s.meanStoryViewCount)
            ), s.enabledNotificationsPercentage)
            is TdApi.ChatStatisticsSupergroup -> ChatStats(false, period(s.period), listOf(
                v("Участники", s.memberCount), v("Сообщения", s.messageCount), v("Читают", s.viewerCount), v("Пишут", s.senderCount)
            ), null)
            else -> null
        }
    }.getOrNull()

    /** «Заявки на вступление» from all links of the chat, newest first. */
    suspend fun joinRequests(chatId: Long): List<JoinRequest> =
        client.query(TdApi.GetChatJoinRequests(chatId, "", "", null, 100)).requests.map { request ->
            val user = runCatching { client.query(TdApi.GetUser(request.userId)) }.getOrNull()
            JoinRequest(request.userId, user?.displayName() ?: "Пользователь", user?.profilePhoto?.small?.toRef(), request.bio.orEmpty(), request.date)
        }

    suspend fun processJoinRequest(chatId: Long, userId: Long, approve: Boolean) { client.query(TdApi.ProcessChatJoinRequest(chatId, userId, approve)) }

    /** «Запретить копирование»: members can't forward, save or screenshot-copy messages. Owner only. */
    suspend fun isProtected(chatId: Long): Boolean = client.query(TdApi.GetChat(chatId)).hasProtectedContent

    suspend fun setProtected(chatId: Long, value: Boolean) { client.query(TdApi.ToggleChatHasProtectedContent(chatId, value)) }

    /** t.me link to a message of a group or channel; null where Telegram has none (private chats). */
    suspend fun messageLink(chatId: Long, messageId: Long): String? =
        runCatching { client.query(TdApi.GetMessageLink(chatId, messageId, 0, 0, "", false, false)).link }.getOrNull()

    /** Sends a report; call again with the chosen reason or the comment until it is [ReportStep.Done]. */
    suspend fun report(chatId: Long, messageIds: List<Long>, optionId: ByteArray = ByteArray(0), text: String = ""): ReportStep = runCatching {
        when (val result = client.query(TdApi.ReportChat(chatId, optionId, messageIds.toLongArray(), text))) {
            is TdApi.ReportChatResultOk -> ReportStep.Done
            is TdApi.ReportChatResultOptionRequired -> ReportStep.ChooseReason(result.title, result.options.map { it.id to it.text })
            is TdApi.ReportChatResultTextRequired -> ReportStep.AddComment(result.optionId, result.isOptional)
            else -> ReportStep.Failed
        }
    }.getOrDefault(ReportStep.Failed)

    private companion object { const val LIMIT = 200 }
}
