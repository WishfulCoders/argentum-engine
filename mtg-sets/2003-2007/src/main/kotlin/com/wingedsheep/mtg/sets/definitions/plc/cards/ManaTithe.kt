package com.wingedsheep.mtg.sets.definitions.plc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mana Tithe — Planar Chaos #25 (canonical printing)
 * {W} · Instant
 *
 * Counter target spell unless its controller pays {1}.
 *
 * Planar Chaos's white "color-shifted" Force Spike.
 */
val ManaTithe = card("Mana Tithe") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {1}."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{1}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Martina Pilcerova"
        flavorText = "\"Those who seek to upset the balance must be taxed for such ambitions.\"\n" +
            "—Verithain, mesa high priest"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d48d622-f397-4f31-b1a5-0c23f60aa71c.jpg?1783943165"
        ruling("2004-10-04", "The payment is optional.")
    }
}
