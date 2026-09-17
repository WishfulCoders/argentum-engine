package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rtr.cards.DetentionSphere
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Detention Sphere (RTR #155): the targeted permanent and every other permanent with its name are
 * exiled, a differently named one stays, and all of them come back when the Sphere leaves.
 */
class DetentionSphereScenarioTest : FunSpec({

    val shatter = card("Test Enchantment Shatter") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target("target enchantment", Targets.Enchantment)
            effect = Effects.Destroy(t)
        }
    }

    fun GameTestDriver.settle(target: EntityId? = null) {
        var guard = 0
        while (guard++ < 30) {
            when (val d = pendingDecision) {
                is YesNoDecision -> submitYesNo(d.playerId, true)
                is ChooseTargetsDecision -> submitTargetSelection(d.playerId, listOf(target!!))
                null -> if (state.stack.isNotEmpty()) bothPass() else break
                else -> autoResolveDecision()
            }
        }
    }

    test("exiles the target and its namesakes, and returns them when it leaves") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DetentionSphere, shatter))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val lionsA = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val lionsB = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        val sphere = driver.putCardInHand(you, "Detention Sphere")
        driver.giveMana(you, Color.WHITE, 1)
        driver.giveMana(you, Color.BLUE, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, sphere).error shouldBe null
        driver.settle(target = lionsA)

        withClue("both Lions gone, the Courser untouched") {
            driver.getExileCardNames(opponent).count { it == "Savannah Lions" } shouldBe 2
            driver.getCreatures(opponent) shouldContain courser
        }

        val onBattlefield = driver.findPermanent(you, "Detention Sphere")!!
        val shatterCard = driver.putCardInHand(you, "Test Enchantment Shatter")
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, shatterCard, listOf(onBattlefield)).error shouldBe null
        driver.settle()

        withClue("the Sphere left, so the exiled cards return") {
            driver.getCreatures(opponent).count { driver.getCardName(it) == "Savannah Lions" } shouldBe 2
            driver.getExileCardNames(opponent) shouldNotContain "Savannah Lions"
        }
    }
})
