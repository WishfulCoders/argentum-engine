package com.wingedsheep.sdk.tooling

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

/**
 * Transforms card JSON between verbose and compact forms.
 *
 * **Compact** (for export): Simplifies singleton polymorphic objects like
 * `{"type": "EntersBattlefield"}` to just `"EntersBattlefield"`, and compacts
 * [GameObjectFilter] objects to query strings via [FilterQueryLanguage].
 *
 * **Expand** (for loading): Reverses the compact transformation. Rather than matching on a
 * hand-maintained allowlist of key names — which silently rots the moment a new polymorphic field
 * is added, and cannot disambiguate keys that are polymorphic in one type but a plain enum in
 * another (`scope`, `counterType`) — expand walks the [CardDefinition] serial-descriptor tree in
 * lockstep with the JSON. At every position it knows the *expected type*, so it restores a bare
 * string to `{"type": "..."}` exactly when the schema says that position is polymorphic, parses a
 * filter query string exactly where a [GameObjectFilter] is expected, and leaves genuine strings,
 * enums, and contextual values (ManaCost/TypeLine) untouched. This makes compact/expand symmetric by
 * construction; a new polymorphic type needs no change here.
 *
 * This transformation is applied at the CardExporter/CardLoader boundary, keeping the core
 * kotlinx.serialization infrastructure unchanged.
 */
@OptIn(ExperimentalSerializationApi::class) // SerialDescriptor.kind/PolymorphicKind introspection
object CompactJsonTransformer {

    /**
     * Keys whose values are GameObjectFilter JSON objects that can be
     * compacted to query strings via [FilterQueryLanguage].
     */
    private val FILTER_KEYS = setOf(
        "filter", "baseFilter", "sourceFilter",
        "tokenFilter", "cardFilter", "matchFilter", "targetFilter"
    )

    private val FILTER_SERIAL_NAME = serializer<GameObjectFilter>().descriptor.serialName
    private val DYNAMIC_AMOUNT_SERIAL_NAME = serializer<DynamicAmount>().descriptor.serialName

    // =============================================================================
    // Compact
    // =============================================================================

    /**
     * Compact a JSON element by replacing singleton polymorphic objects with strings.
     *
     * Rules:
     * - `{"type": "Foo"}` (single key "type") → `"Foo"`
     * - All other elements are recursed into unchanged
     */
    fun compact(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> {
            if (isSingletonObject(element)) {
                // Compact: {"type": "X"} → "X"
                element["type"]!!
            } else {
                // Recurse into all values, compacting filters to query strings
                JsonObject(element.mapValues { (key, v) -> compactValue(key, v) })
            }
        }
        is JsonArray -> JsonArray(element.map { compact(it) })
        is JsonPrimitive -> element
    }

    private fun compactValue(key: String, value: JsonElement): JsonElement {
        // Try to compact GameObjectFilter objects to query strings
        if (key in FILTER_KEYS && value is JsonObject && FilterQueryLanguage.isGameObjectFilter(value)) {
            // First compact the inner elements (e.g., singleton predicates)
            val compacted = compact(value) as JsonObject
            val query = FilterQueryLanguage.formatFilter(compacted)
            // Only emit the query string if it can actually be parsed back. This guards against
            // formatFilter/parseFilter gaps (e.g. multi-word `name:` terms, or `name:` inside an
            // OR) where formatFilter produces a string parseFilter rejects — such filters stay in
            // the verbose object form, which the descriptor-driven expand restores losslessly,
            // rather than producing JSON that can't be re-read. (The corpus round-trip test in
            // mtg-sets is the correctness oracle that a parseable query also decodes faithfully.)
            if (query != null && parsesBack(query)) return JsonPrimitive(query)
            // If the query language can't represent it, fall through to normal compaction
            return compacted
        }
        return compact(value)
    }

    /** True iff [query] parses back without error (see [compactValue]). */
    private fun parsesBack(query: String): Boolean =
        runCatching { FilterQueryLanguage.parseFilter(query) }.isSuccess

    /**
     * Check if a JSON object is a singleton polymorphic object: exactly one key "type"
     * with a string value.
     */
    private fun isSingletonObject(obj: JsonObject): Boolean {
        return obj.size == 1
                && obj.containsKey("type")
                && obj["type"] is JsonPrimitive
                && (obj["type"] as JsonPrimitive).isString
    }

    // =============================================================================
    // Expand (schema-driven)
    // =============================================================================

    /** Expand a compacted card JSON element back to the verbose form kotlinx.serialization decodes. */
    fun expand(element: JsonElement): JsonElement =
        expand(element, serializer<CardDefinition>().descriptor)

    /**
     * Expand [element] against the [descriptor] of the type expected at this position. A null
     * descriptor means the position is unknown to the schema (e.g. an `ignoreUnknownKeys` extra
     * field); such values are decode-ignored anyway, so they are left verbatim.
     */
    private fun expand(element: JsonElement, descriptor: SerialDescriptor?): JsonElement {
        if (descriptor == null) return element
        return when (element) {
            is JsonNull -> element
            is JsonPrimitive -> expandPrimitive(element, descriptor)
            is JsonArray -> expandArray(element, descriptor)
            is JsonObject -> expandObject(element, descriptor)
        }
    }

    private fun expandPrimitive(prim: JsonPrimitive, descriptor: SerialDescriptor): JsonElement {
        // kotlinx appends "?" to a nullable descriptor's serial name; compare against the base.
        val baseName = descriptor.serialName.removeSuffix("?")
        if (prim.isString) {
            return when {
                // A bare string where a polymorphic type is expected = a compacted singleton.
                descriptor.kind is PolymorphicKind ->
                    buildJsonObject { put("type", prim.content) }
                // A query string where a GameObjectFilter is expected = a compacted filter.
                baseName == FILTER_SERIAL_NAME ->
                    expand(FilterQueryLanguage.parseFilter(prim.content), descriptor)
                // Genuine String / enum / contextual (ManaCost, TypeLine) — leave as-is.
                else -> prim
            }
        }
        // Backward compatibility: a bare integer where a DynamicAmount is expected expands to a
        // Fixed amount (pre-DynamicAmount JSON files wrote plain ints for these positions).
        if (prim.intOrNull != null && baseName == DYNAMIC_AMOUNT_SERIAL_NAME) {
            return buildJsonObject { put("type", "Fixed"); put("amount", prim.int) }
        }
        return prim
    }

    private fun expandArray(arr: JsonArray, descriptor: SerialDescriptor): JsonElement {
        val elementDescriptor = if (descriptor.kind == StructureKind.LIST) {
            descriptor.getElementDescriptor(0)
        } else {
            null
        }
        return JsonArray(arr.map { expand(it, elementDescriptor) })
    }

    private fun expandObject(obj: JsonObject, descriptor: SerialDescriptor): JsonElement = when (descriptor.kind) {
        is PolymorphicKind -> {
            // {"type": "Sub", ...fields}: resolve the concrete subtype, expand its fields against it.
            val typeName = (obj["type"] as? JsonPrimitive)?.contentOrNull
            val subDescriptor = typeName?.let { resolveSubtype(descriptor, it) }
            JsonObject(
                obj.mapValues { (key, value) ->
                    if (key == "type") value else expand(value, subDescriptor?.elementDescriptorOrNull(key))
                },
            )
        }
        StructureKind.MAP ->
            JsonObject(obj.mapValues { (_, value) -> expand(value, descriptor.getElementDescriptor(1)) })
        StructureKind.CLASS, StructureKind.OBJECT ->
            JsonObject(obj.mapValues { (key, value) -> expand(value, descriptor.elementDescriptorOrNull(key)) })
        // CONTEXTUAL or any unexpected structure: recurse without guidance so nothing is mangled.
        else -> JsonObject(obj.mapValues { (_, value) -> expand(value, null) })
    }

    private fun SerialDescriptor.elementDescriptorOrNull(name: String): SerialDescriptor? {
        val index = getElementIndex(name)
        return if (index >= 0) getElementDescriptor(index) else null
    }

    /**
     * Resolve the concrete subtype descriptor for [typeName] within a sealed/polymorphic
     * [polyDescriptor]. A sealed descriptor's second element ("value") enumerates the subtypes,
     * keyed by their serial name.
     */
    private fun resolveSubtype(polyDescriptor: SerialDescriptor, typeName: String): SerialDescriptor? {
        if (polyDescriptor.elementsCount < 2) return null
        val subtypes = polyDescriptor.getElementDescriptor(1)
        val index = subtypes.getElementIndex(typeName)
        if (index >= 0) return subtypes.getElementDescriptor(index)
        // Fallback: match by serial name (handles fully-qualified discriminators).
        for (i in 0 until subtypes.elementsCount) {
            val child = subtypes.getElementDescriptor(i)
            if (child.serialName == typeName || child.serialName.substringAfterLast('.') == typeName) {
                return child
            }
        }
        return null
    }
}
