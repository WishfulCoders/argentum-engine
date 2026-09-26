package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dark Offering
 * {4}{B}{B}
 * Sorcery
 * Destroy target nonblack creature. You gain 3 life.
 *
 * The Portal "nonblack removal plus lifegain" template — Soul Shred with a destroy instead of
 * damage. The destroy is plain (regenerable), so no can't-be-regenerated marker.
 */
val DarkOffering = card("Dark Offering") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target nonblack creature. You gain 3 life."

    spell {
        val t = target(TargetFilter.Creature.notColor(Color.BLACK))
        effect = Effects.Destroy(t) then Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"Our greatest hope has become our enemy's greatest triumph.\"\n—Restela, Alaborn marshal"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ce0cef9-6de4-4a71-b76a-eb0198387294.jpg"
    }
}
