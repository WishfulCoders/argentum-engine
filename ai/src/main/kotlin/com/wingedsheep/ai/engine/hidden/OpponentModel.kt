package com.wingedsheep.ai.engine.hidden

/**
 * Prior knowledge available when sampling an opponent's hidden cards.
 *
 * A known list is appropriate for open-decklist formats and the arena. When no list is available,
 * identity permutation removes knowledge of which hidden card occupies which slot without
 * inventing cards outside the game. The latter is intentionally "cheating-lite": it knows the
 * hidden multiset, but not hand contents or library order.
 *
 * Note that a known list, minus the cards already seen, *is* the hidden multiset, so the two carry
 * the same information. Only [DecklistMixture] knows less than the truth.
 */
sealed interface OpponentModel {
    data class KnownDecklist(val cards: Map<String, Int>) : OpponentModel {
        init {
            require(cards.values.all { it >= 0 }) { "Decklist counts must be non-negative" }
        }
    }

    data object IdentityPermutation : OpponentModel

    /**
     * A prior over the opponent's deck: each sample draws one of [lists] uniformly and samples the
     * hidden cards from it as [KnownDecklist] would. [lists] are other players' decks — the whole set's
     * for "knows nothing about this opponent", or one colour pair's for "knows their colours"
     * (mtg-draft-ai `docs/51_ptcg_style_selfplay` §9). The opponent's real deck must not be among them.
     */
    data class DecklistMixture(val lists: List<Map<String, Int>>) : OpponentModel {
        init {
            require(lists.isNotEmpty()) { "A mixture needs at least one decklist" }
            require(lists.all { l -> l.values.all { it >= 0 } }) { "Decklist counts must be non-negative" }
        }
    }
}
