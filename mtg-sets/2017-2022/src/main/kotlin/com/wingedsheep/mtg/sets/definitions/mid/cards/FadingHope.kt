package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fading Hope
 * {U}
 * Instant
 * Return target creature to its owner's hand. If its mana value was 3 or less, scry 1.
 */
val FadingHope = card("Fading Hope") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand. If its mana value was 3 or less, scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature) then
            Effects.If(
                condition = Conditions.TargetSpellManaValueAtMost(DynamicAmounts.fixed(3), creature),
                then = Patterns.Library.scry(1)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Rovina Cai"
        flavorText = "\"At least I won't become one of . . . those things.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2fb1fff-12be-4bd5-8dba-c36e84d49651.jpg?1634348819"
    }
}
