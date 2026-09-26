package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * End-Blaze Epiphany — {X}{R} Instant.
 *
 *   End-Blaze Epiphany deals X damage to target creature. When that creature dies this turn,
 *   exile a number of cards from the top of your library equal to its power, then choose a card
 *   exiled this way. Until the end of your next turn, you may play that card.
 *
 * "Its power" is read with last-known information, so a creature finished off by a -N/-N effect
 * can die with negative power. A negative number of cards is zero (CR 107.1b): the delayed
 * trigger exiles nothing. The gather step used to pass the negative count straight to
 * `take(n)`, which throws.
 */
class EndBlazeEpiphanyScenarioTest : ScenarioTestBase() {

    private fun TestGame.castEpiphany(x: Int, targetName: String) {
        val epiphany = state.getHand(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "End-Blaze Epiphany"
        }
        val target = findPermanent(targetName)!!
        val cast = execute(
            CastSpell(player1Id, epiphany, listOf(ChosenTarget.Permanent(target)), xValue = x)
        )
        withClue("casting End-Blaze Epiphany with X=$x: ${cast.error}") { cast.error shouldBe null }
        if (hasPendingDecision()) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        test("the creature dies with power 2: exile the top two cards and choose one") {
            val game = scenario()
                .withPlayers("Caster", "Defender")
                .withCardInHand(1, "End-Blaze Epiphany")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val before = game.librarySize(1)
            game.castEpiphany(x = 2, targetName = "Grizzly Bears")

            withClue("Grizzly Bears died to 2 damage") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
            withClue("two cards were exiled from the top of the caster's library") {
                game.librarySize(1) shouldBe before - 2
            }
            withClue("the caster is choosing one of the exiled cards to play") {
                (game.getPendingDecision() is SelectCardsDecision) shouldBe true
            }
        }

        test("a creature that dies with negative power exiles nothing (CR 107.1b)") {
            val game = scenario()
                .withPlayers("Caster", "Defender")
                .withCardInHand(1, "End-Blaze Epiphany")
                .withCardInHand(1, "Disfigure")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val before = game.librarySize(1)
            // X = 0: no damage, but the "when that creature dies this turn" trigger is set up.
            game.castEpiphany(x = 0, targetName = "Llanowar Elves")
            withClue("0 damage leaves the 1/1 alive") {
                game.isOnBattlefield("Llanowar Elves") shouldBe true
            }

            // Disfigure makes it -1/-1: it dies with power -1.
            val disfigure = game.castSpell(1, "Disfigure", game.findPermanent("Llanowar Elves")!!)
            withClue("casting Disfigure: ${disfigure.error}") { disfigure.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            val results = game.resolveStack()

            withClue("resolving the delayed trigger raised no error") {
                results.mapNotNull { it.error } shouldBe emptyList()
            }
            withClue("Llanowar Elves died") {
                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            }
            withClue("a negative number of cards is zero: nothing was exiled") {
                game.librarySize(1) shouldBe before
            }
            withClue("the stack is empty and nothing is waiting on a choice") {
                game.state.stack.isEmpty() shouldBe true
                game.hasPendingDecision() shouldBe false
            }
        }
    }
}
