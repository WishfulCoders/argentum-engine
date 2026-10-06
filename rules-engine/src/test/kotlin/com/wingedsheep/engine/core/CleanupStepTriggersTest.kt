package com.wingedsheep.engine.core

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The cleanup step's exception to "no player receives priority" (CR 514.3 / 514.3a): when a
 * triggered ability is waiting — including one that triggers "at the beginning of the next cleanup
 * step" — it is put on the stack, the active player gets priority, and once the stack is empty and
 * all players pass another cleanup step begins. The turn ends only from a cleanup step in which
 * nothing was waiting.
 */
class CleanupStepTriggersTest : FunSpec({

    val chime = card("Cleanup Chime") {
        manaCost = "{W}"
        typeLine = "Instant"
        oracleText = "At the beginning of the next cleanup step, you gain 3 life."
        spell { effect = Effects.CreateDelayedTrigger(Effects.GainLife(3), step = Step.CLEANUP) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + chime)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castChime(player: EntityId) {
        val card = putCardInHand(player, "Cleanup Chime")
        giveMana(player, Color.WHITE)
        castSpell(player, card).error shouldBe null
    }

    test("a delayed trigger at the beginning of the next cleanup step stops the step and gives the active player priority") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val turn = driver.state.turnNumber
        driver.castChime(player)
        driver.bothPass()

        driver.passPriorityUntil(Step.CLEANUP)
        driver.state.turnNumber shouldBe turn
        driver.state.priorityPlayerId shouldBe player
        driver.stackSize shouldBe 1

        driver.bothPass()
        driver.getLifeTotal(player) shouldBe 23
        // Still the same cleanup step: the trigger resolved, the active player has priority again.
        driver.state.step shouldBe Step.CLEANUP
        driver.state.activePlayerId shouldBe player
        driver.state.priorityPlayerId shouldBe player

        // Stack empty, everyone passes: another cleanup step begins, nothing waits, the turn ends.
        driver.bothPass()
        driver.state.activePlayerId shouldBe driver.getOpponent(player)
        driver.state.turnNumber shouldBe turn + 1
    }

    test("a cleanup trigger created in the cleanup step's priority window fires in the next cleanup step") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val turn = driver.state.turnNumber
        driver.castChime(player)
        driver.bothPass()
        driver.passPriorityUntil(Step.CLEANUP)
        driver.stackSize shouldBe 1

        // In response to the first trigger, set up a second "next cleanup step".
        driver.castChime(player)
        driver.bothPass() // the second Chime resolves
        driver.bothPass() // the first trigger resolves
        driver.getLifeTotal(player) shouldBe 23
        driver.state.step shouldBe Step.CLEANUP

        // The stack is empty and both pass: a new cleanup step begins and the second trigger fires.
        driver.bothPass()
        driver.state.turnNumber shouldBe turn
        driver.state.step shouldBe Step.CLEANUP
        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(player) shouldBe 26

        driver.bothPass()
        driver.state.turnNumber shouldBe turn + 1
        driver.getLifeTotal(player) shouldBe 26
    }

    test("the hand-size discard comes first; the waiting trigger is put on the stack once it is done") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.castChime(player)
        driver.bothPass()
        driver.getHand(player).forEach { driver.moveToGraveyard(it) }
        repeat(8) { driver.putCardInHand(player, "Plains") }

        driver.passPriorityUntil(Step.CLEANUP)
        val discard = driver.state.pendingDecision
        discard shouldNotBe null
        driver.submitCardSelection(player, listOf(driver.getHand(player).first()))

        driver.getHandSize(player) shouldBe 7
        driver.state.step shouldBe Step.CLEANUP
        driver.state.priorityPlayerId shouldBe player
        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(player) shouldBe 23
        driver.bothPass()
        driver.state.activePlayerId shouldBe driver.getOpponent(player)
    }

    test("a cleanup trigger that can't fire until a later turn does not hold up this turn's cleanup") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val turn = driver.state.turnNumber
        driver.registerCard(card("Next Turn Cleanup Chime") {
            manaCost = "{W}"
            typeLine = "Instant"
            spell {
                effect = Effects.CreateDelayedTrigger(
                    Effects.GainLife(3),
                    step = Step.CLEANUP,
                    timing = com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming.NEXT_TURN
                )
            }
        })
        val card = driver.putCardInHand(player, "Next Turn Cleanup Chime")
        driver.giveMana(player, Color.WHITE)
        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()

        // This turn's cleanup gives no priority: the turn passes straight to the opponent.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.activePlayerId shouldBe opponent
        driver.state.turnNumber shouldBe turn + 1
        driver.getLifeTotal(player) shouldBe 20

        // The next turn's cleanup step stops for it — after the opponent, who drew a card this
        // turn, discards down to seven.
        driver.passPriorityUntil(Step.CLEANUP)
        driver.state.activePlayerId shouldBe opponent
        if (driver.state.pendingDecision != null) {
            driver.submitCardSelection(opponent, listOf(driver.getHand(opponent).first()))
        }
        driver.state.priorityPlayerId shouldBe opponent
        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(player) shouldBe 23
    }
})
