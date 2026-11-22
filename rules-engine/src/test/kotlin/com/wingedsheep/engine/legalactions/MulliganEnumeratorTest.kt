package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.BottomCards
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.TakeMulligan
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/** Before the first turn the enumerator offers the opening-hand decisions (CR 103.5) and nothing else. */
class MulliganEnumeratorTest : FunSpec({

    fun driver(deck: Deck = Deck.of("Island" to 20, "Forest" to 20)): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initGame(deck, deck, skipMulligans = false)
    }

    fun GameTestDriver.offers(player: com.wingedsheep.sdk.model.EntityId) =
        services.legalActionEnumerator.enumerate(state, player)

    test("each undecided player is offered keep and mulligan, and every offer is accepted") {
        val d = driver()
        val processor = ActionProcessor(d.services)
        for (player in listOf(d.player1, d.player2)) {
            val offers = d.offers(player)
            offers.map { it.action } shouldContainExactlyInAnyOrder listOf(KeepHand(player), TakeMulligan(player))
            offers.forEach { processor.validate(d.state, it.action) shouldBe null }
        }
    }

    test("a player who kept is offered nothing while another still decides") {
        val d = driver()
        d.submit(KeepHand(d.player1)).error shouldBe null
        d.offers(d.player1).shouldBeEmpty()
        d.offers(d.player2).map { it.action } shouldContainExactlyInAnyOrder
            listOf(KeepHand(d.player2), TakeMulligan(d.player2))
    }

    test("after keeping a mulligan, each distinct choice of cards to bottom is offered once") {
        val d = driver()
        d.submit(TakeMulligan(d.player1)).error shouldBe null
        d.submit(TakeMulligan(d.player1)).error shouldBe null
        d.submit(KeepHand(d.player1)).error shouldBe null
        d.submit(KeepHand(d.player2)).error shouldBe null

        val names = d.state.getHand(d.player1).map { d.getCardName(it) }
        val islands = names.count { it == "Island" }
        val forests = names.count { it == "Forest" }
        // Two cards from Islands and Forests: II, IF, FF — each only if the hand holds enough.
        val expected = listOf(islands >= 2, islands >= 1 && forests >= 1, forests >= 2).count { it }

        val offers = d.offers(d.player1)
        offers.size shouldBe expected
        val processor = ActionProcessor(d.services)
        offers.forEach {
            (it.action as BottomCards).cardIds.size shouldBe 2
            processor.validate(d.state, it.action) shouldBe null
        }
        d.offers(d.player2).shouldBeEmpty()

        d.submit(offers.first().action).error shouldBe null
        d.state.getHand(d.player1).size shouldBe 5
        d.state.step shouldBe Step.UPKEEP
        d.offers(d.player1).none { it.action is KeepHand || it.action is BottomCards } shouldBe true
    }
})
