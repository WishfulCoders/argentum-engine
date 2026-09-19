package com.wingedsheep.gym.service

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.contract.Observation
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * One seat learns, the other is played by the engine's own AI inside the env.
 *
 * Without this a trainer would have to reimplement the opponent outside the engine that defines it,
 * and the policy it trained would be evaluated against a player it never met.
 */
private val Observation.asGame: TrainingObservation get() = this as TrainingObservation

class PilotSeatTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }

    fun deck() = DeckSpec.Explicit(mapOf("Mountain" to 17, "Raging Goblin" to 3))

    fun oneSided(seed: Long = 1L, limits: EnvLimits = EnvLimits()) = EnvConfig(
        players = listOf(
            PlayerSpec(name = "Learner", deck = deck(), agent = AgentSpec.Learner()),
            PlayerSpec(name = "Pilot", deck = deck(), agent = AgentSpec.Pilot("current")),
        ),
        skipMulligans = true,
        startingPlayerIndex = 0,
        perspectivePlayerIndex = 0,
        seed = seed,
        limits = limits,
    )

    /**
     * Do as little as the rules allow: pass when passing is offered, and otherwise take the first
     * legal action — a required declaration step offers no pass.
     */
    fun pass(svc: MultiEnvService, envId: EnvId) {
        val actions = svc.observe(envId).registry.legalActions
        val entry = actions.firstOrNull { (_, la) -> la.action is PassPriority } ?: actions.first()
        svc.step(StepRequest(envId, entry.first))
    }

    test("every observation the caller sees belongs to the learner seat") {
        val svc = MultiEnvService(registry())
        val created = svc.create(oneSided())
        val learner = created.observation.observation.asGame.perspectivePlayerId

        created.observation.observation.asGame.priorityPlayerId shouldBe learner
        created.observation.observation.asGame.legalActions.shouldNotBeEmpty()

        repeat(40) {
            if (!svc.status(created.envId).done) {
                pass(svc, created.envId)
                val observation = svc.observe(created.envId).observation.asGame
                if (!observation.terminated) {
                    // The pilot's priority never reaches the caller, and neither does a decision.
                    observation.priorityPlayerId shouldBe learner
                    observation.agentToAct shouldBe learner
                    observation.pendingDecision.shouldBeNull()
                }
            }
        }
    }

    test("the pilot's actions are counted, and the learner's are not") {
        val svc = MultiEnvService(registry())
        val created = svc.create(oneSided(seed = 5L))

        // Passing the learner's whole turn hands the pilot a turn of its own to play.
        repeat(12) { if (!svc.status(created.envId).done) pass(svc, created.envId) }

        svc.status(created.envId).autoAdvanced shouldBeGreaterThan 0
    }

    test("a seat nobody learns is refused, and so is observing from the pilot's") {
        shouldThrow<IllegalArgumentException> {
            EnvConfig(
                players = listOf(
                    PlayerSpec(name = "A", deck = deck(), agent = AgentSpec.Pilot()),
                    PlayerSpec(name = "B", deck = deck(), agent = AgentSpec.Pilot()),
                ),
            )
        }
        shouldThrow<IllegalArgumentException> {
            EnvConfig(
                players = listOf(
                    PlayerSpec(name = "A", deck = deck(), agent = AgentSpec.Learner()),
                    PlayerSpec(name = "B", deck = deck(), agent = AgentSpec.Pilot()),
                ),
                perspectivePlayerIndex = 1,
            )
        }
    }

    test("a learner that only ever passes loses a decided game to the pilot") {
        val svc = MultiEnvService(registry())
        val created = svc.create(oneSided(seed = 21L))
        val learner = created.observation.observation.asGame.perspectivePlayerId

        var guard = 0
        while (!svc.status(created.envId).done && guard++ < 4_000) {
            pass(svc, created.envId)
        }

        val status = svc.status(created.envId)
        status.terminated.shouldBeTrue()
        status.truncated.shouldBeFalse()
        status.reward.first { it.playerId == learner }.value shouldBe -1.0
        status.reward.first { it.playerId != learner }.value shouldBe 1.0
    }

    test("an env with no agent spec still hands every seat to the caller") {
        val svc = MultiEnvService(registry())
        val created = svc.create(
            EnvConfig(
                players = listOf(
                    PlayerSpec(name = "A", deck = deck()),
                    PlayerSpec(name = "B", deck = deck()),
                ),
                skipMulligans = true,
                startingPlayerIndex = 0,
                seed = 2L,
            )
        )

        // Both seats are learners by default, and nothing is auto-advanced for a caller that
        // has always driven the whole game itself.
        svc.status(created.envId).autoAdvanced shouldBe 0
    }

    test("reset can change which seat the pilot plays") {
        val svc = MultiEnvService(registry())
        val created = svc.create(oneSided(seed = 3L))

        val flipped = EnvConfig(
            players = listOf(
                PlayerSpec(name = "Pilot", deck = deck(), agent = AgentSpec.Pilot("current")),
                PlayerSpec(name = "Learner", deck = deck(), agent = AgentSpec.Learner()),
            ),
            skipMulligans = true,
            startingPlayerIndex = 0,
            perspectivePlayerIndex = 1,
            seed = 3L,
        )
        val opening = svc.reset(created.envId, flipped).observation.asGame

        // Seat 0 is on the play and is now the pilot, so it has already taken its turn.
        opening.perspectivePlayerId shouldBe opening.priorityPlayerId
        svc.status(created.envId).autoAdvanced shouldBeGreaterThan 0
    }
})
