package com.wingedsheep.gameserver.ai

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import org.springframework.stereotype.Component

/**
 * Reads which set(s) a submitted decklist was built from, so an AI seat on Auto can play out of the
 * same card pool as the human across the table.
 *
 * A card's canonical `CardDefinition` lives in its *earliest* printing's set, so `card.setCode`
 * alone would place a Bloomburrow deck's reprinted removal in a 1990s core set. Membership is
 * therefore read the way [com.wingedsheep.ai.engine.deck.FormatCardPool] reads a set's pool: its
 * own cards plus its reprint rows. The answer is then a greedy set cover over the deck's distinct
 * non-basic names — the set that explains the most cards first, then the next while it still
 * explains a meaningful slice of what's left — so a reprint shared by many sets is absorbed by
 * whichever set the rest of the deck already points at.
 *
 * Basic lands carry no set identity (every set prints them) and are ignored. Extension sets (bonus
 * sheets such as "The Big Score") are never chosen: they can't stand as a pool on their own.
 */
@Component
class DeckSetInference(
    private val boosterGenerator: BoosterGenerator,
    private val cardRegistry: CardRegistry,
) {
    /** Card name → codes of the (non-extension) sets that print it. Built once; sets are static. */
    private val setsByName: Map<String, Set<String>> by lazy {
        val index = mutableMapOf<String, MutableSet<String>>()
        for (config in boosterGenerator.availableSets.values) {
            if (config.extensionSet) continue
            val names = config.cards.map { it.name } + config.printings.map { it.name }
            for (name in names) index.getOrPut(name) { mutableSetOf() }.add(config.setCode)
        }
        index
    }

    /**
     * The sets [deckList] was built from, best explanation first — or empty when the deck has no
     * clear set identity (a many-set constructed pile, or nothing but basics), in which case the
     * caller should fall back to whatever it would have done without a match.
     */
    fun setsOf(deckList: Map<String, Int>): List<String> {
        val names = nonBasicNames(deckList)
        if (names.isEmpty()) return emptyList()

        val uncovered = names.toMutableSet()
        val chosen = mutableListOf<String>()
        while (chosen.size < MAX_SETS && uncovered.size > names.size * DONE_WHEN_UNCOVERED_AT_MOST) {
            val best = uncovered
                .flatMap { setsByName[it].orEmpty() }
                .filter { it !in chosen }
                .groupingBy { it }
                .eachCount()
                .entries
                .maxWithOrNull(
                    compareBy<Map.Entry<String, Int>> { it.value }
                        // Two sets explaining the same cards: prefer the one a sealed pool can be
                        // opened from, then the bigger one (a full set over a precon reprinting it).
                        .thenBy { boosterGenerator.availableSets[it.key]?.fullyImplemented == true }
                        .thenBy { boosterGenerator.availableSets[it.key]?.distinctCardCount ?: 0 },
                ) ?: break
            // A later set has to pull its weight; one stray reprint doesn't widen the pool.
            if (chosen.isNotEmpty() && best.value < MIN_CARDS_FOR_EXTRA_SET) break
            chosen += best.key
            uncovered.removeAll { best.key in setsByName[it].orEmpty() }
        }

        val covered = names.size - uncovered.size
        return if (covered >= names.size * MIN_COVERAGE) chosen else emptyList()
    }

    /** Share of [deckList]'s distinct non-basic names that [setCodes] print, 0.0–1.0. */
    fun coverage(deckList: Map<String, Int>, setCodes: Collection<String>): Double {
        val names = nonBasicNames(deckList)
        if (names.isEmpty()) return 0.0
        val inSets = names.count { name -> setsByName[name].orEmpty().any { it in setCodes } }
        return inSets.toDouble() / names.size
    }

    /** Distinct non-basic card names, with any `Name#printing` pin stripped. */
    fun nonBasicNames(deckList: Map<String, Int>): Set<String> =
        deckList.filterValues { it > 0 }.keys
            .map { it.substringBefore('#') }
            .filterNot { cardRegistry.getCard(it)?.typeLine?.isBasicLand == true }
            .toSet()

    private companion object {
        /** More than three sets and the deck is a cross-set pile, not "built from" any of them. */
        const val MAX_SETS = 3
        /** Stop widening once at most this share of the deck is unexplained. */
        const val DONE_WHEN_UNCOVERED_AT_MOST = 0.1
        const val MIN_CARDS_FOR_EXTRA_SET = 2
        /** Below this, the chosen sets are a minority of the deck and matching them would mislead. */
        const val MIN_COVERAGE = 0.6
    }
}
