package com.kirox.app.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.kirox.app.KiroXApplication
import org.json.JSONArray
import org.json.JSONObject

/**
 * «Резервная копия настроек»: every kiroX setting in one JSON file — appearance, kiroX options,
 * notifications, data and storage, language and name packs. Telegram data, the passcode and keys
 * are never included. Restoring replaces these settings and restarts kiroX so all screens read them.
 */
object SettingsBackup {
    /** SharedPreferences files that hold user settings; nothing else is read or written. */
    private val FILES = listOf(KiroxTweaks.PREFS, "auto_download", "storage_policy", "media_prefs", "notification_prefs", "i18n", CustomThemes.PREFS, MessageFilters.PREFS, Playlists.PREFS)

    suspend fun export(context: Context): String {
        val prefs = JSONObject()
        FILES.forEach { name ->
            val values = JSONObject()
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                val typed = when (value) {
                    is Boolean -> JSONObject().put("t", "b").put("v", value)
                    is Int -> JSONObject().put("t", "i").put("v", value)
                    is Long -> JSONObject().put("t", "l").put("v", value)
                    is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                    is String -> JSONObject().put("t", "s").put("v", value)
                    is Set<*> -> JSONObject().put("t", "set").put("v", JSONArray(value.map { it.toString() }))
                    else -> null
                }
                if (typed != null) values.put(key, typed)
            }
            prefs.put(name, values)
        }
        val appearance = JSONObject((context.applicationContext as KiroXApplication).settings.exportAll())
        return JSONObject()
            .put("kirox_backup", 1)
            .put("app_version", com.kirox.app.BuildConfig.VERSION_NAME)
            .put("appearance", appearance)
            .put("prefs", prefs)
            .toString(2)
    }

    /** Restores [text]; false when it is not a kiroX settings backup (nothing is changed then). */
    suspend fun restore(context: Context, text: String): Boolean {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (!json.has("kirox_backup")) return false
        val prefs = json.optJSONObject("prefs") ?: JSONObject()
        FILES.forEach { name ->
            val values = prefs.optJSONObject(name) ?: return@forEach
            // Written at once (commit), so the restart right after reads the restored values.
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) {
                clear()
                values.keys().forEach { key ->
                    val typed = values.optJSONObject(key) ?: return@forEach
                    when (typed.optString("t")) {
                        "b" -> putBoolean(key, typed.optBoolean("v"))
                        "i" -> putInt(key, typed.optInt("v"))
                        "l" -> putLong(key, typed.optLong("v"))
                        "f" -> putFloat(key, typed.optDouble("v").toFloat())
                        "s" -> putString(key, typed.optString("v"))
                        "set" -> typed.optJSONArray("v")?.let { array -> putStringSet(key, (0 until array.length()).map { array.getString(it) }.toSet()) }
                    }
                }
            }
        }
        json.optJSONObject("appearance")?.let { appearance ->
            val values = appearance.keys().asSequence().mapNotNull { key -> appearance.opt(key)?.let { key to it } }.toMap()
            (context.applicationContext as KiroXApplication).settings.importAll(values)
        }
        return true
    }

    /** Starts kiroX again, so every setting is read fresh. */
    fun restart(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        Runtime.getRuntime().exit(0)
    }
}
