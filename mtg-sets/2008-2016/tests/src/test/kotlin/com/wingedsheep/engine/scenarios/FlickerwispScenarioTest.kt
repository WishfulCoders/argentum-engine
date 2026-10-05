package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eve.cards.Flickerwisp
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Flickerwisp (EVE #6): "When this creature enters, exile another target permanent. Return that
 * card to the battlefield under its owner's control at the beginning of the next end step."
 */
class FlickerwispScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Flickerwisp)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        return Triple(driver, me, driver.getOpponent(me))
    }

    /** Cast Flickerwisp and aim its ETB trigger at [target]. */
    fun GameTestDriver.castWispAt(me: EntityId, target: EntityId) {
        giveMana(me, Color.WHITE, 3)
        val wisp = putCardInHand(me, "Flickerwisp")
        castSpell(me, wisp)
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 50) {
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> submitTargetSelection(me, listOf(target))
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    fun GameTestDriver.bearsOnBattlefield(): List<EntityId> = state.getBattlefield().filter {
        state.getEntity(it)?.get<CardComponent>()?.name == "Centaur Courser"
    }

    test("exiles another permanent and returns it at the beginning of the next end step") {
        val (driver, me, opponent) = setup()
        val bears = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.castWispAt(me, bears)
        driver.bearsOnBattlefield() shouldBe emptyList()
        driver.getExileCardNames(opponent).contains("Centaur Courser") shouldBe true

        driver.passPriorityUntil(Step.END)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 20) { driver.bothPass(); guard++ }

        val returned = driver.bearsOnBattlefield()
        returned.size shouldBe 1
        driver.state.getEntity(returned.single())?.get<ControllerComponent>()?.playerId shouldBe opponent
    }

    test("a stolen permanent comes back under its owner's control") {
        val (driver, me, opponent) = setup()
        val bears = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.replaceState(driver.state.updateEntity(bears) { it.with(ControllerComponent(me)) })

        driver.castWispAt(me, bears)
        driver.passPriorityUntil(Step.END)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 20) { driver.bothPass(); guard++ }

        val returned = driver.bearsOnBattlefield()
        returned.size shouldBe 1
        driver.state.getEntity(returned.single())?.get<ControllerComponent>()?.playerId shouldBe opponent
    }
})
