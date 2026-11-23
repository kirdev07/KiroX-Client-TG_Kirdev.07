package com.kirox.app.v3data

import com.kirox.app.telegram.TelegramMessageKind
import com.kirox.app.telegram.TelegramUserStatus
import com.kirox.app.ui.v3.data.KxFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

/** Russian formatting of ui/v3/data/KxFormat against official Telegram (ru) and the v3 mockups. */
class KxFormatTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val now = 1_795_348_800_000L // Sun 2026-11-22 12:00 UTC
    private val nowS = (now / 1000).toInt()

    @Test fun chatTimeTodayYesterdayWeekYearOlder() {
        assertEquals("09:05", KxFormat.chatTime(1_795_338_300, now, utc))
        assertEquals("вчера", KxFormat.chatTime(1_795_305_540, now, utc)) // Sat 23:59
        assertEquals("вт", KxFormat.chatTime(1_794_909_600, now, utc))
        assertEquals("пн", KxFormat.chatTime(1_794_823_200, now, utc)) // 6 days back
        assertEquals("15 нояб.", KxFormat.chatTime(1_794_823_200 - 86_400, now, utc)) // 7 days back
        assertEquals("3 февр.", KxFormat.chatTime(1_770_112_800, now, utc))
        assertEquals("31.12.25", KxFormat.chatTime(1_767_175_200, now, utc))
        assertEquals("", KxFormat.chatTime(0, now, utc))
    }

    @Test fun chatTimeUsesTheLocalDayNotUtc() {
        // 23:30 UTC on Saturday is already Sunday 02:30 in Moscow (UTC+3).
        val msk = TimeZone.getTimeZone("Europe/Moscow")
        assertEquals("02:30", KxFormat.chatTime(1_795_305_540 - 29 * 60, now, msk))
        assertEquals("вчера", KxFormat.chatTime(1_795_305_540 - 29 * 60, now, utc))
    }

    @Test fun messageTimeIsTwoDigitHours() {
        assertEquals("09:05", KxFormat.messageTime(1_795_338_300, utc))
        assertEquals("", KxFormat.messageTime(0, utc))
    }

    @Test fun dayHeaders() {
        assertEquals("Сегодня", KxFormat.dayHeader(1_795_338_300, now, utc))
        assertEquals("Вчера", KxFormat.dayHeader(1_795_305_540, now, utc))
        assertEquals("17 ноября", KxFormat.dayHeader(1_794_909_600, now, utc))
        assertEquals("31 декабря 2025", KxFormat.dayHeader(1_767_175_200, now, utc))
        assertTrue(KxFormat.sameDay(1_795_338_300, nowS, utc))
        assertFalse(KxFormat.sameDay(1_795_305_540, nowS, utc))
    }

    @Test fun russianPlurals() {
        fun p(n: Int) = KxFormat.plural(n, "участник", "участника", "участников")
        assertEquals("участник", p(1)); assertEquals("участника", p(2)); assertEquals("участника", p(4))
        assertEquals("участников", p(5)); assertEquals("участников", p(11)); assertEquals("участников", p(12))
        assertEquals("участников", p(14)); assertEquals("участник", p(21)); assertEquals("участника", p(22))
        assertEquals("участников", p(0)); assertEquals("участников", p(111)); assertEquals("участник", p(101))
        assertEquals("1 участник", KxFormat.members(1))
        assertEquals("1 284 участника", KxFormat.members(1284))
        assertEquals("24 500 подписчиков", KxFormat.subscribers(24_500))
        assertEquals("1 000 000 подписчиков", KxFormat.subscribers(1_000_000))
        assertEquals("999 подписчиков", KxFormat.subscribers(999))
    }

    @Test fun durationsAndSizes() {
        assertEquals("0:24", KxFormat.duration(24))
        assertEquals("1:02:03", KxFormat.duration(3723))
        assertEquals("42 сек", KxFormat.callDuration(42))
        assertEquals("12 мин", KxFormat.callDuration(12 * 60))
        assertEquals("1 ч 5 мин", KxFormat.callDuration(3900))
        assertEquals("", KxFormat.callDuration(0))
        assertEquals("2,4 МБ", KxFormat.fileSize((2.4 * 1024 * 1024).toLong()))
        assertEquals("640 КБ", KxFormat.fileSize(640 * 1024))
        assertEquals("1,5 ГБ", KxFormat.fileSize(3L shl 29))
    }

    /** 59.5+ minutes must not become «60 мин». */
    @Test fun callDurationNeverSixtyMinutes() {
        assertEquals("1 ч", KxFormat.callDuration(3590))
    }

    /** Digits must stay Latin on an Arabic/Persian system locale (String.format uses the default locale). */
    @Test fun formattingIgnoresSystemLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("09:05", KxFormat.messageTime(1_795_338_300, utc))
            assertEquals("0:24", KxFormat.duration(24))
            assertEquals("2,4 МБ", KxFormat.fileSize((2.4 * 1024 * 1024).toLong()))
        } finally { Locale.setDefault(saved) }
    }

    @Test fun storyAgo() {
        assertEquals("только что", KxFormat.ago(nowS - 20, now, utc))
        assertEquals("5 мин назад", KxFormat.ago(nowS - 300, now, utc))
        assertEquals("3 ч назад", KxFormat.ago(nowS - 3 * 3600, now, utc))
        assertEquals("", KxFormat.ago(0, now, utc))
    }

    @Test fun compactViews() {
        assertEquals("999", KxFormat.compactCount(999))
        assertEquals("1,2K", KxFormat.compactCount(1_234))
        assertEquals("24K", KxFormat.compactCount(24_000))
        assertEquals("1,5M", KxFormat.compactCount(1_500_000))
    }

    /** 999 950 views must read «1M», not «1000K». */
    @Test fun compactViewsRollOver() {
        assertEquals("1M", KxFormat.compactCount(999_950))
    }

    @Test fun lastSeen() {
        assertEquals("в сети", KxFormat.status(TelegramUserStatus.Online, now, utc))
        assertEquals("в сети только что", KxFormat.status(TelegramUserStatus.Offline(nowS - 20), now, utc))
        assertEquals("в сети 1 минуту назад", KxFormat.status(TelegramUserStatus.Offline(nowS - 60), now, utc))
        assertEquals("в сети 2 минуты назад", KxFormat.status(TelegramUserStatus.Offline(nowS - 120), now, utc))
        assertEquals("в сети 5 минут назад", KxFormat.status(TelegramUserStatus.Offline(nowS - 300), now, utc))
        assertEquals("в сети 21 минуту назад", KxFormat.status(TelegramUserStatus.Offline(nowS - 21 * 60), now, utc))
        assertEquals("в сети в 09:05", KxFormat.status(TelegramUserStatus.Offline(1_795_338_300), now, utc))
        assertEquals("в сети вчера в 23:59", KxFormat.status(TelegramUserStatus.Offline(1_795_305_540), now, utc))
        assertEquals("в сети 17 нояб. в 10:00", KxFormat.status(TelegramUserStatus.Offline(1_794_909_600), now, utc))
        assertEquals("в сети 31.12.25", KxFormat.status(TelegramUserStatus.Offline(1_767_175_200), now, utc))
        assertEquals("в сети недавно", KxFormat.status(TelegramUserStatus.Recently, now, utc))
        assertEquals("в сети на этой неделе", KxFormat.status(TelegramUserStatus.LastWeek, now, utc))
        assertEquals("", KxFormat.status(null, now, utc))
    }

    @Test fun previewsLikeTelegram() {
        assertEquals("🎤 Голосовое сообщение · 0:24", KxFormat.preview("Голосовое сообщение", TelegramMessageKind.Voice, 24))
        assertEquals("📹 Видеосообщение · 0:05", KxFormat.preview("Видеосообщение", TelegramMessageKind.VideoNote, 5))
        assertEquals("📷 Фото", KxFormat.preview("Фотография", TelegramMessageKind.Photo, 0))
        assertEquals("📷 Закат", KxFormat.preview("Закат", TelegramMessageKind.Photo, 0))
        assertEquals("📎 Файл", KxFormat.preview("Документ", TelegramMessageKind.Document, 0))
        assertEquals("😀 Стикер", KxFormat.preview("Стикер 😀", TelegramMessageKind.Sticker, 0))
        assertEquals("Стикер", KxFormat.preview("Стикер", TelegramMessageKind.Sticker, 0))
        assertEquals("GIF", KxFormat.preview("GIF", TelegramMessageKind.Animation, 0))
        assertEquals("строка 1 строка 2", KxFormat.preview("строка 1\nстрока 2", TelegramMessageKind.Text, 0))
    }

    /** A plain text message that happens to read «Опрос» or «Звонок» must not get a media emoji. */
    @Test fun plainTextIsNeverLabelled() {
        assertEquals("Опрос", KxFormat.preview("Опрос", TelegramMessageKind.Text, 0))
        assertEquals("Звонок", KxFormat.preview("Звонок", TelegramMessageKind.Text, 0))
        assertEquals("Геопозиция", KxFormat.preview("Геопозиция", TelegramMessageKind.Text, 0))
    }
}
