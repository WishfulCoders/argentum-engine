package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.dsl.Targets

/**
 * Hypnotic Cloud
 * {1}{B}
 * Sorcery
 * Kicker {4}
 * Target player discards a card. If this spell was kicked, that player discards
 * three cards instead.
 */
val HypnoticCloud = card("Hypnotic Cloud") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Kicker {4} (You may pay an additional {4} as you cast this spell.)\n" +
        "Target player discards a card. If this spell was kicked, that player discards three cards instead."

    keywordAbility(KeywordAbility.kicker("{4}"))

    spell {
        val targetPlayer = target(Targets.Player)
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.Discard(3, targetPlayer),
            otherwise = Effects.Discard(1, targetPlayer),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7502ea2-7555-449e-baee-6ecef5573a3b.jpg?1562928792"
    }
}
