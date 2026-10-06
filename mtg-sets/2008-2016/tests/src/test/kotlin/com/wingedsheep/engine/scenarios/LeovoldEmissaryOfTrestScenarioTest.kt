package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Leovold, Emissary of Trest (CN2 #77): the draw cap ([com.wingedsheep.sdk.scripting.RestrictDrawsPerTurn])
 * and the "you or a permanent you control becomes the target of a spell or ability an opponent
 * controls" trigger (`BecomesTargetEvent.targetPlayer`).
 */
class LeovoldEmissaryOfTrestScenarioTest : ScenarioTestBase() {

    init {
        fun opponentsTurn() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Leovold, Emissary of Trest")
            .withCardInHand(2, "Shock")
            .withLandsOnBattlefield(2, "Mountain", 1)
            .apply { repeat(3) { withCardInLibrary(1, "Forest") } }
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("an opponent's spell targeting you lets you draw a card") {
            val game = opponentsTurn().build()

            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 1
            game.getLifeTotal(1) shouldBe 18
        }

        test("an opponent's spell targeting a permanent you control triggers it; the draw is optional") {
            val game = opponentsTurn().build()
            val leovold = game.findPermanent("Leovold, Emissary of Trest")!!

            game.castSpell(2, "Shock", leovold).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            withClue("declined the may") { game.handSize(1) shouldBe 0 }
        }

        test("your own spell targeting you does not trigger it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Leovold, Emissary of Trest")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .apply { repeat(3) { withCardInLibrary(1, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Shock", 1).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.handSize(1) shouldBe 0
        }

        test("an opponent targeting their own permanent does not trigger it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Leovold, Emissary of Trest")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .apply { repeat(3) { withCardInLibrary(1, "Forest") } }
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.handSize(1) shouldBe 0
        }

        test("each opponent can't draw more than one card each turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Leovold, Emissary of Trest")
                .withCardInHand(1, "Inspiration")
                .withLandsOnBattlefield(1, "Island", 4)
                .apply { repeat(4) { withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Inspiration", 2).error shouldBe null
            game.resolveStack()
            game.handSize(2) shouldBe 1
        }
    }
}
