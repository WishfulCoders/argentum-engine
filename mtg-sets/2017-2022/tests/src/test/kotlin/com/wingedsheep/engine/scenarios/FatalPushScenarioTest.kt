package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fatal Push (AER #57) — {B} Instant.
 *
 * "Destroy target creature if it has mana value 2 or less.
 *  Revolt — Destroy that creature if it has mana value 4 or less instead if a permanent left the
 *  battlefield under your control this turn."
 *
 * Per the 2020-08-07 ruling, any creature is a legal target; the mana value check happens only on
 * resolution. These tests pin the mana-value gate at both thresholds and the Revolt upgrade from
 * "2 or less" to "4 or less" once a permanent has left the battlefield under the caster's control
 * this turn.
 */
class FatalPushScenarioTest : ScenarioTestBase() {

    init {
        context("Fatal Push") {

            test("without revolt, destroys a creature with mana value 2 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fatal Push")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears") // {1}{G}, mana value 2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Fatal Push", bears).error shouldBe null
                game.resolveStack()

                withClue("mana value 2 is destroyed without revolt") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("without revolt, a mana value 3 creature survives") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fatal Push")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Centaur Courser") // {2}{G}, mana value 3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Fatal Push", courser).error shouldBe null
                game.resolveStack()

                withClue("mana value 3 is too high without revolt; the spell fizzles harmlessly") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                    game.isInGraveyard(2, "Centaur Courser") shouldBe false
                }
            }

            test("revolt: a permanent leaving the battlefield under your control raises the bar to 4") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fatal Push")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Llanowar Elves") // will leave the battlefield to set revolt
                    .withCardOnBattlefield(2, "Hill Giant") // {3}{R}, mana value 4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                val moved = game.zones.moveToZone(
                    state = game.state,
                    entityId = elves,
                    destinationZone = Zone.GRAVEYARD
                )
                game.state = moved.state

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Fatal Push", giant).error shouldBe null
                game.resolveStack()

                withClue("revolt is active, so mana value 4 is destroyed instead of surviving") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }
        }
    }
}
