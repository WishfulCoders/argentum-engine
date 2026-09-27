package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Path to Exile (CON #15).
 *
 * "{W} Instant
 *  Exile target creature. Its controller may search their library for a basic land card, put that
 *  card onto the battlefield tapped, then shuffle."
 *
 * The targeted creature's *controller* (not the caster) decides whether to search, and declining
 * skips the shuffle too.
 */
class PathToExileScenarioTest : ScenarioTestBase() {

    init {
        test("exiles the target creature; its controller may fetch a tapped basic land") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Path to Exile")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            val cast = game.castSpell(1, "Path to Exile", targetId = bears)
            withClue("Casting Path to Exile at Grizzly Bears should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Grizzly Bears should be exiled") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }

            withClue("The creature's controller (player 2), not the caster, decides whether to search") {
                game.hasPendingDecision() shouldBe true
                game.getPendingDecision()?.playerId shouldBe game.player2Id
            }
            game.answerYesNo(true)
            game.resolveStack()

            withClue("Player 2 should choose which basic land to fetch") {
                game.getPendingDecision()?.playerId shouldBe game.player2Id
            }
            val forest = game.state.getLibrary(game.player2Id).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
            }
            game.selectCards(listOf(forest))
            game.resolveStack()

            withClue("The fetched Forest should be on the battlefield, tapped") {
                val forestOnBf = game.findPermanent("Forest")
                (forestOnBf != null) shouldBe true
                val forestEntity = game.state.getEntity(forestOnBf!!)
                forestEntity?.get<TappedComponent>() shouldBe TappedComponent
                forestEntity?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
            }
        }

        test("the controller may decline the search, and then no shuffle or fetch happens") {
            val game = scenario()
                .withPlayers("Caster", "Victim")
                .withCardInHand(1, "Path to Exile")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val librarySizeBefore = game.librarySize(2)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Path to Exile", targetId = bears)
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("Player 2 should be asked whether to search") {
                game.hasPendingDecision() shouldBe true
                game.getPendingDecision()?.playerId shouldBe game.player2Id
            }
            game.answerYesNo(false)
            game.resolveStack()

            withClue("Grizzly Bears is still exiled regardless of the decline") {
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }
            withClue("No land should have entered the battlefield") {
                game.isOnBattlefield("Forest") shouldBe false
            }
            withClue("The library is untouched (no fetch, and nothing to shuffle)") {
                game.librarySize(2) shouldBe librarySizeBefore
            }
        }
    }
}
