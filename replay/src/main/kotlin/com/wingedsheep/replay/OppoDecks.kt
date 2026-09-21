package com.wingedsheep.replay

import com.wingedsheep.engine.hidden.HiddenWorldMaterializationRequest
import com.wingedsheep.engine.hidden.HiddenWorldMaterializationResult
import com.wingedsheep.engine.hidden.HiddenWorldMaterializer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.model.EntityId
import java.io.File
import kotlin.random.Random

/**
 * Where a rollout gets the opponent's unseen cards (mtg-draft-ai `docs/36` §4.4).
 *
 * - [STUB]: whatever the rebuilt state holds — [Reconstructor]'s known cards padded with basic lands.
 * - [DONOR]: the known cards, and the rest from a real 17Lands deck of the opponent's colours out of another
 *   draft, sampled per rollout.
 * - [MIRROR]: the same, with the user's own deck as the donor.
 */
enum class OppoDeckMode { STUB, DONOR, MIRROR }

/**
 * Resamples the opponent's hidden cards — hand and library — before a rollout, so the continuation is played
 * against a plausible Limited deck rather than [Reconstructor]'s pile of basics. The rebuilt state is not
 * touched: rebuild fidelity is checked against snapshots and must not move, so this lives on the rollout side.
 *
 * A deck is built per rollout: every card the opponent was seen with, plus the donor's cards to a full deck,
 * keeping the donor's land count (the known lands count towards it). The cards the opponent has visibly used
 * (battlefield, graveyard, exile, stack, and any hidden card already revealed) are taken out, the known cards
 * still unseen go into the hidden slots first, and the rest of the slots are filled from the donor's cards at
 * random. Hand and library are resampled together, so the opponent's hand is a draw from the same belief,
 * which is also the no-lookahead discipline: the rebuilt hand holds cards chosen because they were cast later.
 */
class OppoDeckSampler(
    private val registry: CardRegistry,
    private val snapshotter: Snapshotter,
    val mode: OppoDeckMode,
    donors: List<GameSpec> = emptyList(),
) {
    private val materializer = HiddenWorldMaterializer(registry)
    /** Donor decks the engine can build: one card it lacks would fail every rollout that drew that deck. */
    private val decks: List<Donor> = donors
        .filter { d -> d.userDeck.all { registry.getCard(snapshotter.engineName(it)) != null } }
        .map { Donor(draftOf(it.gameId), colours(it.mainColors), it.userDeck) }
    val donorDecks: Int get() = decks.size
    private val matches = HashMap<Pair<String, String>, List<Donor>>()

    init {
        require(mode != OppoDeckMode.DONOR || decks.isNotEmpty()) { "the donor mode needs donor decks" }
    }

    private class Donor(val draft: String, val colours: String, val deck: List<String>)

    /**
     * [state] with the opponent's hidden cards resampled for rollout [rng], or null when the materializer
     * refused every attempt (the caller keeps the stub then, and counts it).
     */
    fun resample(state: GameState, spec: GameSpec, user: EntityId, rng: Random): GameState? {
        if (mode == OppoDeckMode.STUB) return state
        val oppo = state.turnOrder.first { it != user }
        val donor = when (mode) {
            OppoDeckMode.MIRROR -> spec.userDeck
            else -> donorsFor(spec).let { it[rng.nextInt(it.size)].deck }
        }
        val deck = build(spec.oppoKnown, donor, rng)
        fun revealed(id: EntityId) = state.getEntity(id)?.has<RevealedToComponent>() == true
        val hidden = (state.getHand(oppo) + state.getLibrary(oppo)).toMutableList()
        val fixed = hidden.filter(::revealed).toMutableList()
        hidden.removeAll(fixed.toSet())
        val shown = (state.getBattlefield().filter { owner(state, it) == oppo } + state.getGraveyard(oppo) +
            state.getExile(oppo) + state.stack.filter { owner(state, it) == oppo })
        repeat(MAX_RETRIES) {
            val names = assign(deck, spec.oppoKnown, (shown + fixed).mapNotNull { snapshotter.name(state, it) },
                hidden.size, donor, rng)
            val slots = hidden.shuffled(rng).zip(names).associate { (id, name) ->
                id to registry.requireCard(snapshotter.engineName(name))
            }
            when (val r = materializer.materialize(state, HiddenWorldMaterializationRequest(slots, state.rng))) {
                is HiddenWorldMaterializationResult.Materialized -> return r.state
                is HiddenWorldMaterializationResult.Unsupported -> {
                    // a pinned slot keeps its card and counts as seen; anything else cannot be retried
                    val id = r.reason.entityId?.takeIf { it in hidden } ?: return null
                    hidden.remove(id)
                    fixed += id
                }
            }
        }
        return null
    }

    private fun owner(state: GameState, id: EntityId): EntityId? =
        state.getEntity(id)?.get<CardComponent>()?.ownerId

    /** The known cards plus the donor's, to a deck of [Reconstructor.DECK_SIZE] with the donor's land count. */
    private fun build(known: List<String>, donor: List<String>, rng: Random): List<String> {
        val (donorLands, donorSpells) = donor.partition(::isLand)
        val (knownLands, knownSpells) = known.partition(::isLand)
        val lands = donorLands.size.coerceAtMost(Reconstructor.DECK_SIZE)
        return known +
            fill(minus(donorLands, knownLands), lands - knownLands.size, donorLands, rng) +
            fill(minus(donorSpells, knownSpells), Reconstructor.DECK_SIZE - lands - knownSpells.size, donorSpells, rng)
    }

    /**
     * [k] card names for the hidden slots: the known cards not yet seen first, then the rest of [deck] at random,
     * then — if the deck has run out, which a long game with many visible cards can do — the donor's cards again.
     */
    private fun assign(deck: List<String>, known: List<String>, seen: List<String>, k: Int, donor: List<String>, rng: Random): List<String> {
        val unseenKnown = minus(known, seen)
        val rest = minus(minus(deck, seen), unseenKnown).shuffled(rng)
        val names = (unseenKnown.shuffled(rng) + rest).take(k)
        return names + fill(emptyList(), k - names.size, donor.ifEmpty { deck }, rng)
    }

    /** [k] names from [pool] without replacement, topped up from [backup] with replacement. */
    private fun fill(pool: List<String>, k: Int, backup: List<String>, rng: Random): List<String> {
        if (k <= 0) return emptyList()
        val taken = pool.shuffled(rng).take(k)
        return taken + List(k - taken.size) { backup[rng.nextInt(backup.size)] }
    }

    private fun isLand(name: String) = registry.getCard(snapshotter.engineName(name))?.typeLine?.isLand == true

    /**
     * Donors of the opponent's colours, from other drafts: the opponent's recorded colours plus those of any basic
     * they were seen with. Exact colour match first, else the smallest decks containing those colours, else the
     * decks sharing the most of them.
     */
    private fun donorsFor(spec: GameSpec): List<Donor> {
        val want = colours(spec.oppColors + spec.oppoKnown.mapNotNull { n ->
            Reconstructor.BASICS.entries.firstOrNull { it.value == n }?.key
        }.joinToString(""))
        return matches.getOrPut(draftOf(spec.gameId) to want) {
            val pool = decks.filter { it.draft != draftOf(spec.gameId) }.ifEmpty { decks }
            pool.filter { it.colours == want }.ifEmpty {
                val supersets = pool.filter { d -> want.all { it in d.colours } }
                supersets.filter { it.colours.length == supersets.minOf { s -> s.colours.length } }
            }.ifEmpty {
                val overlap = pool.maxOf { d -> want.count { it in d.colours } }
                pool.filter { d -> want.count { it in d.colours } == overlap }
            }
        }
    }

    companion object {
        private const val MAX_RETRIES = 8

        fun draftOf(gameId: String) = gameId.substringBefore(':')

        fun colours(s: String) = "WUBRG".filter { it in s }

        /** Every spec in [paths] (JSONL, one [GameSpec] a line) whose deck is a whole one. */
        fun load(paths: List<File>): List<GameSpec> = paths.flatMap { f ->
            f.readLines().filter { it.isNotBlank() }.map { specJson.decodeFromString(GameSpec.serializer(), it) }
        }.filter { it.userDeck.size >= Reconstructor.DECK_SIZE }

        /** Multiset difference: [a] with one occurrence of each of [b]'s removed. */
        fun minus(a: List<String>, b: List<String>): List<String> {
            val left = b.groupingBy { it }.eachCount().toMutableMap()
            return a.filter { n -> (left[n] ?: 0).let { c -> if (c > 0) { left[n] = c - 1; false } else true } }
        }
    }
}
