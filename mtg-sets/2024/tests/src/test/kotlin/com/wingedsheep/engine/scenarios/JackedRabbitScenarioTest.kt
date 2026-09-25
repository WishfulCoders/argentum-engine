package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jacked Rabbit — {X}{1}{W} Creature — Rabbit Warrior 1/2.
 *   Ravenous (This creature enters with X +1/+1 counters on it. If X is 5 or more, draw a card
 *   when it enters.)
 *   Whenever this creature attacks, create a number of 1/1 white Rabbit creature tokens equal to
 *   this creature's power.
 *
 * Pins Ravenous (CR 702.156a) on both sides of the X ≥ 5 line, and the power-scaled attack trigger.
 */
class JackedRabbitScenarioTest : ScenarioTestBase() {

    private fun rabbitCounters(game: TestGame): Int {
        val rabbit = game.findPermanent("Jacked Rabbit")!!
        return game.state.getEntity(rabbit)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Ravenous") {
            test("X = 4 enters with four counters and draws nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Jacked Rabbit")
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Jacked Rabbit", xValue = 4).error shouldBe null
                game.resolveStack()

                withClue("enters with X = 4 +1/+1 counters") { rabbitCounters(game) shouldBe 4 }
                withClue("X < 5 — no card drawn") { game.handSize(1) shouldBe 0 }
            }

            test("X = 5 enters with five counters and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Jacked Rabbit")
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Plains", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Jacked Rabbit", xValue = 5).error shouldBe null
                game.resolveStack()

                withClue("enters with X = 5 +1/+1 counters") { rabbitCounters(game) shouldBe 5 }
                withClue("X >= 5 — draws one card") { game.handSize(1) shouldBe 1 }
            }

            test("put onto the battlefield without being cast, X is 0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Jacked Rabbit")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                rabbitCounters(game) shouldBe 0
            }
        }

        context("attack trigger") {
            test("creates Rabbit tokens equal to its power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Jacked Rabbit")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val rabbit = game.findPermanent("Jacked Rabbit")!!
                game.state = game.state.updateEntity(rabbit) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }

                game.declareAttackers(mapOf("Jacked Rabbit" to 2)).error shouldBe null
                game.resolveStack()

                withClue("a 3/4 Jacked Rabbit makes three 1/1 Rabbits") {
                    game.findAllPermanents("Rabbit Token").size shouldBe 3
                }
            }
        }
    }
}
