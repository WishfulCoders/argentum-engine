package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetEnchantedLandType

/**
 * Spreading Seas
 * {1}{U}
 * Enchantment — Aura
 * Enchant land
 * When this Aura enters, draw a card.
 * Enchanted land is an Island.
 *
 * Tainted Well with an Island: the land loses its other land types and printed abilities and taps
 * for {U}, keeping its name, supertypes and basic/snow status (ruling).
 */
val SpreadingSeas = card("Spreading Seas") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted land is an Island."

    auraTarget = Targets.Land

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = SetEnchantedLandType("Island")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Jung Park"
        flavorText = "Most inhabitants of Zendikar have given up on the idea of an accurate map."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37454c1c-4098-4ac2-884e-3f65f1384bdb.jpg?1783942160"
        ruling("2009-10-01", "The enchanted land loses its existing land types and any abilities printed on it. It now has the land type Island and has the ability to tap to produce {U}. Spreading Seas doesn't change the enchanted land's name or whether it's legendary, basic, or snow.")
    }
}
