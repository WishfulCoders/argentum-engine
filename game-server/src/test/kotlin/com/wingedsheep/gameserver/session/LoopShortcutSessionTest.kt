package com.wingedsheep.gameserver.session

import com.wingedsheep.ai.AiPlayerController
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.loop.LoopStop
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.ai.AiWebSocketSession
import com.wingedsheep.mtg.sets.definitions.chk.cards.KikiJikiMirrorBreaker
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * The session side of loop shortcuts (MTR 4.4): a person who plays one iteration of a loop against
 * the AI is offered it on their state updates, and repeating it applies every action through the
 * engine, rolls the replay log forward, and can be taken back as one decision.
 */
class LoopShortcutSessionTest : ScenarioTestBase() {

    private val kikiAbility = KikiJikiMirrorBreaker.activatedAbilities.single().id

    init {
        test("one hand-played Kiki-Jiki + Pestermite + Impact Tremors iteration is offered, and repeating it wins") {
            val game = kikiBoard(opponentLife = 6)
            val session = newSession(game, opponentIsAi = true)
            val p1 = game.player1Id
            val p2 = game.player2Id

            playOneIteration(session, game)
            life(session, p2) shouldBe 5

            val offer = session.loopOfferInfoFor(p1).shouldNotBeNull()
            offer.label shouldBe "Activate Kiki-Jiki, Mirror Breaker"
            offer.perIteration shouldContain "Player2: life −1"
            offer.perIteration shouldContain "You: tokens +1"
            offer.iterationsToWin shouldBe 5
            session.loopOfferInfoFor(p2).shouldBeNull()

            val actionsBefore = session.getRecordedActions().size
            val result = session.executeLoopShortcut(p1, offer.iterationsToWin!!)
                .shouldBeInstanceOf<GameSession.LoopShortcutResult.Success>()
            result.iterations shouldBe 5
            result.stop shouldBe LoopStop.GAME_OVER
            session.isGameOver() shouldBe true
            session.getStateForTesting()!!.winnerId shouldBe p1
            // Every repeated action is in the replay log, the last iteration cut short by the win.
            val replayed = session.getRecordedActions().size - actionsBefore
            (replayed > 4 * offer.actionsPerIteration && replayed <= 5 * offer.actionsPerIteration) shouldBe true
        }

        test("a repeat is taken back as one decision, and the loop must be played again to be offered") {
            val game = kikiBoard(opponentLife = 20)
            val session = newSession(game, opponentIsAi = true)
            val p1 = game.player1Id

            playOneIteration(session, game)
            val before = session.getStateForTesting()!!
            val actionsBefore = session.getRecordedActions().size
            session.loopOfferInfoFor(p1).shouldNotBeNull()

            session.executeLoopShortcut(p1, 3).shouldBeInstanceOf<GameSession.LoopShortcutResult.Success>()
            tokens(session) shouldBe 4
            session.loopOfferInfoFor(p1).shouldBeNull()
            session.takebackLabelFor(p1) shouldBe "Repeat loop ×3"

            session.executeTakeback(p1).shouldBeInstanceOf<GameSession.ActionResult.Success>()
            session.getStateForTesting() shouldBe before
            session.getRecordedActions().size shouldBe actionsBefore
            tokens(session) shouldBe 1
        }

        test("no offer against a person (they may shorten the loop, and nothing asks them yet)") {
            val game = kikiBoard(opponentLife = 20)
            val session = newSession(game, opponentIsAi = false)
            playOneIteration(session, game)
            session.loopOfferInfoFor(game.player1Id).shouldBeNull()
            session.executeLoopShortcut(game.player1Id, 3)
                .shouldBeInstanceOf<GameSession.LoopShortcutResult.Failure>()
        }
    }

    private fun kikiBoard(opponentLife: Int): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Kiki-Jiki, Mirror Breaker")
        .withCardOnBattlefield(1, "Pestermite")
        .withCardOnBattlefield(1, "Impact Tremors")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withLifeTotal(2, opponentLife)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Activate Kiki-Jiki on Pestermite and see it through: both players pass, the copy untaps Kiki-Jiki. */
    private fun playOneIteration(session: GameSession, game: TestGame) {
        val p1 = game.player1Id
        val kiki = game.findPermanent("Kiki-Jiki, Mirror Breaker")!!
        val pestermite = game.findPermanent("Pestermite")!!
        session.executeAction(
            p1, ActivateAbility(p1, kiki, kikiAbility, targets = listOf(ChosenTarget.Permanent(pestermite))),
            recordTakeback = true,
        ).shouldBeInstanceOf<GameSession.ActionResult.Success>()
        repeat(100) {
            val s = session.getStateForTesting()!!
            if (s.gameOver) return
            val d = s.pendingDecision
            if (d != null) {
                session.executeAction(d.playerId, SubmitDecision(d.playerId, untap(d, kiki)), recordTakeback = d.playerId == p1)
            } else if (s.priorityPlayerId == p1 && s.stack.isEmpty()) {
                return
            } else {
                val who = s.priorityPlayerId!!
                session.executeAction(who, PassPriority(who))
            }
        }
        error("the iteration did not settle")
    }

    private fun untap(d: PendingDecision, kiki: EntityId): DecisionResponse = when (d) {
        is ChooseTargetsDecision -> TargetsResponse(d.id, mapOf(0 to listOf(kiki)))
        is YesNoDecision -> YesNoResponse(d.id, true)
        is ChooseOptionDecision -> OptionChosenResponse(d.id, d.options.indexOfFirst { it.startsWith("Untap") })
        is OrderObjectsDecision -> OrderedResponse(d.id, d.objects)
        else -> error("unexpected decision ${d::class.simpleName}")
    }

    private fun life(session: GameSession, player: EntityId): Int =
        session.getStateForTesting()!!.getEntity(player)!!.get<LifeTotalComponent>()!!.life

    private fun tokens(session: GameSession): Int {
        val s = session.getStateForTesting()!!
        return s.getBattlefield().count { s.getEntity(it)?.has<TokenComponent>() == true }
    }

    private fun newSession(game: TestGame, opponentIsAi: Boolean): GameSession {
        val session = GameSession(cardRegistry = cardRegistry)
        val ws1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
        val ws2: WebSocketSession = if (opponentIsAi) {
            AiWebSocketSession(
                aiPlayerId = game.player2Id,
                controller = mockk<AiPlayerController>(relaxed = true),
                thinkingDelayMs = 0,
                onActionReady = { _, _, _ -> },
                onMulliganKeep = {},
                onMulliganTake = {},
                onBottomCards = { _, _ -> },
            )
        } else {
            mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
        }
        session.injectStateForTesting(
            game.state,
            mapOf(
                game.player1Id to PlayerSession(ws1, game.player1Id, "Player1"),
                game.player2Id to PlayerSession(ws2, game.player2Id, "Player2"),
            ),
        )
        return session
    }
}
