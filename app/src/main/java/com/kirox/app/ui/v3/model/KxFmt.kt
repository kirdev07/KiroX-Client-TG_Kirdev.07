package com.kirox.app.ui.v3.model

import java.util.Calendar
import java.util.Locale

/*
 * critique-C: ONE Russian number / size / date / length format for every stats, media and download screen.
 */

private fun oneDecimal(v: Double, unit: String): String =
    ("%.1f".format(Locale.ROOT, v).removeSuffix(".0").replace('.', ',')) + " " + unit

/** «950», «1 234», «12,3 тыс.», «1,2 млн». */
fun kxCount(n: Long): String = when {
    n < 0 -> "-" + kxCount(-n)
    n < 10_000 -> n.toString().reversed().chunked(3).joinToString(" ").reversed()
    n < 999_950 -> oneDecimal(n / 1_000.0, "тыс.")
    n < 999_950_000 -> oneDecimal(n / 1_000_000.0, "млн")
    else -> oneDecimal(n / 1_000_000_000.0, "млрд")
}

fun kxCount(n: Int): String = kxCount(n.toLong())

/** «0 Б», «640 КБ», «2,4 МБ», «1,1 ГБ» (comma decimal). */
fun kxBytes(bytes: Long): String = when {
    bytes < 1024 -> "${bytes.coerceAtLeast(0)} Б"
    bytes < 1L shl 20 -> "${bytes / 1024} КБ"
    bytes < 1L shl 30 -> oneDecimal(bytes / (1L shl 20).toDouble(), "МБ")
    else -> oneDecimal(bytes / (1L shl 30).toDouble(), "ГБ")
}

private val KxShortMonths = listOf("янв.", "февр.", "мар.", "апр.", "мая", "июн.", "июл.", "авг.", "сент.", "окт.", "нояб.", "дек.")

/** «3 нояб.» this year, «3 нояб. 2025» otherwise. */
fun kxShortDate(epochMs: Long, now: Long = System.currentTimeMillis()): String {
    val c = Calendar.getInstance().apply { timeInMillis = epochMs }
    val y = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
    val base = "${c.get(Calendar.DAY_OF_MONTH)} ${KxShortMonths[c.get(Calendar.MONTH)]}"
    return if (c.get(Calendar.YEAR) == y) base else "$base ${c.get(Calendar.YEAR)}"
}

/** «0:24», «12:05», «1:02:03». */
fun kxDuration(seconds: Int): String {
    val s0 = seconds.coerceAtLeast(0)
    val h = s0 / 3600; val m = s0 % 3600 / 60; val s = s0 % 60
    return if (h > 0) "%d:%02d:%02d".format(Locale.ROOT, h, m, s) else "%d:%02d".format(Locale.ROOT, m, s)
}
