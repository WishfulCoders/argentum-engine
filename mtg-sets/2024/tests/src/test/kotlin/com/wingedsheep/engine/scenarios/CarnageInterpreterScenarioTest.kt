package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.clu.cards.CarnageInterpreter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Carnage Interpreter (CLU #26) — {1}{B/R}{B/R} Creature — Devil Detective 3/3.
 *
 *   When this creature enters, discard your hand, then investigate four times.
 *   As long as you have one or fewer cards in hand, this creature gets +2/+2 and has menace.
 *
 * Pins the composition: the discard and the four Clues are independent (an empty hand still
 * investigates four times), and the +2/+2 and menace switch on and off together with hand size.
 */
class CarnageInterpreterScenarioTest : ScenarioTestBase() {

    private fun TestGame.interpreterStats(): Triple<Int?, Int?, Boolean> {
        val id = findPermanent("Carnage Interpreter")!!
        val projected = state.projectedState
        return Triple(projected.getPower(id), projected.getToughness(id), projected.hasKeyword(id, Keyword.MENACE))
    }

    init {
        cardRegistry.register(CarnageInterpreter)

        test("enters: discards the whole hand, then makes four Clues, and is a 5/5 menace") {
            val game = scenario()
                .withPlayers("Detective", "Opponent")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Carnage Interpreter")
                .withCardsInHand(1, "Grizzly Bears", 3)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Carnage Interpreter").error shouldBe null
            game.resolveStack()

            withClue("the whole hand is discarded") {
                game.handSize(1) shouldBe 0
                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 3
            }
            withClue("investigate four times") {
                game.findPermanents("Clue").size shouldBe 4
            }
            withClue("an empty hand turns on +2/+2 and menace") {
                game.interpreterStats() shouldBe Triple(5, 5, true)
            }
        }

        test("an empty hand when the trigger resolves still investigates four times") {
            val game = scenario()
                .withPlayers("Detective", "Opponent")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Carnage Interpreter")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Carnage Interpreter").error shouldBe null
            game.resolveStack()

            game.findPermanents("Clue").size shouldBe 4
        }

        test("one card in hand keeps the bonus; two cards turn off both the +2/+2 and menace") {
            val oneCard = scenario()
                .withPlayers("Detective", "Opponent")
                .withCardOnBattlefield(1, "Carnage Interpreter")
                .withCardsInHand(1, "Grizzly Bears", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            oneCard.interpreterStats() shouldBe Triple(5, 5, true)

            val twoCards = scenario()
                .withPlayers("Detective", "Opponent")
                .withCardOnBattlefield(1, "Carnage Interpreter")
                .withCardsInHand(1, "Grizzly Bears", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            twoCards.interpreterStats() shouldBe Triple(3, 3, false)
        }
    }
}
