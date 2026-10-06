package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fire Covenant — Ice Age #289
 * {1}{B}{R} · Instant · Uncommon
 *
 * As an additional cost to cast this spell, pay X life.
 * Fire Covenant deals X damage divided as you choose among any number of target creatures.
 *
 * X is announced with the spell as the life paid for [Costs.additional.PayXLife] (the mana cost has
 * no `{X}`), and that announced X drives everything at cast time: at most X targets, so each can be
 * dealt at least 1 (CR 601.2d), and the division, announced with the targets, must total X. The
 * stack object carries the same X to resolution, where the division is honoured as announced — the
 * share of a target that became illegal is simply not dealt.
 */
val FireCovenant = card("Fire Covenant") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, pay X life.\n" +
        "Fire Covenant deals X damage divided as you choose among any number of target creatures."

    additionalCost(Costs.additional.PayXLife())

    spell {
        targets(
            TargetFilter(GameObjectFilter.Creature),
            minCount = 0,
            unlimited = true,
            dynamicMaxCount = DynamicAmounts.xValue(),
        )
        effect = Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "289"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a0139c2-ad86-4c71-ab6d-4840c37d5d20.jpg?1783947467"
        ruling("2007-09-16", "If X is 0, the number of targets must also be 0.")
        ruling("2007-09-16", "You must distribute at least 1 damage to each target.")
        ruling(
            "2007-09-16",
            "You divide the damage as you cast the spell. You can't redistribute the damage if any of " +
                "the target creatures becomes illegal before the spell resolves."
        )
    }
}
