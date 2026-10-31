package com.kirox.app.files

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * «Проверка файлов»: kiroX's own check of a file before it is opened. It has no virus database; it
 * looks for the tricks malicious files use — programs and installers, a disguised type (a program
 * named «photo.jpg»), double extensions, hidden right-to-left names, macros in documents, scripts in
 * PDF and HTML, dangerous APK permissions, archives with programs inside, zip bombs and path tricks.
 * The SHA-256 lets the user look the file up on VirusTotal themselves.
 */
object FileGuard {
    enum class Level { Safe, Caution, Danger }

    data class Report(val level: Level, val reasons: List<String>, val sha256: String, val kind: String)

    /** Programs and scripts that run when opened on a phone or a computer. */
    private val executable = setOf(
        "exe", "msi", "bat", "cmd", "com", "scr", "pif", "cpl", "vbs", "vbe", "js", "jse", "wsf", "wsh", "hta",
        "ps1", "psm1", "jar", "sh", "lnk", "reg", "dll", "sys", "elf", "bin", "run", "app", "command", "scpt", "gadget", "msc", "inf"
    )
    private val installers = setOf("apk", "xapk", "apks", "apkm", "aab", "dex", "ipa", "deb", "rpm", "dmg", "pkg")
    private val macroDocs = setOf("docm", "xlsm", "pptm", "dotm", "xltm", "potm", "xlsb", "xlam", "ppam")
    private val disguises = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "mp4", "mov", "avi", "mkv", "mp3", "m4a", "ogg", "wav", "flac",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "zip", "rar", "7z"
    )
    val archives = setOf("zip", "jar", "apk", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "docx", "xlsx", "pptx")
    private val dangerousPermissions = mapOf(
        "android.permission.SEND_SMS" to "отправка SMS (платные подписки)",
        "android.permission.RECEIVE_SMS" to "чтение входящих SMS (коды банков)",
        "android.permission.READ_SMS" to "чтение SMS",
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to "спецвозможности — полное управление экраном",
        "android.permission.BIND_DEVICE_ADMIN" to "права администратора устройства",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "установка других приложений",
        "android.permission.SYSTEM_ALERT_WINDOW" to "окна поверх других приложений",
        "android.permission.READ_CALL_LOG" to "журнал звонков",
        "android.permission.PROCESS_OUTGOING_CALLS" to "перехват звонков",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to "чтение всех уведомлений",
        "android.permission.QUERY_ALL_PACKAGES" to "список всех приложений",
        "android.permission.READ_CONTACTS" to "контакты",
        "android.permission.RECORD_AUDIO" to "микрофон",
        "android.permission.CAMERA" to "камера",
        "android.permission.ACCESS_FINE_LOCATION" to "точное местоположение",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "доступ ко всем файлам"
    )
    /** The worst of these mean the app can steal codes or take over the phone. */
    private val critical = setOf(
        "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS", "android.permission.READ_SMS",
        "android.permission.BIND_ACCESSIBILITY_SERVICE", "android.permission.BIND_DEVICE_ADMIN", "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    )

    /** Right-to-left and isolate controls that can flip how a name reads (U+202A–202E, U+2066–2068). */
    private val bidiControls = setOf(0x202A, 0x202B, 0x202D, 0x202E, 0x2066, 0x2067, 0x2068)

    fun extension(name: String) = name.substringAfterLast('.', "").lowercase()

    /** What the first bytes say the file really is; null when unknown. */
    fun sniff(file: File): String? = runCatching {
        val head = ByteArray(264)
        val n = file.inputStream().use { it.read(head) }
        if (n < 4) return@runCatching null
        fun at(offset: Int, vararg bytes: Int) = n >= offset + bytes.size && bytes.withIndex().all { (i, b) -> head[offset + i] == b.toByte() }
        when {
            at(0, 0x4D, 0x5A) -> "exe"
            at(0, 0x7F, 0x45, 0x4C, 0x46) -> "elf"
            at(0, 0x64, 0x65, 0x78, 0x0A) -> "dex"
            at(0, 0x50, 0x4B, 0x03, 0x04) || at(0, 0x50, 0x4B, 0x05, 0x06) -> "zip"
            at(0, 0x52, 0x61, 0x72, 0x21) -> "rar"
            at(0, 0x37, 0x7A, 0xBC, 0xAF) -> "7z"
            at(0, 0x1F, 0x8B) -> "gz"
            at(257, 0x75, 0x73, 0x74, 0x61, 0x72) -> "tar"
            at(0, 0x25, 0x50, 0x44, 0x46) -> "pdf"
            at(0, 0xFF, 0xD8, 0xFF) -> "jpg"
            at(0, 0x89, 0x50, 0x4E, 0x47) -> "png"
            at(0, 0x47, 0x49, 0x46, 0x38) -> "gif"
            at(0, 0x52, 0x49, 0x46, 0x46) -> "riff"
            at(4, 0x66, 0x74, 0x79, 0x70) -> "mp4"
            at(0, 0x49, 0x44, 0x33) || at(0, 0xFF, 0xFB) -> "mp3"
            at(0, 0x4F, 0x67, 0x67, 0x53) -> "ogg"
            at(0, 0xD0, 0xCF, 0x11, 0xE0) -> "ole"
            at(0, 0x23, 0x21) -> "script"
            at(0, 0xCA, 0xFE, 0xBA, 0xBE) || at(0, 0xCF, 0xFA, 0xED, 0xFE) -> "macho"
            else -> null
        }
    }.getOrNull()

    fun sha256(file: File): String = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val n = input.read(buffer); if (n <= 0) break; digest.update(buffer, 0, n) }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrDefault("")

    /** A slow check (reads the file): call it off the main thread. [name] is the name shown in the chat. */
    fun check(context: Context, file: File, name: String = file.name): Report {
        val reasons = mutableListOf<String>()
        var level = Level.Safe
        fun flag(l: Level, reason: String) { reasons += reason; if (l > level) level = l }
        val ext = extension(name)
        val real = sniff(file)
        val parts = name.lowercase().split('.')

        // Names built to fool: hidden right-to-left text, «фото.jpg.exe», «документ.pdf      .apk».
        if (name.any { it.code in bidiControls }) flag(Level.Danger, "В имени скрытые символы, которые переворачивают расширение — так маскируют программы")
        if (parts.size >= 3 && parts[parts.size - 2] in disguises && (ext in executable || ext in installers)) flag(Level.Danger, "Двойное расширение: файл притворяется «.${parts[parts.size - 2]}», а на деле «.$ext»")
        if (Regex("\\s{4,}\\.").containsMatchIn(name)) flag(Level.Caution, "В имени много пробелов перед расширением — так прячут настоящий тип")

        when {
            ext in executable -> flag(Level.Danger, "Программа или скрипт (.$ext): запускается при открытии на компьютере")
            ext in installers -> flag(Level.Caution, "Установщик приложения (.$ext): ставьте только из надёжных источников")
            ext in macroDocs -> flag(Level.Caution, "Документ с макросами (.$ext): макросы могут запускать код")
        }
        // The content does not match the name: a program dressed up as a picture or a document.
        val disguisedAs = ext in disguises || ext.isEmpty()
        when (real) {
            "exe", "elf", "macho", "dex" -> if (ext !in executable && ext !in installers) flag(Level.Danger, "Внутри программа, хотя файл назван «.$ext»")
            "script" -> if (disguisedAs) flag(Level.Danger, "Внутри скрипт, хотя файл назван «.$ext»")
            "zip" -> if (ext in setOf("jpg", "jpeg", "png", "gif", "mp4", "mp3", "pdf", "txt")) flag(Level.Caution, "Внутри архив, хотя файл назван «.$ext»")
        }

        if (real == "zip" || ext in setOf("zip", "apk", "jar", "docx", "xlsx", "pptx", "xapk", "apks")) checkZip(file, ext, ::flag)
        if (ext == "apk" || (real == "zip" && runCatching { ZipFile(file).use { it.getEntry("AndroidManifest.xml") != null } }.getOrDefault(false))) checkApk(context, file, ::flag)
        if (real == "pdf" || ext == "pdf") checkText(file, listOf("/JavaScript", "/JS ", "/Launch", "/EmbeddedFile"), "PDF с активным содержимым (скрипты, запуск программ или вложенные файлы)", ::flag)
        if (ext in setOf("html", "htm", "svg", "xhtml", "mht")) checkText(file, listOf("<script", "javascript:", "onload=", "<form", "password"), "Веб-страница со скриптами или формой ввода — так делают поддельные страницы входа", ::flag)
        if (real == "ole" && ext in setOf("doc", "xls", "ppt")) checkText(file, listOf("VBA", "Macros", "_VBA_PROJECT"), "Старый документ Office с макросами", ::flag)

        val kind = when {
            real == "zip" && ext == "apk" -> "APK"
            real != null -> real.uppercase()
            else -> ext.uppercase().ifEmpty { "?" }
        }
        return Report(level, reasons.distinct(), sha256(file), kind)
    }

    private fun checkZip(file: File, ext: String, flag: (Level, String) -> Unit) = runCatching {
        ZipFile(file).use { zip ->
            var total = 0L
            var programs = 0
            var nested = 0
            zip.entries().asSequence().take(20_000).forEach { entry ->
                val entryName = entry.name
                if (entryName.contains("../") || entryName.startsWith("/")) flag(Level.Danger, "Архив пишет файлы за пределы папки («$entryName»)")
                total += entry.size.coerceAtLeast(0)
                val e = extension(entryName)
                if (e in executable || (e in installers && ext !in setOf("xapk", "apks"))) programs++
                if (e in setOf("zip", "rar", "7z")) nested++
                if (entryName.endsWith("vbaProject.bin")) flag(Level.Caution, "В документе есть макросы")
            }
            if (programs > 0 && ext !in setOf("apk", "jar")) flag(Level.Danger, "В архиве программы или установщики: $programs")
            if (nested > 3) flag(Level.Caution, "Много архивов внутри архива — так прячут содержимое от проверки")
            val ratio = if (file.length() > 0) total / file.length() else 0
            if (total > 2L * 1024 * 1024 * 1024 || ratio > 200) flag(Level.Danger, "«Zip-бомба»: при распаковке займёт ${total / (1024 * 1024)} МБ — может забить память")
        }
    }.onFailure {
        // Password-protected or broken archives fail to open.
        flag(Level.Caution, "Архив не открывается для проверки (повреждён или защищён паролем)")
    }

    private fun checkApk(context: Context, file: File, flag: (Level, String) -> Unit) {
        val info = runCatching {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_PERMISSIONS)
        }.getOrNull() ?: return
        val requested = info.requestedPermissions.orEmpty().toSet()
        // Services that ask for accessibility or notification access declare it as their permission.
        val services = runCatching {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SERVICES)?.services.orEmpty().mapNotNull { it.permission }
        }.getOrDefault(emptyList())
        val all = requested + services
        val found = all.filter { it in dangerousPermissions }
        if (found.isEmpty()) return
        val worst = found.any { it in critical }
        flag(if (worst) Level.Danger else Level.Caution, "Приложение «${info.packageName}» просит: " + found.joinToString(", ") { dangerousPermissions.getValue(it) })
        if (worst && info.packageName.let { it.startsWith("com.android.") || it.startsWith("com.google.") || it.contains("telegram") })
            flag(Level.Danger, "Притворяется системным приложением или Telegram (${info.packageName})")
    }

    /** Looks for [markers] in the first 8 MB. */
    private fun checkText(file: File, markers: List<String>, reason: String, flag: (Level, String) -> Unit) = runCatching {
        val size = minOf(file.length(), 8L * 1024 * 1024).toInt()
        val bytes = ByteArray(size)
        RandomAccessFile(file, "r").use { it.readFully(bytes) }
        val text = String(bytes, Charsets.ISO_8859_1)
        if (markers.any { text.contains(it, ignoreCase = true) }) flag(Level.Caution, reason)
    }
}
