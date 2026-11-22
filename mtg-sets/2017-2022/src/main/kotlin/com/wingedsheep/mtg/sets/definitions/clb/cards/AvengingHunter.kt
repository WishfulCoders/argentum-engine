package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Avenging Hunter
 * {4}{G}
 * Creature — Dragon Ranger
 * 5/4
 * Trample
 * When this creature enters, you take the initiative.
 */
val AvengingHunter = card("Avenging Hunter") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon Ranger"
    power = 5
    toughness = 4
    oracleText = "Trample\nWhen this creature enters, you take the initiative."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.TakeTheInitiative()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "215"
        artist = "Borja Pindado"
        flavorText = "\"I will remember my friends. But you? You won't be remembered at all.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4072640-0f9b-4f0a-84cc-eda415cc92e7.jpg?1783922721"
    }
}
