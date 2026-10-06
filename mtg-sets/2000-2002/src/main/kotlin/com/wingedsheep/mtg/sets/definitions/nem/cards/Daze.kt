package com.wingedsheep.mtg.sets.definitions.nem.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Daze
 * {1}{U}
 * Instant
 *
 * You may return an Island you control to its owner's hand rather than pay this spell's mana cost.
 * Counter target spell unless its controller pays {1}.
 *
 * The free alternative is a [SelfAlternativeCost] whose only cost is the non-mana
 * [Costs.additional.ReturnToHand] of one Island you control (any land with the Island subtype,
 * basic or not). The counter half is the ordinary [Effects.CounterUnlessPays].
 */
val Daze = card("Daze") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may return an Island you control to its owner's hand rather than pay this " +
        "spell's mana cost.\nCounter target spell unless its controller pays {1}."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.ReturnToHand(GameObjectFilter.Land.withSubtype(Subtype.ISLAND))
        )
    )

    spell {
        val t = target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{1}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Matthew D. Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d03bff25-0d5e-4dcf-8d75-6df846afea3b.jpg?1789015977"
        ruling(
            "2026-03-20",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying (such as the alternative cost of Daze), add any cost increases, then apply any cost " +
                "reductions. The mana value of the spell is determined by only its mana cost, no matter what " +
                "the total cost to cast that spell was."
        )
    }
}
