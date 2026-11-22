package com.kirox.app.ui.v3.screens.chat

import com.kirox.app.ui.v3.KxScreenFn

/** Screens of this section by board name (mockup file name without .dc.html). */
val ChatScreens: Map<String, KxScreenFn> = mapOf(
    "Search" to { ctx -> SearchScreen(ctx) },
    "NewMsg" to { ctx -> NewMsgScreen(ctx) },
    "Saved" to { ctx -> SavedScreen(ctx) },
    "Stickers" to { ctx -> StickersScreen(ctx) },
    "Poll" to { ctx -> PollScreen(ctx) },
    "Location" to { ctx -> LocationScreen(ctx) },
    "Secret" to { ctx -> SecretScreen(ctx) },
    "Bot" to { ctx -> BotScreen(ctx) },
    "Forward" to { ctx -> ForwardScreen(ctx) },
    "ChatSearch" to { ctx -> ChatSearchScreen(ctx) },
    "Schedule" to { ctx -> ScheduleScreen(ctx) },
    "Translate" to { ctx -> TranslateScreen(ctx) },
    "StickerPack" to { ctx -> StickerPackScreen(ctx) },
    "QuickReplies" to { ctx -> QuickRepliesScreen(ctx) },
    "Topics" to { ctx -> TopicsScreen(ctx) },
    "SharedMedia" to { ctx -> SharedMediaScreen(ctx) },
    "Comments" to { ctx -> CommentsScreen(ctx) },
    "SendMedia" to { ctx -> SendMediaScreen(ctx) },
    "Quote" to { ctx -> QuoteScreen(ctx) },
    "JumpDate" to { ctx -> JumpDateScreen(ctx) },
)
