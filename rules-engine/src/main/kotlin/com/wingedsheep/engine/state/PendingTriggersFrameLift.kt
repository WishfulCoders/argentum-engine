package com.wingedsheep.engine.state

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Reads games persisted while the rest of a trigger wave waited on the continuation stack. A wave
 * whose trigger stopped for a choice used to park its remaining triggers in a
 * `PendingTriggersContinuation` frame beneath the question; they now live in
 * [GameState.triggersBeingPlaced]. This moves each such frame's triggers into that field, in the
 * order the frames would have run (top of the stack first), and drops the frames.
 */
internal object PendingTriggersFrameLift {
    private const val FRAME = "com.wingedsheep.engine.core.PendingTriggersContinuation"

    fun lift(element: JsonElement): JsonElement {
        val state = element as? JsonObject ?: return element
        val stack = state["continuationStack"] as? JsonArray ?: return state
        val (frames, kept) = stack.partition { (it as? JsonObject)?.get("type") == JsonPrimitive(FRAME) }
        if (frames.isEmpty()) return state
        val waiting = frames.asReversed().flatMap { (it as JsonObject)["remainingTriggers"] as? JsonArray ?: JsonArray(emptyList()) }
        val alreadyPlacing = state["triggersBeingPlaced"] as? JsonArray ?: JsonArray(emptyList())
        return JsonObject(state + mapOf(
            "continuationStack" to JsonArray(kept),
            "triggersBeingPlaced" to JsonArray(alreadyPlacing + waiting),
        ))
    }
}
