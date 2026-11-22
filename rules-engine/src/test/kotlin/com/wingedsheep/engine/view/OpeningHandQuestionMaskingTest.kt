package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The opening-hand Leyline question (CR 103.6) is about a card in a hidden hand. While it is open
 * and after it is answered, the opponent must not learn which card it is: no name, art or id in
 * the decision status, and no name in the logged answer. The deciding player still sees both.
 */
class OpeningHandQuestionMaskingTest : FunSpec({

    val art = "https://cards.example/test-leyline.jpg"

    val leyline = card("Test Leyline") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        mayBeginGameOnBattlefield()
    }

    /** A game paused on player 1's Leyline question, with the card's art stamped. */
    fun askedAboutLeyline(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + leyline)
            initGame(Deck.of("Test Leyline" to 1, "Island" to 39), Deck.of("Island" to 40), skipMulligans = true)
        }
        var s = d.state
        val library = ZoneKey(d.player1, Zone.LIBRARY)
        val hand = ZoneKey(d.player1, Zone.HAND)
        val card = s.getLibrary(d.player1).firstOrNull { s.getEntity(it)?.get<CardComponent>()?.name == "Test Leyline" }
        if (card != null) {
            val outgoing = s.getHand(d.player1).first()
            s = s.removeFromZone(library, card).addToZone(hand, card)
                .removeFromZone(hand, outgoing).addToZone(library, outgoing)
        }
        val leylineId = s.getHand(d.player1).first { s.getEntity(it)?.get<CardComponent>()?.name == "Test Leyline" }
        s = s.updateEntity(leylineId) { it.with(it.get<CardComponent>()!!.copy(imageUri = art)) }
        d.replaceState(ActionProcessor(d.services).beginGame(s).state)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        return d to leylineId
    }

    test("the opponent's decision status names no card, shows no art and carries no id") {
        val (d, leylineId) = askedAboutLeyline()
        val enricher = DecisionEnricher(d.cardRegistry)

        val status = enricher.opponentDecisionStatus(d.pendingDecision!!, d.state, d.player2)
        status.sourceName shouldBe null
        status.sourceImageUri shouldBe null
        status.sourceId shouldBe null

        val ownStatus = enricher.opponentDecisionStatus(d.pendingDecision!!, d.state, d.player1)
        ownStatus.sourceName shouldBe "Test Leyline"
        ownStatus.sourceId shouldBe leylineId
    }

    test("the opponent is sent the question without the card's name; its owner still sees it") {
        val (d, _) = askedAboutLeyline()
        val enricher = DecisionEnricher(d.cardRegistry)

        enricher.enrich(d.pendingDecision!!, d.state, d.player2).context.sourceName shouldBe null
        enricher.enrich(d.pendingDecision!!, d.state, d.player1).context.sourceName shouldBe "Test Leyline"
    }

    test("declining logs no card name for the opponent; the decider's own log still names it") {
        val (d, _) = askedAboutLeyline()
        val result = d.submitYesNo(d.player1, false)
        result.error shouldBe null

        fun logFor(viewer: EntityId) = ClientEventTransformer.transform(result.events, viewer)
            .filterIsInstance<ClientEvent.DecisionMade>().single().description

        logFor(d.player2) shouldBe "Opponent: Chose No"
        logFor(d.player2) shouldNotContain "Test Leyline"
        logFor(d.player1) shouldContain "(Test Leyline) Chose No"
    }
})
