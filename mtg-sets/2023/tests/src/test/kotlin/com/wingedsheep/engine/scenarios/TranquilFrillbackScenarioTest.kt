package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Tranquil Frillback (March of the Machine: The Aftermath #24).
 *
 * When this creature enters, you may pay {G} up to three times. When you pay this cost one or
 * more times, choose up to that many —
 * • Destroy target artifact or enchantment.
 * • Exile target player's graveyard.
 * • You gain 4 life.
 *
 * The repeated-payment primitive is covered by `PayManaCostRepeatedlyTest`; these cover the card:
 * the payment count caps the modes, the {G} unit is colour-checked, and each mode does what it says.
 */
class TranquilFrillbackScenarioTest : ScenarioTestBase() {

    private val relic = card("Test Relic") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }

    private fun chooseMode(game: TestGame, label: String) {
        val decision = game.getPendingDecision() as? ChooseOptionDecision
            ?: error("expected a mode question; got ${game.getPendingDecision()}")
        val index = decision.options.indexOfFirst { it.contains(label, ignoreCase = true) }
        check(index >= 0) { "$label not offered; options=${decision.options}" }
        game.submitDecision(OptionChosenResponse(decision.id, optionIndex = index))
    }

    /** Target the opponent for the player requirement, the Relic for the artifact one. */
    private fun answerTargets(game: TestGame) {
        while (true) {
            val decision = game.getPendingDecision() as? ChooseTargetsDecision ?: return
            val relicId = game.findPermanent("Test Relic")
            val picks = decision.targetRequirements.associate { requirement ->
                val legal = decision.legalTargets[requirement.index].orEmpty()
                val pick = legal.firstOrNull { it == game.player2Id }
                    ?: legal.firstOrNull { it == relicId }
                    ?: legal.first()
                requirement.index to listOf(pick)
            }
            game.submitDecision(TargetsResponse(decision.id, picks))
        }
    }

    /** Cast Frillback off three Forests, leaving [forests] Forests and [mountains] Mountains. */
    private fun castFrillback(forests: Int, mountains: Int = 0): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Tranquil Frillback")
            .withLandsOnBattlefield(1, "Forest", forests + 3)
            .withCardOnBattlefield(2, "Test Relic")
            .withCardInGraveyard(2, "Grizzly Bears")
            .withCardInGraveyard(2, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (mountains > 0) builder.withLandsOnBattlefield(1, "Mountain", mountains)
        val game = builder.build()
        game.castSpell(1, "Tranquil Frillback").error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        cardRegistry.register(relic)

        test("paying {G} three times allows all three modes") {
            val game = castFrillback(forests = 4)
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)

            val count = game.getPendingDecision() as? ChooseNumberDecision
                ?: error("expected the repeat-count question; got ${game.getPendingDecision()}")
            withClue("'up to three times', even with four Forests open") { count.maxValue shouldBe 3 }
            game.chooseNumber(3)

            chooseMode(game, "Destroy")
            chooseMode(game, "Exile")
            chooseMode(game, "gain 4 life")
            answerTargets(game)
            game.resolveStack()

            withClue("the artifact was destroyed") { game.findPermanent("Test Relic") shouldBe null }
            withClue("the opponent's graveyard was exiled") {
                game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 0
                game.findCardsInGraveyard(2, "Forest").size shouldBe 0
            }
            game.getLifeTotal(1) shouldBe 24
        }

        test("paying once allows exactly one mode") {
            val game = castFrillback(forests = 3)
            game.answerYesNo(true)
            game.chooseNumber(1)

            chooseMode(game, "gain 4 life")
            withClue("one payment, one mode") {
                (game.getPendingDecision() is ChooseOptionDecision) shouldBe false
            }
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 24
            game.findPermanent("Test Relic") shouldNotBe null
            game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 1
        }

        test("the {G} unit needs green mana — Mountains don't raise the cap") {
            val game = castFrillback(forests = 1, mountains = 3)
            game.answerYesNo(true)
            withClue("one green source means one repetition, paid without a count prompt") {
                (game.getPendingDecision() is ChooseNumberDecision) shouldBe false
            }
            chooseMode(game, "Destroy")
            answerTargets(game)
            game.resolveStack()
            game.findPermanent("Test Relic") shouldBe null
        }

        test("declining the payment chooses no modes") {
            val game = castFrillback(forests = 3)
            game.answerYesNo(false)
            game.resolveStack()

            (game.getPendingDecision() is ChooseOptionDecision) shouldBe false
            game.getLifeTotal(1) shouldBe 20
            game.findPermanent("Test Relic") shouldNotBe null
        }
    }
}
