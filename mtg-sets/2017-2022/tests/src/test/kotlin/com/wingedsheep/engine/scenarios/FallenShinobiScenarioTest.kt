package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fallen Shinobi — "Whenever this creature deals combat damage to a player, that player exiles the
 * top two cards of their library. Until end of turn, you may play those cards without paying their
 * mana costs."
 *
 * The cards land in the *damaged player's* exile, but the Shinobi's controller is the one who may
 * play them — a spell for free, and a land with their own land drop.
 */
class FallenShinobiScenarioTest : ScenarioTestBase() {

    private fun connect(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Fallen Shinobi", summoningSickness = false)
            .withCardInLibrary(2, "Grizzly Bears")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Fallen Shinobi" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers().error shouldBe null
        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        return game
    }

    private fun exiledCard(game: TestGame, name: String): EntityId =
        game.state.getExile(game.player2Id).first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        context("Fallen Shinobi") {
            test("the damaged player exiles their top two cards") {
                val game = connect()
                game.getLifeTotal(2) shouldBe 15
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Forest") shouldBe true
                game.librarySize(2) shouldBe 0
            }

            test("you may cast an exiled spell without paying its mana cost") {
                val game = connect()
                val bears = exiledCard(game, "Grizzly Bears")
                withClue("no mana is available, so only a free cast can succeed") {
                    game.execute(CastSpell(game.player1Id, bears)).error shouldBe null
                }
                game.resolveStack()
                val onBattlefield = game.findPermanent("Grizzly Bears")
                (onBattlefield != null) shouldBe true
                game.state.getBattlefield(game.player1Id).contains(onBattlefield) shouldBe true
            }

            test("you may play an exiled land with your land drop") {
                val game = connect()
                val forest = exiledCard(game, "Forest")
                game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
                game.isOnBattlefield("Forest") shouldBe true
            }
        }
    }
}
