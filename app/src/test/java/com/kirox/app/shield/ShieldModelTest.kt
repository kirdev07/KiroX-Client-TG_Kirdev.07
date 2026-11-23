package com.kirox.app.shield

import com.kirox.app.security.shield.ShieldContext
import com.kirox.app.security.shield.ShieldModel
import com.kirox.app.security.shield.ShieldSample
import com.kirox.app.security.shield.ShieldScorer
import com.kirox.app.security.shield.ShieldSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** «Защита kiroX»: the learned model on the seed set (held-out split marked "h":1 in assets/shield/seed.json). */
class ShieldModelTest {
    private val seedFile = listOf(File("src/main/assets/shield/seed.json"), File("app/src/main/assets/shield/seed.json")).first { it.exists() }
    private val seed: List<ShieldSample> by lazy { ShieldSeed.parse(seedFile.readText(Charsets.UTF_8)) }
    private val train get() = seed.filter { !it.heldOut }
    private val test get() = seed.filter { it.heldOut }

    private fun trained() = ShieldModel().apply { train(this@ShieldModelTest.train) }

    @Test fun seedIsBalancedSmallAndParsed() {
        assertTrue("seed has ${seed.size}", seed.size in 300..700)
        assertTrue(seed.count { it.scam } > 120 && seed.count { !it.scam } > 120)
        assertTrue("held out ${test.size}", test.size >= 60)
        assertTrue("seed.json is ${seedFile.length()} bytes", seedFile.length() < 1_000_000)
    }

    @Test fun modelAloneIsAtLeast90PercentOnHeldOut() {
        val m = trained()
        val right = test.count { (m.logOdds(it.text) > 0) == it.scam }
        val acc = right.toDouble() / test.size
        println("SHIELD model-only held-out accuracy: $right/${test.size} = ${"%.3f".format(acc)}; vocabulary ${m.vocabularySize}")
        assertTrue("model accuracy $acc", acc >= 0.90)
    }

    @Test fun rulesPlusModelIsAtLeast90PercentOnHeldOut() {
        val m = trained()
        var tp = 0; var tn = 0; var fp = 0; var fn = 0
        for (s in test) {
            val r = ShieldScorer.score(s.text, m, ShieldContext(stranger = true)).risk >= 0.5
            when { r && s.scam -> tp++; !r && !s.scam -> tn++; r -> fp++; else -> fn++ }
        }
        val acc = (tp + tn).toDouble() / test.size
        println("SHIELD combined held-out: acc=${"%.3f".format(acc)} tp=$tp tn=$tn fp=$fp fn=$fn precision=${"%.3f".format(tp.toDouble() / (tp + fp).coerceAtLeast(1))} recall=${"%.3f".format(tp.toDouble() / (tp + fn).coerceAtLeast(1))}")
        assertTrue("combined accuracy $acc", acc >= 0.90)
        assertTrue("false positives $fp", fp <= test.count { !it.scam } / 10)
    }

    @Test fun rulesAloneAccuracyIsReported() {
        var right = 0
        for (s in test) if ((ShieldScorer.score(s.text, null, ShieldContext(stranger = true)).risk >= 0.5) == s.scam) right++
        println("SHIELD rules-only held-out accuracy: $right/${test.size} = ${"%.3f".format(right.toDouble() / test.size)}")
    }

    @Test fun learnsIncrementallyFromOwnMarks() {
        val m = trained()
        // A new kind of scam the seed never saw: «бабушкин пенсионный фонд» + «секретный бонус».
        val novel = "Пенсионный фонд дарит секретный бонус бабушкам, напишите фамилию и номер СНИЛС в ответ"
        val before = m.logOdds(novel)
        m.learn("Пенсионный фонд дарит секретный бонус, напишите номер СНИЛС", scam = true, weight = 4.0)
        val after = m.logOdds(novel)
        println("SHIELD incremental: log-odds $before → $after")
        assertTrue("after a scam mark the score rises ($before → $after)", after > before + 2.0)
        // «Это нормально» works the other way for a sender's usual words.
        val normal = "Скидки на фрукты в Магнуме до воскресенья, бери апельсины"
        val b2 = m.logOdds(normal)
        m.learn("Скидки на фрукты в Магнуме до воскресенья", scam = false, weight = 4.0)
        assertTrue(m.logOdds(normal) < b2)
        // Reset = a fresh model from the seed: back to the start.
        assertEquals(before, trained().logOdds(novel), 1e-9)
    }

    @Test fun scoringIsUnder5msPerMessage() {
        val m = trained()
        val texts = seed.map { it.text }
        repeat(2) { texts.forEach { ShieldScorer.score(it, m) } } // warm-up
        val t0 = System.nanoTime()
        texts.forEach { ShieldScorer.score(it, m) }
        val perMs = (System.nanoTime() - t0) / 1e6 / texts.size
        println("SHIELD scoring: ${"%.3f".format(perMs)} ms per message")
        assertTrue("$perMs ms per message", perMs < 5.0)
        val t1 = System.nanoTime(); ShieldModel().train(seed); println("SHIELD training on ${seed.size}: ${(System.nanoTime() - t1) / 1_000_000} ms")
    }

    @Test fun telegramFlagsRaiseAndVerifiedLowers() {
        val m = trained()
        val t = "Здравствуйте, напишите мне в личку по поводу вашего заказа"
        val base = ShieldScorer.score(t, m).risk
        assertTrue(ShieldScorer.score(t, m, ShieldContext(telegramScam = true)).risk > base + 0.2)
        assertTrue(ShieldScorer.score(t, m, ShieldContext(verified = true)).risk < base)
    }

    @Test fun verdictExplainsTopReasons() {
        val v = ShieldScorer.score("Служба поддержки Telegram: срочно пришлите код из смс, иначе аккаунт будет удален", trained())
        assertTrue(v.risk > 0.9)
        assertEquals("просят код входа", v.reasons.first())
        assertTrue(v.headline.startsWith("Похоже на мошенничество: "))
        assertTrue(v.explain.size >= 2)
    }

    @Test fun parserHandlesEscapes() {
        val s = ShieldSeed.parse("""[{"t": "a \"b\" с", "s": 1, "h": 1}, {"s":0,"t":"x\ny"}]""")
        assertEquals(2, s.size)
        assertEquals("a \"b\" с", s[0].text); assertTrue(s[0].scam && s[0].heldOut)
        assertEquals("x\ny", s[1].text)
    }
}
