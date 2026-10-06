package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mana Leak — Stronghold #36 (canonical printing)
 * {1}{U} · Instant
 *
 * Counter target spell unless its controller pays {3}.
 */
val ManaLeak = card("Mana Leak") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {3}."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{3}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Christopher Rush"
        flavorText = "\"The fatal flaw in every plan is the assumption that you know more than your enemy.\"\n—Volrath"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abcaf16d-aa02-43e2-aa38-bb1835d47a05.jpg?1783946567"
    }
}
