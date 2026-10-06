package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Underworld Breach (THB #161).
 *
 * "Each nonland card in your graveyard has escape. The escape cost is equal to the card's mana cost
 * plus exile three other cards from your graveyard. / At the beginning of the end step, sacrifice
 * this enchantment."
 */
class UnderworldBreachScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        return Triple(driver, player, driver.getOpponent(player))
    }

    fun escapeBolt(player: EntityId, bolt: EntityId, opponent: EntityId, exiled: List<EntityId>) = CastSpell(
        playerId = player,
        cardId = bolt,
        targets = listOf(ChosenTarget.Player(opponent)),
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.ESCAPE,
        additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
        paymentStrategy = PaymentStrategy.FromPool
    )

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 50) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("a Lightning Bolt in the graveyard escapes for {R} plus three other cards, and can escape again") {
        val (driver, player, opponent) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Underworld Breach")
        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        val firstFodder = (1..3).map { driver.putCardInGraveyard(player, "Mountain") }
        val secondFodder = (1..3).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.giveMana(player, Color.RED, 2)

        val offer = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .single { (it.action as? CastSpell)?.cardId == bolt }
        offer.actionType shouldBe "CastWithEscape"
        offer.manaCostString shouldBe "{R}"

        val first = driver.submit(escapeBolt(player, bolt, opponent, firstFodder))
        withClue("error=${first.error}") { first.outcome shouldBe Outcome.Done }
        resolveStack(driver)
        driver.getLifeTotal(opponent) shouldBe 17
        // Escape doesn't exile the spell: Bolt is back in the graveyard, ready to escape again.
        driver.getGraveyard(player) shouldContain bolt
        driver.getExile(player) shouldContainAll firstFodder

        val second = driver.submit(escapeBolt(player, bolt, opponent, secondFodder))
        withClue("error=${second.error}") { second.outcome shouldBe Outcome.Done }
        resolveStack(driver)
        driver.getLifeTotal(opponent) shouldBe 14
        driver.getExile(player) shouldNotContain bolt
    }

    test("lands in the graveyard don't gain escape") {
        val (driver, player, _) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Underworld Breach")
        val mountains = (1..4).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.giveMana(player, Color.RED, 2)

        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .filter { (it.action as? CastSpell)?.cardId in mountains }
            .shouldBeEmpty()
    }

    test("it is sacrificed at the beginning of the end step") {
        val (driver, player, _) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val breach = driver.putPermanentOnBattlefield(player, "Underworld Breach")

        driver.passPriorityUntil(Step.END)
        resolveStack(driver)

        driver.findPermanent(player, "Underworld Breach").shouldBeNull()
        driver.getGraveyard(player) shouldContain breach
    }

    test("it is sacrificed at the beginning of every end step, not just its controller's") {
        val (driver, player, opponent) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val breach = driver.putPermanentOnBattlefield(opponent, "Underworld Breach")
        driver.findPermanent(opponent, "Underworld Breach").shouldNotBeNull()

        driver.passPriorityUntil(Step.END)
        resolveStack(driver)

        driver.findPermanent(opponent, "Underworld Breach").shouldBeNull()
        driver.getGraveyard(opponent) shouldContain breach
    }
})
