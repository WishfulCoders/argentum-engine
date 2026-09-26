package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Simulacrum Synthesizer
 * {2}{U}
 * Artifact
 * When this artifact enters, scry 2.
 * Whenever another artifact you control with mana value 3 or greater enters, create a 0/0
 * colorless Construct artifact creature token with "This token gets +1/+1 for each artifact
 * you control."
 */
val SimulacrumSynthesizer = card("Simulacrum Synthesizer") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, scry 2.\nWhenever another artifact you control with mana value 3 or greater enters, create a 0/0 colorless Construct artifact creature token with \"This token gets +1/+1 for each artifact you control.\""

    // When this artifact enters, scry 2.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.scry(2)
    }

    // Whenever another artifact you control with mana value 3 or greater enters,
    // create a 0/0 Construct token with "+1/+1 for each artifact you control".
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Artifact.youControl().manaValueAtLeast(3)).enters()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = emptySet(),
            creatureTypes = setOf("Construct"),
            artifactToken = true,
            staticAbilities = listOf(
                GrantDynamicStats(
                    filter = GroupFilter.source(),
                    powerBonus = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Artifact
                    ).count(),
                    toughnessBonus = DynamicAmounts.battlefield(
                        Player.You,
                        GameObjectFilter.Artifact
                    ).count()
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/3/8/3877d2d1-61f2-4295-b0fc-827965eaaefc.jpg?1712317494"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaa05ad1-5cda-4edd-b6bf-562ae3e5011a.jpg?1739804163"
    }
}
