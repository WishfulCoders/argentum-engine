package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.RobberOfTheRich
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Robber of the Rich (ELD #138) — {1}{R} Creature — Human Archer Rogue 2/2.
 *
 *   Reach, haste
 *   Whenever this creature attacks, if defending player has more cards in hand than you, exile
 *   the top card of their library. During any turn you attacked with a Rogue, you may cast that
 *   card and you may spend mana as though it were mana of any color to cast that spell.
 */
class RobberOfTheRichScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RobberOfTheRich))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Robber attacks the opponent; the attack trigger (if any) resolves. */
    fun GameTestDriver.attackWithRobber(robber: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(robber), player2)
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    test("defending player has more cards: top card exiled, castable with any mana this turn") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val robber = driver.putCreatureOnBattlefield(me, "Robber of the Rich")
        driver.putCardInHand(opp, "Forest") // opponent now has more cards in hand than me
        val bolt = driver.putCardOnTopOfLibrary(opp, "Lightning Bolt")

        driver.attackWithRobber(robber)

        withClue("Lightning Bolt was exiled from the opponent's library") {
            driver.getExileCardNames(opp) shouldBe listOf("Lightning Bolt")
        }

        // Still the combat of a turn we attacked with a Rogue: cast the Bolt with colorless mana.
        val lifeBefore = driver.getLifeTotal(opp)
        driver.giveColorlessMana(me, 1)
        val exiled = driver.getExile(opp).single()
        driver.castSpell(me, exiled, targets = listOf(opp)).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
        driver.getLifeTotal(opp) shouldBe lifeBefore - 3
        driver.getExile(opp).contains(bolt) shouldBe false
    }

    test("no trigger when defending player does not have more cards in hand") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val robber = driver.putCreatureOnBattlefield(me, "Robber of the Rich")
        driver.putCardInHand(me, "Forest") // I have more cards than the opponent
        driver.putCardOnTopOfLibrary(opp, "Lightning Bolt")

        driver.attackWithRobber(robber)

        driver.getExile(opp).size shouldBe 0
    }

    test("on a later turn the card is castable only after you attack with a Rogue") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val robber = driver.putCreatureOnBattlefield(me, "Robber of the Rich")
        driver.putCardInHand(opp, "Forest")
        driver.putCardOnTopOfLibrary(opp, "Lightning Bolt")
        driver.attackWithRobber(robber)
        val exiled = driver.getExile(opp).single()

        // Through the opponent's turn to my next precombat main phase.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe opp
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe me

        withClue("no Rogue has attacked this turn yet") {
            driver.giveColorlessMana(me, 1)
            driver.castSpell(me, exiled, targets = listOf(opp)).outcome shouldNotBe Outcome.Done
            driver.getExile(opp).contains(exiled) shouldBe true
        }

        driver.attackWithRobber(robber)
        withClue("after attacking with a Rogue, the exiled card is castable") {
            driver.giveColorlessMana(me, 1) // the earlier pool emptied between steps
            driver.castSpell(me, exiled, targets = listOf(opp)).outcome shouldBe Outcome.Done
        }
    }

    test("the Rogue that attacked may have left the battlefield (ruling 2019-10-04)") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val robber = driver.putCreatureOnBattlefield(me, "Robber of the Rich")
        driver.putCardInHand(opp, "Forest")
        driver.putCardOnTopOfLibrary(opp, "Lightning Bolt")
        driver.attackWithRobber(robber)
        val exiled = driver.getExile(opp).single()

        driver.moveToGraveyard(robber)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, exiled, targets = listOf(opp)).outcome shouldBe Outcome.Done
    }
})
