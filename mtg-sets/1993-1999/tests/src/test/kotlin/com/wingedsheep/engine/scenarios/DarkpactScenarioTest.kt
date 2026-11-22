package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.ante.AnteSettlement
import com.wingedsheep.engine.mechanics.ante.AnteTransferCause
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Darkpact {B}{B}{B} — Sorcery:
 * "You own target card in the ante. Exchange that card with the top card of your library."
 *
 * Pins: the target becomes yours (owner changes, and the change is settled for keeps), it goes to
 * the top of *your* library, and your old top card takes its place in the ante (CR 701.12d).
 */
class DarkpactScenarioTest : ScenarioTestBase() {

    init {
        test("steals an opponent's anted card onto your library and antes your top card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .playingForAnte()
                .withCardInHand(1, "Darkpact")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInAnte(2, "Shivan Dragon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val darkpact = game.findCardsInHand(1, "Darkpact").single()
            val dragon = game.state.getZone(game.player2Id, Zone.ANTE).single()
            game.execute(
                CastSpell(game.player1Id, darkpact, listOf(ChosenTarget.Card(dragon, game.player2Id, Zone.ANTE)))
            ).error shouldBe null
            game.resolveStack()

            withClue("Player 1 now owns the Shivan Dragon, on top of their library") {
                game.state.getEntity(dragon)?.get<OwnerComponent>()?.playerId shouldBe game.player1Id
                game.state.getEntity(dragon)?.get<CardComponent>()?.ownerId shouldBe game.player1Id
                game.state.getZone(game.player1Id, Zone.LIBRARY).first() shouldBe dragon
            }
            withClue("their old top card, the Forest, is in the ante in the Dragon's place") {
                game.isInAnte(1, "Forest") shouldBe true
                game.anteSize(2) shouldBe 0
            }
            withClue("the steal is for keeps: settled even before anyone wins") {
                val settled = AnteSettlement.settle(game.state)
                settled.single().cardName shouldBe "Shivan Dragon"
                settled.single().fromPlayerId shouldBe game.player2Id
                settled.single().toPlayerId shouldBe game.player1Id
                settled.single().cause shouldBe AnteTransferCause.OWNERSHIP
            }
        }

        test("with an empty library the card still becomes yours and goes to your library") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .playingForAnte()
                .withCardInHand(1, "Darkpact")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInAnte(2, "Shivan Dragon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val darkpact = game.findCardsInHand(1, "Darkpact").single()
            val dragon = game.state.getZone(game.player2Id, Zone.ANTE).single()
            game.execute(
                CastSpell(game.player1Id, darkpact, listOf(ChosenTarget.Card(dragon, game.player2Id, Zone.ANTE)))
            ).error shouldBe null
            game.resolveStack()

            game.state.getZone(game.player1Id, Zone.LIBRARY) shouldBe listOf(dragon)
            game.anteSize(1) shouldBe 0
        }
    }
}
