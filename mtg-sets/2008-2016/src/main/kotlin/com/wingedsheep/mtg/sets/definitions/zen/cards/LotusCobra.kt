package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Lotus Cobra
 * {1}{G}
 * Creature — Snake
 * 2/1
 * Landfall — Whenever a land you control enters, add one mana of any color.
 *
 * Canonical printing: Zendikar (2009) is Lotus Cobra's earliest real printing. It previously sat
 * in Bloomburrow Commander, which now carries a `Printing` row instead.
 *
 * Landfall is `Triggers.a(GameObjectFilter.Land.youControl()).enters()` — the `ZoneChangeEvent` over
 * `GameObjectFilter.Land.youControl()` with `TriggerBinding.ANY`.
 */
val LotusCobra = card("Lotus Cobra") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    power = 2
    toughness = 1
    oracleText = "Landfall — Whenever a land you control enters, add one mana of any color."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddAnyColorMana(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "168"
        artist = "Chippy"
        flavorText = "Its scales contain the essence of thousands of lotus blooms."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19adde22-e5eb-4815-beb6-c520b3274cc9.jpg"
    }
}
