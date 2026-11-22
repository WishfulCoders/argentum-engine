package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Suit Up — Kamigawa: Neon Dynasty #81 (canonical printing)
 * {2}{U} · Instant
 *
 * Until end of turn, target creature or Vehicle becomes an artifact creature with base power and
 * toughness 4/5.
 * Draw a card.
 *
 * [Effects.BecomeCreature] adds CREATURE (plus ARTIFACT via `addTypes`) in layer 4 and sets base
 * P/T 4/5 in layer 7b, keeping every other type, subtype and ability — so an uncrewed Vehicle
 * becomes a 4/5 artifact creature and counters/pumps still apply on top.
 */
val SuitUp = card("Suit Up") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature or Vehicle becomes an artifact creature with base power " +
        "and toughness 4/5.\nDraw a card."

    spell {
        val t = target(TargetFilter(GameObjectFilter.CreatureOrVehicle))
        effect = Effects.BecomeCreature(
            target = t,
            power = 4,
            toughness = 5,
            addTypes = setOf("ARTIFACT"),
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Miguel Mercado"
        flavorText = "With a reckless leap into the pilot's seat, Hirori went from smallest kid on the block to biggest hero in the city."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50870bfd-7779-4a1c-b18f-57fac7e193b1.jpg?1783923894"
    }
}
