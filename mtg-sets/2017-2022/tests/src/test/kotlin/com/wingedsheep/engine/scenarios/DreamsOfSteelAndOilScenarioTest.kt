package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Dreams of Steel and Oil (BRO #92): target opponent reveals their hand; you choose an artifact
 * or creature card from it, then one from their graveyard; exile the chosen cards.
 */
class DreamsOfSteelAndOilScenarioTest : ScenarioTestBase() {

    private fun TestGame.names(player: Int, zone: Zone) =
        state.getZone(if (player == 1) player1Id else player2Id, zone).mapNotNull {
            state.getEntity(it)?.get<CardComponent>()?.name
        }

    private fun TestGame.idIn(zone: Zone, name: String) =
        state.getZone(player2Id, zone).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.cast() {
        val cardId = state.getHand(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Dreams of Steel and Oil"
        }
        val cast = execute(CastSpell(player1Id, cardId, listOf(ChosenTarget.Player(player2Id))))
        withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
        resolveStack()
    }

    init {
        test("exiles one artifact or creature card from hand and one from graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dreams of Steel and Oil")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInHand(2, "Hill Giant")
                .withCardInHand(2, "Ornithopter")
                .withCardInHand(2, "Swamp")
                .withCardInGraveyard(2, "Glory Seeker")
                .withCardInGraveyard(2, "Ornithopter")
                .withCardInGraveyard(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast()

            withClue("first choice is from hand") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(game.idIn(Zone.HAND, "Ornithopter")))
            withClue("nothing moves before the graveyard choice") {
                game.names(2, Zone.EXILE) shouldBe emptyList()
            }
            withClue("second choice is from graveyard") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(game.idIn(Zone.GRAVEYARD, "Glory Seeker")))
            game.resolveStack()

            game.names(2, Zone.EXILE) shouldContainExactlyInAnyOrder listOf("Ornithopter", "Glory Seeker")
            game.names(2, Zone.HAND) shouldContainExactlyInAnyOrder listOf("Hill Giant", "Swamp")
            game.names(2, Zone.GRAVEYARD) shouldContainExactlyInAnyOrder listOf("Ornithopter", "Swamp")
        }

        test("an empty hand pool still exiles a card from the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dreams of Steel and Oil")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInHand(2, "Swamp")
                .withCardInGraveyard(2, "Glory Seeker")
                .withCardInGraveyard(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast()
            if (game.hasPendingDecision()) {
                game.selectCards(listOf(game.idIn(Zone.GRAVEYARD, "Glory Seeker")))
                game.resolveStack()
            }

            game.names(2, Zone.EXILE) shouldBe listOf("Glory Seeker")
            game.names(2, Zone.HAND) shouldBe listOf("Swamp")
            game.names(2, Zone.GRAVEYARD) shouldBe listOf("Swamp")
        }
    }
}
