package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Imp's Mischief (PLC #72).
 *
 * "{1}{B} Instant
 *  Change the target of target spell with a single target. You lose life equal to that spell's
 *  mana value."
 *
 * The new target is chosen as Imp's Mischief resolves, and the life loss is the redirected
 * spell's mana value.
 */
class ImpsMischiefScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("redirects a single-target spell to a new target and the caster loses life equal to its mana value") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putPermanentOnBattlefield(activePlayer, "Grizzly Bears")
        val lion = driver.putPermanentOnBattlefield(activePlayer, "Savannah Lions")

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        val mischief = driver.putCardInHand(activePlayer, "Imp's Mischief")
        driver.giveMana(opponent, Color.RED, 1)
        driver.giveMana(activePlayer, Color.BLACK, 2)

        driver.passPriority(activePlayer)
        driver.submit(
            CastSpell(opponent, bolt, targets = listOf(ChosenTarget.Permanent(bear)), paymentStrategy = PaymentStrategy.FromPool)
        )
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        driver.submit(
            CastSpell(activePlayer, mischief, targets = listOf(ChosenTarget.Spell(boltOnStack)), paymentStrategy = PaymentStrategy.FromPool)
        )
        val lifeBefore = driver.getLifeTotal(activePlayer)
        driver.bothPass() // Imp's Mischief resolves, prompting a new target for the bolt

        driver.submitCardSelection(activePlayer, listOf(lion))
        driver.bothPass() // bolt resolves against the new target

        driver.assertPermanentExists(activePlayer, "Grizzly Bears", "the original target should have survived")
        driver.assertInGraveyard(activePlayer, "Savannah Lions")

        // Lightning Bolt has mana value 1.
        driver.getLifeTotal(activePlayer) shouldBe lifeBefore - 1
    }

})
