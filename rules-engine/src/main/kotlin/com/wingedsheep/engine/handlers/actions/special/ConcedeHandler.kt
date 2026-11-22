package com.wingedsheep.engine.handlers.actions.special

import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import kotlin.reflect.KClass
import com.wingedsheep.engine.core.Outcome

/**
 * Handler for the Concede action.
 *
 * Conceding is always legal (CR 104.3a — a player can concede at any time). It marks the
 * player as having lost; the state-based action loop then does the rest:
 * [com.wingedsheep.engine.mechanics.sba.player.PlayerLeavesGameCheck] removes the
 * conceding player's objects (CR 800.4a) and [com.wingedsheep.engine.mechanics.sba.game.GameEndCheck]
 * ends the game only when one player remains. So in a two-player game conceding ends the
 * game immediately, while in a multiplayer pod it continues for the others.
 */
class ConcedeHandler(
    private val sbaChecker: StateBasedActionChecker
) : ActionHandler<Concede> {
    override val actionType: KClass<Concede> = Concede::class

    override fun validate(state: GameState, action: Concede): String? = null

    override fun execute(state: GameState, action: Concede): ExecutionResult {
        // Idempotent: a player already out of the game can't concede again — though one out of
        // a subgame can still concede the main game waiting on it.
        val lostHere = state.getEntity(action.playerId)?.has<PlayerLostComponent>() == true
        if (lostHere && (!action.includingMainGames || state.mainGame == null)) {
            return ExecutionResult.success(state)
        }

        val marked = concede(state, action.playerId, action.includingMainGames)
        if (lostHere) return ExecutionResult.success(marked)
        val lostEvent = PlayerLostEvent(action.playerId, GameEndReason.CONCESSION)

        val sbaResult = sbaChecker.checkAndApply(marked)
        if (sbaResult.outcome !is Outcome.Done) {
            // Paused on an SBA's question (or rejected): pass it on.
            return sbaResult.copy(events = listOf(lostEvent) + sbaResult.events)
        }
        return ExecutionResult.success(
            sbaResult.newState,
            listOf(lostEvent) + sbaResult.events
        )
    }

    /**
     * Mark [playerId] as having conceded [state]'s game and, with [includingMainGames], each main
     * game it is a subgame of (CR 729). A discontinued main game does nothing until it resumes, so
     * its state-based actions remove the player then.
     */
    private fun concede(state: GameState, playerId: EntityId, includingMainGames: Boolean): GameState {
        val marked = if (state.getEntity(playerId)?.has<PlayerLostComponent>() == true) state
            else state.updateEntity(playerId) { it.with(PlayerLostComponent(LossReason.CONCESSION)) }
        val main = state.mainGame?.takeIf { includingMainGames } ?: return marked
        return marked.copy(mainGame = concede(main, playerId, includingMainGames = true))
    }
}
