package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Anchor to the Aether
 * {2}{U}
 * Sorcery
 * Put target creature on top of its owner's library. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)
 *
 * A [Effects.Composite] of the atomic [Effects.PutOnTopOfLibrary] on the chosen creature followed by
 * [Effects.Scry] 1. The scry is unconditional — it happens even if the creature has already left the
 * battlefield.
 */
val AnchorToTheAether = card("Anchor to the Aether") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Put target creature on top of its owner's library. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.PutOnTopOfLibrary(t) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15058f91-d266-4804-96af-fc050b6c8436.jpg?1783938355"
    }
}
