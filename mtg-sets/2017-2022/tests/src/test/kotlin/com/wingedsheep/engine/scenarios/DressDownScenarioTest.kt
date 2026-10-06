package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dress Down (MH2 #39) — Flash; enters: draw a card; creatures lose all abilities; sacrifice it at
 * the beginning of the end step. The first card to lower [com.wingedsheep.sdk.scripting.LoseAllAbilities]
 * over every creature rather than one enchanted creature.
 */
class DressDownScenarioTest : ScenarioTestBase() {
    init {
        test("creatures lose their keywords until Dress Down is sacrificed at the end step") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardInHand(1, "Dress Down")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardOnBattlefield(2, "Serra Angel")
                .apply { repeat(3) { withCardInLibrary(1, "Island") } }
                .apply { repeat(3) { withCardInLibrary(2, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val angel = game.findPermanent("Serra Angel")!!
            game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true

            game.castSpell(1, "Dress Down").error shouldBe null
            game.resolveStack()

            withClue("the enters trigger drew a card") { game.handSize(1) shouldBe 1 }
            withClue("the Angel lost flying and vigilance") {
                game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe false
                game.state.projectedState.hasKeyword(angel, Keyword.VIGILANCE) shouldBe false
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("sacrificed at the beginning of the end step; the Angel flies again") {
                game.isInGraveyard(1, "Dress Down") shouldBe true
                game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true
            }
        }

        test("a creature entering while Dress Down is out has no enters trigger") {
            val game = scenario().withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Dress Down")
                .withCardInHand(1, "Elvish Visionary")
                .withLandsOnBattlefield(1, "Forest", 2)
                .apply { repeat(3) { withCardInLibrary(1, "Island") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Elvish Visionary").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Elvish Visionary") shouldBe true
            withClue("Visionary's draw trigger was removed with its other abilities") {
                game.handSize(1) shouldBe 0
            }
        }
    }
}
