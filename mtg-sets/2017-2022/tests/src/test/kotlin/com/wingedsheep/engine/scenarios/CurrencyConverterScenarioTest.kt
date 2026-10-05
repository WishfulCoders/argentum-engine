package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ncc.cards.CurrencyConverter
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Currency Converter (NCC #81).
 *
 *   Whenever you discard a card, you may exile that card from your graveyard.
 *   {2}, {T}: Draw a card, then discard a card.
 *   {T}: Put a card exiled with this artifact into its owner's graveyard. If it's a land card,
 *   create a Treasure token. If it's a nonland card, create a 2/2 black Rogue creature token.
 *
 * Pins the linked pile across abilities: a discard feeds it (only when accepted), and the {T}
 * ability returns the card to the graveyard and makes the token matching its card type.
 */
class CurrencyConverterScenarioTest : ScenarioTestBase() {

    // {0} sorcery: "Discard a card." — a free way to discard without tapping the Converter.
    private val testRummage = card("Test Rummage") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "Discard a card."
        spell {
            effect = Patterns.Hand.discardCards(1)
        }
    }

    private val payout = CurrencyConverter.activatedAbilities.single { it.description.startsWith("{T}:") }

    private fun TestGame.discardIntoConverter(cardName: String, accept: Boolean) {
        castSpell(1, "Test Rummage").error shouldBe null
        resolveStack()
        if (hasPendingDecision() && getPendingDecision() !is YesNoDecision) {
            selectCards(findCardsInHand(1, cardName))
        }
        resolveStack()
        getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        answerYesNo(accept)
        resolveStack()
    }

    private fun TestGame.activatePayout() {
        val converter = findPermanent("Currency Converter")!!
        val result = execute(ActivateAbility(playerId = player1Id, sourceId = converter, abilityId = payout.id))
        withClue("Payout should activate: ${result.error}") { result.error shouldBe null }
        resolveStack()
    }

    private fun board(discardFodder: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Currency Converter")
        .withCardInHand(1, "Test Rummage")
        .withCardInHand(1, discardFodder)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(testRummage)

        test("a discarded land is exiled, then paid out as a Treasure") {
            val game = board("Forest")
            game.discardIntoConverter("Forest", accept = true)

            withClue("The Forest was exiled from the graveyard") {
                game.isInGraveyard(1, "Forest") shouldBe false
                game.isInExile(1, "Forest") shouldBe true
            }

            game.activatePayout()
            withClue("The Forest went back to its owner's graveyard") {
                game.isInGraveyard(1, "Forest") shouldBe true
                game.state.getZone(game.player1Id, Zone.EXILE).size shouldBe 0
            }
            withClue("A land makes a Treasure, not a Rogue") {
                game.findPermanent("Treasure") shouldNotBe null
                game.findPermanent("Rogue Token") shouldBe null
            }
        }

        test("a discarded nonland card pays out as a 2/2 black Rogue") {
            val game = board("Grizzly Bears")
            game.discardIntoConverter("Grizzly Bears", accept = true)
            game.isInExile(1, "Grizzly Bears") shouldBe true

            game.activatePayout()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            withClue("A nonland card makes a Rogue, not a Treasure") {
                game.findPermanent("Rogue Token") shouldNotBe null
                game.findPermanent("Treasure") shouldBe null
            }
        }

        test("declining leaves the card in the graveyard, and an empty payout does nothing") {
            val game = board("Grizzly Bears")
            game.discardIntoConverter("Grizzly Bears", accept = false)
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true

            game.activatePayout()
            withClue("Nothing was exiled with the Converter, so no token is made") {
                game.findPermanent("Rogue Token") shouldBe null
                game.findPermanent("Treasure") shouldBe null
            }
        }
    }
}
