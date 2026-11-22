package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Morgul-Knife Wound (LTR #98): "Enchanted creature gets -3/-0 and has 'At the beginning of your
 * upkeep, exile this creature unless you pay 2 life.'"
 *
 * The granted upkeep trigger asks its pay-or-suffer question in one sentence naming the enchanted
 * creature — the prompt used to read "Pay 2 life to avoid Exile this creature?".
 */
class MorgulKnifeWoundScenarioTest : ScenarioTestBase() {

    init {
        context("Morgul-Knife Wound") {

            fun board() = scenario()
                .withPlayers("Wounded", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(2, "Morgul-Knife Wound", "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            test("the upkeep prompt names the enchanted creature and reads as one sentence") {
                val game = board()
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<YesNoDecision>()
                decision.prompt shouldBe "Pay 2 life or exile Grizzly Bears?"
            }

            test("paying keeps the creature; declining exiles it") {
                val paid = board()
                paid.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                paid.resolveStack()
                paid.answerYesNo(true)
                withClue("paid 2 life, Bears stay") {
                    paid.getLifeTotal(1) shouldBe 18
                    (paid.findPermanent("Grizzly Bears") != null) shouldBe true
                }

                val declined = board()
                declined.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                declined.resolveStack()
                declined.answerYesNo(false)
                withClue("declined, Bears exiled") {
                    declined.getLifeTotal(1) shouldBe 20
                    declined.isInExile(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
