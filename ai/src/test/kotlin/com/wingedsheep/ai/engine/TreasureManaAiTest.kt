package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The built-in AI spending a **Treasure** on a spell.
 *
 * The engine's auto-pay solver never sacrifices a source (a player must opt in), while the
 * enumerator counts a Treasure towards what is affordable. So a spell the AI can pay for only with
 * a Treasure is offered, chosen, and then refused at the mana step unless the AI first activates
 * the Treasure and casts from the pool, the way a player does.
 */
class TreasureManaAiTest : FunSpec({

    val ogre = card("Treasure Test Ogre") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Ogre"
        power = 3
        toughness = 3
    }

    /** Plays the AI's priority window until it passes; returns how many of its actions were refused. */
    fun playWindow(driver: GameTestDriver, ai: AIPlayer): Int {
        var refused = 0
        repeat(20) {
            val decision = driver.state.pendingDecision
            if (decision != null) {
                driver.submit(SubmitDecision(decision.playerId, ai.respondToDecision(driver.state, decision)))
                return@repeat
            }
            if (driver.state.priorityPlayerId != ai.playerId) return refused
            val action = ai.chooseAction(driver.state)
            if (action is PassPriority) return refused
            if (driver.submit(action).error != null) {
                refused++
                return refused
            }
        }
        return refused
    }

    test("casts a spell that only a Treasure makes affordable") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(ogre, PredefinedTokens.Treasure))
            // no lands in the deck: the only mana is what the test puts on the battlefield
            initMirrorMatch(Deck.of(ogre.name to 40))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = driver.activePlayer!!
        repeat(2) { driver.putLandOnBattlefield(player, "Mountain") }
        val treasure = driver.putPermanentOnBattlefield(player, "Treasure")
        driver.untapPermanent(treasure)

        val handBefore = driver.getHand(player).size

        val refused = playWindow(driver, AIPlayer.create(driver.cardRegistry, player))

        refused shouldBe 0
        driver.getHand(player).size shouldBe handBefore - 1
        driver.findPermanent(player, "Treasure").shouldBeNull()
    }

    test("leaves the Treasure alone when lands pay the spell") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(ogre, PredefinedTokens.Treasure))
            initMirrorMatch(Deck.of(ogre.name to 40))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = driver.activePlayer!!
        repeat(3) { driver.putLandOnBattlefield(player, "Mountain") }
        val treasure = driver.putPermanentOnBattlefield(player, "Treasure")
        driver.untapPermanent(treasure)

        val handBefore = driver.getHand(player).size

        val refused = playWindow(driver, AIPlayer.create(driver.cardRegistry, player))

        refused shouldBe 0
        driver.getHand(player).size shouldBe handBefore - 1
        driver.findPermanent(player, "Treasure").shouldNotBeNull()
    }
})
