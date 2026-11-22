package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.SecuritronSquadron
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SecuritronSquadronScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + SecuritronSquadron)
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("three simultaneous squad copies each receive four counters including their own trigger") {
        val d = setup()
        repeat(11) { d.putLandOnBattlefield(d.player1, "Plains") }
        val card = d.putCardInHand(d.player1, SecuritronSquadron.name)
        d.submit(CastSpell(d.player1, card, declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 3)).error shouldBe null
        d.bothPass()
        d.bothPass()
        repeat(30) { if (d.state.stack.isNotEmpty()) d.bothPass() }
        d.pendingDecision shouldBe null
        val tokens = d.getPermanents(d.player1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
        tokens.size shouldBe 3
        tokens.forEach {
            d.state.getEntity(it)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
        }
        (d.state.getEntity(card)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
    test("squad copies survive the original dying before resolution and each receive three counters") {
        val d = setup()
        repeat(11) { d.putLandOnBattlefield(d.player1, "Plains") }
        val card = d.putCardInHand(d.player1, SecuritronSquadron.name)
        d.submit(CastSpell(d.player1, card, declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 3)).error shouldBe null
        d.bothPass()
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, bolt, listOf(card)).error shouldBe null
        d.bothPass()
        (card in d.getGraveyard(d.player1)) shouldBe true
        repeat(30) { if (d.state.stack.isNotEmpty()) d.bothPass() }
        d.pendingDecision shouldBe null
        d.state.stack.isEmpty() shouldBe true
        val tokens = d.getPermanents(d.player1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
        tokens.size shouldBe 3
        tokens.forEach {
            d.state.getEntity(it)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }
    }
    test("without squad payment the nontoken Squadron gets no counter on entry") {
        val d = setup()
        repeat(2) { d.putLandOnBattlefield(d.player1, "Plains") }
        val card = d.putCardInHand(d.player1, SecuritronSquadron.name)
        d.castSpell(d.player1, card).error shouldBe null
        d.bothPass()
        d.state.stack.isEmpty() shouldBe true
        (d.state.getEntity(card)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
})
