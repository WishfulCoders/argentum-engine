package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Spreading Seas (ZEN).
 *
 * "{1}{U} Enchantment — Aura. Enchant land.
 *  When this Aura enters, draw a card.
 *  Enchanted land is an Island."
 *
 * Per the 2009-10-01 ruling, "is an Island" *replaces* the land's other types (unlike Aquitect's
 * Will's "in addition to"), so the enchanted land loses its old subtypes and mana ability. Pins the
 * draw, the type replacement, and that it applies to an opponent's land too (the aura only needs
 * "enchant land", not "land you don't control" — proving the caster can target their own).
 */
class SpreadingSeasScenarioTest : ScenarioTestBase() {

    init {
        context("Spreading Seas") {

            test("draws a card when the Aura enters") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInHand(1, "Spreading Seas")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mountain = game.findPermanent("Mountain")!!
                val startHandSize = game.handSize(1)

                game.castSpell(1, "Spreading Seas", targetId = mountain).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Casting Spreading Seas (-1) then drawing on ETB (+1) nets back to the starting hand size") {
                    game.handSize(1) shouldBe startHandSize
                }
            }

            test("the enchanted land becomes an Island, replacing its other land types") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardOnBattlefield(2, "Mountain")
                    .withCardAttachedTo(1, "Spreading Seas", "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mountain = game.findPermanent("Mountain")!!
                val subtypes = game.state.projectedState.getSubtypes(mountain).map { it.lowercase() }.toSet()

                withClue("\"Enchanted land is an Island\" replaces its printed type — Mountain is gone") {
                    subtypes shouldBe setOf("island")
                }
            }
        }
    }
}
