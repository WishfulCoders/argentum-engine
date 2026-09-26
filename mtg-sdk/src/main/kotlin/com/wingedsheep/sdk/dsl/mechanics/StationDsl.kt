package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Add the Station keyword ability (CR 702.184a): "Tap another untapped creature you control:
 * Put a number of charge counters on this permanent equal to the tapped creature's power.
 * Activate only as a sorcery."
 *
 * This ability is fully fixed by the rules — every station card (Spacecraft and Planet alike)
 * has exactly this one, so the helper takes no arguments. The charge amount is
 * [DynamicAmount.StationCharge], which carries the CR 702.184c characteristic-substitution
 * (Tapestry Warden's toughness-for-power) without leaking it onto unrelated power reads.
 *
 * What the card gains at each charge threshold (the `{N+}` station symbols, CR 721.2a) is
 * *not* part of this helper — author those as `staticAbility { }` rows (Spacecraft granting
 * keywords / a creature type) or threshold-gated activated abilities, each gated on
 * [Conditions.SourceCounterCountAtLeast] with [CounterType.CHARGE], because the payload differs
 * per card.
 *
 * Example (Wedgelight Rammer):
 * ```
 * station()
 * staticAbility {
 *     condition = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 9)
 *     ability = GrantCardType("CREATURE", GroupFilter.source())
 * }
 * ```
 */
fun CardBuilder.station() {
    activatedAbilities.add(
        ActivatedAbility(
            id = AbilityId.next(),
            cost = AbilityCost.Atom(CostAtom.TapPermanents(
                count = 1,
                filter = GameObjectFilter.Creature,
                excludeSelf = true
            )),
            effect = Effects.AddDynamicCounters(
                counterType = CounterType.CHARGE,
                amount = DynamicAmount.StationCharge,
                target = EffectTarget.Self
            ),
            timing = TimingRule.SorcerySpeed
        )
    )
}
