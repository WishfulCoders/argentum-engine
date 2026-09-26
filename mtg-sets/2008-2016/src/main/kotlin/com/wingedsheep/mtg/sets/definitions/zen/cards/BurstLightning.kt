package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Burst Lightning
 * {R}
 * Instant
 * Kicker {4}
 * Burst Lightning deals 2 damage to any target.
 * If this spell was kicked, it deals 4 damage instead.
 */
val BurstLightning = card("Burst Lightning") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Kicker {4} (You may pay an additional {4} as you cast this spell.)\n" +
        "Burst Lightning deals 2 damage to any target. If this spell was kicked, it deals 4 damage instead."

    keywordAbility(KeywordAbility.kicker("{4}"))

    spell {
        val t = target(Targets.Any)
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.DealDamage(4, t),
            otherwise = Effects.DealDamage(2, t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Vance Kovacs"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dc16614-5cf8-444d-a5ae-cac25018af68.jpg?1782715642"
    }
}
