package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vicious Offering
 * {1}{B}
 * Instant
 * Kicker—Sacrifice a creature.
 * Target creature gets -2/-2 until end of turn. If this spell was kicked,
 * that creature gets -5/-5 until end of turn instead.
 */
val ViciousOffering = card("Vicious Offering") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Kicker—Sacrifice a creature. (You may sacrifice a creature in addition to any other costs as you cast this spell.)\nTarget creature gets -2/-2 until end of turn. If this spell was kicked, that creature gets -5/-5 until end of turn instead."

    keywordAbility(KeywordAbility.kicker(Costs.additional.SacrificePermanent(filter = GameObjectFilter.Creature)))

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.ModifyStats(-5, -5, t, Duration.EndOfTurn),
            otherwise = Effects.ModifyStats(-2, -2, t, Duration.EndOfTurn)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaed7828-57c1-4ad3-a91b-209c66f0876b.jpg?1562741039"
    }
}
