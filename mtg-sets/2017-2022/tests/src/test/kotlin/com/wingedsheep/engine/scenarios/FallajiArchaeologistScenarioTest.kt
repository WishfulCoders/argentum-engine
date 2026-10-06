package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fallaji Archaeologist (BRO #48) — ETB mill three; you may put a noncreature, nonland card milled
 * this way into your hand. If you don't, put a +1/+1 counter on it.
 */
class FallajiArchaeologistScenarioTest : ScenarioTestBase() {

    private fun TestGame.counters(): Int =
        state.getEntity(findPermanent("Fallaji Archaeologist")!!)!!
            .get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun game(vararg library: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Fallaji Archaeologist")
            .withLandsOnBattlefield(1, "Island", 2)
        library.forEach { builder.withCardInLibrary(1, it) }
        return builder
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    private fun TestGame.castAndResolve() {
        castSpell(1, "Fallaji Archaeologist").error shouldBe null
        resolveStack()
        resolveStack()
    }

    init {
        test("taking a noncreature, nonland card puts it in hand and adds no counter") {
            val game = game("Lightning Bolt", "Grizzly Bears", "Forest")
            game.castAndResolve()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(game.findCardsInGraveyard(1, "Lightning Bolt"))
            game.resolveStack()

            game.isInHand(1, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Forest") shouldBe true
            game.counters() shouldBe 0
        }

        test("declining the pick adds a +1/+1 counter") {
            val game = game("Lightning Bolt", "Grizzly Bears", "Forest")
            game.castAndResolve()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(emptyList())
            game.resolveStack()

            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.counters() shouldBe 1
        }

        test("milling no eligible card adds a +1/+1 counter") {
            val game = game("Grizzly Bears", "Forest", "Forest")
            game.castAndResolve()
            if (game.getPendingDecision() is SelectCardsDecision) {
                game.selectCards(emptyList())
                game.resolveStack()
            }

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
            game.counters() shouldBe 1
        }
    }
}
