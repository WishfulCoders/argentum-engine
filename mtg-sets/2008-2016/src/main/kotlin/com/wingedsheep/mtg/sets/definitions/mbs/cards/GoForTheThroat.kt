package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Go for the Throat — Mirrodin Besieged #43
 * {1}{B} · Instant · Uncommon
 *
 * Destroy target nonartifact creature.
 *
 * The Terror shape with only the nonartifact restriction (and regeneration allowed). The target
 * filter is rechecked on resolution, so a creature that has become an artifact by then makes the
 * spell fizzle (ruling).
 */
val GoForTheThroat = card("Go for the Throat") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target nonartifact creature."

    spell {
        val t = target(TargetFilter.Creature.nonartifact())
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "David Rapoza"
        flavorText = "Having flesh is increasingly a liability on Mirrodin."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c665cfc-7e9a-444b-96b5-e8e4ef57a98a.jpg?1783941384"

        ruling(
            "2011-06-01",
            "If the targeted creature is also an artifact when Go for the Throat tries to resolve, it won't resolve."
        )
    }
}
