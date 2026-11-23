package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi
import org.junit.Assert.assertEquals
import org.junit.Test

/** lead walk-through: spoiler text never shows in the chat-list preview. */
class SpoilerPreviewTest {
    private fun text(raw: String, vararg spoilers: Pair<Int, Int>) = TdApi.Message().apply {
        content = TdApi.MessageText().apply {
            text = TdApi.FormattedText(raw, spoilers.map { (o, l) -> TdApi.TextEntity(o, l, TdApi.TextEntityTypeSpoiler()) }.toTypedArray())
        }
    }

    @Test fun spoilerMasked() {
        assertEquals("R2-1 spoiler: ▒▒▒▒▒▒ ▒▒▒▒ end", messagePreview(text("R2-1 spoiler: hidden word end", 14 to 11)))
    }

    @Test fun emojiIsOneSymbolAndOtherEntitiesKept() {
        assertEquals("a ▒▒ b", messagePreview(text("a 😀x b", 2 to 3)))
        val bold = TdApi.Message().apply { content = TdApi.MessageText().apply { text = TdApi.FormattedText("bold", arrayOf(TdApi.TextEntity(0, 4, TdApi.TextEntityTypeBold()))) } }
        assertEquals("bold", messagePreview(bold))
    }

    @Test fun captionMasked() {
        val photo = TdApi.Message().apply { content = TdApi.MessagePhoto().apply { caption = TdApi.FormattedText("see this", arrayOf(TdApi.TextEntity(4, 4, TdApi.TextEntityTypeSpoiler()))) } }
        assertEquals("see ▒▒▒▒", messagePreview(photo))
    }
}
