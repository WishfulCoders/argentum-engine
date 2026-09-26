package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val TamsResistance = card("Tam's Resistance") {
    manaCost = "{1}{G/U}"
    colorIdentity = "UG"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on up to one target creature. It gains vigilance until end of turn.\n" +
        "Empower Jace 4. (Put four loyalty counters on a Jace token you control. If you don't control one, first create a blue Jace planeswalker token with \"[−1]: Surveil 1\" and \"[−3]: Draw a card.\")"

    spell {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.VIGILANCE, creature) then
            Patterns.Mechanic.empowerJace(4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Samuel Perin"
        flavorText = "Tam had offered to help and Vraska believed her."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3d33df2-a77b-4c2e-ba7f-2cc9c1505f9b.jpg?1788878215"
        inBooster = false
    }
}
