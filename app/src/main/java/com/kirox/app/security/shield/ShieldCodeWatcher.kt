package com.kirox.app.security.shield

import com.kirox.app.telegram.TelegramClient
import org.drinkless.tdlib.TdApi

/**
 * «Код-щит»: listens for new messages from Telegram's service account (777000) and hands their login codes to
 * [ShieldCodes] (memory only, 15 minutes). Nothing is stored or sent. Created once in TelegramRepositories.
 */
class ShieldCodeWatcher(client: TelegramClient) {
    init {
        client.addUpdateHandler { update ->
            if (update is TdApi.UpdateNewMessage) {
                val m = update.message
                val fromService = (m.senderId as? TdApi.MessageSenderUser)?.userId == ShieldCodes.SERVICE_USER || m.chatId == ShieldCodes.SERVICE_USER
                if (fromService && !m.isOutgoing) (m.content as? TdApi.MessageText)?.text?.text?.let { ShieldCodes.remember(it) }
            }
        }
    }
}
