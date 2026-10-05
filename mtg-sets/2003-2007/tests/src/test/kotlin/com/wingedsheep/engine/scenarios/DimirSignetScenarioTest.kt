package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rav.cards.DimirSignet
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dimir Signet: {1}, {T}: Add {U}{B}. The {1} must actually be paid — tapping the artifact alone
 * (no untapped lands, empty pool) must not produce mana.
 */
class DimirSignetScenarioTest : FunSpec({

    val signetAbilityId = DimirSignet.activatedAbilities[0].id

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DimirSignet)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("with a floating {1}, Dimir Signet adds one blue and one black") {
        val driver = setup()
        val player = driver.activePlayer!!
        val signet = driver.putPermanentOnBattlefield(player, "Dimir Signet")
        driver.giveColorlessMana(player, 1)

        val result = driver.submit(
            ActivateAbility(player, signet, signetAbilityId, paymentStrategy = PaymentStrategy.FromPool)
        )
        result.outcome shouldBe Outcome.Done
        val pool = driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!
        pool.blue shouldBe 1
        pool.black shouldBe 1
    }

    test("with an untapped land, auto-pay taps it for the {1}") {
        val driver = setup()
        val player = driver.activePlayer!!
        val signet = driver.putPermanentOnBattlefield(player, "Dimir Signet")
        driver.putLandOnBattlefield(player, "Forest")

        val result = driver.submit(ActivateAbility(player, signet, signetAbilityId))
        result.outcome shouldBe Outcome.Done
        val pool = driver.state.getEntity(player)!!.get<ManaPoolComponent>()!!
        pool.blue shouldBe 1
        pool.black shouldBe 1
    }

    test("with all lands tapped and an empty pool, activation is rejected") {
        val driver = setup()
        val player = driver.activePlayer!!
        val signet = driver.putPermanentOnBattlefield(player, "Dimir Signet")
        val forest = driver.putLandOnBattlefield(player, "Forest")
        driver.tapPermanent(forest)

        val strategies = listOf(
            PaymentStrategy.FromPool,
            PaymentStrategy.AutoPay,
            PaymentStrategy.Explicit(manaAbilitiesToActivate = emptyList()),
        )
        for (strategy in strategies) {
            val result = driver.submit(ActivateAbility(player, signet, signetAbilityId, paymentStrategy = strategy))
            (result.outcome == Outcome.Done) shouldBe false
            driver.state.getEntity(signet)!!.has<TappedComponent>() shouldBe false
            val pool = driver.state.getEntity(player)!!.get<ManaPoolComponent>()
            (pool?.blue ?: 0) shouldBe 0
            (pool?.black ?: 0) shouldBe 0
        }
    }

    test("a lone Signet is not a mana source: it cannot pay for any spell or ability") {
        val driver = setup()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Dimir Signet")

        val solver = driver.services.manaSolver
        for (cost in listOf("{1}", "{U}", "{B}", "{U}{B}", "{2}{B}")) {
            solver.canPay(driver.state, player, ManaCost.parse(cost)) shouldBe false
        }
    }

    test("Signet plus one land nets two mana, not three") {
        val driver = setup()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Dimir Signet")
        driver.putLandOnBattlefield(player, "Forest")

        val solver = driver.services.manaSolver
        solver.canPay(driver.state, player, ManaCost.parse("{U}{B}")) shouldBe true
        solver.canPay(driver.state, player, ManaCost.parse("{1}{U}{B}")) shouldBe false
    }

    test("the Signet cannot pay for its own {1}") {
        val driver = setup()
        val player = driver.activePlayer!!
        val signet = driver.putPermanentOnBattlefield(player, "Dimir Signet")

        val result = driver.submit(ActivateAbility(player, signet, signetAbilityId))
        (result.outcome == Outcome.Done) shouldBe false
        driver.state.getEntity(signet)!!.has<TappedComponent>() shouldBe false
    }
})
