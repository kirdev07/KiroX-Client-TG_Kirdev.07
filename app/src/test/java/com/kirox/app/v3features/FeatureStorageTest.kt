package com.kirox.app.v3features

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.kirox.app.ui.v3.data.KxFeatureFiles
import com.kirox.app.ui.v3.data.KxFeatureJson
import com.kirox.app.ui.v3.data.KxPcm
import com.kirox.app.ui.v3.features.KxRemindPresets
import com.kirox.app.ui.v3.model.KxBookmark
import com.kirox.app.ui.v3.model.KxFeatureKeys
import com.kirox.app.ui.v3.model.KxNote
import com.kirox.app.ui.v3.model.KxReminder
import com.kirox.app.ui.v3.model.KxTemplate
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class FeatureStorageTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun jsonRoundTrips() {
        val t = listOf(KxTemplate(1, "привет", "Здравствуйте, {имя}!\n\"кавычки\"", 3))
        assertEquals(t, KxFeatureJson.templates(KxFeatureJson.templates(t)))
        val b = listOf(KxBookmark(-100123, 77, "Группа", "Анна", "текст", 1_700_000_000, 1_700_000_000_123))
        assertEquals(b, KxFeatureJson.bookmarks(KxFeatureJson.bookmarks(b)))
        val n = mapOf(5L to KxNote(5, "Анна", "кофе без сахара", 42))
        assertEquals(n, KxFeatureJson.notes(KxFeatureJson.notes(n)))
        val r = listOf(KxReminder(0x5E0001, 5, 9, "Анна", "прочитать", 1_800_000_000_000))
        assertEquals(r, KxFeatureJson.reminders(KxFeatureJson.reminders(r)))
        assertEquals(mapOf("tr.wifiOnly" to 0), KxFeatureJson.settings(KxFeatureJson.settings(mapOf("tr.wifiOnly" to 0))))
    }

    @Test fun brokenJsonIsEmpty() {
        assertTrue(KxFeatureJson.templates("not json").isEmpty())
        assertTrue(KxFeatureJson.bookmarks(null).isEmpty())
        assertTrue(KxFeatureJson.notes("[{\"c\":0,\"t\":\"x\"}]").isEmpty())
    }

    @Test fun bookmarksSavedPerAccountAndPrivate() {
        val a = KxFeatureFiles.of(context, 1001)
        a.addBookmark(KxBookmark(5, 9, "Анна", "Анна", "билеты", 1, 10))
        a.addBookmark(KxBookmark(5, 10, "Анна", "Вы", "ответ", 2, 20))
        a.addBookmark(KxBookmark(5, 9, "Анна", "Анна", "билеты", 1, 30)) // again: moves to the top, no duplicate
        assertEquals(listOf(9L, 10L), a.bookmarks.map { it.messageId })
        assertTrue(a.isBookmarked(5, 10))
        a.removeBookmark(5, 10)
        assertFalse(a.isBookmarked(5, 10))
        val saved = KxFeatureJson.bookmarks(KxFeatureFiles.prefs(context, KxFeatureFiles.PRIVATE).getString("b.1001", null))
        assertEquals(listOf(9L), saved.map { it.messageId })
        assertTrue(KxFeatureFiles.of(context, 2002).bookmarks.isEmpty())
        // Private lists never go to the settings-backup files.
        assertFalse(KxFeatureFiles.prefs(context, KxFeatureFiles.TEMPLATES).all.keys.any { it.startsWith("b.") })
    }

    @Test fun notesRemindersAndSettingsSaved() {
        val f = KxFeatureFiles.of(context, 3003)
        f.setNote(7, "Илья", "  должен 500  ")
        assertEquals("должен 500", KxFeatureJson.notes(KxFeatureFiles.prefs(context, KxFeatureFiles.PRIVATE).getString("n.3003", null))[7L]?.text)
        f.setNote(7, "Илья", " ")
        assertTrue(f.notes.isEmpty())
        val r = f.addReminder(KxReminder(0, 7, 99, "Илья", "посмотреть", System.currentTimeMillis() + 3_600_000))
        assertTrue(r.id >= 0x5E0000)
        assertEquals(listOf(r), KxFeatureFiles.allReminders(context).filter { it.first == 3003L }.map { it.second })
        f.removeReminder(r.id)
        assertTrue(KxFeatureFiles.allReminders(context).none { it.first == 3003L })
        f.putSetting(KxFeatureKeys.TR_WIFI_ONLY, 0)
        assertEquals(0, KxFeatureJson.settings(KxFeatureFiles.prefs(context, KxFeatureFiles.SETTINGS).getString("s", null))[KxFeatureKeys.TR_WIFI_ONLY])
    }

    @Test fun templatesGoToTheBackupFile() {
        val f = KxFeatureFiles.of(context, 4004)
        f.saveTemplate(KxTemplate(0, "адрес", "ул. Примерная"))
        assertEquals("адрес", KxFeatureJson.templates(KxFeatureFiles.prefs(context, KxFeatureFiles.TEMPLATES).getString("t.4004", null)).single().key)
    }

    @Test fun pcmMonoAndResample() {
        assertArrayEquals(shortArrayOf(15, -5), KxPcm.mono(shortArrayOf(10, 20, -10, 0), 2))
        val r = KxPcm.resample(ShortArray(48_000) { 100 }, 48_000)
        assertEquals(16_000, r.size)
        assertTrue(r.all { it.toInt() == 100 })
        assertArrayEquals(byteArrayOf(1, 0, -1, -1), KxPcm.bytes(shortArrayOf(1, -1)))
    }

    @Test fun remindPresetsAreInTheFuture() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 23, 21, 30, 0) }
        val p = KxRemindPresets.of(now)
        assertTrue(p.all { it.at > now.timeInMillis })
        assertFalse(p.any { it.title == "Сегодня вечером" }) // 21:30 is past the evening slot
    }
}
