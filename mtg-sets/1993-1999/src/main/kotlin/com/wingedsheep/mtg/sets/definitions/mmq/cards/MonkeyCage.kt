package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Monkey Cage
 * {5}
 * Artifact
 * When a creature enters, sacrifice this artifact and create X 2/2 green Monkey creature
 * tokens, where X is that creature's mana value.
 *
 * Any creature, under any player's control. The sacrifice is not a cost or an "if you do": the
 * tokens are created even if the Cage has already left the battlefield by resolution
 * (`SacrificeSelfEffect` is a no-op then). X reads the triggering creature's mana value.
 */
val MonkeyCage = card("Monkey Cage") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "When a creature enters, sacrifice this artifact and create X 2/2 green Monkey " +
        "creature tokens, where X is that creature's mana value."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).enters()
        effect = SacrificeSelfEffect then Effects.CreateToken(
            count = DynamicAmounts.triggeringManaValue(),
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Monkey"),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "307"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07f6be53-7a20-4e6b-a6ce-11cba06af8cb.jpg?1783945911"
    }
}
