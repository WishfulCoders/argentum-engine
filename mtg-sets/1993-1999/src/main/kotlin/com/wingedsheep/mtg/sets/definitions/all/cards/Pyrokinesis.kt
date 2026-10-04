package com.wingedsheep.mtg.sets.definitions.all.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pyrokinesis
 * {4}{R}{R}
 * Instant
 * You may exile a red card from your hand rather than pay this spell's mana cost.
 * Pyrokinesis deals 4 damage divided as you choose among any number of target creatures.
 *
 * The pitch cost is Force of Negation's [SelfAlternativeCost] shape — `{0}` plus exiling one red
 * card from hand — with no condition, so it is available on any turn. Cost increases still apply on
 * top of it (ruling 2016-06-08). The damage is [Effects.DividedDamage] over an unlimited creature
 * target requirement capped at four targets, since each target must be assigned at least 1 damage
 * (CR 601.2d); the division is fixed as the spell is cast, and an illegal target's share is simply
 * not dealt.
 */
val Pyrokinesis = card("Pyrokinesis") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "You may exile a red card from your hand rather than pay this spell's mana cost.\n" +
        "Pyrokinesis deals 4 damage divided as you choose among any number of target creatures."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.ExileCards(
                count = 1,
                filter = GameObjectFilter.Any.withColor(Color.RED),
                fromZone = CostZone.HAND,
            ),
        ),
    )

    spell {
        targets(
            TargetFilter(GameObjectFilter.Creature),
            unlimited = true,
            dynamicMaxCount = DynamicAmounts.fixed(4),
        )
        effect = Effects.DividedDamage(total = 4, minTargets = 1, maxTargets = 4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Ron Spencer"
        flavorText = "\"Anybody want some . . . *toast*?\"\n—Jaya Ballard, Task Mage"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db2a5e85-6cbc-43c1-9362-4056ad017ef0.jpg?1783947176"
        ruling("2016-06-08", "Each target must be assigned at least 1 damage.")
        ruling("2016-06-08", "You divide the damage as you cast Pyrokinesis, not as it resolves. If any of the targets become illegal, damage is dealt to the other targets as originally assigned. If all targets are illegal, Pyrokinesis doesn't resolve.")
        ruling("2016-06-08", "If another effect causes Pyrokinesis to cost more, you must pay that additional cost even if you pay its alternative cost.")
    }
}
