package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi

private val retryAfter = Regex("""retry after (\d+)""", RegexOption.IGNORE_CASE)
private val floodWait = Regex("""FLOOD_WAIT_(\d+)""")

/** User-facing Russian text for TDLib errors. Never includes codes, passwords or other input. */
internal fun humanError(error: TdApi.Error): String {
    val message = error.message.orEmpty()
    val waitSeconds = (retryAfter.find(message) ?: floodWait.find(message))?.groupValues?.get(1)?.toIntOrNull()
    if (error.code == 429 || waitSeconds != null) {
        return if (waitSeconds != null) "Слишком много попыток. Повторите через ${formatWait(waitSeconds)}" else "Слишком много попыток. Повторите позже"
    }
    return when (message) {
        "PHONE_CODE_INVALID", "CODE_INVALID" -> "Неверный код подтверждения"
        "PHONE_CODE_EXPIRED" -> "Срок действия кода истёк. Запросите новый"
        "PHONE_CODE_EMPTY" -> "Введите код подтверждения"
        "PHONE_NUMBER_INVALID" -> "Неверный номер телефона"
        "PHONE_NUMBER_BANNED" -> "Этот номер заблокирован в Telegram"
        "PHONE_NUMBER_FLOOD" -> "С этого номера было слишком много попыток входа"
        "PHONE_PASSWORD_FLOOD" -> "Слишком много попыток ввода пароля. Повторите позже"
        "PASSWORD_HASH_INVALID" -> "Неверный пароль"
        "PASSWORD_RECOVERY_NA" -> "Восстановление пароля недоступно для этого аккаунта"
        "PASSWORD_RECOVERY_EXPIRED" -> "Срок действия кода восстановления истёк"
        "CODE_EMPTY" -> "Введите код"
        "EMAIL_INVALID", "EMAIL_ADDRESS_INVALID" -> "Неверный адрес электронной почты"
        "EMAIL_CODE_INVALID", "EMAIL_VERIFY_EXPIRED" -> "Неверный или устаревший код из письма"
        "FIRSTNAME_INVALID" -> "Недопустимое имя"
        "LASTNAME_INVALID" -> "Недопустимая фамилия"
        "AUTH_TOKEN_EXPIRED" -> "QR-код устарел, сейчас появится новый"
        "SESSION_PASSWORD_NEEDED" -> "Требуется облачный пароль"
        else -> premiumLimit(message) ?: when {
            error.code == 406 -> "Действие недоступно"
            error.code >= 500 || error.code == -1 -> "Telegram временно недоступен. Проверьте соединение"
            else -> "Не удалось выполнить запрос"
        }
    }
}

/**
 * compat: Telegram's Premium-only actions and account limits, said honestly (a free user learns why it failed,
 * and that Premium raises the limit) instead of «Не удалось выполнить запрос». Null for any other error.
 */
private fun premiumLimit(message: String): String? {
    val m = message.lowercase()
    return when {
        "premium" in m -> "Доступно с Telegram Premium"
        "pinned" in m && ("maximum" in m || "too_much" in m || "too much" in m) -> "Достигнут лимит закреплённых чатов. С Telegram Premium — больше"
        "folder" in m && ("maximum" in m || "too_much" in m || "too much" in m) || "filters_too_much" in m -> "Достигнут лимит папок. С Telegram Premium — больше"
        "channels_too_much" in m -> "Вы состоите в слишком многих группах и каналах. С Telegram Premium — больше"
        "file is too big" in m || "file_too_big" in m || "file_parts_invalid" in m -> "Файл больше лимита Telegram (2 ГБ, с Premium — 4 ГБ)"
        "reactions_too_many" in m || "reaction_too_much" in m -> "Слишком много реакций на сообщение. С Telegram Premium — больше"
        "usernames_active_too_much" in m -> "Достигнут лимит активных имён пользователя"
        else -> null
    }
}

private fun formatWait(seconds: Int): String = when {
    seconds < 60 -> "$seconds с"
    seconds < 3600 -> "${(seconds + 59) / 60} мин"
    else -> "${(seconds + 3599) / 3600} ч"
}

/** Text for a failed cloud password action; codes and passwords are never shown. */
fun passwordError(error: Throwable): String = when (error) {
    is TdException -> when (error.error.message) {
        "PASSWORD_HASH_INVALID" -> "Неверный текущий пароль"
        "NEW_SALT_INVALID", "NEW_SETTINGS_INVALID" -> "Пароль не подошёл. Попробуйте другой"
        "EMAIL_UNCONFIRMED" -> "Подтвердите почту кодом из письма"
        else -> humanError(error.error)
    }
    else -> "Не удалось сохранить"
}
