package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Graveyard Trespasser // Graveyard Glutton (MID #104).
 *
 * Front (3/3, daybound): whenever it enters or attacks, exile up to one target card from a graveyard;
 * if a creature card was exiled this way, each opponent loses 1 and you gain 1.
 * Back (4/4, nightbound): the same trigger exiles up to two target cards from graveyards and drains 1
 * for *each* creature card exiled this way.
 */
class GraveyardTrespasserScenarioTest : ScenarioTestBase() {

    init {
        context("Graveyard Trespasser — front face") {

            test("ETB exiling a creature card drains each opponent for 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graveyard Trespasser")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Graveyard Trespasser").error shouldBe null
                game.resolveStack()

                val giant = game.findCardsInGraveyard(2, "Hill Giant").first()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the creature card is exiled") { game.isInExile(2, "Hill Giant") shouldBe true }
                withClue("drain 1") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                }
            }

            test("attack trigger exiling a noncreature card does not drain") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Graveyard Trespasser")
                    .withCardInGraveyard(2, "Mountain")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.copy(dayNight = DayNight.DAY)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Graveyard Trespasser" to 2)).error shouldBe null

                val mountain = game.findCardsInGraveyard(2, "Mountain").first()
                game.selectTargets(listOf(mountain)).error shouldBe null
                game.resolveStack()

                withClue("the land card is exiled") { game.isInExile(2, "Mountain") shouldBe true }
                withClue("no creature exiled -> no drain") {
                    game.getLifeTotal(2) shouldBe 20
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }

        context("Graveyard Glutton — night face") {

            test("cast at night it enters as Glutton and drains once per creature card exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graveyard Trespasser")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.copy(dayNight = DayNight.NIGHT)

                game.castSpell(1, "Graveyard Trespasser").error shouldBe null
                game.resolveStack()

                withClue("a daybound permanent enters night-face up at night") {
                    game.findPermanent("Graveyard Glutton") shouldNotBe null
                }

                val giant = game.findCardsInGraveyard(2, "Hill Giant").first()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                game.selectTargets(listOf(giant, bears)).error shouldBe null
                game.resolveStack()

                withClue("both creature cards exiled") {
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                }
                withClue("two creature cards -> drain 2") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("only creature cards count toward the drain") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Graveyard Trespasser")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInGraveyard(2, "Mountain")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.copy(dayNight = DayNight.NIGHT)

                game.castSpell(1, "Graveyard Trespasser").error shouldBe null
                game.resolveStack()

                val giant = game.findCardsInGraveyard(2, "Hill Giant").first()
                val mountain = game.findCardsInGraveyard(2, "Mountain").first()
                game.selectTargets(listOf(giant, mountain)).error shouldBe null
                game.resolveStack()

                withClue("one creature card -> drain 1") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                }
            }
        }
    }
}
