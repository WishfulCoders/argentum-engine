package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "This creature enters prepared" (CR 722.3a) holds however the creature enters, not only when it
 * is cast: reanimated from the graveyard, blinked, or created as a token copy (CR 722.3c's example
 * prepares a token copy). A creature that leaves and returns is a new object (CR 400.7) and enters
 * prepared afresh, with a live prepare-spell copy in exile.
 *
 * Uses Hallway Heckler (enters prepared; prepare spell Vicious Verse).
 */
class EntersPreparedScenarioTest : ScenarioTestBase() {

    private fun TestGame.preparedCopies(playerNumber: Int): List<EntityId> {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).filter { id ->
            state.getEntity(id)?.get<PreparedSpellCopyComponent>() != null
        }
    }

    private fun TestGame.assertPrepared(permanent: EntityId, expectedCopies: Int = 1) {
        val prepared = state.getEntity(permanent)?.get<PreparedComponent>()
        withClue("Hallway Heckler enters prepared") { prepared shouldNotBe null }
        val copies = preparedCopies(1)
        withClue("One Vicious Verse copy per prepared Heckler sits in exile") { copies shouldHaveSize expectedCopies }
        withClue("The prepared permanent links to a copy that exists in exile") {
            (prepared!!.exileCopyId in copies) shouldBe true
        }
        withClue("The exile copy is the prepare spell") {
            state.getEntity(prepared!!.exileCopyId)?.get<CardComponent>()?.typeLine?.toString()
                ?.contains("Sorcery") shouldBe true
        }
    }

    init {
        context("enters prepared without being cast") {

            test("reanimated from the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Hallway Heckler")
                    .withCardInHand(1, "Reanimate")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heckler = game.findCardsInGraveyard(1, "Hallway Heckler").single()
                val reanimate = game.state.getHand(game.player1Id).single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Reanimate"
                }
                val result = game.execute(
                    CastSpell(game.player1Id, reanimate, listOf(ChosenTarget.Card(heckler, game.player1Id, Zone.GRAVEYARD)))
                )
                withClue("Casting Reanimate should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val onBattlefield = game.findPermanent("Hallway Heckler")
                withClue("Hallway Heckler returned to the battlefield") { onBattlefield shouldNotBe null }
                game.assertPrepared(onBattlefield!!)
            }

            test("blinked: the returning object is prepared again with a fresh copy") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hallway Heckler")
                    .withCardInHand(1, "Cloudshift")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Casting Hallway Heckler") { game.castSpell(1, "Hallway Heckler").error shouldBe null }
                game.resolveStack()
                val heckler = game.findPermanent("Hallway Heckler")!!
                game.assertPrepared(heckler)
                val firstCopy = game.state.getEntity(heckler)!!.get<PreparedComponent>()!!.exileCopyId

                withClue("Casting Cloudshift") { game.castSpell(1, "Cloudshift", heckler).error shouldBe null }
                game.resolveStack()

                val returned = game.findPermanent("Hallway Heckler")!!
                game.assertPrepared(returned)
                withClue("The old copy left with the old object; the new one is a fresh copy") {
                    (game.state.getEntity(returned)!!.get<PreparedComponent>()!!.exileCopyId != firstCopy) shouldBe true
                }
            }

            test("a token copy enters prepared") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hallway Heckler")
                    .withCardInHand(1, "Cackling Counterpart")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val original = game.findPermanent("Hallway Heckler")!!
                withClue("Casting Cackling Counterpart") {
                    game.castSpell(1, "Cackling Counterpart", original).error shouldBe null
                }
                game.resolveStack()

                val token = game.findPermanents("Hallway Heckler").single { it != original }
                withClue("The token copy is prepared") {
                    game.state.getEntity(token)?.get<PreparedComponent>() shouldNotBe null
                }
                withClue("The original was placed directly, so only the token has a copy in exile") {
                    game.preparedCopies(1) shouldHaveSize 1
                }
            }
        }
    }
}
