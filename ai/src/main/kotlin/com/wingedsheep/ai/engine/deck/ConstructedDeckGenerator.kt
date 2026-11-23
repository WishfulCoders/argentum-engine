package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.draftsim.DraftsimCardOps
import com.wingedsheep.ai.draftsim.DraftsimDeckBuilder
import com.wingedsheep.ai.draftsim.DraftsimDeckShape
import com.wingedsheep.ai.draftsim.DraftsimPoolCard
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random
import org.slf4j.LoggerFactory

/**
 * Builds a 60-card constructed deck that is legal in a given [DeckFormat].
 *
 * This is the constructed counterpart to [com.wingedsheep.ai.engine.SealedDeckGenerator]: where that
 * one opens boosters and autobuilds a 40-card limited deck, this one draws from a *constructed* card
 * pool — every card the format allows — and hands it to the same Draftsim autobuilder, running at
 * [DraftsimDeckShape.CONSTRUCTED] instead of the limited 23+17.
 *
 * It exists because the AI seat used to play a limited deck no matter what the lobby asked for. A
 * Pauper lobby validated the human's deck down to commons and then sat them across from a sealed
 * pool full of rares; the format restriction only ever applied to one side of the table.
 *
 * **Per-card legality is the whole of the pool filter.** [CardDefinition.legalFormats] is
 * Scryfall-sourced and already accounts for bans and set legality, so "is this card allowed in
 * Pauper" needs no rarity check here. Structural rules on top of that (singleton, 100 cards, a
 * commander in the command zone) are *not* modelled here: commander-shape formats are rejected
 * outright and belong to [CommanderDeckGenerator], which builds that shape instead.
 */
class ConstructedDeckGenerator(
    private val boosterGenerator: BoosterGenerator,
    private val cardRegistry: CardRegistry,
    /**
     * Randomness source for the shortlist draw and, on the fallback path, the colour pick and curve
     * fill. Defaults to [Random.Default] so live lobbies keep drawing fresh decks; pass a seeded
     * [Random] to make a run reproducible — the arena does, so a rerun at the same seed plays the
     * same decks.
     */
    private val random: Random = Random.Default,
) {
    private val cardPool = FormatCardPool(boosterGenerator, cardRegistry)

    /**
     * Builds a format-legal deck from the cards printed in [setCodes].
     *
     * @param setCodes sets to draw from. Empty means "every set" — the full format-legal pool.
     * @param format the constructed format the deck must be legal in.
     * @throws IllegalArgumentException if [format] is commander-shaped (use [CommanderDeckGenerator]
     *         for those), or if the legal pool is too thin to build from.
     */
    fun generate(setCodes: List<String>, format: DeckFormat): Map<String, Int> {
        require(!format.isCommanderShape) {
            "Commander-shape formats need a designated commander and singleton rules; " +
                "ConstructedDeckGenerator only builds the 60-card constructed shape. " +
                "Use CommanderDeckGenerator instead."
        }

        val pool = cardPool.legalPool(setCodes, format)
        require(pool.isNotEmpty()) {
            "No ${format.displayName}-legal cards available" +
                if (setCodes.isEmpty()) "" else " in ${setCodes.joinToString(", ")}"
        }

        val basics = cardPool.basicLands(setCodes)

        logger.info(
            "Building a {} deck from {} legal cards ({})",
            format.displayName,
            pool.size,
            if (setCodes.isEmpty()) "all sets" else setCodes.joinToString(", "),
        )

        val draftsim = draftsimBuild(pool)
        if (draftsim != null) {
            // Pin the basics to the set's art, exactly as a human's submitted deck is.
            return BoosterGenerator.withBasicLandArt(draftsim, basics.associateBy { it.name })
        }
        logger.warn(
            "Draftsim produced no {} build from {} legal cards; falling back to the random builder",
            format.displayName,
            pool.size,
        )
        val fallback = RandomDeckGenerator(
            cardPool = pool,
            basicLandVariants = basics,
            setCodes = setCodes,
            random = random,
        ).generate()
        requirePlayable(fallback, pool, format)
        return fallback
    }

    /**
     * Refuse a deck the pool was too thin to really build, the contract upstream's generator has
     * and `RandomDeckResolver` (taken from upstream in the 2026-11-22 merge) relies on: throwing is
     * what lets it fall back to a format-wide build instead of seating a pile. The random builder
     * cannot fill [SHAPE]'s spell slots from a thin pool and returns what it found plus its fixed
     * lands — four instants made it a 40-card "constructed" deck. The floors are upstream's: four
     * fifths of the spell slots and two fifths of the creature floor, and the deck must be legal
     * size. Draftsim's own build is already refused when short, in [draftsimBuild].
     */
    private fun requirePlayable(deckList: Map<String, Int>, pool: List<CardDefinition>, format: DeckFormat) {
        val byName = pool.associateBy { it.name }
        var nonland = 0
        var creatures = 0
        for ((key, count) in deckList) {
            val card = byName[key.substringBefore('#')] ?: continue
            if (card.isLand) continue
            nonland += count
            if (card.typeLine.isCreature) creatures += count
        }
        val size = deckList.values.sum()
        require(size >= DECK_SIZE && nonland >= MIN_NONLAND && creatures >= MIN_CREATURES) {
            "The ${format.displayName}-legal pool is too thin for a playable deck: $size cards " +
                "(need $DECK_SIZE), $nonland nonland (need $MIN_NONLAND), $creatures creatures (need $MIN_CREATURES)"
        }
    }

    /** Convenience overload: build from the whole format-legal card base. */
    fun generate(format: DeckFormat): Map<String, Int> = generate(emptyList(), format)

    /**
     * [generate] in the [GeneratedDeck] shape the server's lobbies read since the 2026-11-22
     * upstream merge. Upstream's version also draws a fifteen-card wishboard; this fork kept its
     * own deck builder (upstream's needs its card-reading layer), so the sideboard is empty.
     */
    fun generateDeck(setCodes: List<String>, format: DeckFormat): GeneratedDeck =
        GeneratedDeck(deckList = generate(setCodes, format))

    /**
     * Autobuild [pool] with Draftsim at the constructed shape, or null when it can't fill a deck.
     *
     * Two things have to be bridged to get a limited autobuilder onto a constructed pool:
     *
     *  - **Size.** A sealed pool is ~90 cards; a constructed one is thousands, and the scorer is
     *    quadratic in pool size (every card is scored against the whole pool). So the pool is first
     *    [shortlist]ed down to sealed scale.
     *  - **Copies.** Draftsim picks from *physical* pool cards and caps a name at 4 (2 for legendary
     *    creatures). Constructed's four-of rule is exactly that cap, so handing it four instances of
     *    every shortlisted card gets four-ofs of the good cards for free.
     *
     * Nonbasic lands are left out of the shortlist, so the manabase comes back as pure basics. The
     * builder *can* use pool lands, but it takes them unranked, and a constructed pool's land slice
     * is mostly narrow utility lands — a bad trade against a clean basics manabase.
     */
    private fun draftsimBuild(pool: List<CardDefinition>): Map<String, Int>? {
        val tables = ConstructedRatings.tables()
        val shortlist = shortlist(pool.filterNot { it.isLand }, DraftsimCardOps(tables))
        if (shortlist.isEmpty()) return null

        val instances = shortlist.flatMapIndexed { index, card ->
            val scorerCard = card.toScorerCard()
            (0 until COPIES_PER_CARD).map { copy -> DraftsimPoolCard(scorerCard, "pool-$index-$copy") }
        }
        val nameByInstance = instances.associate { it.instanceId to it.card.name }

        // "draft" mode ranks the two best hypotheses and skips the three-color good-stuff build,
        // which would ask an all-basics manabase to cast three colors.
        val build = runCatching { DraftsimDeckBuilder(tables, SHAPE).buildDecks(instances, mode = "draft") }
            .onFailure { logger.warn("Draftsim constructed build threw", it) }
            .getOrNull()
            ?.firstOrNull()
            ?: return null

        val deckList = LinkedHashMap<String, Int>()
        for (instanceId in build.deckInstanceIds) {
            val name = nameByInstance[instanceId] ?: continue
            deckList[name] = (deckList[name] ?: 0) + 1
        }
        for ((color, count) in build.basicsNeeded) {
            val name = COLOR_TO_BASIC[color] ?: continue
            if (count > 0) deckList[name] = (deckList[name] ?: 0) + count
        }

        // A short build is a build the pool couldn't support; the random fallback is checked in [generate].
        val size = deckList.values.sum()
        if (size < DECK_SIZE) {
            logger.warn("Draftsim constructed build came back {} cards short of {}", DECK_SIZE - size, DECK_SIZE)
            return null
        }
        return deckList
    }

    /**
     * Draw [SHORTLIST_SIZE] cards from [pool], biased hard towards the ones Draftsim rates highly.
     *
     * The weight is the rating *cubed*, so bombs almost always make the cut while the tail still
     * turns over between games — a constructed lobby that seated the identical AI deck every time
     * would be a worse experience than the unrated random build this replaces.
     *
     * Unrated cards (a set Draftsim never covered, or our own content) fall back to the rarity
     * ladder rather than dropping out, so a pool with no ratings at all still shortlists sanely.
     */
    private fun shortlist(pool: List<CardDefinition>, ops: DraftsimCardOps): List<CardDefinition> =
        pool.weightedSample(SHORTLIST_SIZE, random) { card ->
            val rating = ops.ratingFallback(card.toScorerCard()).coerceAtLeast(ConstructedRatings.MIN_WEIGHT)
            rating * rating * rating
        }

    private companion object {
        private val logger = LoggerFactory.getLogger(ConstructedDeckGenerator::class.java)

        private val SHAPE = DraftsimDeckShape.CONSTRUCTED
        private val DECK_SIZE = SHAPE.nonlandCount + SHAPE.landCount
        private val MIN_NONLAND = SHAPE.nonlandCount * 4 / 5
        private val MIN_CREATURES = SHAPE.creatureFloor * 2 / 5

        /**
         * How many distinct cards the autobuilder actually sees. Sealed scale — the builder's home
         * turf, and what keeps its quadratic scoring cheap — while still leaving every two-colour
         * guild enough castable cards to fill 36 slots.
         */
        private const val SHORTLIST_SIZE = 120

        /** Instances per shortlisted card — Draftsim's own four-of cap, so it can build four-ofs. */
        private const val COPIES_PER_CARD = 4

        private val COLOR_TO_BASIC =
            mapOf("W" to "Plains", "U" to "Island", "B" to "Swamp", "R" to "Mountain", "G" to "Forest")
    }
}
