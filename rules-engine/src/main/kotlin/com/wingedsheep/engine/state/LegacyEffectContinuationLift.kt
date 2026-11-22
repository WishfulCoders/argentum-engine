package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.ProgramCursor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Reads games persisted while a composite's remaining steps were an `EffectContinuation`
 * (`remainingEffects`, `effectContext`): "run these next". On 2026-10-17 that frame became the
 * effect interpreter's [com.wingedsheep.engine.core.EffectProgram], whose cursor names the node
 * that is executing, not the next one.
 *
 * Each old frame becomes `EffectProgram(root = CompositeEffect(remainingEffects), origin = null,
 * cursor = [Start], effectContext)`, wherever it nests (on the stack, inside a suspension or
 * another frame). `Start` means nothing in the program has run, so resuming the converted frame
 * runs `remainingEffects` from the first, in order, in the frame's context — as the old frame did.
 * The root composite has `stopOnError = false`, as the old frame's runner behaved. The old frame
 * never recorded where its effects came from, so the origin is null (reserved for converted
 * frames) and positions are relative to that residual root, not to the card's effect.
 */
internal object LegacyEffectContinuationLift {
    private const val RETIRED = "com.wingedsheep.engine.core.EffectContinuation"
    private const val CURRENT = "com.wingedsheep.engine.core.EffectProgram"

    private val start: JsonElement =
        Json.encodeToJsonElement(ProgramCursor.serializer(), ProgramCursor.START)

    /** Returns [element] itself (no copy) when nothing in it needs lifting — the common case. */
    fun lift(element: JsonElement): JsonElement = when (element) {
        is JsonArray -> {
            val children = element.map(::lift)
            if (children.indices.all { children[it] === element[it] }) element else JsonArray(children)
        }
        is JsonObject -> {
            val lifted = if ((element["type"] as? JsonPrimitive)?.content == RETIRED) liftFrame(element) else element
            val children = lifted.mapValues { (_, value) -> lift(value) }
            if (lifted === element && children.all { (key, value) -> value === element[key] }) element
            else JsonObject(children)
        }
        else -> element
    }

    private fun liftFrame(old: JsonObject): JsonObject = JsonObject(
        buildMap {
            put("type", JsonPrimitive(CURRENT))
            put("root", JsonObject(buildMap {
                put("type", JsonPrimitive("Composite"))
                old["remainingEffects"]?.let { put("effects", it) }
            }))
            put("origin", JsonNull)
            put("cursor", start)
            old["effectContext"]?.let { put("effectContext", it) }
        }
    )
}
