package com.kirox.app.telegram

sealed interface TelegramAuthState {
    data object Initializing : TelegramAuthState
    data object WaitPhoneNumber : TelegramAuthState
    data class WaitCode(val phoneNumber: String, val length: Int, val canResend: Boolean, val timeout: Int, val sentTo: CodeDestination = CodeDestination.Other, val words: Boolean = false) : TelegramAuthState
    data class WaitPassword(val hint: String, val hasRecoveryEmail: Boolean, val recoveryEmailPattern: String) : TelegramAuthState
    data object WaitEmailAddress : TelegramAuthState
    data class WaitEmailCode(val length: Int, val emailPattern: String = "") : TelegramAuthState
    data class WaitOtherDevice(val link: String) : TelegramAuthState
    data class WaitRegistration(val termsOfService: String, val minUserAge: Int) : TelegramAuthState
    data object Ready : TelegramAuthState
    data object Closing : TelegramAuthState
    data class Blocked(val message: String) : TelegramAuthState
}

enum class TelegramConnection { WaitingForNetwork, ConnectingToProxy, Connecting, Updating, Ready }

/** A Telegram file reference for lazy download; [localPath] is set when already on disk. */
data class TelegramFileRef(val id: Int, val localPath: String?)

enum class LastMessageState { None, Sent, Read }

/** What a chat is, from TDLib's chat type: used by the local Личные / Группы / Каналы filters. */
enum class TelegramChatKind { Private, Group, Channel }

data class TelegramChatItem(
    val id: Long,
    val title: String,
    val preview: String,
    val timestamp: Int,
    val unreadCount: Int,
    val unreadMentionCount: Int = 0,
    val isMarkedUnread: Boolean = false,
    val isPinned: Boolean,
    val isMuted: Boolean = false,
    val draft: String? = null,
    val outgoingState: LastMessageState = LastMessageState.None,
    val photo: TelegramFileRef?,
    val archived: Boolean,
    /** The user's own chat (Saved Messages). */
    val isSaved: Boolean = false,
    val kind: TelegramChatKind = TelegramChatKind.Private,
    // Extra list facts for the v3 UI (ui/v3/data); the old list does not read them.
    /** Who wrote the last message: "Вы" for own ones, the member's first name in groups; null in private chats and channels. */
    val lastSender: String? = null,
    /** Delivery of the last own message, including sending and failed. */
    val lastDelivery: TelegramDelivery = TelegramDelivery.None,
    /** What the last message is, to draw "🎤 Голосовое сообщение · 0:24"-style previews. */
    val lastKind: TelegramMessageKind = TelegramMessageKind.Text,
    /** Seconds of the last message when it is a voice or video message; 0 otherwise. */
    val lastDuration: Int = 0,
    /** The other person of a private or secret chat; 0 for groups and channels. */
    val peerUserId: Long = 0,
    val isBot: Boolean = false,
    val isSecret: Boolean = false,
    val isVerified: Boolean = false,
    /** The other person of a private chat is online right now. */
    val isOnline: Boolean = false,
    val unreadReactionCount: Int = 0,
    /** Muted, with "use default" resolved from the scope settings (the old [isMuted] treats it as unmuted). */
    val isMutedResolved: Boolean = false,
    /** Telegram accent color id (0..6 are the classic initials colours). */
    val accentColorId: Int = -1,
    val isPremium: Boolean = false,
    val isScam: Boolean = false,
    val isFake: Boolean = false,
    val hasScheduled: Boolean = false,
    /** Forum supergroup (topics). */
    val isForum: Boolean = false,
    /** Group/channel the user may delete for everyone (creator). */
    val canDeleteForAll: Boolean = false,
    /** Last message is a service one: its line («Анна вступил(а) в группу»); then [lastSender] is null. */
    val lastServiceText: String? = null,
    /** Question of a poll that is the last message. */
    val lastPollQuestion: String? = null
)

sealed interface TelegramChatListKey {
    data object Main : TelegramChatListKey
    data object Archive : TelegramChatListKey
    data class Folder(val id: Int) : TelegramChatListKey
}

/** [icon]: TDLib ChatFolderIcon name («Work», «Love»…; empty when none) — used by the v3 folders board. */
data class TelegramFolder(val id: Int, val title: String, val icon: String = "")

data class TelegramChatListState(
    val chats: List<TelegramChatItem> = emptyList(),
    val loading: Boolean = false,
    val complete: Boolean = false
)

data class TelegramChatLists(
    val main: TelegramChatListState = TelegramChatListState(),
    val archive: TelegramChatListState = TelegramChatListState(),
    val folders: List<TelegramFolder> = emptyList(),
    val folderChats: Map<Int, TelegramChatListState> = emptyMap()
) {
    operator fun get(key: TelegramChatListKey): TelegramChatListState = when (key) {
        TelegramChatListKey.Main -> main
        TelegramChatListKey.Archive -> archive
        is TelegramChatListKey.Folder -> folderChats[key.id] ?: TelegramChatListState()
    }
}

sealed interface TelegramUserStatus {
    data object Online : TelegramUserStatus
    data class Offline(val wasOnline: Int) : TelegramUserStatus
    data object Recently : TelegramUserStatus
    data object LastWeek : TelegramUserStatus
    data object LastMonth : TelegramUserStatus
    data object LongAgo : TelegramUserStatus
}

data class TelegramContact(
    val id: Long,
    val name: String,
    val username: String?,
    val status: TelegramUserStatus,
    val photo: TelegramFileRef?,
    /** Digits only, without "+"; empty when hidden. */
    val phone: String = ""
)

enum class TelegramMessageKind { Text, Photo, Video, Animation, VideoNote, Voice, Audio, Document, Sticker, Other }

/** A message's main file and what is known about it before it is downloaded. */
data class TelegramMedia(
    val file: TelegramFileRef,
    /** Bytes; 0 when unknown. */
    val size: Long = 0,
    /** Seconds. */
    val duration: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "",
    /** Music title, or the file name for documents. */
    val title: String = "",
    val performer: String = "",
    /** Sticker formats other than static WEBP are animated and drawn from their thumbnail for now. */
    val animatedSticker: Boolean = false,
    /** Voice note loudness, 0–31 per bar, as Telegram sends it; empty when unknown. */
    val waveform: List<Int> = emptyList(),
    /** TGS (Lottie) sticker; WEBM ones are video stickers. */
    val lottieSticker: Boolean = false
)

/** Formatting of a message text; offsets and lengths are in UTF-16 units, as in Kotlin strings. */
data class TelegramTextEntity(val offset: Int, val length: Int, val type: TelegramEntityType, val url: String? = null)

/** [CustomEmoji] keeps the emoji's id in TelegramTextEntity.url. */
enum class TelegramEntityType { Bold, Italic, Underline, Strike, Code, Pre, Spoiler, Url, TextUrl, Mention, MentionName, Hashtag, Email, Phone, BotCommand, Quote, CustomEmoji }
enum class TelegramDelivery { None, Sending, Sent, Read, Failed }

data class TelegramMessage(
    val id: Long,
    val chatId: Long,
    val senderName: String,
    val isOutgoing: Boolean,
    val kind: TelegramMessageKind,
    val text: String,
    val date: Int,
    val edited: Boolean,
    val delivery: TelegramDelivery,
    val replyAuthor: String? = null,
    val replyText: String? = null,
    val forwardedFrom: String? = null,
    val photo: TelegramFileRef? = null,
    val photoAspect: Float = 0f,
    val fileName: String? = null,
    val canEdit: Boolean = false,
    /** Emoji reactions: emoji, count, whether this account chose it. */
    val reactions: List<Triple<String, Int, Boolean>> = emptyList(),
    /** File of a video, GIF, video note, music, document or sticker. */
    val media: TelegramMedia? = null,
    /** Preview image of [media], when Telegram provides one. */
    val thumbnail: TelegramFileRef? = null,
    val entities: List<TelegramTextEntity> = emptyList(),
    /** Messages of one album share this id; 0 when not in an album. */
    val albumId: Long = 0,
    /** Comments under a channel post; null when the post has no comments. */
    val commentCount: Int? = null,
    /** Views of a channel post; null outside channels. */
    val viewCount: Int? = null,
    val poll: TelegramPoll? = null,
    /** Inline buttons under a bot message, by rows. */
    val buttons: List<List<TelegramButton>> = emptyList(),
    /** Message this one answers, in the same chat; 0 when none. */
    val replyToMessageId: Long = 0,
    /** Deleted by its author while the chat was open; kept on screen with a mark («Сохранять удалённые»). */
    val deleted: Boolean = false,
    /** «Запрет копирования» in the chat: no forwarding, saving or copying. */
    val isProtected: Boolean = false,
    /** Who sent it: a user id, or a chat id (negative) for channels and anonymous admins. */
    val senderId: Long = 0,
    /** Service message ("Анна добавила Олега", "Группа создана"): its line; null for ordinary messages. Read by the v3 UI. */
    val serviceText: String? = null,
    /** Photo/video/GIF sent «под спойлером»: hidden until tapped. Read by the v3 UI. */
    val hasSpoiler: Boolean = false,
    /** View-once / self-destructing media (selfDestructType, secret photo/video): opened once, kept out of galleries. Read by the v3 UI. */
    val selfDestruct: Boolean = false
)

/** A poll or quiz in a message. */
data class TelegramPoll(
    val question: String,
    val options: List<TelegramPollOption>,
    val totalVoters: Int,
    val isClosed: Boolean,
    val multipleAnswers: Boolean,
    val isQuiz: Boolean,
    val isAnonymous: Boolean,
    /** Whether this account may still vote (not closed, not restricted, not voted unless revoting). */
    val canVote: Boolean,
    val voted: Boolean
)

data class TelegramPollOption(val text: String, val percent: Int, val voters: Int, val chosen: Boolean, val correct: Boolean)

/** A button under a bot message. */
data class TelegramButton(val text: String, val kind: Kind, val url: String? = null, val data: ByteArray? = null) {
    enum class Kind { Callback, Url, WebApp, Other }
    override fun equals(other: Any?): Boolean = other is TelegramButton && other.text == text && other.kind == kind && other.url == url && other.data.contentEqualsNullable(data)
    override fun hashCode(): Int = (text.hashCode() * 31 + kind.hashCode()) * 31 + (url?.hashCode() ?: 0)
}

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean = if (this == null) other == null else other != null && contentEquals(other)

/** A message found by the global search, with its chat for the result row. */
data class TelegramFoundMessage(val chatId: Long, val messageId: Long, val chatTitle: String, val text: String, val date: Int, val photo: TelegramFileRef?)

enum class TelegramPeerKind { User, Bot, Group, Channel, Saved, Unknown }

data class TelegramChatHistory(
    val chatId: Long = 0,
    val title: String = "",
    val photo: TelegramFileRef? = null,
    val peerKind: TelegramPeerKind = TelegramPeerKind.Unknown,
    val peerStatus: TelegramUserStatus? = null,
    val memberCount: Int = 0,
    /** Oldest first. */
    val messages: List<TelegramMessage> = emptyList(),
    val loadingOlder: Boolean = false,
    val reachedStart: Boolean = false,
    /** End-to-end encrypted secret chat; its chat id is not the peer's user id. */
    val isSecret: Boolean = false,
    /** Unsent text saved in Telegram for this chat (synced with other devices), empty if none. */
    val draft: String = "",
    /** Newest pinned message, for the bar above the history. */
    val pinned: TelegramMessage? = null,
    val pinnedIds: Set<Long> = emptySet(),
    /** Loaded message the screen should scroll to; 0 when none (see TelegramMessageRepository.jumpTo). */
    val jumpTo: Long = 0,
    /** Set for a comment thread or forum topic: shown as the header title instead of the chat. */
    val threadTitle: String? = null,
    /** Comment thread shown (its id in the discussion group); 0 otherwise. */
    val threadId: Long = 0,
    /** Forum topic shown; 0 otherwise. */
    val forumTopicId: Int = 0,
    /** Forum supergroup opened as a whole: the screen shows its topics. */
    val isForum: Boolean = false,
    /** Custom keyboard a bot sent (rows of button texts); empty when none. */
    val replyKeyboard: List<List<String>> = emptyList(),
    /** Published from the chat's real data; false for placeholders while it opens (draft not known yet). */
    val loaded: Boolean = false
)

/** A topic of a forum supergroup. */
data class TelegramForumTopic(
    val id: Int,
    val title: String,
    val isClosed: Boolean,
    val isPinned: Boolean,
    val unreadCount: Int,
    val preview: String,
    val date: Int,
    val isGeneral: Boolean
)

enum class TelegramCallKind { Incoming, Outgoing, Missed, Unanswered }

data class TelegramCall(
    val messageId: Long,
    val chatId: Long,
    val peerName: String,
    val photo: TelegramFileRef?,
    val kind: TelegramCallKind,
    val isVideo: Boolean,
    val durationSeconds: Int,
    val date: Int,
    /** A group call not bound to a chat (MessageGroupCall), e.g. a 1:1 call upgraded to a conference. */
    val isGroup: Boolean = false
)

data class TelegramCallsState(
    val calls: List<TelegramCall> = emptyList(),
    val loading: Boolean = false,
    val complete: Boolean = false
)

data class TelegramStoryChat(
    val chatId: Long,
    val title: String,
    val photo: TelegramFileRef?,
    val storyIds: List<Int>,
    val hasUnread: Boolean,
    val isSelf: Boolean,
    /** Unix time of the newest active story (v3 «15 мин назад»); 0 when unknown. */
    val newestDate: Int = 0
)

data class TelegramStory(
    val id: Int,
    val chatId: Long,
    val date: Int,
    val image: TelegramFileRef?,
    val aspect: Float,
    val isVideo: Boolean,
    val caption: String,
    /** Video file of a video story. */
    val video: TelegramFileRef? = null,
    /** Seconds; shown time of a video story. */
    val durationS: Double = 0.0,
    val canReply: Boolean = false,
    val canDelete: Boolean = false,
    /** Own story: views (and who viewed) can be shown. */
    val canSeeViews: Boolean = false,
    val viewCount: Int = 0,
    /** Own story: reactions left by viewers. */
    val reactionCount: Int = 0,
    /** Emoji reaction this account set, or null. */
    val chosenReaction: String? = null
)

/** Someone who viewed an own story, with the reaction they left. */
data class TelegramStoryViewer(val name: String, val photo: TelegramFileRef?, val date: Int, val reaction: String?, val userId: Long = 0)

/** What the bottom of a chat shows (v3 UI): composer, «Вступить», channel reader, restricted, blocked user, bot not started. */
enum class TelegramWriteAccess { Write, Join, ReadOnly, Restricted, Blocked, StartBot }

/** What may be done with one message (TDLib GetMessageProperties), for the v3 message menu. */
data class TelegramMessageRights(
    val canEdit: Boolean, val canDeleteForAll: Boolean, val canDeleteForMe: Boolean,
    val canForward: Boolean, val canCopy: Boolean, val canPin: Boolean
)

data class TelegramUserProfile(
    val id: Long,
    val name: String,
    val username: String?,
    val phoneNumber: String,
    val bio: String = "",
    val photo: TelegramFileRef?,
    val isPremium: Boolean = false,
    val isVerified: Boolean = false,
    /** "d.MM" or "d.MM.yyyy", empty when hidden or not set. */
    val birthday: String = "",
    val firstName: String = "",
    val lastName: String = "",
    /** Custom emoji next to the name (Telegram Premium); 0 when none. */
    val emojiStatusId: Long = 0
)

/** Where Telegram sent the login code. */
enum class CodeDestination { App, Sms, Call, Fragment, Other }

/** A message waiting to be sent; [sendAt] 0 — when the other person comes online. */
data class ScheduledMessage(val id: Long, val text: String, val sendAt: Int)

/** sendText's scheduleAt: send when the other person comes online. */
const val SEND_WHEN_ONLINE = -1

/** One answer of an inline bot, as a row above the composer. */
data class InlineResult(val id: String, val title: String, val description: String)

data class InlineResults(val queryId: Long, val results: List<InlineResult>)

/** A running bot mini app: close it with its [launchId] when the sheet goes away. */
data class WebAppLaunch(val launchId: Long, val url: String)

/** The «≡ Меню» button of a bot chat, which opens the bot's mini app. */
data class BotMenu(val botUserId: Long, val text: String, val url: String)

/** A folder someone shared by a t.me/addlist link: its name and the chats you are not in yet. */
data class FolderInvite(val link: String, val title: String, val newChats: List<Pair<Long, String>>, val alreadyAdded: Int)

/** A tag of Saved Messages (Premium): the reaction that marks messages, its own name and how many. */
data class SavedTag(val emoji: String, val label: String, val count: Int)

/** A gift shown in a profile: its picture, price in stars and who gave it. */
/** A gift from Telegram's catalog: its sticker, price in Stars, and whether it is sold out. */
data class GiftOption(val id: Long, val sticker: TelegramFileRef?, val emoji: String, val stars: Long, val soldOut: Boolean)

data class ProfileGift(val sticker: TelegramFileRef?, val emoji: String, val stars: Long, val from: String, val title: String)
