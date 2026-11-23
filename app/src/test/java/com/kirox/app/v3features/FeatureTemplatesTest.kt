package com.kirox.app.v3features

import com.kirox.app.ui.v3.data.KxScheduleMapping
import com.kirox.app.ui.v3.model.KxMemoryFeatures
import com.kirox.app.ui.v3.model.KxSendWhen
import com.kirox.app.ui.v3.model.KxTemplate
import com.kirox.app.ui.v3.model.KxTemplateVars
import org.drinkless.tdlib.TdApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FeatureTemplatesTest {
    private val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 23, 14, 5, 0) }
    private val vars = KxTemplateVars.Vars(peerName = "Анна Петрова", myName = "Алекс Миронов", chatTitle = "Анна Петрова", now = now)

    @Test fun expandsKnownVariables() {
        assertEquals("Здравствуйте, Анна! Я Алекс.", KxTemplateVars.expand("Здравствуйте, {имя}! Я {моё_имя}.", vars))
        assertEquals("Анна Петрова · 23 ноября · 14:05 · 24 ноября", KxTemplateVars.expand("{полное_имя} · {дата} · {время} · {завтра}", vars))
        assertEquals("Hi Анна", KxTemplateVars.expand("Hi {name}", vars))
        assertEquals("Я Алекс", KxTemplateVars.expand("Я {мое_имя}", vars))
    }

    @Test fun unknownVariablesAndEmptyNamesStay() {
        assertEquals("Цена {цена} руб.", KxTemplateVars.expand("Цена {цена} руб.", vars))
        assertEquals("Привет, !", KxTemplateVars.expand("Привет, {имя}!", vars.copy(peerName = "")))
        assertEquals("{} и {123}", KxTemplateVars.expand("{} и {123}", vars))
    }

    @Test fun slashMatchesPrefixThenContains() {
        val all = listOf(KxTemplate(1, "адрес", "a"), KxTemplate(2, "привет", "b"), KxTemplate(3, "мойадрес", "c"))
        assertEquals(listOf(1L, 2L, 3L), KxTemplateVars.matches("/", all)!!.map { it.id })
        assertEquals(listOf(1L, 3L), KxTemplateVars.matches("/адр", all)!!.map { it.id })
        assertEquals(emptyList<KxTemplate>(), KxTemplateVars.matches("/zzz", all))
        assertNull(KxTemplateVars.matches("привет", all))
        assertNull(KxTemplateVars.matches("/адрес и ещё", all))
    }

    @Test fun keysAreShortWords() {
        assertTrue(KxTemplateVars.validKey("привет_2"))
        assertFalse(KxTemplateVars.validKey("a"))
        assertFalse(KxTemplateVars.validKey("два слова"))
        assertFalse(KxTemplateVars.validKey("x".repeat(25)))
    }

    @Test fun templatesCreateEditMoveDelete() {
        val f = KxMemoryFeatures()
        f.saveTemplate(KxTemplate(0, "/Привет", "  Здравствуйте  "))
        f.saveTemplate(KxTemplate(0, "адрес", "ул. Примерная"))
        assertEquals(listOf("привет", "адрес"), f.templates.map { it.key })
        assertEquals("Здравствуйте", f.templates[0].text)
        val id = f.templates[0].id
        f.templateUsed(id); f.templateUsed(id)
        f.saveTemplate(f.templates[0].copy(text = "Добрый день"))
        assertEquals(2, f.templates[0].uses)
        assertEquals("Добрый день", f.templates[0].text)
        f.moveTemplate(id, 1)
        assertEquals(listOf("адрес", "привет"), f.templates.map { it.key })
        f.deleteTemplate(id)
        assertEquals(listOf("адрес"), f.templates.map { it.key })
    }

    @Test fun sendWhenOnlineNeedsVisibleLastSeen() {
        assertTrue(KxSendWhen.lastSeenVisible("в сети"))
        assertTrue(KxSendWhen.lastSeenVisible("был(а) 5 минут назад"))
        assertTrue(KxSendWhen.lastSeenVisible("был(а) вчера в 21:14"))
        assertFalse(KxSendWhen.lastSeenVisible("был(а) недавно"))
        assertFalse(KxSendWhen.lastSeenVisible("был(а) на этой неделе"))
        assertFalse(KxSendWhen.lastSeenVisible("был(а) давно"))
        assertFalse(KxSendWhen.lastSeenVisible("бот"))
        assertFalse(KxSendWhen.lastSeenVisible(""))
    }

    @Test fun schedulingStateMapping() {
        assertTrue(KxScheduleMapping.state(KxSendWhen.WHEN_ONLINE) is TdApi.MessageSchedulingStateSendWhenOnline)
        val at = KxScheduleMapping.state(1_800_000_000) as TdApi.MessageSchedulingStateSendAtDate
        assertEquals(1_800_000_000, at.sendDate)
        assertNull(KxScheduleMapping.state(0))
        assertNull(KxScheduleMapping.state(-5))
    }
}
