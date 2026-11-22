package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.GameEndedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Divine Intervention (LEG) — {6}{W}{W} Enchantment:
 * "This enchantment enters with two intervention counters on it. At the beginning of your upkeep,
 * remove an intervention counter from this enchantment. When you remove the last intervention
 * counter from this enchantment, the game is a draw."
 */
class DivineInterventionScenarioTest : ScenarioTestBase() {

    // A {0} instant that removes one intervention counter from a target permanent — whoever casts
    // it is the player who removed the counter.
    private val unwind = card("Unwind Intervention") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Remove an intervention counter from target permanent."
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.RemoveCounters(CounterType.INTERVENTION, 1, t)
        }
    }

    private fun TestGame.interventionCounters(): Int =
        state.getEntity(findPermanent("Divine Intervention")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.INTERVENTION) ?: 0

    private fun TestGame.setInterventionCounters(count: Int) {
        val id = findPermanent("Divine Intervention")!!
        state = state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.INTERVENTION, count)) }
    }

    init {
        cardRegistry.register(unwind)

        context("Divine Intervention") {
            test("enters with two intervention counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Divine Intervention")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divine Intervention")
                game.resolveStack()

                game.interventionCounters() shouldBe 2
            }

            test("the first upkeep removes one counter and the game goes on") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Divine Intervention")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                game.setInterventionCounters(2)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.interventionCounters() shouldBe 1
                game.state.gameOver shouldBe false
            }

            test("removing the last counter at upkeep makes the game a draw, even for a player who can't lose") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Divine Intervention")
                    .withCardOnBattlefield(2, "Platinum Angel")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                game.setInterventionCounters(1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                val events = game.resolveStack().flatMap { it.events }

                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe null
                events shouldContain GameEndedEvent(winnerId = null, reason = GameEndReason.DRAW)
            }

            test("the controller removing the last counter with any other effect also makes it a draw") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Divine Intervention")
                    .withCardInHand(1, "Unwind Intervention")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setInterventionCounters(1)

                game.castSpell(1, "Unwind Intervention", game.findPermanent("Divine Intervention"))
                game.resolveStack()

                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe null
            }

            test("an opponent removing the last counter doesn't trigger it, and nothing can afterwards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Divine Intervention")
                    .withCardInHand(2, "Unwind Intervention")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.setInterventionCounters(1)

                game.castSpell(2, "Unwind Intervention", game.findPermanent("Divine Intervention"))
                game.resolveStack()

                game.interventionCounters() shouldBe 0
                game.state.gameOver shouldBe false
                game.state.stack.isEmpty() shouldBe true

                // With no counter left, its controller's next upkeep removes nothing — no draw.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()
                game.state.gameOver shouldBe false
            }
        }
    }
}
