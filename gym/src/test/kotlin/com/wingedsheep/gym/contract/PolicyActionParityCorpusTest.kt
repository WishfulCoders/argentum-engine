package com.wingedsheep.gym.contract

import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.gym.service.AgentSpec
import com.wingedsheep.gym.service.SnapshotCodec
import com.wingedsheep.gym.service.SnapshotHandle
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PolicyActionParityCorpusTest : FunSpec({
    test("saved pilot-play states expose identical arena and learner gym actions") {
        val cards = CardRegistry().apply {
            register(PortalSet.cards)
            register(PortalSet.basicLands)
        }
        val deck = Deck.of("Mountain" to 17, "Raging Goblin" to 3)
        val codec = SnapshotCodec()
        val saved = mutableListOf<SnapshotHandle>()

        // Collect priority positions after actual pilot choices, including both players' turns.
        listOf(3819L, 73L, 1204L).forEach { seed ->
            val environment = GameEnvironment.create(cards)
            environment.reset(GameConfig(
                players = listOf(PlayerConfig("One", deck), PlayerConfig("Two", deck)),
                skipMulligans = true, startingPlayerIndex = 0, seed = seed,
            ))
            val gym = GameGymEnv(
                environment, perspectivePlayerIndex = 0, defaultRevealAll = false,
                agents = listOf(AgentSpec.Learner(), AgentSpec.Learner()),
            )
            repeat(20) {
                if (!gym.isTerminal && environment.agentToAct != null) {
                    saved += gym.snapshot(codec)
                    gym.playout(maxLearnerActions = 1)
                }
            }
        }

        val enumerator = LegalActionEnumerator.create(cards)
        val simulator = GameSimulator(cards)
        val builder = ObservationBuilder(cards)
        var substantive = 0
        try {
            saved.size shouldBe 60
            saved.forEach { handle ->
                val restored = GameEnvironment.create(cards)
                val gym = GameGymEnv(
                    restored, perspectivePlayerIndex = 0, defaultRevealAll = false,
                    agents = listOf(AgentSpec.Learner(), AgentSpec.Learner()),
                )
                val gymView = gym.restore(codec, handle)
                val actor = requireNotNull(restored.agentToAct)
                val arenaActions = PolicyActionBoundary.mask(
                    enumerator.enumerate(restored.state, actor, EnumerationMode.ACTIONS_ONLY),
                    restored.state, simulator,
                )
                val arenaView = builder.build(
                    restored.state, restored.playerIds[0], arenaActions, false,
                )
                (gymView.observation as TrainingObservation).legalActions shouldBe
                    (arenaView.observation as TrainingObservation).legalActions
                gymView.registry.legalActions shouldBe arenaView.registry.legalActions
                substantive += arenaActions.count { it.affordable && it.action !is PassPriority }
            }
            (substantive > 0) shouldBe true
        } finally {
            saved.forEach(codec::dispose)
            codec.size() shouldBe 0
        }
    }
})
