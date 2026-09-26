package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fleeting Flight
 * {W}
 * Instant
 * Put a +1/+1 counter on target creature. It gains flying until end of turn.
 * Prevent all combat damage that would be dealt to it this turn.
 */
val FleetingFlight = card("Fleeting Flight") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature. It gains flying until end of turn. " +
        "Prevent all combat damage that would be dealt to it this turn."

    spell {
        val target = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, target) then
            Effects.GrantKeyword(Keyword.FLYING, target) then
            Effects.PreventDamage(target = target, combatOnly = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Leonardo Santanna"
        flavorText = "On that fateful day, Sir Brilbury's fear of heights was outmatched by his sense of duty."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55139100-9342-41fd-b10a-8e9932e605d4.jpg?1782689255"
    }
}
