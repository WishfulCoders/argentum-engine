package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c18.cards.CovetedJewel
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Coveted Jewel (C18 #54) — ETB draw three; {T}: three mana of one color; "Whenever one or more
 * creatures an opponent controls attack you and aren't blocked, that player draws three cards and
 * gains control of this artifact. Untap it."
 *
 * The trigger's own rules (batching, planeswalker attacks, per-player fan-out) are pinned by
 * `AttackYouUnblockedTriggerTest`; here the card's whole payoff is checked: the attacker draws
 * three, takes the Jewel, and it comes back untapped so they can use it at once — and the Jewel
 * goes home the same way when its original owner gets through.
 */
class CovetedJewelScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + CovetedJewel)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("entering draws three cards") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val jewel = driver.putCardInHand(me, "Coveted Jewel")
        driver.giveColorlessMana(me, 6)
        val handBefore = driver.getHandSize(me)
        driver.castSpell(me, jewel).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the artifact spell
        driver.bothPass() // resolve the enters trigger
        driver.getHandSize(me) shouldBe handBefore - 1 + 3
    }

    test("an unblocked attacker's controller draws three and takes the Jewel, untapped") {
        val driver = newDriver()
        val attacker = driver.activePlayer!!
        val me = driver.getOpponent(attacker)
        val jewel = driver.putPermanentOnBattlefield(me, "Coveted Jewel")
        driver.tapPermanent(jewel)
        val bear = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(bear)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, mapOf(bear to me))
        driver.bothPass()
        driver.declareNoBlockers(me)
        driver.stackSize shouldBe 1
        val handBefore = driver.getHandSize(attacker)
        driver.bothPass()

        driver.getHandSize(attacker) shouldBe handBefore + 3
        withClue("the attacking player gains control of the Jewel, and it is untapped") {
            driver.state.projectedState.getController(jewel) shouldBe attacker
            driver.isTapped(jewel) shouldBe false
        }
    }

    test("it changes hands back when the new controller is attacked and doesn't block") {
        val driver = newDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        val jewel = driver.putPermanentOnBattlefield(p2, "Coveted Jewel")
        val p1Bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(p1Bear)

        fun attackWith(active: EntityId, creature: EntityId, defender: EntityId) {
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(active, mapOf(creature to defender))
            driver.bothPass()
            driver.declareNoBlockers(defender)
            driver.bothPass()
        }

        attackWith(p1, p1Bear, p2)
        driver.state.projectedState.getController(jewel) shouldBe p1

        // p2's turn: p2 attacks p1, who now controls the Jewel.
        driver.passPriorityUntil(Step.END)
        var guard = 0
        while (driver.activePlayer != p2 && guard++ < 50) driver.bothPass()
        val p2Bear = driver.putCreatureOnBattlefield(p2, "Grizzly Bears")
        driver.removeSummoningSickness(p2Bear)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        attackWith(p2, p2Bear, p1)

        driver.state.projectedState.getController(jewel) shouldBe p2
    }
})
