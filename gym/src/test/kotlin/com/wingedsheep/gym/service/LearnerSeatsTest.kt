package com.wingedsheep.gym.service

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.contract.Observation
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val Observation.asGame: TrainingObservation get() = this as TrainingObservation

/**
 * Two learner seats in one env (mtg-draft-ai `docs/51` §4): each decision is observed from the seat
 * that has it, and `status.actingSeat` says which seat that is.
 *
 * Observing from a fixed seat would hand the second learner its opponent's hand and the wrong seat's
 * legal actions, which is exactly the information a self-play policy must not see.
 */
class LearnerSeatsTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }

    fun deck() = DeckSpec.Explicit(mapOf("Mountain" to 17, "Raging Goblin" to 3))

    fun config(second: AgentSpec, seed: Long = 7L) = EnvConfig(
        players = listOf(
            PlayerSpec(name = "A", deck = deck(), agent = AgentSpec.Learner()),
            PlayerSpec(name = "B", deck = deck(), agent = second),
        ),
        skipMulligans = true,
        startingPlayerIndex = 0,
        perspectivePlayerIndex = 0,
        seed = seed,
    )

    fun pass(svc: MultiEnvService, envId: EnvId) {
        val actions = svc.observe(envId).registry.legalActions
        val entry = actions.firstOrNull { (_, la) -> la.action is PassPriority } ?: actions.first()
        svc.step(StepRequest(envId, entry.first))
    }

    test("each learner decision is observed from its own seat, and only its own hand is visible") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(AgentSpec.Learner()))
        val seen = mutableSetOf<Int>()
        var guard = 0
        while (!svc.status(created.envId).done && guard++ < 4_000) {
            val status = svc.status(created.envId)
            val observation = svc.observe(created.envId).observation.asGame
            val seat = status.actingSeat!!
            seen += seat
            observation.perspectivePlayerId shouldBe observation.agentToAct
            observation.players.first { it.isPerspective }.id shouldBe observation.agentToAct
            // The other seat's hand is a hidden zone with no identities in it.
            val otherHand = observation.zones.first {
                it.zoneType == Zone.HAND && it.ownerId != observation.perspectivePlayerId
            }
            otherHand.hidden.shouldBeTrue()
            otherHand.cards.shouldBeEmpty()
            pass(svc, created.envId)
        }
        seen shouldContainExactlyInAnyOrder setOf(0, 1)
        val status = svc.status(created.envId)
        status.terminated.shouldBeTrue()
        status.reward.map { it.value }.sorted() shouldBe listOf(-1.0, 1.0)
    }

    test("with one learner seat the acting seat is always the perspective seat") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(AgentSpec.Pilot("current"), seed = 9L))
        var guard = 0
        while (!svc.status(created.envId).done && guard++ < 200) {
            svc.status(created.envId).actingSeat shouldBe 0
            pass(svc, created.envId)
        }
    }

    test("both learners' observations carry distinct perspectives within one game") {
        val svc = MultiEnvService(registry())
        val created = svc.create(config(AgentSpec.Learner(), seed = 11L))
        val perspectives = mutableSetOf<Any>()
        var guard = 0
        while (!svc.status(created.envId).done && guard++ < 400) {
            perspectives += svc.observe(created.envId).observation.asGame.perspectivePlayerId
            pass(svc, created.envId)
        }
        perspectives.size shouldBe 2
        perspectives.first() shouldNotBe perspectives.last()
    }
})
