package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Oust (ROE #40) — {W} Sorcery.
 *
 * Put target creature into its owner's library second from the top. Its controller gains 3 life.
 *
 * Pins: the creature lands directly under the top card of its owner's library, and the life goes
 * to the creature's controller, not to the caster.
 */
class OustScenarioTest : ScenarioTestBase() {

    init {
        context("Oust") {
            test("creature goes second from the top; its controller gains 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Oust")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angel = game.findPermanent("Serra Angel")!!
                val casterLife = game.getLifeTotal(1)
                val ownerLife = game.getLifeTotal(2)

                game.castSpell(1, "Oust", angel).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Serra Angel") shouldBe false
                val library = game.state.getLibrary(game.player2Id)
                library.size shouldBe 3
                game.state.getEntity(library[1])?.get<CardComponent>()?.name shouldBe "Serra Angel"

                game.getLifeTotal(2) shouldBe ownerLife + 3
                game.getLifeTotal(1) shouldBe casterLife
            }
        }
    }
}
