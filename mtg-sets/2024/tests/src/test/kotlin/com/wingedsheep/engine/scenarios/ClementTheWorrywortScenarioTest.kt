package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.BloomburrowSet
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Clement, the Worrywort — {1}{G}{U} Legendary Creature — Frog Druid 3/3
 * Vigilance
 * Whenever Clement or another creature you control enters, return up to one target creature you
 * control with lesser mana value to its owner's hand.
 * Frogs you control have "{T}: Add {G} or {U}. Spend this mana only to cast a creature spell."
 *
 * The first tests pin the granted mana ability; the rest pin that the bounce is a real, optional
 * target chosen as the trigger goes on the stack (CR 603.3d): capped below the entering creature's
 * mana value, unable to reach a creature with shroud, and doing nothing if the target is gone on
 * resolution (CR 608.2b).
 */
class ClementTheWorrywortScenarioTest : FunSpec({

    val cardRegistry = CardRegistry().apply {
        register(BloomburrowSet.cards)
        register(BloomburrowSet.basicLands)
        register(PortalSet.cards)
        register(PortalSet.basicLands)
    }
    val manaSolver = ManaSolver(cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    fun createGameWithClement(): Triple<GameState, EntityId, EntityId> {
        val player1 = EntityId.generate()
        val clementId = EntityId.generate()

        val clementDef = cardRegistry.getCard("Clement, the Worrywort")!!

        // Build minimal game state with Clement on the battlefield
        var state = GameState(turnOrder = listOf(player1, EntityId.generate()))

        // Add player entity
        state = state.withEntity(player1, ComponentContainer.of(
            PlayerComponent("Player1"),
            LifeTotalComponent(20)
        ))

        // Add Clement to the battlefield (no summoning sickness)
        val clementContainer = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = clementDef.name,
                name = clementDef.name,
                manaCost = clementDef.manaCost,
                typeLine = clementDef.typeLine,
                oracleText = clementDef.oracleText,
                baseStats = clementDef.creatureStats,
                baseKeywords = clementDef.keywords,
                colors = clementDef.colors,
                ownerId = player1
            ),
            ControllerComponent(player1),
            OwnerComponent(player1)
        )
        state = state.withEntity(clementId, clementContainer)
        state = state.addToZone(ZoneKey(player1, Zone.BATTLEFIELD), clementId)

        // Add a library card to prevent draw-from-empty
        val libraryCardId = EntityId.generate()
        val forestDef = cardRegistry.getCard("Forest")!!
        state = state.withEntity(libraryCardId, ComponentContainer.of(
            CardComponent(
                cardDefinitionId = forestDef.name,
                name = forestDef.name,
                manaCost = ManaCost.ZERO,
                typeLine = forestDef.typeLine,
                ownerId = player1
            ),
            OwnerComponent(player1)
        ))
        state = state.addToZone(ZoneKey(player1, Zone.LIBRARY), libraryCardId)

        return Triple(state, player1, clementId)
    }

    test("ManaSolver recognizes Clement as mana source via its own static grant") {
        val (state, player1, clementId) = createGameWithClement()

        val sources = manaSolver.findAvailableManaSources(state, player1)

        val clementSource = sources.find { it.entityId == clementId }
        clementSource shouldNotBe null
        clementSource!!.producesColors shouldContain Color.GREEN
        clementSource.producesColors shouldContain Color.BLUE
        clementSource.restriction shouldBe ManaRestriction.CreatureSpellsOnly
    }

    test("Clement with summoning sickness is not available as mana source") {
        val (baseState, player1, clementId) = createGameWithClement()

        // Add summoning sickness
        val state = baseState.updateEntity(clementId) { it.with(SummoningSicknessComponent) }

        val sources = manaSolver.findAvailableManaSources(state, player1)

        val clementSource = sources.find { it.entityId == clementId }
        clementSource shouldBe null
    }

    fun driverWithClementInHand(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        return driver to me
    }

    fun GameTestDriver.castClement(me: EntityId) {
        val clement = putCardInHand(me, "Clement, the Worrywort")
        giveColorlessMana(me, 1)
        giveMana(me, Color.GREEN, 1)
        giveMana(me, Color.BLUE, 1)
        castSpell(me, clement).error shouldBe null
        bothPass() // Clement resolves; its own enters trigger fires
    }

    test("the bounce target is chosen on trigger, among lesser-mana-value creatures without shroud") {
        val (driver, me) = driverWithClementInHand()
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")      // MV 2
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")     // MV 1
        val budoka = driver.putCreatureOnBattlefield(me, "Humble Budoka")     // MV 2, shroud
        val giant = driver.putCreatureOnBattlefield(me, "Hill Giant")         // MV 4 — not lesser
        val theirs = driver.putCreatureOnBattlefield(driver.getOpponent(me), "Savannah Lions")

        driver.castClement(me)

        val decision = driver.pendingDecision
        withClue("the enters trigger asks for its target: $decision") {
            (decision is ChooseTargetsDecision) shouldBe true
        }
        decision as ChooseTargetsDecision
        withClue("lesser mana value, you control, and not shroud") {
            decision.legalTargets[0]?.toSet() shouldBe setOf(bears, lions)
        }
        withClue("the targets are optional (\"up to one\")") { decision.targetRequirements[0].minTargets shouldBe 0 }
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null
        driver.bothPass()

        driver.findCardInHand(me, "Grizzly Bears") shouldBe bears
        withClue("everything else stays put") {
            driver.state.getBattlefield().containsAll(listOf(lions, budoka, giant, theirs)) shouldBe true
        }
    }

    test("choosing no target returns nothing") {
        val (driver, me) = driverWithClementInHand()
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Savannah Lions")

        driver.castClement(me)
        val decision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to emptyList()))).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.state.getBattlefield().contains(bears) shouldBe true
    }

    test("a target that leaves the battlefield in response is not replaced on resolution") {
        val (driver, me) = driverWithClementInHand()
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")

        driver.castClement(me)
        val decision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null

        driver.moveToGraveyard(bears)
        driver.bothPass()

        driver.getGraveyard(me).contains(bears) shouldBe true
        withClue("Savannah Lions was never targeted and stays on the battlefield") {
            driver.state.getBattlefield().contains(lions) shouldBe true
        }
    }
})
