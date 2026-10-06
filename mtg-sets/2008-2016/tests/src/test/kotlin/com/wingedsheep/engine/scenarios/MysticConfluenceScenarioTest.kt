package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c15.cards.MysticConfluence
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mystic Confluence (C15 #14) — {3}{U}{U} Instant.
 *
 *   Choose three. You may choose the same mode more than once.
 *   • Counter target spell unless its controller pays {3}.
 *   • Return target creature to its owner's hand.
 *   • Draw a card.
 *
 * Proves one of each mode with per-mode targets (a spell and a creature) and a repeated mode.
 */
class MysticConfluenceScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MysticConfluence))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, startingPlayer = 1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while (stackSize > 0 && guard++ < 30) {
            val d = pendingDecision
            if (d is YesNoDecision) submitYesNo(d.playerId, false) else bothPass()
        }
    }

    test("counter + bounce + draw: each mode with its own target") {
        val driver = newDriver()
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.castSpell(opponent, bolt, targets = listOf(you)).error shouldBe null
        driver.passPriority(opponent)

        val confluence = driver.putCardInHand(you, "Mystic Confluence")
        driver.giveMana(you, Color.BLUE, 5)
        val handBefore = driver.getHandSize(you)
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = confluence,
                targets = listOf(ChosenTarget.Spell(bolt), ChosenTarget.Permanent(bears)),
                chosenModes = listOf(0, 1, 2),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Spell(bolt)),
                    listOf(ChosenTarget.Permanent(bears)),
                    emptyList()
                )
            )
        ).error shouldBe null
        driver.drain()

        driver.getLifeTotal(you) shouldBe 20
        driver.getGraveyardCardNames(opponent) shouldBe listOf("Lightning Bolt")
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getHand(opponent).contains(bears) shouldBe true
        // Cast Confluence (-1), drew one (+1).
        driver.getHandSize(you) shouldBe handBefore
    }

    test("the draw mode chosen three times draws three cards") {
        val driver = newDriver()
        val opponent = driver.activePlayer!!
        val you = driver.getOpponent(opponent)
        driver.passPriority(opponent)

        val confluence = driver.putCardInHand(you, "Mystic Confluence")
        driver.giveMana(you, Color.BLUE, 5)
        val handBefore = driver.getHandSize(you)
        driver.submit(
            CastSpell(playerId = you, cardId = confluence, chosenModes = listOf(2, 2, 2))
        ).error shouldBe null
        driver.drain()

        driver.getHandSize(you) shouldBe handBefore - 1 + 3
    }
})
