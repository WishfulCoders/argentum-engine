package com.wingedsheep.gym.service

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.ValueLabel
import com.wingedsheep.gym.ValueLabelRequest
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The value-self-play label (mtg-draft-ai `docs/50`): the pilot's own scored candidates, each played
 * out in shared determinized worlds. What the fit relies on is pinned here: the pass is always
 * labelled, the pilot's choice is marked, every candidate has one outcome per world, labelling does
 * not move the env, and the same request gives the same label.
 */
class ValueLabelTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }

    fun deck() = DeckSpec.Explicit(mapOf("Mountain" to 16, "Raging Goblin" to 4, "Minotaur Warrior" to 4, "Scorching Spear" to 4))

    fun anchored(seed: Long) = EnvConfig(
        players = listOf(
            PlayerSpec(name = "Learner", deck = deck(), agent = AgentSpec.Learner(decisionProfile = "current")),
            PlayerSpec(name = "Pilot", deck = deck(), agent = AgentSpec.Pilot("current")),
        ),
        skipMulligans = true,
        startingPlayerIndex = 0,
        perspectivePlayerIndex = 0,
        seed = seed,
    )

    val worlds = listOf(1L, 2L, 3L, 4L)

    /** Walk the pilot's own trajectory to the first decision it scores two or more candidates at. */
    fun firstLabel(svc: MultiEnvService, env: EnvId, request: ValueLabelRequest): ValueLabel {
        repeat(80) {
            svc.valueLabel(env, request)?.let { return it }
            if (svc.playout(env, maxLearnerActions = 1).observation.let { (it as com.wingedsheep.gym.contract.TrainingObservation).terminated }) {
                error("episode ended before a scored decision")
            }
        }
        error("no scored decision in 80 pilot actions")
    }

    test("every labelled candidate has one outcome per world, the pass among them") {
        val svc = MultiEnvService(registry())
        val env = svc.create(anchored(seed = 21L)).envId
        val label = firstLabel(svc, env, ValueLabelRequest(worldSeeds = worlds))

        label.features shouldHaveSize 26
        label.candidates.first().type shouldBe "PassPriority"
        label.pilotIndex shouldBeGreaterThanOrEqual 0
        label.pilotIndex shouldBeLessThanOrEqual label.candidates.lastIndex
        for (c in label.candidates) {
            c.f shouldHaveSize 26
            c.roll.n shouldBe worlds.size
            c.roll.outcomes.all { it in "WLU" } shouldBe true
            c.roll.illegal shouldBe 0
        }
        label.branches shouldBe label.candidates.size * worlds.size
    }

    test("labelling does not move the env, and the same request gives the same label") {
        val svc = MultiEnvService(registry())
        val env = svc.create(anchored(seed = 22L)).envId
        val request = ValueLabelRequest(worldSeeds = worlds)
        val first = firstLabel(svc, env, request)
        val stepsAfter = svc.status(env).stepCount

        val again = svc.valueLabel(env, request).shouldNotBeNull()

        svc.status(env).stepCount shouldBe stepsAfter
        again.candidates.map { it.roll.outcomes } shouldBe first.candidates.map { it.roll.outcomes }
        again.candidates.map { it.f } shouldBe first.candidates.map { it.f }
        again.pilotIndex shouldBe first.pilotIndex
    }

    test("a cap keeps the pass and the pilot's choice") {
        val svc = MultiEnvService(registry())
        val env = svc.create(anchored(seed = 23L)).envId
        val label = firstLabel(svc, env, ValueLabelRequest(worldSeeds = listOf(5L), maxCandidates = 2))

        label.candidates.size shouldBeLessThanOrEqual 2
        label.candidates.first().type shouldBe "PassPriority"
        label.pilotIndex shouldBeGreaterThanOrEqual 0
    }
})
