package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Galvanic Blast
 * {R}
 * Instant
 * Galvanic Blast deals 2 damage to any target.
 * Metalcraft — Galvanic Blast deals 4 damage instead if you control three or more artifacts.
 *
 * Metalcraft is an ability word with no rules meaning (Chrome Steed); the "instead" is a
 * [DynamicAmount.Conditional] on the artifact count, read as the spell resolves.
 */
val GalvanicBlast = card("Galvanic Blast") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Galvanic Blast deals 2 damage to any target.\n" +
        "Metalcraft — Galvanic Blast deals 4 damage instead if you control three or more artifacts."

    spell {
        val t = target("any target", Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmount.Conditional(
                Conditions.YouControlAtLeast(3, GameObjectFilter.Artifact),
                DynamicAmount.Fixed(4),
                DynamicAmount.Fixed(2)
            ),
            t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Marc Simonetti"
        flavorText = "Mirrodin has little weather, but it certainly has lightning."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5881bbc-8600-464d-9dcd-5a7780918d1d.jpg?1783941725"
    }
}
