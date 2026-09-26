package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Ethereal Haze
 * {W}
 * Instant — Arcane
 *
 * Prevent all damage that would be dealt by creatures this turn.
 *
 * Not a Fog: Fog prevents all *combat* damage, while this prevents *all* damage from a class of
 * sources, so the shield keeps `PreventionScope.AllDamage` (the default) and narrows the
 * *source* side instead — [PreventionDirection.FromTarget] with a
 * [PreventionSourceFilter.Matching] over every creature. A creature's activated ability that
 * pings is stopped too, which is what the printed line says. The filter is re-evaluated when each
 * damage instance would be dealt, so a creature that enters later this turn is covered.
 * (`Effects.PreventCombatDamageFrom` is the combat-only sibling and would silently narrow the card.)
 */
val EtherealHaze = card("Ethereal Haze") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant — Arcane"
    oracleText = "Prevent all damage that would be dealt by creatures this turn."

    spell {
        effect = Effects.PreventDamage(
            direction = PreventionDirection.FromTarget,
            sources = PreventionSourceFilter.Matching(GameObjectFilter.Creature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Chris Appelhans"
        flavorText = "\"Imagine a dove flying through smoke. Does the dove injure the smoke? Does the smoke impede the dove?\"\n—Teachings of Eight-and-a-Half-Tails"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f1996af-5f15-447f-9b1d-98a7e97df53a.jpg?1783944341"
    }
}
