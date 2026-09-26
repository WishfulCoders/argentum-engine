package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Regression for the Goblin Fireleaper bug: a dies trigger that "deals damage equal to its power"
 * ([DynamicAmounts.sourcePower]) rendered "Deal 0 damage to target" on the stack, even though the
 * resolved damage was right.
 *
 * Root cause: when a *targeted* triggered ability's continuation was resumed into its on-stack
 * component ([com.wingedsheep.engine.handlers.continuations.EffectAndTriggerContinuationResumer]),
 * the captured `lastKnownPower`/`lastKnownToughness` (CR 608.2h / 113.7a — an effect that needs
 * info from a source no longer in its zone uses the source's last known information; 603.10 makes
 * the dies trigger itself look back in time) were dropped. With no LKI on the component:
 *  - execution (source-id = the dead card) still fell back to the card's *printed* base power, so
 *    an un-pumped creature dealt the right number by luck — but a buffed creature would deal its
 *    base power, ignoring counters/pumps;
 *  - the client stack text (source-id = the ability's own stack entity, which has no card stats)
 *    fell all the way through to 0.
 *
 * A +1/+1 counter makes last-known power (4) differ from printed power (3), so both the dealt
 * damage and the rendered text must reflect 4.
 */
class DiesTriggerSourcePowerStackTextTest : FunSpec({

    val Fireleaper = card("LKI Fireleaper") {
        manaCost = "{0}"; typeLine = "Creature — Goblin"; power = 3; toughness = 3
        triggeredAbility {
            trigger = Triggers.self.dies()
            val tgt = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.sourcePower(), tgt)
        }
    }

    val Bolt = card("LKI Lethal Bolt") {
        manaCost = "{0}"; typeLine = "Sorcery"; oracleText = "Deal 9 damage to target creature."
        spell {
            val c = target(TargetFilter.Creature)
            effect = Effects.DealDamage(9, c)
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Fireleaper, Bolt))
    }

    test("dies trigger 'deals damage equal to its power' shows the real power on the stack, not 0") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val fireleaper = d.putCreatureOnBattlefield(active, "LKI Fireleaper")
        // +1/+1 counter: last-known power becomes 4, distinct from the printed 3.
        d.addComponent(
            fireleaper,
            CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))
        )

        // Kill it with a sorcery; its dies trigger then asks for a target.
        val bolt = d.putCardInHand(active, "LKI Lethal Bolt")
        d.castSpell(active, bolt, listOf(fireleaper))
        d.bothPass() // resolve the bolt -> Fireleaper dies -> dies trigger pauses for a target

        (fireleaper !in d.state.getBattlefield()) shouldBe true

        // Aim the dies trigger at the opponent player so the resolved amount is observable as life loss.
        val oppLifeBefore = d.getLifeTotal(opp)
        d.submitTargetSelection(active, listOf(opp))

        // The triggered ability is now on the stack — render it and assert the resolved power.
        val stackId = d.state.stack.first()
        val view = ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            .transform(d.state, viewingPlayerId = active)
        val stackCard = view.cards[stackId]
        stackCard.shouldNotBeNull()
        stackCard.oracleText shouldContain "4"
        stackCard.oracleText shouldNotContain "0 damage"

        // And the resolved damage matches the rendered amount.
        d.bothPass()
        d.getLifeTotal(opp) shouldBe oppLifeBefore - 4
    }
})
