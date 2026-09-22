package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Shared helpers for the benchmarks that drive real AI-vs-AI games —
 * [SimulationThroughputBenchmark] and the arena (`com.wingedsheep.ai.arena`).
 *
 * The unseeded deck builders in [GameBenchmark] and [AdvisorBenchmark] predate this and are left
 * alone: they measure per-game cost, where deck reproducibility buys nothing.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Seeded sealed decks — same seed, same decks, every rerun
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Every card [set] can actually open in a booster: its own definitions **plus its reprints**.
 *
 * A set's `cards` come from [com.wingedsheep.mtg.sets.discovery.CardDiscovery.findIn], which
 * returns only the [CardDefinition] values declared in that set's own package. A card the set
 * reprints — Terramorphic Expanse in SOS, Evolving Wilds and the shocklands in ECL — declares a
 * name-only [Printing] row here and keeps its definition in the package of its earliest printing,
 * so it is *not* in `cards`. Passing `set.cards` to [generateSeededSealedPool] therefore made those
 * cards impossible to open, which is not a subtle shortfall: it is 26 % of the non-basic lands in a
 * real SOS deck and 88 % of them in a real ECL deck, and it silently disarmed the `-crack` arm of
 * mtg-draft-ai `docs/46` §9.6 (300W-300L on a zero-width CI, because the card the arm re-prices was
 * never in a deck). mtg-draft-ai `docs/48` is the census.
 *
 * The game server has never had this problem — `GameBeansConfig.boosterCardPool` resolves reprints
 * the same way, and its own docstring records the same bug being fixed there for mixed sets. This
 * is the arena's copy of that resolution, kept here because `ai`'s test sources cannot see
 * `game-server`. Behaviour matches it: the canonical definition is overlaid with the reprint's
 * presentation **and rarity** (a card common here may be rare where it was first printed, and the
 * booster slots are filled by rarity), one entry per oracle name, alternate-frame and promo
 * treatments deferred to, and a printing whose canonical is implemented nowhere is skipped.
 *
 * Sorted by name, like `CardDiscovery`'s own output, so a seeded pool is reproducible.
 */
internal fun draftableCards(set: MtgSet): List<CardDefinition> {
    val ownNames = set.cards.mapTo(hashSetOf()) { it.name }
    val reprints = set.printings
        .filter { it.name !in ownNames }
        .groupBy { it.name }
        .mapNotNull { (_, treatments) ->
            val printing = treatments.firstOrNull { !it.isAlternateFrame && !it.isPromo }
                ?: treatments.first()
            canonicalByName[printing.name]?.let { canonical ->
                val skinned = canonical.withPrinting(printing)
                skinned.copy(metadata = skinned.metadata.copy(rarity = printing.rarity))
            }
        }
    return (set.cards + reprints).sortedBy { it.name }
}

/**
 * Name -> canonical definition across every implemented set, for resolving a reprint's `Printing`.
 *
 * [com.wingedsheep.mtg.sets.discovery.CardDiscovery.findSets] warns that its ordering is not
 * deterministic, so the sets are sorted before the first definition of a name wins. Without that a
 * card reprinted twice could resolve to a different set's copy between runs, and the arena's whole
 * contract is that a seed reproduces a deck.
 */
private val canonicalByName: Map<String, CardDefinition> by lazy {
    MtgSetCatalog.all
        .sortedBy { it.code }
        .flatMap { it.cards }
        .groupBy { it.name }
        .mapValues { (_, definitions) -> definitions.first() }
}

/** Opens six boosters' worth of the cards [set] can print, and autobuilds a 40-card limited deck. */
internal fun buildSeededSealedDeck(set: MtgSet, rng: Random): Deck =
    buildSeededSealedDeck(draftableCards(set), rng)

/** Opens six boosters' worth of cards and autobuilds a 40-card limited deck from them. */
internal fun buildSeededSealedDeck(allCards: List<CardDefinition>, rng: Random): Deck {
    val pool = generateSeededSealedPool(allCards, rng)
    val deckMap = buildHeuristicSealedDeck(pool)
    return Deck(deckMap.flatMap { (name, count) -> List(count) { name } })
}

/** Six boosters: 11 commons, 3 uncommons and a rare (1-in-8 mythic) each, no duplicates per pack. */
internal fun generateSeededSealedPool(allCards: List<CardDefinition>, rng: Random): List<CardDefinition> {
    val nonBasics = allCards.filter { !it.typeLine.isBasicLand }
    val commons = nonBasics.filter { it.metadata.rarity == Rarity.COMMON }
    val uncommons = nonBasics.filter { it.metadata.rarity == Rarity.UNCOMMON }
    val rares = nonBasics.filter { it.metadata.rarity == Rarity.RARE }
    val mythics = nonBasics.filter { it.metadata.rarity == Rarity.MYTHIC }

    val pool = mutableListOf<CardDefinition>()
    repeat(6) {
        val usedNames = mutableSetOf<String>()
        fun pick(from: List<CardDefinition>): CardDefinition? {
            val available = from.filter { it.name !in usedNames }
            if (available.isEmpty()) return null
            return available[rng.nextInt(available.size)].also { usedNames.add(it.name) }
        }
        repeat(11) { pick(commons)?.let { pool.add(it) } }
        repeat(3) { pick(uncommons)?.let { pool.add(it) } }
        val rare = if (mythics.isNotEmpty() && rng.nextDouble() < 0.125) pick(mythics) else null
        pool.add(rare ?: pick(rares) ?: pick(uncommons) ?: pick(commons)!!)
    }
    return pool
}

// ─────────────────────────────────────────────────────────────────────────────
// Illegal-action recovery
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mirror of `AIPlayer.playPriorityWindow`'s fallback: when the chosen action is rejected, pass —
 * except in a combat declaration step, where passing leaves mandatory attackers/blockers
 * undeclared and the game wedges.
 *
 * A benchmark that needs this has already found a bug; the point is to keep the *rest* of the run
 * going so the exception histogram sees every distinct failure, not just the first.
 */
internal fun safeFallbackAction(
    state: GameState,
    playerId: EntityId,
    enumerator: LegalActionEnumerator
): GameAction {
    val attackersDeclared = state.getEntity(playerId)?.has<AttackersDeclaredThisCombatComponent>() == true
    val blockersDeclared = state.getEntity(playerId)?.has<BlockersDeclaredThisCombatComponent>() == true
    return when {
        state.step == Step.DECLARE_ATTACKERS && state.activePlayerId == playerId && !attackersDeclared -> {
            val la = enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY)
                .find { it.actionType == "DeclareAttackers" }
            val mandatory = la?.mandatoryAttackers ?: emptyList()
            val opponentId = state.getOpponents(playerId).firstOrNull()
            DeclareAttackers(
                playerId,
                if (mandatory.isNotEmpty() && opponentId != null) mandatory.associateWith { opponentId } else emptyMap()
            )
        }

        state.step == Step.DECLARE_BLOCKERS && state.activePlayerId != playerId && !blockersDeclared -> {
            val la = enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY)
                .find { it.actionType == "DeclareBlockers" }
            val mandatory = la?.mandatoryBlockerAssignments ?: emptyMap()
            DeclareBlockers(playerId, mandatory.mapValues { (_, targets) -> targets.take(1) })
        }

        else -> PassPriority(playerId)
    }
}
