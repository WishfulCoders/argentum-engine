package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dispersal Shield
 * {1}{U}
 * Instant
 * Counter target spell if its mana value is less than or equal to the
 * greatest mana value among permanents you control.
 */
val DispersalShield = card("Dispersal Shield") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell if its mana value is less than or equal to the greatest mana value among permanents you control."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.If(
            condition = Conditions.TargetSpellManaValueAtMost(DynamicAmounts.battlefield(Player.You).maxManaValue(), spell),
            then = Effects.CounterSpell()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Dave Dorman"
        flavorText = "Maybe next time."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c257df6-f275-40db-bfe3-a9291356cdf7.jpg?1562525399"
    }
}
