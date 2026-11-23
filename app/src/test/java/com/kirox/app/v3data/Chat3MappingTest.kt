package com.kirox.app.v3data

import com.kirox.app.ui.v3.data.KxChat3Mapping
import com.kirox.app.ui.v3.model.KxEntity
import com.kirox.app.ui.v3.model.KxEntityType
import org.drinkless.tdlib.TdApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Pure parts of ui/v3/data/TelegramKxChat3 (wave3-chat): entities on send, detected entities, link warnings, birthdays. */
class Chat3MappingTest {
    @Test fun entitiesOnSend() {
        val f = KxChat3Mapping.formatted("Привет мир", listOf(
            KxEntity(0, 6, KxEntityType.Bold), KxEntity(7, 3, KxEntityType.TextLink, "https://ex.com"),
            KxEntity(7, 3, KxEntityType.TextLink, null), KxEntity(0, 6, KxEntityType.Mention), KxEntity(5, 40, KxEntityType.Italic),
        ))
        assertEquals(2, f.entities.size)
        assertTrue(f.entities[0].type is TdApi.TextEntityTypeBold)
        assertEquals("https://ex.com", (f.entities[1].type as TdApi.TextEntityTypeTextUrl).url)
    }

    @Test fun detected() {
        val text = "Пиши на a@b.kz или звони +7 701 000"
        val mail = KxChat3Mapping.detected(text, TdApi.TextEntity(8, 6, TdApi.TextEntityTypeEmailAddress()))
        assertEquals("mailto:a@b.kz", mail?.url)
        val phone = KxChat3Mapping.detected(text, TdApi.TextEntity(25, 10, TdApi.TextEntityTypePhoneNumber()))
        assertEquals("tel:+7701000", phone?.url)
        assertEquals("https://kotlin.club", KxChat3Mapping.detected("kotlin.club", TdApi.TextEntity(0, 11, TdApi.TextEntityTypeUrl()))?.url)
        assertNull(KxChat3Mapping.detected("x", TdApi.TextEntity(0, 9, TdApi.TextEntityTypeUrl())))
    }

    @Test fun linkWarnings() {
        assertEquals("sub.example.com", KxChat3Mapping.host("https://Sub.Example.com/a?b"))
        assertNull(KxChat3Mapping.linkWarning("example.com", "https://example.com/page"))
        assertNull(KxChat3Mapping.linkWarning("example.com", "https://www.mail.example.com"))
        assertNotNull(KxChat3Mapping.linkWarning("kaspi.kz", "https://kaspi-bonus.top/login"))
        assertNotNull(KxChat3Mapping.linkWarning("", "https://xn--80ak6aa92e.com"))
        assertNotNull(KxChat3Mapping.linkWarning("", "https://kаspi.kz")) // Cyrillic «а»
        assertNull(KxChat3Mapping.linkWarning("нажми сюда", "https://kotlin.club"))
    }

    @Test fun birthdays() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 23, 15, 0) }
        assertEquals(0, KxChat3Mapping.daysUntil(23, 11, now))
        assertEquals(1, KxChat3Mapping.daysUntil(24, 11, now))
        assertEquals(364, KxChat3Mapping.daysUntil(22, 11, now))
        assertEquals(98, KxChat3Mapping.daysUntil(29, 2, now)) // 2027 has no 29.02 → 1 March
    }
}
