package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ulg.cards.MemoryJar
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe

/**
 * Memory Jar — every hand goes into its owner's exile face down, everyone draws seven, and at the
 * next end step each player discards their hand and gets back exactly the cards they exiled.
 * Pins the cross-turn delayed trigger, the per-owner return, and that the exiled cards are face down.
 */
class MemoryJarScenarioTest : FunSpec({

    val abilityId = MemoryJar.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MemoryJar)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("hands are exiled face down, seven drawn, and swapped back at the next end step") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCardInHand(me, "Lightning Bolt")
        driver.putCardInHand(opp, "Savannah Lions")
        val myOldHand = driver.getHand(me)
        val oppOldHand = driver.getHand(opp)
        val jar = driver.putPermanentOnBattlefield(me, "Memory Jar")

        driver.submit(ActivateAbility(me, jar, abilityId)).outcome shouldBe Outcome.Done
        driver.bothPass()

        // Each player's old hand sits in their own exile, face down; each drew a fresh seven.
        driver.getExile(me) shouldContainExactlyInAnyOrder myOldHand
        driver.getExile(opp) shouldContainExactlyInAnyOrder oppOldHand
        (myOldHand + oppOldHand).forEach { driver.state.getEntity(it)!!.has<FaceDownComponent>() shouldBe true }
        driver.getHandSize(me) shouldBe 7
        driver.getHandSize(opp) shouldBe 7
        val myJarHand = driver.getHand(me)
        val oppJarHand = driver.getHand(opp)
        myJarHand shouldNotContainAnyOf myOldHand
        driver.getGraveyardCardNames(me) shouldBe listOf("Memory Jar")

        // The delayed trigger waits for the end step.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.getHand(me) shouldContainExactlyInAnyOrder myOldHand
        driver.getHand(opp) shouldContainExactlyInAnyOrder oppOldHand
        driver.getGraveyard(me) shouldContainExactlyInAnyOrder myJarHand + jar
        driver.getGraveyard(opp) shouldContainExactlyInAnyOrder oppJarHand
        driver.getExile(me).size shouldBe 0
        driver.getExile(opp).size shouldBe 0
        myOldHand.forEach { driver.state.getEntity(it)!!.has<FaceDownComponent>() shouldBe false }
    }
})
