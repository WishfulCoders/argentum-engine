package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmp.cards.Reanimate
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Reanimate (TMP #151) — {B} Sorcery.
 *
 *   "Put target creature card from a graveyard onto the battlefield under your control. You lose
 *    life equal to that card's mana value."
 *
 * Proves the two things a plain `Move` gets wrong: a card from an *opponent's* graveyard enters
 * under the caster's control (not its owner's), and the caster loses life equal to the card's mana
 * value.
 */
class ReanimateScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + Reanimate)
        initMirrorMatch(deck = Deck.of("Swamp" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("reanimates an opponent's creature under your control and you lose its mana value") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val force = d.putCardInGraveyard(opp, "Force of Nature") // {3}{G}{G}, mana value 5

        val spell = d.putCardInHand(me, "Reanimate")
        d.giveMana(me, Color.BLACK, 1)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(force, opp, Zone.GRAVEYARD))).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.getPermanents(me) shouldContain force
        d.state.projectedState.getController(force) shouldBe me
        d.getLifeTotal(me) shouldBe 15
        d.getLifeTotal(opp) shouldBe 20
    }

    test("reanimates from your own graveyard") {
        val d = driver()
        val me = d.activePlayer!!
        val courser = d.putCardInGraveyard(me, "Centaur Courser") // {2}{G}, mana value 3

        val spell = d.putCardInHand(me, "Reanimate")
        d.giveMana(me, Color.BLACK, 1)
        d.castSpellWithTargets(me, spell, listOf(ChosenTarget.Card(courser, me, Zone.GRAVEYARD))).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.getPermanents(me) shouldContain courser
        d.getLifeTotal(me) shouldBe 17
    }
})
