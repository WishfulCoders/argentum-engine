package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.WarrenWarleader
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Warren Warleader (BLB #38) — "Whenever you attack, choose one — • Create a 1/1 white Rabbit
 * creature token that's tapped and attacking. • Attacking creatures you control get +1/+1."
 *
 * Regression: when Warren Warleader itself stayed home, the Rabbit token's defender fell back
 * to the trigger's player — the attacker — so the token attacked its own controller and the
 * opponent could not block it.
 */
class WarrenWarleaderScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WarrenWarleader))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    /** Declare [attackers], pick the token mode, and let the trigger resolve; returns the Rabbit. */
    fun GameTestDriver.attackAndMakeRabbit(
        attacker: EntityId,
        defender: EntityId,
        attackers: List<EntityId>
    ): EntityId {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attacker, attackers, defender)
        var safety = 0
        while (findPermanent(attacker, "Rabbit Token") == null && safety++ < 10) {
            val decision = pendingDecision
            if (decision is ChooseOptionDecision) {
                submitDecision(attacker, OptionChosenResponse(decision.id, 0))
            } else {
                bothPass()
            }
        }
        return findPermanent(attacker, "Rabbit Token")!!
    }

    test("the Rabbit token attacks the opponent and can be blocked when Warleader stays home") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        driver.putCreatureOnBattlefield(attacker, "Warren Warleader")
        val bears = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        val blocker = driver.putCreatureOnBattlefield(defender, "Centaur Courser")
        driver.removeSummoningSickness(blocker)

        val rabbit = driver.attackAndMakeRabbit(attacker, defender, listOf(bears))

        driver.state.getEntity(rabbit)?.get<AttackingComponent>()?.defenderId shouldBe defender

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(blocker to listOf(rabbit)))
        driver.state.getEntity(rabbit)?.has<BlockedComponent>() shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.findPermanent(attacker, "Rabbit Token") shouldBe null
        driver.assertLifeTotal(defender, 18)
    }

    test("the Rabbit token joins Warleader's attack when Warleader attacks") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val warleader = driver.putCreatureOnBattlefield(attacker, "Warren Warleader")
        driver.removeSummoningSickness(warleader)

        val rabbit = driver.attackAndMakeRabbit(attacker, defender, listOf(warleader))

        driver.state.getEntity(rabbit)?.get<AttackingComponent>() shouldNotBe null
        driver.state.getEntity(rabbit)?.get<AttackingComponent>()?.defenderId shouldBe defender
    }
})
