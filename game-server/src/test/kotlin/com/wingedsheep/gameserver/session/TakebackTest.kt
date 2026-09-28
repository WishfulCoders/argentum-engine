package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Take-backs rewind a player's own decisions within the current turn — including ones the engine's
 * strict undo refuses (a resolved spell, a real attack) and everything that happened since.
 */
class TakebackTest : ScenarioTestBase() {

    init {
        test("a resolved spell is taken back: card in hand, lands untapped, its target alive, replay rolled back") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Shock")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val session = newSession(game)
            val p1 = game.player1Id
            val p2 = game.player2Id
            val before = session.getStateForTesting()!!
            val actionsBefore = session.getRecordedActions().size

            val shock = game.findCardsInHand(1, "Shock").single()
            val bears = game.findPermanent("Grizzly Bears")!!
            session.executeAction(p1, CastSpell(p1, shock, listOf(ChosenTarget.Permanent(bears))), recordTakeback = true)
                .shouldBeInstanceOf<GameSession.ActionResult.Success>()
            session.executeAction(p1, PassPriority(p1), recordTakeback = true)
            session.executeAction(p2, PassPriority(p2))
            onBattlefield(session, 2, "Grizzly Bears") shouldBe false

            session.isUndoAvailable(p1) shouldBe false
            session.takebackLabelFor(p1) shouldBe "Cast Shock"
            session.takebackLabelFor(p2).shouldBeNull()

            session.executeTakeback(p1).shouldBeInstanceOf<GameSession.ActionResult.Success>()
            session.getStateForTesting() shouldBe before
            session.getRecordedActions().size shouldBe actionsBefore
            onBattlefield(session, 2, "Grizzly Bears") shouldBe true
            session.takebackLabelFor(p1).shouldBeNull()
        }

        test("a real attack is taken back to the declare-attackers step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val session = newSession(game)
            val p1 = game.player1Id
            val bears = game.findPermanent("Grizzly Bears")!!

            session.executeAction(p1, DeclareAttackers(p1, mapOf(bears to game.player2Id)), recordTakeback = true)
                .shouldBeInstanceOf<GameSession.ActionResult.Success>()
            session.isUndoAvailable(p1) shouldBe false
            session.takebackLabelFor(p1) shouldBe "Attack"

            session.executeTakeback(p1).shouldBeInstanceOf<GameSession.ActionResult.Success>()
            val restored = session.getStateForTesting()!!
            restored.step shouldBe Step.DECLARE_ATTACKERS
            restored.priorityPlayerId shouldBe p1
        }

        test("an action not flagged as a person's leaves no point") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val session = newSession(game)
            val p1 = game.player1Id
            val shock = game.findCardsInHand(1, "Shock").single()
            session.executeAction(p1, CastSpell(p1, shock, listOf(ChosenTarget.Player(game.player2Id))))
            session.takebackLabelFor(p1).shouldBeNull()
        }

        test("points expire when the turn changes") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Shock")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.ENDING, Step.END)
                .build()
            val session = newSession(game)
            val p1 = game.player1Id
            val p2 = game.player2Id
            val turn = session.getStateForTesting()!!.turnNumber
            val shock = game.findCardsInHand(1, "Shock").single()
            session.executeAction(p1, CastSpell(p1, shock, listOf(ChosenTarget.Player(p2))), recordTakeback = true)
            session.takebackLabelFor(p1) shouldBe "Cast Shock"

            repeat(40) {
                val s = session.getStateForTesting()!!
                if (s.turnNumber != turn) return@repeat
                s.priorityPlayerId?.let { session.executeAutoPass(it) }
            }
            session.getStateForTesting()!!.turnNumber shouldBe turn + 1
            session.takebackLabelFor(p1).shouldBeNull()
            session.executeTakeback(p1).shouldBeInstanceOf<GameSession.ActionResult.Failure>()
        }

        test("a request waiting on the opponent is voided when anyone acts") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val session = newSession(game)
            val p1 = game.player1Id
            val p2 = game.player2Id
            val shock = game.findCardsInHand(1, "Shock").single()
            session.executeAction(p1, CastSpell(p1, shock, listOf(ChosenTarget.Player(p2))), recordTakeback = true)

            val pending = session.openTakebackRequest(p1).shouldNotBeNull()
            pending.label shouldBe "Cast Shock"
            // While it is out, the requester is not offered another, and the opponent is asked.
            session.takebackLabelFor(p1).shouldBeNull()
            session.takebackRequestFor(p2)?.label shouldBe "Cast Shock"
            session.takebackRequestFor(p2)?.requesterName shouldBe "Player1"
            session.takebackRequestFor(p1).shouldBeNull()
            // The requester cannot answer their own request.
            session.answerTakebackRequest(p1).shouldBeNull()

            session.executeAction(p1, PassPriority(p1))
            session.takebackRequestFor(p2).shouldBeNull()
            session.answerTakebackRequest(p2).shouldBeNull()

            // A fresh request, answered before anyone acts, goes through.
            session.openTakebackRequest(p1).shouldNotBeNull()
            session.answerTakebackRequest(p2).shouldNotBeNull()
        }
    }

    private var playerIds: List<EntityId> = emptyList()

    private fun onBattlefield(session: GameSession, player: Int, name: String): Boolean {
        val state = session.getStateForTesting()!!
        val owner = playerIds[player - 1]
        return state.zones[ZoneKey(owner, Zone.BATTLEFIELD)].orEmpty()
            .any { state.getEntity(it)?.get<CardComponent>()?.name == name }
    }

    private fun newSession(game: TestGame): GameSession {
        playerIds = listOf(game.player1Id, game.player2Id)
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
