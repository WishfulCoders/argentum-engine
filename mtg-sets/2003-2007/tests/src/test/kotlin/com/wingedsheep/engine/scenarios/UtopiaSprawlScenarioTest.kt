package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Utopia Sprawl (DIS #99).
 *
 * "{G} Enchantment — Aura
 *  Enchant Forest
 *  As this Aura enters, choose a color.
 *  Whenever enchanted Forest is tapped for mana, its controller adds an additional one mana of
 *  the chosen color."
 *
 * Unlike Shimmerwilds Growth (its color-changing cousin), Utopia Sprawl does not touch the
 * Forest's own {T}: Add {G} ability at all — it only adds one extra mana of the chosen color on
 * top. Tapping the enchanted Forest should therefore always still produce {G}, plus one mana of
 * whatever color was chosen (which may itself be green, stacking to {G}{G}).
 */
class UtopiaSprawlScenarioTest : FunSpec({

    // Explicit test Forest so we can reference its mana ability id directly.
    val TestForest = basicLand("Forest") {}
    val manaAbilityId = TestForest.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestForest))
        return driver
    }

    test("tapping the enchanted Forest produces its normal green plus one mana of the chosen color") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")
        val sprawl = driver.putCardInHand(activePlayer, "Utopia Sprawl")
        driver.giveMana(activePlayer, Color.GREEN, 1)
        driver.castSpell(activePlayer, sprawl, listOf(forest))
        driver.bothPass()

        // The EntersWithChoice replacement pauses for the color choice as the Aura enters.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        val decision = driver.pendingDecision as ChooseColorDecision
        driver.submitDecision(activePlayer, ColorChosenResponse(decision.id, Color.BLUE))

        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = forest, abilityId = manaAbilityId)
        )
        result.outcome shouldBe Outcome.Done

        val pool = driver.state.getEntity(activePlayer)!!.get<ManaPoolComponent>()!!
        pool.green shouldBe 1
        pool.blue shouldBe 1
    }

    test("choosing green stacks with the Forest's own green, yielding two green") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")
        val sprawl = driver.putCardInHand(activePlayer, "Utopia Sprawl")
        driver.giveMana(activePlayer, Color.GREEN, 1)
        driver.castSpell(activePlayer, sprawl, listOf(forest))
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseColorDecision
        driver.submitDecision(activePlayer, ColorChosenResponse(decision.id, Color.GREEN))

        driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = forest, abilityId = manaAbilityId)
        ).outcome shouldBe Outcome.Done

        val pool = driver.state.getEntity(activePlayer)!!.get<ManaPoolComponent>()!!
        pool.green shouldBe 2
    }

    test("an unenchanted Forest still just taps for one green") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val forest = driver.putPermanentOnBattlefield(activePlayer, "Forest")

        driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = forest, abilityId = manaAbilityId)
        ).outcome shouldBe Outcome.Done

        val pool = driver.state.getEntity(activePlayer)!!.get<ManaPoolComponent>()!!
        pool.green shouldBe 1
    }
})
