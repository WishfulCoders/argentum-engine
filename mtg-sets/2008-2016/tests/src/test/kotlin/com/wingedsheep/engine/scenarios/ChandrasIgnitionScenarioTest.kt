package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.ChandrasIgnition
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Chandra's Ignition (ORI #137) — "Target creature you control deals damage equal to its power to
 * each other creature and each opponent." Both sides of the board, the opponent, and not itself.
 */
class ChandrasIgnitionScenarioTest : FunSpec({

    test("a 3/3 deals 3 to every other creature, yours included, and to the opponent") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ChandrasIgnition)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val mine = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val spell = driver.putCardInHand(you, "Chandra's Ignition")
        driver.giveMana(you, Color.RED, 5)

        driver.castSpell(you, spell, listOf(courser)).error shouldBe null
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(you) shouldBe 20
        driver.getCreatures(you) shouldContain courser
        driver.getCreatures(you) shouldNotContain mine
        driver.getCreatures(opponent) shouldNotContain theirs
    }
})
