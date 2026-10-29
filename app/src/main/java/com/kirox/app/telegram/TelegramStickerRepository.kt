package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi

/** A sticker or saved GIF in the panel: what to draw and what to send. */
data class TelegramPanelItem(
    /** TDLib file id, also used to send it again without uploading. */
    val fileId: Int,
    val media: TelegramMedia,
    val thumbnail: TelegramFileRef?,
    val isGif: Boolean = false
)

/** An installed sticker set: its tab in the panel. */
data class TelegramStickerSetInfo(val id: Long, val title: String, val cover: TelegramPanelItem?)

/**
 * Stickers and GIFs for the composer panel: installed sets, recently sent stickers and saved GIFs.
 * Results come straight from TDLib, which keeps them cached and up to date.
 */
class TelegramStickerRepository(private val client: TelegramClient) {
    suspend fun installedSets(): List<TelegramStickerSetInfo> = runCatching {
        client.query(TdApi.GetInstalledStickerSets(TdApi.StickerTypeRegular())).sets.map { set ->
            TelegramStickerSetInfo(set.id, set.title, set.covers?.firstOrNull()?.let(::stickerItem))
        }
    }.getOrDefault(emptyList())

    suspend fun set(id: Long): List<TelegramPanelItem> = runCatching {
        client.query(TdApi.GetStickerSet(id)).stickers.map(::stickerItem)
    }.getOrDefault(emptyList())

    suspend fun recent(): List<TelegramPanelItem> = runCatching {
        client.query(TdApi.GetRecentStickers(false)).stickers.map(::stickerItem)
    }.getOrDefault(emptyList())

    suspend fun savedGifs(): List<TelegramPanelItem> = runCatching {
        client.query(TdApi.GetSavedAnimations()).animations.map { a ->
            val (media, thumbnail) = mediaOf(TdApi.MessageAnimation(a, TdApi.FormattedText("", emptyArray()), false, false, false))
            TelegramPanelItem(a.animation.id, media!!, thumbnail, isGif = true)
        }
    }.getOrDefault(emptyList())

    private var gifBotId = 0L

    /** GIFs for [query] from Telegram's own @gif bot, as the official apps search them. */
    suspend fun searchGifs(chatId: Long, query: String): List<TelegramPanelItem> = runCatching {
        if (gifBotId == 0L) gifBotId = (client.query(TdApi.SearchPublicChat("gif")).type as TdApi.ChatTypePrivate).userId
        client.query(TdApi.GetInlineQueryResults(gifBotId, chatId, null, query, "")).results.mapNotNull { result ->
            val a = (result as? TdApi.InlineQueryResultAnimation)?.animation ?: return@mapNotNull null
            val (media, thumbnail) = mediaOf(TdApi.MessageAnimation(a, TdApi.FormattedText("", emptyArray()), false, false, false))
            media?.let { TelegramPanelItem(a.animation.id, it, thumbnail, isGif = true) }
        }
    }.getOrDefault(emptyList())

    private fun stickerItem(sticker: TdApi.Sticker): TelegramPanelItem {
        val (media, thumbnail) = stickerMedia(sticker)
        return TelegramPanelItem(sticker.sticker.id, media, thumbnail)
    }
}
