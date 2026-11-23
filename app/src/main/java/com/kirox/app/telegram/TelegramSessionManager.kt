package com.kirox.app.telegram

import android.content.Context
import java.io.File

class TelegramSessionManager(context: Context, val name: String = "session-primary") {
    // Test-server databases never share a folder with real-Telegram ones (the testdc build is also its own app).
    private val root = File(context.filesDir, (if (com.kirox.app.BuildConfig.TELEGRAM_USE_TEST_DC) "tdlib-testdc/" else "tdlib/") + name)
    val databaseDirectory: String get() = File(root, "database").apply { mkdirs() }.absolutePath
    val filesDirectory: String get() = File(root, "files").apply { mkdirs() }.absolutePath
    val keyStore = TdlibKeyStore(context, name)

    val hasDatabase: Boolean get() = File(root, "database").list().orEmpty().isNotEmpty()

    /** Drops a session whose encryption key is lost; the user has to sign in again. */
    fun reset() {
        root.deleteRecursively()
        keyStore.delete()
    }
}
