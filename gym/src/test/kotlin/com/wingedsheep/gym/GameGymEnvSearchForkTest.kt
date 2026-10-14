package com.wingedsheep.gym

import com.wingedsheep.ai.engine.evaluation.CardValueTable
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.contract.ActionParams
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.gym.service.AgentSpec
import com.wingedsheep.gym.service.DeckResolver
import com.wingedsheep.gym.service.DeckSpec
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * mtg-draft-ai docs/65 §3 item 1: what the in-process search relies on. A [GameGymEnv.forkForSearch] fork must
 * step exactly as a [GameGymEnv.fork] does (it skips the observation build and shares the simulator, nothing
 * else), and [GameGymEnv.determinize] with a viewer must keep that seat's hidden cards and resample the other's.
 */
class GameGymEnvSearchForkTest : FunSpec({

    val registry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }
    val spells = PortalSet.cards.map { it.name }.filter { it !in setOf("Plains", "Island", "Swamp", "Mountain", "Forest") }
        .distinct().take(12)
    val deck = DeckSpec.Explicit(spells.associateWith { 2 } + mapOf("Mountain" to 8, "Forest" to 8))

    /** Two learner seats: the env stops at whichever acts, so both hands are someone's hidden cards. */
    fun env(seed: Long): GameGymEnv {
        val env = GameGymEnv(
            GameEnvironment.create(registry), 0, false,
            agents = listOf(AgentSpec.Learner(decisionProfile = "current"), AgentSpec.Learner(decisionProfile = "current")),
        )
        val decks = DeckResolver(registry)
        env.reset(
            GameConfig(
                players = listOf(PlayerConfig("A", decks.resolve(deck)), PlayerConfig("B", decks.resolve(deck))),
                skipMulligans = true, startingPlayerIndex = 0, seed = seed,
            ),
        )
        return env
    }

    fun hand(env: GameGymEnv, seat: Int): List<String> {
        val state = env.environment.state
        return state.getHand(env.environment.playerIds[seat]).mapNotNull { CardValueTable.name(state, it) }.sorted()
    }

    test("a search fork steps exactly as a fork does, and neither moves the source") {
        val source = env(seed = 21L)
        var obs = source.observe(null).observation as TrainingObservation
        repeat(12) { i ->
            if (source.isTerminal) return@repeat
            // A parameter-free action, the same one in both forks: pass every other step.
            val action = obs.legalActions.firstOrNull { it.kind == "PassPriority" && i % 2 == 0 }
                ?: obs.legalActions.first { it.affordable && it.targetRequirements.isEmpty() && !it.hasXCost }
            val before = source.environment.state
            val plain = (source.fork() as GameGymEnv).step(action.actionId, ActionParams.EMPTY).observation
            val search = source.forkForSearch().step(action.actionId, ActionParams.EMPTY).observation
            search shouldBe plain
            (source.environment.state === before) shouldBe true
            obs = source.step(action.actionId, ActionParams.EMPTY).observation as TrainingObservation
        }
    }

    test("determinize with a viewer keeps that seat's hand and resamples the other seat's") {
        val source = env(seed = 22L)
        for (viewer in 0..1) {
            val other = 1 - viewer
            val worlds = (1..8).map { s ->
                source.forkForSearch().also { it.determinize(1_000L + s, viewer) }
            }
            // The viewer sees its own hand, so every world keeps it.
            worlds.forEach { hand(it, viewer) shouldBe hand(source, viewer) }
            // What it cannot see is resampled: some world deals the other seat a different hand.
            worlds.count { hand(it, other) != hand(source, other) } shouldBeGreaterThan 0
        }
    }
})
