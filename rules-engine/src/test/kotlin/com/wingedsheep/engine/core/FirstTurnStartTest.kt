package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * How a game reaches its first turn, with and without a mulligan phase. Both paths run the
 * opening-hand actions (CR 103.6) and then begin turn 1, whose untap step gives no one priority
 * (CR 502.4): the first priority is the starting player's, in upkeep.
 */
class FirstTurnStartTest : FunSpec({

    val leyline = card("Test Leyline") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        mayBeginGameOnBattlefield()
    }

    fun driver(skipMulligans: Boolean, deck1: Deck = Deck.of("Island" to 40)): GameTestDriver =
        GameTestDriver().apply {
            registerCards(TestCards.all + leyline)
            initGame(deck1, Deck.of("Island" to 40), skipMulligans = skipMulligans)
        }

    fun GameTestDriver.moveToOpeningHand(playerId: EntityId, name: String) {
        var s = state
        val library = ZoneKey(playerId, Zone.LIBRARY)
        val hand = ZoneKey(playerId, Zone.HAND)
        val wanted = s.getLibrary(playerId).firstOrNull { s.getEntity(it)?.get<CardComponent>()?.name == name }
            ?: return // already drawn
        val outgoing = s.getHand(playerId).first()
        s = s.removeFromZone(library, wanted).addToZone(hand, wanted)
        s = s.removeFromZone(hand, outgoing).addToZone(library, outgoing)
        replaceState(s)
    }

    /** What a host does after [GameInitializer.initializeGame]. */
    fun GameTestDriver.beginGame() {
        replaceState(ActionProcessor(services).beginGame(state).state)
    }

    fun GameTestDriver.keepBoth() {
        submit(KeepHand(player1)).error shouldBe null
        submit(KeepHand(player2)).error shouldBe null
    }

    fun GameTestDriver.leylineOnBattlefield() =
        state.getBattlefield().map { state.getEntity(it)?.get<CardComponent>()?.name }

    test("skipped mulligans: the first priority is the starting player's, in turn 1's upkeep") {
        val d = driver(skipMulligans = true)
        d.beginGame()

        d.state.turnNumber shouldBe 1
        d.state.step shouldBe Step.UPKEEP
        d.state.priorityPlayerId shouldBe d.player1
        d.state.pendingDecision shouldBe null
    }

    test("beginGame is idempotent") {
        val d = driver(skipMulligans = true)
        d.beginGame()
        val begun = d.state
        d.beginGame()
        d.state shouldBe begun
    }

    test("skipped mulligans: a Leyline in the opening hand is asked about before turn 1") {
        val d = driver(skipMulligans = true, deck1 = Deck.of("Test Leyline" to 1, "Island" to 39))
        d.moveToOpeningHand(d.player1, "Test Leyline")
        d.beginGame()

        val prompt = d.pendingDecision
        prompt.shouldBeInstanceOf<YesNoDecision>()
        prompt.playerId shouldBe d.player1
        d.state.step shouldBe Step.UNTAP

        d.submitYesNo(d.player1, true).error shouldBe null
        d.leylineOnBattlefield() shouldContain "Test Leyline"
        d.state.step shouldBe Step.UPKEEP
        d.state.priorityPlayerId shouldBe d.player1
    }

    test("with mulligans: the last keep reaches turn 1's upkeep with the starting player's priority") {
        val d = driver(skipMulligans = false)
        d.beginGame()
        d.state.step shouldBe Step.UNTAP

        d.keepBoth()
        d.state.turnNumber shouldBe 1
        d.state.step shouldBe Step.UPKEEP
        d.state.priorityPlayerId shouldBe d.player1
    }

    test("with mulligans: a Leyline in the opening hand is asked about before turn 1") {
        val d = driver(skipMulligans = false, deck1 = Deck.of("Test Leyline" to 1, "Island" to 39))
        d.moveToOpeningHand(d.player1, "Test Leyline")
        d.keepBoth()

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.state.step shouldBe Step.UNTAP
        d.submitYesNo(d.player1, false).error shouldBe null
        d.leylineOnBattlefield() shouldNotContain "Test Leyline"
        d.state.step shouldBe Step.UPKEEP
    }

    test("nothing but the bottom-cards choice is accepted while a kept hand still owes its bottom") {
        val d = driver(skipMulligans = false)
        d.submit(TakeMulligan(d.player1)).error shouldBe null
        d.keepBoth()

        d.submit(PassPriority(d.player1)).error shouldBe "Opening hands are still being decided"
        d.submit(BottomCards(d.player1, listOf(d.state.getHand(d.player1).first()))).error shouldBe null
        d.state.step shouldBe Step.UPKEEP
    }
})
