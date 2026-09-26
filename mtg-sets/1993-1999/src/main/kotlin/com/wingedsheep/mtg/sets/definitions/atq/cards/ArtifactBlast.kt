package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Artifact Blast
 * {R}
 * Instant
 * Counter target artifact spell.
 */
val ArtifactBlast = card("Artifact Blast") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Counter target artifact spell."

    spell {
        target(TargetFilter(GameObjectFilter.Artifact, zone = Zone.STACK))
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Mark Poole"
        flavorText = "The first line of defense against Urza and Mishra, the Artifact Blast achieved widespread fame until an unlucky mage discovered it was useless on the devices the brothers had already created."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1506d99d-7b2e-4101-84a5-c950dadb263a.jpg?1562899411"
    }
}
