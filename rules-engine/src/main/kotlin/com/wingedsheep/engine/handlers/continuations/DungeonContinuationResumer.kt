package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.VentureChooseDungeonContinuation
import com.wingedsheep.engine.core.VentureChooseRoomContinuation
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.dungeons.Dungeons

/**
 * Resumes a venture into the dungeon (CR 701.49) once the venturing player has answered:
 *
 * - [VentureChooseDungeonContinuation] — which dungeon card to put into the command zone (CR 701.49a);
 * - [VentureChooseRoomContinuation] — which arrow to follow out of the current room (CR 309.5a).
 *
 * Either way the marker moves and [com.wingedsheep.engine.core.VenturedIntoDungeonEvent] announces
 * the room, whose ability then triggers (CR 309.4c).
 */
class DungeonContinuationResumer(
    @Suppress("unused") private val services: EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(VentureChooseDungeonContinuation::class, ::resumeChooseDungeon),
        resumer(VentureChooseRoomContinuation::class, ::resumeChooseRoom),
    )

    private fun resumeChooseDungeon(
        state: GameState,
        continuation: VentureChooseDungeonContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected a dungeon choice")
        }
        val name = continuation.candidates.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "No dungeon at option ${response.optionIndex}")
        val dungeon = Dungeons.byName(name)
            ?: return ExecutionResult.error(state, "Unknown dungeon $name")
        // The player can't have gained a dungeon while answering, but a stale answer must not put a
        // second one into the command zone (CR 309.3).
        if (DungeonService.marker(state, continuation.playerId) != null) return checkForMore(state, emptyList())
        val (entered, events) = DungeonService.enter(state, continuation.playerId, dungeon, continuation.sourceName)
        return checkForMore(entered, events)
    }

    private fun resumeChooseRoom(
        state: GameState,
        continuation: VentureChooseRoomContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected a room choice")
        }
        val room = continuation.candidates.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "No room at option ${response.optionIndex}")
        val marker = DungeonService.marker(state, continuation.playerId)
        if (marker == null || marker.dungeonEntityId != continuation.dungeonEntityId) {
            return checkForMore(state, emptyList())
        }
        val (moved, events) = DungeonService.moveTo(state, continuation.playerId, room, continuation.sourceName)
        return checkForMore(moved, events)
    }
}
