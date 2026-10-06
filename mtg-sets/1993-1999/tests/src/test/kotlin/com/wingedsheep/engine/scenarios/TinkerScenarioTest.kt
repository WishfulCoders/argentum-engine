package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Tinker (ULG #45) — sacrifice an artifact as an additional cost, then search for an artifact card
 * and put it onto the battlefield.
 */
class TinkerScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing Ornithopter puts the searched-for Juggernaut onto the battlefield") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Tinker")
                .withCardOnBattlefield(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Juggernaut")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithAdditionalSacrifice(1, "Tinker", "Ornithopter").error shouldBe null
            game.isInGraveyard(1, "Ornithopter") shouldBe true
            game.resolveStack()
            game.selectCards(game.findCardsInLibrary(1, "Juggernaut")).error shouldBe null

            game.isOnBattlefield("Juggernaut") shouldBe true
            game.librarySize(1) shouldBe 1
        }
    }
}
