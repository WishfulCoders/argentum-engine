package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rapier Wit
 * {1}{W}
 * Instant
 * Tap target creature. If it's your turn, put a stun counter on it. (If a permanent with a
 * stun counter would become untapped, remove one from it instead.)
 * Draw a card.
 *
 * The stun counter is gated on [IsYourTurn] via [Effects.If] — a synchronous
 * resolution-time test, no pause. Stun is engine-wired (CR 122.1d) through `untapOrConsumeStun`,
 * so `Effects.AddCounters(CounterType.STUN, ...)` is all that's needed. `ContextTarget(0)` (the
 * default for the single-target effects) is the tapped creature.
 */
val RapierWit = card("Rapier Wit") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Tap target creature. If it's your turn, put a stun counter on it. (If a " +
        "permanent with a stun counter would become untapped, remove one from it instead.)\n" +
        "Draw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t) then
            Effects.If(
                condition = IsYourTurn,
                then = Effects.AddCounters(CounterType.STUN, 1, t),
            ) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Joshua Raphael"
        flavorText = "\"I make promises, not threats.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97b50521-5a0f-4dbd-8e15-f0f0d059c258.jpg?1775937109"
    }
}
