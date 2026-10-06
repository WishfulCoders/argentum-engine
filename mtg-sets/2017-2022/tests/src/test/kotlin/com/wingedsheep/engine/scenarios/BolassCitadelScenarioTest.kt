package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.war.cards.BolassCitadel
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bolas's Citadel — the top card of your library is playable: a spell cast from there costs life
 * equal to its mana value instead of its mana cost, a land uses the normal land drop, and the
 * Citadel can be one of the ten nonland permanents sacrificed to drain each opponent for 10.
 */
class BolassCitadelScenarioTest : FunSpec({

    val drainAbilityId = BolassCitadel.activatedAbilities.first().id

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BolassCitadel)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    test("a spell on top of the library is cast for life equal to its mana value, no mana spent") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Bolas's Citadel")
        val courser = driver.putCardOnTopOfLibrary(you, "Centaur Courser") // {2}{G}, mana value 3

        driver.submit(CastSpell(playerId = you, cardId = courser, paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 17
        driver.findPermanent(you, "Centaur Courser") shouldBe courser
    }

    test("a land on top of the library can be played as the land drop") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Bolas's Citadel")
        val forest = driver.putCardOnTopOfLibrary(you, "Forest")

        driver.playLand(you, forest).outcome shouldBe Outcome.Done
        driver.findPermanent(you, "Forest") shouldBe forest
        driver.getLifeTotal(you) shouldBe 20
    }

    test("tap and sacrifice ten nonland permanents, the Citadel among them: each opponent loses 10") {
        val (driver, you) = newGame()
        val opponent = driver.getOpponent(you)
        val citadel = driver.putPermanentOnBattlefield(you, "Bolas's Citadel")
        val fodder = (1..9).map { driver.putCreatureOnBattlefield(you, "Savannah Lions") }

        driver.submit(
            ActivateAbility(
                you, citadel, drainAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = fodder + citadel)
            )
        ).error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 10
        driver.getLifeTotal(you) shouldBe 20
        driver.getCreatures(you).size shouldBe 0
        driver.findPermanent(you, "Bolas's Citadel") shouldBe null
    }
})
