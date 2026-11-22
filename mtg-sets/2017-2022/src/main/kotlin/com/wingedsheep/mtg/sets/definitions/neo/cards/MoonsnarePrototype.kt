package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Moonsnare Prototype — Kamigawa: Neon Dynasty #69 (canonical printing)
 * {U} · Artifact
 *
 * {T}, Tap an untapped artifact or creature you control: Add {C}.
 * Channel — {4}{U}, Discard this card: The owner of target nonland permanent puts it on their
 * choice of the top or bottom of their library.
 *
 * The mana ability is the Springleaf Drum shape; the source is excluded from its own tap cost
 * because {T} already taps it. The channel half is Desynchronize's owner-chooses placement.
 */
val MoonsnarePrototype = card("Moonsnare Prototype") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{T}, Tap an untapped artifact or creature you control: Add {C}.\n" +
        "Channel — {4}{U}, Discard this card: The owner of target nonland permanent puts it on " +
        "their choice of the top or bottom of their library."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.CreatureOrArtifact, excludeSelf = true)
        )
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    // Channel — {4}{U}, Discard this card (from hand).
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{U}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.PutOnTopOrBottomOfLibrary(permanent)
        description = "Channel — {4}{U}, Discard this card: The owner of target nonland permanent puts " +
            "it on their choice of the top or bottom of their library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Fariba Khamseh"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d8bc0e9-a536-4bca-92a6-8dca85e1e984.jpg?1783923897"
        ruling(
            "2022-02-18",
            "The owner of that permanent chooses whether it goes on top or bottom. Other players get " +
                "to know what they chose."
        )
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
        ruling(
            "2022-02-18",
            "If a channel ability requires a target, you may not activate it without a target just to " +
                "discard the card."
        )
    }
}
