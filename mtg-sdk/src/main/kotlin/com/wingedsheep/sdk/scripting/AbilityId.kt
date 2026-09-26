package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Unique identifier for an ability instance.
 *
 * An id only has to tell apart the abilities one object can hold at once — its own, plus whatever
 * is granted to it — which is why keyword-synthesized abilities get fixed names (`flanking`,
 * `suspend_countdown`) shared by every card. Everything else is minted by [next] inside an
 * [AbilityIdScope], so a card's ids are a function of the card alone: `"Llanowar Elves:1"` on
 * every run, whatever order the corpus happened to load in.
 */
@JvmInline
@Serializable
value class AbilityId(val value: String) {
    companion object {
        /**
         * The next id of the card being built — see [AbilityIdScope]. Fails outside a scope: an
         * ability built with no card around it (an engine-synthesized trigger, a test fixture)
         * must name its id, since there is nothing to derive one from.
         */
        fun next(): AbilityId = AbilityIdScope.next()

        /**
         * Create a deterministic AbilityId for a Class level-up ability.
         * Uses a fixed prefix so the engine can match the ability when activated.
         */
        fun classLevelUp(targetLevel: Int): AbilityId = AbilityId("class_level_up_$targetLevel")

        /**
         * Create a deterministic AbilityId for an intrinsic mana ability granted by
         * a basic land subtype (CR 305.7). The engine synthesizes these on the fly
         * from projected basic-land subtypes so the same id resolves for any land
         * with the matching subtype, regardless of card definition.
         */
        fun intrinsicMana(colorSymbol: Char): AbilityId = AbilityId("intrinsic_mana_$colorSymbol")
    }
}

/**
 * The card an [AbilityId.next] belongs to while that card is being built.
 *
 * `card("Name") { … }` runs its block inside [within], so every ability constructed while the
 * block runs — top-level, granted, nested in an effect, synthesized by a keyword helper — is
 * numbered in construction order under the card's name. Construction order is fixed by the card's
 * source, so the ids are too; nothing process-global is read or advanced. The scope is per thread
 * and nests (a back face or a token built inside a card gets its own), and it is always restored.
 */
object AbilityIdScope {

    private class Scope(val owner: String) {
        var count = 0
        var targetSlots = 0
    }

    private val active = ThreadLocal<Scope?>()

    /** Run [block] with [owner]'s ids being minted; the previous scope, if any, is restored after. */
    fun <T> within(owner: String, block: () -> T): T {
        val outer = active.get()
        active.set(Scope(owner))
        try {
            return block()
        } finally {
            active.set(outer)
        }
    }

    internal fun next(): AbilityId {
        val scope = active.get() ?: error(
            "AbilityId.next() outside a card: build the ability inside card(...) { } " +
                "(or AbilityIdScope.within), or give it an explicit AbilityId"
        )
        scope.count += 1
        return AbilityId("${scope.owner}:${scope.count}")
    }

    /**
     * The next target-binding slot of the card being built, or null outside a card. Target ids are
     * unique per *card*, not per declaring block: a reflexive or delayed trigger's targets are
     * resolved alongside the enclosing ability's, so two blocks each minting `t0` would collide.
     */
    internal fun nextTargetSlot(): Int? = active.get()?.let { it.targetSlots++ }
}
