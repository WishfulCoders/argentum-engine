package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.ManaDrain
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mana Drain (LEG #65): "Counter target spell. At the beginning of your next main phase, add an
 * amount of {C} equal to that spell's mana value."
 *
 * Exercises the "next main phase" delayed trigger: on an opponent's turn the mana comes in your next
 * precombat main phase; cast during your own precombat main phase it comes in that turn's
 * postcombat main phase (2020-11-10 ruling). The amount is the countered spell's mana value, read
 * before the spell leaves the stack.
 */
class ManaDrainScenarioTest : FunSpec({

    fun newDriver(startingPlayer: Int): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ManaDrain))
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20, startingPlayer = startingPlayer)
        return driver
    }

    fun GameTestDriver.colorless(player: EntityId) = state.getEntity(player)?.get<ManaPoolComponent>()?.colorless ?: 0

    fun GameTestDriver.advanceTo(player: EntityId, step: Step) {
        var guard = 0
        while (!(state.activePlayerId == player && state.step == step) && guard++ < 400) {
            if (state.pendingDecision != null) autoResolveDecision()
            else {
                autoSubmitCombatDeclarationIfNeeded()
                state.priorityPlayerId?.let { passPriority(it) }
            }
        }
        check(state.activePlayerId == player && state.step == step) { "did not reach $step" }
    }

    fun GameTestDriver.drain(me: EntityId, spell: EntityId) {
        val drain = putCardInHand(me, "Mana Drain")
        giveMana(me, Color.BLUE, 2)
        castSpellWithTargets(me, drain, listOf(ChosenTarget.Spell(spell))).outcome shouldBe Outcome.Done
    }

    test("countering on the opponent's turn adds {C} equal to its mana value in my next precombat main") {
        val driver = newDriver(startingPlayer = 1)
        val opp = driver.activePlayer!!
        val me = driver.getOpponent(opp)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCardInHand(opp, "Grizzly Bears")
        driver.giveMana(opp, Color.GREEN, 2)
        driver.castSpell(opp, bears).outcome shouldBe Outcome.Done
        driver.passPriority(opp)
        driver.drain(me, bears)
        driver.bothPass()
        driver.stackSize shouldBe 0
        driver.assertInGraveyard(opp, "Grizzly Bears")

        // Not on the opponent's postcombat main phase — it is *my* next main phase.
        driver.advanceTo(opp, Step.POSTCOMBAT_MAIN)
        driver.stackSize shouldBe 0

        driver.advanceTo(me, Step.PRECOMBAT_MAIN)
        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.colorless(me) shouldBe 2
    }

    test("cast during my precombat main phase, the mana comes in this turn's postcombat main phase") {
        val driver = newDriver(startingPlayer = 0)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // I cast a creature; the opponent responds with Lightning Bolt; I Mana Drain the Bolt.
        val myBears = driver.putCardInHand(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, myBears).outcome shouldBe Outcome.Done
        driver.passPriority(me)
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.giveMana(opp, Color.RED, 1)
        driver.castSpell(opp, bolt, listOf(me)).outcome shouldBe Outcome.Done
        driver.passPriority(opp)
        driver.drain(me, bolt)
        driver.bothPass() // Mana Drain resolves: Bolt countered, delayed trigger scheduled
        driver.bothPass() // Grizzly Bears resolves
        driver.stackSize shouldBe 0
        driver.getLifeTotal(me) shouldBe 20

        driver.advanceTo(me, Step.POSTCOMBAT_MAIN)
        driver.stackSize shouldBe 1
        driver.bothPass()
        driver.colorless(me) shouldBe 1

        // One-shot: nothing more at my next precombat main phase.
        driver.advanceTo(me, Step.PRECOMBAT_MAIN)
        driver.stackSize shouldBe 0
        driver.state.delayedTriggers.size shouldBe 0
    }
})
