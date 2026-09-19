package com.wingedsheep.gym.service

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.gym.contract.Observation
import com.wingedsheep.gym.contract.ResolvedAction
import com.wingedsheep.gym.contract.StateDigest
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

/**
 * Seeds, truncation and the episode envelope — the three things a training loop needs from an env
 * and none of which the stepped gym had. A rollout that cannot be replayed cannot be debugged, and
 * an env with no bound does not return at all when a policy stops passing.
 */
private val Observation.asGame: TrainingObservation get() = this as TrainingObservation

class EnvEpisodeTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }

    fun deck() = DeckSpec.Explicit(mapOf("Mountain" to 17, "Raging Goblin" to 3))

    fun config(seed: Long? = null, limits: EnvLimits = EnvLimits()) = EnvConfig(
        players = listOf(
            PlayerSpec(name = "Alice", deck = deck()),
            PlayerSpec(name = "Bob", deck = deck()),
        ),
        skipMulligans = true,
        startingPlayerIndex = 0,
        seed = seed,
        limits = limits,
    )

    /** Pass priority, the one action always available to whoever is being asked. */
    fun pass(svc: MultiEnvService, envId: EnvId, observation: com.wingedsheep.gym.contract.ObservationResult) {
        val entry = observation.registry.legalActions.first { (_, la) -> la.action is PassPriority }
        svc.step(StepRequest(envId, entry.first))
    }

    // =========================================================================
    // Seeds
    // =========================================================================

    test("an explicit seed is reported back and replays the same opening") {
        val svc = MultiEnvService(registry())
        val first = svc.create(config(seed = 4_242L))
        val second = svc.create(config(seed = 4_242L))

        svc.status(first.envId).seed shouldBe 4_242L
        svc.status(second.envId).seed shouldBe 4_242L
        StateDigest.compute(second.observation.observation.asGame) shouldBe
            StateDigest.compute(first.observation.observation.asGame)
    }

    test("a different seed deals a different game, and an unseeded env still reports what it drew") {
        val svc = MultiEnvService(registry())
        val seeded = svc.create(config(seed = 1L))
        val other = svc.create(config(seed = 2L))
        StateDigest.compute(other.observation.observation.asGame) shouldNotBe
            StateDigest.compute(seeded.observation.observation.asGame)

        // Fresh entropy is the default; the drawn seed is still recorded, so the game is replayable.
        val drawn = svc.status(svc.create(config()).envId).seed
        drawn.shouldNotBeNull()
        val replayed = svc.create(config(seed = drawn))
        svc.status(replayed.envId).seed shouldBe drawn
    }

    // =========================================================================
    // The envelope
    // =========================================================================

    test("a running episode reports no reward and is not done") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(seed = 7L))
        val status = svc.status(created.envId)

        status.terminated.shouldBeFalse()
        status.truncated.shouldBeFalse()
        status.done.shouldBeFalse()
        status.reward.shouldBeEmpty()
        status.truncationReason.shouldBeNull()
    }

    test("a decided game pays the winner +1 and the loser -1") {
        // The heuristic AI plays both seats, so this reaches a real terminal state quickly.
        val svc = MultiEnvService(registry())
        val cards = svc.deckResolver.resolve(deck())
        val environment = GameEnvironment.create(registry())
        environment.playGame(
            GameConfig(
                players = listOf(
                    PlayerConfig(name = "Alice", deck = cards),
                    PlayerConfig(name = "Bob", deck = cards),
                ),
                skipMulligans = true,
                startingPlayerIndex = 0,
                seed = 11L,
            )
        )

        val status = GameGymEnv(environment, 0, false).status()
        status.terminated.shouldBeTrue()
        status.done.shouldBeTrue()
        status.reward shouldHaveSize 2
        status.reward.map { it.value }.sum() shouldBe 0.0
        status.reward.maxOf { it.value } shouldBeGreaterThan 0.0
        status.seed shouldBe 11L
    }

    // =========================================================================
    // Truncation
    // =========================================================================

    test("an env that cannot pass its turn truncates, pays nothing, and refuses to step again") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(seed = 3L, limits = EnvLimits(maxActionsWithoutProgress = 4)))

        var observation = created.observation
        repeat(12) {
            if (!svc.status(created.envId).done) {
                observation = svc.observe(created.envId)
                pass(svc, created.envId, observation)
            }
        }

        val status = svc.status(created.envId)
        status.truncated.shouldBeTrue()
        status.terminated.shouldBeFalse()
        status.done.shouldBeTrue()
        // Truncation decides nothing, so it pays nothing: it is not a win and not a loss.
        status.reward.shouldBeEmpty()
        status.truncationReason.shouldNotBeNull().shouldStartWith("stuck(")

        val entry = svc.observe(created.envId).registry.legalActions
            .first { (_, la) -> la.action is PassPriority }
        shouldThrow<IllegalStateException> { svc.step(StepRequest(created.envId, entry.first)) }
    }

    test("the turn cap truncates a game that runs long") {
        val svc = MultiEnvService(registry())
        val created = svc.create(
            config(seed = 5L, limits = EnvLimits(maxTurnsPerSeat = 1, maxActionsWithoutProgress = 10_000)),
        )

        repeat(60) {
            if (!svc.status(created.envId).done) {
                pass(svc, created.envId, svc.observe(created.envId))
            }
        }

        val status = svc.status(created.envId)
        status.truncated.shouldBeTrue()
        status.truncationReason shouldBe "maxTurns(1)"
    }

    test("reset clears truncation and re-seeds the episode") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(seed = 3L, limits = EnvLimits(maxActionsWithoutProgress = 4)))

        repeat(12) {
            if (!svc.status(created.envId).done) {
                pass(svc, created.envId, svc.observe(created.envId))
            }
        }
        svc.status(created.envId).truncated.shouldBeTrue()

        svc.reset(created.envId, config(seed = 99L))
        val status = svc.status(created.envId)
        status.truncated.shouldBeFalse()
        status.truncationReason.shouldBeNull()
        status.seed shouldBe 99L
        status.stepCount shouldBe 0
    }

    test("limits must be positive") {
        shouldThrow<IllegalArgumentException> { EnvLimits(maxTurnsPerSeat = 0) }
        shouldThrow<IllegalArgumentException> { EnvLimits(maxActions = 0) }
        shouldThrow<IllegalArgumentException> { EnvLimits(maxActionsWithoutProgress = 0) }
    }

    // =========================================================================
    // Fork
    // =========================================================================

    test("a fork inherits the seed and the limits it was forked under") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(seed = 17L))
        val child = svc.fork(created.envId, 1).single()

        svc.status(child).seed shouldBe 17L
        svc.status(child).truncated.shouldBeFalse()
    }
})
