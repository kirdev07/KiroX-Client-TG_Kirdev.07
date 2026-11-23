package com.kirox.app.security.shield

import kotlin.math.exp
import kotlin.math.ln

/** One labelled message of the seed set or a user mark (scam = true). */
data class ShieldSample(val text: String, val scam: Boolean, val heldOut: Boolean = false)

/**
 * The learned part of «Защита kiroX»: a multinomial Naive Bayes over [ShieldText.features] (Bernoulli-style: each
 * feature counts once per message). Trained on the phone from the seed set in assets at first use (a few ms) and
 * updated one message at a time from the user's own marks «Это мошенник» / «Это нормально». Everything stays in
 * memory and in a small file on this phone ([ShieldMarks]); nothing is uploaded.
 *
 * Not thread-safe by itself: [KxShieldEngine] guards it.
 */
class ShieldModel(private val alpha: Double = 0.3) {
    // counts[0] = normal, counts[1] = scam
    private val counts = arrayOf(HashMap<String, Double>(4096), HashMap<String, Double>(4096))
    private val totals = doubleArrayOf(0.0, 0.0)
    private val docs = doubleArrayOf(0.0, 0.0)
    private val vocab = HashSet<String>(6000)

    val vocabularySize get() = vocab.size
    val documents get() = docs[0] + docs[1]

    /** Adds one message with [weight] (a user mark weighs more than a seed line). */
    fun learn(text: String, scam: Boolean, weight: Double = 1.0) = learnFeatures(ShieldText.features(text), scam, weight)

    fun learnFeatures(features: Collection<String>, scam: Boolean, weight: Double = 1.0) {
        val c = if (scam) 1 else 0
        docs[c] += weight
        for (f in features) {
            counts[c][f] = (counts[c][f] ?: 0.0) + weight
            totals[c] += weight
            vocab += f
        }
    }

    fun train(samples: Iterable<ShieldSample>) = samples.forEach { learn(it.text, it.scam) }

    /**
     * Log-likelihood ratio log P(scam | text) − log P(normal | text). Unknown words are skipped (they say nothing
     * about either class), so a message full of new words stays near the prior.
     */
    fun logOdds(text: String): Double = logOddsFeatures(ShieldText.features(text))

    fun logOddsFeatures(features: Collection<String>): Double {
        if (docs[0] <= 0 || docs[1] <= 0) return 0.0
        val v = vocab.size.toDouble().coerceAtLeast(1.0)
        var z = ln(docs[1] / docs[0])
        val d1 = ln(totals[1] + alpha * v)
        val d0 = ln(totals[0] + alpha * v)
        for (f in features) {
            if (f !in vocab) continue
            z += ln((counts[1][f] ?: 0.0) + alpha) - d1
            z -= ln((counts[0][f] ?: 0.0) + alpha) - d0
        }
        return z
    }

    /** The words that pushed [text] most towards «scam» (for «Почему?»), strongest first. */
    fun topScamFeatures(text: String, n: Int = 3): List<String> {
        if (docs[0] <= 0 || docs[1] <= 0) return emptyList()
        val v = vocab.size.toDouble()
        val d1 = ln(totals[1] + alpha * v); val d0 = ln(totals[0] + alpha * v)
        return ShieldText.features(text).asSequence()
            .filter { it in vocab && !it.startsWith("!") && !it.startsWith("~") && !it.startsWith("#") && !it.startsWith("%") && !it.contains('_') }
            .map { f -> f to ((ln((counts[1][f] ?: 0.0) + alpha) - d1) - (ln((counts[0][f] ?: 0.0) + alpha) - d0)) }
            .filter { it.second > 1.0 }
            .sortedByDescending { it.second }.take(n).map { it.first }.toList()
    }

    companion object {
        /** The model's own 0..1 (temperature-softened: NB is over-confident on short texts). */
        fun probability(logOdds: Double, temperature: Double = 4.0): Double = 1.0 / (1.0 + exp(-logOdds / temperature))
    }
}

/**
 * Seed set reader: a JSON array of {"t": text, "s": 1|0, "h": 1 (held out for the accuracy test, optional)}.
 * A tiny parser on purpose: org.json is a stub in JVM unit tests, and the file has this one shape.
 */
object ShieldSeed {
    fun parse(json: String): List<ShieldSample> {
        val out = ArrayList<ShieldSample>(600)
        var i = 0
        val n = json.length
        fun skipWs() { while (i < n && json[i].isWhitespace()) i++ }
        fun str(): String {
            val sb = StringBuilder()
            i++ // opening quote
            while (i < n) {
                val c = json[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val e = json[i++]
                        when (e) {
                            'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r'); 'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                            'u' -> { sb.append(json.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }
        fun num(): Int { val s = i; while (i < n && (json[i].isDigit() || json[i] == '-')) i++; return json.substring(s, i).toIntOrNull() ?: 0 }
        while (i < n) {
            if (json[i] != '{') { i++; continue }
            i++
            var text = ""; var scam = false; var held = false
            while (i < n) {
                skipWs()
                if (i < n && json[i] == '}') { i++; break }
                if (i < n && json[i] == ',') { i++; continue }
                if (i >= n || json[i] != '"') { i++; continue }
                val key = str(); skipWs(); if (i < n && json[i] == ':') i++; skipWs()
                when {
                    i < n && json[i] == '"' -> { val v = str(); if (key == "t") text = v }
                    else -> { val v = num(); if (key == "s") scam = v == 1; if (key == "h") held = v == 1 }
                }
            }
            if (text.isNotBlank()) out += ShieldSample(text, scam, held)
        }
        return out
    }
}
