package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Totem Speaker
 * {4}{G}
 * Creature — Elf Druid
 * 3/3
 * Whenever a Beast enters, you may gain 3 life.
 */
val TotemSpeaker = card("Totem Speaker") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 3
    toughness = 3
    oracleText = "Whenever a Beast enters, you may gain 3 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.BEAST)).enters()
        optional = true
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Darrell Riche"
        flavorText = "\"Elves and beasts are tied together, and I am the knot that binds them.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce12115b-2667-47f7-bd24-17c982a4f79a.jpg?1562936447"
    }
}
