package com.kirox.app.telegram

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

/** Result of checking a username before saving it. */
enum class UsernameCheck { Free, Occupied, Purchasable, TooMany, Invalid }

/** Users cache, the contact list and the signed-in user's profile. Dispatcher-confined like chats. */
class TelegramUserRepository(private val client: TelegramClient) {
    private val users = HashMap<Long, TdApi.User>()
    private var myId = 0L
    private var myBio = ""
    private var myBirthday = ""
    private var contactsLoaded = false
    private var publishScheduled = false

    private val _profile = MutableStateFlow<TelegramUserProfile?>(null)
    val profile: StateFlow<TelegramUserProfile?> = _profile.asStateFlow()
    private val _contacts = MutableStateFlow<List<TelegramContact>>(emptyList())
    val contacts: StateFlow<List<TelegramContact>> = _contacts.asStateFlow()
    private val _contactsLoading = MutableStateFlow(false)
    val contactsLoading: StateFlow<Boolean> = _contactsLoading.asStateFlow()

    init { client.addUpdateHandler(::handle) }

    /** Dispatcher-only lookup for other repositories. */
    internal fun user(id: Long): TdApi.User? = users[id]
    internal fun myId(): Long = myId

    private fun handle(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> when (update.authorizationState) {
                is TdApi.AuthorizationStateReady -> load()
                // A background pause keeps profile and contacts; Ready reloads them.
                is TdApi.AuthorizationStateClosed -> if (!client.paused) {
                    users.clear(); myId = 0; myBio = ""; myBirthday = ""; contactsLoaded = false
                    _profile.value = null; _contacts.value = emptyList()
                }
            }
            is TdApi.UpdateUser -> { users[update.user.id] = update.user; schedulePublish() }
            is TdApi.UpdateUserStatus -> users[update.userId]?.let { it.status = update.status; schedulePublish() }
            is TdApi.UpdateUserFullInfo -> if (update.userId == myId) { applyFullInfo(update.userFullInfo); schedulePublish() }
        }
    }

    private fun load() {
        client.send(TdApi.GetMe()) { result ->
            if (result !is TdApi.User) return@send
            myId = result.id
            users[result.id] = result
            client.send(TdApi.GetUserFullInfo(result.id)) { info ->
                if (info is TdApi.UserFullInfo) applyFullInfo(info)
                schedulePublish()
            }
            schedulePublish()
        }
        _contactsLoading.value = true
        client.send(TdApi.GetContacts()) { result ->
            // TDLib sends UpdateUser for every returned id before this result, so the cache is filled.
            contactsLoaded = true
            _contactsLoading.value = false
            schedulePublish()
        }
    }

    private fun schedulePublish() {
        if (publishScheduled) return
        publishScheduled = true
        client.post(PUBLISH_DELAY_MS) {
            publishScheduled = false
            users[myId]?.let { _profile.value = it.toProfile(myBio, myBirthday) }
            if (contactsLoaded) {
                _contacts.value = users.values.filter { it.isContact && it.id != myId }.map { it.toContact() }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
        }
    }

    private fun applyFullInfo(info: TdApi.UserFullInfo) {
        myBio = info.bio?.text.orEmpty()
        myBirthday = info.birthdate?.let { formatBirthday(it.day, it.month, it.year) }.orEmpty()
    }

    /** Id of the signed-in user's own chat (Saved Messages), creating it locally if needed. */
    suspend fun savedMessagesChatId(): Long {
        val me = client.query(TdApi.GetMe())
        return client.query(TdApi.CreatePrivateChat(me.id, false)).id
    }

    /**
     * Adds a contact by phone number. Returns the Telegram user id, or null when the number
     * is not registered in Telegram.
     */
    /** Renames a contact on this account; the phone number is kept as Telegram has it. */
    suspend fun renameContact(userId: Long, firstName: String, lastName: String) {
        client.query(TdApi.AddContact(userId, TdApi.ImportedContact("", firstName, lastName, null), false))
    }

    suspend fun removeContact(userId: Long) { client.query(TdApi.RemoveContacts(longArrayOf(userId))) }

    suspend fun setBlocked(userId: Long, blocked: Boolean) {
        client.query(TdApi.SetMessageSenderBlockList(TdApi.MessageSenderUser(userId), if (blocked) TdApi.BlockListMain() else null))
    }

    suspend fun addContact(phone: String, firstName: String, lastName: String): Long? {
        val contact = TdApi.ImportedContact(phone, firstName, lastName, null)
        val result = client.query(TdApi.ImportContacts(arrayOf(contact)))
        return result.userIds.firstOrNull()?.takeIf { it != 0L }
    }

    /**
     * Saves only what changed, all requests at once; TDLib then sends UpdateUser and the profile refreshes.
     * The full info (bio) is re-read only when the bio changed.
     */
    suspend fun updateProfile(first: String, last: String, bio: String, username: String, old: TelegramUserProfile?) = kotlinx.coroutines.coroutineScope {
        val jobs = buildList {
            if (first != old?.firstName || last != old.lastName) add(async { client.query(TdApi.SetName(first, last)) })
            if (bio != old?.bio) add(async { client.query(TdApi.SetBio(bio)) })
            if (username != old?.username.orEmpty()) add(async { client.query(TdApi.SetUsername(username)) })
        }
        jobs.awaitAll()
        if (bio != old?.bio) client.query(TdApi.GetUserFullInfo(myId)).let { info -> client.post { applyFullInfo(info); schedulePublish() } }
    }

    /** Whether [username] can be taken by this account; asked while the user types. */
    suspend fun checkUsername(username: String): UsernameCheck =
        when (client.query(TdApi.CheckChatUsername(myId, username))) {
            is TdApi.CheckChatUsernameResultOk -> UsernameCheck.Free
            is TdApi.CheckChatUsernameResultUsernameOccupied -> UsernameCheck.Occupied
            is TdApi.CheckChatUsernameResultUsernamePurchasable -> UsernameCheck.Purchasable
            is TdApi.CheckChatUsernameResultPublicChatsTooMany -> UsernameCheck.TooMany
            else -> UsernameCheck.Invalid
        }

    /** Gifts shown on a user's profile (the ones they keep visible). */
    suspend fun gifts(userId: Long): Pair<Int, List<ProfileGift>> = runCatching {
        val result = client.query(TdApi.GetReceivedGifts("", TdApi.MessageSenderUser(userId), 0, false, false, false, false, false, false, false, false, false, "", 50))
        result.totalCount to result.gifts.map { gift ->
            val from = (gift.senderId as? TdApi.MessageSenderUser)?.let { s -> runCatching { client.query(TdApi.GetUser(s.userId)).displayName() }.getOrNull() }.orEmpty()
            when (val sent = gift.gift) {
                is TdApi.SentGiftRegular -> ProfileGift(stillOf(sent.gift.sticker), sent.gift.sticker?.emoji.orEmpty(), sent.gift.starCount, from, "")
                is TdApi.SentGiftUpgraded -> ProfileGift(null, "🎁", 0, from, "${sent.gift.title} #${sent.gift.number}")
                else -> ProfileGift(null, "🎁", 0, from, "")
            }
        }
    }.getOrDefault(0 to emptyList())

    /** Gifts that can be bought for Stars now, cheapest first. */
    suspend fun giftCatalog(): List<GiftOption> = runCatching {
        client.query(TdApi.GetAvailableGifts()).gifts.map { it.gift }.filter { !it.isPremium || it.starCount > 0 }.map { g ->
            val limits = g.overallLimits
            GiftOption(g.id, stillOf(g.sticker), g.sticker?.emoji.orEmpty(), g.starCount, limits != null && limits.totalCount > 0 && limits.remainingCount == 0)
        }.sortedBy { it.stars }
    }.getOrDefault(emptyList())

    /** Buys [giftId] for [userId] with Stars; [private] hides the sender's name from others. */
    suspend fun sendGift(giftId: Long, userId: Long, text: String, private: Boolean) {
        client.query(TdApi.SendGift(giftId, TdApi.MessageSenderUser(userId), TdApi.FormattedText(text, emptyArray()), private, false))
    }

    /** Gifts this account received. */
    suspend fun myGifts(): Pair<Int, List<ProfileGift>> = gifts(myId)

    /** Your Telegram Stars balance; null when Telegram doesn't say. */
    suspend fun stars(): Long? = runCatching {
        client.query(TdApi.GetStarTransactions(TdApi.MessageSenderUser(myId), "", null, "", 1)).starAmount.starCount
    }.getOrNull()

    private fun stillOf(sticker: TdApi.Sticker?): TelegramFileRef? {
        sticker ?: return null
        if (sticker.format is TdApi.StickerFormatWebp) return sticker.sticker.toRef()
        return sticker.thumbnail?.takeIf { it.format is TdApi.ThumbnailFormatWebp || it.format is TdApi.ThumbnailFormatPng || it.format is TdApi.ThumbnailFormatJpeg }?.file?.toRef()
    }

    /** Statuses Telegram suggests for the picker. */
    suspend fun defaultEmojiStatuses(): List<Long> = runCatching { client.query(TdApi.GetDefaultEmojiStatuses()).customEmojiIds.toList() }.getOrDefault(emptyList())

    /** Sets the emoji status (Telegram Premium); 0 removes it. */
    suspend fun setEmojiStatus(customEmojiId: Long) {
        client.query(TdApi.SetEmojiStatus(if (customEmojiId == 0L) null else TdApi.EmojiStatus(TdApi.EmojiStatusTypeCustomEmoji(customEmojiId), 0)))
    }

    suspend fun setProfilePhoto(path: String) {
        client.query(TdApi.SetProfilePhoto(TdApi.InputChatPhotoStatic(TdApi.InputFileLocal(path)), false))
    }

    /** A square MP4 of up to 10 s as the profile video; its first frame is the still picture. */
    suspend fun setProfileVideo(path: String) {
        client.query(TdApi.SetProfilePhoto(TdApi.InputChatPhotoAnimation(TdApi.InputFileLocal(path), 0.0), false))
    }

    private companion object { const val PUBLISH_DELAY_MS = 50L }
}

internal fun TdApi.User.displayName(): String =
    listOf(firstName, lastName).filter { !it.isNullOrBlank() }.joinToString(" ").ifBlank { "Удалённый аккаунт" }

internal fun TdApi.User.toContact() = TelegramContact(
    id, displayName(), usernames?.activeUsernames?.firstOrNull(), status.toStatus(), profilePhoto?.small?.toRef(), phoneNumber.orEmpty()
)

// The editable username is the one SetUsername changes; collectible ones only come after it.
internal fun TdApi.User.toProfile(bio: String, birthday: String = "") = TelegramUserProfile(
    id, displayName(), usernames?.editableUsername?.takeIf { it.isNotEmpty() } ?: usernames?.activeUsernames?.firstOrNull(), phoneNumber.orEmpty(), bio, profilePhoto?.small?.toRef(),
    isPremium = isPremium, isVerified = verificationStatus?.isVerified == true, birthday = birthday,
    firstName = firstName.orEmpty(), lastName = lastName.orEmpty(), emojiStatusId = emojiStatusId()
)

internal fun formatBirthday(day: Int, month: Int, year: Int): String =
    if (day <= 0 || month <= 0) "" else if (year > 0) "%d.%02d.%d".format(day, month, year) else "%d.%02d".format(day, month)

internal fun TdApi.UserStatus?.toStatus(): TelegramUserStatus = when (this) {
    is TdApi.UserStatusOnline -> TelegramUserStatus.Online
    is TdApi.UserStatusOffline -> TelegramUserStatus.Offline(wasOnline)
    is TdApi.UserStatusRecently -> TelegramUserStatus.Recently
    is TdApi.UserStatusLastWeek -> TelegramUserStatus.LastWeek
    is TdApi.UserStatusLastMonth -> TelegramUserStatus.LastMonth
    else -> TelegramUserStatus.LongAgo
}
