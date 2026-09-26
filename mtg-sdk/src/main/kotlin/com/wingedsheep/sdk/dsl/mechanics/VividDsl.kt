package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Add the Vivid ability-word tag for display and attach an ETB triggered ability
 * whose effect scales with the number of distinct colors among permanents you
 * control (Lorwyn Eclipsed, effect-scaling half).
 *
 * The factory receives a [DynamicAmount] representing that colour count so the
 * effect composes with it naturally, e.g.:
 *
 * ```kotlin
 * vividEtb { colors ->
 *     CompositeEffect(listOf(
 *         GatherUntilMatchEffect(filter = GameObjectFilter.Permanent, count = colors, ...),
 *         ...
 *     ))
 * }
 * ```
 *
 * This is a DSL convenience — it emits an ordinary [TriggeredAbility] and a
 * [Keyword.VIVID] tag, so the resulting [com.wingedsheep.sdk.model.CardDefinition]
 * serializes/deserializes exactly like a hand-written equivalent.
 */
fun CardBuilder.vividEtb(effectFactory: (DynamicAmount) -> Effect) {
    keywordSet.add(Keyword.VIVID)
    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.self.enters(),
            effect = effectFactory(DynamicAmounts.colorsAmongPermanents())
        )
    )
}

/**
 * Add the Vivid ability-word tag for display and the "This spell costs {1} less
 * to cast for each colour among permanents you control" cost reduction
 * (Lorwyn Eclipsed, cost-reduction half).
 *
 * Like [vividEtb], this emits normal serializable data — a [Keyword.VIVID] tag
 * plus a [ModifySpellCost] self-cast static ability sourced from
 * [CostReductionSource.ColorsAmongPermanentsYouControl].
 */
fun CardBuilder.vividCostReduction() {
    keywordSet.add(Keyword.VIVID)
    staticAbilities.add(
        ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.ColorsAmongPermanentsYouControl,
            ),
        ),
    )
}
