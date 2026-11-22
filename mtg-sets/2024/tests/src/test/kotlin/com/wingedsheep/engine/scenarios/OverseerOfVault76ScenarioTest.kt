package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.OverseerOfVault76
import com.wingedsheep.mtg.sets.definitions.mrd.cards.RaiseTheAlarm
import com.wingedsheep.mtg.sets.definitions.usg.cards.GloriousAnthem
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.components.identity.TokenComponent

class OverseerOfVault76ScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(OverseerOfVault76, RaiseTheAlarm, GloriousAnthem))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        repeat(30) {
            if (d.pendingDecision == null && d.state.stack.isNotEmpty())
                d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
        }
    }
    fun counters(d: GameTestDriver, id: EntityId, kind: CounterType = CounterType.QUEST) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0
    fun setQuest(d: GameTestDriver, id: EntityId, count: Int) =
        d.addComponent(id, CountersComponent().withAdded(CounterType.QUEST, count))
    fun cast(d: GameTestDriver, name: String) : EntityId {
        val id = d.putCardInHand(d.player1, name)
        d.giveMana(d.player1, Color.WHITE, 8)
        d.giveMana(d.player1, Color.GREEN, 8)
        d.castSpell(d.player1, id).error shouldBe null
        resolve(d)
        return id
    }
    fun combat(d: GameTestDriver) {
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        resolve(d)
    }

    test("self enters even above three projected power; other creatures use projected entry power") {
        val d = setup()
        d.putPermanentOnBattlefield(d.player1, GloriousAnthem.name)
        val overseer = cast(d, OverseerOfVault76.name)
        d.state.projectedState.getPower(overseer) shouldBe 4
        counters(d, overseer) shouldBe 1
        cast(d, "Centaur Courser") // Printed three, projected four: excluded.
        counters(d, overseer) shouldBe 1
        cast(d, "Grizzly Bears") // Printed two, projected three: included.
        counters(d, overseer) shouldBe 2
    }

    test("simultaneous Overseer and other entrants use projected power in either entry order") {
        for (names in listOf(
            listOf(OverseerOfVault76.name, "Grizzly Bears", "Centaur Courser"),
            listOf("Centaur Courser", "Grizzly Bears", OverseerOfVault76.name),
        )) {
            val d = setup()
            val reunion = card("Overseer Test Reunion") {
                manaCost = "{0}"
                typeLine = "Sorcery"
                spell {
                    effect = Effects.Pipeline {
                        val creatures = gather(CardSource.FromZone(
                            Zone.HAND,
                            Player.You,
                            GameObjectFilter.Creature,
                        ))
                        move(creatures, CardDestination.ToZone(
                            Zone.BATTLEFIELD,
                            Player.You,
                        ))
                    }
                }
            }
            d.registerCards(listOf(reunion))
            d.putPermanentOnBattlefield(d.player1, GloriousAnthem.name)
            val ids = names.associateWith { d.putCardInHand(d.player1, it) }
            val spell = d.putCardInHand(d.player1, reunion.name)
            d.castSpell(d.player1, spell).error shouldBe null
            resolve(d)
            val ordering = d.pendingDecision as? OrderObjectsDecision
            if (ordering != null) d.submitOrderedResponse(ordering.playerId, ordering.objects).error shouldBe null
            resolve(d)
            d.pendingDecision shouldBe null
            d.state.stack.size shouldBe 0
            val overseer = ids.getValue(OverseerOfVault76.name)
            d.state.projectedState.getPower(overseer) shouldBe 4
            d.state.projectedState.getPower(ids.getValue("Grizzly Bears")) shouldBe 3
            d.state.projectedState.getPower(ids.getValue("Centaur Courser")) shouldBe 4
            counters(d, overseer) shouldBe 2
        }
    }

    test("each simultaneous qualifying token entry contributes a quest counter") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        cast(d, RaiseTheAlarm.name)
        counters(d, overseer) shouldBe 2
    }

    test("remove three from one permanent, then respond before the separate team buff resolves") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        setQuest(d, overseer, 3)
        combat(d)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, List(3) { overseer }).error shouldBe null
        counters(d, overseer) shouldBe 0
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.state.stack.size shouldBe 1
        resolve(d)
        counters(d, overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

    test("split removal includes noncreature permanents and never buffs the opponent") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        val land = d.putLandOnBattlefield(d.player1, "Plains")
        val enemy = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        setQuest(d, overseer, 2); setQuest(d, land, 1)
        combat(d)
        d.submitCardSelection(d.player1, listOf(overseer, overseer, land)).error shouldBe null
        resolve(d)
        counters(d, overseer) shouldBe 0
        counters(d, land) shouldBe 0
        counters(d, land, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        counters(d, enemy, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.state.projectedState.hasKeyword(enemy, Keyword.VIGILANCE) shouldBe false
    }

    test("combat trigger pays from another permanent after Overseer leaves before resolution") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val land = d.putLandOnBattlefield(d.player1, "Plains")
        setQuest(d, land, 3)
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, bolt, listOf(overseer)).error shouldBe null
        d.bothPass()
        (overseer in d.getGraveyard(d.player1)) shouldBe true
        resolve(d)
        d.submitCardSelection(d.player1, List(3) { land }).error shouldBe null
        resolve(d)
        counters(d, land) shouldBe 0
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
    }

    test("a token copy's combat trigger survives its source ceasing to exist and creates a separate buff") {
        val d = setup()
        val copySpell = card("Overseer Test Token Copy") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell {
                effect = Effects.CreateTokenCopyOfTarget(
                    target(TargetFilter.Creature))
            }
        }
        d.registerCard(copySpell)
        val original = d.putCreatureOnBattlefield(d.player2, OverseerOfVault76.name)
        val copyCard = d.putCardInHand(d.player1, copySpell.name)
        d.castSpell(d.player1, copyCard, listOf(original)).error shouldBe null
        resolve(d)
        val token = d.getPermanents(d.player1).single {
            d.state.getEntity(it)?.has<TokenComponent>() == true
        }
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val land = d.putLandOnBattlefield(d.player1, "Plains")
        setQuest(d, land, 3)
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, bolt, listOf(token)).error shouldBe null
        d.bothPass()
        d.state.getEntity(token) shouldBe null
        resolve(d)
        val payment = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        payment.options shouldBe listOf(land)
        payment.selectionLimits shouldBe mapOf(land to 3)
        payment.context.sourceName shouldBe OverseerOfVault76.name
        d.submitCardSelection(d.player1, List(3) { land }).error shouldBe null
        counters(d, land) shouldBe 0
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.state.stack.size shouldBe 1
        resolve(d)
        counters(d, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
        counters(d, original, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }

    test("declining preserves quest counters and creates no reflexive trigger") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        setQuest(d, overseer, 3)
        combat(d)
        d.submitCardSelection(d.player1, emptyList()).error shouldBe null
        resolve(d)
        counters(d, overseer) shouldBe 3
        counters(d, overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.state.stack.size shouldBe 0
    }

    test("two quest counters cannot pay; other counter kinds and opposing quests do not count") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        d.addComponent(overseer, CountersComponent().withAdded(CounterType.QUEST, 2).withAdded(CounterType.CHARGE, 5))
        val enemy = d.putLandOnBattlefield(d.player2, "Plains")
        setQuest(d, enemy, 8)
        combat(d)
        d.pendingDecision shouldBe null
        counters(d, overseer) shouldBe 2
        counters(d, overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }

    test("partial, over-capacity and excess payments reject without spending counters") {
        val d = setup()
        val overseer = d.putCreatureOnBattlefield(d.player1, OverseerOfVault76.name)
        val land = d.putLandOnBattlefield(d.player1, "Plains")
        setQuest(d, overseer, 2); setQuest(d, land, 2)
        combat(d)
        d.submitCardSelection(d.player1, listOf(overseer)).error?.isNotEmpty() shouldBe true
        d.submitCardSelection(d.player1, List(3) { overseer }).error?.isNotEmpty() shouldBe true
        d.submitCardSelection(d.player1, listOf(overseer, overseer, land, land)).error?.isNotEmpty() shouldBe true
        counters(d, overseer) shouldBe 2
        counters(d, land) shouldBe 2
        d.submitCardSelection(d.player1, listOf(overseer, land, land)).error shouldBe null
        resolve(d)
        counters(d, overseer) shouldBe 1
        counters(d, land) shouldBe 0
        counters(d, overseer, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }
})
