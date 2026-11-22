package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Activating an ability is as reproducible as the rest of the engine (#2705): the same action
 * applied to the same state yields a structurally equal state, so replays and state hashes of an
 * activation don't drift. The activation's resolution key used to be a fresh `UUID` per call.
 */
class ActivationReproducibilityTest : FunSpec({

    val lifeEngine = card("Reproducible Engine") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "{1}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    val manaRock = card("Reproducible Rock") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "{T}: Add {C}."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = AddColorlessManaEffect(1)
            manaAbility = true
        }
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(lifeEngine, manaRock))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Applies [action] twice from the same starting state and returns both results. */
    fun GameTestDriver.twice(action: GameAction): Pair<GameState, GameState> {
        val before = state
        submitSuccess(action)
        val first = state
        replaceState(before)
        submitSuccess(action)
        return first to state
    }

    test("activating an ability that uses the stack twice from one state gives equal states") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val engine = driver.putPermanentOnBattlefield(p1, "Reproducible Engine")
        driver.giveMana(p1, Color.GREEN, 1)

        val (first, second) = driver.twice(
            ActivateAbility(p1, engine, lifeEngine.activatedAbilities.single().id)
        )

        first.stack.size shouldBe 1
        first shouldBe second
    }

    test("activating a mana ability twice from one state gives equal states") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val rock = driver.putPermanentOnBattlefield(p1, "Reproducible Rock")

        val (first, second) = driver.twice(
            ActivateAbility(p1, rock, manaRock.activatedAbilities.single().id)
        )

        first shouldBe second
    }
})
