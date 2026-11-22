package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Continuation frames for venturing into the dungeon (CR 701.49) — the two choices a venture can
 * stop for. Resumed by [com.wingedsheep.engine.handlers.continuations.DungeonContinuationResumer].
 */

/**
 * Resume a venture after [playerId] chose which dungeon to put into the command zone (CR 701.49a),
 * from [candidates] (dungeon names, aligned with the decision's options).
 */
@Serializable
@SerialName("VentureChooseDungeonContinuation")
data class VentureChooseDungeonContinuation(
    val playerId: EntityId,
    val candidates: List<String>,
    val sourceName: String
) : AnswerContinuation

/**
 * Resume a venture after [playerId] chose which arrow to follow out of their current room
 * (CR 309.5a, 701.49b). [candidates] are room indices of [dungeonName], aligned with the decision's
 * options; [dungeonEntityId] is the dungeon card the marker is on, re-checked on resume.
 */
@Serializable
@SerialName("VentureChooseRoomContinuation")
data class VentureChooseRoomContinuation(
    val playerId: EntityId,
    val dungeonEntityId: EntityId,
    val dungeonName: String,
    val candidates: List<Int>,
    val sourceName: String
) : AnswerContinuation
