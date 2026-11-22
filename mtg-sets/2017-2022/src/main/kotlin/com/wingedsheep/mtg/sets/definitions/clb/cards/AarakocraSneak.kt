package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aarakocra Sneak
 * {3}{U}
 * Creature — Bird Rogue
 * 1/4
 * Flying
 * When this creature enters, you take the initiative.
 */
val AarakocraSneak = card("Aarakocra Sneak") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Rogue"
    power = 1
    toughness = 4
    oracleText = "Flying\nWhen this creature enters, you take the initiative."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.TakeTheInitiative()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Andrea Radeck"
        flavorText = "\"Shhh! I think I hear goblins. And I definitely smell them.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a83882c-3e03-4e85-aaac-97fa1d08a772.jpg?1783922797"
    }
}
