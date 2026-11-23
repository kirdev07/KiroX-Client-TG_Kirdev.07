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
    /** wave5: media sent under a spoiler — blurred until tapped. */
    val spoiler: Boolean = false,
    /** wave5: view-once / self-destructing media — a cover in the chat, opened once in a protected view. */
    val viewOnce: Boolean = false,
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

// wave3-media ---------------------------------------------------------------------------------

/** A photo or video of a chat (viewer, album, video player). [media]: full photo / downloaded video path. */
@Immutable
data class KxMediaItem(
    val chatId: Long, val messageId: Long, val video: Boolean = false,
    val media: String? = null, val thumb: String? = null, val fileId: Int = 0,
    /** Seconds of a video. */
    val duration: Int = 0, val aspect: Float = 0f, val size: Long = 0,
    val sender: String = "", val time: String = "", val caption: String = "", val albumId: Long = 0,
    val canDelete: Boolean = false, val canDeleteForAll: Boolean = false, val canForward: Boolean = true, val canSave: Boolean = true,
    /** Video chapters from caption timecodes («0:00 Вступление»): (second, title). */
    val chapters: List<Pair<Int, String>> = emptyList(),
)

/** Download state of one file; [path] once the whole file is on the phone. */
@Immutable
data class KxFileState(val path: String? = null, val downloaded: Long = 0, val total: Long = 0, val downloading: Boolean = false) {
    val fraction: Float get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
}

/** «Проверка файлов» result before a file is opened: [level] 0 safe, 1 caution, 2 danger; [installer] = an app installer (APK…). */
@Immutable
data class KxFileCheck(val level: Int, val reasons: List<String> = emptyList(), val kind: String = "", val installer: Boolean = false)

/** A music file sent in some chat. */
@Immutable
data class KxTrack(val chatId: Long, val messageId: Long, val title: String, val performer: String = "", val chatTitle: String = "",
                   val duration: Int = 0, val fileId: Int = 0, val size: Long = 0, val cover: String? = null, val date: String = "")

/** The app's music / voice player. [repeat]: 0 off, 1 all, 2 one. */
@Immutable
data class KxPlayerState(val track: KxTrack? = null, val playing: Boolean = false, val positionMs: Long = 0, val durationMs: Long = 0,
                         val shuffle: Boolean = false, val repeat: Int = 0, val loading: Boolean = false, val source: String = "",
                         /** wave5: local file the player has loaded (a voice message or a track); null when idle. */
                         val path: String? = null,
                         val queue: List<KxTrack> = emptyList())

/** A file in Telegram's download list (SearchFileDownloads). */
@Immutable
data class KxDownload(val fileId: Int, val chatId: Long, val messageId: Long, val name: String, val kind: KxMsgKind, val size: Long,
                      val downloaded: Long = 0, val paused: Boolean = false, val done: Boolean = false, val path: String? = null,
                      val thumb: String? = null, val chatTitle: String = "", val time: String = "", val addedAt: Int = 0)

/** A voice (or round video) message of a chat. */
@Immutable
data class KxVoiceItem(val chatId: Long, val messageId: Long, val sender: String, val out: Boolean, val time: String, val seconds: Int,
                       val waveform: List<Float> = emptyList(), val listened: Boolean = true, val fileId: Int = 0, val path: String? = null,
                       val transcript: String? = null, val video: Boolean = false, val date: Int = 0)

/** An own story (archive / profile). [month] for grouping («Ноябрь 2026»). */
@Immutable
data class KxStoryItem(val id: Int, val date: String, val media: String? = null, val video: Boolean = false, val caption: String = "",
                       val views: Int = 0, val reactions: Int = 0, val onProfile: Boolean = false, val active: Boolean = false,
                       val month: String = "", val unix: Int = 0, val fileId: Int = 0)

/** A photo or video on the phone (MediaStore). [uri]: content uri as text. */
@Immutable
data class KxDeviceMedia(val uri: String, val video: Boolean, val durationMs: Long = 0)

enum class KxCallPhase { Calling, Ringing, Incoming, Connecting, Active, Ended, Failed }

/** The current one-to-one call. [sinceMs]: System.currentTimeMillis when media started (0 = not yet). */
@Immutable
data class KxCallState(val id: Int, val peerId: Long, val name: String, val avatar: String? = null, val video: Boolean = false,
                       val outgoing: Boolean = true, val phase: KxCallPhase = KxCallPhase.Calling, val sinceMs: Long = 0,
                       val endedSeconds: Int = 0, val emojis: List<String> = emptyList(), val micMuted: Boolean = false,
                       val cameraOn: Boolean = false, val remoteVideo: Boolean = false, val remoteMuted: Boolean = false,
                       val signalBars: Int = 4, val reconnecting: Boolean = false, val endReason: String = "", val failure: String = "")

@Immutable
data class KxCallMember(val key: String, val name: String, val avatar: String? = null, val self: Boolean = false, val speaking: Boolean = false,
                        val muted: Boolean = true, val hand: Boolean = false, val video: Boolean = false)

/** The video chat this account is in. */
@Immutable
data class KxGroupCallState(val chatId: Long, val title: String, val members: List<KxCallMember> = emptyList(), val count: Int = 0,
                            val muted: Boolean = true, val canUnmute: Boolean = true, val cameraOn: Boolean = false,
                            val connected: Boolean = false, val reconnecting: Boolean = false, val failure: String = "")

/** Traffic since [since]; [network] 0 mobile, 1 Wi-Fi, 2 roaming; [category] photo/video/voice/music/files/messages/calls/other. */
@Immutable
data class KxTrafficRow(val network: Int, val category: String, val sent: Long, val received: Long)
@Immutable
data class KxTraffic(val since: String, val rows: List<KxTrafficRow> = emptyList())

/** Result of a network check: [pingMs] to Telegram (null = no answer). */
@Immutable
data class KxNetCheck(val online: Boolean, val network: String, val connection: String, val pingMs: Int? = null, val proxy: String? = null,
                      val proxyPingMs: Int? = null, val dnsOk: Boolean? = null)

/** A notification sound: id 0 = no sound, 1 = default; others are Telegram's saved sounds. */
@Immutable
data class KxSound(val id: Long, val title: String, val seconds: Int = 0, val fileId: Int = 0)

/** Text or a human error (transcription). */
@Immutable
data class KxTextResult(val text: String? = null, val error: String? = null)

// wave3-account ---------------------------------------------------------------------------------

/** A Telegram chat folder for editing. [colorId] -1 = no colour; include/exclude flags as in TDLib. */
@Immutable
data class KxFolderInfo(
    val title: String, val icon: String = "", val colorId: Int = -1, val chatIds: List<Long> = emptyList(), val excludedIds: List<Long> = emptyList(),
    val contacts: Boolean = false, val nonContacts: Boolean = false, val groups: Boolean = false, val channels: Boolean = false, val bots: Boolean = false,
    val excludeMuted: Boolean = false, val excludeRead: Boolean = false, val excludeArchived: Boolean = false,
    /** Unread chats of the folder right now (preview). */
    val unread: Int = 0, val shareable: Boolean = false,
)

/** A channel that can be shown in the own profile. */
@Immutable
data class KxPersonalChannel(val chatId: Long, val title: String, val subscribers: Int = 0, val lastPost: String = "", val avatar: String? = null, val colorIndex: Int = -1)

/** [current] chat id shown in the profile (0 = none) and the channels Telegram allows. */
@Immutable
data class KxPersonalChannels(val current: Long = 0, val channels: List<KxPersonalChannel> = emptyList())

/** New terms of service: [id] for acceptTerms, [minAge] 0 = none. */
@Immutable
data class KxTerms(val id: String, val text: String, val minAge: Int = 0)

/** A Telegram accent colour: [colors] ARGB for the dark theme (1–3 stripes), [premium] = not a built-in one. */
@Immutable
data class KxAccent(val id: Int, val colors: List<Int>, val premium: Boolean = false)

/** An emoji status (custom emoji); [sticker] path when downloaded, [emoji] the plain fallback. */
@Immutable
data class KxEmojiStatus(val customEmojiId: Long, val emoji: String, val sticker: String? = null)

/** A country for the phone field: ISO [code], [name], [callingCode] without «+», [flag] emoji. */
@Immutable
data class KxCountry(val code: String, val name: String, val callingCode: String, val flag: String = "")

// wave3-chat (binder-chat) -----------------------------------------------------------------------

/** How a message goes out. [at]: 0 = now, unix time = scheduled, -1 = when the other person comes online.
 *  [effectId]: KxEffect.id (private chats). [previewLarge]: null = Telegram decides. */
@Immutable
data class KxSendOptions(
    val replyTo: Long? = null, val silent: Boolean = false, val at: Int = 0, val effectId: Long = 0,
    val entities: List<KxEntity> = emptyList(),
    val noPreview: Boolean = false, val previewAbove: Boolean = false, val previewLarge: Boolean? = null, val previewUrl: String = "",
)

/** Who a message can be sent as in a group/channel (yourself, the channel, an anonymous admin). */
@Immutable
data class KxSender(val id: Long, val name: String, val sub: String = "", val premiumOnly: Boolean = false, val selected: Boolean = false,
                    val avatar: String? = null, val channel: Boolean = false)

/** Message effect of private chats ([premium] = only with Premium). */
@Immutable
data class KxEffect(val id: Long, val emoji: String, val premium: Boolean = false)

/** Inline bot results: send one with sendInlineResult(chatId, [queryId], result.id). */
@Immutable
data class KxInlineResults(val queryId: Long, val bot: String, val results: List<KxInlineResult>, val nextOffset: String = "")

/** [kind]: "gif", "sticker", "photo", "video", "article", "other". [thumb] is a local path when downloaded. */
@Immutable
data class KxInlineResult(val id: String, val kind: String, val title: String = "", val sub: String = "", val thumb: String? = null)

/** Facts of one message for «Информация». [read]: «Прочитано сегодня в 14:02», empty when unknown; [readNote] explains why not. */
@Immutable
data class KxMsgInfo(val sent: String, val edited: String = "", val read: String = "", val readNote: String = "", val views: Int = 0,
                     val forwards: Int = 0, val via: String = "", val autoDeleteIn: Int = 0)

/** A member who read a message (small groups, last 7 days) and the reaction they left. */
@Immutable
data class KxViewer(val userId: Long, val name: String, val time: String, val reaction: String = "", val avatar: String? = null)

/** Reactions to offer for a message. Custom-emoji reactions are left out (they need their own picker). */
@Immutable
data class KxReactionPanel(val top: List<String>, val recent: List<String> = emptyList(), val popular: List<String> = emptyList(),
                           val premiumOnly: Set<String> = emptySet(), val customAllowed: Boolean = false)

@Immutable
data class KxLinkPreview(val url: String, val site: String, val title: String, val description: String = "", val hasLargeMedia: Boolean = false)

@Immutable
data class KxStickerSetInfo(val id: Long, val name: String, val title: String, val count: Int, val animated: Boolean = false,
                            val installed: Boolean = false, val archived: Boolean = false, val official: Boolean = false,
                            val stickers: List<KxSticker> = emptyList())

/** A gift on the own profile. [id] = receivedGiftId; [stars] = what converting gives (0 = cannot convert). */
@Immutable
data class KxReceivedGift(val id: String, val emoji: String, val from: String, val text: String = "", val date: String = "", val stars: Long = 0,
                          val saved: Boolean = false, val sticker: String? = null)

/** One step of Telegram's report flow: choose one of [options] (id, text), write [textNeeded] text, or [done]. */
@Immutable
data class KxReportStep(val done: Boolean = false, val title: String = "", val options: List<Pair<String, String>> = emptyList(),
                        val textNeeded: Boolean = false, val textOptional: Boolean = false, val optionId: String = "", val error: String? = null)

/** Notification settings of one chat. [muteFor]: seconds left, 0 = sound on; [mentions]/[pinned]: notify about them. */
@Immutable
data class KxChatNotify(val muteFor: Int, val preview: Boolean = true, val mentions: Boolean = true, val pinned: Boolean = true, val useDefault: Boolean = true)

@Immutable
data class KxChecklistTask(val id: Int, val text: String, val done: Boolean = false, val doneBy: String = "")

@Immutable
data class KxChecklist(val title: String, val tasks: List<KxChecklistTask>, val canAdd: Boolean = false, val canMark: Boolean = false)

/** A public channel / group (recommendations, search, similar). */
@Immutable
data class KxPublicChat(val id: Long, val title: String, val username: String = "", val members: Int = 0, val channel: Boolean = true,
                        val joined: Boolean = false, val avatar: String? = null, val verified: Boolean = false)

/** A contact's birthday; [daysLeft] 0 = today. [year] 0 = hidden. */
@Immutable
data class KxBirthday(val userId: Long, val name: String, val day: Int, val month: Int, val year: Int = 0, val daysLeft: Int = 0, val avatar: String? = null)

/** Own role and marks of a group/channel. [linkedChatId]: discussion group of a channel (0 = none). */
@Immutable
data class KxChatFacts(val creator: Boolean = false, val admin: Boolean = false, val verified: Boolean = false, val scam: Boolean = false,
                       val fake: Boolean = false, val linkedChatId: Long = 0)

/** kiroX-local chat settings (KxStore.prefs / putPref). */
object KxChatPrefs {
    /** Seconds a sent text waits so «Отменить» can take it back (0 = off). */
    const val UNDO_SEND = "c3.undoSend"
    /** 1 = ask before opening a link whose text and address differ. */
    const val LINK_WARN = "c3.linkWarn"
}
