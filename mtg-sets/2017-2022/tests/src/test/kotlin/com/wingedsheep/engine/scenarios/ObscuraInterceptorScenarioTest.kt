package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.ObscuraInterceptor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Obscura Interceptor (SNC #209) — {1}{W}{U}{B} Creature — Octopus Wizard 3/1.
 *   Flash. Lifelink.
 *   When this creature enters, it connives. When it connives this way, return up to one target
 *   spell to its owner's hand.
 *
 * Proves the reflexive shape: the spell target is chosen only after the connive's draw and discard,
 * the chosen spell goes back to its owner's hand instead of resolving, and declining the "up to one"
 * target leaves the spell on the stack to resolve normally.
 */
class ObscuraInterceptorScenarioTest : FunSpec({

    data class Setup(val d: GameTestDriver, val interceptor: EntityId, val bolt: EntityId, val toDiscard: EntityId)

    /**
     * Cast Lightning Bolt at the opponent, hold priority and flash in Obscura Interceptor, then resolve
     * the Interceptor and its ETB trigger up to the connive's discard.
     */
    fun flashInOverBolt(): Setup {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ObscuraInterceptor)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), skipMulligans = true)
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bolt = d.putCardInHand(you, "Lightning Bolt")
        val interceptor = d.putCardInHand(you, "Obscura Interceptor")
        val toDiscard = d.putCardInHand(you, "Grizzly Bears")

        d.giveMana(you, Color.RED, 1)
        d.submit(
            CastSpell(
                you, bolt,
                targets = listOf(ChosenTarget.Player(d.getOpponent(you))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done

        d.giveMana(you, Color.WHITE, 2)
        d.giveMana(you, Color.BLUE, 1)
        d.giveMana(you, Color.BLACK, 1)
        d.submit(
            CastSpell(playerId = you, cardId = interceptor, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done

        d.bothPass() // resolve Obscura Interceptor; its ETB trigger goes on the stack above the Bolt
        d.bothPass() // resolve the trigger: draw, then pause on the connive discard

        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        return Setup(d, interceptor, bolt, toDiscard)
    }

    fun Setup.discardThenChooseTarget(chosen: List<EntityId>) {
        val discard = d.pendingDecision as SelectCardsDecision
        d.submitDecision(d.player1, CardsSelectedResponse(discard.id, listOf(toDiscard)))

        // Only now — after the connive — does the reflexive trigger ask for its spell target.
        val targeting = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targeting.legalTargets[0]!!.contains(bolt) shouldBe true
        d.submitDecision(d.player1, TargetsResponse(targeting.id, mapOf(0 to chosen)))
    }

    fun Setup.resolveStack() {
        var guard = 0
        while (guard++ < 10 && d.state.stack.isNotEmpty()) d.bothPass()
    }

    fun Setup.plusOneCounters(): Int =
        d.state.getEntity(interceptor)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("after conniving, the targeted spell returns to its owner's hand instead of resolving") {
        val s = flashInOverBolt()
        s.discardThenChooseTarget(listOf(s.bolt))
        s.resolveStack()

        s.plusOneCounters() shouldBe 1 // discarded a nonland card
        s.d.getHand(s.d.player1).contains(s.bolt) shouldBe true
        s.d.getLifeTotal(s.d.player2) shouldBe 20
    }

    test("choosing no target leaves the spell to resolve") {
        val s = flashInOverBolt()
        s.discardThenChooseTarget(emptyList())
        s.resolveStack()

        s.d.getGraveyard(s.d.player1).contains(s.bolt) shouldBe true
        s.d.getLifeTotal(s.d.player2) shouldBe 17
    }
})
