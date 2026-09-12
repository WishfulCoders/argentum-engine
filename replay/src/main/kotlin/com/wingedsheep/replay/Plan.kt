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
    /** Activated abilities still to take, by side, as recorded ([matchesActivation]). */
    val activations: Map<String, List<String>> = emptyMap(),
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

    /** The index of [side]'s first recorded activation that the engine's [description] is, or -1. */
    fun activation(side: String, description: String): Int =
        activations[side].orEmpty().indexOfFirst { matchesActivation(description, it) }

    fun activate(side: String, index: Int): Plan =
        copy(activations = activations + (side to activations[side].orEmpty().filterIndexed { i, _ -> i != index }))

    /** True when [side] still has a land or spell to play this half-turn. */
    fun hasWork(side: String, active: Boolean): Boolean =
        spells[side].orEmpty().isNotEmpty() || (active && lands.isNotEmpty())

    val done: Boolean
        get() = lands.isEmpty() && spells.values.all { it.isEmpty() } && activations.values.all { it.none(::required) } &&
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
                activations = ht.activated.filterValues { it.isNotEmpty() },
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

/**
 * A recorded activation the half-turn must use. One logged without a cost may be a loyalty ability
 * but also a saga chapter or a spell's mode, which the search does not activate, so it is allowed
 * rather than required. Activated keywords (Equip {1}, Crew 2, Station, cycling) are required.
 */
private fun required(recorded: String): Boolean =
    ": " in recorded || "cycling" in recorded.lowercase() || KEYWORD.containsMatchIn(recorded)

/** Activated abilities 17Lands logs by keyword alone; the export keeps them ("Crew 2", "Station"). */
private val KEYWORD = Regex("""^(?:[^—]+ — )?(?:Equip|Crew|Saddle|Station)\b""")

/**
 * Whether the engine's description of an activated ability, [engine] ("{1}{W}, Blight 1, Sacrifice
 * this Aura: Exile enchanted creature."), is the ability 17Lands logged as [recorded] ("{1}{W}, Blight
 * 1, Sacrifice CARDNAME: Exile enchanted creature."). The two word their costs differently ("this
 * Aura", CARDNAME), so costs are compared part by part on their first word; a recorded text with no
 * cost (a loyalty ability, logged without its "+1") is compared on its first words, and cycling on
 * the keyword alone.
 */
fun matchesActivation(engine: String, recorded: String): Boolean {
    fun norm(t: String) = t.substringAfter(" — ").lowercase().replace(Regex("\\s+"), " ").trim()
    val e = norm(engine)
    val r = norm(recorded)
    fun words(t: String, n: Int) = t.split(' ').take(n)
    // the engine spells Station out: "Tap another untapped creature you control: Put charge counters ..."
    if (r == "station") return "charge counters" in e
    // cycling on the keyword ("basic landcycling"): the engine follows it with the card's name
    // ("Islandcycling Giant Koi"), 17Lands with the cost
    fun keyword(t: String) = t.split(' ').let { w -> w.take(w.indexOfFirst { "cycling" in it } + 1) }
    if ("cycling" in words(e, 2).joinToString(" ") || "cycling" in words(r, 2).joinToString(" ")) {
        return keyword(e) == keyword(r)
    }
    fun cost(t: String) = if (": " in t) t.substringBefore(": ").split(", ").map { it.substringBefore(' ') } else null
    val ec = cost(e)
    val rc = cost(r)
    if (ec != null && rc != null) return ec == rc
    return words(e.substringAfter(": "), 3) == words(r.substringAfter(": "), 3)
}
