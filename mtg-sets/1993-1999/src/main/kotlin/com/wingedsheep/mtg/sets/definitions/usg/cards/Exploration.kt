package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop

/**
 * Exploration
 * {G}
 * Enchantment
 * You may play an additional land on each of your turns.
 *
 * [GrantAdditionalLandDrop] grants are additive, so two Explorations give two extra land drops.
 */
val Exploration = card("Exploration") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "You may play an additional land on each of your turns."

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "250"
        artist = "Brian Snõddy"
        flavorText = "The first explorers found Argoth a storehouse of natural wealth—towering forests grown over rich veins of ore."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f09e451-0246-45a2-8bfd-07d3c65ddfe6.jpg?1783946316"

        ruling("2022-12-08", "Exploration's ability is cumulative with other effects that allow you to play additional lands, including ones created by other Explorations you control.")
    }
}
