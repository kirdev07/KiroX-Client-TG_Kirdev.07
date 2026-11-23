package com.kirox.app.ui.v3.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.kirox.app.ui.v3.model.KxFeatureKeys
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 * features: translation on the phone for accounts without Telegram Premium (ML Kit Translation + Language ID).
 * The text never leaves the phone; only the language models (~30 MB each) are downloaded once from Google, after
 * the user agreed (KxFeatureChat shows the question; «только по Wi-Fi» by default). Premium keeps Telegram's translation.
 */

internal suspend fun <T> Task<T>.kxAwait(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
    addOnCanceledListener { c.cancel() }
}

/** Pure rules of the on-device translation (unit-tested). */
internal object KxTranslateRules {
    const val MARK = "переведено на телефоне"

    /** «ru» / «en» / … the phone can translate, or null. */
    fun mlLang(code: String): String? = TranslateLanguage.fromLanguageTag(code.substringBefore('-').lowercase())

    /** Translation with the on-device mark under it (the bubble shows it as is). */
    fun marked(text: String) = "$text\n· $MARK"

    /** Models still to download for [from] → [to] ([have] = downloaded codes). */
    fun missing(from: String, to: String, have: Set<String>): List<String> = listOf(from, to).distinct().filter { it !in have }
}

object KxOnDeviceTranslate {
    /** The question «скачать модель?» waiting for the user ([langs] = ISO codes). */
    class Ask(val langs: List<String>, val answer: CompletableDeferred<Boolean>)
    var ask by mutableStateOf<Ask?>(null)
        private set
    /** A model download is running (the chat shows a quiet line). */
    var downloading by mutableStateOf<List<String>>(emptyList())
        private set
    /** Chat layers on screen that can show [ask]; with none the answer is «скачайте в настройках». */
    @Volatile internal var hosts = 0

    fun answer(yes: Boolean) { val a = ask ?: return; ask = null; a.answer.complete(yes) }

    /** Store result: [text] translated (marked), or [error]; both null = nothing to show (the user said no). */
    data class Result(val text: String? = null, val error: String? = null)

    suspend fun detect(text: String): String? = runCatching {
        LanguageIdentification.getClient().identifyLanguage(text).kxAwait().takeIf { it != "und" }
    }.getOrNull()

    suspend fun downloaded(): Set<String> = runCatching {
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel::class.java).kxAwait().map { it.language }.toSet()
    }.getOrDefault(emptySet())

    suspend fun delete(lang: String): String? = runCatching {
        RemoteModelManager.getInstance().deleteDownloadedModel(TranslateRemoteModel.Builder(lang).build()).kxAwait(); null
    }.getOrElse { "Не удалось удалить модель" }

    /** Downloads the models of [langs]; null = ready. */
    suspend fun download(langs: List<String>, wifiOnly: Boolean): String? {
        if (langs.isEmpty()) return null
        downloading = langs
        try {
            val cond = DownloadConditions.Builder().apply { if (wifiOnly) requireWifi() }.build()
            val m = RemoteModelManager.getInstance()
            // Without Wi-Fi (Wi-Fi only) the download waits; give up after 15 minutes.
            val ok = withTimeoutOrNull(15 * 60_000L) { langs.forEach { m.download(TranslateRemoteModel.Builder(it).build(), cond).kxAwait() }; true }
            return if (ok == true) null else if (wifiOnly) "Модель скачается, когда телефон будет в Wi-Fi" else "Модель не скачалась"
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
            return "Модель не скачалась: проверьте интернет"
        } finally { downloading = emptyList() }
    }

    /**
     * Translates [text] into [toCode] on the phone. null = not for the phone (Premium, switched off, an unknown or
     * unsupported language): the caller asks Telegram as before.
     */
    suspend fun forStore(premium: Boolean, settings: Map<String, Int>, text: String, toCode: String): Result? {
        if (premium || settings[KxFeatureKeys.TR_ON_DEVICE] == 0 || text.isBlank()) return null
        val to = KxTranslateRules.mlLang(toCode) ?: return null
        val fromCode = detect(text) ?: return Result(error = "Не удалось определить язык сообщения")
        val from = KxTranslateRules.mlLang(fromCode) ?: return null
        if (from == to) return Result(error = "Сообщение уже на этом языке")
        val missing = KxTranslateRules.missing(from, to, downloaded())
        if (missing.isNotEmpty()) {
            if (hosts <= 0) return Result(error = "Скачайте модель перевода: Настройки → Функции kiroX → Перевод на телефоне")
            val yes = (ask?.takeIf { it.langs == missing } ?: Ask(missing, CompletableDeferred()).also { ask = it }).answer.await()
            if (!yes) return Result()
            download(missing, settings[KxFeatureKeys.TR_WIFI_ONLY] != 0)?.let { return Result(error = it) }
        }
        val tr = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(from).setTargetLanguage(to).build())
        return try {
            Result(text = KxTranslateRules.marked(tr.translate(text).kxAwait()))
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
            Result(error = "Не удалось перевести на телефоне")
        } finally { tr.close() }
    }

    /** Settings: download a model by hand (no question: the user pressed the button). */
    suspend fun prepare(code: String, wifiOnly: Boolean): String? {
        val lang = KxTranslateRules.mlLang(code) ?: return "Этот язык не переводится на телефоне"
        return download(KxTranslateRules.missing(lang, lang, downloaded()), wifiOnly)
    }
}
