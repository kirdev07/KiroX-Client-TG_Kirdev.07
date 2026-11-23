package com.kirox.app.v3data

import com.kirox.app.ui.v3.model.KxEntity
import com.kirox.app.ui.v3.model.KxEntityType
import com.kirox.app.ui.v3.screens.KxChatLinks
import com.kirox.app.ui.v3.screens.KxLinkTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Tappable links in v3 chat bubbles (ui/v3/screens/ChatLinks.kt). */
class ChatLinksTest {
    private fun at(text: String, part: String, type: KxEntityType, url: String? = null) = KxEntity(text.indexOf(part), part.length, type, url)

    @Test fun detectedLinks() {
        val t = "Смотри example.com/a, пиши a@b.kz, звони +7 701 000 00 00"
        assertEquals(KxLinkTarget.Web("https://example.com/a"), KxChatLinks.target(t, at(t, "example.com/a", KxEntityType.Link)))
        assertEquals(KxLinkTarget.External("mailto:a@b.kz"), KxChatLinks.target(t, at(t, "a@b.kz", KxEntityType.Link)))
        assertEquals(KxLinkTarget.External("tel:+77010000000"), KxChatLinks.target(t, at(t, "+7 701 000 00 00", KxEntityType.Link)))
        // the url the data layer already filled wins
        assertEquals(KxLinkTarget.External("tel:+7701"), KxChatLinks.target(t, at(t, "+7 701 000 00 00", KxEntityType.Link, "tel:+7701")))
    }

    @Test fun mentionsHashtagsCommands() {
        val t = "@durov #новости /start Иван"
        assertEquals(KxLinkTarget.Username("durov"), KxChatLinks.target(t, at(t, "@durov", KxEntityType.Mention)))
        assertEquals(KxLinkTarget.Hashtag("новости"), KxChatLinks.target(t, at(t, "#новости", KxEntityType.Hashtag)))
        assertEquals(KxLinkTarget.Command("/start"), KxChatLinks.target(t, at(t, "/start", KxEntityType.Mention)))
        assertEquals(KxLinkTarget.User(42), KxChatLinks.target(t, at(t, "Иван", KxEntityType.Mention, "tg://user?id=42")))
        assertNull(KxChatLinks.target(t, KxEntity(20, 40, KxEntityType.Mention)))
        assertNull(KxChatLinks.target(t, at(t, "Иван", KxEntityType.Bold)))
    }

    @Test fun telegramLinksStayInApp() {
        assertEquals(KxLinkTarget.Username("kirox_news"), KxChatLinks.url("https://t.me/kirox_news/123"))
        assertEquals(KxLinkTarget.Username("kirox_news"), KxChatLinks.url("t.me/kirox_news"))
        assertEquals(KxLinkTarget.Username("kirox"), KxChatLinks.url("tg://resolve?domain=kirox"))
        assertEquals(KxLinkTarget.Invite("https://t.me/+AbCdEf"), KxChatLinks.url("https://t.me/+AbCdEf"))
        assertEquals(KxLinkTarget.Web("https://t.me/addstickers/pack"), KxChatLinks.url("https://t.me/addstickers/pack"))
        assertEquals(KxLinkTarget.External("geo:0,0"), KxChatLinks.url("geo:0,0"))
    }

    @Test fun warnings() {
        val t = "Расписание: вот тут, сайт rasp.kz и kaspi.kz"
        // text_url whose words are not the address → ask
        assertNotNull(KxChatLinks.warning(t, at(t, "вот тут", KxEntityType.TextLink, "https://rasp.example.kz/trains")))
        // text_url showing its own address → no question
        assertNull(KxChatLinks.warning(t, at(t, "rasp.kz", KxEntityType.TextLink, "https://rasp.kz/")))
        // text names another site
        assertNotNull(KxChatLinks.warning(t, at(t, "kaspi.kz", KxEntityType.TextLink, "https://kasp1.top/login")))
        // plain detected link: fine unless the address itself looks forged
        assertNull(KxChatLinks.warning(t, at(t, "rasp.kz", KxEntityType.Link)))
        assertNotNull(KxChatLinks.warning("xn--80ak6aa92e.com", KxEntity(0, 18, KxEntityType.Link)))
        assertNull(KxChatLinks.warning(t, at(t, "вот тут", KxEntityType.Mention)))
    }
}
