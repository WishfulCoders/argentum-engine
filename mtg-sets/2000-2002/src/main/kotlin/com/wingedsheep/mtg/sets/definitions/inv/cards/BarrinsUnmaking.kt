package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.TargetSharesMostCommonColor
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Barrin's Unmaking (INV 46)
 * {1}{U}
 * Instant
 * Return target permanent to its owner's hand if that permanent shares a color with the most
 * common color among all permanents or a color tied for most common.
 *
 * Reuses [TargetSharesMostCommonColor] (shared with Tsabo's Assassin) gating a
 * [Effects.ReturnToHand] via [Effects.If]. The condition is evaluated at resolution against
 * the board's current color distribution (CR 608.2); if the target permanent doesn't share the
 * most-common color it stays put.
 */
val BarrinsUnmaking = card("Barrin's Unmaking") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target permanent to its owner's hand if that permanent shares a color " +
        "with the most common color among all permanents or a color tied for most common."

    spell {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.If(
            condition = TargetSharesMostCommonColor(),
            then = Effects.ReturnToHand(permanent)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Luca Zontini"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d4cecb0-12b5-4678-b5e7-8cec8fc86cef.jpg?1562910648"
    }
}
