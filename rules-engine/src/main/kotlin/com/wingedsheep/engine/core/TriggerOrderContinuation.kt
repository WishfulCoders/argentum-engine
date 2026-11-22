package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * [playerId] is ordering their triggered abilities in [com.wingedsheep.engine.state.GameState.triggersBeingPlaced]
 * (CR 603.3b). The wave itself waits in the state, so the answer only rearranges that player's part
 * of it; players after them in APNAP order are asked next.
 */
@Serializable
data class TriggerOrderContinuation(
    val playerId: EntityId,
) : AnswerContinuation
