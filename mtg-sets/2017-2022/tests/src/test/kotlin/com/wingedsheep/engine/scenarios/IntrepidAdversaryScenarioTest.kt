package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Intrepid Adversary — {1}{W} 3/1 lifelink (MID).
 *
 * "When this creature enters, you may pay {1}{W} any number of times. When you pay this cost one or
 * more times, put that many valor counters on this creature. Creatures you control get +1/+1 for
 * each valor counter on this creature."
 *
 * Pins the valor counter end to end: the number of payments becomes the valor count on a separate
 * reflexive stack object (CR 603.12a — it triggers once however many times the cost was paid, and
 * players can respond before the counters arrive), and the anthem scales with the live count.
 */
class IntrepidAdversaryScenarioTest : ScenarioTestBase() {

    private fun cast(plains: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Intrepid Adversary")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", plains)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Intrepid Adversary").error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
        return game
    }

    private fun TestGame.valor(): Int =
        state.getEntity(findPermanent("Intrepid Adversary")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.VALOR) ?: 0

    private fun TestGame.pt(name: String): Pair<Int?, Int?> {
        val id = findPermanent(name)!!
        return state.projectedState.getPower(id) to state.projectedState.getToughness(id)
    }

    init {
        test("paying twice puts two valor counters on it via a separate reflexive trigger") {
            // 2 Plains cast it; 4 more pay {1}{W} twice.
            val game = cast(plains = 6)
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            val count = game.getPendingDecision() as ChooseNumberDecision
            count.maxValue shouldBe 2
            game.chooseNumber(2)

            withClue("the counters wait for the reflexive ability to resolve (the ruling)") {
                game.state.stack.size shouldBe 1
                game.valor() shouldBe 0
            }
            game.resolveStack()

            game.valor() shouldBe 2
            withClue("+1/+1 per valor counter, itself included") {
                game.pt("Intrepid Adversary") shouldBe (5 to 3)
                game.pt("Grizzly Bears") shouldBe (4 to 4)
            }
        }

        test("one affordable payment needs no count and gives one counter") {
            val game = cast(plains = 4)
            game.answerYesNo(true)
            game.resolveStack()

            game.valor() shouldBe 1
            game.pt("Grizzly Bears") shouldBe (3 to 3)
        }

        test("declining the payment puts no counters and grants nothing") {
            val game = cast(plains = 6)
            game.answerYesNo(false)
            game.resolveStack()

            game.valor() shouldBe 0
            game.state.stack.size shouldBe 0
            game.pt("Intrepid Adversary") shouldBe (3 to 1)
            game.pt("Grizzly Bears") shouldBe (2 to 2)
        }

        test("with no mana left to pay even once, nothing is asked and nothing triggers") {
            val game = cast(plains = 2)
            game.getPendingDecision() shouldBe null
            game.valor() shouldBe 0
            game.pt("Grizzly Bears") shouldBe (2 to 2)
        }
    }
}
