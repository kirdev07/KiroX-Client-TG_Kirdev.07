package com.kirox.app.shield

import com.kirox.app.security.shield.ShieldApk
import com.kirox.app.security.shield.ShieldCodes
import com.kirox.app.security.shield.ShieldLinks
import com.kirox.app.security.shield.ShieldRules
import com.kirox.app.security.shield.ShieldSendRisk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** «Защита kiroX»: signal rules, «Код-щит», «Рентген ссылки» and the pure part of «Рентген APK». */
class ShieldRulesTest {
    private fun ids(t: String) = ShieldRules.signals(t).map { it.id }.toSet()

    @Test fun codeRequests() {
        assertTrue("code" in ids("Привет! Я случайно отправил тебе код, перешли пожалуйста"))
        assertTrue("code" in ids("Скиньте код подтверждения из SMS"))
        assertTrue("code" in ids("Пришлите кoд из смс")) // Latin «o» inside a Russian word
        assertTrue("code" in ids("Please send me the verification code"))
        assertTrue("code" in ids("Сәлем, кодты жіберші"))
        // Everyday codes are not login codes.
        assertFalse("code" in ids("Скинь код от домофона, я у подъезда"))
        assertFalse("code" in ids("Напиши промокод на скидку"))
        assertFalse("code" in ids("Сделай ревью моего кода на гитхабе"))
    }

    @Test fun otherSignals() {
        assertTrue("password" in ids("Пришлите облачный пароль от телеграм для проверки"))
        assertTrue("seed" in ids("Введите сид-фразу из 12 слов, чтобы получить награду"))
        assertTrue("card" in ids("Назовите номер карты, срок действия и CVV"))
        assertTrue("money_urgent" in ids("Срочно переведите деньги на резервный счет"))
        assertFalse("money_urgent" in ids("Переведи мне 500 за пиццу, когда сможешь"))
        assertTrue("support" in ids("Служба поддержки Telegram: ваш аккаунт будет удален"))
        assertTrue("remote" in ids("Установите AnyDesk, я помогу вернуть деньги"))
        assertTrue("remote" in ids("Скачай RustDesk"))
        assertTrue("crypto" in ids("Удвоим ваши TON за сутки, гарантированный доход"))
        assertTrue("prize" in ids("Поздравляем! Вы выиграли iPhone, заберите приз"))
        assertTrue("vote" in ids("Проголосуй за мою племянницу в конкурсе"))
        assertTrue("new_number" in ids("Мама, это я, пишу с нового номера"))
        assertTrue("job_spam" in ids("Заработок от 5000 в день без вложений, пиши в лс"))
        assertTrue("qr" in ids("Отсканируй QR код камерой телеграма"))
        assertTrue(ids("Купи хлеб и молоко, пожалуйста").isEmpty())
        assertTrue(ids("Во сколько завтра встречаемся у кинотеатра?").isEmpty())
    }

    // ---- Код-щит ----
    @After fun clearCodes() = ShieldCodes.forgetAll()

    @Test fun codeShieldCatchesFreshLoginCode() {
        val now = 1_000_000_000L
        ShieldCodes.remember("Код для входа в Telegram: 52817. Не давайте код никому, даже если его требуют от имени Telegram!", now)
        val r = ShieldCodes.check("вот 52817", now + 3 * 60_000)
        assertTrue(r is ShieldSendRisk.LoginCode); assertEquals("52817", (r as ShieldSendRisk.LoginCode).code); assertEquals(3, r.minutesAgo)
        assertTrue(ShieldCodes.check("5 2 8 1 7", now + 60_000) is ShieldSendRisk.LoginCode)
        assertTrue(ShieldCodes.check("52-817", now + 60_000) is ShieldSendRisk.LoginCode)
        // Other numbers, and the same code after 15 minutes, pass.
        assertNull(ShieldCodes.check("встретимся в 18:30, квартира 52", now + 60_000))
        assertNull(ShieldCodes.check("52817", now + ShieldCodes.WINDOW_MS + 1))
    }

    @Test fun codeShieldIgnoresNonCodeServiceTexts() {
        assertTrue(ShieldCodes.extract("Telegram обновил условия использования 2024").isEmpty())
        assertEquals(listOf("12345"), ShieldCodes.extract("Login code: 12345. Do not give this code to anyone"))
    }

    @Test fun passwordLooksLike() {
        assertTrue(ShieldCodes.looksLikePassword("Qwerty123!"))
        assertTrue(ShieldCodes.looksLikePassword("k7#Lp9x2Vq"))
        assertTrue(ShieldCodes.check("Tg2fa\$ecret9") is ShieldSendRisk.Password)
        assertFalse(ShieldCodes.looksLikePassword("Отлично!!!"))
        assertFalse(ShieldCodes.looksLikePassword("Привет, как дела?"))
        assertFalse(ShieldCodes.looksLikePassword("https://Kaspi.kz/Pay1"))
        assertFalse(ShieldCodes.looksLikePassword("ivan.petrov@mail.ru"))
        assertFalse(ShieldCodes.looksLikePassword("+7 701 123 45 67"))
        assertFalse(ShieldCodes.looksLikePassword("hello"))
    }

    // ---- Рентген ссылки ----
    @Test fun lookAlikeDomains() {
        assertEquals(0, ShieldLinks.inspect("https://kaspi.kz/pay").level)
        assertTrue(ShieldLinks.inspect("https://online.sberbank.ru/x").trusted)
        listOf("https://te1egram.org/login", "https://telegrarn.org", "http://kaspii.kz", "https://gosuslugl.ru", "https://halykbanк.kz").forEach {
            val v = ShieldLinks.inspect(it)
            assertEquals("$it → ${v.reasons}", 2, v.level)
            assertTrue("$it looks like ${v.looksLike}", v.looksLike != null)
        }
        assertEquals("telegram.org", ShieldLinks.inspect("https://te1egram.org").looksLike)
        // Brand inside a stranger's site.
        val sub = ShieldLinks.inspect("https://kaspi.kz.secure-login.top/auth")
        assertEquals("secure-login.top", sub.site); assertEquals(2, sub.level)
        assertEquals(2, ShieldLinks.inspect("https://kaspi-bonus.top").level)
        // Ordinary sites stay quiet.
        assertEquals(0, ShieldLinks.inspect("https://habr.com/ru/articles/1").level)
        assertEquals(0, ShieldLinks.inspect("https://eda.ru/recepty").level)
    }

    @Test fun punycodeAtIpAndShorteners() {
        val puny = ShieldLinks.inspect("https://xn--telegram-q0h.org") // has a non-Latin letter
        assertTrue(puny.level >= 1)
        val cyr = ShieldLinks.inspect("https://теlеgram.org") // Cyrillic «т», «е»
        assertEquals(2, cyr.level); assertEquals("telegram.org", cyr.looksLike); assertTrue(cyr.oddChars.isNotEmpty())
        val at = ShieldLinks.inspect("https://kaspi.kz@evil.example/pay")
        assertEquals(2, at.level); assertEquals("evil.example", at.host)
        assertEquals(2, ShieldLinks.inspect("http://185.12.44.7/login").level)
        val short = ShieldLinks.inspect("https://bit.ly/3abcd")
        assertEquals(1, short.level); assertTrue(short.reasons.first().contains("Короткая ссылка"))
        assertEquals(2, ShieldLinks.inspect("tg://login?token=abc").level)
        // The visible text names another site.
        assertEquals(2, ShieldLinks.inspect("https://phish.example/k", "kaspi.kz").level)
    }

    @Test fun skeletonAndDistance() {
        assertEquals(ShieldLinks.skeleton("telegram"), ShieldLinks.skeleton("te1egrarn"))
        assertEquals(1, ShieldLinks.levenshtein("kaspi", "kaspii"))
        assertEquals("kaspi.kz", ShieldLinks.registrable("a.b.kaspi.kz"))
        assertEquals("bank.com.kz", ShieldLinks.registrable("www.bank.com.kz"))
    }

    // ---- Рентген APK (pure part) ----
    @Test fun apkAssessment() {
        val troj = ShieldApk.assess("com.bank.update", "Kaspi Обновление", "1.0", "AB:CD", "CN=Android Debug,O=Android,C=US",
            setOf("android.permission.RECEIVE_SMS", "android.permission.BIND_ACCESSIBILITY_SERVICE", "android.permission.INTERNET"))
        assertEquals(2, troj.level)
        assertTrue(troj.debugSigned)
        assertTrue(troj.notes.any { it.contains("Kaspi") })
        assertEquals(2, troj.permissions.size)
        val plain = ShieldApk.assess("org.example.notes", "Notes", "2.1", "11:22", "CN=Example", setOf("android.permission.CAMERA"))
        assertEquals(1, plain.level)
    }
}
