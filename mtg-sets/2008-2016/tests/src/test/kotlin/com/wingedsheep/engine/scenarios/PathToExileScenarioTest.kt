package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Path to Exile (CON #15): "Exile target creature. Its controller may search their library for a
 * basic land card, put that card onto the battlefield tapped, then shuffle."
 *
 * The exiled creature's controller — not the caster — makes the optional search, in their own
 * library; declining fetches nothing.
 */
class PathToExileScenarioTest : ScenarioTestBase() {

    private fun TestGame.basicOnBattlefield(name: String, controller: EntityId): EntityId? =
        state.getBattlefield().firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<ControllerComponent>()?.playerId == controller
        }

    private fun TestGame.acceptAndPick(player: EntityId, landName: String) {
        hasPendingDecision() shouldBe true
        answerYesNo(true)
        resolveStack()
        if (hasPendingDecision()) {
            val land = state.getLibrary(player).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == landName
            }
            selectCards(listOf(land))
            resolveStack()
        }
    }

    init {
        context("Path to Exile") {

            test("exiles the creature; its controller fetches a tapped basic land") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Path to Exile")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Path to Exile", bears)
                game.resolveStack()
                game.acceptAndPick(game.player2Id, "Forest")

                withClue("Grizzly Bears is exiled, not destroyed") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
                val forest = game.basicOnBattlefield("Forest", game.player2Id)
                withClue("player 2 fetched a tapped Forest") {
                    (forest != null) shouldBe true
                    game.state.getEntity(forest!!)?.get<TappedComponent>() shouldBe TappedComponent
                }
            }

            test("the controller may decline the search") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Path to Exile")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Path to Exile", bears)
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.basicOnBattlefield("Forest", game.player2Id) shouldBe null
                game.librarySize(2) shouldBe 1
            }

            test("a stolen creature's controller, not its owner, gets the land from their own library") {
                val game = scenario()
                    .withPlayers("Owner", "Thief")
                    .withCardInHand(1, "Path to Exile")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) { it.with(ControllerComponent(game.player2Id)) }

                game.castSpell(1, "Path to Exile", bears)
                game.resolveStack()
                game.acceptAndPick(game.player2Id, "Swamp")

                game.isInExile(1, "Grizzly Bears") shouldBe true
                withClue("the thief (controller) fetched from their own library") {
                    (game.basicOnBattlefield("Swamp", game.player2Id) != null) shouldBe true
                    game.librarySize(1) shouldBe 1
                }
            }
        }
    }
}
