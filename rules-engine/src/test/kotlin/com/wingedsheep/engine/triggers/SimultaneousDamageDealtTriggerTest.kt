package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Whenever [this / equipped creature / a creature you control] deals combat damage" names no
 * recipient, so its trigger event is the source's whole simultaneous combat damage (CR 510.2), and
 * an ability triggers only once each time its trigger event occurs (CR 603.2c). Trampling over a
 * blocker or splitting damage between two blockers is one trigger, carrying the total — Umezawa's
 * Jitte gets two counters, not four; Drinker of Sorrow's 2004-10-04 ruling: "You only sacrifice one
 * permanent, no matter how many things it deals damage to." A trigger that names its recipient
 * ("deals combat damage to a creature") is still once per recipient, and first-strike and regular
 * damage are separate combat damage steps (CR 510.4), so separate triggers.
 */
class SimultaneousDamageDealtTriggerTest : FunSpec({

    // "Whenever equipped creature deals combat damage, put a charge counter on this Equipment."
    val chargeBlade = card("Test Charge Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        triggeredAbility {
            trigger = Triggers.attached.dealsCombatDamage()
            effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        }
    }

    // "Whenever equipped creature deals 3 or more combat damage, you gain that much life." Two
    // 2-damage triggers would both miss the gate, so a life gain of 4 proves one folded trigger.
    val fervorBlade = card("Test Fervor Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        triggeredAbility {
            trigger = Triggers.attached.dealsCombatDamage()
            triggerRestriction = Conditions.CompareAmounts(
                DynamicAmounts.triggerDamageAmount(), ComparisonOperator.GTE, 3
            )
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        }
    }

    // SELF, Drinker of Sorrow's shape: "Whenever this creature deals combat damage, you gain 1 life."
    val drinker = card("Test Sorrow Drinker") {
        manaCost = "{1}"
        typeLine = "Creature — Horror"
        power = 4
        toughness = 8
        triggeredAbility {
            trigger = Triggers.self.dealsCombatDamage()
            effect = Effects.GainLife(1)
        }
    }

    // Names its recipient: "Whenever this creature deals combat damage to a creature, gain 1 life."
    val biter = card("Test Creature Biter") {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 8
        triggeredAbility {
            trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature)
            effect = Effects.GainLife(1)
        }
    }

    // ANY observer: "Whenever a creature you control deals combat damage, you gain 1 life."
    val watcher = card("Test Combat Watcher") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage()
            effect = Effects.GainLife(1)
        }
    }

    val brute = card("Test Brute") {
        manaCost = "{1}"
        typeLine = "Creature — Giant"
        power = 4
        toughness = 8
    }

    val trampler = card("Test Trampler") {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 8
        keywords(Keyword.TRAMPLE)
    }

    val glassBrute = card("Test Glass Brute") {
        manaCost = "{1}"
        typeLine = "Creature — Giant"
        power = 4
        toughness = 4
    }

    val doubleStriker = card("Test Double Striker") {
        manaCost = "{1}"
        typeLine = "Creature — Soldier"
        power = 2
        toughness = 8
        keywords(Keyword.DOUBLE_STRIKE)
    }

    val bear = card("Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val wall = card("Test Big Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 10
    }

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(
            TestCards.all + listOf(
                chargeBlade, fervorBlade, drinker, biter, watcher, brute, trampler, glassBrute,
                doubleStriker, bear, wall
            )
        )
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.attach(equipment: EntityId, host: EntityId) {
        val existing = state.getEntity(host)?.get<AttachmentsComponent>()?.attachedIds.orEmpty()
        replaceState(state.updateEntity(equipment) { it.with(AttachedToComponent(host)) }
            .updateEntity(host) { it.with(AttachmentsComponent(existing + equipment)) })
    }

    fun GameTestDriver.charge(equipment: EntityId): Int =
        state.getEntity(equipment)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    /** Each attacker in [blocks] attacks; its listed blockers block it; combat runs out with triggers resolved. */
    fun GameTestDriver.combat(blocks: Map<EntityId, List<EntityId>>) {
        val active = activePlayer!!
        val defender = getOpponent(active)
        (blocks.keys + blocks.values.flatten()).forEach(::removeSummoningSickness)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(active, blocks.keys.toList(), defender).outcome shouldBe Outcome.Done
        bothPass()
        val blockerMap = blocks.flatMap { (attacker, bs) -> bs.map { it to listOf(attacker) } }.toMap()
        if (blockerMap.isEmpty()) declareNoBlockers(defender) else declareBlockers(defender, blockerMap)
        passPriorityUntil(Step.END_COMBAT)
    }

    test("trample over a blocker is one trigger, not one per recipient") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Trampler")
        val blade = d.putPermanentOnBattlefield(me, "Test Charge Blade")
        d.attach(blade, attacker)
        val blocker = d.putCreatureOnBattlefield(them, "Test Bear")

        d.combat(mapOf(attacker to listOf(blocker)))

        // 2 lethal to the bear, 2 trampled over: both recipients were dealt damage.
        d.state.getBattlefield().contains(blocker) shouldBe false
        d.getLifeTotal(them) shouldBe 18
        d.charge(blade) shouldBe 1
    }

    test("damage split between two blockers is one trigger") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Brute")
        val blade = d.putPermanentOnBattlefield(me, "Test Charge Blade")
        d.attach(blade, attacker)
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.combat(mapOf(attacker to listOf(b1, b2)))

        d.state.getBattlefield().contains(b1) shouldBe false
        d.state.getBattlefield().contains(b2) shouldBe false
        d.charge(blade) shouldBe 1
    }

    test("the folded trigger is one stack object") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Brute")
        d.attach(d.putPermanentOnBattlefield(me, "Test Charge Blade"), attacker)
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")
        listOf(attacker, b1, b2).forEach(d::removeSummoningSickness)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(attacker), them)
        d.bothPass()
        d.declareBlockers(them, mapOf(b1 to listOf(attacker), b2 to listOf(attacker)))

        var guard = 0
        while (guard++ < 20 && d.state.stack.isEmpty() && d.state.step != Step.END_COMBAT) {
            if (d.state.pendingDecision != null) d.confirmCombatDamage() else d.bothPass()
        }

        d.state.stack.size shouldBe 1
    }

    test("the folded trigger carries the total damage") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Trampler")
        d.attach(d.putPermanentOnBattlefield(me, "Test Fervor Blade"), attacker)
        val blocker = d.putCreatureOnBattlefield(them, "Test Bear")

        d.combat(mapOf(attacker to listOf(blocker)))

        // 2 to the bear + 2 to the player = one 4-damage trigger; two 2-damage triggers miss the gate.
        d.getLifeTotal(them) shouldBe 18
        d.getLifeTotal(me) shouldBe 24
    }

    test("an equipped creature killed by the combat damage it dealt still triggers once") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Glass Brute")
        val blade = d.putPermanentOnBattlefield(me, "Test Charge Blade")
        d.attach(blade, attacker)
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.combat(mapOf(attacker to listOf(b1, b2)))

        // 2 + 2 from the bears kills the 4/4; the Blade, unattached by the same SBA pass, still
        // triggers off the last-known attachment — once.
        d.state.getBattlefield().contains(attacker) shouldBe false
        d.charge(blade) shouldBe 1
    }

    test("first-strike and regular combat damage steps trigger separately") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Double Striker")
        val blade = d.putPermanentOnBattlefield(me, "Test Charge Blade")
        d.attach(blade, attacker)
        val blocker = d.putCreatureOnBattlefield(them, "Test Big Wall")

        d.combat(mapOf(attacker to listOf(blocker)))

        d.charge(blade) shouldBe 2
    }

    test("unblocked damage to the player triggers once") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Brute")
        val blade = d.putPermanentOnBattlefield(me, "Test Charge Blade")
        d.attach(blade, attacker)

        d.combat(mapOf(attacker to emptyList()))

        d.getLifeTotal(them) shouldBe 16
        d.charge(blade) shouldBe 1
    }

    test("two equipped creatures each trigger their own Equipment once") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val a1 = d.putCreatureOnBattlefield(me, "Test Brute")
        val a2 = d.putCreatureOnBattlefield(me, "Test Brute")
        val blade1 = d.putPermanentOnBattlefield(me, "Test Charge Blade")
        val blade2 = d.putPermanentOnBattlefield(me, "Test Charge Blade")
        d.attach(blade1, a1)
        d.attach(blade2, a2)
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.combat(mapOf(a1 to listOf(b1, b2), a2 to emptyList()))

        d.charge(blade1) shouldBe 1
        d.charge(blade2) shouldBe 1
    }

    test("SELF: a creature's own recipient-less combat-damage trigger fires once (Drinker of Sorrow ruling)") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Sorrow Drinker")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.combat(mapOf(attacker to listOf(b1, b2)))

        d.getLifeTotal(me) shouldBe 21
    }

    test("a trigger that names its recipient still fires once per recipient") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Creature Biter")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.combat(mapOf(attacker to listOf(b1, b2)))

        d.getLifeTotal(me) shouldBe 22
    }

    test("ANY observer: once per damaging creature, not per recipient") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Test Combat Watcher")
        val a1 = d.putCreatureOnBattlefield(me, "Test Brute")
        val a2 = d.putCreatureOnBattlefield(me, "Test Trampler")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b3 = d.putCreatureOnBattlefield(them, "Test Bear")

        // a1 splits between two blockers, a2 tramples over one: four recipients, two creatures.
        d.combat(mapOf(a1 to listOf(b1, b2), a2 to listOf(b3)))

        d.getLifeTotal(them) shouldBe 18
        d.getLifeTotal(me) shouldBe 22
    }
})
