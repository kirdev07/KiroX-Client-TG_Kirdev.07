package com.kirox.app.telegram

import org.junit.Assert.assertEquals
import org.junit.Test

class HideLoginCodeTest {
    @Test fun codeIsHidden() {
        assertEquals("Код для входа в Telegram: •••••. Не давайте код никому", hideLoginCode("Код для входа в Telegram: 79321. Не давайте код никому"))
        assertEquals("Login code: ••••••", hideLoginCode("Login code: 123456"))
    }

    @Test fun otherNumbersStay() {
        assertEquals("Ваш номер +7 701 123-45-67", hideLoginCode("Ваш номер +7 701 123-45-67"))
        assertEquals("В 2026 году", hideLoginCode("В 2026 году"))
    }
}
