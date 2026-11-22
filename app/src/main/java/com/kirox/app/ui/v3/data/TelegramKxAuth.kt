package com.kirox.app.ui.v3.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kirox.app.phone.PhoneEntryState
import com.kirox.app.phone.PhoneStatus
import com.kirox.app.telegram.CodeDestination
import com.kirox.app.telegram.TelegramAuthState
import com.kirox.app.telegram.TelegramViewModel
import com.kirox.app.ui.v3.model.KxAuth
import com.kirox.app.ui.v3.model.KxAuthStep
import com.kirox.app.ui.v3.model.KxCodeVia
import com.kirox.app.ui.v3.screens.core.KxAuthFlow
import com.kirox.app.ui.v3.theme.KxTheme

/**
 * v3 sign-in on the real TelegramAuthRepository. It only forwards what the user typed (phone, code,
 * password); nothing is sent on its own. The country, number checks and formatting come from the
 * old login's [PhoneEntryState], so both designs keep the same chosen country.
 *
 * The values are read straight from Compose state given by the host ([authState], [busy], [authError],
 * [blocked]) and from [phone], so the screens recompose without a scope of their own.
 */
class TelegramKxAuth(
    private val vm: TelegramViewModel,
    private val phone: PhoneEntryState,
    private val authState: () -> TelegramAuthState,
    private val isBusy: () -> Boolean,
    private val authError: () -> String?,
    private val blocked: () -> String?,
    private val onChooseCountry: () -> Unit,
    /** TelegramAuthRepository.recoveryCodeSent, read as Compose state by the host. */
    private val recoverySent: () -> Boolean = { false },
    /** Adding a second account: the host knows whether it can be cancelled (TelegramAccounts). */
    private val cancellable: () -> Boolean = { false },
    private val onCancel: () -> Unit = {},
) : KxAuth {
    /** Number check message from PhoneEntryState («Проверьте номер…»), shown before anything is sent. */
    private var localError by mutableStateOf<String?>(null)

    override val step: KxAuthStep get() = stepOf(authState())
    override val busy: Boolean get() = isBusy()
    override val error: String?
        get() = localError ?: blocked() ?: (authState() as? TelegramAuthState.Blocked)?.message ?: authError()
    override val countryName: String get() = phone.country.name
    override val dialCode: String get() = phone.country.dialCode

    override val phoneMasked: String get() = (authState() as? TelegramAuthState.WaitCode)?.phoneNumber?.let(::maskPhone).orEmpty()

    override val codeLength: Int
        get() = when (val s = authState()) {
            is TelegramAuthState.WaitCode -> s.length
            is TelegramAuthState.WaitEmailCode -> s.length
            else -> 5
        }

    override val codeInApp: Boolean get() = (authState() as? TelegramAuthState.WaitCode)?.sentTo == CodeDestination.App

    /** TDLib's timeout from the moment the code was sent; the code screen counts it down itself. */
    override val resendIn: Int
        get() = (authState() as? TelegramAuthState.WaitCode)?.let { if (it.canResend) it.timeout else 0 } ?: 0

    override val passwordHint: String get() = (authState() as? TelegramAuthState.WaitPassword)?.hint.orEmpty()

    override val qrLink: String? get() = (authState() as? TelegramAuthState.WaitOtherDevice)?.link

    override val codeVia: KxCodeVia
        get() = when (val s = authState()) {
            is TelegramAuthState.WaitEmailCode -> KxCodeVia.Email
            is TelegramAuthState.WaitCode -> when (s.sentTo) {
                CodeDestination.App -> KxCodeVia.App
                CodeDestination.Call -> KxCodeVia.Call
                CodeDestination.Fragment -> KxCodeVia.Fragment
                CodeDestination.Sms, CodeDestination.Other -> KxCodeVia.Sms
            }
            else -> KxCodeVia.Sms
        }
    override val emailPattern: String get() = (authState() as? TelegramAuthState.WaitEmailCode)?.emailPattern.orEmpty()
    override val codeIsWords: Boolean get() = (authState() as? TelegramAuthState.WaitCode)?.words == true
    override val hasRecoveryEmail: Boolean get() = (authState() as? TelegramAuthState.WaitPassword)?.hasRecoveryEmail == true
    override val recoveryEmailPattern: String get() = (authState() as? TelegramAuthState.WaitPassword)?.recoveryEmailPattern.orEmpty()
    override val recoveryCodeSent: Boolean get() = recoverySent()
    override val termsOfService: String get() = (authState() as? TelegramAuthState.WaitRegistration)?.termsOfService.orEmpty()
    override val minAge: Int get() = (authState() as? TelegramAuthState.WaitRegistration)?.minUserAge ?: 0
    override val canCancel: Boolean get() = cancellable()

    override fun submitEmail(address: String) {
        if (!busy && address.isNotBlank()) vm.auth.email(address.trim())
    }

    /** «Забыли пароль?»: Telegram e-mails a code to the recovery address (only when one is set). */
    override fun requestRecovery() { if (!busy && hasRecoveryEmail) vm.auth.requestPasswordRecovery() }

    override fun submitRecovery(code: String, newPassword: String, hint: String) {
        if (!busy && code.isNotBlank()) vm.auth.recoverPassword(code.trim(), newPassword, hint)
    }

    override fun cancelRecovery() = vm.auth.cancelPasswordRecovery()

    override fun cancel() { if (canCancel) onCancel() }

    /**
     * [digits]: the national number typed after the dial code. Checked with the phone engine first;
     * only a valid number goes to Telegram. [syncContacts]: kiroX has no contact upload at sign-in,
     * so it is not used yet.
     */
    override fun submitPhone(digits: String, syncContacts: Boolean) {
        if (busy) return
        phone.updateNumber(digits)
        if (!phone.validate()) { localError = (phone.status as? PhoneStatus.Error)?.message; return }
        localError = null
        vm.auth.clearError()
        vm.auth.phone(phone.internationalNumber)
    }

    override fun submitCode(code: String) {
        if (busy || code.isBlank()) return
        when (authState()) {
            is TelegramAuthState.WaitEmailCode -> vm.auth.emailCode(code)
            is TelegramAuthState.WaitCode -> vm.auth.code(code)
            else -> Unit
        }
    }

    override fun submitPassword(password: String) {
        if (!busy && password.isNotEmpty()) vm.auth.password(password)
    }

    override fun register(firstName: String, lastName: String) {
        if (!busy && firstName.isNotBlank()) vm.auth.register(firstName.trim(), lastName.trim())
    }

    override fun resend() {
        if ((authState() as? TelegramAuthState.WaitCode)?.canResend == true && !busy) vm.auth.resend()
    }

    override fun requestQr() { if (!busy) vm.auth.requestQr() }

    /** Back from a later step abandons this login (as the old screens do), so a fresh number can be entered. */
    override fun back() {
        localError = null
        vm.auth.clearError()
        when {
            // Inside password recovery, back only leaves the recovery (as the old screen does).
            step == KxAuthStep.Password && recoverySent() -> vm.auth.cancelPasswordRecovery()
            step != KxAuthStep.Phone && step != KxAuthStep.Ready -> vm.auth.logOut()
        }
    }

    override fun openCountryPicker() { localError = null; onChooseCountry() }

    companion object {
        /**
         * Whether the v3 auth boards can show [state]. Password, registration and QR need their boards
         * registered in KxRegistry ([boards]: its screen names, checked at runtime). The e-mail address
         * step and word/phrase codes always stay on the old screens.
         */
        fun supports(state: TelegramAuthState, boards: Set<String> = emptySet()): Boolean = when (state) {
            TelegramAuthState.WaitPhoneNumber, TelegramAuthState.Initializing, TelegramAuthState.Closing, is TelegramAuthState.Blocked -> true
            is TelegramAuthState.WaitCode -> !state.words && state.length in 3..8
            is TelegramAuthState.WaitEmailCode -> state.length in 3..8
            is TelegramAuthState.WaitPassword -> "Password" in boards
            is TelegramAuthState.WaitRegistration -> "ProfileSetup" in boards
            is TelegramAuthState.WaitOtherDevice -> "QrLogin" in boards
            else -> false
        }

        internal fun stepOf(state: TelegramAuthState): KxAuthStep = when (state) {
            is TelegramAuthState.WaitCode, is TelegramAuthState.WaitEmailCode -> KxAuthStep.Code
            is TelegramAuthState.WaitPassword -> KxAuthStep.Password
            is TelegramAuthState.WaitRegistration -> KxAuthStep.Register
            is TelegramAuthState.WaitOtherDevice -> KxAuthStep.Qr
            TelegramAuthState.WaitEmailAddress -> KxAuthStep.Email
            TelegramAuthState.Ready -> KxAuthStep.Ready
            else -> KxAuthStep.Phone
        }

        /**
         * «+7 700 *** ** 00»: the number grouped the way its country writes it, with the country code,
         * the first group and the last two digits visible.
         */
        internal fun maskPhone(raw: String): String {
            val digits = raw.filter(Char::isDigit)
            if (digits.isEmpty()) return ""
            val formatted = runCatching { com.kirox.app.phone.PhoneNumberEngine().formatInternational(digits) }.getOrDefault("+$digits")
            val groups = formatted.removePrefix("+").split(' ').filter { it.isNotEmpty() }
            if (groups.size < 3) return formatted
            val total = groups.sumOf { it.length }
            var seen = 0
            return "+" + groups.mapIndexed { i, g ->
                g.map { c ->
                    seen++
                    if (i <= 1 || seen > total - 2) c else '*'
                }.joinToString("")
            }.joinToString(" ")
        }
    }
}

/** The v3 auth boards in the kiroX v3 theme, for KiroXApp while «Новый дизайн» is on. */
@Composable
fun KxTelegramAuthHost(auth: TelegramKxAuth, reducedMotion: Boolean = false) = KxTheme(reducedMotion) { KxAuthFlow(auth) }
