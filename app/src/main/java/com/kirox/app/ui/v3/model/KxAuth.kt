package com.kirox.app.ui.v3.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Sign-in steps of the v3 design (boards Login / Code / Password / QrLogin / ProfileSetup). */
enum class KxAuthStep { Phone, Code, Email, Password, Register, Qr, Ready }
enum class KxCodeVia { App, Sms, Call, Fragment, Email }

/**
 * Sign-in for the v3 screens. The demo version only simulates; the TDLib version (ui/v3/data)
 * drives TelegramViewModel.auth. Values are Compose snapshot state.
 */
interface KxAuth {
    val step: KxAuthStep
    val busy: Boolean
    /** Human Russian error («Неверный код»), null when none. */
    val error: String?
    val countryName: String
    val dialCode: String
    /** «+7 700 *** ** 00» on the code screen. */
    val phoneMasked: String
    val codeLength: Int
    /** true: code goes to Telegram on another device; false: SMS / call. */
    val codeInApp: Boolean
    /** Seconds until «Отправить снова» is allowed; 0 = now. */
    val resendIn: Int
    val passwordHint: String
    /** tg://login?token=… for the QR screen, null while not requested. */
    val qrLink: String?

    // Optional extras (defaults keep older implementations compiling)
    val codeVia: KxCodeVia get() = if (codeInApp) KxCodeVia.App else KxCodeVia.Sms
    val emailPattern: String get() = ""
    /** Code sent as words / a phrase (not digits): the v3 code board can't show it, old screen does. */
    val codeIsWords: Boolean get() = false
    val hasRecoveryEmail: Boolean get() = false
    val recoveryEmailPattern: String get() = ""
    val recoveryCodeSent: Boolean get() = false
    val termsOfService: String get() = ""
    val minAge: Int get() = 0
    /** Adding a second account can be cancelled. */
    val canCancel: Boolean get() = false
    /** Digits last sent from the phone board; «←» from the code step shows them again. */
    val typedPhone: String get() = ""
    fun submitEmail(address: String) {}
    fun requestRecovery() {}
    fun submitRecovery(code: String, newPassword: String, hint: String) {}
    fun cancelRecovery() {}
    fun cancel() {}

    fun submitPhone(digits: String, syncContacts: Boolean)
    fun submitCode(code: String)
    fun submitPassword(password: String)
    fun register(firstName: String, lastName: String) {}
    fun resend()
    fun requestQr()
    fun back()
    fun openCountryPicker() {}
}

/** Simulated sign-in for the preview app and tests: any 5 digits pass, never contacts Telegram. */
class KxDemoAuth : KxAuth {
    override var step by androidx.compose.runtime.mutableStateOf(KxAuthStep.Phone)
    override var busy by androidx.compose.runtime.mutableStateOf(false)
    override var error by androidx.compose.runtime.mutableStateOf<String?>(null)
    override val countryName = "Казахстан"
    override val dialCode = "+7"
    override var phoneMasked by androidx.compose.runtime.mutableStateOf("+7 700 *** ** 00")
    override val codeLength = 5
    override val codeInApp = true
    override var resendIn by androidx.compose.runtime.mutableIntStateOf(42)
    override val passwordHint = "любимый фильм"
    override val qrLink: String? = "tg://login?token=demo"
    override fun submitPhone(digits: String, syncContacts: Boolean) {
        if (digits.length < 10) { error = "Введите номер полностью"; return }
        error = null; phoneMasked = "+7 ${digits.take(3)} *** ** ${digits.takeLast(2)}"; step = KxAuthStep.Code
    }
    override fun submitCode(code: String) { if (code.length == codeLength) { error = null; step = KxAuthStep.Ready } }
    override fun submitPassword(password: String) { step = KxAuthStep.Ready }
    override fun resend() { resendIn = 60 }
    override fun requestQr() { step = KxAuthStep.Qr }
    override fun back() { error = null; step = KxAuthStep.Phone }
}
