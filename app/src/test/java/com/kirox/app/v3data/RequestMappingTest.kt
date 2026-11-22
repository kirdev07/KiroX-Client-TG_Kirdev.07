package com.kirox.app.v3data

import com.kirox.app.telegram.TelegramMessageKind
import com.kirox.app.ui.v3.data.KxRequestMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** «Незнакомцы» card texts (KxRequestMapping in ui/v3/data/TelegramKxLocal.kt, binder2). */
class RequestMappingTest {
    @Test fun metaLine() {
        assertEquals("@erlan_s · 2 общие группы · нет фото", KxRequestMapping.meta("erlan_s", false, 2))
        assertEquals("нет общих чатов · нет фото", KxRequestMapping.meta("", false, 0))
        assertEquals("1 общая группа", KxRequestMapping.meta(" ", true, 1))
        assertEquals("5 общих групп", KxRequestMapping.meta("", true, 5))
        assertEquals("11 общих групп", KxRequestMapping.meta("", true, 11))
        assertEquals("21 общая группа", KxRequestMapping.meta("", true, 21))
        assertEquals("22 общие группы", KxRequestMapping.meta("", true, 22))
    }

    @Test fun hiddenMedia() {
        assertEquals("Фото скрыто", KxRequestMapping.hiddenMedia(TelegramMessageKind.Photo))
        assertEquals("Видео скрыто", KxRequestMapping.hiddenMedia(TelegramMessageKind.Video))
        assertEquals("Видео скрыто", KxRequestMapping.hiddenMedia(TelegramMessageKind.Animation))
        assertEquals("Файл скрыт", KxRequestMapping.hiddenMedia(TelegramMessageKind.Document))
        assertNull(KxRequestMapping.hiddenMedia(TelegramMessageKind.Text))
    }
}
