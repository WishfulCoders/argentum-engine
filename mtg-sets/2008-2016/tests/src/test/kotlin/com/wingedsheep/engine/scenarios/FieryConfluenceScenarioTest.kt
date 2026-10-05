package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c15.cards.FieryConfluence
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fiery Confluence (C15 #26) — {2}{R}{R} Sorcery.
 *
 *   Choose three. You may choose the same mode more than once.
 *   • Fiery Confluence deals 1 damage to each creature.
 *   • Fiery Confluence deals 2 damage to each opponent.
 *   • Destroy target artifact.
 */
class FieryConfluenceScenarioTest : FunSpec({

    val relic = card("Test Confluence Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FieryConfluence)
        driver.registerCard(relic)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.payFor() {
        giveColorlessMana(player1, 2)
        giveMana(player1, Color.RED, 2)
    }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("damage-to-creatures twice plus burn once: 2 damage to every creature, 2 to the opponent") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        driver.putCreatureOnBattlefield(me, "Goblin Guide")
        driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        driver.putCreatureOnBattlefield(opp, "Savannah Lions")
        val spell = driver.putCardInHand(me, "Fiery Confluence")
        driver.payFor()

        driver.submit(
            CastSpell(playerId = me, cardId = spell, chosenModes = listOf(0, 0, 1))
        ).outcome shouldBe Outcome.Done
        driver.drainStack()

        driver.findPermanent(me, "Goblin Guide") shouldBe null
        driver.findPermanent(opp, "Savannah Lions") shouldBe null
        driver.findPermanent(opp, "Centaur Courser") shouldNotBe null
        driver.getLifeTotal(opp) shouldBe 18
        driver.getLifeTotal(me) shouldBe 20
    }

    test("burn mode three times: 6 damage to the opponent only") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val spell = driver.putCardInHand(me, "Fiery Confluence")
        driver.payFor()

        driver.submit(
            CastSpell(playerId = me, cardId = spell, chosenModes = listOf(1, 1, 1))
        ).outcome shouldBe Outcome.Done
        driver.drainStack()

        driver.getLifeTotal(opp) shouldBe 14
        driver.getLifeTotal(me) shouldBe 20
    }

    test("destroy-artifact twice picks two different artifacts") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val a1 = driver.putPermanentOnBattlefield(opp, "Test Confluence Relic")
        val a2 = driver.putPermanentOnBattlefield(opp, "Test Confluence Relic")
        val spell = driver.putCardInHand(me, "Fiery Confluence")
        driver.payFor()

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(a1), ChosenTarget.Permanent(a2)),
                chosenModes = listOf(1, 2, 2),
                modeTargetsOrdered = listOf(
                    emptyList(),
                    listOf(ChosenTarget.Permanent(a1)),
                    listOf(ChosenTarget.Permanent(a2))
                )
            )
        ).outcome shouldBe Outcome.Done
        driver.drainStack()

        driver.findPermanent(opp, "Test Confluence Relic") shouldBe null
        driver.getLifeTotal(opp) shouldBe 18
    }
})
