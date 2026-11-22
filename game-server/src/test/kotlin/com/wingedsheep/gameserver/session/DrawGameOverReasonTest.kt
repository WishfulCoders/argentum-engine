package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.scenario.PlayerConfig
import com.wingedsheep.gameserver.scenario.ScenarioBuilderService
import com.wingedsheep.gameserver.scenario.ScenarioMode
import com.wingedsheep.gameserver.scenario.ScenarioRequest
import com.wingedsheep.gameserver.scenario.ScenarioSeat
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * A game with no winner reports why: everyone losing at once is [GameOverReason.DRAW], and a draw
 * nobody lost — an effect saying "the game is a draw" (CR 104.4c, Divine Intervention) — is
 * [GameOverReason.CARD_EFFECT], so the client doesn't tell the players they both lost.
 */
class DrawGameOverReasonTest : ScenarioTestBase() {

    private fun seatState(vararg names: String): Pair<GameState, List<EntityId>> {
        val build = ScenarioBuilderService(cardRegistry).buildScenario(
            ScenarioRequest(players = names.map { ScenarioSeat(it, PlayerConfig()) }, mode = ScenarioMode.SELF)
        )
        return build.state to build.playerIds
    }

    private fun twoSeatState() = seatState("Alice", "Bob")

    init {
        test("a draw nobody lost is reported as a card effect") {
            val (state, _) = twoSeatState()
            val session = GameSession(cardRegistry = cardRegistry)
            session.injectStateForDevScenario(state.copy(gameOver = true, winnerId = null))

            session.getGameOverReason() shouldBe GameOverReason.CARD_EFFECT
        }

        test("everyone losing at once is still a plain draw") {
            val (state, players) = twoSeatState()
            val lost = players.fold(state) { s, p -> s.updateEntity(p) { it.with(PlayerLostComponent(LossReason.LIFE_ZERO)) } }
            val session = GameSession(cardRegistry = cardRegistry)
            session.injectStateForDevScenario(lost.copy(gameOver = true, winnerId = null))

            session.getGameOverReason() shouldBe GameOverReason.DRAW
        }

        test("in a pod, a seat eliminated earlier doesn't turn a card-effect draw into a plain one") {
            val (state, players) = seatState("Alice", "Bob", "Charlie")
            val earlierLoss = state.updateEntity(players[2]) { it.with(PlayerLostComponent(LossReason.LIFE_ZERO)) }
            val session = GameSession(cardRegistry = cardRegistry)
            session.injectStateForDevScenario(earlierLoss.copy(gameOver = true, winnerId = null))

            session.getGameOverReason() shouldBe GameOverReason.CARD_EFFECT
        }
    }
}
