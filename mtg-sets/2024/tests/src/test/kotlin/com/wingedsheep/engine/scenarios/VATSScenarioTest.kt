package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.VATS
import com.wingedsheep.mtg.sets.definitions.mom.cards.OculusWhelp
import com.wingedsheep.mtg.sets.definitions.emn.cards.SmolderingWerewolf
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VATSScenarioTest : FunSpec({
    fun setup() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(VATS, OculusWhelp, SmolderingWerewolf))
        it.initMirrorMatch(Deck.of("Swamp" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun cast(d: GameTestDriver, targets: List<EntityId>) = d.run {
        val spell = putCardInHand(activePlayer!!, "V.A.T.S.")
        giveMana(activePlayer!!, Color.BLACK, 4)
        castSpell(activePlayer!!, spell, targets)
    }
    fun boost(d: GameTestDriver, id: EntityId, n: Int = 1) =
        d.addComponent(id, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to n)))

    test("destroys any number of equal-toughness creatures across controllers") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val c = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        boost(d, a); boost(d, b)
        cast(d, listOf(a, b, c)).outcome shouldBe Outcome.Done
        d.bothPass()
        (a in d.getGraveyard(d.player1)) shouldBe true
        listOf(b, c).all { it in d.getGraveyard(d.player2) } shouldBe true
    }
    test("rejects unequal toughness before paying or placing on the stack") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        (cast(d, listOf(a, b)).outcome is Outcome.Rejected) shouldBe true
        d.state.stack shouldBe emptyList()
    }
    test("zero targets is a legal cast") {
        val d = setup()
        cast(d, emptyList()).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.stack shouldBe emptyList()
        d.state.gameOver shouldBe false
    }
    test("a single target remains legal when its toughness changes") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        cast(d, listOf(a)).outcome shouldBe Outcome.Done
        boost(d, a)
        d.bothPass()
        (a in d.getGraveyard(d.player2)) shouldBe true
    }
    test("no creature is destroyed if the surviving legal targets now have unequal toughness") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        cast(d, listOf(a, b)).outcome shouldBe Outcome.Done
        boost(d, a)
        d.bothPass()
        d.getPermanents(d.player1).contains(a) shouldBe true
        d.getPermanents(d.player2).contains(b) shouldBe true
        d.state.stack shouldBe emptyList()
    }
    test("a departed target is excluded before comparing the survivors") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        cast(d, listOf(a, b)).outcome shouldBe Outcome.Done
        d.moveToGraveyard(a)
        boost(d, b)
        d.bothPass()
        (b in d.getGraveyard(d.player2)) shouldBe true
    }
    test("split second prevents a response spell") {
        val d = setup()
        val a = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        cast(d, listOf(a)).outcome shouldBe Outcome.Done
        d.passPriority(d.activePlayer!!)
        val responder = d.state.priorityPlayerId!!
        val shock = d.putCardInHand(responder, "Lightning Bolt")
        d.giveMana(responder, Color.RED)
        (d.castSpell(responder, shock, listOf(a)).outcome is Outcome.Rejected) shouldBe true
        d.bothPass()
        (a in d.getGraveyard(d.player1)) shouldBe true
    }
    test("simultaneous destruction keeps a dies ability granted by a transformed permanent") {
        val d = setup()
        val p = d.activePlayer!!
        val wolf = d.putCreatureOnBattlefield(p, "Smoldering Werewolf")
        val whelp = d.putCreatureOnBattlefield(p, "Oculus Whelp")
        d.giveMana(p, Color.RED, 6)
        d.submit(ActivateAbility(p, wolf, SmolderingWerewolf.script.activatedAbilities.first().id)).error shouldBe null
        d.bothPass()
        d.getCardName(wolf) shouldBe "Erupting Dreadwolf"
        boost(d, whelp, 2)
        val before = d.getHandSize(p)
        cast(d, listOf(wolf, whelp)).error shouldBe null
        d.bothPass()
        (wolf in d.getGraveyard(p)) shouldBe true
        (whelp in d.getGraveyard(p)) shouldBe true
        d.bothPass()
        d.getHandSize(p) shouldBe before + 1
    }
    test("collection free casting offers all compatible targets instead of a one-target cap") {
        val d = setup()
        val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val b = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val free = card("Test VATS Free Cast") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            spell {
                effect = Effects.Pipeline {
                    val spells = gather(CardSource.FromZone(Zone.GRAVEYARD, filter = GameObjectFilter.Instant))
                    run(Effects.CastFromCollectionWithoutPayingCost(spells.key))
                }
            }
        }
        d.registerCards(listOf(free))
        d.putCardInGraveyard(p, "V.A.T.S.")
        val source = d.putCardInHand(p, free.name)
        d.castSpell(p, source).error shouldBe null
        d.bothPass()
        val decision = d.state.pendingDecision as ChooseTargetsDecision
        decision.targetRequirements.single().maxTargets shouldBe 2
        decision.targetRequirements.single().selectionGroups shouldBe listOf(listOf(a, b))
        d.submit(SubmitDecision(p, TargetsResponse(decision.id, mapOf(0 to listOf(a, b))))).error shouldBe null
        d.bothPass()
        listOf(a, b).all { it in d.getGraveyard(d.player2) } shouldBe true
    }

})
