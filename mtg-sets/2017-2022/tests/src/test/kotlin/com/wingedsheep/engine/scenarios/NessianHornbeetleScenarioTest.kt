package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nessian Hornbeetle (THB #182, reprinted in FDN) — {1}{G} Creature — Insect, 2/2.
 *
 *   At the beginning of combat on your turn, if you control another creature with power 4 or
 *   greater, put a +1/+1 counter on this creature.
 *
 * Once its own counters take it to power 4 the Hornbeetle must not satisfy its own condition.
 */
class NessianHornbeetleScenarioTest : ScenarioTestBase() {
    private fun counters(game: TestGame): Int =
        game.state.getEntity(game.findPermanent("Nessian Hornbeetle")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Nessian Hornbeetle") {
            test("a power-4 Hornbeetle alone gets no counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nessian Hornbeetle")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val beetle = game.findPermanent("Nessian Hornbeetle")!!
                game.state = game.state.updateEntity(beetle) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                counters(game) shouldBe 2
            }

            test("another creature with power 4 or greater gives it a counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nessian Hornbeetle")
                    .withCardOnBattlefield(1, "Colossal Dreadmaw")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                counters(game) shouldBe 1
            }
        }
    }
}
