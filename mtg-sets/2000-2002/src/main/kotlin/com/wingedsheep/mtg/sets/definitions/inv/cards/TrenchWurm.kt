package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Trench Wurm
 * {3}{B}
 * Creature — Wurm
 * 3/3
 * {2}{R}, {T}: Destroy target nonbasic land.
 */
val TrenchWurm = card("Trench Wurm") {
    manaCost = "{3}{B}"
    colorIdentity = "BR"
    typeLine = "Creature — Wurm"
    power = 3
    toughness = 3
    oracleText = "{2}{R}, {T}: Destroy target nonbasic land."

    activatedAbility {
        val permanent = target(
            TargetFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsLand,
                        CardPredicate.Not(CardPredicate.IsBasicLand),
                    )
                )
            ),
        )
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        effect = Effects.Destroy(permanent)
        description = "{2}{R}, {T}: Destroy target nonbasic land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Wayne England"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b076f85-d1bf-491a-af9d-f35b8e1bd163.jpg?1562900334"
    }
}
