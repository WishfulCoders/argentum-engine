package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.kld.cards.BomatCourier
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bomat Courier (KLD #199).
 *
 *   Haste
 *   Whenever this creature attacks, exile the top card of your library face down.
 *   {R}, Discard your hand, Sacrifice this creature: Put all cards exiled with this creature into
 *   their owners' hands.
 *
 * Pins the cross-turn shape: attack triggers fill a face-down linked pile, and the activated
 * ability still finds that pile after the Courier was sacrificed as part of its cost.
 */
class BomatCourierScenarioTest : ScenarioTestBase() {

    private val cashIn = BomatCourier.activatedAbilities.single()

    init {
        test("attacking exiles the top card face down; cashing in discards the hand and returns the cache") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Bomat Courier", summoningSickness = true)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val libraryBefore = game.librarySize(1)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("Haste lets the Courier attack the turn it arrives") {
                game.declareAttackers(mapOf("Bomat Courier" to 2)).error shouldBe null
            }
            game.resolveStack()

            withClue("The attack trigger exiled the top card of the library") {
                game.librarySize(1) shouldBe libraryBefore - 1
            }
            val exiled = game.state.getZone(game.player1Id, Zone.EXILE)
            withClue("Exactly one card is exiled, and it is face down") {
                exiled.size shouldBe 1
                game.state.getEntity(exiled.single())?.has<FaceDownComponent>() shouldBe true
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            val courier = game.findPermanent("Bomat Courier")!!
            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = courier, abilityId = cashIn.id)
            )
            withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("The Courier was sacrificed and the hand discarded") {
                game.isInGraveyard(1, "Bomat Courier") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            withClue("The exiled card came back to its owner's hand") {
                game.state.getZone(game.player1Id, Zone.EXILE).size shouldBe 0
                game.handSize(1) shouldBe 1
                game.isInHand(1, "Hill Giant") shouldBe true
            }
        }

        test("cashing in with an empty cache still sacrifices and discards") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Bomat Courier")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val courier = game.findPermanent("Bomat Courier")!!
            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = courier, abilityId = cashIn.id)
            )
            withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isInGraveyard(1, "Bomat Courier") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 0
        }
    }
}
