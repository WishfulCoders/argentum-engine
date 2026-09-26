package com.wingedsheep.gym.contract

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.gym.service.AgentSpec
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.mtg.sets.definitions.ecl.cards.GristleGlutton
import com.wingedsheep.mtg.sets.definitions.lci.cards.AdaptiveGemguard
import com.wingedsheep.mtg.sets.definitions.tdm.cards.MoltenExhale
import com.wingedsheep.mtg.sets.definitions.sos.cards.AjanisResponse
import com.wingedsheep.mtg.sets.definitions.sos.cards.GroupProject
import com.wingedsheep.mtg.sets.definitions.sos.cards.RubbleRouser
import com.wingedsheep.mtg.sets.definitions.sos.cards.ShatteredAcolyte
import com.wingedsheep.mtg.sets.definitions.sos.cards.SuspendAggression
import com.wingedsheep.mtg.sets.definitions.nph.cards.Dismember
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.dsl.card
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

    test("Treasure-only casts match arena staging and complete in the learner gym") {
        val ogre = card("Policy Treasure Ogre") {
            manaCost = "{2}{R}"
            typeLine = "Creature — Ogre"
            power = 3
            toughness = 3
        }
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ogre, PredefinedTokens.Treasure))
        driver.initMirrorMatch(Deck.of(ogre.name to 40), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(2) { driver.putLandOnBattlefield(player, "Mountain") }
        val treasure = driver.putPermanentOnBattlefield(player, "Treasure")
        driver.untapPermanent(treasure)
        val spell = driver.putCardInHand(player, ogre.name)

        val simulator = GameSimulator(driver.cardRegistry)
        val legal = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
        val cast = PolicyActionBoundary.mask(legal, driver.state, simulator)
            .first { (it.action as? CastSpell)?.cardId == spell }
        cast.affordable shouldBe true
        val completed = cast.action
        simulator.accepts(driver.state, completed) shouldBe false
        val staged = requireNotNull(simulator.floatSacrificeMana(driver.state, completed))
        staged.activations.size shouldBe 1
        staged.activations.single().sourceId shouldBe treasure

        val raw = GameEnvironment.create(driver.cardRegistry)
        raw.restore(driver.state, listOf(driver.player1, driver.player2))
        val stager = PolicyActionStager(simulator)
        stager.begin(raw.state, CastSpell(player, EntityId("absent-card"))) shouldBe null
        stager.continuePending(raw.state) shouldBe null
        (treasure in raw.state.getBattlefield()) shouldBe true
        val payment = requireNotNull(stager.begin(raw.state, completed))
        payment.staged shouldBe true
        payment.action shouldBe staged.activations.single()
        raw.stepExactlyOne(payment.action)
        raw.lastRejection shouldBe null
        val finalCast = requireNotNull(stager.continuePending(raw.state))
        finalCast.action shouldBe completed
        raw.stepExactlyOne(finalCast.action)
        raw.lastRejection shouldBe null
        stager.continuePending(raw.state) shouldBe null

        val arenaView = ObservationBuilder(driver.cardRegistry)
            .build(driver.state, player, PolicyActionBoundary.mask(legal, driver.state, simulator))
        val environment = GameEnvironment.create(driver.cardRegistry)
        environment.restore(driver.state, listOf(driver.player1, driver.player2))
        val gym = GameGymEnv(
            environment, perspectivePlayerIndex = 0, defaultRevealAll = false,
            agents = listOf(AgentSpec.Learner(), AgentSpec.Learner()),
        )
        val gymView = gym.observe()
        (gymView.observation as TrainingObservation).legalActions shouldBe
            (arenaView.observation as TrainingObservation).legalActions
        gymView.registry.legalActions shouldBe arenaView.registry.legalActions
        val actionId = gymView.observation.legalActions
            .single { it.sourceEntityId == spell && it.kind == "CastSpell" }.actionId
        gym.step(actionId)
        environment.lastRejection shouldBe null
        (treasure in raw.state.getBattlefield()) shouldBe false
        (spell in raw.state.getHand(player)) shouldBe false
        (treasure in environment.state.getBattlefield()) shouldBe false
        (spell in environment.state.getHand(player)) shouldBe false

        driver.putLandOnBattlefield(player, "Mountain")
        val direct = requireNotNull(stager.begin(driver.state, completed))
        direct.staged shouldBe false
        direct.action shouldBe completed
        stager.continuePending(driver.state) shouldBe null
    }

    test("learner gym applies the arena preflight before changing a stale episode") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val land = driver.putCardInHand(player, "Mountain")
        val environment = GameEnvironment.create(driver.cardRegistry)
        environment.restore(driver.state, listOf(driver.player1, driver.player2))
        val gym = GameGymEnv(
            environment, perspectivePlayerIndex = 0, defaultRevealAll = false,
            agents = listOf(AgentSpec.Learner(), AgentSpec.Learner()),
        )
        val view = gym.observe().observation as TrainingObservation
        val actionId = view.legalActions.single {
            it.kind == "PlayLand" && it.sourceEntityId == land
        }.actionId

        environment.stepExactlyOne(PlayLand(player, land))
        environment.lastRejection shouldBe null
        val before = environment.state
        val steps = environment.stepCount
        shouldThrow<IllegalArgumentException> { gym.step(actionId) }
        environment.state shouldBe before
        environment.stepCount shouldBe steps
        environment.lastRejection shouldBe null
    }

    test("Blight options match the arena and learner gym and pay the chosen creature") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GristleGlutton)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val glutton = driver.putCreatureOnBattlefield(player, "Gristle Glutton")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(glutton)
        val simulator = GameSimulator(driver.cardRegistry)
        val legal = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
        val bare = legal.first { (it.action as? ActivateAbility)?.sourceId == glutton }
        PolicyActionBoundary.callable(bare) shouldBe false

        val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
        val ability = masked.first { (it.action as? ActivateAbility)?.sourceId == glutton }
        ability.affordable shouldBe true
        ability.policyBlightTargetOptions shouldBe listOf(bear, glutton).sortedBy { it.value }
        ability.policyBlightTargetOptions.forEach { target ->
            val completed = ActionParameterizer.apply(
                ability.action, ActionParams(blightTarget = target), driver.state,
            )
            simulator.accepts(driver.state, completed) shouldBe true
        }
        val view = ObservationBuilder(driver.cardRegistry).build(driver.state, player, masked)
            .observation as TrainingObservation
        val option = view.legalActions.first { it.sourceEntityId == glutton && it.kind == "ActivateAbility" }
        option.blightTargetOptions shouldBe ability.policyBlightTargetOptions
        option.blightAmount shouldBe 1

        val environment = GameEnvironment.create(driver.cardRegistry)
        environment.restore(driver.state, listOf(driver.player1, driver.player2))
        val gym = GameGymEnv(
            environment, perspectivePlayerIndex = 0, defaultRevealAll = false,
            agents = listOf(AgentSpec.Learner(), AgentSpec.Learner()),
        )
        val gymView = gym.observe().observation as TrainingObservation
        gymView.legalActions shouldBe view.legalActions
        shouldThrow<IllegalArgumentException> { gym.step(option.actionId) }
        shouldThrow<IllegalArgumentException> {
            gym.step(option.actionId, ActionParams(blightTarget = EntityId("not-a-candidate")))
        }
        gym.step(option.actionId, ActionParams(blightTarget = bear))
        environment.lastRejection shouldBe null
        environment.state.getEntity(bear)?.get<CountersComponent>()
            ?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
    }

    test("bounded TapPermanents selections preflight and match the learner gym") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AdaptiveGemguard)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val gemguard = driver.putCreatureOnBattlefield(player, "Adaptive Gemguard")
        val first = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        listOf(gemguard, first, second).forEach(driver::removeSummoningSickness)
        val simulator = GameSimulator(driver.cardRegistry)
        val legal = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
        val bare = legal.first { (it.action as? ActivateAbility)?.sourceId == gemguard }
        PolicyActionBoundary.callable(bare) shouldBe false

        val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
        val ability = masked.first { (it.action as? ActivateAbility)?.sourceId == gemguard }
        ability.affordable shouldBe true
        ability.policyTapPaymentOptions.any { it.toSet() == setOf(first, second) } shouldBe true
        ability.policyTapPaymentOptions.forEach { selection ->
            selection.size shouldBe 2
            val completed = ActionParameterizer.apply(
                ability.action, ActionParams(tappedPermanents = selection), driver.state,
            )
            simulator.accepts(driver.state, completed) shouldBe true
        }
        val view = ObservationBuilder(driver.cardRegistry).build(driver.state, player, masked)
            .observation as TrainingObservation
        val option = view.legalActions.first { it.sourceEntityId == gemguard && it.kind == "ActivateAbility" }
        option.tapCount shouldBe 2
        option.tapPaymentOptions shouldBe ability.policyTapPaymentOptions

        val environment = GameEnvironment.create(driver.cardRegistry)
        environment.restore(driver.state, listOf(driver.player1, driver.player2))
        val gym = GameGymEnv(
            environment, perspectivePlayerIndex = 0, defaultRevealAll = false,
            agents = listOf(AgentSpec.Learner(), AgentSpec.Learner()),
        )
        (gym.observe().observation as TrainingObservation).legalActions shouldBe view.legalActions
        shouldThrow<IllegalArgumentException> { gym.step(option.actionId) }
        shouldThrow<IllegalArgumentException> {
            gym.step(option.actionId, ActionParams(tappedPermanents = listOf(first)))
        }
        val selection = option.tapPaymentOptions.first { it.toSet() == setOf(first, second) }
        gym.step(option.actionId, ActionParams(tappedPermanents = selection))
        environment.lastRejection shouldBe null
        environment.state.getEntity(first)?.has<TappedComponent>() shouldBe true
        environment.state.getEntity(second)?.has<TappedComponent>() shouldBe true
    }

    test("targeted Behold cast exposes only complete engine-checked target payments") {
        val dragonDef = CardDefinition.creature(
            name = "Policy Test Dragon", manaCost = ManaCost.parse("{4}{R}"),
            subtypes = setOf(Subtype.DRAGON), power = 4, toughness = 4,
        )
        val ogreDef = CardDefinition.creature(
            name = "Policy Test Ogre", manaCost = ManaCost.parse("{3}{R}"),
            subtypes = setOf(Subtype("Ogre")), power = 4, toughness = 4,
        )
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MoltenExhale)
        driver.registerCard(dragonDef)
        driver.registerCard(ogreDef)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val dragon = driver.putCreatureOnBattlefield(player, "Policy Test Dragon")
        val handDragon = driver.putCardInHand(player, "Policy Test Dragon")
        val ogre = driver.putCreatureOnBattlefield(opponent, "Policy Test Ogre")
        val spell = driver.putCardInHand(player, "Molten Exhale")
        driver.passPriorityUntil(Step.END)
        driver.giveMana(player, Color.RED, 2)
        val simulator = GameSimulator(driver.cardRegistry)
        val legal = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
        val bare = legal.first { (it.action as? CastSpell)?.cardId == spell && it.actionType == "CastWithKicker" }
        bare.additionalCostInfo?.costType shouldBe "Behold"
        PolicyActionBoundary.callable(bare) shouldBe false

        val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
        val cast = masked.first { (it.action as? CastSpell)?.cardId == spell && it.actionType == "CastWithKicker" }
        cast.affordable shouldBe true
        cast.policyBeholdPaymentOptions.keys shouldBe cast.validTargets.orEmpty().toSet()
        cast.policyBeholdPaymentOptions[ogre]?.toSet() shouldBe
            setOf(listOf(dragon), listOf(handDragon))
        cast.policyBeholdPaymentOptions.forEach { (target, payments) ->
            payments.forEach { payment ->
                val completed = ActionParameterizer.apply(
                    cast.action, ActionParams(targets = listOf(target), beheldCards = payment),
                    driver.state,
                )
                simulator.accepts(driver.state, completed) shouldBe true
            }
        }
        val view = ObservationBuilder(driver.cardRegistry).build(driver.state, player, masked)
            .observation as TrainingObservation
        val option = view.legalActions.first { it.sourceEntityId == spell && it.kind == "CastWithKicker" }
        option.beholdCount shouldBe 1
        option.beholdPaymentOptions shouldBe cast.policyBeholdPaymentOptions

        val environment = GameEnvironment.create(driver.cardRegistry)
        environment.restore(driver.state, listOf(driver.player1, driver.player2))
        val gym = GameGymEnv(
            environment, perspectivePlayerIndex = 0, defaultRevealAll = false,
            agents = listOf(AgentSpec.Learner(), AgentSpec.Learner()),
        )
        (gym.observe().observation as TrainingObservation).legalActions shouldBe view.legalActions
        shouldThrow<IllegalArgumentException> {
            gym.step(option.actionId, ActionParams(targets = listOf(ogre)))
        }
        shouldThrow<IllegalArgumentException> {
            gym.step(option.actionId, ActionParams(targets = listOf(ogre), beheldCards = listOf(spell)))
        }
        gym.step(option.actionId, ActionParams(targets = listOf(ogre), beheldCards = listOf(dragon)))
        environment.lastRejection shouldBe null
    }

    test("a cast priced by its target offers only the targets the engine accepts") {
        // Ajani's Response costs {3} less if it targets a tapped creature. The enumerator calls it castable
        // for {1}{W} while any creature is tapped, and the first live PPO iteration (mtg-draft-ai docs/51 §3.1)
        // picked the untapped one, which costs {4}{W} and failed the final preflight.
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AjanisResponse)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val tapped = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val untapped = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.tapPermanent(tapped)
        val spell = driver.putCardInHand(player, "Ajani's Response")
        driver.giveMana(player, Color.WHITE, 2)
        val simulator = GameSimulator(driver.cardRegistry)
        val legal = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
        val bare = legal.first { (it.action as? CastSpell)?.cardId == spell }
        bare.affordable shouldBe true
        bare.validTargets.orEmpty().toSet() shouldBe setOf(tapped, untapped)

        val cast = PolicyActionBoundary.mask(legal, driver.state, simulator)
            .first { (it.action as? CastSpell)?.cardId == spell }
        cast.affordable shouldBe true
        cast.validTargets shouldBe listOf(tapped)
        simulator.accepts(
            driver.state, ActionParameterizer.apply(cast.action, ActionParams(targets = listOf(untapped)), driver.state),
        ) shouldBe false

        // Every other action keeps the targets it was enumerated with.
        val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
        legal.zip(masked).filter { (original, _) -> (original.action as? CastSpell)?.cardId != spell }
            .forEach { (original, after) -> after.validTargets shouldBe original.validTargets }
    }

    test("a parameter-free cast the engine would refuse is masked before the policy sees it") {
        // Group Project's flashback costs "tap three untapped creatures", which the flashback enumerator
        // prices as {0}: it was advertised as affordable with nothing to tap (mtg-draft-ai docs/51 §3.1).
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GroupProject)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val spell = driver.putCardInGraveyard(player, "Group Project")
        val simulator = GameSimulator(driver.cardRegistry)
        val legal = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
        val bare = legal.first { (it.action as? CastSpell)?.cardId == spell }
        bare.affordable shouldBe true
        simulator.accepts(driver.state, bare.action) shouldBe false

        val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
        masked.first { (it.action as? CastSpell)?.cardId == spell }.affordable shouldBe false
        // Everything the engine accepts stays callable.
        legal.zip(masked).filter { (original, _) -> (original.action as? CastSpell)?.cardId != spell }
            .forEach { (original, after) ->
                if (PolicyActionBoundary.callable(original)) after.affordable shouldBe original.affordable
            }
    }

    test("a single-target cast the engine refuses at its first target is masked") {
        // Dismember ({1}{B/P}{B/P}) was advertised as affordable and refused at every target
        // (mtg-draft-ai docs/51 §3.1). Whatever the engine accepts must stay callable.
        fun castable(color: Color, amount: Int): Pair<Boolean, Boolean> {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.registerCard(Dismember)
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            driver.putCreatureOnBattlefield(driver.getOpponent(player), "Grizzly Bears")
            val spell = driver.putCardInHand(player, "Dismember")
            driver.giveMana(player, color, amount)
            val simulator = GameSimulator(driver.cardRegistry)
            val legal = LegalActionEnumerator.create(driver.cardRegistry)
                .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
            val bare = legal.first { (it.action as? CastSpell)?.cardId == spell }
            val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
                .first { (it.action as? CastSpell)?.cardId == spell }
            val accepted = simulator.accepts(
                driver.state,
                ActionParameterizer.apply(bare.action, ActionParams(targets = listOf(bare.validTargets!!.first())), driver.state),
            )
            masked.affordable shouldBe (bare.affordable && accepted)
            return bare.affordable to masked.affordable
        }
        // Paid in black mana, it stays callable.
        castable(Color.BLACK, 3) shouldBe (true to true)
        // Only {1}: the Phyrexian symbols need 4 life, which the policy cannot choose to pay. The
        // enumerator calls it affordable and the engine refuses it, so the mask removes it.
        castable(Color.RED, 1) shouldBe (true to false)
    }

    test("a mana ability whose extra cost cannot be paid is masked, and stays callable when it can") {
        // Rubble Rouser ({T}, exile a card from your graveyard: add {R}) was advertised as affordable with an
        // empty graveyard and refused at the step (mtg-draft-ai docs/43 §5.1).
        fun rouser(graveyard: Boolean): Pair<Boolean, Boolean> {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.registerCard(RubbleRouser)
            driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
            val player = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val source = driver.putCreatureOnBattlefield(player, "Rubble Rouser")
            driver.removeSummoningSickness(source)
            if (graveyard) driver.putCardInGraveyard(player, "Mountain")
            val simulator = GameSimulator(driver.cardRegistry)
            val legal = LegalActionEnumerator.create(driver.cardRegistry)
                .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
            val bare = legal.first { (it.action as? ActivateAbility)?.sourceId == source }
            bare.isManaAbility shouldBe true
            val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
            masked.first { (it.action as? ActivateAbility)?.sourceId == source }.affordable shouldBe
                (bare.affordable && simulator.accepts(driver.state, bare.action))
            // Plain {T} mana abilities are never preflighted and keep their enumerated affordability.
            legal.zip(masked).filter { (original, _) -> original.isManaAbility &&
                (original.action as? ActivateAbility)?.sourceId != source }
                .forEach { (original, after) -> after.affordable shouldBe original.affordable }
            return bare.affordable to masked.first { (it.action as? ActivateAbility)?.sourceId == source }.affordable
        }
        rouser(graveyard = true) shouldBe (true to true)
        rouser(graveyard = false).second shouldBe false
    }

    test("a cast that may target the caster's own mana source offers only the targets it can still pay for") {
        // Suspend Aggression ({1}{R}{W}) aimed at the caster's only Treasure, with a Plains and a Mountain, was
        // advertised as affordable and refused: the Treasure is both the target and the third mana
        // (mtg-draft-ai docs/43 §5.3).
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SuspendAggression, PredefinedTokens.Treasure))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Plains")
        driver.putPermanentOnBattlefield(player, "Mountain")
        val treasure = driver.putPermanentOnBattlefield(player, "Treasure")
        val bears = driver.putCreatureOnBattlefield(driver.getOpponent(player), "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Suspend Aggression")
        val simulator = GameSimulator(driver.cardRegistry)
        val legal = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
        val bare = legal.first { (it.action as? CastSpell)?.cardId == spell }
        bare.affordable shouldBe true
        // As the step does: accepted directly, or after floating the Treasure's mana (auto-pay never sacrifices it).
        fun accepts(target: EntityId) = PolicyActionStager(simulator).begin(
            driver.state, ActionParameterizer.apply(bare.action, ActionParams(targets = listOf(target)), driver.state),
        ) != null
        accepts(bears) shouldBe true
        accepts(treasure) shouldBe false
        val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
            .first { (it.action as? CastSpell)?.cardId == spell }
        masked.affordable shouldBe true
        masked.validTargets shouldBe bare.validTargets.orEmpty().filter { accepts(it) }
    }

    test("an activation that may target the player's own mana source offers only the targets it can pay for") {
        // Shattered Acolyte ({1}, sacrifice it: destroy target artifact or enchantment) aimed at the player's only
        // Treasure was advertised as affordable and refused (mtg-draft-ai docs/51 §4.4a's dry run).
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShatteredAcolyte, PredefinedTokens.Treasure))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val acolyte = driver.putCreatureOnBattlefield(player, "Shattered Acolyte")
        val mine = driver.putPermanentOnBattlefield(player, "Treasure")
        val theirs = driver.putPermanentOnBattlefield(driver.getOpponent(player), "Treasure")
        val simulator = GameSimulator(driver.cardRegistry)
        val legal = LegalActionEnumerator.create(driver.cardRegistry)
            .enumerate(driver.state, player, EnumerationMode.ACTIONS_ONLY)
        val bare = legal.first { (it.action as? ActivateAbility)?.sourceId == acolyte && !it.isManaAbility }
        bare.affordable shouldBe true
        fun accepts(target: EntityId) = PolicyActionStager(simulator).begin(
            driver.state, ActionParameterizer.apply(bare.action, ActionParams(targets = listOf(target)), driver.state),
        ) != null
        accepts(theirs) shouldBe true
        accepts(mine) shouldBe false
        val masked = PolicyActionBoundary.mask(legal, driver.state, simulator)
            .first { (it.action as? ActivateAbility)?.sourceId == acolyte && !it.isManaAbility }
        masked.affordable shouldBe true
        masked.validTargets shouldBe listOf(theirs)
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
