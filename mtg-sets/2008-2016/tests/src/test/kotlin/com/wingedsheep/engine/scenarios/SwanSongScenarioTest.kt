package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Swan Song (THS).
 *
 * Oracle: "Counter target enchantment, instant, or sorcery spell. Its controller creates a 2/2 blue
 * Bird creature token with flying."
 *
 * The target is a type union on the stack; the Bird goes to the countered spell's controller, and
 * (per the 2013-09-15 ruling) still does when that spell can't be countered.
 */
class SwanSongScenarioTest : ScenarioTestBase() {

    init {
        context("Swan Song") {
            test("counters an instant; its controller creates a 2/2 blue flying Bird") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Swan Song")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.passPriority()

                val swan = game.castSpellTargetingStackSpell(1, "Swan Song", "Lightning Bolt")
                withClue("Casting Swan Song should succeed: ${swan.error}") {
                    swan.error shouldBe null
                }

                game.resolveStack()

                withClue("Lightning Bolt should be countered") {
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                    game.getLifeTotal(1) shouldBe lifeBefore
                }

                val birds = game.findAllPermanents("Bird Token")
                withClue("the countered spell's controller (Player 2) creates one Bird") {
                    birds.size shouldBe 1
                }
                val bird = birds.single()
                val projected = game.state.projectedState
                withClue("the Bird is a 2/2 blue flyer controlled by Player 2") {
                    game.state.getEntity(bird)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                    projected.getPower(bird) shouldBe 2
                    projected.getToughness(bird) shouldBe 2
                    projected.hasColor(bird, Color.BLUE) shouldBe true
                    projected.hasKeyword(bird, Keyword.FLYING) shouldBe true
                }
            }

            test("counters an enchantment spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Swan Song")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Glorious Anthem")
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Glorious Anthem").error shouldBe null
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "Swan Song", "Glorious Anthem").error shouldBe null
                game.resolveStack()

                withClue("Glorious Anthem should be countered, and Player 2 gets the Bird") {
                    game.isInGraveyard(2, "Glorious Anthem") shouldBe true
                    game.isOnBattlefield("Glorious Anthem") shouldBe false
                    game.findAllPermanents("Bird Token").size shouldBe 1
                }
            }

            test("cannot target a creature spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Swan Song")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                withClue("targeting a creature spell must be rejected") {
                    game.castSpellTargetingStackSpell(1, "Swan Song", "Grizzly Bears").error shouldNotBe null
                }
            }

            test("a spell that can't be countered still resolves, but its controller gets a Bird") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Swan Song")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Abrupt Decay")
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Abrupt Decay", bears).error shouldBe null
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "Swan Song", "Abrupt Decay").error shouldBe null
                game.resolveStack()

                withClue("Abrupt Decay can't be countered, so it still destroys Grizzly Bears") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                val birds = game.findAllPermanents("Bird Token")
                withClue("Player 2 still creates the Bird") {
                    birds.size shouldBe 1
                    game.state.getEntity(birds.single())?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                }
            }
        }
    }
}
