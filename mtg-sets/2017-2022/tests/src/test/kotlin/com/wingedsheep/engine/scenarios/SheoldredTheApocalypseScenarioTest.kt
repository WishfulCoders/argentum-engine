package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sheoldred, the Apocalypse — {2}{B}{B} Legendary Creature — Phyrexian Praetor, 4/5
 *   "Deathtouch
 *    Whenever you draw a card, you gain 2 life.
 *    Whenever an opponent draws a card, they lose 2 life."
 */
class SheoldredTheApocalypseScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
    }

    fun GameTestDriver.drainStack() {
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("has deathtouch") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sheoldred = d.putCreatureOnBattlefield(you, "Sheoldred, the Apocalypse")

        StateProjector().project(d.state).hasKeyword(sheoldred, Keyword.DEATHTOUCH) shouldBe true
    }

    test("an opponent who draws a card loses 2 life, and you gain nothing") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(you, "Sheoldred, the Apocalypse")

        // Opponent's turn: their draw-step draw fires the drain.
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.drainStack()

        d.getLifeTotal(opponent) shouldBe 18
        d.getLifeTotal(you) shouldBe 20
    }

    test("you gain 2 life when you draw a card") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(you, "Sheoldred, the Apocalypse")

        // Through the opponent's turn and into your next draw step.
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.drainStack()
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.drainStack()

        d.activePlayer shouldBe you
        d.getLifeTotal(you) shouldBe 22
        d.getLifeTotal(opponent) shouldBe 18
    }
})
