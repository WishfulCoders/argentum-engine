package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bloody Betrayal
 * {2}{R}
 * Sorcery
 *
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until
 * end of turn. Create a Blood token.
 */
val BloodyBetrayal = card("Bloody Betrayal") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains " +
        "haste until end of turn. Create a Blood token. (It's an artifact with \"{1}, {T}, Discard " +
        "a card, Sacrifice this token: Draw a card.\")"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.CreateBlood(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8970a5d6-dcab-415a-851b-20e228ef7d16.jpg?1782703085"
    }
}
