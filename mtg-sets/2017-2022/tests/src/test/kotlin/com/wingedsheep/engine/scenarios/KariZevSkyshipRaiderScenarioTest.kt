package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.KariZevSkyshipRaider
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kari Zev, Skyship Raider (AER #87) — {1}{R} Legendary Creature — Human Pirate 1/3.
 *
 *   First strike, menace
 *   Whenever Kari Zev attacks, create Ragavan, a legendary 2/1 red Monkey creature token. Ragavan
 *   enters tapped and attacking. Exile that token at end of combat.
 */
class KariZevSkyshipRaiderScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KariZevSkyshipRaider))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("attacking creates Ragavan tapped and attacking; it deals damage and is exiled at end of combat") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2
        val kari = driver.putCreatureOnBattlefield(me, "Kari Zev, Skyship Raider")
        driver.removeSummoningSickness(kari)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(kari), opp)
        driver.bothPass() // resolve the attack trigger

        val ragavan = driver.findPermanent(me, "Ragavan")
        withClue("Ragavan token is on the battlefield") { ragavan shouldNotBe null }
        driver.isTapped(ragavan!!) shouldBe true
        driver.state.getEntity(ragavan)?.get<AttackingComponent>() shouldNotBe null
        driver.state.projectedState.getPower(ragavan) shouldBe 2
        driver.state.projectedState.getToughness(ragavan) shouldBe 1

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        withClue("Kari (1, first strike) + Ragavan (2) connect") {
            driver.getLifeTotal(opp) shouldBe 17
        }
        withClue("Ragavan is exiled at end of combat") {
            driver.findPermanent(me, "Ragavan") shouldBe null
        }
        driver.findPermanent(me, "Kari Zev, Skyship Raider") shouldNotBe null
    }
})
