package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.components.identity.CardComponent

/**
 * A projected keyword / colour / type / subtype set that starts as a shared, read-only [base] and
 * copies it into a set of its own only when something first changes it.
 *
 * [StateProjector.project] runs on every new state and used to build four fresh `LinkedHashSet`s per
 * permanent from the card's printed characteristics, although most permanents come out of the
 * layers with those sets untouched. The printed sets are now built once per [CardComponent] (see
 * [ProjectionBase]) and shared, and a permanent pays for a copy only when an effect edits it
 * (mtg-draft-ai `docs/56` §3 item 3b).
 *
 * Behaves exactly as the `LinkedHashSet` it replaces, iteration order included: the copy is a
 * `LinkedHashSet` of [base] in [base]'s order, and every later edit is an edit of that copy.
 * Removing through an iterator (`removeAll { }`, `retainAll`) copies first and removes the element
 * just returned from the copy, while the iteration itself continues over the unchanged [base].
 */
internal class CopyOnWriteNameSet(private val base: Set<String>) : AbstractMutableSet<String>() {
    private var own: LinkedHashSet<String>? = null

    private fun owned(): LinkedHashSet<String> = own ?: LinkedHashSet(base).also { own = it }

    private val current: Set<String> get() = own ?: base

    override val size: Int get() = current.size

    override fun contains(element: String): Boolean = current.contains(element)

    override fun isEmpty(): Boolean = current.isEmpty()

    override fun add(element: String): Boolean {
        if (own == null && element in base) return false
        return owned().add(element)
    }

    override fun remove(element: String): Boolean {
        if (own == null && element !in base) return false
        return owned().remove(element)
    }

    override fun clear() {
        val o = own
        if (o != null) o.clear() else if (base.isNotEmpty()) own = LinkedHashSet()
    }

    override fun iterator(): MutableIterator<String> {
        own?.let { return it.iterator() }
        val shared = base.iterator()
        return object : MutableIterator<String> {
            private var last: String? = null
            private var canRemove = false
            override fun hasNext(): Boolean = shared.hasNext()
            override fun next(): String = shared.next().also { last = it; canRemove = true }
            override fun remove() {
                check(canRemove) { "next() has not been called, or remove() already was" }
                canRemove = false
                owned().remove(last)
            }
        }
    }
}

/**
 * A card's printed keyword/flag names, colour names, type names and subtype names, in the order
 * [StateProjector] has always inserted them — the starting sets of every projection of a
 * permanent with this [CardComponent]. Read-only; built once per component instance.
 */
internal class ProjectionBase(card: CardComponent) {
    val keywords: Set<String> = linkedSetOf<String>().apply {
        card.baseKeywords.forEach { add(it.name) }
        card.baseFlags.forEach { add(it.name) }
    }
    val colors: Set<String> = card.colors.mapTo(linkedSetOf()) { it.name }
    val types: Set<String> = linkedSetOf<String>().apply {
        card.typeLine.supertypes.forEach { add(it.name) }
        card.typeLine.cardTypes.forEach { add(it.name) }
        card.typeLine.subtypes.forEach { add(it.value) }
    }
    val subtypes: Set<String> = card.typeLine.subtypes.mapTo(linkedSetOf()) { it.value }
}
