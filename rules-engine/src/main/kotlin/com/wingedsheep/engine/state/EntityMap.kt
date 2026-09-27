package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId

/**
 * The persistent map behind [GameState.entities]: an immutable insertion-ordered map whose
 * single-entry updates cost O(1) amortised instead of a copy of every entity.
 *
 * [GameState.withEntity] used to be `entities + (id to container)`, which copies the whole
 * `LinkedHashMap` — a hundred-odd entries — for every tap, counter and zone move, and on an AI
 * game that copy was ~7 % of all time (mtg-draft-ai `docs/56` §3 item 6). Here a version is a
 * shared, never-mutated [base] plus a short overlay of the changes made since it: [changed]
 * replaces or removes (value `null`) keys of [base] in place, and [appended] holds keys [base]
 * does not have, in the order they were added. When the overlay reaches [MAX_OVERLAY] entries the
 * next update folds it into a fresh base.
 *
 * **It iterates exactly like the `LinkedHashMap` it replaces**, which matters because arena games
 * must replay bit-identically: [base]'s keys in [base]'s order (a replaced key keeps its place, a
 * removed one is skipped), then the added keys in the order they were added. A removed key that
 * is added again goes to the end, as `LinkedHashMap.remove` + `put` does.
 */
class EntityMap private constructor(
    private val base: Map<EntityId, ComponentContainer>,
    private val baseRemoved: Int,
    private val changedKeys: Array<EntityId?>,
    private val changedValues: Array<ComponentContainer?>,
    private val appendedKeys: Array<EntityId?>,
    private val appendedValues: Array<ComponentContainer?>,
) : AbstractMap<EntityId, ComponentContainer>() {

    /**
     * One bit per overlay key (a bit of its hash), so that a lookup of a key the overlay does not
     * hold — nearly every lookup — costs one test before going to [base].
     */
    private val overlayMask: Long = run {
        var m = 0L
        for (k in changedKeys) m = m or bit(k!!.hashCode())
        for (k in appendedKeys) m = m or bit(k!!.hashCode())
        m
    }

    // Overlays are at most MAX_OVERLAY long, so a scan beats hashing; the key's cached String hash
    // screens out almost every non-match before equals runs.
    private fun changedIndex(key: EntityId, hash: Int = key.hashCode()): Int {
        val keys = changedKeys
        for (i in keys.indices) { val k = keys[i]!!; if (k.hashCode() == hash && k == key) return i }
        return -1
    }

    private fun appendedIndex(key: EntityId, hash: Int = key.hashCode()): Int {
        val keys = appendedKeys
        for (i in keys.indices) { val k = keys[i]!!; if (k.hashCode() == hash && k == key) return i }
        return -1
    }

    override val size: Int get() = base.size - baseRemoved + appendedKeys.size

    override fun isEmpty(): Boolean = size == 0

    override fun get(key: EntityId): ComponentContainer? {
        val hash = key.hashCode()
        if (overlayMask and bit(hash) == 0L) return base[key]
        val a = appendedIndex(key, hash)
        if (a >= 0) return appendedValues[a]
        val c = changedIndex(key, hash)
        if (c >= 0) return changedValues[c]
        return base[key]
    }

    override fun containsKey(key: EntityId): Boolean = get(key) != null

    /** This map with [key] mapped to [value]; an existing key keeps its position. */
    fun put(key: EntityId, value: ComponentContainer): EntityMap {
        val a = appendedIndex(key)
        if (a >= 0) {
            return EntityMap(base, baseRemoved, changedKeys, changedValues, appendedKeys, appendedValues.copyOf().also { it[a] = value })
        }
        val c = changedIndex(key)
        if (c >= 0) {
            // Replacing a key of base: in place, whether it is live or was removed. A removed key
            // is not in base's order any more, so it is re-added at the end instead.
            if (changedValues[c] != null) {
                return EntityMap(base, baseRemoved, changedKeys, changedValues.copyOf().also { it[c] = value }, appendedKeys, appendedValues)
            }
            return grow(appendedKeys.plusElement(key), appendedValues.plusElement(value), changedKeys, changedValues, baseRemoved)
        }
        if (base.containsKey(key)) {
            return grow(appendedKeys, appendedValues, changedKeys.plusElement(key), changedValues.plusElement(value), baseRemoved)
        }
        return grow(appendedKeys.plusElement(key), appendedValues.plusElement(value), changedKeys, changedValues, baseRemoved)
    }

    /** This map without [key]. */
    fun remove(key: EntityId): EntityMap {
        val a = appendedIndex(key)
        if (a >= 0) {
            val keys = appendedKeys.filterIndexed { i, _ -> i != a }.toTypedArray()
            val values = appendedValues.filterIndexed { i, _ -> i != a }.toTypedArray()
            return EntityMap(base, baseRemoved, changedKeys, changedValues, keys, values)
        }
        val c = changedIndex(key)
        if (c >= 0) {
            if (changedValues[c] == null) return this
            return EntityMap(base, baseRemoved + 1, changedKeys, changedValues.copyOf().also { it[c] = null }, appendedKeys, appendedValues)
        }
        if (!base.containsKey(key)) return this
        return grow(appendedKeys, appendedValues, changedKeys.plusElement(key), changedValues.plusElement(null), baseRemoved + 1)
    }

    private fun grow(
        aKeys: Array<EntityId?>, aValues: Array<ComponentContainer?>,
        cKeys: Array<EntityId?>, cValues: Array<ComponentContainer?>,
        removed: Int,
    ): EntityMap {
        val next = EntityMap(base, removed, cKeys, cValues, aKeys, aValues)
        return if (aKeys.size + cKeys.size > MAX_OVERLAY) next.flattened() else next
    }

    private fun flattened(): EntityMap {
        val flat = LinkedHashMap<EntityId, ComponentContainer>(size * 2)
        for ((k, v) in entries) flat[k] = v
        return EntityMap(flat, 0, NO_KEYS, NO_VALUES, NO_KEYS, NO_VALUES)
    }

    override val entries: Set<Map.Entry<EntityId, ComponentContainer>> = object : AbstractSet<Map.Entry<EntityId, ComponentContainer>>() {
        override val size: Int get() = this@EntityMap.size
        override fun iterator(): Iterator<Map.Entry<EntityId, ComponentContainer>> = EntryIterator()
    }

    /** [base] in order with [changed] applied, then [appended]. */
    private inner class EntryIterator : Iterator<Map.Entry<EntityId, ComponentContainer>> {
        private val baseEntries = base.entries.iterator()
        private var appendedAt = 0
        private var next: Map.Entry<EntityId, ComponentContainer>? = advance()

        private fun advance(): Map.Entry<EntityId, ComponentContainer>? {
            while (baseEntries.hasNext()) {
                val entry = baseEntries.next()
                if (changedKeys.isEmpty()) return entry
                val hash = entry.key.hashCode()
                if (overlayMask and bit(hash) == 0L) return entry
                val c = changedIndex(entry.key, hash)
                if (c < 0) return entry
                val value = changedValues[c] ?: continue
                return Entry(entry.key, value)
            }
            if (appendedAt < appendedKeys.size) {
                val i = appendedAt++
                return Entry(appendedKeys[i]!!, appendedValues[i]!!)
            }
            return null
        }

        override fun hasNext(): Boolean = next != null

        override fun next(): Map.Entry<EntityId, ComponentContainer> {
            val current = next ?: throw NoSuchElementException()
            next = advance()
            return current
        }
    }

    private class Entry(override val key: EntityId, override val value: ComponentContainer) : Map.Entry<EntityId, ComponentContainer> {
        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && other.key == key && other.value == value
        override fun hashCode(): Int = key.hashCode() xor value.hashCode()
        override fun toString(): String = "$key=$value"
    }

    companion object {
        private fun bit(hash: Int): Long = 1L shl ((hash xor (hash ushr 16)) and 63)

        /** Overlay entries before an update folds them into a new base. */
        private const val MAX_OVERLAY = 8
        private val NO_KEYS = arrayOfNulls<EntityId>(0)
        private val NO_VALUES = arrayOfNulls<ComponentContainer>(0)

        /**
         * [map] as an [EntityMap]: itself when it already is one, else a copy (the caller's map
         * may be mutable, and a base must never change).
         */
        fun of(map: Map<EntityId, ComponentContainer>): EntityMap =
            map as? EntityMap ?: EntityMap(LinkedHashMap(map), 0, NO_KEYS, NO_VALUES, NO_KEYS, NO_VALUES)
    }
}
