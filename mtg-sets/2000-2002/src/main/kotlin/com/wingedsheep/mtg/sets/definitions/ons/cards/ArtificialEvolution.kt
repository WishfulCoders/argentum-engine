package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Artificial Evolution
 * {U}
 * Instant
 * Change the text of target spell or permanent by replacing all instances of one
 * creature type with another. The new creature type can't be Wall.
 * (This effect lasts indefinitely.)
 */
val ArtificialEvolution = card("Artificial Evolution") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Change the text of target spell or permanent by replacing all instances of one creature type with another. The new creature type can't be Wall. (This effect lasts indefinitely.)"

    spell {
        val t = target(TargetSpellOrPermanent())
        effect = Effects.ChangeCreatureTypeText(
            target = t,
            excludedTypes = listOf("Wall")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Greg Staples"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f46894d1-2503-43fa-938e-7bbf19101d13.jpg?1562952988"
    }
}
