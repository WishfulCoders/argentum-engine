package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Disruption Protocol
 * {U}{U}
 * Instant
 * As an additional cost to cast this spell, tap an untapped artifact you control or pay {1}.
 * Counter target spell.
 *
 * The additional cost is the "tap an untapped artifact or pay {1}" or-pay shape: the tap leg is an
 * ordinary tap-permanents cost, and declining it folds {1} into the spell's cost.
 */
val DisruptionProtocol = card("Disruption Protocol") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, tap an untapped artifact you control or pay {1}.\n" +
        "Counter target spell."

    additionalCost(
        Costs.additional.OrPay(
            cost = Costs.additional.TapPermanents(count = 1, filter = GameObjectFilter.Artifact),
            alternativeManaCost = "{1}",
        )
    )

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Pauline Voss"
        flavorText = "\"Let's just turn that off, shall we?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/5/053ab598-06a4-43ae-b9fd-c291bd05642c.jpg?1783923908"
    }
}
