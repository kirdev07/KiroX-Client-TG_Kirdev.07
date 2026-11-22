package com.kirox.app.telegram

import org.drinkless.tdlib.TdApi

/** Birthday in a profile; [year] 0 when it is hidden or not set. */
data class Birthday(val day: Int, val month: Int, val year: Int) {
    private val months = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    val text: String get() = "$day ${months.getOrElse(month - 1) { "" }}" + if (year > 0) " $year" else ""
}

/** One photo of a profile's history: TDLib photo id, when it was set, the largest size, video avatar. */
data class ProfilePhotoItem(val id: Long, val addedDate: Int, val file: TelegramFileRef?, val animated: Boolean)

/** What a profile shows besides the basics: birthday and the personal channel. */
data class ProfileExtras(val birthday: Birthday? = null, val personalChatId: Long = 0, val personalChatTitle: String = "")

/**
 * Account and profile actions: profile photos, birthday, personal channel, changing the number,
 * signing in on another device by its QR code, ending inactive sessions, deleting the account.
 */
class TelegramAccountTools(private val client: TelegramClient) {
    /** Every profile photo of [userId], newest first, as the largest size. */
    suspend fun profilePhotos(userId: Long): List<TelegramFileRef> = runCatching {
        client.query(TdApi.GetUserProfilePhotos(userId, 0, 100)).photos.mapNotNull { photo ->
            photo.sizes.maxByOrNull { it.width }?.photo?.toRef()
        }
    }.getOrDefault(emptyList())

    /** Profile photo history with ids (newest first): for «Сделать главным» and «Удалить». */
    suspend fun profilePhotoHistory(userId: Long): List<ProfilePhotoItem> =
        client.query(TdApi.GetUserProfilePhotos(userId, 0, 100)).photos.map { photo ->
            ProfilePhotoItem(photo.id, photo.addedDate, photo.sizes.maxByOrNull { it.width }?.photo?.toRef(), photo.animation != null)
        }

    /** Makes an older own photo the current one again. */
    suspend fun setMainProfilePhoto(photoId: Long) { client.query(TdApi.SetProfilePhoto(TdApi.InputChatPhotoPrevious(photoId), false)) }

    /** Deletes an own profile photo for good. */
    suspend fun deleteProfilePhoto(photoId: Long) { client.query(TdApi.DeleteProfilePhoto(photoId)) }

    suspend fun extras(userId: Long): ProfileExtras = runCatching {
        val info = client.query(TdApi.GetUserFullInfo(userId))
        val title = if (info.personalChatId != 0L) runCatching { client.query(TdApi.GetChat(info.personalChatId)).title }.getOrDefault("") else ""
        ProfileExtras(info.birthdate?.let { Birthday(it.day, it.month, it.year) }, info.personalChatId, title)
    }.getOrDefault(ProfileExtras())

    suspend fun myExtras(): ProfileExtras = runCatching { extras(client.query(TdApi.GetMe()).id) }.getOrDefault(ProfileExtras())

    /** Sets the own birthday, or removes it with null. */
    suspend fun setBirthday(birthday: Birthday?) {
        client.query(TdApi.SetBirthdate(birthday?.let { TdApi.Birthdate(it.day, it.month, it.year) }))
    }

    /** Own public channels that can be shown in the profile. */
    suspend fun channelsForProfile(): List<Pair<Long, String>> = runCatching {
        client.query(TdApi.GetSuitablePersonalChats()).chatIds.toList().mapNotNull { id ->
            runCatching { id to client.query(TdApi.GetChat(id)).title }.getOrNull()
        }
    }.getOrDefault(emptyList())

    /** The channel shown in the own profile; 0 removes it. */
    suspend fun setPersonalChannel(chatId: Long) { client.query(TdApi.SetPersonalChat(chatId)) }

    /** Asks Telegram to send a code to [phone] for moving the account to it; returns where the code went. */
    suspend fun sendChangeNumberCode(phone: String): String {
        val info = client.query(TdApi.SendPhoneNumberCode(phone, TdApi.PhoneNumberAuthenticationSettings(), TdApi.PhoneNumberCodeTypeChange()))
        return when (info.type) {
            is TdApi.AuthenticationCodeTypeTelegramMessage -> "Код отправлен в Telegram"
            is TdApi.AuthenticationCodeTypeSms -> "Код отправлен по SMS"
            is TdApi.AuthenticationCodeTypeCall -> "Код продиктуют звонком"
            else -> "Код отправлен"
        }
    }

    /** Confirms the new number with the code; the account then uses it. */
    suspend fun confirmChangeNumber(code: String) { client.query(TdApi.CheckPhoneNumberCode(code)) }

    /** Signs in the device that shows [link] (tg://login?token=…) as a QR code. */
    suspend fun confirmQrLogin(link: String): String = runCatching {
        val session = client.query(TdApi.ConfirmQrCodeAuthentication(link))
        listOf(session.applicationName, session.deviceModel).filter { it.isNotBlank() }.joinToString(", ")
    }.getOrThrow()

    /** After how many days of not being used other sessions end by themselves. */
    suspend fun inactiveSessionDays(): Int = runCatching { client.query(TdApi.GetActiveSessions()).inactiveSessionTtlDays }.getOrDefault(0)

    suspend fun setInactiveSessionDays(days: Int) { client.query(TdApi.SetInactiveSessionTtl(days)) }

    /** Deletes the account for good; [password] is the cloud password when it is on. */
    suspend fun deleteAccount(reason: String, password: String) { client.query(TdApi.DeleteAccount(reason, password)) }
}
