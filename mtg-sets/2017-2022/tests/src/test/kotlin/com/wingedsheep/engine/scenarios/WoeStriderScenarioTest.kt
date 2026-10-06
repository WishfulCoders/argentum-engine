package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Woe Strider (THB #123).
 *
 * "When this creature enters, create a 0/1 white Goat creature token. / Sacrifice another creature:
 * Scry 1. / Escape—{3}{B}{B}, Exile four other cards from your graveyard. / This creature escapes
 * with two +1/+1 counters on it."
 */
class WoeStriderScenarioTest : FunSpec({

    val projector = StateProjector()

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun resolveAll(driver: GameTestDriver) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 50) {
            if (driver.pendingDecision == null) driver.bothPass() else driver.autoResolveDecision()
        }
    }

    fun plusOne(driver: GameTestDriver, id: EntityId) =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("escaped Woe Strider enters with two +1/+1 counters and still makes a Goat") {
        val (driver, player) = setup()
        val strider = driver.putCardInGraveyard(player, "Woe Strider")
        val fodder = (1..4).map { driver.putCardInGraveyard(player, "Swamp") }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 5)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = strider,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        resolveAll(driver)

        val perm = driver.findPermanent(player, "Woe Strider").shouldNotBeNull()
        plusOne(driver, perm) shouldBe 2
        projector.getProjectedPower(driver.state, perm) shouldBe 5
        projector.getProjectedToughness(driver.state, perm) shouldBe 4
        driver.getExile(player) shouldContainAll fodder
        driver.findPermanent(player, "Goat Token").shouldNotBeNull()
    }

    test("hard-cast Woe Strider enters without counters") {
        val (driver, player) = setup()
        val strider = driver.putCardInHand(player, "Woe Strider")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 3)

        driver.submit(CastSpell(player, strider, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        resolveAll(driver)

        val perm = driver.findPermanent(player, "Woe Strider").shouldNotBeNull()
        plusOne(driver, perm) shouldBe 0
        projector.getProjectedPower(driver.state, perm) shouldBe 3
    }

    test("three other cards can't pay the escape cost") {
        val (driver, player) = setup()
        val strider = driver.putCardInGraveyard(player, "Woe Strider")
        val fodder = (1..3).map { driver.putCardInGraveyard(player, "Swamp") }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 5)

        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = strider,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.getGraveyard(player) shouldContain strider
    }
})
