package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Brazen Borrower // Petty Theft (ELD #39).
 *
 * Creature face: {1}{U}{U} 3/1 Faerie Rogue — Flash, Flying, can block only creatures with flying.
 * Adventure face: Petty Theft {1}{U}, Instant — Adventure — "Return target nonland permanent an
 * opponent controls to its owner's hand."
 *
 * The composition worth proving is the whole instant-speed line on an opponent's turn: Petty
 * Theft bounces, exiles the card, and the flash creature is then cast from exile still on that
 * opponent's turn. Plus the two target restrictions (an opponent's permanent, nonland).
 */
class BrazenBorrowerScenarioTest : ScenarioTestBase() {

    init {
        test("Petty Theft bounces on the opponent's turn, then the Borrower is flashed in from exile") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Brazen Borrower")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val borrower = game.findCardsInHand(1, "Brazen Borrower").single()
            val bears = game.findPermanent("Grizzly Bears")!!

            // faceIndex = 0 is the Adventure face (CR 715).
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = borrower,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    faceIndex = 0
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears went back to its owner's hand") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInHand(2, "Grizzly Bears") shouldBe true
            }
            withClue("the Adventure exiled itself (CR 715.3d)") {
                game.isInExile(1, "Brazen Borrower") shouldBe true
            }

            // Still the opponent's main phase: flash lets the creature come down from exile now.
            game.state.activePlayerId shouldBe game.player2Id
            if (game.state.priorityPlayerId != game.player1Id) game.passPriority()
            game.castSpellFromExile(1, "Brazen Borrower").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Brazen Borrower") shouldBe true
            game.isInExile(1, "Brazen Borrower") shouldBe false
        }

        test("Petty Theft can't target your own permanent or an opponent's land") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Brazen Borrower")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val borrower = game.findCardsInHand(1, "Brazen Borrower").single()
            for (target in listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Forest")!!)) {
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = borrower,
                        targets = listOf(ChosenTarget.Permanent(target)),
                        faceIndex = 0
                    )
                ).error shouldNotBe null
            }
        }
    }
}
