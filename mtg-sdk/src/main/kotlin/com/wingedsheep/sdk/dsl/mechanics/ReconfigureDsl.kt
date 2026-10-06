package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.effects.UnattachEquipmentEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Add Reconfigure [cost] (CR 702.151, Kamigawa: Neon Dynasty) to an Equipment creature card.
 *
 * ```kotlin
 * reconfigure("{2}")
 * ```
 *
 * Reconfigure represents two activated abilities (CR 702.151a), both composed of existing
 * primitives:
 *
 *  - "[cost]: Attach this permanent to another target creature you control. Activate only as a
 *    sorcery." — an [AttachEquipmentEffect] on [TargetFilter.OtherCreatureYouControl] (an Equipment
 *    can never be attached to itself, CR 301.5c).
 *  - "[cost]: Unattach this permanent. Activate only if this permanent is attached to a creature and
 *    only as a sorcery." — an [UnattachEquipmentEffect] on itself, gated by an
 *    [ActivationRestriction.OnlyIfCondition] on the source being attached to a creature.
 *
 * Neither is an equip ability (`isEquipAbility = false`): per the Neon Dynasty release notes,
 * reconfigure doesn't count for Fighter Class, Leonin Shikari and other equip-keyed rules.
 *
 * The keyword itself ([Keyword.RECONFIGURE]) is load-bearing: it lets this Equipment equip while it
 * is a creature (CR 301.5c), and attaching it to a creature — by this ability or any other effect —
 * makes it stop being a creature until it becomes unattached (CR 702.151b). The engine records that
 * on the attach and projects it in layer 4; nothing about it lives in these two abilities.
 */
fun CardBuilder.reconfigure(cost: String) {
    val manaCost = ManaCost.parse(cost)
    keywordSet.add(Keyword.RECONFIGURE)
    activatedAbilities.add(reconfigureAttachAbility(manaCost))
    activatedAbilities.add(reconfigureUnattachAbility(manaCost))
}

/** Reconfigure's attach half (CR 702.151a). */
fun reconfigureAttachAbility(cost: ManaCost): ActivatedAbility {
    val label = "another creature you control"
    return ActivatedAbility(
        cost = AbilityCost.Atom(CostAtom.Mana(cost)),
        effect = AttachEquipmentEffect(EffectTarget.BoundVariable(label)),
        targetRequirements = listOf(TargetObject(filter = TargetFilter.OtherCreatureYouControl, id = label)),
        timing = TimingRule.SorcerySpeed,
        descriptionOverride = "Reconfigure $cost: Attach to target creature you control",
    )
}

/** Reconfigure's unattach half (CR 702.151a). */
fun reconfigureUnattachAbility(cost: ManaCost): ActivatedAbility = ActivatedAbility(
    cost = AbilityCost.Atom(CostAtom.Mana(cost)),
    effect = UnattachEquipmentEffect(EffectTarget.Self),
    timing = TimingRule.SorcerySpeed,
    restrictions = listOf(
        ActivationRestriction.OnlyIfCondition(
            Conditions.SourceMatches(GameObjectFilter.Any.attachedTo(GameObjectFilter.Creature))
        )
    ),
    descriptionOverride = "Reconfigure $cost: Unattach",
)
