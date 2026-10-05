package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Unholy Heat
 * {R}
 * Instant
 * Unholy Heat deals 2 damage to target creature or planeswalker.
 * Delirium — Unholy Heat deals 6 damage instead if there are four or more card types among cards
 * in your graveyard.
 *
 * One target, one damage event: [Effects.If] over [Conditions.Delirium] picks 6 or 2 at resolution,
 * while Unholy Heat itself is still on the stack and so not counted (ruling 2021-06-18).
 */
val UnholyHeat = card("Unholy Heat") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Unholy Heat deals 2 damage to target creature or planeswalker.\nDelirium — Unholy Heat deals " +
        "6 damage instead if there are four or more card types among cards in your graveyard."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.Delirium(4),
            then = Effects.DealDamage(6, t),
            otherwise = Effects.DealDamage(2, t),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Kari Christensen"
        flavorText = "\"The devils use fire. Why shouldn't we?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b73d294-6ab1-4051-9b0f-d8e335d37674.jpg?1783926838"
        ruling("2021-06-18", "Unholy Heat checks your graveyard as it resolves to determine if it deals 2 or 6 damage. At that time, Unholy Heat isn't in the graveyard yet.")
    }
}
