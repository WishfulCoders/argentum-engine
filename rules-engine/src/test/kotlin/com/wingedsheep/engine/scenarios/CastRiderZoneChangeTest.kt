package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A cast-this-way destination rider ("you may cast target instant or sorcery card from your
 * graveyard this turn. If that spell would be put into your graveyard, exile it instead" — Jace,
 * Telepath Unbound) is stamped on the card *before* it is cast. CR 400.7: once the card changes
 * zones any other way, it is a new object the granting effect has lost track of, so the rider
 * must not follow it — Jace's ruling: "If, at any time, the card goes to a hidden zone (such as
 * your hand or your library), the effect loses track of the card. It won't be exiled, even if
 * that card is put into your graveyard later that turn."
 */
class CastRiderZoneChangeTest : FunSpec({

    val grant = card("Test Recall Grant") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            target(TargetFilter.InstantOrSorceryInYourGraveyard)
            effect = Effects.Pipeline {
                val card = gather(CardSource.ChosenTargets)
                run(Effects.GrantMayPlayFromExile(
                    from = card,
                    expiry = MayPlayExpiry.EndOfTurn,
                    insteadOfGraveyard = AfterResolveDestination.EXILE
                ))
            }
        }
    }
    val payoff = card("Test Rider Payoff") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(2) }
    }
    val regrowth = card("Test Rider Regrowth") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.InstantOrSorceryInYourGraveyard)
            effect = Effects.Move(t, Zone.HAND)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(grant, payoff, regrowth))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.grantOn(card: EntityId) {
        val spell = putCardInHand(player1, grant.name)
        giveColorlessMana(player1, 1)
        castSpellWithTargets(player1, spell, listOf(ChosenTarget.Card(card, player1, Zone.GRAVEYARD))).error shouldBe null
        bothPass()
    }
    fun GameTestDriver.hasRider(card: EntityId) = state.getEntity(card)?.has<AfterResolveDestinationComponent>() == true

    test("control: the card cast from the graveyard this way is exiled as it resolves") {
        val d = driver()
        val card = d.putCardInGraveyard(d.player1, payoff.name)
        d.grantOn(card)
        d.hasRider(card) shouldBe true
        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, card).error shouldBe null
        d.bothPass()
        d.getExile(d.player1).contains(card) shouldBe true
        d.getGraveyard(d.player1).contains(card) shouldBe false
    }

    test("a card that went to its owner's hand loses the rider: cast from hand, it goes to the graveyard") {
        val d = driver()
        val card = d.putCardInGraveyard(d.player1, payoff.name)
        d.grantOn(card)
        val bounce = d.putCardInHand(d.player1, regrowth.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpellWithTargets(d.player1, bounce, listOf(ChosenTarget.Card(card, d.player1, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass()
        d.getHand(d.player1).contains(card) shouldBe true
        withClue("CR 400.7: the card in hand is a new object") { d.hasRider(card) shouldBe false }

        d.giveColorlessMana(d.player1, 1)
        d.castSpell(d.player1, card).error shouldBe null
        d.bothPass()
        d.getGraveyard(d.player1).contains(card) shouldBe true
        d.getExile(d.player1).contains(card) shouldBe false
    }
})
