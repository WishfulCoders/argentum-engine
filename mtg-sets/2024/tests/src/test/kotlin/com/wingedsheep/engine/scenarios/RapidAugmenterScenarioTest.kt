package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rapid Augmenter — {1}{U}{R} Creature — Otter Artificer 1/3.
 *   Haste
 *   Whenever another creature you control with base power 1 enters, it gains haste until end of turn.
 *   Whenever another creature you control enters, if it wasn't cast, put a +1/+1 counter on this
 *   creature and this creature can't be blocked this turn.
 */
class RapidAugmenterScenarioTest : ScenarioTestBase() {

    private fun TestGame.augmenterCounters(): Int {
        val augmenter = findPermanent("Rapid Augmenter")!!
        return state.getEntity(augmenter)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        test("a cast base-power-1 creature gains haste but grows nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Rapid Augmenter")
                .withCardInHand(1, "Savannah Lions")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Savannah Lions").error shouldBe null
            game.resolveStack()

            val lions = game.findPermanent("Savannah Lions")!!
            withClue("base power 1 — gains haste") {
                game.state.projectedState.hasKeyword(lions, Keyword.HASTE) shouldBe true
            }
            withClue("it was cast — no counter") { game.augmenterCounters() shouldBe 0 }
        }

        test("a cast base-power-3 creature does not gain haste") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Rapid Augmenter")
                .withCardInHand(1, "Centaur Courser")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Centaur Courser").error shouldBe null
            game.resolveStack()

            val courser = game.findPermanent("Centaur Courser")!!
            game.state.projectedState.hasKeyword(courser, Keyword.HASTE) shouldBe false
        }

        test("tokens that weren't cast grow it, make it unblockable, and gain haste") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Rapid Augmenter")
                .withCardInHand(1, "Raise the Alarm")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Raise the Alarm").error shouldBe null
            game.resolveStack()

            val augmenter = game.findPermanent("Rapid Augmenter")!!
            withClue("two uncast Soldier tokens — two +1/+1 counters") { game.augmenterCounters() shouldBe 2 }
            withClue("can't be blocked this turn") {
                game.state.projectedState.hasKeyword(augmenter, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
            }
            val soldiers = game.findAllPermanents("Soldier Token")
            soldiers.size shouldBe 2
            withClue("1/1 Soldier tokens have base power 1 — both gain haste") {
                soldiers.all { game.state.projectedState.hasKeyword(it, Keyword.HASTE) } shouldBe true
            }
        }
    }
}
