package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * City of Shadows
 * Land
 * {T}, Exile a creature you control: Put a storage counter on this land.
 * {T}: Add {C} for each storage counter on this land.
 *
 * Both abilities cost {T}, which is the card's whole tension: a turn spent charging is a turn not
 * spent spending, and nothing but the shared tap cost enforces that.
 *
 * The mana ability counts the storage counters at resolution off the land itself, so a charge made
 * earlier in the turn already counts — and a counter removed by something else stops counting.
 * Storage counters carry no rule of their own; this card's second ability is the only thing that
 * gives the pile meaning.
 *
 * The exile is a **cost**, so the creature is gone before the counter is placed and the ability
 * can't be responded to by killing the creature in between.
 */
val CityOfShadows = card("City of Shadows") {
    typeLine = "Land"
    oracleText = "{T}, Exile a creature you control: Put a storage counter on this land.\n" +
        "{T}: Add {C} for each storage counter on this land."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            // The battlefield zone map is keyed by owner, so `youControl()` is what actually
            // enforces the printed "you control".
            Costs.ExilePermanentsFixed(1, GameObjectFilter.Creature.youControl()),
        )
        effect = Effects.AddCounters(CounterType.STORAGE, 1, EffectTarget.Self)
        description = "{T}, Exile a creature you control: Put a storage counter on this land."
    }

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddColorlessMana(
            DynamicAmounts.countersOnSelf(CounterType.STORAGE)
        )
        description = "{T}: Add {C} for each storage counter on this land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Tom Wänerstrand"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76e5ee8a-34e5-4a2e-a04e-9fcdc7e53dda.jpg?1783947922"
    }
}
