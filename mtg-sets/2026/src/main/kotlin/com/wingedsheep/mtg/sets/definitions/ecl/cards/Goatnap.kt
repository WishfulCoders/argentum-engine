package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val Goatnap = card("Goatnap") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn. If that creature is a Goat, it also gets +3/+0 until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withSubtype("Goat"), t),
                then = Effects.ModifyStats(3, 0, t)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Vincent Christiaens"
        flavorText = "Jealous of his sister's cloudgoat, the giant went to find the next best thing."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d8dbec5-ae71-4f82-898a-b930ec677403.jpg?1767658184"
    }
}
