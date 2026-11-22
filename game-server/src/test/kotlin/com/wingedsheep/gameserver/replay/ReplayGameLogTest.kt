package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession
import java.time.Instant

/**
 * [ReplayGameLog] — the server replay → readable AI-review log path behind `just ai-replay-logs`.
 * A game recorded through the real [GameSession] path renders turn by turn with the AI seat marked;
 * a record current code can't reproduce is cut at the divergence and says so up front.
 */
class ReplayGameLogTest : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    private fun recordGame(): CompactReplay {
        val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
        val human = EntityId.of("log-human")
        val ai = EntityId.of("log-ai")
        val deck = mapOf("Forest" to 30, "Grizzly Bears" to 10)
        session.addPlayer(PlayerSession(mockWs("log-ws1"), human, "Alice"), deck)
        session.addPlayer(PlayerSession(mockWs("log-ws2"), ai, "[AI] Bear Tyrant"), deck)
        session.startGame()
        session.keepHand(human)
        session.keepHand(ai)
        repeat(40) {
            val state = session.getStateForTesting() ?: return@repeat
            if (state.gameOver) return@repeat
            state.priorityPlayerId?.let { session.executeAutoPass(it) }
        }
        return CompactReplay(
            gameId = session.sessionId,
            players = session.getPlayers().map { ReplayPlayerInfo(it.playerId.value, it.playerName) },
            startedAt = Instant.now().toString(),
            endedAt = Instant.now().toString(),
            winnerName = null,
            setup = requireNotNull(session.getReplaySetup()),
            actions = session.getRecordedActions(),
            engineVersion = "test-build",
            pinnedCards = session.getPinnedCards(),
            checkpoints = session.getReplayCheckpoints(),
        )
    }

    init {
        test("a recorded game renders turn by turn with the AI seat and the human named") {
            val replay = recordGame()
            val result = ReplayGameLog(cardRegistry)
                .render(replay, aiSeats = setOf("[AI] Bear Tyrant"), serverWinner = "Alice")

            result.fidelity shouldBe if (replay.checkpoints.isEmpty()) ReplayFidelity.UNVERIFIED else ReplayFidelity.EXACT
            result.actionsApplied shouldBe replay.actions.size
            result.text shouldContain "engineVersion=test-build"
            result.text shouldContain "AI: [AI] Bear Tyrant   human: Alice"
            result.text shouldContain "=== TURN 1"
            result.text shouldContain "[AI] Bear Tyrant life=20"
            result.text shouldContain "hand: "
            result.text shouldContain "winner=Alice"
            // Seat ids read as names, never as raw ids.
            result.text shouldNotContain "log-human"
            result.text shouldNotContain "log-ai"
            // Forty passes don't finish the game: the record ends with it running, and says so.
            result.text shouldContain "=== RECORD ENDS"
            result.text shouldNotContain "DIVERGED"
        }

        test("a replay that no longer re-simulates is cut at the divergence and flagged at the top") {
            val replay = recordGame()
            val drifted = replay.copy(
                checkpoints = listOf(ReplayCheckpoint(afterActionCount = 1, fingerprint = "0000000000000000")),
            )
            val result = ReplayGameLog(cardRegistry).render(drifted, aiSeats = setOf("[AI] Bear Tyrant"))

            result.fidelity shouldBe ReplayFidelity.DIVERGED
            result.actionsApplied shouldBe 0
            result.text.lines()[3] shouldContain "FIDELITY: DIVERGED at action 0"
            result.text shouldContain "the log stops here"
            result.text shouldNotContain "=== RECORD ENDS"
        }
    }
}
