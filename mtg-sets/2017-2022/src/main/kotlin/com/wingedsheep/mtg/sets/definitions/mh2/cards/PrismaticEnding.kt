package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Prismatic Ending — Modern Horizons 2 #25
 * {X}{W} · Sorcery
 *
 * Converge — Exile target nonland permanent if its mana value is less than or equal to the number
 * of colors of mana spent to cast this spell.
 *
 * The mana value is *not* a targeting restriction (printed ruling): any nonland permanent is a
 * legal target, and the comparison is made only as the spell resolves. So the target filter is
 * plain [TargetFilter.NonlandPermanent] and the exile sits behind a resolution-time
 * [Conditions.CompareAmounts] of the target's mana value against
 * [DynamicAmounts.colorsOfManaSpent] — the resolving spell's own payment record, the same amount
 * Archaic's Agony reads. A copy, or a free cast, spent no colors, so only mana value 0 qualifies.
 */
val PrismaticEnding = card("Prismatic Ending") {
    manaCost = "{X}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Converge — Exile target nonland permanent if its mana value is less than or equal to the number of colors of mana spent to cast this spell."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaValueOf(permanent),
                ComparisonOperator.LTE,
                DynamicAmounts.colorsOfManaSpent(),
            ),
            then = Effects.Exile(permanent),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "John Stanko"
        flavorText = "Right before his demise, he experienced a shattering revelation."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/825969b9-3c70-4fca-8cab-696e9ca7cdb2.jpg?1783926888"
        ruling("2021-06-18", "The mana value is not a targeting condition. It is checked only as the spell resolves.")
        ruling("2021-06-18", "If you cast a spell with converge without spending any mana to cast it (perhaps because an effect allowed you to cast it without paying its mana cost), then the number of colors spent to cast it will be zero.")
    }
}
