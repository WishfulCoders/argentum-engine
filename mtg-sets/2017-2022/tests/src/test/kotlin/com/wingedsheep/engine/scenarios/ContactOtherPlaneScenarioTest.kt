package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.DiceRolls
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Contact Other Plane (AFR #52) — "Roll a d20. 1—9 | Draw two cards. 10—19 | Scry 2, then draw two
 * cards. 20 | Scry 3, then draw three cards." Each row is checked for both halves: how many cards
 * the scry looked at (none on 1—9) and how many were drawn after it.
 */
class ContactOtherPlaneScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Contact Other Plane")
            .withLandsOnBattlefield(1, "Island", 4)
        repeat(8) { builder.withCardInLibrary(1, "Island") }
        return builder
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    /** Cast with the d20 forced to [natural]; returns (cards scried, cards drawn). */
    private fun TestGame.castRolling(natural: Int): Pair<Int, Int> {
        castSpell(1, "Contact Other Plane").error shouldBe null
        val handAfterCast = handSize(1)
        state = state.copy(rng = DiceRolls.rngRolling(20, natural))
        resolveStack()
        var scried = 0
        (getPendingDecision() as? SelectCardsDecision)?.let { decision ->
            scried = decision.options.size
            submitDecision(CardsSelectedResponse(decision.id, emptyList()))
            (getPendingDecision() as? ReorderLibraryDecision)?.let {
                submitDecision(OrderedResponse(it.id, it.cards))
            }
            resolveStack()
        }
        getPendingDecision().shouldBeNull()
        isInGraveyard(1, "Contact Other Plane") shouldBe true
        return scried to handSize(1) - handAfterCast
    }

    init {
        test("1—9: draw two cards, no scry") {
            game().castRolling(1) shouldBe (0 to 2)
            game().castRolling(9) shouldBe (0 to 2)
        }
        test("10—19: scry 2, then draw two cards") {
            game().castRolling(10) shouldBe (2 to 2)
            game().castRolling(19) shouldBe (2 to 2)
        }
        test("20: scry 3, then draw three cards") {
            game().castRolling(20) shouldBe (3 to 3)
        }
    }
}
