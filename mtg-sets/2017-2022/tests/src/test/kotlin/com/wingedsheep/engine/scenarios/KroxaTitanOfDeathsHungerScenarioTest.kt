package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kroxa, Titan of Death's Hunger (THB #221).
 *
 * "When Kroxa enters, sacrifice it unless it escaped. / Whenever Kroxa enters or attacks, each
 * opponent discards a card, then each opponent who didn't discard a nonland card this way loses 3
 * life. / Escape—{B}{B}{R}{R}, Exile five other cards from your graveyard."
 */
class KroxaTitanOfDeathsHungerScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        return Triple(driver, player, opponent)
    }

    /** Resolve everything; the opponent discards [preferred] when offered it, else the first card. */
    fun resolveAll(driver: GameTestDriver, opponent: EntityId, preferred: EntityId? = null) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 50) {
            val decision = driver.pendingDecision
            when {
                decision == null -> driver.bothPass()
                decision is SelectCardsDecision && decision.playerId == opponent &&
                    preferred != null && preferred in decision.options ->
                    driver.submitCardSelection(opponent, listOf(preferred))
                else -> driver.autoResolveDecision()
            }
        }
    }

    test("hard-cast Kroxa is sacrificed; an opponent who discards a land loses 3 life") {
        val (driver, player, opponent) = setup()
        val kroxa = driver.putCardInHand(player, "Kroxa, Titan of Death's Hunger")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val oppHandBefore = driver.getHandSize(opponent)
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveMana(player, Color.RED, 1)

        val result = driver.submit(CastSpell(player, kroxa, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        resolveAll(driver, opponent)

        driver.findPermanent(player, "Kroxa, Titan of Death's Hunger").shouldBeNull()
        driver.getGraveyard(player) shouldContain kroxa
        withClue("opponent discarded one (land) card") { driver.getHandSize(opponent) shouldBe oppHandBefore - 1 }
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("escaped Kroxa stays; an opponent who discards a nonland card loses no life") {
        val (driver, player, opponent) = setup()
        val kroxa = driver.putCardInGraveyard(player, "Kroxa, Titan of Death's Hunger")
        val fodder = (1..5).map { driver.putCardInGraveyard(player, "Swamp") }
        val bears = driver.putCardInHand(opponent, "Grizzly Bears")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 2)
        driver.giveMana(player, Color.RED, 2)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = kroxa,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(player) shouldContainAll fodder
        resolveAll(driver, opponent, preferred = bears)

        driver.findPermanent(player, "Kroxa, Titan of Death's Hunger").shouldNotBeNull()
        driver.getGraveyard(opponent) shouldContain bears
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
