package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.YesManPersonalSecuritron
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class YesManPersonalSecuritronScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + YesManPersonalSecuritron)
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun resolve(d: GameTestDriver) {
        repeat(30) {
            if (d.pendingDecision == null && d.state.stack.isNotEmpty())
                d.passPriority(d.state.priorityPlayerId!!).error shouldBe null
        }
        d.state.stack.isEmpty() shouldBe true
        d.pendingDecision shouldBe null
    }
    fun activate(d: GameTestDriver, id: EntityId) = d.submit(ActivateAbility(
        playerId = d.player1, sourceId = id,
        abilityId = YesManPersonalSecuritron.activatedAbilities.single().id,
        targets = listOf(ChosenTarget.Player(d.player2)),
    ))
    fun bolt(d: GameTestDriver, id: EntityId) {
        val spell = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED)
        d.castSpell(d.player1, spell, listOf(id)).error shouldBe null
        d.bothPass()
    }
    fun tokens(d: GameTestDriver, player: EntityId) = d.getPermanents(player)
        .filter { d.state.getEntity(it)?.has<TokenComponent>() == true }

    test("giving control creates a separate response window before the activating player draws") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        d.removeSummoningSickness(id)
        val hand = d.getHandSize(d.player1)
        activate(d, id).error shouldBe null
        d.bothPass()
        d.state.projectedState.getController(id) shouldBe d.player2
        d.getHandSize(d.player1) shouldBe hand
        d.state.stack.size shouldBe 1
        resolve(d)
        d.getHandSize(d.player1) shouldBe hand + 2
        d.getHandSize(d.player2) shouldBe 7
        d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.QUEST) shouldBe 1
    }
    test("removing Yes Man in response prevents both the reflexive trigger and its draw") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        d.removeSummoningSickness(id)
        val hand = d.getHandSize(d.player1)
        activate(d, id).error shouldBe null
        bolt(d, id)
        resolve(d)
        d.getHandSize(d.player1) shouldBe hand
        tokens(d, d.player1).size shouldBe 0
    }
    test("removing Yes Man after transfer preserves the reflexive draw but adds no counter") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        d.removeSummoningSickness(id)
        val hand = d.getHandSize(d.player1)
        activate(d, id).error shouldBe null
        d.bothPass()
        bolt(d, id)
        resolve(d)
        d.getHandSize(d.player1) shouldBe hand + 2
        tokens(d, d.player1).size shouldBe 0
        (id in d.getGraveyard(d.player1)) shouldBe true
    }
    test("a stolen nontoken pays its owner in tapped Soldiers using departure-time quest counters") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        d.removeSummoningSickness(id)
        d.addComponent(id, ControllerComponent(d.player2))
        d.addComponent(id, CountersComponent(mapOf(CounterType.QUEST to 3)))
        bolt(d, id)
        resolve(d)
        val soldiers = tokens(d, d.player1)
        soldiers.size shouldBe 3
        tokens(d, d.player2).size shouldBe 0
        soldiers.forEach { d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
    }
    test("a stolen token copy still pays its owner after state-based cleanup removes its entity") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        d.removeSummoningSickness(id)
        d.addComponent(id, TokenComponent)
        d.addComponent(id, ControllerComponent(d.player2))
        d.addComponent(id, CountersComponent(mapOf(CounterType.QUEST to 4)))
        bolt(d, id)
        d.state.getEntity(id) shouldBe null
        resolve(d)
        val soldiers = tokens(d, d.player1)
        soldiers.size shouldBe 4
        tokens(d, d.player2).size shouldBe 0
        soldiers.forEach { d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
    }
    test("activation is prohibited on the opponent's turn") {
        val d = setup()
        val id = d.putCreatureOnBattlefield(d.player1, YesManPersonalSecuritron.name)
        d.removeSummoningSickness(id)
        d.replaceState(d.state.copy(activePlayerId = d.player2, priorityPlayerId = d.player1))
        (activate(d, id).error != null) shouldBe true
    }
})
