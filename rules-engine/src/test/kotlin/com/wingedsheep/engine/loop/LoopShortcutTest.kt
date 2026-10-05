package com.wingedsheep.engine.loop

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * [LoopShortcut] and the battlefield token cap on [LoopEngine] (+ Impact Tremors): one iteration is
 * played by hand through the processor, then the loop must be found, described and repeated.
 */
class LoopShortcutTest : ScenarioTestBase() {

    private val ability = LoopEngine.activatedAbilities.single().id
    private val shortcut = LoopShortcut(actionProcessor)

    init {
        cardRegistry.register(LoopEngine)
    }

    private inner class Recorder(val game: TestGame) {
        val history = ArrayList<LoopStep>()

        fun act(action: GameAction) {
            val result = actionProcessor.process(game.state, action).result
            withClue("${action::class.simpleName} rejected: ${result.error}") { result.error shouldBe null }
            history += LoopStep(game.state, action)
            game.state = result.state
        }

        /** Pass until [player] holds priority over an empty stack again. */
        fun settle(player: EntityId) {
            repeat(100) {
                val s = game.state
                if (s.gameOver) return
                check(s.pendingDecision == null) { "unexpected decision ${s.pendingDecision}" }
                if (s.priorityPlayerId == player && s.stack.isEmpty()) return
                act(PassPriority(s.priorityPlayerId!!))
            }
            error("did not settle")
        }

        fun oneIteration(engine: EntityId) {
            act(ActivateAbility(game.player1Id, engine, ability))
            settle(game.player1Id)
        }
    }

    private fun board(opponentLife: Int = 20, tokens: Int = 0, tremors: Boolean = true): TestGame {
        var b = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Loop Engine")
            .withCardInHand(1, "Mountain")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withLifeTotal(2, opponentLife)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (tremors) b = b.withCardOnBattlefield(1, "Impact Tremors")
        repeat(tokens) { i -> b = b.withCardOnBattlefield(1 + i % 2, "Grizzly Bears", isToken = true) }
        return b.build()
    }

    private fun TestGame.tokens(): Int = state.getBattlefield().count { state.getEntity(it)?.has<TokenComponent>() == true }

    init {
        test("a hand-played iteration is found, knows how many repetitions win, and stops at game over") {
            val game = board(opponentLife = 6)
            val rec = Recorder(game)
            val engine = game.findPermanent("Loop Engine")!!
            rec.oneIteration(engine)
            game.state.lifeTotal(game.player2Id) shouldBe 5

            val loop = shortcut.detect(rec.history, game.state, game.player1Id).shouldNotBeNull()
            loop.delta.perPlayer.getValue(game.player2Id).life shouldBe -1
            loop.delta.perPlayer.getValue(game.player1Id).tokens shouldBe 1
            loop.iterationsToWin shouldBe 5
            loop.maxIterations shouldBe GameLimits.MAX_TOKENS_ON_BATTLEFIELD - 1

            val run = shortcut.run(loop, game.state, 5)
            run.stop shouldBe LoopStop.GAME_OVER
            run.iterations shouldBe 5
            run.state.winnerId shouldBe game.player1Id
        }

        test("without a payoff the loop still repeats, and a land drop is not a loop") {
            val game = board(tremors = false)
            val rec = Recorder(game)
            rec.oneIteration(game.findPermanent("Loop Engine")!!)
            val loop = shortcut.detect(rec.history, game.state, game.player1Id).shouldNotBeNull()
            loop.iterationsToWin.shouldBeNull()
            val run = shortcut.run(loop, game.state, 10)
            run.stop shouldBe LoopStop.COMPLETED
            game.state = run.state
            game.tokens() shouldBe 11

            val land = Recorder(board(tremors = false))
            land.act(PlayLand(land.game.player1Id, land.game.state.getHand(land.game.player1Id).single()))
            shortcut.detect(land.history, land.game.state, land.game.player1Id).shouldBeNull()
        }

        test("a loop offered on a state the game has moved past is refused") {
            val game = board()
            val rec = Recorder(game)
            val engine = game.findPermanent("Loop Engine")!!
            rec.oneIteration(engine)
            val loop = shortcut.detect(rec.history, game.state, game.player1Id).shouldNotBeNull()
            rec.oneIteration(engine)
            val run = shortcut.run(loop, game.state, 3)
            run.stop shouldBe LoopStop.FAILED
            run.state shouldBe game.state
        }

        test("the battlefield token cap: one below it a token is made, at it none is") {
            val below = board(tokens = GameLimits.MAX_TOKENS_ON_BATTLEFIELD - 1, tremors = false)
            Recorder(below).oneIteration(below.findPermanent("Loop Engine")!!)
            below.tokens() shouldBe GameLimits.MAX_TOKENS_ON_BATTLEFIELD

            val at = board(tokens = GameLimits.MAX_TOKENS_ON_BATTLEFIELD, tremors = false)
            Recorder(at).oneIteration(at.findPermanent("Loop Engine")!!)
            at.tokens() shouldBe GameLimits.MAX_TOKENS_ON_BATTLEFIELD
            at.state.stack.isEmpty() shouldBe true
        }
    }
}
