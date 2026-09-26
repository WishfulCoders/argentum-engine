package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nightmare Sower
 * {3}{B}
 * Creature — Faerie Assassin
 * 2/3
 *
 * Flying, lifelink
 * Whenever you cast a spell during an opponent's turn, put a -1/-1 counter on
 * up to one target creature.
 */
val NightmareSower = card("Nightmare Sower") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Faerie Assassin"
    power = 2
    toughness = 3
    oracleText = "Flying, lifelink\nWhenever you cast a spell during an opponent's turn, " +
        "put a -1/-1 counter on up to one target creature."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.casts()
        triggerRestriction = Conditions.IsNotYourTurn
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Tommy Arnold"
        flavorText = "The fae giggled and the nightmare took root."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35dfa0f9-faf3-4a85-b02d-0c5830783511.jpg?1767957131"
    }
}
