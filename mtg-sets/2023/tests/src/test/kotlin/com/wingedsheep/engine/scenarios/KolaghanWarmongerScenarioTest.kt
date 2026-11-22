package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kolaghan Warmonger (MAT #17) — {2}{R} 3/2 Ogre Warrior, haste. "Whenever this creature attacks,
 * look at the top six cards of your library. You may reveal a Dragon card from among them and put
 * it into your hand. Put the rest on the bottom of your library in a random order."
 */
class KolaghanWarmongerScenarioTest : FunSpec({

    val testDragon = CardDefinition.creature("Test Dragon", ManaCost.parse("{4}{R}"), setOf(Subtype.DRAGON), 4, 4)

    test("attacking looks at the top six, takes only a Dragon, and bottoms the rest") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + testDragon)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Haste: it can attack the turn it arrives.
        val warmonger = driver.putCreatureOnBattlefield(player, "Kolaghan Warmonger")
        val seventh = driver.putCardOnTopOfLibrary(player, "Grizzly Bears")
        val top = (1..5).map { driver.putCardOnTopOfLibrary(player, "Mountain") }
        val dragon = driver.putCardOnTopOfLibrary(player, "Test Dragon")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(warmonger), opponent)
        var guard = 0
        while (driver.state.pendingDecision == null && guard++ < 20) driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain dragon
        decision.options shouldNotContain seventh
        driver.submitCardSelection(player, listOf(dragon))

        driver.getHand(player) shouldContain dragon
        val library = driver.state.getLibrary(player)
        library.first() shouldBe seventh
        library.takeLast(5).toSet() shouldBe top.toSet()
    }
})
