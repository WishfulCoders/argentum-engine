package com.wingedsheep.gym.contract

import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.gym.service.AgentSpec
import com.wingedsheep.gym.service.SnapshotCodec
import com.wingedsheep.gym.service.SnapshotHandle
import com.wingedsheep.mtg.sets.definitions.blb.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PolicyActionParityCorpusTest : FunSpec({
    test("saved pilot and mechanic states expose identical arena and learner gym actions") {
        val portalCards = CardRegistry().apply {
            register(PortalSet.cards)
            register(PortalSet.basicLands)
        }
        val bloomburrowCards = CardRegistry().apply {
            register(BloomburrowSet.cards)
            register(BloomburrowSet.basicLands)
        }
        val codec = SnapshotCodec()
        val saved = mutableListOf<Pair<CardRegistry, SnapshotHandle>>()

        // Collect priority positions after actual pilot choices, including both players' turns.
        fun collectTrajectory(cards: CardRegistry, deck: Deck, seeds: List<Long>) = seeds.forEach { seed ->
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
                    saved += cards to gym.snapshot(codec)
                    gym.playout(maxLearnerActions = 1)
                }
            }
        }
        collectTrajectory(
            portalCards, Deck.of("Mountain" to 17, "Raging Goblin" to 3),
            listOf(3819L, 73L, 1204L),
        )
        val portalPositions = saved.size
        collectTrajectory(
            bloomburrowCards,
            Deck.of("Forest" to 12, "Plains" to 8, "Valley Mightcaller" to 10, "Brightblade Stoat" to 10),
            listOf(609L, 2024L),
        )
        val bloomburrowPositions = saved.size - portalPositions
        val curatedStart = saved.size

        // Save small, deliberately constructed positions for choice-bearing mechanics that the
        // pilot trajectories are unlikely to visit often enough to guard by themselves.
        fun driver(deck: Deck) = GameTestDriver().apply {
            registerCards(TestCards.all)
            initMirrorMatch(deck, skipMulligans = true)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        fun save(driver: GameTestDriver) {
            saved += driver.cardRegistry to codec.save(
                driver.state, listOf(driver.player1, driver.player2), 0,
            )
        }
        val convoke = driver(Deck.of("Forest" to 20, "Plains" to 20))
        val convokePlayer = convoke.activePlayer!!
        repeat(4) { convoke.putLandOnBattlefield(convokePlayer, "Forest") }
        convoke.putLandOnBattlefield(convokePlayer, "Plains")
        convoke.putCreatureOnBattlefield(convokePlayer, "Sun-Dappled Celebrant")
        convoke.putCardInHand(convokePlayer, "Sun-Dappled Celebrant")
        convoke.putCardInHand(convokePlayer, "Pawpatch Formation")
        val equipment = convoke.putPermanentOnBattlefield(convokePlayer, "Bonesplitter")
        convoke.putCreatureOnBattlefield(convokePlayer, "Grizzly Bears")
        save(convoke)

        val blight = driver(Deck.of("Mountain" to 40))
        val blightPlayer = blight.activePlayer!!
        val glutton = blight.putCreatureOnBattlefield(blightPlayer, "Gristle Glutton")
        blight.putCreatureOnBattlefield(blightPlayer, "Grizzly Bears")
        blight.putCreatureOnBattlefield(blight.getOpponent(blightPlayer), "Grizzly Bears")
        blight.removeSummoningSickness(glutton)
        repeat(3) { blight.putLandOnBattlefield(blightPlayer, "Mountain") }
        blight.putCardInHand(blightPlayer, "Lightning Bolt")
        blight.putCardInHand(blightPlayer, "Blaze")
        save(blight)

        val tap = driver(Deck.of("Plains" to 40))
        val tapPlayer = tap.activePlayer!!
        val gemguard = tap.putCreatureOnBattlefield(tapPlayer, "Adaptive Gemguard")
        val tapFirst = tap.putCreatureOnBattlefield(tapPlayer, "Grizzly Bears")
        val tapSecond = tap.putCreatureOnBattlefield(tapPlayer, "Grizzly Bears")
        listOf(gemguard, tapFirst, tapSecond).forEach(tap::removeSummoningSickness)
        save(tap)

        val behold = driver(Deck.of("Mountain" to 40))
        val beholdPlayer = behold.activePlayer!!
        behold.putCreatureOnBattlefield(behold.getOpponent(beholdPlayer), "Grizzly Bears")
        behold.putCardInHand(beholdPlayer, "Ebon Dragon")
        behold.putCardInHand(beholdPlayer, "Molten Exhale")
        behold.passPriorityUntil(Step.END)
        behold.giveMana(beholdPlayer, Color.RED, 2)
        save(behold)

        val covered = mutableSetOf<String>()
        try {
            portalPositions shouldBe 60
            (bloomburrowPositions >= 30) shouldBe true
            (saved.size - curatedStart) shouldBe 4
            saved.forEachIndexed { index, (cards, handle) ->
                val enumerator = LegalActionEnumerator.create(cards)
                val simulator = GameSimulator(cards)
                val builder = ObservationBuilder(cards)
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
                arenaActions.filter { it.affordable }.forEach { action ->
                    fun preflight(params: ActionParams) {
                        val complete = ActionParameterizer.apply(action.action, params, restored.state)
                        check(PolicyActionStager(simulator).begin(restored.state, complete) != null) {
                            "Saved position $index cannot preflight ${action.actionType}: ${action.description} with $params"
                        }
                    }
                    if (action.action !is PassPriority) covered += "nonpass"
                    if (action.policyConvokePaymentOptions.isNotEmpty()) covered += "convoke"
                    if (action.policyBlightTargetOptions.isNotEmpty()) covered += "blight"
                    if (action.policyTapPaymentOptions.isNotEmpty()) covered += "tap"
                    if (action.policyBeholdPaymentOptions.isNotEmpty()) covered += "behold"
                    if ((action.action as? ActivateAbility)?.sourceId == equipment) covered += "equipment"
                    if (action.requiresTargets) covered += "target"
                    if (action.hasXCost) covered += "x"
                    if (action.actionType == "CastSpellMode" || action.modalEnumeration != null) covered += "modal"
                    if (action.action is PassPriority || action.action is PlayLand ||
                        action.action is CastSpell || action.action is ActivateAbility
                    ) {
                        if (!action.requiresTargets && !action.hasXCost && !action.requiresDamageDistribution &&
                            action.policyConvokePaymentOptions.isEmpty() &&
                            action.policyBlightTargetOptions.isEmpty() && action.policyTapPaymentOptions.isEmpty() &&
                            action.policyBeholdPaymentOptions.isEmpty() && action.modalEnumeration == null
                        ) {
                            preflight(ActionParams.EMPTY)
                        }
                    }
                    if (index >= curatedStart) {
                        action.policyConvokePaymentOptions.forEach { preflight(ActionParams(convokePayments = it)) }
                        action.policyBlightTargetOptions.forEach { preflight(ActionParams(blightTarget = it)) }
                        action.policyTapPaymentOptions.forEach { preflight(ActionParams(tappedPermanents = it)) }
                        action.policyBeholdPaymentOptions.forEach { (target, payments) ->
                            payments.forEach { preflight(ActionParams(targets = listOf(target), beheldCards = it)) }
                        }
                        if (action.requiresTargets && action.minTargets == 1 && action.targetCount == 1 &&
                            action.additionalCostInfo == null && !action.hasXCost
                        ) {
                            action.validTargets.orEmpty().firstOrNull()?.let {
                                preflight(ActionParams(targets = listOf(it)))
                            }
                        }
                    }
                }
            }
            covered shouldBe setOf(
                "nonpass", "convoke", "blight", "tap", "behold", "equipment", "target", "x", "modal",
            )
        } finally {
            saved.forEach { (_, handle) -> codec.dispose(handle) }
            codec.size() shouldBe 0
        }
    }
})
