package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Scavenge ability (CR 702.97a, Return to Ravnica):
 *
 *   "[Cost], Exile this card from your graveyard: Put a number of +1/+1 counters equal to the
 *    power of the card you exiled on target creature. Activate only as a sorcery."
 *
 * Like [embalmAbility] and [unearthAbility], scavenge is an ordinary graveyard-activated ability
 * composed of existing primitives:
 *  - the mana cost plus [AbilityCost.ExileSelf] — the card leaves as part of the cost, so the
 *    ability can't be answered by removing it from the graveyard;
 *  - `activateFromZone = Zone.GRAVEYARD` + `timing = TimingRule.SorcerySpeed`;
 *  - [Effects.AddDynamicCounters] of the source's power — its power as it last existed in the
 *    graveyard, which the activation records before the exile cost moves it (a characteristic-
 *    defining P/T such as Tarmogoyf's still counts the card itself).
 *
 * [cost] `null` is "the scavenge cost is equal to its mana cost" — [AbilityCost.OwnManaCost],
 * priced per card when activated. That is the shape a whole-graveyard grant needs (Young
 * Deathclaws, Varolz, the Scar-Striped): one ability given to every creature card, each paying its
 * own mana cost, with any {X} as 0 (CR 107.3h). A card with no mana cost can't pay it (CR 118.6).
 *
 * ```kotlin
 * staticAbility {
 *     ability = GrantActivatedAbility(
 *         scavengeAbility(),
 *         GroupFilter(GameObjectFilter.Creature.ownedByYou()),
 *         recipientZone = Zone.GRAVEYARD,
 *     )
 * }
 * ```
 *
 * The id is keyword-scoped (like [embalmAbility]) so the ability needs no surrounding card scope.
 */
fun scavengeAbility(cost: ManaCost? = null): ActivatedAbility {
    val builder = ActivatedAbilityBuilder()
    val creature = builder.target(TargetFilter.Creature)
    val manaCost = cost?.let { AbilityCost.Atom(CostAtom.Mana(it)) } ?: AbilityCost.OwnManaCost
    val costText = cost?.toString() ?: "its mana cost"
    return ActivatedAbility(
        id = AbilityId("scavenge_${cost ?: "own"}"),
        cost = AbilityCost.Composite(listOf(manaCost, AbilityCost.ExileSelf)),
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), creature),
        targetRequirements = builder.targetRequirements,
        timing = TimingRule.SorcerySpeed,
        activateFromZone = Zone.GRAVEYARD,
        descriptionOverride = "Scavenge — $costText (Exile this card from your graveyard and pay " +
            "$costText: Put a number of +1/+1 counters equal to this card's power on target " +
            "creature. Scavenge only as a sorcery.)",
    )
}

