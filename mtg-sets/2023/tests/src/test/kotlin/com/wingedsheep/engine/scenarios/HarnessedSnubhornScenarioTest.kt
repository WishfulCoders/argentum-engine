package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Harnessed Snubhorn (MAT #3) — {3}{W} 2/5 Dinosaur, vigilance. "Whenever this creature deals
 * combat damage to a player, return target artifact or enchantment card from your graveyard to
 * the battlefield."
 */
class HarnessedSnubhornScenarioTest : FunSpec({

    val relic = card("Test Relic") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }

    test("combat damage to a player returns an artifact card from your graveyard, never a creature card") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + relic)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val snubhorn = driver.putCreatureOnBattlefield(player, "Harnessed Snubhorn")
        driver.removeSummoningSickness(snubhorn)
        val relicCard = driver.putCardInGraveyard(player, "Test Relic")
        val bears = driver.putCardInGraveyard(player, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(snubhorn), opponent)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent)

        var guard = 0
        while (driver.state.pendingDecision == null && guard++ < 20) driver.bothPass()
        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets.values.flatten()
        legal shouldContain relicCard
        legal shouldNotContain bears

        driver.submitTargetSelection(player, listOf(relicCard))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
        driver.findPermanent(player, "Test Relic") shouldBe relicCard
    }
})
