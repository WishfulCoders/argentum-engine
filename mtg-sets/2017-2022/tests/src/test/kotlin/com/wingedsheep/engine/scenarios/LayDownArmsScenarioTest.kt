package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Lay Down Arms (BRO #11) — {W} Sorcery.
 *
 *   Exile target creature with mana value less than or equal to the number of Plains you control.
 *   Its controller gains 3 life.
 *
 * The cap is a targeting restriction over a dynamic Plains count: only Plains count (a Mountain
 * doesn't), and a creature above the cap can't be targeted at all. The life goes to the exiled
 * creature's controller, not the caster.
 */
class LayDownArmsScenarioTest : ScenarioTestBase() {

    init {
        context("Lay Down Arms") {

            test("exiles a creature within the Plains count and its controller gains 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lay Down Arms")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Lay Down Arms", bears)
                withClue("Mana value 2 ≤ two Plains, so the target is legal: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears is exiled") {
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                withClue("The creature's controller gains 3 life") {
                    game.getLifeTotal(2) shouldBe 23
                }
                withClue("The caster gains nothing") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("a creature above the Plains count can't be targeted; non-Plains lands don't count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lay Down Arms")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Lay Down Arms", bears)
                withClue("Mana value 2 > one Plains, so targeting is rejected") {
                    cast.error shouldNotBe null
                }
                withClue("Grizzly Bears stays on the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe bears
                }
            }
        }
    }
}
