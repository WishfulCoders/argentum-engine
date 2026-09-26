package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Omnipresence — {5}{G}{G}{G} Enchantment (FRA #110).
 *
 *   You may cast spells with mana value less than or equal to the number of creatures you
 *   control from your hand without paying their mana costs.
 */
class OmnipresenceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.freeCastOffered(player: EntityId, cardId: EntityId): Boolean =
        LegalActionEnumerator.create(cardRegistry).enumerate(state, player, EnumerationMode.FULL).any { la ->
            (la.action as? CastSpell)?.let { it.cardId == cardId && it.useWithoutPayingManaCost } == true
        }

    test("a spell whose mana value is at most your creature count is cast for free from hand") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Omnipresence")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        // Hill Giant is mana value 4 > 3 creatures; Craw Wurm is 6. Centaur Courser is 3.
        val courser = driver.putCardInHand(player, "Centaur Courser")
        val giant = driver.putCardInHand(player, "Hill Giant")

        driver.freeCastOffered(player, courser) shouldBe true
        driver.freeCastOffered(player, giant) shouldBe false

        // No mana in pool — the only way to cast it is the free variant.
        driver.submitSuccess(CastSpell(player, courser, useWithoutPayingManaCost = true))
        driver.bothPass()
        driver.findPermanent(player, "Centaur Courser") shouldNotBe null

        // Now four creatures: Hill Giant (mana value 4) fits.
        driver.freeCastOffered(player, giant) shouldBe true
        driver.submitSuccess(CastSpell(player, giant, useWithoutPayingManaCost = true))
        driver.bothPass()
        driver.findPermanent(player, "Hill Giant") shouldNotBe null
    }

    test("a spell over the cap is refused, and the cap tracks creatures leaving") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Omnipresence")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val bearsInHand = driver.putCardInHand(player, "Grizzly Bears")
        driver.freeCastOffered(player, bearsInHand) shouldBe true

        // Drop to one creature — a mana value 2 spell no longer fits.
        driver.moveToGraveyard(bear)
        driver.freeCastOffered(player, bearsInHand) shouldBe false
        driver.submit(CastSpell(player, bearsInHand, useWithoutPayingManaCost = true)).outcome shouldNotBe Outcome.Done
    }

    test("with no creatures only mana value 0 spells are free") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Omnipresence")
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.freeCastOffered(player, bears) shouldBe false
    }

    test("the free cast is an option — paying the mana cost is still offered") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Omnipresence")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        repeat(2) { driver.putLandOnBattlefield(player, "Forest") }
        val bears = driver.putCardInHand(player, "Grizzly Bears")

        val casts = driver.legalActions(player).mapNotNull { it.action as? CastSpell }.filter { it.cardId == bears }
        casts.any { it.useWithoutPayingManaCost } shouldBe true
        casts.any { !it.useWithoutPayingManaCost } shouldBe true
    }

    test("an {X} spell cast this way has X = 0, and a free cast announcing X > 0 is refused") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Omnipresence")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        // Stream of Life ({X}{G}) has mana value 1 in hand.
        val stream = driver.putCardInHand(player, "Stream of Life")
        val target = listOf(ChosenTarget.Player(player))
        driver.freeCastOffered(player, stream) shouldBe true

        driver.submit(
            CastSpell(player, stream, targets = target, xValue = 5, useWithoutPayingManaCost = true)
        ).outcome shouldNotBe Outcome.Done

        val lifeBefore = driver.getLifeTotal(player)
        driver.submitSuccess(CastSpell(player, stream, targets = target, useWithoutPayingManaCost = true))
        driver.bothPass()
        driver.getLifeTotal(player) shouldBe lifeBefore
    }

    test("the permission is hand-only and benefits only its controller") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.putPermanentOnBattlefield(player, "Omnipresence")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val calculator = CostCalculator(driver.cardRegistry, PredicateEvaluator(driver.cardRegistry))
        val bearsDef = driver.cardRegistry.requireCard("Grizzly Bears")
        calculator.hasFreeCastPermission(driver.state, player, bearsDef, Zone.HAND) shouldBe true
        calculator.hasFreeCastPermission(driver.state, player, bearsDef, Zone.EXILE) shouldBe false
        calculator.hasFreeCastPermission(driver.state, player, bearsDef, Zone.GRAVEYARD) shouldBe false
        calculator.hasFreeCastPermission(driver.state, opponent, bearsDef, Zone.HAND) shouldBe false
    }
})
