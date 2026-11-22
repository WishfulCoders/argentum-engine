package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayCardDecision
import com.wingedsheep.engine.core.PlayCardResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Word of Command — "Look at target opponent's hand and choose a card from it. You control that
 * player until Word of Command finishes resolving. The player plays that card if able. While doing
 * so, the player can activate mana abilities only if they're from lands that player controls and
 * only if mana they produce is spent to activate other mana abilities of lands the player controls
 * and/or to play that card. If the chosen card is cast as a spell, you control the player while that
 * spell is resolving."
 *
 * Player 1 casts it at player 2 throughout. Player 1's Swamps pay {B}{B}.
 */
class WordOfCommandScenarioTest : ScenarioTestBase() {

    private fun TestGame.tapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    /** Resolve Word of Command and choose [cardName] from player 2's hand. */
    private fun TestGame.commandToPlay(cardName: String) {
        castSpellTargetingPlayer(1, "Word of Command", 2).error shouldBe null
        resolveStack()
        val choice = getPendingDecision()
        choice.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the caster chooses from the opponent's hand") { choice.playerId shouldBe player1Id }
        selectCards(listOf(findCardsInHand(2, cardName).first())).error shouldBe null
    }

    private fun TestGame.play(action: (EntityId) -> com.wingedsheep.engine.core.GameAction) {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<PlayCardDecision>()
        withClue("the controlled player's forced play is answered by the caster") {
            state.actorFor(player2Id) shouldBe player1Id
        }
        submitDecision(PlayCardResponse(decision.id, action(decision.cardId))).error shouldBe null
    }

    init {
        context("Word of Command") {

            test("forces a six-mana creature using all six of the opponent's lands, at instant speed on the caster's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Word of Command")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Craw Wurm")
                    .withLandsOnBattlefield(2, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.commandToPlay("Craw Wurm")
                game.play { CastSpell(game.player2Id, it) }

                withClue("Word of Command finished resolving, so control has returned") {
                    game.state.actorFor(game.player2Id) shouldBe game.player2Id
                }
                game.resolveStack()
                val wurm = game.findPermanent("Craw Wurm")!!
                game.state.getEntity(wurm)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
                game.findPermanents("Forest").all { game.tapped(it) } shouldBe true
            }

            test("non-land mana abilities may not help, so a card payable only with them is not played") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Word of Command")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.commandToPlay("Grizzly Bears")

                withClue("no play is offered, so Word of Command finishes without one") {
                    game.hasPendingDecision() shouldBe false
                }
                game.state.stack.isEmpty() shouldBe true
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.tapped(game.findPermanent("Llanowar Elves")!!) shouldBe false
                game.tapped(game.findPermanent("Forest")!!) shouldBe false
                game.state.actorFor(game.player2Id) shouldBe game.player2Id
            }

            test("a chosen land is played on its controller's turn while a land play remains") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Word of Command")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.commandToPlay("Forest")
                game.play { PlayLand(game.player2Id, it) }

                game.findPermanent("Forest") shouldBe game.findPermanents("Forest").single()
                game.isInHand(2, "Forest") shouldBe false
                game.state.actorFor(game.player2Id) shouldBe game.player2Id
            }

            test("a chosen land can't be played on another player's turn, so nothing happens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Word of Command")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.commandToPlay("Forest")

                game.hasPendingDecision() shouldBe false
                game.isInHand(2, "Forest") shouldBe true
            }

            test("the caster also controls the player while the forced spell resolves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Word of Command")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Demonic Tutor")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInLibrary(2, "Craw Wurm")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.commandToPlay("Demonic Tutor")
                game.play { CastSpell(game.player2Id, it) }
                game.state.actorFor(game.player2Id) shouldBe game.player2Id

                game.resolveStack()
                val search = game.getPendingDecision()!!
                withClue("the search belongs to player 2 but is answered by the caster") {
                    search.playerId shouldBe game.player2Id
                    game.state.actorFor(game.player2Id) shouldBe game.player1Id
                }
                game.selectCards(listOf(game.findCardsInLibrary(2, "Grizzly Bears").first())).error shouldBe null
                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.state.actorFor(game.player2Id) shouldBe game.player2Id
            }
        }
    }
}
