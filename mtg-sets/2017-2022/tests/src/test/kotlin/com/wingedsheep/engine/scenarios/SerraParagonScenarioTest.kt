package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Serra Paragon (DMU #32): once during each of your turns, play a land or cast a permanent spell
 * with mana value 3 or less from your graveyard — one shared allowance — and whatever you play or
 * cast that way gains "When this permanent is put into a graveyard from the battlefield, exile it
 * and you gain 2 life."
 */
class SerraParagonScenarioTest : ScenarioTestBase() {

    init {
        context("Serra Paragon") {
            test("a creature cast from the graveyard returns to exile when it dies, and the land is no longer offered") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Serra Paragon")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Plains")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findCardsInGraveyard(1, "Savannah Lions").single()
                val graveyardPlains = game.findCardsInGraveyard(1, "Plains").single()
                withClue("both halves of the grant are offered") {
                    game.getLegalActions(1).any { (it.action as? CastSpell)?.cardId == lions } shouldBe true
                    game.getLegalActions(1).any { (it.action as? PlayLand)?.cardId == graveyardPlains } shouldBe true
                }

                game.castSpellFromGraveyard(1, "Savannah Lions").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Savannah Lions") shouldBe true
                withClue("the shared once-per-turn allowance is spent") {
                    game.getLegalActions(1).none { (it.action as? PlayLand)?.cardId == graveyardPlains } shouldBe true
                }

                // The opponent bolts the Lions on their own turn: the gained trigger exiles it.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val life = game.getLifeTotal(1)
                val lionsOnField = game.findPermanent("Savannah Lions")
                lionsOnField shouldNotBe null
                game.castSpell(2, "Lightning Bolt", lionsOnField).error shouldBe null
                game.resolveStack()

                withClue("exiled instead of staying in the graveyard, and 2 life gained") {
                    game.isInExile(1, "Savannah Lions") shouldBe true
                    game.isInGraveyard(1, "Savannah Lions") shouldBe false
                    game.getLifeTotal(1) shouldBe life + 2
                }
            }
        }
    }
}
