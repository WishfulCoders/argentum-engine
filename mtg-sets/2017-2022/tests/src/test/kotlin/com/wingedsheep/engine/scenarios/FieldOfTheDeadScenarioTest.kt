package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Field of the Dead — the intervening "if" counts lands with different names, the entering land
 * (Field itself included) counts, and duplicate names count once.
 */
class FieldOfTheDeadScenarioTest : ScenarioTestBase() {

    private fun TestGame.zombies(): Int = findAllPermanents("Zombie Token").size

    private fun TestGame.playLand(name: String) {
        val land = findCardsInHand(1, name).first()
        execute(PlayLand(player1Id, land)).error shouldBe null
        resolveStack()
    }

    init {
        context("Field of the Dead") {
            test("entering as the seventh differently named land makes a Zombie") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Island")
                    .withCardOnBattlefield(1, "Swamp")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardOnBattlefield(1, "Caldera Lake")
                    .withCardInHand(1, "Field of the Dead")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.playLand("Field of the Dead")
                withClue("Field counts itself: seven names, one Zombie") { game.zombies() shouldBe 1 }
            }

            test("duplicate names count once — six names makes nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Island")
                    .withCardOnBattlefield(1, "Swamp")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardInHand(1, "Field of the Dead")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.playLand("Field of the Dead")
                withClue("seven lands but only six names") { game.zombies() shouldBe 0 }
            }

            test("another land you control entering triggers it") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Field of the Dead")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Island")
                    .withCardOnBattlefield(1, "Swamp")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Caldera Lake")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.playLand("Forest")
                withClue("the Forest is the seventh name") { game.zombies() shouldBe 1 }
            }
        }
    }
}
