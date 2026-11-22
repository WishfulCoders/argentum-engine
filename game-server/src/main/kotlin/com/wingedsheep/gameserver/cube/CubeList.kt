package com.wingedsheep.gameserver.cube

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.PrintingRef
import com.wingedsheep.sdk.model.Rarity

/**
 * User-authored cube definition. Entries retain counts because singleton is conventional, not a
 * rule, and may pin a printing for presentation without changing the card's oracle identity.
 */
data class CubeList(
    val name: String,
    val cards: List<CubeCardEntry>,
    val basicLandSetCode: String,
    val packSize: Int = DEFAULT_PACK_SIZE,
    val boosterStyle: CubeBoosterStyle = CubeBoosterStyle.SHUFFLED,
) {
    init {
        require(name.isNotBlank()) { "Cube name must not be blank" }
        require(packSize > 0) { "Cube pack size must be positive, got $packSize" }
    }

    companion object {
        const val DEFAULT_PACK_SIZE = 15
    }
}

data class CubeCardEntry(
    val name: String,
    val count: Int = 1,
    val printing: PrintingRef? = null,
    /**
     * The card's rarity *in this cube*, which is what a booster-style cube prints it at. Null keeps
     * the card's own rarity. A cube built from a set carries the set's rarities here, because a
     * reprint's canonical rarity is the one from its first printing.
     */
    val rarity: Rarity? = null,
) {
    init {
        require(name.isNotBlank()) { "Cube card name must not be blank" }
        require(count > 0) { "Cube card count must be positive for $name, got $count" }
    }
}

/**
 * A fully resolved, playable cube. [cards] is expanded according to each entry's count — a physical
 * copy each for [CubeBoosterStyle.SHUFFLED], a weight within its rarity for the booster styles — and
 * each card already carries its cube rarity.
 */
data class ResolvedCube(
    val name: String,
    val cards: List<CardDefinition>,
    val basicLandSetCode: String,
    val packSize: Int,
    val boosterStyle: CubeBoosterStyle = CubeBoosterStyle.SHUFFLED,
) {
    /** Cards per pack as players see it: the cube's own size when dealt, the booster layout's otherwise. */
    val cardsPerPack: Int get() = boosterStyle.cardsPerPack(packSize)

    /** Explicit per-name rarities, for persisting a lobby's cube without its card objects. */
    fun rarityByName(): Map<String, Rarity> = cards.associate { it.name to it.metadata.rarity }
}
