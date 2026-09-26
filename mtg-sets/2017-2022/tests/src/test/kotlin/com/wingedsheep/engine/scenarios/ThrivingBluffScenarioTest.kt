package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thriving Bluff — "This land enters tapped. As it enters, choose a color other than red.
 * {T}: Add {R} or one mana of the chosen color."
 *
 * Exercises the two new SDK axes the Thriving cycle needs: `EntersWithChoice.excludedColors`
 * (the choice offers four colors and the engine refuses the excluded one) and
 * `ManaColorSet.Union` (the land taps for its fixed color *or* the chosen one, and nothing else).
 */
class ThrivingBluffScenarioTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    /** Play Thriving Bluff, answer its color choice with [color], and untap it for the test. */
    fun GameTestDriver.playBluffChoosing(player: EntityId, color: Color): EntityId {
        val bluff = putCardInHand(player, "Thriving Bluff")
        playLand(player, bluff)
        val decision = pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        submitDecision(player, ColorChosenResponse(decision.id, color))
        untapPermanent(bluff)
        return bluff
    }

    test("the entry choice offers every color but red and refuses red") {
        val (driver, player) = newGame()
        val bluff = driver.putCardInHand(player, "Thriving Bluff")
        driver.playLand(player, bluff)

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        withClue("'choose a color other than red'") {
            decision.availableColors shouldBe setOf(Color.WHITE, Color.BLUE, Color.BLACK, Color.GREEN)
            decision.prompt shouldBe "Choose a color other than red"
        }

        driver.submitExpectFailure(SubmitDecision(player, ColorChosenResponse(decision.id, Color.RED)))
        withClue("A refused answer leaves the choice open") {
            driver.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        }

        driver.submitDecision(player, ColorChosenResponse(decision.id, Color.BLUE))
        withClue("The land enters tapped with blue recorded as its chosen color") {
            driver.isTapped(bluff) shouldBe true
            driver.state.getEntity(bluff)!!.chosenColor() shouldBe Color.BLUE
        }
    }

    test("taps for the chosen color") {
        val (driver, player) = newGame()
        driver.playBluffChoosing(player, Color.BLUE)

        val opt = driver.putCardInHand(player, "Opt")
        driver.castSpell(player, opt)
        withClue("Opt ({U}) is paid by the Bluff's chosen-color mana") {
            driver.getStackSpellNames() shouldBe listOf("Opt")
        }
    }

    test("taps for red") {
        val (driver, player) = newGame()
        driver.playBluffChoosing(player, Color.BLUE)

        val shock = driver.putCardInHand(player, "Shock")
        driver.castSpell(player, shock, listOf(driver.getOpponent(player)))
        withClue("Shock ({R}) is paid by the Bluff's fixed red mana") {
            driver.getStackSpellNames() shouldBe listOf("Shock")
        }
    }

    test("does not tap for a color it was not given") {
        val (driver, player) = newGame()
        driver.playBluffChoosing(player, Color.BLUE)

        val growth = driver.putCardInHand(player, "Giant Growth")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.submitExpectFailure(
            com.wingedsheep.engine.core.CastSpell(
                playerId = player,
                cardId = growth,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bear)),
                paymentStrategy = com.wingedsheep.engine.core.PaymentStrategy.AutoPay
            )
        )
        withClue("Giant Growth ({G}) can't be paid from a Bluff that chose blue") {
            driver.getStackSpellNames() shouldBe emptyList()
        }
    }
})
