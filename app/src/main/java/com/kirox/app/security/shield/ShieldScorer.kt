package com.kirox.app.security.shield

import kotlin.math.exp

/** How eager the warnings are (hub «Строгость»). [threshold] is the risk from which a strip shows. */
enum class ShieldStrictness(val label: String, val threshold: Double) {
    Soft("Мягко", 0.80), Normal("Обычно", 0.62), Strict("Строго", 0.45);
    companion object { fun of(i: Int) = entries.getOrElse(i) { Normal } }
}

/** Who wrote the message, as far as the chat knows: raises or lowers the score. */
data class ShieldContext(
    /** Private chat with someone who is not in the contacts (or a new chat). */
    val stranger: Boolean = true,
    /** Telegram itself marked the account as scam / fake. */
    val telegramScam: Boolean = false,
    val telegramFake: Boolean = false,
    /** A verified account (blue tick): much less likely. */
    val verified: Boolean = false,
    /** Group message: only strong cases warn. */
    val group: Boolean = false,
)

/** A scored message: [risk] 0..1, [reasons] short lines for the strip, [explain] for «Почему?». */
data class ShieldVerdict(
    val risk: Double,
    val reasons: List<String>,
    val explain: List<Pair<String, String>>,
    val modelRisk: Double,
    val signals: List<ShieldSignal>,
) {
    /** The strip line: «Похоже на мошенничество: просят код входа». */
    val headline: String get() = "Похоже на мошенничество" + (reasons.firstOrNull()?.let { ": $it" } ?: "")
    companion object { val NONE = ShieldVerdict(0.0, emptyList(), emptyList(), 0.0, emptyList()) }
}

/**
 * Rules + learned model → one calibrated risk. logit = bias + Σ rule weights (diminishing after the strongest two)
 * + model term + context. The model term is the NB log-odds squashed to ±[MODEL_CAP], so the model alone can move
 * a borderline text but cannot shout over the rules, and the rules alone explain every strong warning.
 * Calibration (bias, scale) was tuned on the seed set: a normal message sits near 0.05, one clear signal ≈ 0.5–0.65,
 * a signal confirmed by the model ≈ 0.8+, two signals ≈ 0.9+.
 */
object ShieldScorer {
    private const val BIAS = -3.0
    private const val MODEL_SCALE = 0.25
    private const val MODEL_CAP = 3.2

    fun score(text: String, model: ShieldModel?, ctx: ShieldContext = ShieldContext()): ShieldVerdict {
        if (text.isBlank() || text.length < 6) return ShieldVerdict.NONE
        val sample = if (text.length > 1200) text.take(1200) else text
        val signals = ShieldRules.signals(sample)
        val llr = model?.logOdds(sample) ?: 0.0
        var z = BIAS
        signals.forEachIndexed { i, s -> z += s.weight * (if (i < 2) 1.0 else 0.5) }
        val m = (llr * MODEL_SCALE).coerceIn(-MODEL_CAP, MODEL_CAP)
        z += m
        if (ctx.telegramScam) z += 3.5
        if (ctx.telegramFake) z += 2.5
        if (ctx.verified) z -= 2.0
        if (!ctx.stranger) z -= 0.6
        if (ctx.group) z -= 0.4
        val risk = sigmoid(z)
        val reasons = ArrayList<String>()
        val explain = ArrayList<Pair<String, String>>()
        if (ctx.telegramScam) { reasons += "Telegram пометил аккаунт как мошеннический"; explain += "Метка Telegram" to "Сам Telegram получил жалобы на этот аккаунт и пометил его как мошеннический." }
        if (ctx.telegramFake) { reasons += "Telegram пометил аккаунт как поддельный"; explain += "Метка Telegram" to "Telegram считает, что аккаунт выдаёт себя за другого человека или организацию." }
        signals.forEach { reasons += it.reason; explain += it.reason.replaceFirstChar { c -> c.uppercase() } to it.explain }
        if (m > 0.8) {
            val words = model?.topScamFeatures(sample).orEmpty()
            if (signals.isEmpty()) reasons += "похоже на известные мошеннические сообщения"
            explain += "Обученная модель kiroX" to ("Текст похож на мошеннические сообщения, на которых kiroX учился" +
                (if (words.isNotEmpty()) " (слова: " + words.joinToString(", ") { "«$it»" } + ")" else "") + ". Модель работает только на телефоне и учится на ваших отметках.")
        }
        if (ctx.stranger && !ctx.group && risk > 0.3) explain += "Незнакомый собеседник" to "Этого человека нет в ваших контактах. Мошенники почти всегда пишут первыми с новых аккаунтов."
        return ShieldVerdict(risk, reasons.distinct(), explain, ShieldModel.probability(llr), signals)
    }

    fun sigmoid(z: Double) = 1.0 / (1.0 + exp(-z))
}
