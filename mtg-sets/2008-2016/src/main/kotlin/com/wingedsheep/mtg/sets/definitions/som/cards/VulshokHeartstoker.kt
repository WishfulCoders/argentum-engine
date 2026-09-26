package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vulshok Heartstoker
 * {2}{R}
 * Creature — Human Shaman
 * 2/2
 *
 * When this creature enters, target creature gets +2/+0 until end of turn.
 */
val VulshokHeartstoker = card("Vulshok Heartstoker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, target creature gets +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature)
        description = "When this creature enters, target creature gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Shelly Wan"
        flavorText = "He fashions stirring words with as much passion as a smith fashioning a warhammer."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d3152bc-5c59-4e98-95de-a51de05a3c98.jpg?1783941721"
    }
}
