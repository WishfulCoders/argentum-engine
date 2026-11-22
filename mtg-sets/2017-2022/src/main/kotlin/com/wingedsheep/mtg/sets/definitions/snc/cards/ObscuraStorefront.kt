package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Obscura Storefront — Streets of New Capenna #252. One of SNC's family fetch lands.
 *
 * When this land enters, sacrifice it. When you do, search your library for a basic Plains,
 * Island, or Swamp card, put it onto the battlefield tapped, then shuffle and you gain 1 life.
 *
 * The sacrifice is gated on the land still being on the battlefield: if it left before the
 * enters trigger resolved, it can't be sacrificed, so the reflexive "when you do" never triggers.
 */
val ObscuraStorefront = card("Obscura Storefront") {
    typeLine = "Land"
    oracleText = "When this land enters, sacrifice it. When you do, search your library for a basic " +
        "Plains, Island, or Swamp card, put it onto the battlefield tapped, then shuffle and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.SourceInZone(Zone.BATTLEFIELD),
            then = Effects.ReflexiveTrigger(
                action = Effects.SacrificeTarget(EffectTarget.Self),
                optional = false,
                reflexiveEffect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                        listOf(Subtype.PLAINS, Subtype.ISLAND, Subtype.SWAMP)
                    ),
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                    shuffleAfter = true
                ) then Effects.GainLife(1),
                descriptionOverride = "Sacrifice this land. When you do, search your library for a basic " +
                    "Plains, Island, or Swamp card, put it onto the battlefield tapped, then shuffle and you gain 1 life."
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "252"
        artist = "Muhammad Firdaus"
        flavorText = "In volatile times, a sense of certainty is in high demand."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1016b884-9a4c-41de-bd65-14271f4f6f9a.jpg?1783923055"
    }
}
