package com.kirox.app.ui.v3.data

import com.kirox.app.telegram.TelegramMessageKind
import com.kirox.app.telegram.TelegramUserStatus
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Pure text formatting for the v3 UI (Russian, as in the mockups). No TDLib, no Android: [now] (ms)
 * and [zone] are parameters so the TG tester can check every case in plain unit tests.
 */
internal object KxFormat {
    private val shortMonths = arrayOf("янв.", "февр.", "мар.", "апр.", "мая", "июн.", "июл.", "авг.", "сент.", "окт.", "нояб.", "дек.")
    private val months = arrayOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    private val weekdays = arrayOf("вс", "пн", "вт", "ср", "чт", "пт", "сб") // Calendar.SUNDAY == 1

    /** Chat list / call time: today «HH:mm», yesterday «вчера», 2–6 days «пн», this year «12 нояб.», older «12.11.25». */
    fun chatTime(unix: Int, now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): String {
        if (unix <= 0) return ""
        val at = cal(unix * 1000L, zone)
        val days = daysBetween(unix * 1000L, now, zone)
        return when {
            days <= 0 -> hhmm(at)
            days == 1 -> "вчера"
            days in 2..6 -> weekdays[at.get(Calendar.DAY_OF_WEEK) - 1]
            at.get(Calendar.YEAR) == cal(now, zone).get(Calendar.YEAR) -> "${at.get(Calendar.DAY_OF_MONTH)} ${shortMonths[at.get(Calendar.MONTH)]}"
            else -> "%02d.%02d.%02d".format(Locale.ROOT, at.get(Calendar.DAY_OF_MONTH), at.get(Calendar.MONTH) + 1, at.get(Calendar.YEAR) % 100)
        }
    }

    /** Story age: «только что», «15 мин назад», «3 ч назад», then like [chatTime]. */
    fun ago(unix: Int, now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): String {
        if (unix <= 0) return ""
        val minutes = ((now / 1000 - unix) / 60).toInt()
        return when {
            minutes < 1 -> "только что"
            minutes < 60 -> "$minutes мин назад"
            minutes < 24 * 60 -> "${minutes / 60} ч назад"
            else -> chatTime(unix, now, zone)
        }
    }

    /** Bubble time «09:05». */
    fun messageTime(unix: Int, zone: TimeZone = TimeZone.getDefault()): String = if (unix <= 0) "" else hhmm(cal(unix * 1000L, zone))

    /** Date separator: «Сегодня», «Вчера», «12 ноября», «12 ноября 2025». */
    fun dayHeader(unix: Int, now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): String {
        val at = cal(unix * 1000L, zone)
        return when (daysBetween(unix * 1000L, now, zone)) {
            0 -> "Сегодня"
            1 -> "Вчера"
            else -> "${at.get(Calendar.DAY_OF_MONTH)} ${months[at.get(Calendar.MONTH)]}" +
                if (at.get(Calendar.YEAR) == cal(now, zone).get(Calendar.YEAR)) "" else " ${at.get(Calendar.YEAR)}"
        }
    }

    /** Same calendar day (for date separators). */
    fun sameDay(a: Int, b: Int, zone: TimeZone = TimeZone.getDefault()): Boolean = daysBetween(a * 1000L, b * 1000L, zone) == 0

    /** Russian plural: 1 участник, 2 участника, 5 участников, 11 участников, 21 участник. */
    fun plural(n: Int, one: String, few: String, many: String): String {
        val m10 = Math.abs(n) % 10; val m100 = Math.abs(n) % 100
        return when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }

    /** «1 284»: thousands grouped with a space, as in the mockups. */
    fun count(n: Int): String = n.toString().reversed().chunked(3).joinToString(" ").reversed()

    /** Channel views «950», «1,2K», «3,4M». */
    fun compactCount(n: Int): String {
        fun short(value: Double, unit: String) = "%.1f$unit".format(Locale.ROOT, value).replace(".0$unit", unit).replace('.', ',')
        return when {
            // 999 950 rounds to «1000K»: switch to millions at the rounded value.
            n >= 999_950 -> short(n / 1_000_000.0, "M")
            n >= 1_000 -> short(n / 1_000.0, "K")
            else -> n.toString()
        }
    }

    fun members(n: Int): String = "${count(n)} ${plural(n, "участник", "участника", "участников")}"
    fun subscribers(n: Int): String = "${count(n)} ${plural(n, "подписчик", "подписчика", "подписчиков")}"

    /** Voice / video length «0:24», «1:02:03». */
    fun duration(seconds: Int): String {
        val h = seconds / 3600; val m = seconds % 3600 / 60; val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(Locale.ROOT, h, m, s) else "%d:%02d".format(Locale.ROOT, m, s)
    }

    /** Call length in the call list: «42 сек», «12 мин», «1 ч 5 мин». */
    fun callDuration(seconds: Int): String {
        if (seconds <= 0) return ""
        if (seconds < 60) return "$seconds сек"
        // Rounded to the nearest minute first, so 59:40 reads «1 ч», never «60 мин».
        val minutes = (seconds + 30) / 60
        return if (minutes < 60) "$minutes мин" else "${minutes / 60} ч" + (minutes % 60).let { if (it > 0) " $it мин" else "" }
    }

    /** «2,4 МБ», «640 КБ». */
    fun fileSize(bytes: Long): String = when {
        bytes <= 0 -> ""
        bytes >= 1L shl 30 -> "%.1f ГБ".format(Locale.ROOT, bytes / (1L shl 30).toDouble()).replace('.', ',')
        bytes >= 1L shl 20 -> "%.1f МБ".format(Locale.ROOT, bytes / (1L shl 20).toDouble()).replace('.', ',')
        bytes >= 1024 -> "${bytes / 1024} КБ"
        else -> "$bytes Б"
    }

    /** Official Telegram wording: «в сети», «был(а) 5 минут назад», «был(а) в 21:14», «был(а) вчера в 21:14», «был(а) 12.11.25». */
    fun status(status: TelegramUserStatus?, now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): String = when (status) {
        null -> ""
        TelegramUserStatus.Online -> "в сети"
        TelegramUserStatus.Recently -> "был(а) недавно"
        TelegramUserStatus.LastWeek -> "был(а) на этой неделе"
        TelegramUserStatus.LastMonth -> "был(а) в этом месяце"
        TelegramUserStatus.LongAgo -> "был(а) давно"
        is TelegramUserStatus.Offline -> {
            val ms = status.wasOnline * 1000L
            val minutes = ((now - ms) / 60_000).toInt()
            val at = cal(ms, zone)
            when {
                minutes < 1 -> "был(а) только что"
                minutes < 60 -> "был(а) $minutes ${plural(minutes, "минуту", "минуты", "минут")} назад"
                daysBetween(ms, now, zone) == 0 -> "был(а) в ${hhmm(at)}"
                daysBetween(ms, now, zone) == 1 -> "был(а) вчера в ${hhmm(at)}"
                at.get(Calendar.YEAR) == cal(now, zone).get(Calendar.YEAR) -> "был(а) ${at.get(Calendar.DAY_OF_MONTH)} ${shortMonths[at.get(Calendar.MONTH)]} в ${hhmm(at)}"
                else -> "был(а) %02d.%02d.%02d".format(Locale.ROOT, at.get(Calendar.DAY_OF_MONTH), at.get(Calendar.MONTH) + 1, at.get(Calendar.YEAR) % 100)
            }
        }
    }

    /**
     * Chat list preview from telegram's [preview] (caption or a plain label such as «Фотография»),
     * the last message [kind] and its [duration]: «🎤 Голосовое сообщение · 0:24», «📷 Фото», «😀 Стикер».
     */
    fun preview(preview: String, kind: TelegramMessageKind, duration: Int, pollQuestion: String? = null): String {
        val text = preview.replace('\n', ' ').trim()
        fun labelled(emoji: String, default: String, label: String) = "$emoji " + if (text == default || text.isEmpty()) label else text
        val time = if (duration > 0) " · ${duration(duration)}" else ""
        return when (kind) {
            TelegramMessageKind.Voice -> if (text == "Голосовое сообщение" || text.isEmpty()) "🎤 Голосовое сообщение$time" else "🎤 $text"
            TelegramMessageKind.VideoNote -> "📹 Видеосообщение$time"
            TelegramMessageKind.Photo -> labelled("📷", "Фотография", "Фото")
            TelegramMessageKind.Video -> labelled("🎬", "Видео", "Видео")
            TelegramMessageKind.Animation -> if (text == "GIF" || text.isEmpty()) "GIF" else "GIF · $text"
            TelegramMessageKind.Audio -> labelled("🎵", "Аудио", "Аудио")
            TelegramMessageKind.Document -> labelled("📎", "Документ", "Файл")
            // telegram gives «Стикер 😀»; Telegram shows «😀 Стикер».
            TelegramMessageKind.Sticker -> text.removePrefix("Стикер").trim().let { if (it.isEmpty() || text == it) text.ifEmpty { "Стикер" } else "$it Стикер" }
            // A text message is never labelled, even when it reads «Опрос».
            TelegramMessageKind.Text -> text
            else -> when (text) {
                "Опрос" -> "📊 " + (pollQuestion?.takeIf { it.isNotBlank() } ?: "Опрос")
                "Геопозиция" -> "📍 Геопозиция"
                "Контакт" -> "👤 Контакт"
                "Звонок" -> "📞 Звонок"
                else -> text
            }
        }
    }

    private fun hhmm(c: Calendar) = "%02d:%02d".format(Locale.ROOT, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))

    private fun cal(ms: Long, zone: TimeZone) = Calendar.getInstance(zone).apply { timeInMillis = ms }

    private fun daysBetween(from: Long, to: Long, zone: TimeZone): Int {
        fun start(ms: Long) = cal(ms, zone).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        // Rounded: a day across a daylight-saving switch is 23 or 25 hours long.
        return Math.round((start(to) - start(from)) / 86_400_000.0).toInt()
    }
}
