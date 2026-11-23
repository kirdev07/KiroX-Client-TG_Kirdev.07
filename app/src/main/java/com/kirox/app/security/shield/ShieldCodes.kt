package com.kirox.app.security.shield

import java.util.concurrent.CopyOnWriteArrayList

/** What [ShieldCodes.check] found in a message the user is about to send. */
sealed interface ShieldSendRisk {
    /** A fresh Telegram login code that arrived [minutesAgo] minutes ago from the service account. */
    data class LoginCode(val code: String, val minutesAgo: Int) : ShieldSendRisk
    /** A single strong token that looks like a password (cloud password / 2FA). */
    data object Password : ShieldSendRisk
}

/**
 * «Код-щит»: kiroX remembers, in memory only and for [WINDOW_MS], the login codes Telegram's service account
 * (777000) sends, and stops the user before those digits are sent to anyone. The codes are never written to disk
 * and forgotten after the window.
 */
object ShieldCodes {
    const val SERVICE_USER = 777000L
    const val WINDOW_MS = 15 * 60_000L

    private data class Seen(val code: String, val at: Long)
    private val seen = CopyOnWriteArrayList<Seen>()

    private val CODE = Regex("""(?<![\d\-])(\d{5,6})(?![\d\-])""")

    /** Codes in a service message («Код для входа в Telegram: 12345…»). */
    fun extract(text: String): List<String> {
        val t = text.lowercase()
        // Telegram's code messages talk about a code / login; other service texts (terms, notices) are skipped.
        if (!(t.contains("код") || t.contains("code") || t.contains("login") || t.contains("вход") || t.contains("кіру"))) return emptyList()
        return CODE.findAll(text).map { it.groupValues[1] }.distinct().toList()
    }

    /** Called for every new message of the service chat (see [ShieldCodeWatcher]). */
    fun remember(serviceText: String, now: Long = System.currentTimeMillis()) {
        prune(now)
        extract(serviceText).forEach { seen += Seen(it, now) }
    }

    fun recent(now: Long = System.currentTimeMillis()): List<Pair<String, Long>> { prune(now); return seen.map { it.code to it.at } }

    fun forgetAll() = seen.clear()

    private fun prune(now: Long) { seen.removeAll { now - it.at > WINDOW_MS || it.at > now + 60_000 } }

    /**
     * Risk of sending [outgoing]: a remembered code written plainly or split («12 345», «1-2-3-4-5»), else a
     * password-looking token, else null.
     */
    fun check(outgoing: String, now: Long = System.currentTimeMillis(), codes: List<Pair<String, Long>> = recent(now)): ShieldSendRisk? {
        val text = outgoing.trim()
        if (text.isEmpty()) return null
        if (codes.isNotEmpty()) {
            // Digit runs joined across spaces, dots and dashes only (not across words), so «12 345» counts but a date + a price do not.
            val joined = Regex("""\d(?:[\s.\-–—_]*\d)*""").findAll(text).map { m -> m.value.filter { it.isDigit() } }.toList()
            for ((code, at) in codes) {
                if (joined.any { it == code || (it.length <= code.length + 2 && it.contains(code)) })
                    return ShieldSendRisk.LoginCode(code, ((now - at) / 60_000L).toInt().coerceAtLeast(0))
            }
        }
        if (looksLikePassword(text)) return ShieldSendRisk.Password
        return null
    }

    /** One 8..64-character token with at least three of: lower, upper, digit, symbol; not a link, mail, @name or number. */
    fun looksLikePassword(text: String): Boolean {
        val t = text.trim()
        if (t.length !in 8..64 || t.any { it.isWhitespace() }) return false
        val low = t.lowercase()
        if (low.startsWith("http") || low.startsWith("www.") || low.contains("://") || t.startsWith("@") || t.startsWith("#") || t.startsWith("/")) return false
        if (Regex("""^[^@\s]+@[^@\s]+\.[a-z]{2,}$""").matches(low)) return false
        if (Regex("""^[a-z0-9-]+(\.[a-z0-9-]+)+(/.*)?$""").matches(low)) return false // a bare domain
        if (t.all { it.isDigit() || it in "+-() ." }) return false
        // Plain words with a capital and a trailing «!» or digits from a chat («Привет!!», «Спасибо2») are not passwords.
        val classes = listOf(t.any { it.isLowerCase() }, t.any { it.isUpperCase() }, t.any { it.isDigit() }, t.any { !it.isLetterOrDigit() }).count { it }
        if (classes < 3 || t.none { it.isDigit() }) return false
        // «Отлично!!!» has no digit; «Qwerty123!» / «k7#Lp9x2» switch between kinds of characters several times.
        val switches = t.zipWithNext().count { (a, b) -> kind(a) != kind(b) }
        return switches >= 2
    }

    private fun kind(c: Char) = when { c.isDigit() -> 0; c.isUpperCase() -> 3; c.isLetter() -> 1; else -> 2 }
}
