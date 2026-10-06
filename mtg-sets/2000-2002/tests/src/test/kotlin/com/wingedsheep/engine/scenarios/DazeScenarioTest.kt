package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.nem.cards.Daze
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Daze {1}{U} — Instant.
 *
 * "You may return an Island you control to its owner's hand rather than pay this spell's mana cost.
 *  Counter target spell unless its controller pays {1}."
 *
 * The first card whose [com.wingedsheep.sdk.scripting.SelfAlternativeCost] is a bounce cost, so the
 * cases worth proving are the free cast paying with an Island (which returns to hand), the alternative
 * being unavailable with no Island, and the "unless pays {1}" tax both declined (countered) and paid.
 */
class DazeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Daze))
        return driver
    }

    fun altCastOffered(driver: GameTestDriver, player: EntityId, cardId: EntityId): Boolean =
        driver.legalActions(player).any { legal ->
            val action = legal.action
            action is CastSpell && action.cardId == cardId && action.useAlternativeCost &&
                action.alternativeCostType == AlternativeCostType.SELF_ALTERNATIVE
        }

    /** Opponent is active and casts Lightning Bolt at "you"; returns (you, opponent, bolt). */
    fun setUpBolt(driver: GameTestDriver): Triple<EntityId, EntityId, EntityId> {
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.castSpell(opponent, bolt, targets = listOf(you)).error shouldBe null
        driver.passPriority(opponent)
        return Triple(you, opponent, bolt)
    }

    fun answerTax(driver: GameTestDriver, pay: Boolean) {
        while (driver.stackSize > 0) {
            val d = driver.pendingDecision
            if (d is YesNoDecision) driver.submitYesNo(d.playerId, pay) else driver.bothPass()
        }
    }

    test("returning an Island casts Daze for free; with no spare mana the spell is countered") {
        val driver = createDriver()
        val (you, opponent, bolt) = setUpBolt(driver)
        val island = driver.putLandOnBattlefield(you, "Island")
        val daze = driver.putCardInHand(you, "Daze")
        altCastOffered(driver, you, daze) shouldBe true

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = daze,
                targets = listOf(ChosenTarget.Spell(bolt)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(island))
            )
        ).error shouldBe null

        // The Island went back to its owner's hand as the cost.
        driver.state.getBattlefield(you).contains(island) shouldBe false
        driver.getHand(you).contains(island) shouldBe true

        answerTax(driver, pay = false)

        driver.getLifeTotal(you) shouldBe 20
        driver.getGraveyardCardNames(opponent) shouldBe listOf("Lightning Bolt")
        driver.getGraveyardCardNames(you) shouldBe listOf("Daze")
    }

    test("the alternative cost needs an Island: a Mountain is not enough") {
        val driver = createDriver()
        val (you, _, bolt) = setUpBolt(driver)
        val mountain = driver.putLandOnBattlefield(you, "Mountain")
        val daze = driver.putCardInHand(you, "Daze")
        altCastOffered(driver, you, daze) shouldBe false

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = daze,
                targets = listOf(ChosenTarget.Spell(bolt)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(mountain))
            )
        ).error shouldNotBe null
    }

    test("if the spell's controller pays {1}, the spell resolves") {
        val driver = createDriver()
        val (you, opponent, bolt) = setUpBolt(driver)
        val island = driver.putLandOnBattlefield(you, "Island")
        val daze = driver.putCardInHand(you, "Daze")
        driver.giveMana(opponent, Color.RED, 1)

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = daze,
                targets = listOf(ChosenTarget.Spell(bolt)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(bouncedPermanents = listOf(island))
            )
        ).error shouldBe null

        answerTax(driver, pay = true)

        driver.getLifeTotal(you) shouldBe 17
    }
})
