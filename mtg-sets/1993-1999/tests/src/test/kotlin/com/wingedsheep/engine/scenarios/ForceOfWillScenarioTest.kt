package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.all.cards.ForceOfWill
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Force of Will {3}{U}{U} — Instant.
 *
 * "You may pay 1 life and exile a blue card from your hand rather than pay this spell's mana cost.
 *  Counter target spell."
 *
 * The alternative cost bundles two non-mana costs (life + a pitched blue card) with no turn gate,
 * so the cases worth proving are: both halves are paid and the spell is countered; a non-blue card
 * can't be pitched; and with no other blue card in hand the alternative isn't offered.
 */
class ForceOfWillScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ForceOfWill))
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
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.castSpell(opponent, bolt, targets = listOf(you)).error shouldBe null
        driver.passPriority(opponent)
        return Triple(you, opponent, bolt)
    }

    test("paying 1 life and pitching a blue card counters the spell") {
        val driver = createDriver()
        val (you, opponent, bolt) = setUpBolt(driver)
        val pitched = driver.putCardInHand(you, "Counterspell")
        val force = driver.putCardInHand(you, "Force of Will")
        altCastOffered(driver, you, force) shouldBe true

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = force,
                targets = listOf(ChosenTarget.Spell(bolt)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(pitched))
            )
        ).error shouldBe null
        driver.getLifeTotal(you) shouldBe 19
        while (driver.stackSize > 0) driver.bothPass()

        driver.getLifeTotal(you) shouldBe 19
        driver.getGraveyardCardNames(opponent) shouldBe listOf("Lightning Bolt")
        driver.getExileCardNames(you) shouldBe listOf("Counterspell")
        driver.getGraveyardCardNames(you) shouldBe listOf("Force of Will")
    }

    test("a non-blue card can't be pitched, and with no blue card the alternative isn't offered") {
        val driver = createDriver()
        val (you, _, bolt) = setUpBolt(driver)
        val red = driver.putCardInHand(you, "Lightning Bolt")
        val force = driver.putCardInHand(you, "Force of Will")
        altCastOffered(driver, you, force) shouldBe false

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = force,
                targets = listOf(ChosenTarget.Spell(bolt)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(red))
            )
        ).error shouldNotBe null
        driver.getLifeTotal(you) shouldBe 20
    }
})
