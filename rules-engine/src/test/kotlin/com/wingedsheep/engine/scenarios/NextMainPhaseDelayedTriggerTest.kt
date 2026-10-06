package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
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

/**
 * Engine tests for the "at the beginning of your next main phase" delayed trigger
 * (`Effects.AtBeginningOfYourNextMainPhase`, i.e. `step = PRECOMBAT_MAIN` +
 * `alsoAtSteps = [POSTCOMBAT_MAIN]` gated to the controller's turn).
 *
 * Rules pinned here:
 *  - CR 505.1: a turn has two main phases; CR 603.7: a delayed trigger fires the *next* time its
 *    event occurs, once. So the trigger fires at whichever of the controller's main phases begins
 *    first after it is created, and never again.
 *  - Mana Drain's 2020-11-10 ruling: created during your precombat main phase or your combat phase,
 *    it fires at that turn's postcombat main phase; otherwise at your next precombat main phase.
 *  - "Your": an opponent's main phases never fire it.
 */
class NextMainPhaseDelayedTriggerTest : FunSpec({

    val promise = card("Promise of the Next Main") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            effect = Effects.AtBeginningOfYourNextMainPhase(Effects.GainLife(3))
        }
    }

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(promise))
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        return Triple(driver, me, driver.getOpponent(me))
    }

    /** Cast the promise now and let it resolve, leaving the delayed trigger scheduled. */
    fun GameTestDriver.castPromise(caster: EntityId) {
        // On an opponent's turn the active player holds priority first; they pass it to the caster.
        state.priorityPlayerId?.takeIf { it != caster }?.let { passPriority(it) }
        val spell = putCardInHand(caster, "Promise of the Next Main")
        giveMana(caster, Color.BLUE, 1)
        castSpell(caster, spell).outcome shouldBe Outcome.Done
        bothPass()
        stackSize shouldBe 0
        state.delayedTriggers.size shouldBe 1
    }

    /** Advance (passing priority, auto-resolving anything that comes up) to [player]'s [step]. */
    fun GameTestDriver.advanceTo(player: EntityId, step: Step) {
        var guard = 0
        while (!(state.activePlayerId == player && state.step == step) && guard++ < 400) {
            if (state.pendingDecision != null) autoResolveDecision()
            else {
                autoSubmitCombatDeclarationIfNeeded()
                state.priorityPlayerId?.let { passPriority(it) }
            }
        }
        check(state.activePlayerId == player && state.step == step) { "did not reach $step for $player" }
    }

    /** At a main phase's start: is the delayed trigger on the stack? Resolve it if so. */
    fun GameTestDriver.resolveIfTriggered(): Boolean {
        val fired = stackSize == 1
        if (fired) bothPass()
        return fired
    }

    test("created in your upkeep: fires at this turn's precombat main phase, once") {
        val (driver, me, opp) = setup()
        driver.advanceTo(me, Step.UPKEEP)
        driver.castPromise(me)

        driver.advanceTo(me, Step.PRECOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe true
        driver.getLifeTotal(me) shouldBe 23
        driver.state.delayedTriggers.size shouldBe 0

        driver.advanceTo(me, Step.POSTCOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe false
        driver.getLifeTotal(me) shouldBe 23
    }

    test("created in your precombat main phase: fires at this turn's postcombat main phase") {
        val (driver, me, opp) = setup()
        driver.advanceTo(me, Step.PRECOMBAT_MAIN)
        driver.castPromise(me)

        driver.advanceTo(me, Step.POSTCOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe true
        driver.getLifeTotal(me) shouldBe 23

        // Consumed: neither the opponent's turn nor my next precombat main fire it again.
        driver.advanceTo(opp, Step.POSTCOMBAT_MAIN)
        driver.advanceTo(me, Step.PRECOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe false
        driver.getLifeTotal(me) shouldBe 23
    }

    test("created during your combat: fires at this turn's postcombat main phase") {
        val (driver, me, opp) = setup()
        driver.advanceTo(me, Step.BEGIN_COMBAT)
        driver.castPromise(me)

        driver.advanceTo(me, Step.POSTCOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe true
        driver.getLifeTotal(me) shouldBe 23
    }

    test("created in your postcombat main phase: waits for your next turn's precombat main phase") {
        val (driver, me, opp) = setup()
        driver.advanceTo(me, Step.POSTCOMBAT_MAIN)
        driver.castPromise(me)

        driver.advanceTo(opp, Step.PRECOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe false
        driver.advanceTo(opp, Step.POSTCOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe false
        driver.getLifeTotal(me) shouldBe 20

        driver.advanceTo(me, Step.PRECOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe true
        driver.getLifeTotal(me) shouldBe 23
    }

    test("created on an opponent's turn: their main phases don't count; yours does") {
        val (driver, me, opp) = setup()
        driver.advanceTo(opp, Step.PRECOMBAT_MAIN)
        driver.castPromise(me)

        driver.advanceTo(opp, Step.POSTCOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe false
        driver.getLifeTotal(me) shouldBe 20

        driver.advanceTo(me, Step.PRECOMBAT_MAIN)
        driver.resolveIfTriggered() shouldBe true
        driver.getLifeTotal(me) shouldBe 23
    }
})
