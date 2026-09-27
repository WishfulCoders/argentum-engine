package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.roe.cards.RaidBombardment
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Raid Bombardment (ROE #161) and the [com.wingedsheep.sdk.scripting.targets.EffectTarget.AttackedBy]
 * reference it introduced: "Whenever a creature you control with power 2 or less attacks, this
 * enchantment deals 1 damage to the player or planeswalker that creature is attacking."
 *
 * Each case is one of the card's rulings or one of the reference's resolution rules (CR 608.2h):
 * the attacked player, the attacked planeswalker itself (not its controller), one trigger per
 * attacker, power checked only on triggering, the attacker's last-known defender after it has
 * left the battlefield, and nothing at all once the attacker has been removed from combat or the
 * attacked planeswalker has gone. Battles are not modelled by the engine, so their ruling has no
 * test here.
 */
class RaidBombardmentScenarioTest : FunSpec({

    val testWalker = card("Test Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Tester"
        startingLoyalty = 3
        loyaltyAbility(1) {
            effect = Effects.GainLife(1)
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(RaidBombardment, testWalker))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(d.activePlayer!!, "Raid Bombardment")
        return d
    }

    fun GameTestDriver.attacker(name: String): EntityId =
        putCreatureOnBattlefield(activePlayer!!, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.walker(owner: EntityId): EntityId {
        val walker = putPermanentOnBattlefield(owner, "Test Walker")
        replaceState(
            state.updateEntity(walker) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(CounterType.LOYALTY, 3))
            }
        )
        return walker
    }

    fun GameTestDriver.loyalty(walker: EntityId): Int =
        state.getEntity(walker)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /** Resolve everything on the stack without leaving the declare-attackers step. */
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && guard++ < 20) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
    }

    test("a power-2-or-less attacker pings the player it attacks") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val lions = d.attacker("Savannah Lions")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(lions), opp).error shouldBe null
        d.assertStackSize(1)
        d.resolveStack()

        d.assertStep(Step.DECLARE_ATTACKERS)
        d.getLifeTotal(opp) shouldBe 19
        d.getLifeTotal(me) shouldBe 20
    }

    test("a creature with power 3 does not trigger it") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val courser = d.attacker("Centaur Courser")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(courser), opp).error shouldBe null

        d.assertStackSize(0)
        d.getLifeTotal(opp) shouldBe 20
    }

    test("it triggers once for each qualifying attacker") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val lions = listOf(d.attacker("Savannah Lions"), d.attacker("Savannah Lions"))
        val courser = d.attacker("Centaur Courser")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, lions + courser, opp).error shouldBe null
        d.resolveStack()

        d.getLifeTotal(opp) shouldBe 18
    }

    test("an attacked planeswalker takes the damage, not its controller") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val walker = d.walker(opp)
        val lions = d.attacker("Savannah Lions")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, mapOf(lions to walker)).error shouldBe null
        d.resolveStack()

        d.loyalty(walker) shouldBe 2
        d.getLifeTotal(opp) shouldBe 20
    }

    test("power raised after it triggers still deals the damage") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val lions = d.attacker("Savannah Lions")
        val growth = d.putCardInHand(me, "Giant Growth")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(lions), opp).error shouldBe null
        d.giveMana(me, Color.GREEN, 1)
        d.castSpell(me, growth, listOf(lions)).error shouldBe null
        d.resolveStack()

        d.state.projectedState.getPower(lions) shouldBe 4
        d.getLifeTotal(opp) shouldBe 19
    }

    test("an attacker killed in response still has the damage dealt to the player it was attacking") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val lions = d.attacker("Savannah Lions")
        val bolt = d.putCardInHand(opp, "Lightning Bolt")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(lions), opp).error shouldBe null
        d.passPriority(me)
        d.giveMana(opp, Color.RED, 1)
        d.castSpell(opp, bolt, listOf(lions)).error shouldBe null
        d.resolveStack()

        d.getGraveyardCardNames(me) shouldContain "Savannah Lions"
        d.getLifeTotal(opp) shouldBe 19
    }

    test("an attacker killed in response still has the damage dealt to the planeswalker it was attacking") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val walker = d.walker(opp)
        val lions = d.attacker("Savannah Lions")
        val bolt = d.putCardInHand(opp, "Lightning Bolt")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, mapOf(lions to walker)).error shouldBe null
        d.passPriority(me)
        d.giveMana(opp, Color.RED, 1)
        d.castSpell(opp, bolt, listOf(lions)).error shouldBe null
        d.resolveStack()

        d.getGraveyardCardNames(me) shouldContain "Savannah Lions"
        d.loyalty(walker) shouldBe 2
        d.getLifeTotal(opp) shouldBe 20
    }

    test("an attacker removed from combat is attacking nothing, so nothing is dealt") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val lions = d.attacker("Savannah Lions")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(lions), opp).error shouldBe null
        // What an effect that "removes it from combat" leaves behind (CR 506.4).
        d.replaceState(d.state.updateEntity(lions) { it.without<AttackingComponent>() })
        d.resolveStack()

        d.getLifeTotal(opp) shouldBe 20
    }

    test("an attacked planeswalker that has left the battlefield is not replaced by its controller") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val walker = d.walker(opp)
        val lions = d.attacker("Savannah Lions")
        val bolt = d.putCardInHand(me, "Lightning Bolt")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, mapOf(lions to walker)).error shouldBe null
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(walker)).error shouldBe null
        d.resolveStack()

        d.getGraveyardCardNames(opp) shouldContain "Test Walker"
        d.getLifeTotal(opp) shouldBe 20
    }
})
