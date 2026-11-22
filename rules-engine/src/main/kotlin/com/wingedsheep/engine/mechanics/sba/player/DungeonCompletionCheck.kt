package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.dungeons.Dungeons

/**
 * CR 704.5t (and 309.6) — if a player's venture marker is on the bottommost room of a dungeon card,
 * and that dungeon card isn't the source of a room ability that has triggered but not yet left the
 * stack, the dungeon card's owner removes it from the game. Removing it is completing it (CR 309.7).
 *
 * "Triggered but not yet left the stack" covers both a room ability on the stack and one still
 * waiting to be put there — the settle boundary performs state-based actions before it places the
 * room ability the venture just triggered, exactly as for a Saga's final chapter (CR 714.4).
 */
class DungeonCompletionCheck : StateBasedActionCheck {
    override val name = "704.5t Dungeon Completion"
    override val order = SbaOrder.DUNGEON_COMPLETION

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (playerId in state.turnOrder) {
            val marker = DungeonService.marker(newState, playerId) ?: continue
            val dungeon = Dungeons.byName(marker.dungeon) ?: continue
            if (!dungeon.isBottommost(marker.room)) continue

            val cardId = marker.dungeonEntityId
            val roomOnStack = newState.stack.any { id ->
                newState.getEntity(id)?.get<TriggeredAbilityOnStackComponent>()?.sourceId == cardId
            }
            val roomWaiting = newState.waitingTriggers.any { it.sourceId == cardId }
            if (roomOnStack || roomWaiting) continue

            val (completed, completionEvents) = DungeonService.complete(newState, playerId)
            newState = completed
            events += completionEvents
        }

        return ExecutionResult.success(newState, events)
    }
}
