package com.kirox.app.security.shield

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

/** One requested permission in human words; [critical] = can steal codes or take over the phone. */
data class ShieldApkPermission(val name: String, val label: String, val critical: Boolean)

/** «Рентген APK»: what an installer file really is, read on the phone before it goes to the system installer. */
data class ShieldApkInfo(
    val packageName: String,
    val label: String,
    val version: String,
    /** SHA-256 of the signing certificate («AB:CD:…»), empty when unsigned / unreadable. */
    val signer: String,
    val signerName: String,
    val debugSigned: Boolean,
    val permissions: List<ShieldApkPermission>,
    /** 0 nothing special, 1 осторожно, 2 опасно. */
    val level: Int,
    val notes: List<String>,
)

object ShieldApk {
    private val PERMS: Map<String, Pair<String, Boolean>> = mapOf(
        "android.permission.RECEIVE_SMS" to ("читать входящие SMS (коды банков и Telegram)" to true),
        "android.permission.READ_SMS" to ("читать все SMS" to true),
        "android.permission.SEND_SMS" to ("отправлять SMS (платные подписки)" to true),
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to ("спецвозможности: видеть экран и нажимать за вас" to true),
        "android.permission.BIND_DEVICE_ADMIN" to ("права администратора: его будет трудно удалить" to true),
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to ("читать все уведомления (там приходят коды)" to true),
        "android.permission.SYSTEM_ALERT_WINDOW" to ("рисовать окна поверх других приложений" to true),
        "android.permission.REQUEST_INSTALL_PACKAGES" to ("ставить другие приложения" to false),
        "android.permission.READ_CALL_LOG" to ("журнал звонков" to false),
        "android.permission.CALL_PHONE" to ("звонить без вашего участия" to false),
        "android.permission.PROCESS_OUTGOING_CALLS" to ("перехватывать звонки" to true),
        "android.permission.ANSWER_PHONE_CALLS" to ("отвечать на звонки" to false),
        "android.permission.READ_CONTACTS" to ("контакты" to false),
        "android.permission.RECORD_AUDIO" to ("микрофон" to false),
        "android.permission.CAMERA" to ("камера" to false),
        "android.permission.ACCESS_FINE_LOCATION" to ("точное местоположение" to false),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to ("местоположение в фоне" to false),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to ("все файлы на телефоне" to false),
        "android.permission.QUERY_ALL_PACKAGES" to ("список всех ваших приложений (ищет банки)" to false),
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" to ("работать в фоне без ограничений" to false),
        "android.permission.RECEIVE_BOOT_COMPLETED" to ("запускаться при включении телефона" to false),
        "android.permission.READ_PHONE_STATE" to ("номер и данные SIM" to false),
    )

    /** Official package names of apps scammers imitate; a «Kaspi» with another package is a fake. */
    private val OFFICIAL: Map<String, Set<String>> = mapOf(
        "telegram" to setOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram", "com.kirox.app"),
        "kaspi" to setOf("kz.kaspi.mobile"),
        "halyk" to setOf("kz.halyk.onlinebank", "kz.homebank.mobile"),
        "сбер" to setOf("ru.sberbankmobile"), "sber" to setOf("ru.sberbankmobile"),
        "тинькофф" to setOf("com.idamob.tinkoff.android"), "tinkoff" to setOf("com.idamob.tinkoff.android"),
        "whatsapp" to setOf("com.whatsapp", "com.whatsapp.w4b"),
        "госуслуги" to setOf("ru.rostel"), "egov" to setOf("kz.nitec.egov.mgov"),
        "jusan" to setOf("kz.tsb.app24.mobile"), "google" to emptySet(),
    )

    /** Reads the APK at [file] (call off the main thread). Null when Android cannot parse it. */
    fun read(context: Context, file: File): ShieldApkInfo? = runCatching {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or
            (if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
        val info = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: return@runCatching null
        info.applicationInfo?.let { it.sourceDir = file.absolutePath; it.publicSourceDir = file.absolutePath }
        val label = runCatching { info.applicationInfo?.loadLabel(pm)?.toString() }.getOrNull().orEmpty().ifBlank { info.packageName }
        @Suppress("DEPRECATION")
        val certs: List<ByteArray> = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.let { s -> (if (s.hasMultipleSigners()) s.apkContentsSigners else s.signingCertificateHistory)?.map { it.toByteArray() } }.orEmpty()
            else info.signatures?.map { it.toByteArray() }.orEmpty()
        val cert = certs.firstOrNull()
        val (signer, signerName) = if (cert == null) "" to "" else fingerprint(cert) to subject(cert)
        val requested = info.requestedPermissions.orEmpty().toSet() + info.services.orEmpty().mapNotNull { it.permission }
        @Suppress("DEPRECATION") val version = info.versionName.orEmpty()
        assess(info.packageName, label, version, signer, signerName, requested)
    }.getOrNull()

    /** Pure part (tested): verdict from what was read. */
    fun assess(pkg: String, label: String, version: String, signer: String, signerName: String, requested: Set<String>): ShieldApkInfo {
        val perms = requested.mapNotNull { p -> PERMS[p]?.let { ShieldApkPermission(p, it.first, it.second) } }.sortedByDescending { it.critical }
        val notes = ArrayList<String>()
        var level = 1 // any installer from a chat deserves a look
        val debug = signerName.contains("Android Debug", ignoreCase = true)
        if (signer.isEmpty()) { notes += "Файл не подписан или подпись не читается — так выглядят подделки"; level = 2 }
        if (debug) { notes += "Подписан отладочным ключом «Android Debug»: это самодельная сборка, а не приложение из магазина"; level = 2 }
        val critical = perms.filter { it.critical }
        if (critical.size >= 2) { notes += "Просит сразу несколько опасных доступов — так устроены банковские трояны"; level = 2 }
        else if (critical.size == 1) notes += "Просит опасный доступ: ${critical[0].label}"
        val hay = (label + " " + pkg).lowercase()
        OFFICIAL.forEach { (brand, official) ->
            if (official.isNotEmpty() && hay.contains(brand) && pkg !in official) { notes += "Называется как «${brand.replaceFirstChar { it.uppercase() }}», но это не официальное приложение ($pkg)"; level = 2 }
        }
        if (pkg.startsWith("com.android.") || pkg.startsWith("com.google.android.") || pkg.startsWith("android.")) { notes += "Притворяется системным приложением ($pkg)"; level = 2 }
        if (notes.isEmpty()) notes += "Явных признаков опасности не найдено — это не гарантия. Ставьте приложения из магазина."
        return ShieldApkInfo(pkg, label, version, signer, signerName, debug, perms, level, notes)
    }

    private fun fingerprint(cert: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(cert).joinToString(":") { "%02X".format(it) }

    private fun subject(cert: ByteArray): String = runCatching {
        val x = java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(cert.inputStream()) as java.security.cert.X509Certificate
        x.subjectX500Principal.name
    }.getOrDefault("")
}
