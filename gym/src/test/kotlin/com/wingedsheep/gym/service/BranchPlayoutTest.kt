package com.wingedsheep.gym.service

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.contract.Observation
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * The primitive a paired branch label is made of: fork a state, take one action in the branch, and
 * let a fixed player finish it inside the engine.
 *
 * Two properties carry the whole experiment. **Isolation**: playing a branch out must not move the
 * env it was forked from, or the generating trajectory would be destroyed by measuring it.
 * **Reproducibility**: two branches that do the same thing must end the same way, or the difference
 * between two branches that do *different* things is not attributable to the action.
 */
private val Observation.asGame: TrainingObservation get() = this as TrainingObservation

class BranchPlayoutTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }

    fun deck() = DeckSpec.Explicit(mapOf("Mountain" to 17, "Raging Goblin" to 3))

    /** Both seats answer to a pilot: the learner's, through its `decisionProfile`. */
    fun pilotAnchored(seed: Long) = EnvConfig(
        players = listOf(
            PlayerSpec(
                name = "Learner",
                deck = deck(),
                agent = AgentSpec.Learner(decisionProfile = "current"),
            ),
            PlayerSpec(name = "Pilot", deck = deck(), agent = AgentSpec.Pilot("current")),
        ),
        skipMulligans = true,
        startingPlayerIndex = 0,
        perspectivePlayerIndex = 0,
        seed = seed,
    )

    fun outcome(svc: MultiEnvService, envId: EnvId): List<Pair<String, Double>> =
        svc.status(envId).reward.map { it.playerId.value to it.value }.sortedBy { it.first }

    test("a playout finishes the episode without returning a learner decision") {
        val svc = MultiEnvService(registry())
        val created = svc.create(pilotAnchored(seed = 11L))

        svc.playout(created.envId)

        val status = svc.status(created.envId)
        status.terminated.shouldBeTrue()
        status.truncated.shouldBeFalse()
        // Somebody won: a playout is a decided game, not an abandoned one.
        status.reward.map { it.value }.sorted() shouldBe listOf(-1.0, 1.0)
        // The learner's own actions were taken for it, and counted separately from the pilot's.
        status.playedOut shouldBeGreaterThan 0
    }

    test("a bounded playout advances the trajectory and stops on a learner decision") {
        val svc = MultiEnvService(registry())
        val created = svc.create(pilotAnchored(seed = 12L))
        val before = svc.status(created.envId).playedOut

        val observation = svc.playout(created.envId, maxLearnerActions = 1).observation.asGame

        svc.status(created.envId).playedOut shouldBe before + 1
        if (!observation.terminated) {
            // Still the learner's move, so a sampler can fork here on the next pass.
            observation.agentToAct shouldBe observation.perspectivePlayerId
        }
    }

    test("two branches that play out identically end identically, and the source does not move") {
        val svc = MultiEnvService(registry())
        val created = svc.create(pilotAnchored(seed = 13L))
        // Walk a few pilot decisions in so the fork point is a real midgame state.
        svc.playout(created.envId, maxLearnerActions = 4)

        val sourceStep = svc.status(created.envId).stepCount
        val (left, right) = svc.fork(created.envId, 2)

        svc.playout(left)
        svc.playout(right)

        // Reproducible: the deal is fixed in the forked state, so a matched branch is matched.
        outcome(svc, left) shouldBe outcome(svc, right)
        svc.status(left).stepCount shouldBe svc.status(right).stepCount

        // Isolated: measuring the branches left the generating game exactly where it was.
        svc.status(created.envId).stepCount shouldBe sourceStep
        svc.status(created.envId).done.shouldBeFalse()
    }

    test("branches that take different first actions still both reach a decided terminal") {
        val svc = MultiEnvService(registry())
        val created = svc.create(pilotAnchored(seed = 14L))
        svc.playout(created.envId, maxLearnerActions = 3)

        val legal = svc.observe(created.envId).registry.legalActions
        val pass = legal.first { (_, la) -> la.action is PassPriority }.first
        val other = legal.firstOrNull { (id, la) -> id != pass && la.action !is PassPriority }?.first

        val (branchA, branchB) = svc.fork(created.envId, 2)
        svc.step(StepRequest(branchA, pass))
        svc.step(StepRequest(branchB, other ?: pass))
        svc.playout(branchA)
        svc.playout(branchB)

        // Whatever the actions were worth, a label needs both sides to have an outcome at all.
        svc.status(branchA).terminated.shouldBeTrue()
        svc.status(branchB).terminated.shouldBeTrue()
        svc.status(branchA).reward.map { it.value }.sorted() shouldBe listOf(-1.0, 1.0)
        svc.status(branchB).reward.map { it.value }.sorted() shouldBe listOf(-1.0, 1.0)
    }
})
