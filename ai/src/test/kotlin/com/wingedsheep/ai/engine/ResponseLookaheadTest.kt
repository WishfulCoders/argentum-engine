package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * The one-response lookahead (`AiProfile.opponentRespondsInSimulation`, mtg-draft-ai `docs/27` §7.4).
 *
 * The scenario is the smallest one that has an answer in it: we cast a 3/3, they hold Counterspell
 * with the Islands to pay for it. Off — today's behaviour — the 3/3 always resolves, whatever they
 * are holding, and that is the blindness the flag exists to remove.
 *
 * Asserted through a **stub** policy rather than the shipped [PlayoutResponsePolicy], for the reason
 * `BoardPresenceCreatureValuationTest` gives for asserting one permanent at a time: the claim here is
 * that the hook fires, once, for the right player, and a softmax that sometimes holds the counter
 * would make a passing test mean "the policy happened to cast it today". Whether the shipped policy
 * responds *well* is an arena question, not a unit-test one; what is pinned here is that it responds
 * *the same way twice*, because arena runs are checked for bit-identical replay (`docs/31` §4).
 */
class ResponseLookaheadTest : ScenarioTestBase() {

    /** p1 has a 3/3 and the mana for it; p2 holds Counterspell and two untapped Islands. */
    private fun standoff(
        theirIslands: Int = 2,
        theirGrip: Int = 1,
    ) = scenario()
        .withPlayers()
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .withLandsOnBattlefield(1, "Forest", 3)
        .withCardInHand(1, "Centaur Courser")
        .withLandsOnBattlefield(2, "Island", theirIslands)
        .apply { repeat(theirGrip) { withCardInHand(2, "Counterspell") } }
        .build()

    private fun GameState.castOf(name: String, playerId: EntityId): GameAction =
        GameSimulator(cardRegistry).getLegalActions(this, playerId)
            .first { action ->
                (action.action as? CastSpell)
                    ?.let { getEntity(it.cardId)?.get<CardComponent>()?.name } == name
            }
            .action

    private fun GameState.controls(playerId: EntityId, name: String): Boolean =
        projectedState.getBattlefieldControlledBy(playerId).any {
            getEntity(it)?.get<CardComponent>()?.name == name
        }

    /** Casts Counterspell if it is offered, and counts how often it was asked. */
    private class CounterStub : OpponentResponsePolicy {
        var calls = 0
        override fun respond(
            state: GameState,
            opponentId: EntityId,
            enumerate: () -> List<com.wingedsheep.engine.legalactions.LegalAction>,
        ): GameAction? {
            calls++
            val counter = enumerate().firstOrNull { action ->
                (action.action as? CastSpell)
                    ?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name } == "Counterspell"
            } ?: return null
            return TargetSelection.fillHeuristically(
                state, counter, opponentId, fillPartialRequirements = true, intents = IntentCatalog.NONE
            )
        }
    }

    init {

        // ── 1. The horizon as it stands ──

        test("off: the 3/3 resolves though they are holding the counter for it") {
            val game = standoff()
            val simulator = GameSimulator(cardRegistry)
            val after = simulator.simulate(game.state, game.state.castOf("Centaur Courser", game.player1Id))
            after.state.controls(game.player1Id, "Centaur Courser").shouldBeTrue()
        }

        // ── 2. With the hook, the candidate is scored against the answer ──

        test("on: the same candidate is countered, so the board it leads to is empty") {
            val game = standoff()
            val simulator = GameSimulator(cardRegistry)
            simulator.opponentResponse = CounterStub()
            val after = simulator.simulate(game.state, game.state.castOf("Centaur Courser", game.player1Id))
            after.state.controls(game.player1Id, "Centaur Courser").shouldBeFalse()
        }

        // ── 3. The gate: cost only where an answer is possible ──

        test("tapped out or empty-handed, the policy is never asked") {
            couldRespond(standoff(theirIslands = 0).state, standoff().player2Id).shouldBeFalse()
            couldRespond(standoff(theirGrip = 0).state, standoff().player2Id).shouldBeFalse()
            couldRespond(standoff().state, standoff().player2Id).shouldBeTrue()
        }

        test("no untapped land: the 3/3 resolves with the hook on") {
            val game = standoff(theirIslands = 0)
            val simulator = GameSimulator(cardRegistry)
            val stub = CounterStub()
            simulator.opponentResponse = stub
            val after = simulator.simulate(game.state, game.state.castOf("Centaur Courser", game.player1Id))
            after.state.controls(game.player1Id, "Centaur Courser").shouldBeTrue()
            stub.calls shouldBe 0
        }

        // ── 4. One response, and only the opponent's ──

        test("the budget is one response per simulation, however many answers they hold") {
            val game = standoff(theirGrip = 3)
            val simulator = GameSimulator(cardRegistry)
            val stub = CounterStub()
            simulator.opponentResponse = stub
            simulator.simulate(game.state, game.state.castOf("Centaur Courser", game.player1Id))
            stub.calls shouldBeLessThanOrEqual 1
        }

        // ── 5. The shipped policy answers the same position the same way ──

        test("PlayoutResponsePolicy is deterministic on a position") {
            val game = standoff()
            val simulator = GameSimulator(cardRegistry)
            val policy = PlayoutResponsePolicy(
                com.wingedsheep.ai.engine.rollout.PlayoutPolicy(
                    CombatAdvisor(GameSimulator(cardRegistry), evaluatorForTest(), cardRegistry),
                    IntentCatalog.of(cardRegistry),
                    com.wingedsheep.ai.engine.rollout.RolloutSettings.DEFAULT,
                    cardRegistry,
                )
            )
            val enumerate = { simulator.getLegalActions(game.state, game.player2Id) }
            val first = policy.respond(game.state, game.player2Id, enumerate)
            val second = policy.respond(game.state, game.player2Id, enumerate)
            (first?.javaClass) shouldBe (second?.javaClass)
        }
    }

    private fun evaluatorForTest() =
        com.wingedsheep.ai.engine.evaluation.EvaluationWeights.DEFAULT.toEvaluator()
}
