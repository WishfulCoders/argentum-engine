package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [DynamicAmount.LeastAmongPlayers] — "the number of lands controlled by the player who controls
 * the fewest" (Balance). The minimum twin of [DynamicAmount.GreatestAmongPlayers]: one number per
 * player, the inner amount evaluated *as* that player, and the smallest wins.
 *
 * Pinned here, at the evaluator, in a three-player game so the per-player boundary is visible:
 * a table-wide count (`AggregateBattlefield(Player.Each, …)`) or a pairwise `Min(you, opponent)`
 * would give a different answer for at least one of these boards.
 */
class LeastAmongPlayersTest : FunSpec({

    fun threePlayers(): Pair<GameTestDriver, List<EntityId>> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        val players = driver.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
        return driver to players
    }

    fun GameTestDriver.evaluate(amount: DynamicAmount, asPlayer: EntityId): Int =
        PredicateEvaluator(cardRegistry = cardRegistry).amounts.evaluate(
            state, amount, EffectContext(sourceId = null, controllerId = asPlayer)
        )

    val fewestLands = DynamicAmounts.fewestControlledBySinglePlayer(GameObjectFilter.Land)

    test("takes the smallest per-player count, not the table total or a pairwise minimum") {
        val (driver, players) = threePlayers()
        repeat(5) { driver.putLandOnBattlefield(players[0], "Mountain") }
        repeat(2) { driver.putLandOnBattlefield(players[1], "Mountain") }
        repeat(4) { driver.putLandOnBattlefield(players[2], "Mountain") }

        for (viewer in players) {
            withClue("the answer is the same whichever player's ability asks") {
                driver.evaluate(fewestLands, viewer) shouldBe 2
            }
        }
    }

    test("a player who controls none of the counted objects makes the answer zero") {
        val (driver, players) = threePlayers()
        repeat(3) { driver.putLandOnBattlefield(players[0], "Mountain") }
        repeat(3) { driver.putLandOnBattlefield(players[2], "Mountain") }

        driver.evaluate(fewestLands, players[0]) shouldBe 0
    }

    test("EachOpponent measures only the asking player's opponents") {
        val (driver, players) = threePlayers()
        driver.putLandOnBattlefield(players[0], "Mountain")
        repeat(3) { driver.putLandOnBattlefield(players[1], "Mountain") }
        repeat(6) { driver.putLandOnBattlefield(players[2], "Mountain") }

        val fewestAmongOpponents = DynamicAmounts.fewestControlledBySinglePlayer(
            GameObjectFilter.Land, players = Player.EachOpponent
        )
        withClue("player 0's own single land is not an opponent's") {
            driver.evaluate(fewestAmongOpponents, players[0]) shouldBe 3
        }
        driver.evaluate(fewestAmongOpponents, players[2]) shouldBe 1
    }

    test("the inner amount is rebound to each measured player, off the battlefield too") {
        val (driver, players) = threePlayers()
        val handSizes = players.map { driver.state.getZone(it, Zone.HAND).size }
        val fewestCardsInHand = DynamicAmounts.leastAmongPlayers(
            DynamicAmounts.zone(Player.You, Zone.HAND).count()
        )

        driver.putCardInHand(players[1], "Mountain")
        driver.putCardInHand(players[1], "Mountain")
        driver.putCardInHand(players[2], "Mountain")

        val expected = minOf(handSizes[0], handSizes[1] + 2, handSizes[2] + 1)
        driver.evaluate(fewestCardsInHand, players[1]) shouldBe expected
    }
})
