package com.wingedsheep.gym

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/** A gym game with a mulligan phase is playable through the flat action space (CR 103.5). */
class GameGymEnvMulliganTest : FunSpec({
    fun registry(): CardRegistry = CardRegistry().apply {
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }

    fun gymEnv(skipMulligans: Boolean): GameGymEnv {
        val environment = GameEnvironment.create(registry())
        val gym = GameGymEnv(environment, perspectivePlayerIndex = 0, defaultRevealAll = false)
        gym.reset(
            GameConfig(
                players = listOf(
                    PlayerConfig("Alice", Deck.of("Mountain" to 10, "Forest" to 10)),
                    PlayerConfig("Bob", Deck.of("Mountain" to 10, "Forest" to 10))
                ),
                skipMulligans = skipMulligans,
                startingPlayerIndex = 0,
                seed = 20261019L
            )
        )
        return gym
    }

    /** Observe as the seat that acts and take its first action of [kind]. */
    fun GameGymEnv.take(kind: String): TrainingObservation {
        val actor = environment.agentToAct!!
        val obs = observeForPlayer(actor).observation as TrainingObservation
        obs.agentToAct shouldBe actor
        val id = obs.legalActions.first { it.kind == kind }.actionId
        return step(id, com.wingedsheep.engine.core.ActionParams()).let {
            observeForPlayer(environment.agentToAct ?: actor).observation as TrainingObservation
        }
    }

    test("the first observation offers keep and mulligan to the starting player") {
        val gym = gymEnv(skipMulligans = false)
        val alice = gym.environment.playerIds[0]
        val obs = gym.observe().observation as TrainingObservation

        obs.agentToAct shouldBe alice
        obs.legalActions.map { it.kind } shouldContainExactlyInAnyOrder listOf("KeepHand", "TakeMulligan")
    }

    test("mulligan, keep, bottom one card and the other keep reach turn 1's upkeep") {
        val gym = gymEnv(skipMulligans = false)
        val (alice, bob) = gym.environment.playerIds

        gym.take("TakeMulligan")
        gym.environment.agentToAct shouldBe alice
        val bottoming = gym.take("KeepHand")

        bottoming.agentToAct shouldBe alice
        bottoming.legalActions.shouldNotBeEmpty()
        bottoming.legalActions.forEach {
            it.kind shouldBe "BottomCards"
            it.targetEntityIds shouldHaveSize 1
        }
        gym.take("BottomCards")
        gym.environment.agentToAct shouldBe bob
        val upkeep = gym.take("KeepHand")

        upkeep.turnNumber shouldBe 1
        upkeep.step shouldBe Step.UPKEEP
        upkeep.agentToAct shouldBe alice
        gym.environment.state.getHand(alice).size shouldBe 6
    }

    test("with skipped mulligans the first priority is the starting player's, in upkeep") {
        val gym = gymEnv(skipMulligans = true)
        val obs = gym.observe().observation as TrainingObservation

        obs.turnNumber shouldBe 1
        obs.step shouldBe Step.UPKEEP
        obs.agentToAct shouldBe gym.environment.playerIds[0]
    }
})
