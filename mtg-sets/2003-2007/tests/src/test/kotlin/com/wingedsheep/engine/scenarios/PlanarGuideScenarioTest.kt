package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lgn.cards.PlanarGuide
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Planar Guide (LGN #18) — "{3}{W}, Exile Planar Guide: Exile all creatures. At the beginning of the
 * next end step, return those cards to the battlefield under their owners' control."
 *
 * Planar Guide exiles itself as part of the cost, so the ability resolves with its source already
 * gone. The creatures must still come back at the end step — and Planar Guide itself, exiled by the
 * cost rather than "this way", must not.
 */
class PlanarGuideScenarioTest : ScenarioTestBase() {

    private val abilityId = PlanarGuide.activatedAbilities.single().id

    init {
        context("Planar Guide") {

            test("every creature is exiled, then returns under its owner's control at the end step") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Planar Guide")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val guide = game.findPermanent("Planar Guide")!!
                // A cast Planar Guide carries its battlefield visit's timestamp, which is what sent
                // the old linked-exile return looking for a pile the delayed trigger couldn't see.
                game.state = game.state.updateEntity(guide) { it.with(BattlefieldEntryTimestampComponent(1L)) }
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = guide, abilityId = abilityId))
                    .error shouldBe null
                game.resolveStack()

                withClue("all creatures are in exile") {
                    game.isInExile(1, "Planar Guide") shouldBe true
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("the exiled creatures return under their owners' control") {
                    val bears = game.findPermanent("Grizzly Bears")!!
                    val giant = game.findPermanent("Hill Giant")!!
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                    game.state.projectedState.getController(giant) shouldBe game.player2Id
                }
                withClue("Planar Guide was exiled as a cost, not by the effect, so it stays exiled") {
                    game.isInExile(1, "Planar Guide") shouldBe true
                    game.isOnBattlefield("Planar Guide") shouldBe false
                }
            }
        }
    }
}
