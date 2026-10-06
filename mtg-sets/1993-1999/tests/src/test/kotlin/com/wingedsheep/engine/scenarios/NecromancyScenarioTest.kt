package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Necromancy (VIS): cast as an enchantment, its enters trigger turns it into an Aura that returns a
 * creature card from any graveyard and enchants it. Cast at sorcery speed it stays; cast at instant
 * speed (here in the opponent's end step) it is sacrificed at the beginning of the next cleanup
 * step — and the creature with it.
 */
class NecromancyScenarioTest : ScenarioTestBase() {

    private fun board(activePlayer: Int, phase: Phase, step: Step) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Necromancy")
        .withCardInGraveyard(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(activePlayer)
        .inPhase(phase, step)
        .withPriorityPlayer(1)
        .build()

    /** Cast Necromancy and resolve it and its enters trigger, aimed at [creature]. */
    private fun TestGame.castNecromancyOn(creature: EntityId) {
        val necromancy = findCardsInHand(1, "Necromancy").single()
        getLegalActions(1).any { (it.action as? CastSpell)?.cardId == necromancy } shouldBe true
        castSpell(1, "Necromancy").error shouldBe null
        passPriority()
        passPriority()
        if (state.pendingDecision != null) selectTargets(listOf(creature)).error shouldBe null
        resolveStack()
    }

    init {
        test("at sorcery speed: becomes an Aura on the returned creature, and stays") {
            val game = board(1, Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castNecromancyOn(giant)

            val necromancy = game.findPermanent("Necromancy")!!
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.state.getEntity(giant)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            game.state.projectedState.hasSubtype(necromancy, "Aura") shouldBe true
            game.state.getEntity(necromancy)?.get<AttachedToComponent>()?.targetId shouldBe giant

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.isOnBattlefield("Necromancy") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("at instant speed in the opponent's end step: sacrificed at the cleanup step, taking the creature with it") {
            val game = board(2, Phase.ENDING, Step.END)
            val giant = game.state.getGraveyard(game.player2Id).single()
            game.castNecromancyOn(giant)
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.state.getEntity(giant)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.state.stack.size shouldBe 1
            game.resolveStack()

            game.isInGraveyard(1, "Necromancy") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }
    }
}
