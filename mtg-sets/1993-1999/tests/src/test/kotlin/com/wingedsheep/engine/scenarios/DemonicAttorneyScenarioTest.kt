package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Demonic Attorney {1}{B}{B} — Sorcery: "Each player antes the top card of their library."
 *
 * Each player antes their *own* top card (only an owner can ante a card, CR 407.4).
 */
class DemonicAttorneyScenarioTest : ScenarioTestBase() {

    init {
        test("each player antes the top card of their own library") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .playingForAnte()
                .withCardInHand(1, "Demonic Attorney")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Shivan Dragon")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Serra Angel")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Demonic Attorney").error shouldBe null
            game.resolveStack()

            withClue("player 1 anted their top card") {
                game.isInAnte(1, "Shivan Dragon") shouldBe true
                game.librarySize(1) shouldBe 1
            }
            withClue("player 2 anted theirs, into their own part of the ante") {
                game.isInAnte(2, "Serra Angel") shouldBe true
                game.librarySize(2) shouldBe 1
            }
        }
    }
}
