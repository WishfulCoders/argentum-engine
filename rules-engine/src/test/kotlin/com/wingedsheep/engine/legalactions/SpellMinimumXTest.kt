package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `CardScript.minimumXValue` — a spell's "X can't be 0" (CR 601.2b announcement floor), on a
 * printed {X} (Welcome the Darkness, Mind Grind) and on a kicker {X} (Thieving Skydiver).
 *
 * Pinned: the cast offer carries `minX` and is unaffordable below it; the engine refuses an
 * announced X under the floor; the floor binds only a cast whose cost has an {X} (the unkicked
 * Skydiver announces nothing); and the announced X reaches the spell's effect / the permanent's
 * enters trigger (CR 107.3m).
 */
class SpellMinimumXTest : FunSpec({

    val xSpell = card("Test Floor Spell") {
        manaCost = "{X}{B}"
        typeLine = "Sorcery"
        minimumXValue = 1
        spell {
            effect = Effects.GainLife(DynamicAmounts.xValue())
        }
    }
    val kickerDiver = card("Test Floor Diver") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Merfolk Rogue"
        power = 2
        toughness = 1
        keywordAbility(KeywordAbility.kicker("{X}"))
        minimumXValue = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            interveningIf = WasKicked
            effect = Effects.GainLife(DynamicAmounts.xValue())
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(xSpell, kickerDiver))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0, skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castOffers(player: EntityId, card: EntityId) =
        legalActions(player).filter { (it.action as? CastSpell)?.cardId == card }

    test("a printed {X} offer is floored at 1 and unaffordable until X = 1 is") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val spell = driver.putCardInHand(me, "Test Floor Spell")
        driver.giveMana(me, Color.BLACK, 1)

        val tooPoor = driver.castOffers(me, spell).single()
        tooPoor.hasXCost shouldBe true
        tooPoor.minX shouldBe 1
        withClue("{B} pays only X = 0, which the spell forbids") { tooPoor.affordable shouldBe false }

        driver.giveColorlessMana(me, 1)
        val affordable = driver.castOffers(me, spell).single()
        affordable.minX shouldBe 1
        affordable.affordable shouldBe true
    }

    test("X = 0 is refused, X = 1 is cast and resolves with X = 1") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val spell = driver.putCardInHand(me, "Test Floor Spell")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 1)
        val life = driver.getLifeTotal(me)

        driver.submitExpectFailure(
            CastSpell(me, spell, xValue = 0, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldNotBe null

        driver.castXSpell(me, spell, xValue = 1).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe life + 1
    }

    test("kicker {X}: the kicked offer is floored, the unkicked one announces no X") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val diver = driver.putCardInHand(me, "Test Floor Diver")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 2)

        val offers = driver.castOffers(me, diver)
        val kicked = offers.single { (it.action as CastSpell).declaredCostSlot == ChoiceSlot.KICKED }
        val plain = offers.single { (it.action as CastSpell).declaredCostSlot == null }
        kicked.hasXCost shouldBe true
        kicked.minX shouldBe 1
        kicked.affordable shouldBe true
        plain.hasXCost shouldBe false
        plain.minX shouldBe 0
    }

    test("a kicked cast with X = 0 is refused; with X = 1 the enters trigger sees X = 1") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val diver = driver.putCardInHand(me, "Test Floor Diver")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 2)
        val life = driver.getLifeTotal(me)

        driver.submitExpectFailure(
            CastSpell(
                me, diver, xValue = 0, declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldNotBe null

        driver.submit(
            CastSpell(
                me, diver, xValue = 1, declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass() // creature spell
        driver.bothPass() // enters trigger
        driver.getLifeTotal(me) shouldBe life + 1
    }

    test("an unkicked cast needs no X") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val diver = driver.putCardInHand(me, "Test Floor Diver")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)

        driver.castSpell(me, diver).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.stackSize shouldBe 0
    }
})
