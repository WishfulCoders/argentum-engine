package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Life // Death (APC #130).
 *
 * Life {G}: "All lands you control become 1/1 creatures until end of turn. They're still lands."
 * Death {1}{B}: "Return target creature card from your graveyard to the battlefield. You lose life
 * equal to its mana value."
 */
class LifeDeathScenarioTest : ScenarioTestBase() {
    init {
        test("Death returns a creature card and its caster loses life equal to its mana value") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Life // Death")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.findCardsInHand(1, "Life // Death").single()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.execute(
                CastSpell(
                    game.player1Id,
                    card,
                    targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                    faceIndex = 1,
                )
            ).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Bears returned") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
            withClue("lost 2 life (Bears' mana value)") { game.getLifeTotal(1) shouldBe 18 }
        }

        test("Life turns your lands, and only yours, into 1/1 creatures that are still lands") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Life // Death")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.findCardsInHand(1, "Life // Death").single()
            game.execute(CastSpell(game.player1Id, card, faceIndex = 0)).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val projected = game.state.projectedState
            for (forest in game.findPermanents("Forest")) {
                projected.isCreature(forest) shouldBe true
                projected.hasType(forest, "LAND") shouldBe true
                projected.getPower(forest) shouldBe 1
                projected.getToughness(forest) shouldBe 1
            }
            val mountain = game.findPermanent("Mountain")!!
            withClue("opponent's land is untouched") { projected.isCreature(mountain) shouldBe false }
        }
    }
}
