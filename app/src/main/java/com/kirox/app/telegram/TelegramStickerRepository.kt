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

    /**
     * Pictures of custom emoji by id: the sticker itself when it is a still image, otherwise its
     * still preview. Animated ones stay still in kiroX.
     */
    suspend fun customEmojiFiles(ids: List<Long>): Map<Long, TelegramFileRef> = runCatching {
        client.query(TdApi.GetCustomEmojiStickers(ids.toLongArray())).stickers.mapNotNull { sticker ->
            val id = (sticker.fullType as? TdApi.StickerFullTypeCustomEmoji)?.customEmojiId ?: return@mapNotNull null
            val still = sticker.thumbnail?.takeIf { it.format is TdApi.ThumbnailFormatWebp || it.format is TdApi.ThumbnailFormatPng || it.format is TdApi.ThumbnailFormatJpeg }?.file
            val file = if (sticker.format is TdApi.StickerFormatWebp) sticker.sticker else still ?: return@mapNotNull null
            id to file.toRef()
        }.toMap()
    }.getOrDefault(emptyMap())

    /** Sets found by name for «Наборы стикеров»; [installed] marks the ones already added. */
    suspend fun searchSets(query: String): List<Pair<TelegramStickerSetInfo, Boolean>> = runCatching {
        client.query(TdApi.SearchStickerSets(TdApi.StickerTypeRegular(), query)).sets.map { set ->
            TelegramStickerSetInfo(set.id, set.title, set.covers?.firstOrNull()?.let(::stickerItem)) to set.isInstalled
        }
    }.getOrDefault(emptyList())

    /** Adds a set to the panel or removes it. */
    suspend fun setInstalled(id: Long, installed: Boolean) { client.query(TdApi.ChangeStickerSet(id, installed, false)) }

    /** New order of the installed sets, as shown in the panel. */
    suspend fun reorder(ids: List<Long>) { client.query(TdApi.ReorderInstalledStickerSets(TdApi.StickerTypeRegular(), ids.toLongArray())) }

    suspend fun set(id: Long): List<TelegramPanelItem> = runCatching {
        client.query(TdApi.GetStickerSet(id)).stickers.map(::stickerItem)
    }.getOrDefault(emptyList())

    suspend fun recent(): List<TelegramPanelItem> = runCatching {
        client.query(TdApi.GetRecentStickers(false)).stickers.map(::stickerItem)
    }.getOrDefault(emptyList())

    suspend fun favorites(): List<TelegramPanelItem> = runCatching {
        client.query(TdApi.GetFavoriteStickers()).stickers.map(::stickerItem)
    }.getOrDefault(emptyList())

    /** Adds or removes a sticker in «Избранные», or a GIF in the saved GIFs; false when Telegram refuses. */
    suspend fun setFavorite(item: TelegramPanelItem, favorite: Boolean): Boolean = runCatching {
        val file = TdApi.InputFileId(item.fileId)
        client.query(when {
            item.isGif && favorite -> TdApi.AddSavedAnimation(file)
            item.isGif -> TdApi.RemoveSavedAnimation(file)
            favorite -> TdApi.AddFavoriteSticker(file)
            else -> TdApi.RemoveFavoriteSticker(file)
        })
        true
    }.getOrDefault(false)

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

    /**
     * A new sticker set of your own from ready 512 px WEBP files, each with its emoji. Telegram picks
     * the set's link name. Returns that name (t.me/addstickers/<name>).
     */
    suspend fun createSet(userId: Long, title: String, stickers: List<Pair<String, String>>): String {
        val list = stickers.map { (path, emoji) -> TdApi.NewSticker(TdApi.InputFileLocal(path), TdApi.StickerFormatWebp(), emoji, null, emptyArray()) }.toTypedArray()
        return client.query(TdApi.CreateNewStickerSet(userId, title, "", TdApi.StickerTypeRegular(), false, list, "kiroX")).name
    }

    /** Stickers from all of Telegram for a word («кот») or an emoji (😂). */
    suspend fun searchStickers(query: String): List<TelegramPanelItem> = runCatching {
        val q = query.trim()
        val emoji = q.takeIf { s -> s.codePoints().anyMatch { it >= 0x2600 } }.orEmpty()
        client.query(TdApi.SearchStickers(TdApi.StickerTypeRegular(), emoji, if (emoji.isEmpty()) q else "", arrayOf("ru", "en"), 0, 60)).stickers.map(::stickerItem)
    }.getOrDefault(emptyList())

    private fun stickerItem(sticker: TdApi.Sticker): TelegramPanelItem {
        val (media, thumbnail) = stickerMedia(sticker)
        return TelegramPanelItem(sticker.sticker.id, media, thumbnail)
    }
}
