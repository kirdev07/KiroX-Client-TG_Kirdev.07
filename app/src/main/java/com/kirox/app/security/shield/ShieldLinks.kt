package com.kirox.app.security.shield

/** «Рентген ссылки»: what a link really is. [level] 0 = nothing found, 1 = осторожно, 2 = опасно. */
data class ShieldLinkVerdict(
    val url: String,
    /** The real host, decoded from punycode when it was encoded («xn--…» → «теlegram.org»). */
    val host: String,
    /** The site that owns the address («kaspi.kz.secure-login.top» → «secure-login.top»). */
    val site: String,
    val level: Int,
    val reasons: List<String>,
    /** The important site this address imitates, if any. */
    val looksLike: String? = null,
    /** Indices in [host] of characters that are not plain Latin (shown highlighted on the card). */
    val oddChars: Set<Int> = emptySet(),
    /** A known important site (exactly or its subdomain). */
    val trusted: Boolean = false,
)

/**
 * Look-alike domains (skeleton à la Unicode UTS #39 + Levenshtein vs [ShieldDomains.KEY]), punycode, «@» in the
 * address, raw IP addresses, link shorteners, risky zones, a brand name inside someone else's site and Telegram
 * login links. Pure and offline: nothing is fetched.
 */
object ShieldLinks {
    fun inspect(url: String, shownText: String? = null): ShieldLinkVerdict {
        val raw = url.trim()
        val lower = raw.lowercase()
        val reasons = ArrayList<String>()
        var level = 0
        fun flag(l: Int, r: String) { reasons += r; if (l > level) level = l }

        if (lower.startsWith("tg://login") || lower.startsWith("tg:login") || Regex("""^(https?://)?(t\.me|telegram\.me)/login""").containsMatchIn(lower))
            flag(2, "Это ссылка входа: она пускает другое устройство в ваш аккаунт Telegram")

        val noScheme = raw.substringAfter("://", raw)
        val authority = noScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        // «https://kaspi.kz@evil.top/…»: everything before «@» is ignored by the browser.
        if (authority.contains('@')) flag(2, "В адресе есть «@»: всё, что слева («${authority.substringBefore('@')}»), браузер пропустит — откроется ${authority.substringAfterLast('@').substringBefore(':')}")
        val hostRaw = authority.substringAfterLast('@').substringBefore(':').trimEnd('.').lowercase()
        if (hostRaw.isEmpty()) return ShieldLinkVerdict(raw, "", "", level, reasons)
        val host = decodePuny(hostRaw)
        val isIp = Regex("""^\d{1,3}(\.\d{1,3}){3}$""").matches(hostRaw) || hostRaw.startsWith("[") || Regex("""^\d{8,10}$""").matches(hostRaw) || Regex("""^0x[0-9a-f]+$""").matches(hostRaw)
        if (isIp) flag(2, "Вместо названия сайта — IP-адрес $hostRaw: так прячут, кому принадлежит страница")

        val odd = host.indices.filter { i -> val c = host[i]; !(c in 'a'..'z' || c in '0'..'9' || c == '.' || c == '-') }.toSet()
        val puny = hostRaw.split('.').any { it.startsWith("xn--") }
        val site = if (isIp) hostRaw else registrable(host)
        val trusted = !isIp && odd.isEmpty() && ShieldDomains.isKey(site)
        var looks: String? = null

        if (!isIp && !trusted) {
            val label = site.substringBefore('.')
            val skel = skeleton(label)
            val siteSkel = skeleton(site)
            // 1) Same skeleton as a key site, or 1–2 typos away («te1egram.org», «kaspii.kz», «telegrarn.org»).
            for (k in ShieldDomains.KEY) {
                val kl = k.substringBefore('.')
                if (kl.length < 4) continue
                val ks = skeleton(kl)
                val sameShape = siteSkel == skeleton(k) || skel == ks
                val near = !sameShape && label.length >= 5 && levenshtein(skel, ks) <= (if (ks.length >= 9) 2 else 1)
                if (sameShape || near) { looks = k; break }
            }
            if (looks != null) flag(2, "Похоже на ${looks}, но это другой адрес: $site")
            // 2) A brand inside a stranger's site: «kaspi-bonus.top», «kaspi.kz.secure-login.top», «telegram-premium.xyz».
            if (looks == null) {
                val parts = host.removeSuffix(site).trimEnd('.').split('.', '-') + label.split('-')
                val brand = ShieldDomains.BRANDS.firstOrNull { b -> parts.any { p -> p == b || (b.length >= 5 && skeleton(p) == skeleton(b)) } }
                if (brand != null) { looks = ShieldDomains.KEY.firstOrNull { it.substringBefore('.') == brand } ?: brand; flag(2, "Название «$brand» стоит в адресе чужого сайта $site") }
            }
        }
        if (puny || odd.isNotEmpty()) {
            val letters = host.filter { it.isLetter() }
            val mixed = letters.any { it in 'a'..'z' } && letters.any { it !in 'a'..'z' }
            flag(if (looks != null || mixed) 2 else 1, if (mixed) "В адресе смешаны буквы разных алфавитов: $host" else "Адрес записан не латиницей (${if (puny) "punycode" else "особые буквы"}): $host")
        }
        if (!trusted && site in ShieldDomains.SHORTENERS) flag(1, "Короткая ссылка ($site): не видно, куда она ведёт на самом деле")
        val tld = site.substringAfterLast('.', "")
        if (!trusted && !isIp && tld in ShieldDomains.RISKY_ZONES && level > 0) flag(level, "Зона .$tld часто используется для одноразовых сайтов")
        if (!trusted && host.count { it == '.' } >= 4) flag(maxOf(level, 1), "Очень длинный адрес из многих частей — так прячут настоящий сайт в конце")
        // The visible text names another site than the real one.
        val shown = shownText?.let { hostOf(it) }?.let { decodePuny(it) }
        if (shown != null && shown != host && !host.endsWith(".$shown") && registrable(shown) != site) flag(2, "Текст ссылки — $shown, а ведёт она на $host")
        return ShieldLinkVerdict(raw, host, site, level, reasons.distinct(), looks, odd, trusted)
    }

    fun hostOf(text: String): String? = text.trim().lowercase().substringAfter("://").removePrefix("www.").substringBefore('/').substringBefore('?')
        .substringBefore('#').substringAfterLast('@').substringBefore(':').takeIf { it.contains('.') && !it.contains(' ') }

    /** «xn--e1awd7f.com» → its Unicode form; unchanged when it is not punycode or does not decode. */
    fun decodePuny(host: String): String = if (!host.contains("xn--")) host else runCatching { java.net.IDN.toUnicode(host, java.net.IDN.ALLOW_UNASSIGNED) }.getOrDefault(host).lowercase()

    /** eTLD+1 with the common two-part zones («bank.com.kz» → «bank.com.kz», «a.b.kaspi.kz» → «kaspi.kz»). */
    fun registrable(host: String): String {
        val p = host.trimEnd('.').split('.').filter { it.isNotEmpty() }
        if (p.size <= 2) return p.joinToString(".")
        val last2 = p.takeLast(2).joinToString(".")
        return if (last2 in ShieldDomains.TWO_PART_ZONES) p.takeLast(3).joinToString(".") else last2
    }

    /** UTS #39-style skeleton: confusable letters and digits fold to one Latin shape («теlegrаm» → «telegram», «rn» → «m»). */
    fun skeleton(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s.lowercase()) sb.append(CONFUSABLE[c] ?: c)
        return sb.toString().replace("rn", "m").replace("vv", "w").replace("cl", "d").replace("ii", "u")
    }

    private val CONFUSABLE: Map<Char, String> = buildMap {
        // Cyrillic (incl. Kazakh) → Latin look-alikes, then Greek
        "аa вb гr еe ёe зe кk мm нh оo пn рp сc тt уy хx шw ьb ыbl іi їi јj ѕs ԁd ӏl һh ғf қk ңh өo ұy үy әe αa βb εe ιi κk νv οo ρp τt υu χx"
            .split(' ').forEach { put(it[0], it.substring(1)) }
        // digits and symbols that pass for letters
        put('0', "o"); put('1', "l"); put('3', "e"); put('5', "s"); put('4', "a"); put('7', "t"); put('8', "b"); put('9', "g"); put('|', "l"); put('!', "l"); put('ı', "i"); put('ɡ', "g")
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }
}
