package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bok.cards.UmezawasJitte
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Umezawa's Jitte — {2} Legendary Artifact — Equipment
 *
 * "Whenever equipped creature deals combat damage, put two charge counters on Umezawa's Jitte.
 *  Remove a charge counter from Umezawa's Jitte: Choose one —
 *  • Equipped creature gets +2/+2 until end of turn.
 *  • Target creature gets -1/-1 until end of turn.
 *  • You gain 2 life.
 *  Equip {2}"
 *
 * The trigger names no recipient, so the equipped creature's simultaneous combat damage is one
 * trigger event per combat damage step (CR 510.2, 603.2c): trample over a blocker, or damage split
 * between two blockers, is two counters — not two per recipient.
 */
class UmezawasJitteScenarioTest : FunSpec({

    val trampler = card("Test Jitte Trampler") {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        power = 5
        toughness = 8
        keywords(Keyword.TRAMPLE)
    }

    val brute = card("Test Jitte Brute") {
        manaCost = "{1}"
        typeLine = "Creature — Giant"
        power = 5
        toughness = 8
    }

    val doubleStriker = card("Test Jitte Double Striker") {
        manaCost = "{1}"
        typeLine = "Creature — Samurai"
        power = 2
        toughness = 8
        keywords(Keyword.DOUBLE_STRIKE)
    }

    val bear = card("Test Jitte Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val wall = card("Test Jitte Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 10
    }

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(UmezawasJitte, trampler, brute, doubleStriker, bear, wall))
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.equip(host: EntityId): EntityId {
        val jitte = putPermanentOnBattlefield(state.getEntity(host)!!
            .get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()!!.playerId, "Umezawa's Jitte")
        replaceState(state.updateEntity(jitte) { it.with(AttachedToComponent(host)) }
            .updateEntity(host) { it.with(AttachmentsComponent(listOf(jitte))) })
        return jitte
    }

    fun GameTestDriver.charge(jitte: EntityId): Int =
        state.getEntity(jitte)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    fun GameTestDriver.setCharge(jitte: EntityId, n: Int) {
        replaceState(state.updateEntity(jitte) { it.with(CountersComponent(mapOf(CounterType.CHARGE to n))) })
    }

    /** [attacker] attacks; [blockers] (possibly none) block it; combat runs out with triggers resolved. */
    fun GameTestDriver.attack(attacker: EntityId, blockers: List<EntityId>) {
        val active = activePlayer!!
        val defender = getOpponent(active)
        (blockers + attacker).forEach(::removeSummoningSickness)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(active, listOf(attacker), defender).outcome shouldBe Outcome.Done
        bothPass()
        if (blockers.isEmpty()) declareNoBlockers(defender)
        else declareBlockers(defender, blockers.associateWith { listOf(attacker) }).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.END_COMBAT)
    }

    fun GameTestDriver.activateMode(jitte: EntityId, mode: Int, target: EntityId? = null) {
        val me = activePlayer!!
        submit(ActivateAbility(playerId = me, sourceId = jitte, abilityId = UmezawasJitte.activatedAbilities[0].id))
            .outcome shouldBe Outcome.Done
        bothPass() // resolve → mode choice
        val modeDecision = pendingDecision as ChooseOptionDecision
        submitDecision(me, OptionChosenResponse(modeDecision.id, mode))
        if (target != null) submitTargetSelection(me, listOf(target))
        var guard = 0
        while (guard++ < 10 && state.stack.isNotEmpty()) bothPass()
    }

    test("unblocked combat damage puts two charge counters on the Jitte") {
        val d = createDriver()
        val me = d.activePlayer!!
        val attacker = d.putCreatureOnBattlefield(me, "Test Jitte Brute")
        val jitte = d.equip(attacker)

        d.attack(attacker, emptyList())

        d.getLifeTotal(d.getOpponent(me)) shouldBe 15
        d.charge(jitte) shouldBe 2
    }

    test("trample over a blocker triggers once: two counters, not four") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Jitte Trampler")
        val jitte = d.equip(attacker)
        val blocker = d.putCreatureOnBattlefield(them, "Test Jitte Bear")

        d.attack(attacker, listOf(blocker))

        // 2 lethal to the bear, 3 trampled over — damage to two recipients, one trigger.
        d.state.getBattlefield().contains(blocker) shouldBe false
        d.getLifeTotal(them) shouldBe 17
        d.charge(jitte) shouldBe 2
    }

    test("damage split between two blockers triggers once: two counters, not four") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Jitte Brute")
        val jitte = d.equip(attacker)
        val b1 = d.putCreatureOnBattlefield(them, "Test Jitte Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Jitte Bear")

        d.attack(attacker, listOf(b1, b2))

        d.state.getBattlefield().contains(b1) shouldBe false
        d.state.getBattlefield().contains(b2) shouldBe false
        d.charge(jitte) shouldBe 2
    }

    test("double strike deals combat damage in two steps: four counters") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Jitte Double Striker")
        val jitte = d.equip(attacker)
        val blocker = d.putCreatureOnBattlefield(them, "Test Jitte Wall")

        d.attack(attacker, listOf(blocker))

        d.charge(jitte) shouldBe 4
    }

    test("mode 1: equipped creature gets +2/+2 until end of turn") {
        val d = createDriver()
        val me = d.activePlayer!!
        val host = d.putCreatureOnBattlefield(me, "Test Jitte Bear")
        val jitte = d.equip(host)
        d.setCharge(jitte, 1)

        d.activateMode(jitte, 0)

        d.charge(jitte) shouldBe 0
        d.state.projectedState.getPower(host) shouldBe 4
        d.state.projectedState.getToughness(host) shouldBe 4
    }

    test("mode 2: target creature gets -1/-1 until end of turn") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val jitte = d.equip(d.putCreatureOnBattlefield(me, "Test Jitte Bear"))
        d.setCharge(jitte, 2)
        val victim = d.putCreatureOnBattlefield(them, "Test Jitte Bear")

        d.activateMode(jitte, 1, victim)
        d.state.projectedState.getPower(victim) shouldBe 1
        d.activateMode(jitte, 1, victim)

        d.charge(jitte) shouldBe 0
        d.state.getBattlefield().contains(victim) shouldBe false
    }

    test("mode 3: you gain 2 life") {
        val d = createDriver()
        val me = d.activePlayer!!
        val jitte = d.equip(d.putCreatureOnBattlefield(me, "Test Jitte Bear"))
        d.setCharge(jitte, 1)

        d.activateMode(jitte, 2)

        d.charge(jitte) shouldBe 0
        d.getLifeTotal(me) shouldBe 22
    }

    test("can't be activated without a charge counter; Umezawa's Jitte is legendary") {
        val d = createDriver()
        val me = d.activePlayer!!
        val jitte = d.equip(d.putCreatureOnBattlefield(me, "Test Jitte Bear"))

        d.submit(ActivateAbility(playerId = me, sourceId = jitte, abilityId = UmezawasJitte.activatedAbilities[0].id))
            .outcome shouldNotBe Outcome.Done
        UmezawasJitte.typeLine.isLegendary shouldBe true
    }
})
