package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Winds of Abandon (MH1 #37): exile target creature you don't control, and its controller
 * searches (mandatory) for a basic land per creature exiled, put onto the battlefield tapped.
 * Overload {4}{W}{W} turns "target" into "each".
 */
class WindsOfAbandonScenarioTest : ScenarioTestBase() {

    private fun TestGame.landsControlledBy(player: EntityId, name: String): List<EntityId> =
        state.getBattlefield().filter { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<ControllerComponent>()?.playerId == player
        }

    /** Answer every search prompt by taking as many offered cards as allowed; returns when the stack is empty. */
    private fun TestGame.drain() {
        var guard = 0
        while ((hasPendingDecision() || state.stack.isNotEmpty()) && guard < 30) {
            val decision = getPendingDecision()
            if (decision is SelectCardsDecision) {
                selectCards(decision.options.take(decision.maxSelections))
            } else if (decision != null) {
                error("unexpected decision $decision")
            } else {
                resolveStack()
            }
            guard++
        }
    }

    init {
        context("Winds of Abandon") {

            test("single target: exiles the creature and its controller fetches one tapped basic land") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Winds of Abandon")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Winds of Abandon", bears).error shouldBe null
                game.resolveStack()
                game.drain()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
                val forests = game.landsControlledBy(game.player2Id, "Forest")
                withClue("exactly one tapped Forest for one creature") {
                    forests.size shouldBe 1
                    game.state.getEntity(forests.single())?.get<TappedComponent>() shouldBe TappedComponent
                }
            }

            test("overload: exiles every creature you don't control, fetching one land per creature") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Winds of Abandon")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithOverload(1, "Winds of Abandon").error shouldBe null
                game.resolveStack()
                game.drain()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Hill Giant") shouldBe true
                withClue("the caster's own creature is untouched") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }
                val forests = game.landsControlledBy(game.player2Id, "Forest")
                withClue("two creatures exiled -> two tapped Forests, the third stays in the library") {
                    forests.size shouldBe 2
                    forests.all { game.state.getEntity(it)?.get<TappedComponent>() == TappedComponent } shouldBe true
                    game.librarySize(2) shouldBe 1
                }
            }
        }
    }
}
