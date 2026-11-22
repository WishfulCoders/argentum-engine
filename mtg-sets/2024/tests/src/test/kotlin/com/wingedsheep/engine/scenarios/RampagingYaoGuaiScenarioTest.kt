package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rampaging Yao Guai {X}{G}{G}{G} Creature — Bear Mutant 2/2 — Fallout #82.
 *
 * "Vigilance, trample. This creature enters with X +1/+1 counters on it. When this creature
 *  enters, destroy any number of target artifacts and/or enchantments with total mana value X or
 *  less."
 *
 * The cap is the X paid for the creature spell, carried from the cast onto the enters trigger's
 * target choice, and it bounds the *sum* of the chosen permanents' mana values — not each one.
 */
class RampagingYaoGuaiScenarioTest : ScenarioTestBase() {

    private fun game(lands: Int): TestGame = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Rampaging Yao Guai")
        .withLandsOnBattlefield(1, "Forest", lands)
        .withCardOnBattlefield(2, "Mind Stone")
        .withCardOnBattlefield(2, "Sol Ring")
        .withCardOnBattlefield(2, "Glorious Anthem")
        .withCardOnBattlefield(2, "Ornithopter")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Rampaging Yao Guai") {

            test("X=3 enters with three counters and destroys targets whose mana values sum to 3") {
                val game = game(lands = 6)

                game.castXSpell(1, "Rampaging Yao Guai", xValue = 3).error shouldBe null
                game.resolveStack()

                val yaoGuai = game.findPermanent("Rampaging Yao Guai").shouldNotBeNull()
                game.state.getEntity(yaoGuai)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3

                val mindStone = game.findPermanent("Mind Stone").shouldNotBeNull()
                val solRing = game.findPermanent("Sol Ring").shouldNotBeNull()
                val anthem = game.findPermanent("Glorious Anthem").shouldNotBeNull()
                val ornithopter = game.findPermanent("Ornithopter").shouldNotBeNull()

                withClue("2 + 3 = 5 exceeds X = 3, so that target set is rejected") {
                    game.selectTargets(listOf(mindStone, anthem)).error shouldNotBe null
                }

                game.selectTargets(listOf(mindStone, solRing, ornithopter)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Mind Stone") shouldBe false
                game.isOnBattlefield("Sol Ring") shouldBe false
                game.isOnBattlefield("Ornithopter") shouldBe false
                withClue("Glorious Anthem was not chosen and survives") {
                    game.isOnBattlefield("Glorious Anthem") shouldBe true
                }
            }

            test("X=0 caps the total at 0 — only a mana value 0 artifact can be chosen") {
                val game = game(lands = 3)

                game.castXSpell(1, "Rampaging Yao Guai", xValue = 0).error shouldBe null
                game.resolveStack()

                val solRing = game.findPermanent("Sol Ring").shouldNotBeNull()
                val ornithopter = game.findPermanent("Ornithopter").shouldNotBeNull()

                withClue("Sol Ring's mana value 1 exceeds X = 0") {
                    game.selectTargets(listOf(solRing)).error shouldNotBe null
                }
                game.selectTargets(listOf(ornithopter)).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Ornithopter") shouldBe false
                game.isOnBattlefield("Sol Ring") shouldBe true
            }
        }
    }
}
