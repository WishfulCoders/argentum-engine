package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.LutriTheSpellchaser
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Lutri, the Spellchaser (IKO) — flash it in with your own instant on the stack; "when Lutri
 * enters, if you cast it, copy target instant or sorcery spell you control". The copy resolves
 * first and may be pointed somewhere new (CR 707.10c).
 */
class LutriTheSpellchaserScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(LutriTheSpellchaser))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("cast with a Lightning Bolt on the stack, Lutri copies the Bolt") {
        val d = driver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)

        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        val lutri = d.putCardInHand(p1, "Lutri, the Spellchaser")
        d.giveMana(p1, Color.RED, 4)

        d.submit(
            CastSpell(p1, bolt, targets = listOf(ChosenTarget.Player(p2)), paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        val boltOnStack = d.getTopOfStack()!!

        // Flash: cast Lutri on top of the Bolt while holding priority.
        d.submit(CastSpell(p1, lutri, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done

        var guard = 0
        while (guard++ < 30 && (d.state.stack.isNotEmpty() || d.state.pendingDecision != null)) {
            val decision = d.state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                // The trigger's target (the only instant you control is the Bolt), then the copy's
                // "choose new targets" prompt — keep the opponent.
                val options = decision.legalTargets.values.flatten()
                val pick = if (boltOnStack in options) boltOnStack else p2
                d.submitTargetSelection(p1, listOf(pick)).error shouldBe null
            } else if (decision != null) {
                d.autoResolveDecision()
            } else {
                d.bothPass()
            }
        }

        d.findPermanent(p1, "Lutri, the Spellchaser").shouldNotBeNull()
        withClue("the copy and the original Bolt both hit the opponent") {
            d.getLifeTotal(p2) shouldBe 14
        }
    }
})
