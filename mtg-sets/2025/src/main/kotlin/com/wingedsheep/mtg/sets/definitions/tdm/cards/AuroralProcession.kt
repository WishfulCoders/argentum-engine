package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Auroral Procession — Tarkir: Dragonstorm #169
 * {G}{U} · Instant
 *
 * Return target card from your graveyard to your hand.
 */
val AuroralProcession = card("Auroral Procession") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Return target card from your graveyard to your hand."

    spell {
        val target = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.ReturnToHand(target)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Marco Gorlei"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/672f94ad-65d6-4c7d-925d-165ef264626f.jpg?1743204647"
    }
}
