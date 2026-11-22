package com.kirox.app.ui.v3.screens.more

import com.kirox.app.ui.v3.KxScreenFn

/** Screens of this section by board name (mockup file name without .dc.html). */
val MoreScreens: Map<String, KxScreenFn> = mapOf(
    "EditMsg" to { ctx -> EditMsgScreen(ctx) },
    "MultiSelect" to { ctx -> MultiSelectScreen(ctx) },
    "Pinned" to { ctx -> PinnedScreen(ctx) },
    "MsgInfo" to { ctx -> MsgInfoScreen(ctx) },
    "ReactPicker" to { ctx -> ReactPickerScreen(ctx) },
    "ReplaceMedia" to { ctx -> ReplaceMediaScreen(ctx) },
    "SendOptions" to { ctx -> SendOptionsScreen(ctx) },
    "SendAs" to { ctx -> SendAsScreen(ctx) },
    "MsgEffects" to { ctx -> MsgEffectsScreen(ctx) },
    "ChatTheme" to { ctx -> ChatThemeScreen(ctx) },
    "EmojiKeyboard" to { ctx -> EmojiKeyboardScreen(ctx) },
    "BigEmoji" to { ctx -> BigEmojiScreen(ctx) },
    "InlineBot" to { ctx -> InlineBotScreen(ctx) },
    "FilePicker" to { ctx -> FilePickerScreen(ctx) },
    "VoiceRecord" to { ctx -> VoiceRecordScreen(ctx) },
    "TypeTranslate" to { ctx -> TypeTranslateScreen(ctx) },
    "ClipPaste" to { ctx -> ClipPasteScreen(ctx) },
    "LinkWarn" to { ctx -> LinkWarnScreen(ctx) },
    "Sensitive" to { ctx -> SensitiveScreen(ctx) },
    "ChatPeek" to { ctx -> ChatPeekScreen(ctx) },
    "NextUnread" to { ctx -> NextUnreadScreen(ctx) },
    "Format" to { ctx -> FormatScreen(ctx) },
    "UndoSend" to { ctx -> UndoSendScreen(ctx) },
    "Drafts" to { ctx -> DraftsScreen(ctx) },
    "Mentions" to { ctx -> MentionsScreen(ctx) },
    "Hashtag" to { ctx -> HashtagScreen(ctx) },
)
