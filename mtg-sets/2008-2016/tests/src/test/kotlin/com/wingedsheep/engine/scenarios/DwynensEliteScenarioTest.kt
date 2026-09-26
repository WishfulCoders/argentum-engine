package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Dwynen's Elite (ORI #173) — {1}{G} Creature — Elf Warrior, 2/2.
 *
 *   When this creature enters, if you control another Elf, create a 1/1 green Elf Warrior
 *   creature token.
 *
 * The Elite is an Elf itself, so the intervening-if must leave it out ("another"). A 17Lands
 * replay (FDN) showed the token made off an otherwise empty board.
 */
class DwynensEliteScenarioTest : ScenarioTestBase() {
    init {
        context("Dwynen's Elite") {
            test("alone, it makes no token: it is not another Elf") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dwynen's Elite")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dwynen's Elite").error shouldBe null
                game.resolveStack()

                game.findPermanents("Elf Warrior Token").size shouldBe 0
            }

            test("with another Elf, it makes one Elf Warrior") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dwynen's Elite")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dwynen's Elite").error shouldBe null
                game.resolveStack()

                game.findPermanents("Elf Warrior Token").size shouldBe 1
            }
        }
    }
}
