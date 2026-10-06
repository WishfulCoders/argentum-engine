package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Heirloom Blade (C17 #52).
 *
 * "Whenever equipped creature dies, you may reveal cards from the top of your library until you
 *  reveal a creature card that shares a creature type with it. Put that card into your hand and
 *  the rest on the bottom of your library in a random order."
 *
 * The interesting part is "it": the filter compares library cards against the creature as it last
 * existed on the battlefield (CR 608.2h), not against its card in the graveyard. A non-Bear
 * creature on top must be skipped and the Bear below it found; a dead token (gone entirely by
 * resolution) and a dead Changeling compare by their last-known types; and a Changeling card in
 * the library shares every creature type.
 */
class HeirloomBladeScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardAttachedTo(1, "Heirloom Blade", "Grizzly Bears")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Hill Giant")     // creature, but a Giant — not a match
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Runeclaw Bear")  // shares Bear with the dead Grizzly Bears
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun dieAndReveal(host: String, isToken: Boolean, library: List<String>): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, host, isToken = isToken)
            .withCardAttachedTo(1, "Heirloom Blade", host)
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        library.forEach { builder.withCardInLibrary(1, it) }
        val game = builder.build()

        game.castSpell(1, "Lightning Bolt", game.findPermanent(host)!!).error shouldBe null
        game.resolveStack()
        game.answerYesNo(true).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Heirloom Blade") {

            test("a dead token compares by its last-known creature types") {
                val game = dieAndReveal("Grizzly Bears", isToken = true, listOf("Hill Giant", "Runeclaw Bear"))
                game.isInHand(1, "Runeclaw Bear") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe false
            }

            test("a Changeling card in the library shares a creature type with anything") {
                val game = dieAndReveal("Grizzly Bears", isToken = false, listOf("Hill Giant", "Woodland Changeling"))
                game.isInHand(1, "Woodland Changeling") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe false
            }

            test("a dead Changeling shares a creature type with any creature card") {
                val game = dieAndReveal("Woodland Changeling", isToken = false, listOf("Plains", "Hill Giant"))
                game.isInHand(1, "Hill Giant") shouldBe true
            }

            test("equipped creature dies: reveal until a creature sharing a type, rest to the bottom") {
                val game = setup()
                val island = game.findCardsInLibrary(1, "Island").single()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                withClue("the dies trigger asks whether to reveal") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("the Bear was found past the Giant and put into hand") {
                    game.isInHand(1, "Runeclaw Bear") shouldBe true
                    game.isInHand(1, "Hill Giant") shouldBe false
                }
                val library = game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                withClue("the unrevealed Island stays on top; the Giant and Plains went to the bottom") {
                    library.size shouldBe 3
                    library.first() shouldBe island
                    game.findCardsInLibrary(1, "Hill Giant").size shouldBe 1
                    game.findCardsInLibrary(1, "Plains").size shouldBe 1
                }
            }

            test("declining the may leaves the library untouched") {
                val game = setup()
                val libraryBefore = game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Runeclaw Bear") shouldBe false
                game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY)) shouldBe libraryBefore
            }
        }
    }
}
