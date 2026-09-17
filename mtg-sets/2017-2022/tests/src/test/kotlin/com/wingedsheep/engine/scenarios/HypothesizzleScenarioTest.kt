package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.grn.cards.Hypothesizzle
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Hypothesizzle (GRN #178) — "Draw two cards. Then you may discard a nonland card. When you do,
 * Hypothesizzle deals 4 damage to target creature."
 *
 * The "When you do" is a reflexive trigger (CR 603.12) that exists only if the discard happened:
 * discarding a nonland card fires it, declining fires nothing, and a hand with no nonland card after
 * the draw means there is no discard to offer at all.
 */
class HypothesizzleScenarioTest : FunSpec({

    class Seen(var prompts: Int = 0, var targetChoices: Int = 0)

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + Hypothesizzle)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast Hypothesizzle and answer its questions: [discard] yes or no, the card, the target. */
    fun GameTestDriver.castAndResolve(you: EntityId, discard: Boolean, card: EntityId?, target: EntityId?): Seen {
        val seen = Seen()
        val spell = putCardInHand(you, "Hypothesizzle")
        giveMana(you, Color.BLUE, 1)
        giveMana(you, Color.RED, 1)
        giveColorlessMana(you, 3)
        castSpell(you, spell).error shouldBe null
        var guard = 0
        while (guard++ < 40) {
            when (val d = pendingDecision) {
                is YesNoDecision -> { seen.prompts++; submitYesNo(d.playerId, discard) }
                is SelectCardsDecision -> submitCardSelection(d.playerId, listOf(card!!))
                is ChooseTargetsDecision -> { seen.targetChoices++; submitTargetSelection(d.playerId, listOf(target!!)) }
                null -> if (state.stack.isNotEmpty()) bothPass() else break
                else -> autoResolveDecision()
            }
        }
        return seen
    }

    test("discarding a nonland card fires the reflexive trigger: 4 damage to the chosen creature") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val lions = driver.putCardInHand(you, "Savannah Lions")

        val seen = driver.castAndResolve(you, discard = true, card = lions, target = courser)

        seen.prompts shouldBe 1
        driver.getGraveyardCardNames(you) shouldContain "Savannah Lions"
        withClue("the 3/3 took 4 damage") {
            driver.getGraveyardCardNames(opponent) shouldContain "Centaur Courser"
        }
    }

    test("declining the discard fires nothing") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val lions = driver.putCardInHand(you, "Savannah Lions")

        val seen = driver.castAndResolve(you, discard = false, card = lions, target = courser)

        seen.prompts shouldBe 1
        seen.targetChoices shouldBe 0
        driver.getHand(you) shouldContain lions
        driver.getCreatures(opponent) shouldContain courser
        driver.getGraveyardCardNames(opponent) shouldNotContain "Centaur Courser"
    }

    test("with only lands in hand there is no discard to offer, and so no trigger") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val handBefore = driver.getHand(you).size

        val seen = driver.castAndResolve(you, discard = true, card = null, target = courser)

        seen.prompts shouldBe 0
        seen.targetChoices shouldBe 0
        withClue("drew two, discarded nothing") {
            driver.getHand(you).size shouldBe handBefore + 2
        }
        driver.getCreatures(opponent) shouldContain courser
    }
})
