package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.core.ModesChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.loop.LoopShortcut
import com.wingedsheep.engine.loop.LoopStep
import com.wingedsheep.engine.loop.LoopStop
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.KikiJikiMirrorBreaker
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Loop shortcuts (MTR 4.4) on the classic Kiki-Jiki + Pestermite loop: Kiki-Jiki copies Pestermite,
 * the copy's enters trigger untaps Kiki-Jiki, repeat. One iteration is played by hand, then
 * [LoopShortcut] must find it, say what it does, and repeat it through the real engine.
 */
class LoopShortcutScenarioTest : ScenarioTestBase() {

    private val kikiAbility = KikiJikiMirrorBreaker.activatedAbilities.single().id
    private val shortcut = LoopShortcut(actionProcessor)

    /** A game plus the per-action history a server would keep. */
    private inner class Recorder(val game: TestGame) {
        val history = ArrayList<LoopStep>()

        fun act(action: GameAction) {
            val result = actionProcessor.process(game.state, action).result
            withClue("${action::class.simpleName} rejected: ${(result.outcome as? Outcome.Rejected)?.reason}") {
                (result.outcome is Outcome.Rejected) shouldBe false
            }
            history += LoopStep(game.state, action)
            game.state = result.state
        }

        /** Pass and answer until [player] holds priority over an empty stack again. */
        fun settle(player: EntityId, answer: (PendingDecision) -> DecisionResponse) {
            var guard = 0
            while (true) {
                check(guard++ < 100) { "did not settle" }
                val s = game.state
                if (s.gameOver) return
                val d = s.pendingDecision
                if (d != null) { act(SubmitDecision(d.playerId, answer(d))); continue }
                if (s.priorityPlayerId == player && s.stack.isEmpty()) return
                act(PassPriority(s.priorityPlayerId!!))
            }
        }
    }

    /** Aim every trigger at [target], say yes, untap, and keep any order offered. */
    private fun untapping(target: EntityId): (PendingDecision) -> DecisionResponse = { d ->
        when (d) {
            is ChooseTargetsDecision -> TargetsResponse(d.id, mapOf(0 to listOf(target)))
            is YesNoDecision -> YesNoResponse(d.id, true)
            is ChooseModeDecision -> ModesChosenResponse(d.id, listOf(d.modes.single { it.text.startsWith("Untap") }.index))
            is ChooseOptionDecision -> OptionChosenResponse(d.id, d.options.indexOfFirst { it.startsWith("Untap") })
            is OrderObjectsDecision -> OrderedResponse(d.id, d.objects)
            else -> error("unexpected decision ${d::class.simpleName}")
        }
    }

    private fun kikiBoard(extra: List<String> = emptyList(), opponentLife: Int = 20): TestGame {
        var b = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Kiki-Jiki, Mirror Breaker")
            .withCardOnBattlefield(1, "Pestermite")
            .withCardInHand(1, "Mountain")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withLifeTotal(2, opponentLife)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (name in extra) b = b.withCardOnBattlefield(1, name)
        return b.build()
    }

    private fun Recorder.oneIteration(kiki: EntityId, pestermite: EntityId) {
        act(ActivateAbility(game.player1Id, kiki, kikiAbility, targets = listOf(ChosenTarget.Permanent(pestermite))))
        settle(game.player1Id, untapping(kiki))
    }

    private fun TestGame.tokens(): Int = state.getBattlefield().count { state.getEntity(it)?.has<TokenComponent>() == true }

    private fun TestGame.life(player: EntityId): Int = state.getEntity(player)!!.get<LifeTotalComponent>()!!.life

    init {
        context("loop shortcut") {

            test("finds Kiki-Jiki + Pestermite after one iteration and repeats it through the engine") {
                val game = kikiBoard()
                val rec = Recorder(game)
                val kiki = game.findPermanent("Kiki-Jiki, Mirror Breaker")!!
                val pestermite = game.findPermanent("Pestermite")!!

                rec.oneIteration(kiki, pestermite)
                withClue("one hand-played iteration: one token, Kiki-Jiki untapped again") {
                    game.tokens() shouldBe 1
                    game.state.getEntity(kiki)!!.has<TappedComponent>() shouldBe false
                }

                val loop = shortcut.detect(rec.history, game.state, game.player1Id).shouldNotBeNull()
                withClue("the loop is the activation; one iteration makes one token and wins nothing") {
                    loop.playerActions.map { it::class.simpleName }.first() shouldBe "ActivateAbility"
                    loop.delta.perPlayer.getValue(game.player1Id).tokens shouldBe 1
                    loop.delta.perPlayer.getValue(game.player1Id).permanents shouldBe 1
                    loop.delta.perPlayer.getValue(game.player2Id).isZero shouldBe true
                    loop.iterationsToWin.shouldBeNull()
                    loop.maxIterations shouldBe GameLimits.MAX_TOKENS_ON_BATTLEFIELD - 1
                }

                val run = shortcut.run(loop, game.state, 20)
                run.stop shouldBe LoopStop.COMPLETED
                run.iterations shouldBe 20
                game.state = run.state
                withClue("twenty more Pestermite tokens, each a real copy that entered and triggered") {
                    game.tokens() shouldBe 21
                    game.findPermanents("Pestermite").size shouldBe 22
                    game.state.getEntity(kiki)!!.has<TappedComponent>() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
            }

            test("with Impact Tremors the loop knows how many iterations win, and stops when the game ends") {
                val game = kikiBoard(extra = listOf("Impact Tremors"), opponentLife = 6)
                val rec = Recorder(game)
                val kiki = game.findPermanent("Kiki-Jiki, Mirror Breaker")!!
                val pestermite = game.findPermanent("Pestermite")!!

                rec.oneIteration(kiki, pestermite)
                game.life(game.player2Id) shouldBe 5

                val loop = shortcut.detect(rec.history, game.state, game.player1Id).shouldNotBeNull()
                loop.delta.perPlayer.getValue(game.player2Id).life shouldBe -1
                loop.iterationsToWin shouldBe 5

                val run = shortcut.run(loop, game.state, loop.iterationsToWin!!)
                run.stop shouldBe LoopStop.GAME_OVER
                run.iterations shouldBe 5
                run.state.gameOver shouldBe true
                run.state.winnerId shouldBe game.player1Id
            }

            test("a sequence that does not come back to where it started is not a loop") {
                val game = kikiBoard()
                val rec = Recorder(game)
                val mountain = game.state.getHand(game.player1Id).single()
                rec.act(PlayLand(game.player1Id, mountain))
                rec.settle(game.player1Id, untapping(mountain))
                shortcut.detect(rec.history, game.state, game.player1Id).shouldBeNull()
            }

            test("the loop is refused once the game has moved on") {
                val game = kikiBoard()
                val rec = Recorder(game)
                val kiki = game.findPermanent("Kiki-Jiki, Mirror Breaker")!!
                val pestermite = game.findPermanent("Pestermite")!!
                rec.oneIteration(kiki, pestermite)
                val loop = shortcut.detect(rec.history, game.state, game.player1Id).shouldNotBeNull()

                rec.oneIteration(kiki, pestermite)
                val run = shortcut.run(loop, game.state, 3)
                run.stop shouldBe LoopStop.FAILED
                run.iterations shouldBe 0
                run.state shouldBe game.state
            }
        }
    }
}
