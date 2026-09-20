package com.wingedsheep.gym.contract

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.gym.service.AgentSpec
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ConvokePayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PolicyActionBoundaryTest : FunSpec({
    test("unsupported choices remain in order but are marked unaffordable") {
        val player = EntityId("player")
        val source = EntityId("source")
        val ability = ActivateAbility(player, source, AbilityId("ability"))
        val actions = listOf(
            LegalAction(PassPriority(player), "PassPriority", "Pass"),
            LegalAction(
                ability, "ActivateAbility", "Sacrifice this",
                additionalCostInfo = AdditionalCostData(
                    "Sacrifice this", "SacrificeSelf", validSacrificeTargets = listOf(source),
                ),
            ),
            LegalAction(
                ability, "ActivateAbility", "Blight one",
                additionalCostInfo = AdditionalCostData("Blight one", "Blight"),
            ),
        )

        val masked = PolicyActionBoundary.mask(actions)
        masked.map { it.action } shouldBe actions.map { it.action }
        masked.map { it.affordable } shouldBe listOf(true, true, false)
        val registry = ActionRegistry.ofLegalActions(masked)
        (registry.resolve(2) as ResolvedAction.Legal).legalAction.affordable shouldBe false
    }

    test("convoke is callable only on an ordinary non-X cast payable with mana alone") {
        val player = EntityId("player")
        val base = LegalAction(
            CastSpell(player, EntityId("card")), "CastSpell", "Cast convoke spell",
            hasConvoke = true,
        )
        PolicyActionBoundary.callable(base) shouldBe false
        PolicyActionBoundary.callable(base.copy(canPayWithoutConvoke = true)) shouldBe true
        PolicyActionBoundary.callable(
            base.copy(canPayWithoutConvoke = true, hasXCost = true)
        ) shouldBe false
        PolicyActionBoundary.callable(
            base.copy(canPayWithoutConvoke = true, actionType = "CastSpellMode")
        ) shouldBe false
    }

    test("pure mana kicker remains callable while choice-bearing kicker stays masked") {
        val player = EntityId("player")
        val kicked = LegalAction(
            CastSpell(player, EntityId("card"), declaredCostSlot = ChoiceSlot.KICKED),
            "CastWithKicker", "Cast kicked spell",
        )
        PolicyActionBoundary.callable(kicked) shouldBe true
        PolicyActionBoundary.callable(kicked.copy(
            additionalCostInfo = AdditionalCostData("Discard a card", "Discard"),
        )) shouldBe false
    }

    test("explicit convoke payments survive the gym step contract without altering other choices") {
        val cards = CardRegistry().apply {
            register(PortalSet.cards)
            register(PortalSet.basicLands)
        }
        val environment = GameEnvironment.create(cards)
        val deck = Deck.of("Mountain" to 17, "Raging Goblin" to 3)
        environment.reset(GameConfig(
            players = listOf(PlayerConfig("One", deck), PlayerConfig("Two", deck)),
            skipMulligans = true, startingPlayerIndex = 0, seed = 3819L,
        ))
        val player = environment.playerIds[0]
        val creature = EntityId("creature")
        val payments = mapOf(creature to ConvokePayment(Color.RED))
        val params = ActionParams(convokePayments = payments)
        Json.decodeFromString<ActionParams>(Json.encodeToString(params)) shouldBe params

        val cast = CastSpell(player, EntityId("card"), declaredCostSlot = ChoiceSlot.KICKED)
        val completed = ActionParameterizer.apply(cast, params, environment.state) as CastSpell
        completed.declaredCostSlot shouldBe ChoiceSlot.KICKED
        completed.alternativePayment?.convokedCreatures shouldBe payments
        (ActionParameterizer.apply(cast, ActionParams.EMPTY, environment.state) as CastSpell)
            .alternativePayment shouldBe null
        shouldThrow<IllegalArgumentException> {
            ActionParameterizer.apply(
                ActivateAbility(player, creature, AbilityId("ability")), params, environment.state,
            )
        }
    }

    test("each exposed convoke payment is accepted by the engine on its paired state") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Plains" to 20),
            skipMulligans = true,
        )
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putLandOnBattlefield(player, "Plains")
        repeat(4) { driver.putLandOnBattlefield(player, "Forest") }
        val creature = driver.putCreatureOnBattlefield(player, "Sun-Dappled Celebrant")
        val spell = driver.putCardInHand(player, "Sun-Dappled Celebrant")
        val simulator = GameSimulator(driver.cardRegistry)
        val legal = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
        val bare = legal.first { (it.action as? CastSpell)?.cardId == spell }
        bare.canPayWithoutConvoke shouldBe false
        PolicyActionBoundary.callable(bare) shouldBe false

        val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
        val cast = masked.first { (it.action as? CastSpell)?.cardId == spell }
        cast.affordable shouldBe true
        cast.policyConvokePaymentOptions.isNotEmpty() shouldBe true
        cast.policyConvokePaymentOptions.forEach { payment ->
            payment shouldBe mapOf(creature to ConvokePayment(Color.WHITE))
            val completed = ActionParameterizer.apply(
                cast.action, ActionParams(convokePayments = payment), driver.state,
            )
            simulator.accepts(driver.state, completed) shouldBe true
        }
        val view = ObservationBuilder(driver.cardRegistry).build(driver.state, player, masked)
            .observation as TrainingObservation
        val option = view.legalActions.first { it.sourceEntityId == spell }
        option.convokePaymentOptions shouldBe cast.policyConvokePaymentOptions
        option.validConvokeCreatures.single().entityId shouldBe creature

        val environment = GameEnvironment.create(driver.cardRegistry)
        environment.restore(driver.state, listOf(driver.player1, driver.player2))
        val gym = GameGymEnv(
            environment, perspectivePlayerIndex = 0, defaultRevealAll = false,
            agents = listOf(AgentSpec.Learner(), AgentSpec.Learner()),
        )
        val gymView = gym.observe().observation as TrainingObservation
        gymView.legalActions shouldBe view.legalActions
        gym.step(option.actionId, ActionParams(convokePayments = option.convokePaymentOptions.first()))
        environment.lastRejection shouldBe null
    }

    test("learner gym action views match the arena-style mask entry by entry") {
        val cards = CardRegistry().apply {
            register(PortalSet.cards)
            register(PortalSet.basicLands)
        }
        val deck = Deck.of("Mountain" to 17, "Raging Goblin" to 3)
        val environment = GameEnvironment.create(cards)
        environment.reset(GameConfig(
            players = listOf(PlayerConfig("One", deck), PlayerConfig("Two", deck)),
            skipMulligans = true, startingPlayerIndex = 0, seed = 3819L,
        ))
        val gym = GameGymEnv(
            environment, perspectivePlayerIndex = 0, defaultRevealAll = false,
            agents = listOf(AgentSpec.Learner(), AgentSpec.Learner()),
        )
        val builder = ObservationBuilder(cards)

        repeat(24) {
            if (gym.isTerminal) return@repeat
            val actor = environment.agentToAct ?: return@repeat
            val arenaStyle = builder.build(
                environment.state, environment.playerIds[0],
                PolicyActionBoundary.mask(environment.legalActions()), false,
            )
            val gymView = gym.observe()
            (gymView.observation as TrainingObservation).legalActions shouldBe
                (arenaStyle.observation as TrainingObservation).legalActions
            gymView.registry.legalActions.map { it.first to it.second.affordable } shouldBe
                arenaStyle.registry.legalActions.map { it.first to it.second.affordable }

            val step = gymView.registry.legalActions.firstOrNull { it.second.action is PassPriority }
                ?: gymView.registry.legalActions.firstOrNull { it.second.affordable }
                ?: return@repeat
            step.second.action.playerId shouldBe actor
            gym.step(step.first)
        }
    }
})
