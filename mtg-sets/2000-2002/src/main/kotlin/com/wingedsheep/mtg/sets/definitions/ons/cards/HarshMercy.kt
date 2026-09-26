package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.withoutSubtypeInStoredList
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.dsl.Effects

/**
 * Harsh Mercy
 * {2}{W}
 * Sorcery
 * Each player chooses a creature type. Destroy all creatures that aren't of a type
 * chosen this way. They can't be regenerated.
 */
val HarshMercy = card("Harsh Mercy") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player chooses a creature type. Destroy all creatures that aren't of a type chosen this way. They can't be regenerated."

    spell {
        effect = Effects.Pipeline {
            val chosenTypes = eachPlayerChoosesCreatureType()
            val creatures = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature))
            val unchosen = filter(creatures, GameObjectFilter.Any.withoutSubtypeInStoredList(chosenTypes))
            destroy(unchosen, noRegenerate = true)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "John Matson"
        flavorText = "\"There is no greater burden than choosing who to save.\" —Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6473b4d-1f59-4216-ace9-f3e5306266fb.jpg?1562937932"
    }
}
