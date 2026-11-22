package com.wingedsheep.engine.mechanics.initiative

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.InitiativeTakenEvent
import com.wingedsheep.engine.mechanics.monarch.DesignationSuccession
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * The single writer of [GameState.initiative] (CR 726, "The Initiative").
 *
 * Mirrors [com.wingedsheep.engine.mechanics.monarch.MonarchService] with one difference that is the
 * whole reason it isn't the same function: taking the initiative while already having it is still
 * taking it (CR 726.5), so [take] always emits [InitiativeTakenEvent] and the inherent
 * "whenever a player takes the initiative, that player ventures into Undercity" triggers again —
 * while becoming the monarch while already the monarch changes nothing.
 *
 * Callers: [com.wingedsheep.engine.handlers.effects.player.TakeDesignationExecutor] (for "you take
 * the initiative" and the inherent combat-damage steal, which is that same effect), and [onPlayerLeft]
 * for the CR 726.4 hand-off. The three inherent triggered abilities live in
 * [com.wingedsheep.engine.event.InitiativeAbilities].
 */
object InitiativeService {

    /** Log/stack label for the sourceless initiative abilities and the leave-the-game hand-off. */
    const val SOURCE_NAME: String = "The Initiative"

    /** [playerId] takes the initiative (CR 726.3); the previous holder, if any, ceases to have it. */
    fun take(state: GameState, playerId: EntityId, sourceName: String): Pair<GameState, List<GameEvent>> {
        val previous = state.initiative
        return state.copy(initiative = playerId) to
            listOf(InitiativeTakenEvent(previousHolderId = previous, newHolderId = playerId, sourceName = sourceName))
    }

    /**
     * CR 726.4 — if the player who has the initiative leaves the game, the active player takes it at
     * the same time; if the active player is leaving too (or there is none), the next player in turn
     * order does. That is *taking* the initiative, so the successor ventures into Undercity.
     *
     * Called by the leave-the-game processor once [leaver] is already marked as lost, so
     * [GameState.activePlayers] no longer contains them.
     */
    fun onPlayerLeft(state: GameState, leaver: EntityId): Pair<GameState, List<GameEvent>> {
        if (state.initiative != leaver) return state to emptyList()
        val successor = DesignationSuccession.successor(state)
            ?: return state.copy(initiative = null) to emptyList()
        return take(state, successor, SOURCE_NAME)
    }
}
