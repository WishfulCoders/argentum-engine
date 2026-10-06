package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

/**
 * Dance of the Dead (ICE): the reanimation Aura returns the creature card *tapped* under your
 * control with +1/+1; the creature doesn't untap during its controller's untap step, and at the
 * beginning of that player's upkeep they may pay {1}{B} to untap it.
 */
class DanceOfTheDeadScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Dance of the Dead")
        .withCardInGraveyard(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castDance() {
        val giant = state.getGraveyard(player2Id).single()
        val aura = findCardsInHand(1, "Dance of the Dead").single()
        execute(CastSpell(player1Id, aura, listOf(ChosenTarget.Card(giant, player2Id, Zone.GRAVEYARD)))).error shouldBe null
        resolveStack()
    }

    init {
        test("returns the creature card tapped under your control, enchanted, with +1/+1") {
            val game = board()
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castDance()

            val aura = game.findPermanent("Dance of the Dead")!!
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.state.getEntity(giant)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe giant
            game.state.projectedState.getPower(giant) shouldBe 4
            game.state.projectedState.getToughness(giant) shouldBe 4
        }

        test("it stays tapped through the untap step; paying {1}{B} in its controller's upkeep untaps it") {
            val game = board()
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castDance()

            // The opponent's turn: no upkeep trigger for them.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.stack.size shouldBe 0
            game.passUntilPhase(Phase.BEGINNING, Step.DRAW)

            // The controller's own turn: the untap step leaves it tapped, the upkeep trigger asks.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            if (game.state.pendingDecision != null) game.submitManaSourcesAutoPay().error shouldBe null
            game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
        }
    }
}
