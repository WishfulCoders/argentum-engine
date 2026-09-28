package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The decision `lastcard` exists for: on our own main phase, with mana open and a creature as the
 * only card in hand, cast it. Without the token the empty-hand cliff (4.0) outweighs a 2/2 on the
 * board, which is the documented failure in `EvaluationWeights.topdeckPenalty` and what held
 * Mudbutton Cursetosser for seven turns in the 2026-09-27 play session.
 */
class LastCardAiTest : ScenarioTestBase() {

    init {
        fun lastCardBears() = scenario()
            .withPlayers()
            .withCardInHand(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Forest", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("without the token the AI holds its last creature") {
            val game = lastCardBears()
            AIPlayer.create(cardRegistry, game.player1Id, AiProfile.CURRENT)
                .chooseAction(game.state).shouldBeInstanceOf<PassPriority>()
        }

        test("with two creatures in hand the token changes nothing — no cliff is crossed") {
            // The first design re-priced a one-creature hand inside the evaluator, which moved the
            // cliff up a card: casting either of two creatures then cost three points more than
            // before. The refund is on the transition, so a two-card hand must score identically.
            fun scores(tokens: String): List<Pair<String, Double?>> {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val recorded = mutableListOf<com.wingedsheep.ai.insight.AiDecisionInsight>()
                AIPlayer.create(
                    cardRegistry, game.player1Id, profileFromTokens(tokens),
                    insightSink = { _, insight -> recorded += insight },
                ).chooseAction(game.state).shouldBeInstanceOf<CastSpell>()
                return recorded.first().options.map { it.label to it.score }
            }
            scores("lastcard") shouldBe scores("current")
        }

        test("with lastcard it casts it") {
            val game = lastCardBears()
            val cast = AIPlayer.create(cardRegistry, game.player1Id, profileFromTokens("lastcard"))
                .chooseAction(game.state).shouldBeInstanceOf<CastSpell>()
            game.state.getEntity(cast.cardId)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
        }
    }
}
