package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi

/**
 * Values TDLib sends only as updates right after start (no getter exists): message effect ids,
 * emoji chat themes, the default (double-tap) reaction and contacts with close birthdays. Created with the services so the first
 * updates are not missed; the v3 store reads them (ui/v3/data/TelegramKxChat3.kt).
 */
class TelegramChat3Updates(client: TelegramClient) {
    @Volatile var effectIds: List<Long> = emptyList(); private set
    @Volatile var chatThemes: List<String> = emptyList(); private set
    /** Emoji of the default reaction; null while unknown or when it is a custom emoji. */
    @Volatile var defaultReaction: String? = null; private set
    /** Contacts with a birthday close to today (what the official apps show): user id → (day, month, year). */
    @Volatile var closeBirthdays: List<Pair<Long, Triple<Int, Int, Int>>> = emptyList(); private set

    init {
        client.addUpdateHandler { update ->
            when (update) {
                is TdApi.UpdateAvailableMessageEffects -> effectIds = (update.reactionEffectIds.toList() + update.stickerEffectIds.toList()).distinct()
                is TdApi.UpdateEmojiChatThemes -> chatThemes = update.chatThemes.map { it.name }
                is TdApi.UpdateDefaultReactionType -> defaultReaction = (update.reactionType as? TdApi.ReactionTypeEmoji)?.emoji
                is TdApi.UpdateContactCloseBirthdays -> closeBirthdays = update.closeBirthdayUsers.map { it.userId to Triple(it.birthdate.day, it.birthdate.month, it.birthdate.year) }
                else -> {}
            }
        }
    }
}
