package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dragon Mantle (THS).
 *
 * "{R} Enchantment — Aura. Enchant creature. When this Aura enters, draw a card.
 *  Enchanted creature has '{R}: This creature gets +1/+0 until end of turn.'"
 *
 * Pins the ETB draw and that the granted firebreathing ability is the *enchanted creature's own*
 * ability (activated off the creature, paid with its own {R}, pumping the creature — not Dragon
 * Mantle itself).
 */
class DragonMantleScenarioTest : ScenarioTestBase() {

    init {
        context("Dragon Mantle") {

            test("draws a card when the Aura enters, attached via casting") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInHand(1, "Dragon Mantle")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val startHandSize = game.handSize(1)

                game.castSpell(1, "Dragon Mantle", targetId = bears).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Dragon Mantle should be attached to Grizzly Bears") {
                    game.isOnBattlefield("Dragon Mantle") shouldBe true
                }
                withClue("Casting Dragon Mantle (-1) then drawing on ETB (+1) nets back to the starting hand size") {
                    game.handSize(1) shouldBe startHandSize
                }
            }

            test("the enchanted creature gains a firebreathing ability it can activate for {R}") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Dragon Mantle", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val firebreathing = game.getLegalActions(1).find { info ->
                    info.actionType == "ActivateAbility" &&
                        (info.action as? ActivateAbility)?.sourceId == bears
                }
                withClue("The granted '{R}: +1/+0' ability should be a legal action on Grizzly Bears") {
                    firebreathing.shouldNotBeNull()
                }

                game.execute(firebreathing!!.action).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("Grizzly Bears (printed 2/2) should be pumped to 3/2 until end of turn") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
