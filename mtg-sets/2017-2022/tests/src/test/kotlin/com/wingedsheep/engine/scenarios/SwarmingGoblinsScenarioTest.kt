package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.DiceRolls
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Swarming Goblins (AFR #162) — "When this creature enters, roll a d20. 1—9 | Create a 1/1 red
 * Goblin creature token. 10—19 | Create two of those tokens. 20 | Create three of those tokens."
 */
class SwarmingGoblinsScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Swarming Goblins")
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        for ((natural, tokens) in listOf(1 to 1, 9 to 1, 10 to 2, 19 to 2, 20 to 3)) {
            test("a d20 of $natural creates $tokens Goblin token(s)") {
                val game = game()
                game.castSpell(1, "Swarming Goblins").error shouldBe null
                game.state = game.state.copy(rng = DiceRolls.rngRolling(20, natural))
                game.resolveStack()
                game.resolveStack()

                val goblins = game.findPermanents("Goblin Token")
                goblins.size shouldBe tokens
                game.isOnBattlefield("Swarming Goblins") shouldBe true
            }
        }
    }
}
