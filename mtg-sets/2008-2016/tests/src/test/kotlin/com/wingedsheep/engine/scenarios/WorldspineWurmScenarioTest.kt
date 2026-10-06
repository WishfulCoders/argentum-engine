package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Worldspine Wurm (RTR #140) — dying fires both its token trigger and its from-anywhere graveyard
 * trigger: three 5/5 trample Wurms, and the card shuffled back into its owner's library.
 */
class WorldspineWurmScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.wurmTokens() = state.getBattlefield().filter { id ->
            state.getEntity(id)?.get<CardComponent>()?.name?.startsWith("Wurm") == true
        }

        test("dies: three 5/5 trample Wurm tokens, and the Wurm is shuffled into its owner's library") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Worldspine Wurm")
                .withCardInHand(2, "Murder")
                .withLandsOnBattlefield(2, "Swamp", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Murder", game.findPermanent("Worldspine Wurm")!!).error shouldBe null
            game.resolveStack()
            // Both triggers are P1's; answer an ordering prompt if one is raised.
            var guard = 0
            while (game.hasPendingDecision() && guard++ < 5) {
                val decision = game.getPendingDecision()!!
                if (decision is OrderObjectsDecision) {
                    game.submitDecision(OrderedResponse(decision.id, decision.objects)).error shouldBe null
                } else break
                game.resolveStack()
            }
            game.resolveStack()

            val tokens = game.wurmTokens()
            withClue("three Wurm tokens") { tokens.size shouldBe 3 }
            tokens.all { game.state.projectedState.getPower(it) == 5 } shouldBe true
            tokens.all { game.state.projectedState.getToughness(it) == 5 } shouldBe true
            tokens.all { game.state.projectedState.hasKeyword(it, Keyword.TRAMPLE) } shouldBe true
            withClue("the card left the graveyard for the library") {
                game.isInGraveyard(1, "Worldspine Wurm") shouldBe false
                game.findCardsInLibrary(1, "Worldspine Wurm").size shouldBe 1
            }
        }
    }
}
