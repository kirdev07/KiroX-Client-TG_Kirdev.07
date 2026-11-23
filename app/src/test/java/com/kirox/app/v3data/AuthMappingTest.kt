package com.kirox.app.v3data

import com.kirox.app.telegram.TelegramAuthState
import com.kirox.app.ui.v3.data.TelegramKxAuth
import com.kirox.app.ui.v3.model.KxAuthStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure parts of ui/v3/data/TelegramKxAuth: step mapping, phone masking, which states the v3 boards support. */
class AuthMappingTest {
    @Test fun steps() {
        assertEquals(KxAuthStep.Phone, TelegramKxAuth.stepOf(TelegramAuthState.WaitPhoneNumber))
        assertEquals(KxAuthStep.Code, TelegramKxAuth.stepOf(TelegramAuthState.WaitCode("77001234567", 5, true, 60)))
        assertEquals(KxAuthStep.Code, TelegramKxAuth.stepOf(TelegramAuthState.WaitEmailCode(6)))
        assertEquals(KxAuthStep.Password, TelegramKxAuth.stepOf(TelegramAuthState.WaitPassword("", false, "")))
        assertEquals(KxAuthStep.Register, TelegramKxAuth.stepOf(TelegramAuthState.WaitRegistration("", 0)))
        assertEquals(KxAuthStep.Qr, TelegramKxAuth.stepOf(TelegramAuthState.WaitOtherDevice("tg://login?token=x")))
        assertEquals(KxAuthStep.Ready, TelegramKxAuth.stepOf(TelegramAuthState.Ready))
    }

    @Test fun maskKeepsCountryFirstGroupAndLastTwoDigits() {
        val masked = TelegramKxAuth.maskPhone("77001234567")
        assertTrue(masked, masked.startsWith("+7 700 "))
        assertTrue(masked, masked.endsWith("67"))
        assertFalse(masked, masked.contains("12345"))
        assertEquals(11, masked.count { it.isDigit() || it == '*' })
        assertEquals("", TelegramKxAuth.maskPhone(""))
    }

    /** A number the engine cannot group must still be masked, never shown in full. */
    @Test fun unknownNumberIsStillMasked() {
        val masked = TelegramKxAuth.maskPhone("999123456789")
        assertTrue(masked, masked.contains('*'))
    }

    @Test fun supportedStates() {
        assertTrue(TelegramKxAuth.supports(TelegramAuthState.WaitPhoneNumber))
        assertTrue(TelegramKxAuth.supports(TelegramAuthState.WaitCode("7", 5, true, 60)))
        // wave6: word codes and the e-mail address step have v3 boards too.
        assertTrue(TelegramKxAuth.supports(TelegramAuthState.WaitCode("7", 5, true, 60, words = true)))
        assertTrue(TelegramKxAuth.supports(TelegramAuthState.WaitEmailAddress))
        assertTrue(TelegramKxAuth.supports(TelegramAuthState.WaitEmailCode(6)))
        assertFalse(TelegramKxAuth.supports(TelegramAuthState.WaitPassword("", false, "")))
        assertTrue(TelegramKxAuth.supports(TelegramAuthState.WaitPassword("", false, ""), setOf("Password")))
    }
}
