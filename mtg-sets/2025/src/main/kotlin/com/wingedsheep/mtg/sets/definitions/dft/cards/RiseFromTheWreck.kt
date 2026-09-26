package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/** Rise from the Wreck — Aetherdrift #178. */
val RiseFromTheWreck = card("Rise from the Wreck") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return up to one target creature card, up to one target Mount card, up to one " +
        "target Vehicle card, and up to one target creature card with no abilities from your " +
        "graveyard to your hand."

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard, optional = true)
        val mount = target(
            TargetFilter(
                GameObjectFilter.Any.withSubtype(Subtype("Mount")).ownedByYou(),
                zone = Zone.GRAVEYARD,
            ),
            optional = true,
        )
        val vehicle = target(
            TargetFilter(
                GameObjectFilter.Any.withSubtype(Subtype.VEHICLE).ownedByYou(),
                zone = Zone.GRAVEYARD,
            ),
            optional = true,
        )
        val noAbilities = target(
            TargetFilter(Filters.CreatureWithNoAbilities.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        effect = Effects.ReturnToHand(creature) then
            Effects.ReturnToHand(mount) then
            Effects.ReturnToHand(vehicle) then
            Effects.ReturnToHand(noAbilities)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43e6ac32-a7a4-4c15-81b0-8485d6a0e7ca.jpg?1783907866"
    }
}
