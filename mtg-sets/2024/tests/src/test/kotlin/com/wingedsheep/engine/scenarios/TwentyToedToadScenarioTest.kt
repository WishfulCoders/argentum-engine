package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Twenty-Toed Toad — {3}{U} Creature — Frog Wizard 3/3.
 *   Your maximum hand size is twenty.
 *   Whenever you attack with two or more creatures, put a +1/+1 counter on this creature and draw a card.
 *   Whenever this creature attacks, you win the game if there are twenty or more counters on it or
 *   you have twenty or more cards in hand.
 *
 * Pins the ruling that maximum-hand-size effects apply in timestamp order (CR 613.11): Reliquary
 * Tower then the Toad is twenty; the Toad then the Tower is no maximum.
 */
class TwentyToedToadScenarioTest : ScenarioTestBase() {

    private fun TestGame.maxHandSize(): Int? =
        getClientState(1).players.first { it.playerId == player1Id }.maxHandSize

    init {
        context("maximum hand size in timestamp order") {
            test("the Toad entering after Reliquary Tower sets twenty") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reliquary Tower")
                    .withCardInHand(1, "Twenty-Toed Toad")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                withClue("Reliquary Tower alone: no maximum") { game.maxHandSize() shouldBe null }

                game.castSpell(1, "Twenty-Toed Toad").error shouldBe null
                game.resolveStack()
                withClue("the later Toad wins: twenty") { game.maxHandSize() shouldBe 20 }
            }

            test("Reliquary Tower entering after the Toad removes the maximum") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Twenty-Toed Toad")
                    .withCardInHand(1, "Reliquary Tower")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Twenty-Toed Toad").error shouldBe null
                game.resolveStack()
                withClue("Toad alone: twenty") { game.maxHandSize() shouldBe 20 }

                val tower = game.findCardsInHand(1, "Reliquary Tower").single()
                game.execute(PlayLand(game.player1Id, tower)).error shouldBe null
                withClue("the later Tower wins: no maximum") { game.maxHandSize() shouldBe null }
            }
        }

        context("attack triggers") {
            test("attacking with two creatures grows the Toad and draws") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Twenty-Toed Toad")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val toad = game.findPermanent("Twenty-Toed Toad")!!

                game.declareAttackers(mapOf("Twenty-Toed Toad" to 2, "Centaur Courser" to 2)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(toad)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.handSize(1) shouldBe 1
                withClue("one counter and one card — nowhere near twenty") { game.state.gameOver shouldBe false }
            }

            test("attacking alone neither grows it nor draws") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Twenty-Toed Toad")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Twenty-Toed Toad" to 2)).error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 0
            }

            test("attacking with twenty cards in hand wins the game") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Twenty-Toed Toad")
                    .withCardsInHand(1, "Island", 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Twenty-Toed Toad" to 2)).error shouldBe null
                game.resolveStack()

                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player1Id
            }

            test("twenty counters of any kind on it win the game") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Twenty-Toed Toad")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val toad = game.findPermanent("Twenty-Toed Toad")!!
                game.state = game.state.updateEntity(toad) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 10, CounterType.CHARGE to 10)))
                }

                game.declareAttackers(mapOf("Twenty-Toed Toad" to 2)).error shouldBe null
                game.resolveStack()

                game.state.winnerId shouldBe game.player1Id
            }
        }
    }
}
