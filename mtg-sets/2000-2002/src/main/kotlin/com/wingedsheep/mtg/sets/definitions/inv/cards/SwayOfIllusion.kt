package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sway of Illusion
 * {1}{U}
 * Instant
 * Any number of target creatures become the color of your choice until end of turn.
 * Draw a card.
 */
val SwayOfIllusion = card("Sway of Illusion") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Any number of target creatures become the color of your choice until end of turn.\n" +
        "Draw a card."

    spell {
        target = TargetObject(filter = TargetFilter.Creature, unlimited = true)
        effect = Effects.ChooseColorThen(
            then = Effects.ForEachTarget(
                Effects.ChangeColorToChosen(EffectTarget.ContextTarget(0))
            ),
            prompt = "Choose a color"
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff65e386-9aec-4deb-a4ec-d9a97bd87645.jpg?1562946589"
    }
}
