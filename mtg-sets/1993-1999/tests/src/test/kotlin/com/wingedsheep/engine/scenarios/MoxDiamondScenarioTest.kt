package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sth.cards.MoxDiamond
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mox Diamond (STH #138) — "If this artifact would enter, you may discard a land card instead. If you
 * do, put this artifact onto the battlefield. If you don't, put it into its owner's graveyard.
 * {T}: Add one mana of any color."
 *
 * 2008-05-01 ruling: if you don't discard a land card, Mox Diamond never enters.
 */
class MoxDiamondScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(MoxDiamond))
        initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.enteredBattlefield(id: EntityId): Boolean =
        events.any { it is ZoneChangeEvent && it.entityId == id && it.toZone == Zone.BATTLEFIELD }

    test("discard a land card: Mox Diamond enters and taps for any color") {
        val d = driver()
        val you = d.activePlayer!!
        val forest = d.putCardInHand(you, "Forest")
        val mountain = d.putCardInHand(you, "Mountain")
        val mox = d.putCardInHand(you, "Mox Diamond")
        d.castSpell(you, mox).error shouldBe null
        d.bothPass()

        val ask = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        ask.options shouldContainExactlyInAnyOrder listOf(forest, mountain)
        d.submitCardSelection(you, listOf(mountain)).error shouldBe null

        d.findPermanent(you, "Mox Diamond") shouldBe mox
        d.getGraveyard(you) shouldContain mountain
        d.getHand(you) shouldContain forest

        val ability = MoxDiamond.script.activatedAbilities.single().id
        d.submit(ActivateAbility(playerId = you, sourceId = mox, abilityId = ability, manaColorChoice = Color.BLUE))
            .error shouldBe null
        (d.state.getEntity(you)?.get<ManaPoolComponent>()?.blue ?: 0) shouldBe 1
    }

    test("don't discard: it goes to the graveyard and never enters") {
        val d = driver()
        val you = d.activePlayer!!
        val forest = d.putCardInHand(you, "Forest")
        val mox = d.putCardInHand(you, "Mox Diamond")
        d.castSpell(you, mox).error shouldBe null
        d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        d.submitCardSelection(you, emptyList()).error shouldBe null

        d.findPermanent(you, "Mox Diamond").shouldBeNull()
        d.getGraveyard(you) shouldContain mox
        d.getHand(you) shouldContain forest
        withClue("never on the battlefield, so nothing that watches for entering could see it") {
            d.enteredBattlefield(mox) shouldBe false
        }
    }

    test("no land card in hand: straight to the graveyard, nobody is asked") {
        val d = driver()
        val you = d.activePlayer!!
        val mox = d.putCardInHand(you, "Mox Diamond")
        d.castSpell(you, mox).error shouldBe null
        d.bothPass()

        d.pendingDecision.shouldBeNull()
        d.getGraveyard(you) shouldContain mox
        d.enteredBattlefield(mox) shouldBe false
    }
})
