package com.kirox.app.security.shield

/**
 * «Защита kiroX»: text normalisation and features shared by the signal rules and the learned model.
 * Pure Kotlin (no Android), so it runs the same in unit tests and on the phone. Nothing here stores or sends text.
 */
object ShieldText {
    /** Lower case, «ё» → «е», look-alike Latin letters inside Cyrillic words → Cyrillic (scammers write «кoд» with a Latin «o»). */
    fun normalize(raw: String): String {
        val s = raw.lowercase().replace('ё', 'е')
        val out = StringBuilder(s.length)
        for (i in s.indices) {
            val c = s[i]
            // A Latin look-alike between two Cyrillic letters is read as Cyrillic.
            val prevCyr = i > 0 && isCyr(s[i - 1])
            val nextCyr = i + 1 < s.length && isCyr(s[i + 1])
            out.append(if ((prevCyr || nextCyr) && c in LAT_TO_CYR) LAT_TO_CYR.getValue(c) else c)
        }
        return out.toString()
    }

    private val LAT_TO_CYR = mapOf('a' to 'а', 'e' to 'е', 'o' to 'о', 'p' to 'р', 'c' to 'с', 'x' to 'х', 'y' to 'у', 'k' to 'к', 'm' to 'м', 't' to 'т', 'h' to 'н', 'b' to 'в')

    fun isCyr(c: Char) = c in 'а'..'я' || c == 'ё' || c in "әғқңөұүһі"

    /** Words of [normalized]: letters/digits runs; a digit run becomes a shape token («#5» = five digits). */
    fun words(normalized: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        fun flush() {
            if (sb.isEmpty()) return
            val w = sb.toString(); sb.setLength(0)
            out += if (w.all { it.isDigit() }) "#" + w.length.coerceAtMost(9) else w
        }
        for (c in normalized) if (c.isLetterOrDigit()) sb.append(c) else flush()
        flush()
        return out
    }

    /**
     * Features of the learned model: word unigrams, a 5-letter stem (Russian/Kazakh endings vary a lot), word
     * bigrams, character 4-grams and shape flags (link, money, «!!», caps). Each feature counts once per message.
     * Chosen on the seed's held-out split (words+stems+bigrams 90.5 %, + char 4-grams 93.7 %).
     */
    fun features(raw: String): Set<String> {
        val n = normalize(raw)
        val w = words(n)
        val f = LinkedHashSet<String>(w.size * 3 + 4)
        for (i in w.indices) {
            val t = w[i]
            if (t.length < 2 && !t.startsWith("#")) continue
            f += t
            if (t.length > 5 && !t.startsWith("#")) f += "~" + t.take(5)
            if (i + 1 < w.size) f += t + "_" + w[i + 1]
            // Character 4-grams with word edges («^пер», «евод», «оду$»): Russian/Kazakh endings and typos still match.
            if (t.length >= 4 && !t.startsWith("#")) { val x = "^$t$"; for (j in 0..x.length - 4) f += "%" + x.substring(j, j + 4) }
        }
        if (LINK.containsMatchIn(n)) f += "!link"
        if (MONEY.containsMatchIn(n)) f += "!money"
        if (raw.count { it == '!' } >= 2) f += "!bang"
        if (raw.length > 12 && raw.count { it.isUpperCase() } * 2 > raw.count { it.isLetter() }) f += "!caps"
        return f
    }

    val LINK = Regex("""(https?://|www\.|\b[a-z0-9-]{2,}\.(ru|kz|com|top|xyz|me|org|net|io|site|online|click|link|shop|info|live|pro|app)\b)""")
    val MONEY = Regex("""(\d[\d\s]*(руб|р\.|₽|тг|тенге|₸|\$|usd|usdt|ton|btc|eth|долл|евро|€))|(\$\s?\d)""")
}
