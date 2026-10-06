package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
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
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Uro, Titan of Nature's Wrath (THB #229).
 *
 * "When Uro enters, sacrifice it unless it escaped. / Whenever Uro enters or attacks, you gain 3
 * life and draw a card, then you may put a land card from your hand onto the battlefield. /
 * Escape—{G}{G}{U}{U}, Exile five other cards from your graveyard."
 */
class UroTitanOfNaturesWrathScenarioTest : FunSpec({

    val uroName = "Uro, Titan of Nature's Wrath"

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        return Triple(driver, player, driver.getOpponent(player))
    }

    /** Resolve everything; on the "you may put a land" prompt put one land if [putLand], else decline. */
    fun resolveAll(driver: GameTestDriver, player: EntityId, putLand: Boolean) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 50) {
            val decision = driver.pendingDecision
            when {
                decision == null -> driver.bothPass()
                decision is SelectCardsDecision && decision.playerId == player ->
                    driver.submitCardSelection(player, if (putLand) decision.options.take(1) else emptyList())
                else -> driver.autoResolveDecision()
            }
        }
    }

    fun escape(player: EntityId, uro: EntityId, exiled: List<EntityId>, choice: Int? = null) = CastSpell(
        playerId = player,
        cardId = uro,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.ESCAPE,
        escapeChoice = choice,
        additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
        paymentStrategy = PaymentStrategy.FromPool
    )

    test("hard-cast Uro is sacrificed, but still gains 3, draws, and may put a land onto the battlefield") {
        val (driver, player, _) = setup()
        val uro = driver.putCardInHand(player, uroName)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.GREEN, 2)
        driver.giveMana(player, Color.BLUE, 1)
        val handBefore = driver.getHandSize(player)
        val landsBefore = driver.getLands(player).size

        val result = driver.submit(CastSpell(player, uro, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        resolveAll(driver, player, putLand = true)

        driver.findPermanent(player, uroName).shouldBeNull()
        driver.getGraveyard(player) shouldContain uro
        driver.getLifeTotal(player) shouldBe 23
        driver.getLands(player).size shouldBe landsBefore + 1
        // Cast Uro (-1), drew one (+1), put a land (-1).
        driver.getHandSize(player) shouldBe handBefore - 1
    }

    test("escaped Uro stays; enters and attacks each gain 3 and draw, and the land is optional") {
        val (driver, player, opponent) = setup()
        val uro = driver.putCardInGraveyard(player, uroName)
        val fodder = (1..5).map { driver.putCardInGraveyard(player, "Forest") }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.GREEN, 2)
        driver.giveMana(player, Color.BLUE, 2)
        val handBefore = driver.getHandSize(player)
        val landsBefore = driver.getLands(player).size

        val result = driver.submit(escape(player, uro, fodder))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(player) shouldContainAll fodder
        resolveAll(driver, player, putLand = false)

        val perm = driver.findPermanent(player, uroName).shouldNotBeNull()
        driver.getLifeTotal(player) shouldBe 23
        driver.getHandSize(player) shouldBe handBefore + 1
        driver.getLands(player).size shouldBe landsBefore

        driver.removeSummoningSickness(perm)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(perm), opponent)
        resolveAll(driver, player, putLand = true)
        driver.getLifeTotal(player) shouldBe 26
        driver.getLands(player).size shouldBe landsBefore + 1
        driver.findPermanent(player, uroName).shouldNotBeNull()
    }

    test("escape needs five other cards") {
        val (driver, player, _) = setup()
        val uro = driver.putCardInGraveyard(player, uroName)
        val fodder = (1..4).map { driver.putCardInGraveyard(player, "Forest") }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.GREEN, 2)
        driver.giveMana(player, Color.BLUE, 2)

        driver.submitExpectFailure(escape(player, uro, fodder + uro))
        driver.getGraveyard(player) shouldContain uro
    }

    test("under Underworld Breach Uro offers both escapes; escaping with Breach's still counts as escaped") {
        val (driver, player, _) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Underworld Breach")
        val uro = driver.putCardInGraveyard(player, uroName)
        val fodder = (1..3).map { driver.putCardInGraveyard(player, "Forest") }
        driver.giveMana(player, Color.GREEN, 2)
        driver.giveMana(player, Color.BLUE, 1)

        val offers = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .filter { (it.action as? CastSpell)?.let { a -> a.cardId == uro && a.alternativeCostType == AlternativeCostType.ESCAPE } == true }
        offers shouldHaveSize 2
        offers.map { it.manaCostString } shouldContainExactlyInAnyOrder listOf("{G}{G}{U}{U}", "{1}{G}{U}")
        val breachChoice = offers.single { it.manaCostString == "{1}{G}{U}" }.action as CastSpell

        // Breach's escape: Uro's mana cost {1}{G}{U} plus three other cards.
        val result = driver.submit(escape(player, uro, fodder, choice = breachChoice.escapeChoice))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        resolveAll(driver, player, putLand = false)

        driver.findPermanent(player, uroName).shouldNotBeNull()
        driver.getLifeTotal(player) shouldBe 23
    }
})
