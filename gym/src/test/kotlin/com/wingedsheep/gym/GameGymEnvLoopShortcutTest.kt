package com.wingedsheep.gym

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.gym.contract.SchemaHash
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * The gym's loop-shortcut macro action (env option `loopShortcuts`, schema v1.14): after the agent
 * plays one iteration of a loop through ordinary gym steps, its legal actions gain `RepeatLoop`
 * entries, and stepping one repeats the loop through the same env step the iteration was played
 * with. The loop is a test card — "{0}: create a 1/1 token" — with Impact Tremors as the payoff, so
 * each repetition makes a token and deals 1 damage to the opponent.
 */
class GameGymEnvLoopShortcutTest : ScenarioTestBase() {

    private val loopEngine = card("Loop Engine") {
        manaCost = "{3}"
        typeLine = "Artifact"
        oracleText = "{0}: Create a 1/1 colorless Servo artifact creature token."
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.CreateToken(power = 1, toughness = 1, colors = emptySet(), creatureTypes = setOf("Servo"))
        }
    }

    init {
        cardRegistry.register(loopEngine)
    }

    private fun gymEnv(opponentLife: Int, loopShortcuts: Boolean = true): Triple<GameGymEnv, EntityId, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Loop Engine")
            .withCardOnBattlefield(1, "Impact Tremors")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withLifeTotal(2, opponentLife)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val environment = GameEnvironment.create(cardRegistry)
        environment.restore(game.state, listOf(game.player1Id, game.player2Id))
        val env = GameGymEnv(environment, perspectivePlayerIndex = 0, defaultRevealAll = true, loopShortcuts = loopShortcuts)
        return Triple(env, game.player1Id, game.player2Id)
    }

    private fun GameGymEnv.obs(): TrainingObservation = observe().observation as TrainingObservation

    private fun GameGymEnv.engine(): EntityId =
        environment.state.getBattlefield().first {
            environment.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Loop Engine"
        }

    /** One iteration through gym calls only: the activation by its action ID. */
    private fun GameGymEnv.playOneIteration() {
        val engine = engine()
        val activate = obs().legalActions.single { it.kind == "ActivateAbility" && it.sourceEntityId == engine }
        step(activate.actionId, ActionParams.EMPTY)
        environment.state.pendingDecision shouldBe null
    }

    init {
        test("the schema hash names the loop shortcut") {
            SchemaHash.CURRENT shouldBe "argentum-gym-contract@v1.14-loop-shortcut"
        }

        test("after one hand-played iteration the agent is offered RepeatLoop ×1, ×to-win and ×max") {
            val (env, me, opponent) = gymEnv(opponentLife = 6)
            env.obs().legalActions.filter { it.kind == "RepeatLoop" }.shouldBeEmpty()

            env.playOneIteration()
            val obs = env.obs()
            obs.players.single { it.id == opponent }.lifeTotal shouldBe 5
            val loops = obs.legalActions.filter { it.kind == "RepeatLoop" }
            loops.map { it.loopIterations } shouldContainExactly listOf(1, 5, loops.last().loopIterations)
            loops.map { it.loopWins } shouldContainExactly listOf(false, true, false)
            val win = loops[1]
            win.loopDelta["opponent.life"] shouldBe -1
            win.loopDelta["self.tokens"] shouldBe 1
            win.sourceEntityId shouldBe env.engine()
            win.affordable shouldBe true
            // Appended after the ordinary actions, with fresh IDs.
            loops.map { it.actionId } shouldBe loops.indices.map { obs.legalActions.size - loops.size + it }

            val stepsBefore = env.status().stepCount
            env.step(win.actionId, ActionParams.EMPTY)
            withClue("five more iterations through the env: the opponent is dead") {
                env.isTerminal shouldBe true
                env.environment.state.winnerId shouldBe me
            }
            val status = env.status()
            status.loopRepeats shouldBe 1
            status.loopIterations shouldBe 5
            status.loopStoppedEarly shouldBe 0
            status.stepCount shouldBe stepsBefore + 1
        }

        test("repeating once leaves a playable game, and the loop must be played again to be offered") {
            val (env, _, opponent) = gymEnv(opponentLife = 20)
            env.playOneIteration()
            val once = env.obs().legalActions.first { it.kind == "RepeatLoop" && it.loopIterations == 1 }
            env.step(once.actionId, ActionParams.EMPTY)
            val obs = env.obs()
            obs.players.single { it.id == opponent }.lifeTotal shouldBe 18
            obs.legalActions.filter { it.kind == "RepeatLoop" }.shouldBeEmpty()
            env.playOneIteration()
            env.obs().legalActions.filter { it.kind == "RepeatLoop" }.isNotEmpty() shouldBe true
        }

        test("off by default: no RepeatLoop entries, and a forked env keeps its history") {
            val (off, _, _) = gymEnv(opponentLife = 20, loopShortcuts = false)
            off.playOneIteration()
            off.obs().legalActions.filter { it.kind == "RepeatLoop" }.shouldBeEmpty()

            val (on, _, _) = gymEnv(opponentLife = 20)
            on.playOneIteration()
            val fork = on.fork() as GameGymEnv
            (fork.observe().observation as TrainingObservation).legalActions
                .filter { it.kind == "RepeatLoop" }.isNotEmpty() shouldBe true
        }
    }
}
