package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Contract from Below {B} — Sorcery:
 * "Discard your hand, ante the top card of your library, then draw seven cards."
 *
 * Pins the order of the three steps: the old hand is discarded first (the fresh seven survive),
 * and the card anted is the library top *before* the draw.
 */
class ContractFromBelowScenarioTest : ScenarioTestBase() {

    init {
        test("discards the hand, antes the top card, then draws seven") {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .playingForAnte()
                .withCardInHand(1, "Contract from Below")
                .withCardInHand(1, "Hill Giant")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInLibrary(1, "Shivan Dragon")
            repeat(8) { builder.withCardInLibrary(1, "Forest") }
            val game = builder
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Contract from Below").error shouldBe null
            game.resolveStack()

            withClue("the old hand is discarded") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Giant Growth") shouldBe true
            }
            withClue("the library top went into the ante, not into the new hand") {
                game.isInAnte(1, "Shivan Dragon") shouldBe true
                game.anteSize(1) shouldBe 1
            }
            withClue("seven fresh cards, none of them discarded") {
                game.handSize(1) shouldBe 7
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
