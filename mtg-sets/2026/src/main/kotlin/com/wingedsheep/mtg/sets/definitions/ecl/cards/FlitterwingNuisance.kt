package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.events.Recipient

val FlitterwingNuisance = card("Flitterwing Nuisance") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "This creature enters with a -1/-1 counter on it.\n" +
        "{2}{U}, Remove a counter from this creature: Whenever a creature you control deals " +
        "combat damage to a player or planeswalker this turn, draw a card."

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithCounters(
        counterType = CounterType.MINUS_ONE_MINUS_ONE,
        count = 1,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{U}"),
            Costs.RemoveCounterFromSelf(CounterType.MINUS_ONE_MINUS_ONE)
        )
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer),
            effect = Effects.DrawCards(1),
            expiry = DelayedTriggerExpiry.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad0f6536-5295-4835-8883-35d711dfe6de.jpg?1767862448"
    }
}
