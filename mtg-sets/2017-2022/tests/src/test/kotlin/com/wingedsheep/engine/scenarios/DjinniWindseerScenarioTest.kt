package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.DieRolledEvent
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.DiceRolls
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Djinni Windseer (AFR #55) — "When this creature enters, roll a d20. 1—9 | Scry 1.
 * 10—19 | Scry 2. 20 | Scry 3." The d20 results table (CR 706.3) picks exactly one scry size, at
 * each row boundary, and the ETB trigger reports one die roll.
 */
class DjinniWindseerScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Djinni Windseer")
            .withLandsOnBattlefield(1, "Island", 4)
        repeat(5) { builder.withCardInLibrary(1, "Island") }
        return builder
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    /** Cast, force the ETB's d20 to [natural], and return how many cards the scry looked at. */
    private fun TestGame.castRolling(natural: Int): Int {
        castSpell(1, "Djinni Windseer").error shouldBe null
        state = state.copy(rng = DiceRolls.rngRolling(20, natural))
        val rolls = mutableListOf<DieRolledEvent>()
        rolls += resolveStack().flatMap { it.events }.filterIsInstance<DieRolledEvent>()
        val decision = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        val looked = decision.options.size
        submitDecision(CardsSelectedResponse(decision.id, emptyList()))
        (getPendingDecision() as? ReorderLibraryDecision)?.let { submitDecision(OrderedResponse(it.id, it.cards)) }
        rolls += resolveStack().flatMap { it.events }.filterIsInstance<DieRolledEvent>()
        rolls.map { it.naturalResult } shouldBe listOf(natural)
        isOnBattlefield("Djinni Windseer") shouldBe true
        return looked
    }

    init {
        for ((natural, scry) in listOf(1 to 1, 9 to 1, 10 to 2, 19 to 2, 20 to 3)) {
            test("a d20 of $natural scries $scry") {
                game().castRolling(natural) shouldBe scry
            }
        }
    }
}
