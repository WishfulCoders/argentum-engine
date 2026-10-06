package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.CantLoseLifeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jou.cards.ManaConfluence
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Mana Confluence (JOU #163): {T}, Pay 1 life: Add one mana of any color.
 *
 * The life is part of the mana ability's cost, so every auto-pay path that taps Confluence must
 * charge it — not only spell casting, but also an activated ability's mana cost (the
 * activation auto-tapper used to tap it for free) — and auto-pay must not tap it at all when the
 * life can't be paid (CR 119.4).
 */
class ManaConfluenceScenarioTest : FunSpec({

    val battery = card("Test Confluence Battery") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.DrawCards(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ManaConfluence + battery)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("auto-pay tapping it for a spell charges 1 life") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val confluence = driver.putLandOnBattlefield(p1, "Mana Confluence")

        val guide = driver.putCardInHand(p1, "Goblin Guide")
        driver.castSpell(p1, guide).error shouldBe null

        driver.isTapped(confluence).shouldBeTrue()
        driver.getLifeTotal(p1) shouldBe 19
        driver.getStackSpellNames() shouldContain "Goblin Guide"
    }

    test("auto-pay tapping it for an activated ability's mana cost charges 1 life") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val confluence = driver.putLandOnBattlefield(p1, "Mana Confluence")
        val artifact = driver.putPermanentOnBattlefield(p1, "Test Confluence Battery")
        val abilityId = battery.activatedAbilities.first().id

        driver.submitSuccess(ActivateAbility(p1, artifact, abilityId))

        driver.isTapped(confluence).shouldBeTrue()
        driver.getLifeTotal(p1) shouldBe 19
    }

    test("auto-pay won't tap it when its controller can't pay the life") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val confluence = driver.putLandOnBattlefield(p1, "Mana Confluence")
        driver.replaceState(driver.state.updateEntity(p1) { it.with(CantLoseLifeComponent()) })

        val guide = driver.putCardInHand(p1, "Goblin Guide")
        driver.castSpell(p1, guide).error.shouldNotBeNull()

        driver.isTapped(confluence).shouldBeFalse()
        driver.getLifeTotal(p1) shouldBe 20
    }
})
