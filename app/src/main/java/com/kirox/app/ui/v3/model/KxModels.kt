package com.kirox.app.ui.v3.model

import androidx.compose.runtime.Immutable

/*
 * kiroX v3 UI models: what the new screens draw. They hold display-ready values only (texts,
 * times already formatted, Russian plurals done), so the screens stay free of TDLib. Two stores
 * fill them: KxDemoStore (demo data: the com.kirox.app.v3 preview app and screenshot tests) and
 * the TDLib store in ui/v3/data (real account).
 *
 * Conventions shared by both stores:
 * - times: "HH:mm" today, «вчера», weekday «пн» this week, «12 нояб.» this year, "dd.MM.yy" older;
 * - last seen: «в сети» only when online, otherwise Telegram wording «был(а) недавно», «был(а) вчера в 21:14»;
 * - colorIndex: Telegram accent color id 0..6 for the initials circle (-1 = derive from id);
 * - avatar / media paths: local file paths, null while not downloaded (initials / placeholder shown).
 */

enum class KxChatKind { Private, Group, Channel, Saved, Bot, Secret }
enum class KxOutState { None, Sending, Sent, Read, Failed }

@Immutable
data class KxChat(
    val id: Long,
    val title: String,
    val kind: KxChatKind = KxChatKind.Private,
    /** Last message preview, already shortened ("🎤 Голосовое сообщение · 0:24"). */
    val preview: String = "",
    /** First name in groups, «Вы» for own messages, null in private incoming / channels / Saved. */
    val previewSender: String? = null,
    val time: String = "",
    val unread: Int = 0,
    val unreadMention: Boolean = false,
    val unreadReaction: Boolean = false,
    /** Marked as unread by the user: badge without a number. */
    val markedUnread: Boolean = false,
    val muted: Boolean = false,
    val pinned: Boolean = false,
    val online: Boolean = false,
    val verified: Boolean = false,
    val premium: Boolean = false,
    val scam: Boolean = false,
    val fake: Boolean = false,
    /** «печатает», «Анна печатает», «записывает голосовое»… (without dots). */
    val typing: String? = null,
    val out: KxOutState = KxOutState.None,
    val hasStory: Boolean = false,
    val archived: Boolean = false,
    val draft: String? = null,
    val avatar: String? = null,
    val colorIndex: Int = -1,
    /** Channel / group without a photo can show an icon instead of the letter (demo «Новости» → campaign). */
    val avatarIcon: String? = null,
    val hasScheduled: Boolean = false,
    val forum: Boolean = false,
    /** Group/channel creator may delete it for everyone. */
    val canDeleteForAll: Boolean = false,
)

/** id 0 = main list «Все чаты»; other ids are TDLib chatFolderId values (positive). */
@Immutable
data class KxFolder(val id: Int, val title: String, val unread: Int = 0,
                    /** TDLib ChatFolderIcon name («Work», «Love», «Custom»…), empty = default icon. */
                    val icon: String = "", val chatCount: Int = 0)

@Immutable
data class KxStoryPeer(val id: Long, val name: String, val unseen: Boolean = true, val mine: Boolean = false, val avatar: String? = null, val colorIndex: Int = -1,
                       /** «15 мин назад» of the newest story. */
                       val time: String = "", val slides: List<KxStorySlide> = emptyList())

/** One story. [media] is a local photo/video path; [look] picks a demo background when there is no media. */
@Immutable
data class KxStorySlide(val id: Int, val caption: String = "", val media: String? = null, val video: Boolean = false, val look: Int = 0)

enum class KxMsgKind { Text, Photo, Video, Gif, Voice, VideoNote, Sticker, File, Music, Location, Poll, Contact, Dice, Service, Unsupported }

@Immutable
data class KxReaction(val emoji: String, val count: Int, val mine: Boolean = false)

@Immutable
data class KxReply(val sender: String, val text: String, val messageId: Long = 0)

enum class KxEntityType { Bold, Italic, Underline, Strike, Code, Pre, Spoiler, Link, TextLink, Mention, Hashtag, CustomEmoji }

/** Text formatting; offsets/lengths in UTF-16 units like TDLib. */
@Immutable
data class KxEntity(val offset: Int, val length: Int, val type: KxEntityType, val url: String? = null)

@Immutable
data class KxMessage(
    val id: Long,
    val out: Boolean,
    val text: String = "",
    val time: String = "",
    val kind: KxMsgKind = KxMsgKind.Text,
    val status: KxOutState = KxOutState.None,
    val sender: String? = null,
    val senderId: Long = 0,
    val senderColorIndex: Int = -1,
    val reply: KxReply? = null,
    val reactions: List<KxReaction> = emptyList(),
    val entities: List<KxEntity> = emptyList(),
    val edited: Boolean = false,
    val forwardedFrom: String? = null,
    /** Voice/video length "0:24", file size "2,4 МБ", etc. */
    val meta: String = "",
    val fileName: String = "",
    /** 0..1 bars of a voice message. */
    val waveform: List<Float> = emptyList(),
    /** Played part of a voice message, 0..1. */
    val played: Float = 0f,
    /** Date separator shown above this message ("Сегодня"). */
    val dayHeader: String? = null,
    /** «Непрочитанные сообщения» separator above this message. */
    val unreadSeparator: Boolean = false,
    // Media
    val media: String? = null,
    val thumb: String? = null,
    val minithumb: ByteArray? = null,
    val aspect: Float = 0f,
    /** TDLib file id to download on demand (0 = none). */
    val fileId: Int = 0,
    /** Consecutive messages with the same non-zero albumId are drawn as one album bubble. */
    val albumId: Long = 0,
    // Channel posts / bots
    val views: String = "",
    val signature: String = "",
    val comments: Int = 0,
    val buttons: List<List<String>> = emptyList(),
    // What the menu may offer (filled from GetMessageProperties by the TDLib store)
    val canEdit: Boolean = out,
    val canDeleteForAll: Boolean = out,
    val canDeleteForMe: Boolean = true,
    val canForward: Boolean = true,
    val canCopy: Boolean = true,
    val canPin: Boolean = true,
    val pinned: Boolean = false,
    /** Shown instead of the text after «Перевести». */
    val translation: String? = null,
    /** Poll / quiz of a [KxMsgKind.Poll] message. */
    val poll: KxPoll? = null,
)

/** What the bottom of a chat shows instead of / as the composer. */
enum class KxComposer { Write, Join, Mute, Unmute, Blocked, Restricted }

@Immutable
data class KxChatHeader(
    val chatId: Long, val title: String, val status: String, val kind: KxChatKind = KxChatKind.Private,
    val online: Boolean = false,
    /** «печатает», «Анна и Максим печатают»… null when nobody types. */
    val typing: String? = null,
    val muted: Boolean = false, val members: Int = 0,
    val pinned: KxReply? = null,
    val composer: KxComposer = KxComposer.Write,
    val avatar: String? = null, val colorIndex: Int = -1,
    val canDeleteForAll: Boolean = false,
)

@Immutable
data class KxProfile(val id: Long, val name: String, val username: String = "", val phone: String = "", val bio: String = "",
                     val status: String = "в сети", val premium: Boolean = false, val avatar: String? = null, val colorIndex: Int = -1,
                     /** «12.03» / «12.03.1998», empty when not set. */
                     val birthday: String = "")

/** Peer profile (board «Профиль»). Counts feed the shared-media tabs. */
@Immutable
data class KxPeer(
    val id: Long, val chatId: Long, val name: String, val status: String, val online: Boolean = false, val premium: Boolean = false,
    val bio: String = "", val username: String = "", val phone: String = "", val birthday: String = "",
    val gifts: Int = 0, val commonGroups: Int = 0, val muted: Boolean = false, val avatar: String? = null, val colorIndex: Int = -1,
    val isContact: Boolean = true, val blocked: Boolean = false,
    val counts: Map<KxSharedTab, Int> = emptyMap(),
)

enum class KxSharedTab(val label: String) { Media("Медиа"), Files("Файлы"), Music("Музыка"), Links("Ссылки"), Voice("Голосовые"), Gif("GIF") }

@Immutable
data class KxSharedItem(val id: Long, val title: String = "", val sub: String = "", val media: String? = null, val video: String? = null,
                        val playing: Boolean = false, val look: Int = 0, val fileExt: String = "",
                        /** Message of the item (forward / delete / «В чате»), and its month for grouping («Ноябрь 2026»). */
                        val messageId: Long = 0, val month: String = "", val fileId: Int = 0)

enum class KxCallDir { Incoming, Outgoing, Missed, Declined }

@Immutable
data class KxCall(val id: Long, val peerId: Long, val name: String, val time: String, val dir: KxCallDir, val video: Boolean = false,
                  val duration: String = "", val count: Int = 1, val avatar: String? = null, val colorIndex: Int = -1,
                  /** Group voice/video chat («Видеочат»). */
                  val group: Boolean = false)

@Immutable
data class KxContact(val id: Long, val name: String, val status: String, val online: Boolean = false, val hasStory: Boolean = false,
                     val avatar: String? = null, val colorIndex: Int = -1)

@Immutable
data class KxSession(val id: Long, val device: String, val app: String, val place: String, val time: String, val current: Boolean = false,
                     /** Signed in but waiting for the cloud password («ожидает пароль»). */
                     val passwordPending: Boolean = false, val official: Boolean = true)

/** Bottom dock tabs, in the mockup order. */
enum class KxTab(val label: String) { Chats("Чаты"), Calls("Звонки"), Stories("Истории"), Settings("Настройки") }

// ---- Round 5 (binder, additive): settings, accounts, search, chat tools ----

@Immutable
data class KxPollOption(val text: String, val percent: Int = 0, val voters: Int = 0, val chosen: Boolean = false, val correct: Boolean = false)

@Immutable
data class KxPoll(
    val question: String, val options: List<KxPollOption>, val total: Int = 0, val closed: Boolean = false,
    val multiple: Boolean = false, val quiz: Boolean = false, val anonymous: Boolean = true, val canVote: Boolean = true, val voted: Boolean = false,
)

/** One privacy item. [level]: 0 = Все, 1 = Мои контакты, 2 = Никто; [levels] are the ones Telegram allows for it. */
@Immutable
data class KxPrivacyRule(val key: String, val title: String, val level: Int, val exceptions: Int = 0, val levels: List<Int> = listOf(0, 1, 2))

/** Notification defaults of one chat type ([key]: "private" / "groups" / "channels"). */
@Immutable
data class KxNotifyScope(val key: String, val title: String, val enabled: Boolean, val preview: Boolean = true, val exceptions: Int = 0)

/** Storage usage: [byType] title → bytes («Фото», «Видео»…), [chats] title → bytes, biggest first. */
@Immutable
data class KxStorage(val totalBytes: Long, val databaseBytes: Long = 0, val byType: List<Pair<String, Long>> = emptyList(),
                     val chats: List<Pair<String, Long>> = emptyList(), val loadingDetails: Boolean = false)

@Immutable
data class KxCloudPassword(val has: Boolean, val hint: String = "", val hasRecoveryEmail: Boolean = false, val pendingEmailPattern: String? = null)

/** A Telegram gift for stars. Sending one spends stars: the UI must show [stars] and ask before sending. */
@Immutable
data class KxGift(val id: Long, val emoji: String, val stars: Long, val soldOut: Boolean = false, val sticker: String? = null)

/** A message found by global search. */
@Immutable
data class KxFound(val chatId: Long, val messageId: Long, val chatTitle: String, val text: String, val time: String, val avatar: String? = null)

/** Forum topic of a supergroup. */
@Immutable
data class KxTopic(val id: Int, val title: String, val unread: Int = 0, val pinned: Boolean = false, val closed: Boolean = false,
                   val preview: String = "", val time: String = "", val general: Boolean = false)

/** Sticker or GIF; [id] is what sendSticker takes. [path] is null until downloaded (then [fileId] can be passed to download). */
@Immutable
data class KxSticker(val id: String, val emoji: String = "", val path: String? = null, val fileId: Int = 0, val animated: Boolean = false, val gif: Boolean = false)

@Immutable
data class KxStickerSet(val id: Long, val title: String, val cover: KxSticker? = null)

/** Saved Messages tag (Premium): reaction emoji, own name, how many messages carry it. */
@Immutable
data class KxSavedTag(val emoji: String, val label: String, val count: Int)

@Immutable
data class KxScheduled(val id: Long, val text: String, val time: String)

enum class KxUsernameState { Free, Taken, Invalid, Purchasable, TooMany }

// ---- Round 5b (binder, additive): groups & channels, proxies, profile ----

/** Facts and rights of a group or channel for its settings boards. [reactions]: null = all allowed, empty = none. */
@Immutable
data class KxGroupInfo(
    val chatId: Long, val title: String, val description: String = "", val username: String = "", val isChannel: Boolean = false,
    val isPublic: Boolean = false, val slowModeSeconds: Int = 0, val joinByRequest: Boolean = false, val reactions: List<String>? = null,
    val stickerSetId: Long = 0, val protectedContent: Boolean = false, val members: Int = 0,
    val owner: Boolean = false, val canChangeInfo: Boolean = false, val canRestrict: Boolean = false, val canPromote: Boolean = false, val canInvite: Boolean = false,
)

/** [role]: «Создатель», «Администратор», «Только чтение», «Заблокирован» or empty for a member. */
@Immutable
data class KxMember(val userId: Long, val name: String, val role: String = "", val status: String = "", val avatar: String? = null, val canEdit: Boolean = false)

@Immutable
data class KxInviteLink(val link: String, val name: String = "", val primary: Boolean = false, val members: Int = 0, val expires: String = "", val limit: Int = 0)

@Immutable
data class KxJoinRequest(val userId: Long, val name: String, val bio: String = "", val time: String = "", val avatar: String? = null)

@Immutable
data class KxLogEntry(val id: Long, val time: String, val who: String, val text: String)

@Immutable
data class KxBoostStatus(val level: Int, val boosts: Int, val levelStart: Int, val nextLevel: Int, val link: String = "", val premiumMembers: Int = 0)

/** Statistics: rows of title, current and previous value. */
@Immutable
data class KxStats(val isChannel: Boolean, val period: String, val rows: List<Triple<String, Double, Double>>, val notificationsPercent: Double? = null)

@Immutable
data class KxPostStats(val views: Int, val forwards: Int, val reactions: Int, val comments: Int)

/** [type]: "SOCKS5", "HTTP" or "MTProto". */
@Immutable
data class KxProxy(val id: Int, val server: String, val port: Int, val type: String, val enabled: Boolean = false)
