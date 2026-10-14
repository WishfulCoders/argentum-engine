package com.wingedsheep.gym.exit

import com.wingedsheep.gym.contract.TrainingObservation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * mtg-draft-ai docs/65 §3 on the recorded artifacts, which live outside the repo (`~/mtg/exit_e1`): each test runs
 * only when its system properties name them (`-De1.model`, `-De1.parity`, `-De1.deals`; B's profile also needs
 * `-Dargentum.ai.apprentice.dir`).
 */
class E1Test : FunSpec({
    val model = System.getProperty("e1.model")?.let(::File)
    val fixtures = System.getProperty("e1.parity")?.let(::File)
    val deals = System.getProperty("e1.deals")?.let(::File)

    test("§3.1a: features exact, logits within the float32 floor, discrete differences only at near-ties").config(
        enabled = model != null && fixtures != null,
    ) {
        PolicyNet(model!!).use { net ->
            val files = fixtures!!.listFiles { f -> f.name.endsWith(".jsonl.gz") }!!.sorted()
            val report = Parity(GameplayPolicy(net)).run(files, 2_000)
            val counts = report.getValue("counts").jsonObject
            val worst = report.getValue("max_abs_diff").jsonObject
            fun count(k: String) = counts[k]?.jsonPrimitive?.int ?: 0
            fun worst(k: String) = worst[k]?.jsonPrimitive?.double ?: 0.0
            count("features_equal") shouldBe count("rows")
            worst("action_logit") shouldBeLessThanOrEqual 2e-4
            worst("value") shouldBeLessThanOrEqual 1e-5
            worst("argmax_swap_python_gap") shouldBeLessThanOrEqual 1e-5
            worst("top_swap_python_gap") shouldBeLessThanOrEqual 1e-5
        }
    }

    test("batched halving rounds make the same search as back-to-back simulations").config(
        enabled = model != null && deals != null,
    ) {
        val engine = Engine()
        val (_, config) = Gate.readDeals(deals!!).first()
        PolicyNet(model!!).use { net ->
            val policy = GameplayPolicy(net)
            fun run(batch: Boolean): List<String> {
                val searcher = Searcher(policy, { policy.encode(it) }, SearchConfig(sims = 32, batchRounds = batch))
                val (env, opening) = engine.create(config)
                var observation = opening.observation as TrainingObservation
                val out = ArrayList<String>()
                for (decision in 0 until 40) {
                    val status = env.status()
                    if (status.terminated || status.truncated) break
                    val (choice, stats) = searcher.decide(env, observation, Searcher.decisionSeed(64, 0, decision.toLong()))
                    stats.target?.let { t -> out += "$decision ${t.chosen} " + t.candidates.joinToString { "${it.choice.key}:${it.visits}" } }
                    observation = env.step(choice.actionId, choice.params.toActionParams()).observation as TrainingObservation
                }
                return out
            }
            val batched = run(true)
            val sequential = run(false)
            (batched.size > 5) shouldBe true
            batched shouldBe sequential
        }
    }
})
