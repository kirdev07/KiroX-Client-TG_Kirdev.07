package com.kirox.app.ui.v3.screens

import com.kirox.app.ui.v3.data.KxChat3Mapping
import com.kirox.app.ui.v3.model.KxEntity
import com.kirox.app.ui.v3.model.KxEntityType

/** Where a tap on a link-like entity in a chat bubble leads. */
sealed interface KxLinkTarget {
    /** http(s) page: in-app browser (setting «Встроенный браузер») or the phone's browser. */
    data class Web(val url: String) : KxLinkTarget
    /** tel:, mailto: and other schemes: handed to the system. */
    data class External(val uri: String) : KxLinkTarget
    /** @username or t.me/username: the public chat in kiroX. */
    data class Username(val name: String) : KxLinkTarget
    /** A mention by name without a username (tg://user?id=…). */
    data class User(val userId: Long) : KxLinkTarget
    /** t.me/+… / t.me/joinchat/…: the JoinRequest board. */
    data class Invite(val link: String) : KxLinkTarget
    data class Hashtag(val tag: String) : KxLinkTarget
    /** /command of a bot: sent to the chat, like Telegram. */
    data class Command(val command: String) : KxLinkTarget
}

/** Pure link logic of the v3 chat bubbles (tested in v3data/ChatLinksTest). */
object KxChatLinks {
    private val tappable = setOf(KxEntityType.Link, KxEntityType.TextLink, KxEntityType.Mention, KxEntityType.Hashtag)

    fun isTappable(e: KxEntity) = e.type in tappable

    /** The visible text of [e] in [text], null when the range is outside the text. */
    fun part(text: String, e: KxEntity): String? {
        if (e.offset < 0 || e.length <= 0 || e.offset + e.length > text.length) return null
        return text.substring(e.offset, e.offset + e.length)
    }

    fun target(text: String, e: KxEntity): KxLinkTarget? {
        val part = part(text, e) ?: return null
        val url = e.url?.trim()?.takeIf { it.isNotEmpty() }
        return when (e.type) {
            KxEntityType.Hashtag -> part.trimStart('#', '$').takeIf { it.isNotBlank() }?.let { KxLinkTarget.Hashtag(it) }
            KxEntityType.Mention -> when {
                url != null && url.startsWith("tg://user?id=") -> url.substringAfter("id=").toLongOrNull()?.let { KxLinkTarget.User(it) }
                part.startsWith("/") -> KxLinkTarget.Command(part)
                part.startsWith("@") && part.length > 1 -> KxLinkTarget.Username(part.substring(1))
                else -> null
            }
            KxEntityType.Link -> url(url ?: guess(part))
            KxEntityType.TextLink -> url?.let { url(it) }
            else -> null
        }
    }

    /** Address for a detected link without an explicit one: e-mail, phone or a web address. */
    internal fun guess(part: String): String {
        val p = part.trim()
        return when {
            p.contains("://") -> p
            p.contains('@') && !p.startsWith("@") -> "mailto:$p"
            p.none { it.isLetter() } && p.count { it.isDigit() } >= 5 && !p.contains('.') -> "tel:" + p.filter { it.isDigit() || it == '+' }
            else -> "https://$p"
        }
    }

    /** Sorts a full address into a target: Telegram links stay in kiroX, the web goes to a browser, other schemes to the system. */
    fun url(raw: String): KxLinkTarget? {
        val u = raw.trim()
        if (u.isEmpty()) return null
        // "geo:0,0", "tel:…", "https://…"; "site.kz:8080/x" has no scheme (a dot before the colon = host:port).
        val scheme = Regex("^([A-Za-z][A-Za-z0-9+-]*):").find(u)?.groupValues?.get(1)?.lowercase()
            ?.takeUnless { u.substringAfter(':').let { rest -> rest.firstOrNull()?.isDigit() == true && !u.contains("://") && rest.takeWhile { it.isDigit() }.length == rest.substringBefore('/').length } }
            .orEmpty()
        if (scheme == "tg") {
            val q = u.substringAfter("?", "")
            val domain = q.split('&').firstOrNull { it.startsWith("domain=") }?.substringAfter('=')
            if (u.startsWith("tg://resolve") && !domain.isNullOrBlank()) return KxLinkTarget.Username(domain)
            if (u.startsWith("tg://user?id=")) return u.substringAfter("id=").substringBefore('&').toLongOrNull()?.let { KxLinkTarget.User(it) }
            return KxLinkTarget.External(u)
        }
        if (scheme.isNotEmpty() && scheme != "http" && scheme != "https") return KxLinkTarget.External(u)
        val web = if (scheme.isEmpty()) "https://$u" else u
        val host = KxChat3Mapping.host(web)
        if (host == "t.me" || host == "telegram.me" || host == "telegram.dog") {
            val path = web.substringAfter("://").substringAfter('/', "").substringBefore('?').substringBefore('#').trimEnd('/')
            val first = path.substringBefore('/')
            when {
                first.startsWith("+") || first == "joinchat" -> return KxLinkTarget.Invite(web)
                first.matches(Regex("[A-Za-z][A-Za-z0-9_]{3,31}")) && first.lowercase() !in TME_SERVICE -> return KxLinkTarget.Username(first)
            }
        }
        return KxLinkTarget.Web(web)
    }

    private val TME_SERVICE = setOf("addstickers", "addemoji", "addlist", "addtheme", "share", "proxy", "socks", "setlanguage", "login", "invoice", "confirmphone", "iv", "boost", "giftcode", "contact", "bg", "c")

    /**
     * Why a tap must ask first, or null: the visible text names something other than the real address (a text_url
     * whose words are not that address), or the address itself looks forged ([KxChat3Mapping.linkWarning]).
     */
    fun warning(text: String, e: KxEntity): String? {
        if (e.type != KxEntityType.Link && e.type != KxEntityType.TextLink) return null
        val part = part(text, e) ?: return null
        val url = e.url?.trim()?.takeIf { it.isNotEmpty() } ?: guess(part)
        if (url.startsWith("tel:") || url.startsWith("mailto:")) return null
        KxChat3Mapping.linkWarning(part, url)?.let { return it }
        if (e.type == KxEntityType.TextLink && !sameAddress(part, url)) return "Текст ссылки не совпадает с адресом"
        return null
    }

    private fun norm(s: String) = s.trim().lowercase().removePrefix("http://").removePrefix("https://").removePrefix("www.").trimEnd('/')
    internal fun sameAddress(shown: String, url: String) = norm(shown) == norm(url)
}
