package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ixidor's Will
 * {2}{U}
 * Instant
 * Counter target spell unless its controller pays {2} for each Wizard on the battlefield.
 */
val IxidorsWill = card("Ixidor's Will") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {2} for each Wizard on the battlefield."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessDynamicPays(
            DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Wizard")).count() * 2
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Eric Peterson"
        flavorText = "\"Some dreams should not come to be.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b713448-853a-41ee-a302-963e9c1c1c65.jpg?1562901464"
    }
}
