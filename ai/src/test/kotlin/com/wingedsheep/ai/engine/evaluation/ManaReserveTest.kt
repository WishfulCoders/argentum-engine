package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

/**
 * [ManaReserve] on its own: a bonus only while all four hold — our turn, an instant-speed answer in
 * hand, untapped lands that pay for it, and something across the table for it to answer.
 */
class ManaReserveTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun reserve(
        answer: String = "Counterspell",
        islands: Int = 2,
        active: Int = 1,
        theirGrip: Int = 1,
        theirCreatures: Int = 0,
        copiesInLibrary: Int = 0,
        scaled: Boolean = false,
    ): Double {
        val game = scenario()
            .withPlayers()
            .withActivePlayer(active)
            .withLandsOnBattlefield(1, "Island", islands)
            .withCardInHand(1, answer)
            .apply { repeat(copiesInLibrary) { withCardInLibrary(1, answer) } }
            .withCardsInHand(2, "Craw Wurm", theirGrip)
            .apply { repeat(theirCreatures) { withCardOnBattlefield(2, "Grizzly Bears") } }
            .build()
        return ManaReserve(intents, WEIGHT, scaled).evaluate(game.state, game.state.projectedState, game.player1Id)
    }

    init {
        test("our turn, a counterspell in hand and the lands to pay for it: the bonus") {
            reserve() shouldBe WEIGHT.plusOrMinus(1e-9)
        }

        test("one land short of the answer's cost: nothing") {
            reserve(islands = 1) shouldBe 0.0
        }

        test("the opponent's turn: nothing, so spending the mana there is free") {
            reserve(active = 2) shouldBe 0.0
        }

        test("nothing across the table to answer: nothing") {
            reserve(theirGrip = 0) shouldBe 0.0
            reserve(theirGrip = 0, theirCreatures = 1) shouldBe WEIGHT.plusOrMinus(1e-9)
        }

        test("a sorcery-speed answer or a combat trick is not held up") {
            reserve(answer = "Wrath of God", islands = 4) shouldBe 0.0
            reserve(answer = "Giant Growth") shouldBe 0.0
        }

        test("scaled by the deck's answers, capped at twice the weight") {
            // One answer in the whole deck: a quarter of a typical deck's four.
            reserve(scaled = true) shouldBe (WEIGHT / ManaReserve.TYPICAL_ANSWERS).plusOrMinus(1e-9)
            reserve(scaled = true, copiesInLibrary = 3) shouldBe WEIGHT.plusOrMinus(1e-9)
            reserve(scaled = true, copiesInLibrary = 11) shouldBe (2 * WEIGHT).plusOrMinus(1e-9)
        }
    }

    private companion object {
        const val WEIGHT = 1.5
    }
}
