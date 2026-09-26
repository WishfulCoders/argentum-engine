package com.wingedsheep.engine.view

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * A permanent cast for its warp cost (CR 702.185, Edge of Eternities) carries a
 * [com.wingedsheep.engine.state.components.battlefield.WarpedComponent] until it's exiled at the
 * beginning of the next end step. [ClientStateTransformer] surfaces that as [ClientCard.isWarped] so
 * the client can show the cosmic "warped" cue (a spinning ring + badge) — otherwise the permanent is
 * visually indistinguishable from one cast for its regular cost. The flag is public (warp casting is
 * visible to all players), so both the controller and the opponent see it.
 */
class WarpedCardVisibilityTest : FunSpec({

    val warpCreature = card("Warp Test Creature") {
        manaCost = "{3}{R}{R}"
        typeLine = "Creature — Elemental"
        power = 4
        toughness = 3
        warp = "{1}{R}"
        keywords(Keyword.HASTE)
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + warpCreature)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    test("a creature cast for its regular cost is not flagged isWarped") {
        val d = driver()
        val player = d.activePlayer!!
        val cardId = d.putCardInHand(player, "Warp Test Creature")
        d.giveMana(player, Color.RED, 5)

        d.submit(CastSpell(playerId = player, cardId = cardId, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()

        val permanent = d.findPermanent(player, "Warp Test Creature").shouldNotBeNull()
        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        view.cards[permanent].shouldNotBeNull().isWarped shouldBe false
    }

    test("a creature cast for its warp cost is flagged isWarped for both players") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val cardId = d.putCardInHand(player, "Warp Test Creature")
        d.giveMana(player, Color.RED, 2) // warp cost {1}{R}

        d.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        d.bothPass()

        val permanent = d.findPermanent(player, "Warp Test Creature").shouldNotBeNull()

        val ownerView = transformer(d).transform(d.state, viewingPlayerId = player)
        val opponentView = transformer(d).transform(d.state, viewingPlayerId = opponent)

        ownerView.cards[permanent].shouldNotBeNull().isWarped shouldBe true
        opponentView.cards[permanent].shouldNotBeNull().isWarped shouldBe true
    }
})
