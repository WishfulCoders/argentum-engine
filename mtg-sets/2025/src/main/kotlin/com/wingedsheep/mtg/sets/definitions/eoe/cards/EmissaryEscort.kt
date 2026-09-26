package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Emissary Escort
 * {1}{U}
 * Artifact Creature — Robot Soldier
 * This creature gets +X/+0, where X is the greatest mana value among other artifacts you control.
 * 0/4
 */
val EmissaryEscort = card("Emissary Escort") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot Soldier"
    power = 0
    toughness = 4
    oracleText = "This creature gets +X/+0, where X is the greatest mana value among other artifacts you control."

    // Static ability: +X/+0 where X is the greatest mana value among other artifacts you control
    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter(
                    cardPredicates = listOf(
                        com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact
                    ),
                    controllerPredicate = com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByYou
                ),
                excludeSelf = true
            ).maxManaValue(),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Igor Grechanyi"
        flavorText = "Emissary mechans are often assigned their own security detail given their importance in inter-Pinnacle dialogs."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b52ba87f-3ac7-4f32-901c-d089df979f94.jpg?1752946771"
    }
}
