package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Stolen Strategy (BBD #64) — "At the beginning of your upkeep, exile the top card of each
 * opponent's library. Until end of turn, you may cast spells from among those exiled cards, and you
 * may spend mana as though it were mana of any color to cast those spells."
 *
 * "Any color" is narrower than "any type" (CR 609.4b, 118.14): colorless is a type of mana but not
 * a color, so a `{C}` pip still needs colorless mana while every colored pip takes anything.
 */
class StolenStrategyScenarioTest : ScenarioTestBase() {

    private fun stolenFrom(topCard: String, vararg extraLands: String): TestGame {
        val game = extraLands.fold(scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Stolen Strategy")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, topCard)
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(2)
            .inPhase(Phase.ENDING, Step.END)) { builder, land -> builder.withCardOnBattlefield(1, land) }
            .build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        withClue("the opponent's top card is exiled: ${game.state.getExile(game.player2Id)}") {
            game.state.getExile(game.player2Id).map {
                game.state.getEntity(it)?.get<CardComponent>()?.name
            } shouldBe listOf(topCard)
        }
        return game
    }

    private fun castAction(game: TestGame, name: String) =
        game.getLegalActions(1).single { it.description.contains(name) }

    init {
        context("Stolen Strategy") {

            test("colored pips can be paid with mana of any color") {
                val game = stolenFrom("Grizzly Bears")
                withClue("Mountains pay {1}{G}") { castAction(game, "Grizzly Bears").isAffordable shouldBe true }
                game.execute(castAction(game, "Grizzly Bears").action).error shouldBe null
                game.resolveStack()
                withClue("the stolen Bears resolve under Player1's control") {
                    game.findPermanent("Grizzly Bears")?.let { game.state.projectedState.getController(it) } shouldBe game.player1Id
                }
            }

            test("a {C} pip still needs colorless mana — it is any color, not any type") {
                val game = stolenFrom("Thought-Knot Seer")
                val seer = castAction(game, "Thought-Knot Seer")
                withClue("four Mountains can't pay {3}{C}") { seer.isAffordable shouldBe false }
                withClue("the cast handler refuses it too") { (game.execute(seer.action).error != null) shouldBe true }
            }

            test("with a colorless source the {C} pip is paid and the rest by colored mana") {
                val game = stolenFrom("Thought-Knot Seer", "Wastes")
                withClue("Wastes pays {C}, Mountains pay {3}") { castAction(game, "Thought-Knot Seer").isAffordable shouldBe true }
            }
        }
    }
}
