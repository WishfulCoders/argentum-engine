package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Titania, Protector of Argoth — the enters trigger returns a land card from your graveyard, and a
 * land you control going to the graveyard from the battlefield (here: destroyed) makes a 5/3
 * Elemental. Pins that the land-"dies" trigger fires for a noncreature permanent and only for
 * your own lands.
 */
class TitaniaProtectorOfArgothScenarioTest : ScenarioTestBase() {

    private fun TestGame.elementalTokens(): Int =
        findAllPermanents("Elemental Token").size

    init {
        context("Titania, Protector of Argoth") {
            test("enters and returns a land card from your graveyard to the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Titania, Protector of Argoth")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInGraveyard(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Titania, Protector of Argoth").error shouldBe null
                game.resolveStack()

                val mountain = game.findCardsInGraveyard(1, "Mountain").single()
                if (game.hasPendingDecision()) game.selectTargets(listOf(mountain))
                game.resolveStack()

                withClue("the Mountain came back from the graveyard") {
                    game.isOnBattlefield("Mountain") shouldBe true
                    game.isInGraveyard(1, "Mountain") shouldBe false
                }
            }

            test("a land you control put into the graveyard from the battlefield makes a 5/3 Elemental") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Titania, Protector of Argoth")
                    .withCardOnBattlefield(1, "Forest")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardsInHand(1, "Stone Rain", 2)
                    .withCardOnBattlefield(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                game.castSpell(1, "Stone Rain", forest).error shouldBe null
                game.resolveStack()

                withClue("the Forest died and Titania made one Elemental") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                    game.elementalTokens() shouldBe 1
                }
                val elemental = game.findAllPermanents("Elemental Token").single()
                game.state.projectedState.getPower(elemental) shouldBe 5
                game.state.projectedState.getToughness(elemental) shouldBe 3
            }

            test("an opponent's land dying makes nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Titania, Protector of Argoth")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInHand(1, "Stone Rain")
                    .withCardOnBattlefield(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val plains = game.findPermanent("Plains")!!
                game.castSpell(1, "Stone Rain", plains).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Plains") shouldBe true
                game.elementalTokens() shouldBe 0
            }
        }
    }
}
