package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Awakened Awareness (NEO #47) — {X}{U}{U} Enchantment — Aura
 *
 *   Enchant artifact or creature
 *   When this Aura enters, put X +1/+1 counters on enchanted permanent.
 *   As long as enchanted permanent is a creature, it has base power and toughness 1/1.
 *
 * Proves the cast-time X survives onto the Aura permanent for its enters trigger, that the
 * counters land on the enchanted permanent (not the Aura), and that the 1/1 base only applies to
 * a creature host.
 */
class AwakenedAwarenessScenarioTest : ScenarioTestBase() {
    init {
        fun plusOnes(game: TestGame, name: String) = game.state.getEntity(game.findPermanent(name)!!)!!
            .get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        test("X = 2 on a 3/3 creature: base becomes 1/1, two counters make it 3/3") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Awakened Awareness")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            val r = game.castXSpell(1, "Awakened Awareness", 2, giant)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            withClue("counters go on the enchanted creature") { plusOnes(game, "Hill Giant") shouldBe 2 }
            plusOnes(game, "Awakened Awareness") shouldBe 0
            game.state.projectedState.getPower(giant) shouldBe 3
            game.state.projectedState.getToughness(giant) shouldBe 3
        }

        test("X = 0 just makes the creature a 1/1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Awakened Awareness")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            val r = game.castXSpell(1, "Awakened Awareness", 0, giant)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            plusOnes(game, "Hill Giant") shouldBe 0
            game.state.projectedState.getPower(giant) shouldBe 1
            game.state.projectedState.getToughness(giant) shouldBe 1
        }

        test("on a noncreature artifact: counters land, but it stays a noncreature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Awakened Awareness")
                .withCardOnBattlefield(1, "Mind Stone")
                .withLandsOnBattlefield(1, "Island", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val stone = game.findPermanent("Mind Stone")!!

            val r = game.castXSpell(1, "Awakened Awareness", 3, stone)
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            plusOnes(game, "Mind Stone") shouldBe 3
            game.state.projectedState.isCreature(stone) shouldBe false
        }
    }
}
