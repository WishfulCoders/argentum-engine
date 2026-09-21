package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Twinflame Travelers — {2}{U}{R} Creature — Elemental Sorcerer 3/3.
 *
 *   Flying
 *   If a triggered ability of another Elemental you control triggers, it triggers an additional time.
 *
 * The doubling is a static ability of Twinflame's, so a Twinflame that has lost all abilities
 * (Noggle the Mind) doubles nothing. `TriggerDetector.duplicateSourceTriggers` used to read the
 * doubler straight off the printed card and never asked, so a Noggled Twinflame still doubled a
 * Rimekin Recluse's enters trigger and bounced two creatures (found in a play session, 2026-09-20).
 *
 * Rimekin Recluse — "When this creature enters, return up to one other target creature to its
 * owner's hand." Each firing is its own trigger choosing its own target (CR 603.2d), so a doubled
 * trigger shows up as a second target decision after the first resolves.
 */
class TwinflameTravelersScenarioTest : ScenarioTestBase() {

    init {

        test("doubles another Elemental's enters trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Rimekin Recluse")
                .withCardOnBattlefield(1, "Twinflame Travelers")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Rimekin Recluse").error shouldBe null
            game.resolveStack()

            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            withClue("Twinflame produced a second instance of Rimekin's enters trigger") {
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
            }
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            game.findPermanent("Hill Giant") shouldBe null
        }

        test("a Twinflame that has lost all abilities doubles nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Rimekin Recluse")
                .withCardOnBattlefield(1, "Twinflame Travelers")
                .withCardAttachedTo(2, "Noggle the Mind", "Twinflame Travelers")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val twinflame = game.findPermanent("Twinflame Travelers")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("Noggle has stripped Twinflame before Rimekin is cast") {
                game.state.projectedState.hasLostAllAbilities(twinflame) shouldBe true
            }

            game.castSpell(1, "Rimekin Recluse").error shouldBe null
            game.resolveStack()

            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            withClue("no second trigger is waiting behind the first") {
                game.hasPendingDecision() shouldBe false
            }

            game.findPermanent("Grizzly Bears") shouldBe null
            withClue("the second creature stays: Rimekin bounced exactly one") {
                (game.findPermanent("Hill Giant") != null) shouldBe true
            }
        }
    }
}
