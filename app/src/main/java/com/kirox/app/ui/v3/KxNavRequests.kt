package com.kirox.app.ui.v3

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * wave5: requests from outside the v3 host (old app shell) — a tapped message notification, the in-app banner,
 * t.me / tg:// links, the music library «open chat». With «Новый дизайн» the old shell forwards the chat here
 * instead of opening its own conversation; KxApp opens it in v3 and consumes the request.
 */
object KxNavRequests {
    /** Chat to open in v3; KxApp sets it back to null once handled (kept until the host is composed). */
    val openChat = MutableStateFlow<Long?>(null)
    /** Fires with every request, so the host can close an old screen shown over v3 (KxLegacy). */
    val opened = MutableSharedFlow<Long>(extraBufferCapacity = 4)

    fun requestChat(chatId: Long) {
        if (chatId == 0L) return
        openChat.value = chatId
        opened.tryEmit(chatId)
    }
}
