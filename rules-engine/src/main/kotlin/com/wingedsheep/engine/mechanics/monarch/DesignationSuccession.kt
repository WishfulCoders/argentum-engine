package com.wingedsheep.engine.mechanics.monarch

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Who inherits a single-holder designation when its holder leaves the game — CR 725.4 (the monarch)
 * and CR 726.4 (the initiative) word it identically: the active player, or, if they are leaving too
 * or there is none, the next player in turn order.
 *
 * Read once the leaver is already marked as lost, so [GameState.activePlayers] no longer holds them.
 */
internal object DesignationSuccession {

    /** The active player if still in the game, else the first still-in player after them in turn order. */
    fun successor(state: GameState): EntityId? {
        val remaining = state.activePlayers
        if (remaining.isEmpty()) return null
        val active = state.activePlayerId ?: return remaining.first()
        if (active in remaining) return active
        val order = state.turnOrder
        val start = order.indexOf(active)
        if (start < 0) return remaining.first()
        return (1..order.size).asSequence()
            .map { order[(start + it) % order.size] }
            .firstOrNull { it in remaining }
    }
}
