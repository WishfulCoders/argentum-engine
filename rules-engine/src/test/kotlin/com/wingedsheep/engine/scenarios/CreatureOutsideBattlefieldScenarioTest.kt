package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.handlers.effects.BattlefieldEntry
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreatureOutsideBattlefield
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test for [CreatureOutsideBattlefield] — Grist, the Hunger Tide's "As long as Grist isn't
 * on the battlefield, it's a 1/1 Insect creature in addition to its other types" (CR 113.6c).
 *
 * Pins the Grist rulings (2021-06-18): anywhere but the battlefield it is a Planeswalker Creature —
 * Insect (in the library from game start, in hand, on the stack, in the graveyard), so "counter
 * target creature spell" can counter it and a graveyard creature-card count includes it; once it
 * enters the battlefield it is just a planeswalker, and it is a creature card again after it dies.
 */
class CreatureOutsideBattlefieldScenarioTest : ScenarioTestBase() {

    private val grist = card("Test Grist") {
        manaCost = "{1}"
        typeLine = "Legendary Planeswalker — Grist"
        startingLoyalty = 3
        oracleText = "As long as Test Grist isn't on the battlefield, it's a 1/1 Insect creature in addition to its other types.\n0: You gain 1 life."
        staticAbility { ability = CreatureOutsideBattlefield(power = 1, toughness = 1, subtypes = setOf("Insect")) }
        loyaltyAbility(0) { effect = Effects.GainLife(1) }
    }

    private val scatter = card("Creature Scatter") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Counter target creature spell."
        spell {
            target(TargetFilter.CreatureSpellOnStack)
            effect = Effects.CounterSpell()
        }
    }

    private val gravecount = card("Grave Count") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You gain life equal to the number of creature cards in your graveyard."
        spell {
            effect = Effects.GainLife(
                DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature).count()
            )
        }
    }

    private val slay = card("Walker Slay") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Destroy target planeswalker."
        spell {
            val t = target(TargetFilter(GameObjectFilter.Planeswalker))
            effect = Effects.Destroy(t)
        }
    }

    private fun TestGame.card(id: EntityId) = state.getEntity(id)!!.get<CardComponent>()!!

    private fun TestGame.assertCreatureCard(id: EntityId) {
        val c = card(id)
        c.typeLine.isCreature shouldBe true
        c.typeLine.cardTypes.any { it.name == "PLANESWALKER" } shouldBe true
        c.typeLine.subtypes.map { it.value }.toSet().containsAll(setOf("Grist", "Insect")) shouldBe true
        c.baseStats?.basePower shouldBe 1
        c.baseStats?.baseToughness shouldBe 1
    }

    init {
        cardRegistry.register(grist)
        cardRegistry.register(scatter)
        cardRegistry.register(gravecount)
        cardRegistry.register(slay)

        test("a card minted for the library is a 1/1 Insect creature card") {
            val container = CardEntityFactory.create(grist, EntityId.of("owner"))
            val c = container.get<CardComponent>()!!
            c.typeLine.isCreature shouldBe true
            c.baseStats?.basePower shouldBe 1
        }

        test("in hand and on the stack it is a creature; a creature-spell counter can counter it") {
            val game = scenario()
                .withPlayers("Grist", "Opponent")
                .withCardInHand(1, "Test Grist")
                .withCardInHand(1, "Creature Scatter")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gristId = game.findCardsInHand(1, "Test Grist").single()
            game.assertCreatureCard(gristId)

            game.castSpell(1, "Test Grist").error shouldBe null
            withClue("on the stack it is a creature spell") { game.assertCreatureCard(gristId) }
            game.castSpellTargetingStackSpell(1, "Creature Scatter", "Test Grist").error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Test Grist") shouldBe true
            withClue("countered into the graveyard, still a creature card") { game.assertCreatureCard(gristId) }
        }

        test("on the battlefield it is only a planeswalker; dead, it is a creature card again") {
            val game = scenario()
                .withPlayers("Grist", "Opponent")
                .withCardInHand(1, "Test Grist")
                .withCardInHand(1, "Walker Slay")
                .withCardInHand(1, "Grave Count")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gristId = game.findCardsInHand(1, "Test Grist").single()
            game.castSpell(1, "Test Grist").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Test Grist") shouldBe true
            val projected = game.state.projectedState
            projected.isCreature(gristId) shouldBe false
            projected.isPlaneswalker(gristId) shouldBe true
            projected.getSubtypes(gristId).contains("Insect") shouldBe false
            game.card(gristId).typeLine.isCreature shouldBe false
            game.state.getEntity(gristId)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 3

            game.castSpell(1, "Walker Slay", gristId).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Test Grist") shouldBe true
            game.assertCreatureCard(gristId)

            game.castSpell(1, "Grave Count").error shouldBe null
            game.resolveStack()
            withClue("the graveyard count includes Grist as a creature card") { game.getLifeTotal(1) shouldBe 21 }
        }

        test("an ad-hoc battlefield insertion (return from linked exile) restores the printed characteristics") {
            val game = scenario()
                .withPlayers("Grist", "Opponent")
                .withCardInHand(1, "Test Grist")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gristId = game.findCardsInHand(1, "Test Grist").single()
            game.assertCreatureCard(gristId)

            val placed = BattlefieldEntry.place(
                game.state.removeFromZone(ZoneKey(game.player1Id, Zone.HAND), gristId),
                game.player1Id,
                gristId
            )
            val c = placed.getEntity(gristId)!!.get<CardComponent>()!!
            c.typeLine.isCreature shouldBe false
            c.typeLine.subtypes.map { it.value }.contains("Insect") shouldBe false
            placed.projectedState.isCreature(gristId) shouldBe false
            placed.projectedState.isPlaneswalker(gristId) shouldBe true
        }

        test("put directly onto the battlefield by the scenario builder it is a planeswalker only") {
            val game = scenario()
                .withPlayers("Grist", "Opponent")
                .withCardOnBattlefield(1, "Test Grist")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gristId = game.findPermanent("Test Grist")!!
            game.state.projectedState.isCreature(gristId) shouldBe false
        }
    }
}
