package com.wingedsheep.gameserver.cube

import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.PlayBooster
import com.wingedsheep.sdk.limited.StandardBooster

/**
 * How a cube becomes packs.
 *
 * [SHUFFLED] is the classic cube: the whole list is shuffled once and dealt out, so every physical
 * card reaches at most one pack and the table is bounded by the cube's size. The two booster styles
 * treat the cube as a *set* instead: each pack is printed by rarity slot from the cube's cards, the
 * way a real booster is printed from a set, so a card may open in several packs (never twice in one)
 * and there is no capacity limit. That is what lets a host draft "Bloomburrow plus the Commander
 * decks' new cards" with the paper rarity distribution.
 */
enum class CubeBoosterStyle(
    /** Pack composition for the booster styles; null for [SHUFFLED], which is dealt by `CubeDealer`. */
    val strategy: BoosterStrategy?,
) {
    SHUFFLED(null),

    /** 7 commons, 3 uncommons, a rare/mythic and two any-rarity wildcards — see [PlayBooster]. */
    PLAY_BOOSTER(PlayBooster()),

    /** The mythic-era Draft Booster: 10 commons, 3 uncommons and a rare/mythic. */
    DRAFT_BOOSTER(StandardBooster(commons = 10)),
    ;

    val printsPacks: Boolean get() = strategy != null

    /**
     * Cards in one pack of this style, given the cube's own [packSize] for [SHUFFLED]. Basic lands
     * are never in the pool (they are supplied at deckbuilding), so the booster styles have no land slot.
     */
    fun cardsPerPack(packSize: Int): Int = when (this) {
        SHUFFLED -> packSize
        PLAY_BOOSTER -> 13
        DRAFT_BOOSTER -> 14
    }

    companion object {
        /** Lenient wire parse: null/blank means [SHUFFLED]; an unknown value is null so callers can reject it. */
        fun parse(raw: String?): CubeBoosterStyle? =
            if (raw.isNullOrBlank()) SHUFFLED else entries.firstOrNull { it.name == raw.trim().uppercase() }
    }
}
