package com.wingedsheep.engine.view

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * An active Paradigm card (Secrets of Strixhaven) sits face-up in exile carrying a
 * [com.wingedsheep.engine.state.components.battlefield.ParadigmComponent]: it recasts a free copy of
 * itself each of its owner's precombat main phases. [ClientStateTransformer] surfaces that as
 * [ClientCard.isParadigm] so the client can show it in a dedicated public pile — otherwise it's
 * indistinguishable from any other exiled card. Paradigm exiles the card face-up (public
 * information), so the flag is visible to both players.
 */
class ParadigmCardVisibilityTest : FunSpec({

    // "Research Seminar" — Sorcery — Lesson: gain 2 life, then exile + recur via Paradigm.
    val researchSeminar = card("Research Seminar") {
        manaCost = "{1}{U}"
        typeLine = "Sorcery — Lesson"
        spell {
            effect = Effects.GainLife(2)
            paradigm()
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(researchSeminar))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    test("a Paradigm card in hand (not yet exiled) is not flagged isParadigm") {
        val d = driver()
        val player = d.activePlayer!!
        val seminar = d.putCardInHand(player, "Research Seminar")

        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        view.cards[seminar]?.isParadigm shouldBe false
    }

    test("casting a Paradigm spell flags it isParadigm in exile for both players") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)

        d.giveMana(player, Color.BLUE, 2) // pays {1}{U}
        val seminar = d.putCardInHand(player, "Research Seminar")
        d.castSpell(player, seminar)
        d.bothPass() // resolve — it lands in exile with the Paradigm marker

        d.getExile(player).contains(seminar) shouldBe true

        // Paradigm exile is face-up / public, so both the owner and the opponent see the flag.
        val ownerView = transformer(d).transform(d.state, viewingPlayerId = player)
        val opponentView = transformer(d).transform(d.state, viewingPlayerId = opponent)

        ownerView.cards[seminar].shouldNotBeNull().isParadigm shouldBe true
        opponentView.cards[seminar].shouldNotBeNull().isParadigm shouldBe true
    }
})
