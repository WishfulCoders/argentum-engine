package com.wingedsheep.replay

/**
 * What the record says each side still has to do in the current half-turn, consumed as the
 * search acts. The search only ever takes an action the plan allows (or passes priority), and a
 * half-turn only ends successfully once the plan is [done].
 */
data class Plan(
    /** The active player's land drops, by name. */
    val lands: Map<String, Int>,
    /** Spells still to cast, by side and name: the active player's casts, and the non-active
     *  player's instants plus the permanents they added (flash). */
    val spells: Map<String, Map<String, Int>>,
    val attacked: List<String>,
    val blocking: List<String>,
    val blocked: List<String>,
    val attacksDone: Boolean = false,
    val blocksDone: Boolean = false,
) {
    fun canPlayLand(name: String): Boolean = (lands[name] ?: 0) > 0
    fun playLand(name: String): Plan = copy(lands = lands.dec(name))

    fun canCast(side: String, name: String): Boolean = (spells[side]?.get(name) ?: 0) > 0
    fun cast(side: String, name: String): Plan =
        copy(spells = spells + (side to spells[side].orEmpty().dec(name)))

    /** True when [side] still has a land or spell to play this half-turn. */
    fun hasWork(side: String, active: Boolean): Boolean =
        spells[side].orEmpty().isNotEmpty() || (active && lands.isNotEmpty())

    val done: Boolean
        get() = lands.isEmpty() && spells.values.all { it.isEmpty() } &&
            (attacked.isEmpty() || attacksDone) && (blocking.isEmpty() || blocksDone)

    companion object {
        fun of(ht: HalfTurnSpec): Plan {
            val a = ht.active
            val n = if (a == "user") "oppo" else "user"
            return Plan(
                lands = Snapshotter.counts(ht.lands),
                spells = mapOf(
                    a to Snapshotter.counts(ht.creatures + ht.noncreatures + ht.instants[a].orEmpty()),
                    n to Snapshotter.counts(ht.instants[n].orEmpty() + ht.flash[n].orEmpty()),
                ),
                attacked = ht.attacked,
                blocking = ht.blocking,
                blocked = ht.blocked,
            )
        }
    }
}

private fun Map<String, Int>.dec(name: String): Map<String, Int> {
    val left = (this[name] ?: 0) - 1
    return if (left <= 0) this - name else this + (name to left)
}
