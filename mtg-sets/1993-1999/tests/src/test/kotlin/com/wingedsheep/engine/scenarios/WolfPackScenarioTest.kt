package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ptk.cards.WolfPack
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class WolfPackScenarioTest : FunSpec({

    fun blockedAttack(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WolfPack)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val wolf = driver.putCreatureOnBattlefield(driver.player1, "Wolf Pack")
        driver.removeSummoningSickness(wolf)
        val blocker = driver.putCreatureOnBattlefield(driver.player2, "Hill Giant")
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(wolf), driver.player2)
        driver.bothPass()
        driver.declareBlockers(driver.player2, mapOf(blocker to listOf(wolf)))
        driver.bothPass()
        return driver
    }

    test("blocked, may assign its 7 damage to the defending player") {
        val driver = blockedAttack()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(driver.player1, true)
        driver.getLifeTotal(driver.player2) shouldBe 13
        driver.getGraveyardCardNames(driver.player2).contains("Hill Giant") shouldBe false
    }

    test("declining assigns damage to the blocker") {
        val driver = blockedAttack()
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(driver.player1, false)
        driver.getLifeTotal(driver.player2) shouldBe 20
        driver.getGraveyardCardNames(driver.player2) shouldContain "Hill Giant"
    }
})
