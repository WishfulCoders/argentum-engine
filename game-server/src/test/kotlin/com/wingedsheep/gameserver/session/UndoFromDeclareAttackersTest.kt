package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Regression: pressing "To attackers" at precombat main should leave the undo
 * checkpoint intact so the active player can step back to the main phase to
 * cast a forgotten sorcery — as long as no attackers have been declared yet.
 *
 * Previously, the opponent's auto-passes through BEGIN_COMBAT and DECLARE_ATTACKERS
 * unconditionally cleared the checkpoint set by SET_PRECOMBAT_CHECKPOINT. A bare
 * `PassPriority` from the opponent now preserves it (matching the engine's
 * `UndoCheckpointAction.PRESERVE` for that action).
 */
class UndoFromDeclareAttackersTest : ScenarioTestBase() {

    init {
        test("AP can undo back to precombat main after auto-passing into declare-attackers") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withLandsOnBattlefield(1, "Mountain", 1) // one main-phase action already taken
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val session = newSession(game)

            // AP presses "To Attackers" — server records SET_PRECOMBAT_CHECKPOINT.
            session.executeAction(game.player1Id, PassPriority(game.player1Id)).also {
                check(it is com.wingedsheep.gameserver.session.GameSession.ActionResult.Success) {
                    "Pass priority should succeed: $it"
                }
            }

            // Drive the auto-pass loop manually until AP lands at DECLARE_ATTACKERS.
            // Stop at DECLARE_ATTACKERS — the active player must explicitly declare
            // attackers there, the engine does not advance past it on its own.
            repeat(100) {
                val state = session.getStateForTesting()!!
                if (state.step == Step.DECLARE_ATTACKERS && state.priorityPlayerId == game.player1Id) {
                    return@repeat
                }
                val priorityPlayer = state.priorityPlayerId!!
                session.executeAutoPass(priorityPlayer)
            }

            val finalState = session.getStateForTesting()!!
            withClue("Expected to arrive at DECLARE_ATTACKERS with AP holding priority") {
                finalState.step shouldBe Step.DECLARE_ATTACKERS
                finalState.priorityPlayerId shouldBe game.player1Id
            }

            withClue("Undo should be available at declare-attackers (no attackers declared yet)") {
                session.isUndoAvailable(game.player1Id) shouldBe true
            }

            // Undoing restores the precombat-main state (with the land still on battlefield).
            val undoResult = session.executeUndo(game.player1Id)
            check(undoResult is com.wingedsheep.gameserver.session.GameSession.ActionResult.Success) {
                "Undo should succeed: $undoResult"
            }

            val restored = session.getStateForTesting()!!
            withClue("Undo should restore precombat main with AP holding priority") {
                restored.step shouldBe Step.PRECOMBAT_MAIN
                restored.priorityPlayerId shouldBe game.player1Id
            }
            withClue("Land played before the checkpoint should still be on the battlefield") {
                val battlefield = restored.zones[ZoneKey(game.player1Id, Zone.BATTLEFIELD)].orEmpty()
                val hasMountain = battlefield.any { id ->
                    restored.getEntity(id)?.get<CardComponent>()?.name == "Mountain"
                }
                hasMountain shouldBe true
            }
        }

        test("AP cannot undo once real attackers have been declared") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val session = newSession(game)

            // AP presses "To Attackers" — server records SET_PRECOMBAT_CHECKPOINT.
            session.executeAction(game.player1Id, PassPriority(game.player1Id))

            // Drive the auto-pass loop until AP lands at DECLARE_ATTACKERS.
            repeat(100) {
                val state = session.getStateForTesting()!!
                if (state.step == Step.DECLARE_ATTACKERS && state.priorityPlayerId == game.player1Id) {
                    return@repeat
                }
                session.executeAutoPass(state.priorityPlayerId!!)
            }

            val atDeclare = session.getStateForTesting()!!
            withClue("Undo is available before attackers are declared") {
                session.isUndoAvailable(game.player1Id) shouldBe true
            }

            // AP declares the Grizzly Bears as an attacker — a real commitment.
            val bearId = atDeclare.zones[ZoneKey(game.player1Id, Zone.BATTLEFIELD)].orEmpty()
                .first { atDeclare.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
            val declareResult = session.executeAction(
                game.player1Id,
                DeclareAttackers(game.player1Id, mapOf(bearId to game.player2Id))
            )
            check(declareResult is com.wingedsheep.gameserver.session.GameSession.ActionResult.Success) {
                "Declare attackers should succeed: $declareResult"
            }

            withClue("Undo must be unavailable once real attackers are declared") {
                session.isUndoAvailable(game.player1Id) shouldBe false
            }
            withClue("Attempting to undo anyway should fail") {
                val undo = session.executeUndo(game.player1Id)
                (undo is com.wingedsheep.gameserver.session.GameSession.ActionResult.Failure) shouldBe true
            }
        }

        test("DP cannot undo once real blockers have been declared") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val session = newSession(game)

            // Drive AP to DECLARE_ATTACKERS and declare a real attacker.
            repeat(100) {
                val state = session.getStateForTesting()!!
                if (state.step == Step.DECLARE_ATTACKERS && state.priorityPlayerId == game.player1Id) {
                    return@repeat
                }
                session.executeAutoPass(state.priorityPlayerId!!)
            }
            val atDeclareAttackers = session.getStateForTesting()!!
            val attackerId = atDeclareAttackers.zones[ZoneKey(game.player1Id, Zone.BATTLEFIELD)].orEmpty()
                .first { atDeclareAttackers.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
            session.executeAction(
                game.player1Id,
                DeclareAttackers(game.player1Id, mapOf(attackerId to game.player2Id))
            )

            // Drive to DECLARE_BLOCKERS with DP holding priority (no blockers declared yet).
            repeat(100) {
                val state = session.getStateForTesting()!!
                if (state.step == Step.DECLARE_BLOCKERS && state.priorityPlayerId == game.player2Id) {
                    return@repeat
                }
                session.executeAutoPass(state.priorityPlayerId!!)
            }

            val atDeclareBlockers = session.getStateForTesting()!!
            withClue("Expected to arrive at DECLARE_BLOCKERS with DP holding priority") {
                atDeclareBlockers.step shouldBe Step.DECLARE_BLOCKERS
                atDeclareBlockers.priorityPlayerId shouldBe game.player2Id
            }
            withClue("Undo is available before blockers are declared") {
                session.isUndoAvailable(game.player2Id) shouldBe true
            }

            // DP declares a real block — a commitment that reveals information.
            val blockerId = atDeclareBlockers.zones[ZoneKey(game.player2Id, Zone.BATTLEFIELD)].orEmpty()
                .first { atDeclareBlockers.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
            val declareResult = session.executeAction(
                game.player2Id,
                DeclareBlockers(game.player2Id, mapOf(blockerId to listOf(attackerId)))
            )
            check(declareResult is com.wingedsheep.gameserver.session.GameSession.ActionResult.Success) {
                "Declare blockers should succeed: $declareResult"
            }

            withClue("Undo must be unavailable once real blockers are declared") {
                session.isUndoAvailable(game.player2Id) shouldBe false
            }
            withClue("Attempting to undo anyway should fail") {
                val undo = session.executeUndo(game.player2Id)
                (undo is com.wingedsheep.gameserver.session.GameSession.ActionResult.Failure) shouldBe true
            }
        }
    }

    private fun newSession(game: TestGame): GameSession {
        val session = GameSession(cardRegistry = cardRegistry)
        val ws1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
        val ws2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
        session.injectStateForTesting(
            game.state,
            mapOf(
                game.player1Id to PlayerSession(ws1, game.player1Id, "Player1"),
                game.player2Id to PlayerSession(ws2, game.player2Id, "Player2")
            )
        )
        return session
    }
}
