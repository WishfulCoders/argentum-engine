package com.wingedsheep.gym.service

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.contract.Observation
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random

/**
 * The checks `docs/43` §3.7 puts before any training run: that a learner cannot see what it must
 * not, that a run can be replayed exactly, and that every engine action an episode takes is
 * accounted for.
 *
 * These are cheap and they fail loudly. A hidden-information leak does not announce itself — it
 * shows up months later as a policy that plays impossibly well in the gym and badly everywhere
 * else — and a rollout that cannot be replayed cannot be debugged at all.
 */
private val Observation.asGame: TrainingObservation get() = this as TrainingObservation

class EnvTrainingContractTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }

    fun deck() = DeckSpec.Explicit(mapOf("Mountain" to 17, "Raging Goblin" to 3))

    fun config(
        seed: Long = 1L,
        revealAll: Boolean = false,
        pilotOpponent: Boolean = true,
    ) = EnvConfig(
        players = listOf(
            PlayerSpec(name = "Learner", deck = deck()),
            PlayerSpec(
                name = "Other",
                deck = deck(),
                agent = if (pilotOpponent) AgentSpec.Pilot("current") else AgentSpec.Learner(),
            ),
        ),
        skipMulligans = true,
        startingPlayerIndex = 0,
        perspectivePlayerIndex = 0,
        seed = seed,
        revealAll = revealAll,
    )

    /** Pass when passing is offered, otherwise take the first legal action. */
    fun act(svc: MultiEnvService, envId: EnvId): Int {
        val actions = svc.observe(envId).registry.legalActions
        val entry = actions.firstOrNull { (_, la) -> la.action is PassPriority } ?: actions.first()
        svc.step(StepRequest(envId, entry.first))
        return entry.first
    }

    fun opponentZones(observation: TrainingObservation, zone: Zone) =
        observation.zones.filter { it.zoneType == zone && it.ownerId != observation.perspectivePlayerId }

    // =========================================================================
    // Hidden information
    // =========================================================================

    test("the learner never sees the opponent's hand or library, at any point in a game") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(seed = 8L))

        repeat(60) {
            if (svc.status(created.envId).done) return@repeat
            val observation = svc.observe(created.envId).observation.asGame
            for (zone in listOf(Zone.HAND, Zone.LIBRARY)) {
                for (view in opponentZones(observation, zone)) {
                    view.hidden.shouldBeTrue()
                    // A hidden zone contributes its true size and no card identities: the unknown
                    // count is what the encoder sees, never an invented card.
                    view.cards.shouldBeEmpty()
                }
            }
            act(svc, created.envId)
        }
    }

    test("the masking test would notice a leak: revealAll shows what the default hides") {
        val svc = MultiEnvService(registry())
        val hidden = svc.create(config(seed = 8L)).observation.observation.asGame
        val revealed = svc.create(config(seed = 8L, revealAll = true)).observation.observation.asGame

        opponentZones(hidden, Zone.HAND).single().cards.shouldBeEmpty()
        opponentZones(revealed, Zone.HAND).single().cards.shouldNotBeEmpty()
        // Same seed, same game: the only difference is what the perspective is allowed to know.
        opponentZones(revealed, Zone.HAND).single().size shouldBe
            opponentZones(hidden, Zone.HAND).single().size
    }

    test("a pilot seat's own play does not leak into the learner's observation") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(seed = 12L))

        // Several full turns of pilot play have happened by now.
        repeat(30) { if (!svc.status(created.envId).done) act(svc, created.envId) }
        svc.status(created.envId).autoAdvanced shouldBeGreaterThan 0

        val observation = svc.observe(created.envId).observation.asGame
        opponentZones(observation, Zone.HAND).single().cards.shouldBeEmpty()
        opponentZones(observation, Zone.LIBRARY).single().cards.shouldBeEmpty()
    }

    // =========================================================================
    // Determinism
    // =========================================================================

    test("the same seed and the same actions replay the same game, digest for digest") {
        val svc = MultiEnvService(registry())

        fun digests(seed: Long): List<String> {
            val created = svc.create(config(seed = seed))
            val trace = mutableListOf(created.observation.observation.asGame.stateDigest)
            repeat(40) {
                if (!svc.status(created.envId).done) {
                    act(svc, created.envId)
                    trace += svc.observe(created.envId).observation.asGame.stateDigest
                }
            }
            return trace
        }

        val first = digests(99L)
        first shouldBe digests(99L)
        // And a different seed is a different game, so the check above is not vacuous.
        first shouldNotBe digests(100L)
    }

    test("a random legal drive never has an action rejected") {
        val svc = MultiEnvService(registry())
        val random = Random(4)
        // Both seats belong to the caller here, so this drives the whole game through the registry.
        val created = svc.create(config(seed = 31L, pilotOpponent = false))

        repeat(400) {
            if (svc.status(created.envId).done) return@repeat
            val actions = svc.observe(created.envId).registry.legalActions
            val affordable = actions.filter { (_, la) -> la.affordable }
            val entry = (affordable.ifEmpty { actions }).random(random)
            // A rejected action throws; the engine leaves state untouched on refusal, so a
            // silent no-op here would look exactly like a legal action that changed nothing.
            svc.step(StepRequest(created.envId, entry.first))
        }
    }

    // =========================================================================
    // Accounting
    // =========================================================================

    test("every engine action is the learner's, a pilot's, or a delegated decision") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(seed = 77L))

        var learnerActions = 0
        repeat(50) {
            if (!svc.status(created.envId).done) {
                act(svc, created.envId)
                learnerActions++
            }
        }

        val status = svc.status(created.envId)
        // stepCount is every action the engine took; autoAdvanced is everything the env did on its
        // own. Nothing may happen in an episode that neither number explains.
        status.stepCount shouldBe learnerActions + status.autoAdvanced
        // Delegated decisions are a subset of what the env advanced, never a separate tally.
        (status.delegatedDecisions <= status.autoAdvanced).shouldBeTrue()
    }

    test("an all-learner env advances only the decisions it answers on the caller's behalf") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(seed = 78L, pilotOpponent = false))

        var actions = 0
        repeat(30) {
            if (!svc.status(created.envId).done) {
                act(svc, created.envId)
                actions++
            }
        }

        val status = svc.status(created.envId)
        // Naming both seats Learner does not make the caller responsible for structured decisions:
        // a Learner acts on priority, and its targets, orderings and damage assignments are still
        // answered by its decisionProfile — the arena's contract, and the one the policy was
        // screened under. So everything advanced here is a delegated decision and nothing else.
        status.autoAdvanced shouldBe status.delegatedDecisions
        status.stepCount shouldBe actions + status.autoAdvanced
        status.truncated.shouldBeFalse()
    }
})
