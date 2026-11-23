package com.kirox.app.telegram

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

/** Authorization state machine. [revision] grows on every TDLib auth update, even when the state is equal. */
class TelegramAuthRepository(private val client: TelegramClient) {
    private val _state = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Initializing)
    val state: StateFlow<TelegramAuthState> = _state.asStateFlow()
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    val blocked: StateFlow<String?> = client.blocked
    private val _recoveryCodeSent = MutableStateFlow(false)
    /** True after Telegram emailed a 2FA recovery code; reset whenever the auth state changes. */
    val recoveryCodeSent: StateFlow<Boolean> = _recoveryCodeSent.asStateFlow()

    init {
        client.addUpdateHandler { update -> if (update is TdApi.UpdateAuthorizationState) apply(update.authorizationState) }
        client.addFailureHandler(::setFailure)
    }

    /**
     * [onOk] runs once TDLib accepted the number. A new number sent from the code / password step may
     * leave the state equal to the old one (StateFlow drops it), so the current state is read again.
     */
    fun phone(value: String, onOk: () -> Unit = {}) = send(TdApi.SetAuthenticationPhoneNumber().apply {
        phoneNumber = value
        settings = TdApi.PhoneNumberAuthenticationSettings()
    }) {
        client.send(TdApi.GetAuthorizationState()) { r -> if (r is TdApi.AuthorizationState) apply(r) }
        onOk()
    }
    fun code(value: String) = send(TdApi.CheckAuthenticationCode(value))
    fun password(value: String) = send(TdApi.CheckAuthenticationPassword(value))
    fun email(value: String) = send(TdApi.SetAuthenticationEmailAddress(value))
    fun emailCode(value: String) = send(TdApi.CheckAuthenticationEmailCode().apply { code = TdApi.EmailAddressAuthenticationCode(value) })
    fun register(first: String, last: String) = send(TdApi.RegisterUser(first, last, false))
    fun resend() = send(TdApi.ResendAuthenticationCode(null))
    fun requestQr() = send(TdApi.RequestQrCodeAuthentication(LongArray(0)))
    fun requestPasswordRecovery() = send(TdApi.RequestAuthenticationPasswordRecovery()) { _recoveryCodeSent.value = true }
    fun recoverPassword(code: String, newPassword: String, newHint: String) = send(TdApi.RecoverAuthenticationPassword(code, newPassword, newHint))
    fun cancelPasswordRecovery() { _recoveryCodeSent.value = false; clearError() }
    /** Logs out a signed-in account, or abandons an unfinished login (e.g. QR) so a fresh one can start. */
    fun logOut() = send(TdApi.LogOut())
    fun clearError() { _error.value = null }

    private fun send(function: TdApi.Function<*>, onOk: () -> Unit = {}) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        val sent = client.send(function) { result ->
            _busy.value = false
            if (result is TdApi.Error) setFailure(humanError(result)) else onOk()
        }
        if (!sent) setFailure("TDLib ещё не готова")
    }

    private fun setFailure(message: String) { _busy.value = false; _error.value = message }

    private fun apply(state: TdApi.AuthorizationState) {
        _state.value = when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> TelegramAuthState.Initializing
            is TdApi.AuthorizationStateWaitPhoneNumber -> TelegramAuthState.WaitPhoneNumber
            is TdApi.AuthorizationStateWaitCode -> TelegramAuthState.WaitCode(
                state.codeInfo.phoneNumber.orEmpty(), codeLength(state.codeInfo.type), state.codeInfo.nextType != null, state.codeInfo.timeout,
                when (state.codeInfo.type) {
                    is TdApi.AuthenticationCodeTypeTelegramMessage -> CodeDestination.App
                    is TdApi.AuthenticationCodeTypeSms -> CodeDestination.Sms
                    is TdApi.AuthenticationCodeTypeCall -> CodeDestination.Call
                    is TdApi.AuthenticationCodeTypeFragment -> CodeDestination.Fragment
                    else -> CodeDestination.Other
                },
                // A word or a phrase sent by SMS instead of digits.
                words = state.codeInfo.type is TdApi.AuthenticationCodeTypeSmsWord || state.codeInfo.type is TdApi.AuthenticationCodeTypeSmsPhrase
            )
            is TdApi.AuthorizationStateWaitPassword -> TelegramAuthState.WaitPassword(
                state.passwordHint.orEmpty(), state.hasRecoveryEmailAddress, state.recoveryEmailAddressPattern.orEmpty()
            )
            is TdApi.AuthorizationStateWaitEmailAddress -> TelegramAuthState.WaitEmailAddress
            is TdApi.AuthorizationStateWaitEmailCode -> TelegramAuthState.WaitEmailCode(state.codeInfo.length, state.codeInfo.emailAddressPattern.orEmpty())
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> TelegramAuthState.WaitOtherDevice(state.link)
            is TdApi.AuthorizationStateWaitRegistration -> TelegramAuthState.WaitRegistration(
                state.termsOfService?.text?.text.orEmpty(), state.termsOfService?.minUserAge ?: 0
            )
            is TdApi.AuthorizationStateReady -> TelegramAuthState.Ready
            is TdApi.AuthorizationStateLoggingOut, is TdApi.AuthorizationStateClosing -> TelegramAuthState.Closing
            is TdApi.AuthorizationStateClosed -> TelegramAuthState.Initializing
            is TdApi.AuthorizationStateWaitPremiumPurchase -> TelegramAuthState.Blocked("Telegram требует завершить покупку Premium в официальном приложении")
            else -> _state.value
        }
        if (state !is TdApi.AuthorizationStateWaitPassword) _recoveryCodeSent.value = false
        _revision.value++
    }

    private fun codeLength(type: TdApi.AuthenticationCodeType): Int = when (type) {
        is TdApi.AuthenticationCodeTypeTelegramMessage -> type.length
        is TdApi.AuthenticationCodeTypeSms -> type.length
        is TdApi.AuthenticationCodeTypeCall -> type.length
        is TdApi.AuthenticationCodeTypeFragment -> type.length
        else -> 0
    }
}

/** Network state from TDLib, for the "Соединение…" indicator. */
class TelegramConnectionRepository(client: TelegramClient) {
    private val _state = MutableStateFlow(TelegramConnection.Connecting)
    val state: StateFlow<TelegramConnection> = _state.asStateFlow()

    init {
        client.addUpdateHandler { update ->
            if (update is TdApi.UpdateConnectionState) _state.value = when (update.state) {
                is TdApi.ConnectionStateWaitingForNetwork -> TelegramConnection.WaitingForNetwork
                is TdApi.ConnectionStateConnectingToProxy -> TelegramConnection.ConnectingToProxy
                is TdApi.ConnectionStateConnecting -> TelegramConnection.Connecting
                is TdApi.ConnectionStateUpdating -> TelegramConnection.Updating
                else -> TelegramConnection.Ready
            }
        }
    }
}

/** Process-wide Telegram graph: one client, one instance of each repository. */
class TelegramServices(context: android.content.Context) {
    val client = TelegramClient(context)
    val auth = TelegramAuthRepository(client)
    val chats = TelegramChatRepository(client)
    val files = TelegramFileRepository(client)
    val users = TelegramUserRepository(client)
    val connection = TelegramConnectionRepository(client)
    /** Reports Wi-Fi / mobile / no network to TDLib so it reconnects right after a network change. */
    val network = ConnectionMonitor(context, client)
    /** Auto-download limits for the current network: the user's own or Telegram's presets. */
    val autoDownload = AutoDownloadPolicy(context, client, network)
    /** «Хранить медиа» and the cache size limit, applied with TDLib's OptimizeStorage. */
    val storage = StoragePolicy(context, client)
    /** «Сохранять в галерею» and call data saving. */
    val media = MediaPreferences(context)
    val messages = TelegramMessageRepository(client, chats, users)
    /** "печатает…" and other member activity, ours sent with SendChatAction. */
    val activity = TelegramChatActionRepository(client, chats, users)
    val stickers = TelegramStickerRepository(client)
    val groupCalls = TelegramGroupCallRepository(client, chats, users)
    val calls = TelegramCallRepository(client, chats)
    val voip = TelegramVoipRepository(client, users) { media.callDataSavingOn(network.network.value) }
    val sessions = TelegramSessionsRepository(client)
    val settings = TelegramSettingsRepository(client)
    /** Deleted and edited messages kept on this phone, when that is turned on in kiroX settings. */
    val archive = MessageArchive(context, client, chats, users)
    /** Managing groups and channels, reports and message links. */
    val admin = TelegramChatAdmin(client)
    val adminExtras = TelegramChatAdminExtras(client)
    val music = TelegramMusic(client)
    val mentions = TelegramMentions(client)
    val account = TelegramAccountTools(client)
    val extras = TelegramExtras(client, chats)
    val audioTags = AudioTags(client)
    /** In-app alerts and the icon counter, kept on this device. */
    val notificationPrefs = com.kirox.app.notify.NotificationPreferences(context)
    val notifications = com.kirox.app.notify.MessageNotifications(context.applicationContext, client, chats, users, notificationPrefs)
    val stories = TelegramStoryRepository(client, chats, users)
    // wave3-chat: update-only values for the v3 chat boards
    val chat3Updates = TelegramChat3Updates(client)
    /** Accent colours and new terms of service (sent once by TDLib), for the v3 account boards. */
    val accountUpdates = TelegramAccountUpdates(client)
    /** shield: «Код-щит» remembers fresh login codes from 777000 in memory for 15 minutes (security/shield). */
    val shieldCodes = com.kirox.app.security.shield.ShieldCodeWatcher(client)
}
