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
 * Brokers Hideout — Streets of New Capenna #248. One of SNC's family fetch lands.
 *
 * When this land enters, sacrifice it. When you do, search your library for a basic Forest,
 * Plains, or Island card, put it onto the battlefield tapped, then shuffle and you gain 1 life.
 *
 * The sacrifice is gated on the land still being on the battlefield: if it left before the
 * enters trigger resolved, it can't be sacrificed, so the reflexive "when you do" never triggers.
 */
val BrokersHideout = card("Brokers Hideout") {
    typeLine = "Land"
    oracleText = "When this land enters, sacrifice it. When you do, search your library for a basic " +
        "Forest, Plains, or Island card, put it onto the battlefield tapped, then shuffle and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.SourceInZone(Zone.BATTLEFIELD),
            then = Effects.ReflexiveTrigger(
                action = Effects.SacrificeTarget(EffectTarget.Self),
                optional = false,
                reflexiveEffect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                        listOf(Subtype.FOREST, Subtype.PLAINS, Subtype.ISLAND)
                    ),
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                    shuffleAfter = true
                ) then Effects.GainLife(1),
                descriptionOverride = "Sacrifice this land. When you do, search your library for a basic " +
                    "Forest, Plains, or Island card, put it onto the battlefield tapped, then shuffle and you gain 1 life."
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "248"
        artist = "James Paick"
        flavorText = "Once the witness was inside, the safe house vanished first from sight, then from memory."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/989b299b-daa9-4bda-94e2-9a2f0e8f2bce.jpg?1783923058"
    }
}
