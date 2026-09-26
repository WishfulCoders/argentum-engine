package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Summit Intimidator — Tarkir: Dragonstorm #125
 * {3}{R} · Creature — Yeti · 4/3
 *
 * Reach
 * When this creature enters, target creature can't block this turn.
 */
val SummitIntimidator = card("Summit Intimidator") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Yeti"
    power = 4
    toughness = 3
    oracleText = "Reach\nWhen this creature enters, target creature can't block this turn."

    keywords(Keyword.REACH)

    triggeredAbility {
        val creature = target(TargetFilter.Creature)
        trigger = Triggers.self.enters()
        effect = Effects.CantBlock(target = creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Diego Gisbert"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3cba0b1-7c22-4e51-b9cf-5bf01e67a222.jpg?1743204466"
    }
}
