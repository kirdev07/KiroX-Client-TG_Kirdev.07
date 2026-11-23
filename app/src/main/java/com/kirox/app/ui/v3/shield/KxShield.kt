package com.kirox.app.ui.v3.shield

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kirox.app.security.shield.ShieldContext
import com.kirox.app.security.shield.ShieldModel
import com.kirox.app.security.shield.ShieldScorer
import com.kirox.app.security.shield.ShieldSeed
import com.kirox.app.security.shield.ShieldStrictness
import com.kirox.app.security.shield.ShieldText
import com.kirox.app.security.shield.ShieldVerdict
import java.io.File
import java.util.Calendar

/** Layers of «Защита kiroX» the hub turns on and off. */
enum class ShieldLayer(val key: String, val title: String, val sub: String) {
    Text("text", "Мошеннические сообщения", "Полоска над подозрительным сообщением от незнакомца"),
    Code("code", "Код-щит", "Остановит, если вы отправляете код входа или пароль"),
    Links("links", "Рентген ссылок", "Двойники сайтов, punycode, «@» в адресе, IP, короткие ссылки"),
    Apk("apk", "Рентген APK", "Пакет, подпись и опасные доступы перед установкой"),
}

/** What kiroX stopped: counted once per warning, per week. */
enum class ShieldEvent(val key: String, val label: String) { Text("text", "Подозрительных сообщений"), Code("code", "Попыток отправить код или пароль"), Link("link", "Опасных ссылок"), Apk("apk", "Установщиков APK") }

/** «Почему?» request shown by [KxShieldGate.Overlay]. */
data class ShieldWhy(val title: String, val verdict: ShieldVerdict, val chatId: Long = 0, val messageId: Long = 0, val text: String = "")

/**
 * Runtime of «Защита kiroX»: the learned model (seed from assets + this account's own marks), settings, trusted
 * chats and weekly counters. Everything lives in this app's private storage on the phone, per account; nothing
 * is sent anywhere. The demo store gets an in-memory account with demo counters.
 */
object KxShield {
    private const val DEMO = "demo"
    private const val MARK_WEIGHT = 4.0

    @Volatile private var app: Context? = null
    @Volatile private var seed: List<Pair<Set<String>, Boolean>> = emptyList()
    private val models = HashMap<String, ShieldModel>()
    private val lock = Any()

    /** Bumps on every change, so composables that read it recompose. */
    var version by mutableIntStateOf(0)
        private set
    /** Seed model trained (until then the rules work alone). */
    var ready by mutableStateOf(false)
        private set
    var why by mutableStateOf<ShieldWhy?>(null)

    private val demoPrefs = HashMap<String, Any>()
    private val counted = HashSet<String>()

    fun init(context: Context) {
        if (app != null) return
        synchronized(lock) {
            if (app != null) return
            app = context.applicationContext
        }
        Thread({
            val started = System.nanoTime()
            val samples = runCatching { context.applicationContext.assets.open("shield/seed.json").bufferedReader(Charsets.UTF_8).use { it.readText() } }
                .map { ShieldSeed.parse(it) }.getOrDefault(emptyList())
            seed = samples.map { ShieldText.features(it.text) to it.scam }
            synchronized(lock) { models.clear() }
            trainMs = (System.nanoTime() - started) / 1_000_000
            AppHandler.post { ready = true; version++ }
        }, "kirox-shield").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }.start()
    }

    var trainMs = 0L
        private set
    val seedSize get() = seed.size

    fun account(storeIsDemo: Boolean, meId: Long) = if (storeIsDemo) DEMO else meId.toString()

    // ---- model ----
    private fun model(acc: String): ShieldModel? {
        if (seed.isEmpty()) return null
        synchronized(lock) {
            models[acc]?.let { return it }
            val m = ShieldModel()
            seed.forEach { (f, s) -> m.learnFeatures(f, s) }
            readMarks(acc).forEach { (f, s) -> m.learnFeatures(f, s, MARK_WEIGHT) }
            models[acc] = m
            return m
        }
    }

    fun score(acc: String, text: String, ctx: ShieldContext): ShieldVerdict = ShieldScorer.score(text, model(acc), ctx)

    /** «Это мошенник» / «Это нормально»: the model learns this one message (only its word features are kept). */
    fun mark(acc: String, chatId: Long, text: String, scam: Boolean) {
        val f = ShieldText.features(text)
        synchronized(lock) { models[acc]?.learnFeatures(f, scam, MARK_WEIGHT) }
        if (acc != DEMO) appendMark(acc, f, scam)
        else demoPrefs["marks"] = ((demoPrefs["marks"] as? Int) ?: 0) + 1
        put(acc, if (scam) "marks.scam" else "marks.ok", getInt(acc, if (scam) "marks.scam" else "marks.ok") + 1)
        if (!scam) trust(acc, chatId, true)
        version++
    }

    /** Forget every own mark and trusted chat of this account; the model goes back to the seed. */
    fun resetLearning(acc: String) {
        synchronized(lock) { models.remove(acc) }
        if (acc == DEMO) demoPrefs.clear() else {
            marksFile(acc)?.delete()
            prefs()?.edit()?.remove("$acc.marks.scam")?.remove("$acc.marks.ok")?.remove("$acc.trusted")?.apply()
        }
        version++
    }

    fun marks(acc: String): Pair<Int, Int> = getInt(acc, "marks.scam") to getInt(acc, "marks.ok")

    // ---- settings ----
    fun on(acc: String, layer: ShieldLayer): Boolean = getInt(acc, "on.${layer.key}", 1) != 0
    fun setOn(acc: String, layer: ShieldLayer, on: Boolean) { put(acc, "on.${layer.key}", if (on) 1 else 0); version++ }
    fun strictness(acc: String) = ShieldStrictness.of(getInt(acc, "strict", ShieldStrictness.Normal.ordinal))
    fun setStrictness(acc: String, s: ShieldStrictness) { put(acc, "strict", s.ordinal); version++ }

    fun trusted(acc: String, chatId: Long): Boolean = chatId.toString() in getString(acc, "trusted").split(',')
    fun trust(acc: String, chatId: Long, on: Boolean) {
        val set = getString(acc, "trusted").split(',').filter { it.isNotBlank() }.toMutableSet()
        if (on) set += chatId.toString() else set -= chatId.toString()
        putString(acc, "trusted", set.joinToString(","))
        version++
    }
    fun trustedCount(acc: String) = getString(acc, "trusted").split(',').count { it.isNotBlank() }

    // ---- counters («Что kiroX остановил» за неделю) ----
    private fun week(): Int = Calendar.getInstance().let { it.firstDayOfWeek = Calendar.MONDAY; it.get(Calendar.YEAR) * 100 + it.get(Calendar.WEEK_OF_YEAR) }

    /** Counts [event] once per [key] (e.g. «chat:message»), for this week. */
    fun count(acc: String, event: ShieldEvent, key: String = System.nanoTime().toString()) {
        val k = "$acc|${week()}|${event.key}|$key"
        if (!counted.add(k)) return
        put(acc, "w${week()}.${event.key}", getInt(acc, "w${week()}.${event.key}") + 1)
        version++
    }

    fun weekCount(acc: String, event: ShieldEvent): Int {
        if (acc == DEMO && demoPrefs["w.${event.key}.touched"] == null) return when (event) { ShieldEvent.Text -> 4; ShieldEvent.Code -> 1; ShieldEvent.Link -> 3; ShieldEvent.Apk -> 1 }
        return getInt(acc, "w${week()}.${event.key}")
    }

    // ---- storage (private app prefs and one small file per account) ----
    private fun prefs() = app?.getSharedPreferences("kirox_shield", Context.MODE_PRIVATE)
    private fun getInt(acc: String, key: String, def: Int = 0): Int =
        if (acc == DEMO) (demoPrefs[key] as? Int) ?: def else prefs()?.getInt("$acc.$key", def) ?: def
    private fun put(acc: String, key: String, v: Int) {
        if (acc == DEMO) { demoPrefs[key] = v; if (key.startsWith("w")) demoPrefs["w.${key.substringAfter('.')}.touched"] = true; return }
        prefs()?.edit()?.putInt("$acc.$key", v)?.apply()
    }
    private fun getString(acc: String, key: String): String = if (acc == DEMO) (demoPrefs[key] as? String).orEmpty() else prefs()?.getString("$acc.$key", "").orEmpty()
    private fun putString(acc: String, key: String, v: String) { if (acc == DEMO) demoPrefs[key] = v else prefs()?.edit()?.putString("$acc.$key", v)?.apply() }

    private fun marksFile(acc: String): File? = app?.let { File(File(it.filesDir, "shield").apply { mkdirs() }, "marks_$acc.txt") }

    /** One line per mark: «1|feature feature …» (word features only, in no particular order — not the message itself). */
    private fun appendMark(acc: String, f: Set<String>, scam: Boolean) {
        val file = marksFile(acc) ?: return
        runCatching { file.appendText((if (scam) "1|" else "0|") + f.filter { !it.contains(' ') && !it.contains('|') }.joinToString(" ") + "\n") }
    }

    private fun readMarks(acc: String): List<Pair<Set<String>, Boolean>> {
        if (acc == DEMO) return emptyList()
        val file = marksFile(acc)?.takeIf { it.exists() } ?: return emptyList()
        return runCatching { file.readLines().takeLast(2000).mapNotNull { l -> val p = l.split('|', limit = 2); if (p.size == 2) p[1].split(' ').filter { it.isNotEmpty() }.toSet() to (p[0] == "1") else null } }.getOrDefault(emptyList())
    }
}

/** Main-thread poster without a View. */
internal object AppHandler {
    private val h = android.os.Handler(android.os.Looper.getMainLooper())
    fun post(r: () -> Unit) { h.post(r) }
}
