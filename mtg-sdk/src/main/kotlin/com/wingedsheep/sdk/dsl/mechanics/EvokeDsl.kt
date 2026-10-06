package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Add Evoke with a cost that has a non-mana part (CR 702.74).
 *
 * The Modern Horizons 2 Incarnations' "Evoke—Exile a white card from your hand." is
 * `evokeWith(Costs.additional.ExileCards(1, GameObjectFilter.Any.withColor(Color.WHITE), CostZone.HAND))`.
 * [mana] is the mana part of the same evoke cost, `{0}` when the cost is non-mana only.
 *
 * A mana-only evoke keeps the plain `evoke = "{cost}"` property. Either way the result is one
 * [KeywordAbility.Evoke], so the engine treats both shapes identically: the evoke cast is
 * offered next to the normal cast, its costs are paid as one alternative cost (CR 118.9,
 * 601.2b/601.2f–h), and the permanent is sacrificed when it enters (CR 702.74a).
 */
fun CardBuilder.evokeWith(vararg additionalCosts: AdditionalCost, mana: String = "{0}") {
    require(additionalCosts.isNotEmpty()) { "evokeWith needs a non-mana cost; use `evoke = \"{cost}\"` for mana-only evoke" }
    require(evoke == null) { "A card has one evoke cost: set either `evoke` or `evokeWith`, not both" }
    keywordAbilityList.add(KeywordAbility.Evoke(ManaCost.parse(mana), additionalCosts.toList()))
}
