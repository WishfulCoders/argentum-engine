package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.combat.CombatRemovalHelper
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * A blocked creature stays blocked after every creature blocking it leaves combat (CR 509.1h).
 * The client used to infer "blocked" from `blockedBy` being non-empty, so once the blocker was
 * sacrificed or removed the attacker looked unblocked. `isBlocked` carries the real status.
 */
class BlockedWithoutBlockersProjectionTest : FunSpec({

    fun attacker(driver: GameTestDriver, viewer: EntityId, id: EntityId): ClientAttacker =
        ClientStateTransformer(cardRegistry = driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(driver.state, viewingPlayerId = viewer)
            .combat!!.attackers.single { it.creatureId == id }

    test("an attacker whose only blocker left combat is projected as blocked with no blockers") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        val bear = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        driver.removeSummoningSickness(bear)
        val blocker = driver.putCreatureOnBattlefield(defender, "Llanowar Elves")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(bear), defender).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(blocker to listOf(bear))).outcome shouldBe Outcome.Done

        attacker(driver, active, bear).isBlocked shouldBe true
        attacker(driver, active, bear).blockedBy shouldBe listOf(blocker)

        driver.replaceState(CombatRemovalHelper.removeFromCombat(driver.state, blocker))

        for (viewer in listOf(active, defender)) {
            val projected = attacker(driver, viewer, bear)
            projected.blockedBy.shouldBeEmpty()
            projected.isBlocked shouldBe true
        }
    }

    test("an unblocked attacker is projected as not blocked") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val defender = driver.getOpponent(active)
        val bear = driver.putCreatureOnBattlefield(active, "Centaur Courser")
        driver.removeSummoningSickness(bear)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(active, listOf(bear), defender).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, emptyMap()).outcome shouldBe Outcome.Done

        attacker(driver, active, bear).isBlocked shouldBe false
    }
})
