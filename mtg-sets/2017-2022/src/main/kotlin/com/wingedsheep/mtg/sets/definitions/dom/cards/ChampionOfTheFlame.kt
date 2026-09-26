package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Champion of the Flame
 * {1}{R}
 * Creature — Human Warrior
 * 1/1
 * Trample
 * Champion of the Flame gets +2/+2 for each Aura and Equipment attached to it.
 */
val ChampionOfTheFlame = card("Champion of the Flame") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 1
    toughness = 1
    oracleText = "Trample\nChampion of the Flame gets +2/+2 for each Aura and Equipment attached to it."

    keywords(Keyword.TRAMPLE)

    // Gets +2/+2 for each Aura and Equipment attached to it
    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.attachmentsOnSelf() * 2,
            toughnessBonus = DynamicAmounts.attachmentsOnSelf() * 2
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Wayne Reynolds"
        flavorText = "Grand Warlord Radha's challengers are always defeated, but she rewards the most passionate with the Flame of Keld. After that, they burn for her."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/460fa161-362f-406d-9149-d412bc51836c.jpg?1562734927"
    }
}
