package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Back for More (IKO #177) — {4}{B}{G} Instant.
 *
 * "Return target creature card from your graveyard to the battlefield. When you do, it fights up
 *  to one target creature you don't control."
 *
 * Same shape as Curse of the Werefox: the fight is a reflexive trigger (CR 603.12) whose target is
 * chosen only after the creature has already returned to the battlefield (2024-04-12 ruling). These
 * tests cover the return + fight happening, the "up to one" decline leaving the returned creature
 * unharmed, and the fight being able to kill the very creature that just came back.
 */
class BackForMoreScenarioTest : ScenarioTestBase() {

    init {
        context("Back for More") {

            test("returns the creature, then it fights and kills the chosen target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Back for More")
                    .withCardInGraveyard(1, "Craw Wurm") // 6/4
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findCardsInGraveyard(1, "Craw Wurm").single()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellTargetingGraveyardCard(1, "Back for More", listOf(wurm)).error shouldBe null
                game.resolveStack() // the Wurm returns -> reflexive fight trigger asks for its target

                withClue("the returned creature is on the battlefield before the fight target is chosen") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                }
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("the 6-power Wurm kills the 2/2; the Wurm survives 2 damage") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                }
            }

            test("\"up to one\" — declining the fight leaves both creatures alone") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Back for More")
                    .withCardInGraveyard(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findCardsInGraveyard(1, "Craw Wurm").single()

                game.castSpellTargetingGraveyardCard(1, "Back for More", listOf(wurm)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.skipTargets().error shouldBe null
                game.resolveStack()

                withClue("no fight happened; both creatures remain") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("the fight can kill the creature that just returned") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Back for More")
                    .withCardInGraveyard(1, "Grizzly Bears") // 2/2
                    .withCardOnBattlefield(2, "Hill Giant") // 3/4
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpellTargetingGraveyardCard(1, "Back for More", listOf(bears)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the 2/2 dies to the Giant's 3 power; the Giant survives 2 damage") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }
        }
    }
}
