package com.wingedsheep.gym.server.config

import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.mtg.sets.MtgSetCatalog

/**
 * Guards the gym's card registry against the registration drift that made self-play lie.
 *
 * `CreatePredefinedTokenExecutor` resolves a token by *name* out of the `CardRegistry` and returns
 * an `EffectResult.error` when the name is missing — no exception, no server-side log. The gym's
 * registry was built from set cards and basic lands only, so every Treasure / Food / Clue / Map /
 * Incubator card in the corpus minted nothing over the gym API while behaving correctly in
 * `game-server`, whose `GameBeansConfig` does register them. Self-play duly reported those cards as
 * broken cards rather than as a broken harness.
 */
class GymBeansConfigTest : FunSpec({

    test("the gym card registry resolves every predefined token by name") {
        val registry = GymBeansConfig().cardRegistry()

        for (token in PredefinedTokens.allTokens) {
            withClue("predefined token '${token.name}' must be registered for the gym") {
                registry.getCard(token.name) shouldNotBe null
            }
        }
    }

    test("an FRA sealed pool draws from the whole set, not the ~15 cards Scryfall flags booster:true") {
        BoosterCatalogue.available shouldBe true
        val config = GymBeansConfig()
        val registry = config.cardRegistry()
        val generator = config.boosterGenerator(registry)
        val fra = MtgSetCatalog.all.single { it.code == "FRA" }
        val ownNonBasic = fra.cards.filter { !it.typeLine.isBasicLand }.map { it.name }.toSet()
        // The raw flags are what broke it: most FRA cards are still booster:false upstream.
        val rawFlagged = fra.cards.count { !it.typeLine.isBasicLand && it.metadata.inBooster }
        withClue("if Scryfall has flagged FRA by now, this guard is moot but harmless") {
            (rawFlagged < ownNonBasic.size) shouldBe true
        }

        val eligible = generator.getSetConfig("FRA")!!.cards
            .filter { !it.typeLine.isBasicLand && it.metadata.inBooster }
            .map { it.name }.toSet()
        withClue("FRA booster-eligible cards (own: ${ownNonBasic.size})") {
            eligible.size shouldBeGreaterThanOrEqual 250
        }

        val opened = (1..60).flatMap { generator.generateSealedPool("FRA").map { it.name } }.toSet()
        println("FRA: ${ownNonBasic.size} own non-basic cards ($rawFlagged flagged booster:true), ${eligible.size} booster-eligible, " +
            "${opened.size} distinct names across 60 sealed pools")
        opened.size shouldBeGreaterThan 200
        generator.getBasicLands("FRA").keys shouldBe setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
    }

    test("SOS and ECL pools keep every card their own booster flags admitted, and add the reprints") {
        val config = GymBeansConfig()
        val generator = config.boosterGenerator(config.cardRegistry())
        for (code in listOf("SOS", "ECL")) {
            val set = MtgSetCatalog.all.single { it.code == code }
            val flaggedOwn = set.cards.filter { !it.typeLine.isBasicLand && it.metadata.inBooster }.map { it.name }.toSet()
            val eligible = generator.getSetConfig(code)!!.cards
                .filter { !it.typeLine.isBasicLand && it.metadata.inBooster }.map { it.name }.toSet()
            println("$code: ${flaggedOwn.size} own cards flagged booster:true, ${eligible.size} booster-eligible now, " +
                "${(eligible - flaggedOwn).size} added, ${(flaggedOwn - eligible).size} dropped ${flaggedOwn - eligible}")
            withClue("$code cards the old per-card flags admitted but the catalogue pool drops") {
                (flaggedOwn - eligible) shouldBe emptySet()
            }
        }
    }
})
