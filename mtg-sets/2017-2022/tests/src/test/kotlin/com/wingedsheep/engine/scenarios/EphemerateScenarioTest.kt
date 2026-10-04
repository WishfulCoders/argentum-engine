package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ephemerate (MH1 #7) — {W} Instant.
 *
 * Exile target creature you control, then return it to the battlefield under its owner's control.
 * Rebound
 *
 * Pins: the creature comes back as a new (untapped) object, and the spell, cast from hand, is
 * exiled by rebound instead of going to the graveyard.
 */
class EphemerateScenarioTest : ScenarioTestBase() {

    init {
        context("Ephemerate") {
            test("blinks a tapped creature back untapped; rebound exiles the spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ephemerate")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Ephemerate", bears).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")
                withClue("the Bears are back on the battlefield, untapped") {
                    (returned != null) shouldBe true
                    game.state.getEntity(returned!!)!!.has<TappedComponent>() shouldBe false
                }
                withClue("rebound exiled Ephemerate instead of putting it in the graveyard") {
                    game.isInExile(1, "Ephemerate") shouldBe true
                    game.isInGraveyard(1, "Ephemerate") shouldBe false
                }
            }
        }
    }
}
