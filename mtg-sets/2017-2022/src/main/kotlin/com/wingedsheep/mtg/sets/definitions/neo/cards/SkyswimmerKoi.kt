package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skyswimmer Koi — Kamigawa: Neon Dynasty #79 (canonical printing)
 * {3}{U} · Creature — Fish · 3/3
 *
 * Flying
 * Whenever an artifact you control enters, you may draw a card. If you do, discard a card.
 */
val SkyswimmerKoi = card("Skyswimmer Koi") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhenever an artifact you control enters, you may draw a card. If you do, discard a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).enters()
        effect = Effects.May(Patterns.Hand.loot())
        description = "Whenever an artifact you control enters, you may draw a card. If you do, discard a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Donato Giancola"
        flavorText = "Once they swam alone among the clouds, but now the people of Kamigawa have built high enough to reach them."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db50259b-8ca8-4fbe-8ae3-6b0312d70980.jpg?1783923894"
    }
}
