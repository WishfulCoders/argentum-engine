package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.Targets

/**
 * Spark Spray
 * {R}
 * Instant
 * Spark Spray deals 1 damage to any target.
 * Cycling {R}
 */
val SparkSpray = card("Spark Spray") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Spark Spray deals 1 damage to any target.\nCycling {R}"

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    keywordAbility(KeywordAbility.cycling("{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Pete Venters"
        flavorText = "It's the only kind of shower goblins will tolerate."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f60d8716-4297-484c-8e02-c30ce2773a65.jpg?1562536945"
    }
}
