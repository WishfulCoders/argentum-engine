package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hurl into History
 * {3}{U}{U}
 * Instant
 * Counter target artifact or creature spell. Discover X, where X is that spell's mana value.
 *
 * X is read from the countered spell (the target) via its mana value. Discover then exiles
 * from the top until a nonland card with mana value ≤ X.
 */
val HurlIntoHistory = card("Hurl into History") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target artifact or creature spell. Discover X, where X is that spell's mana value."
    spell {
        val artifactOrCreatureSpell = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature, zone = Zone.STACK))
        effect = Effects.CounterSpell() then Effects.Discover(
            DynamicAmounts.manaValueOf(artifactOrCreatureSpell)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5946463a-2240-4376-b6f5-fd6e3a9cc51c.jpg?1782694562"
    }
}
