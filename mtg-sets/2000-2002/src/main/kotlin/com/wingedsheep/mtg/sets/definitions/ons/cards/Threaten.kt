package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Threaten
 * {2}{R}
 * Sorcery
 * Untap target creature and gain control of it until end of turn.
 * That creature gains haste until end of turn.
 */
val Threaten = card("Threaten") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Untap target creature and gain control of it until end of turn. That creature gains haste until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Untap(t) then
            Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Mark Brill"
        flavorText = "Goblins' motivational techniques are crude, but effective."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de9676b6-6812-44e5-ad70-f498fbad0e18.jpg?1562947965"
    }
}
