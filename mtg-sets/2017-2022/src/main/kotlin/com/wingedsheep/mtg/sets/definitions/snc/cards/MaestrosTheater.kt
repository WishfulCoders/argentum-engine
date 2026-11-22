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
 * Maestros Theater — Streets of New Capenna #251. One of SNC's family fetch lands.
 *
 * When this land enters, sacrifice it. When you do, search your library for a basic Island,
 * Swamp, or Mountain card, put it onto the battlefield tapped, then shuffle and you gain 1 life.
 *
 * The sacrifice is gated on the land still being on the battlefield: if it left before the
 * enters trigger resolved, it can't be sacrificed, so the reflexive "when you do" never triggers.
 */
val MaestrosTheater = card("Maestros Theater") {
    typeLine = "Land"
    oracleText = "When this land enters, sacrifice it. When you do, search your library for a basic " +
        "Island, Swamp, or Mountain card, put it onto the battlefield tapped, then shuffle and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.SourceInZone(Zone.BATTLEFIELD),
            then = Effects.ReflexiveTrigger(
                action = Effects.SacrificeTarget(EffectTarget.Self),
                optional = false,
                reflexiveEffect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                        listOf(Subtype.ISLAND, Subtype.SWAMP, Subtype.MOUNTAIN)
                    ),
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                    shuffleAfter = true
                ) then Effects.GainLife(1),
                descriptionOverride = "Sacrifice this land. When you do, search your library for a basic " +
                    "Island, Swamp, or Mountain card, put it onto the battlefield tapped, then shuffle and you gain 1 life."
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "251"
        artist = "Jokubas Uogintas"
        flavorText = "Only extraordinary performers are invited to grace the grandest stage in New Capenna."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51eb4a44-a5c8-49ed-b440-2462626bb638.jpg?1783923056"
    }
}
