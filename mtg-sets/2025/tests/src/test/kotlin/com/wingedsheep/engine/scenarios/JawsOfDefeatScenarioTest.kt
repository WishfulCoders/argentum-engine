package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.Overkill
import com.wingedsheep.mtg.sets.definitions.tdc.cards.JawsOfDefeat
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Jaws of Defeat — {3}{B} Enchantment
 * "Whenever a creature you control enters, target opponent loses life equal to the difference
 * between that creature's power and its toughness."
 *
 * Pins the two things the script alone can't show: the difference is absolute (ruling 2025-04-04),
 * and the creature's P/T is read on resolution with last-known information — the Overkill combo
 * (-0/-9999 on the creature in response to the trigger) kills it, and the drain still uses the
 * 3/-9996 it died as.
 */
class JawsOfDefeatScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(JawsOfDefeat, Overkill))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.advanceToPlayer1Main() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety++ < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
    }

    /** Resolve the creature spell and put Jaws's trigger on the stack, targeting [opponent]. */
    fun GameTestDriver.resolveCreatureUntilTriggerOnStack(opponent: EntityId) {
        bothPass()
        val decision = pendingDecision
        if (isPaused && decision is ChooseTargetsDecision) {
            submitTargetSelection(decision.playerId, listOf(opponent))
        }
        stackSize shouldBe 1 // the Jaws trigger; the creature spell has resolved
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (guard++ < 20 && state.stack.isNotEmpty()) bothPass()
    }

    test("drains for the absolute difference — a 2/1 costs the opponent 1 life") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.player2
        driver.putPermanentOnBattlefield(me, "Jaws of Defeat")
        driver.advanceToPlayer1Main()

        val guide = driver.putCardInHand(me, "Goblin Guide")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, guide).outcome shouldBe Outcome.Done
        driver.resolveCreatureUntilTriggerOnStack(opp)
        driver.resolveStack()

        driver.getLifeTotal(opp) shouldBe 19
        driver.getLifeTotal(me) shouldBe 20
    }

    test("a creature with equal power and toughness drains nothing") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.player2
        driver.putPermanentOnBattlefield(me, "Jaws of Defeat")
        driver.advanceToPlayer1Main()

        val courser = driver.putCardInHand(me, "Centaur Courser")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, courser).outcome shouldBe Outcome.Done
        driver.resolveCreatureUntilTriggerOnStack(opp)
        driver.resolveStack()

        driver.getLifeTotal(opp) shouldBe 20
    }

    test("Overkill in response kills the creature, and the drain uses its last-known 3/-9996") {
        val driver = createDriver()
        val me = driver.player1
        val opp = driver.player2
        driver.putPermanentOnBattlefield(me, "Jaws of Defeat")
        driver.advanceToPlayer1Main()

        val courser = driver.putCardInHand(me, "Centaur Courser")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, courser).outcome shouldBe Outcome.Done
        driver.resolveCreatureUntilTriggerOnStack(opp)
        val courserOnBattlefield = driver.findPermanent(me, "Centaur Courser")!!

        // In response to the trigger: Overkill the creature that caused it.
        val overkill = driver.putCardInHand(me, "Overkill")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, overkill, targets = listOf(courserOnBattlefield)).outcome shouldBe Outcome.Done
        driver.getTopOfStackName() shouldBe "Overkill"

        // Overkill resolves; the 3/-9996 creature dies to state-based actions with the trigger still waiting.
        driver.bothPass()
        driver.findPermanent(me, "Centaur Courser") shouldBe null
        driver.getGraveyardCardNames(me).contains("Centaur Courser") shouldBe true
        driver.stackSize shouldBe 1 // the Jaws trigger is still waiting

        driver.resolveStack()

        // |3 - (3 - 9999)| = 9999.
        driver.getLifeTotal(opp) shouldBe 20 - 9999
    }
})
