package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cinder Elemental
 * {3}{R}
 * Creature — Elemental
 * 2/2
 *
 * {X}{R}, {T}, Sacrifice this creature: It deals X damage to any target.
 */
val CinderElemental = card("Cinder Elemental") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "{X}{R}, {T}, Sacrifice this creature: It deals X damage to any target."
    power = 2
    toughness = 2

    activatedAbility {
        val anyTarget = target(Targets.Any)
        cost = Costs.Composite(Costs.Mana("{X}{R}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), anyTarget)
        description = "{X}{R}, {T}, Sacrifice this creature: It deals X damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Greg Staples"
        flavorText = "Their rage can grow to such proportions that they explode in a cloud of fire."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80b39056-2ee8-4cfd-acbd-ba99f74e788d.jpg?1783945942"
    }
}
