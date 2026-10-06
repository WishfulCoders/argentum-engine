package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jadar, Ghoulcaller of Nephalia (MID #108) — "At the beginning of your end step, if you control no
 * creatures with decayed, create a 2/2 black Zombie creature token with decayed."
 *
 * The intervening-if must see the decayed Zombie Jadar made on an earlier turn: the token carries
 * Decayed as a counter, and the condition reads the projected keyword.
 */
class JadarGhoulcallerOfNephaliaScenarioTest : ScenarioTestBase() {
    init {
        test("makes a decayed Zombie, then makes none while that Zombie survives") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Jadar, Ghoulcaller of Nephalia")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            val zombies = game.findPermanents("Zombie Token")
            withClue("first end step: one Zombie") { zombies.size shouldBe 1 }
            withClue("the Zombie has decayed") {
                game.state.getEntity(zombies.single())!!.get<CountersComponent>()!!
                    .getCount(CounterType.DECAYED) shouldBe 1
            }

            // P2's end step is not "your" end step.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            withClue("opponent's end step: nothing") { game.findPermanents("Zombie Token").size shouldBe 1 }

            // P1's next end step: the decayed Zombie is still around, so the condition fails.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            withClue("a creature with decayed exists: no new Zombie") {
                game.findPermanents("Zombie Token").size shouldBe 1
            }
        }

        test("a non-decayed creature does not stop the trigger") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Jadar, Ghoulcaller of Nephalia")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.findPermanents("Zombie Token").size shouldBe 1
        }
    }
}
