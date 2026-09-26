package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats

/**
 * Boneclub Berserker
 * {3}{R}
 * Creature — Goblin Berserker
 * 2/4
 * This creature gets +2/+0 for each other Goblin you control.
 */
val BoneclubBerserker = card("Boneclub Berserker") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Berserker"
    power = 2
    toughness = 4
    oracleText = "This creature gets +2/+0 for each other Goblin you control."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.otherCreaturesWithSubtypeYouControl(Subtype.GOBLIN) * 2,
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Slawomir Maniak"
        flavorText = "Bolstered by the cheers of his warren, Trugg was ready to make his appearance in Auntie Grub's tales."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3dbbe30-3d6e-46f8-92c1-caee995cba1a.jpg?1767732716"
    }
}
