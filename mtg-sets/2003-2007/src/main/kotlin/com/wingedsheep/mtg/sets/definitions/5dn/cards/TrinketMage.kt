package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Trinket Mage
 * {2}{U}
 * Creature — Human Wizard
 * 2/2
 * When this creature enters, you may search your library for an artifact card with mana value 1
 * or less, reveal that card, put it into your hand, then shuffle.
 *
 * The same optional reveal-tutor as Transit Mage with a mana value ≤ 1 filter; an {X} in a library
 * card's cost counts as 0 (2021-03-19 ruling), which the mana value filter already does.
 */
val TrinketMage = card("Trinket Mage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may search your library for an artifact card with " +
        "mana value 1 or less, reveal that card, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Artifact.manaValueAtMost(1),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Mark A. Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c5a41ab-1840-4abb-a8bb-f0b1e7d1b450.jpg?1783944402"
        ruling("2021-03-19", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
    }
}
