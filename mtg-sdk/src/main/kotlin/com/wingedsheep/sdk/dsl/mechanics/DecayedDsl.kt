package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Add Decayed (CR 702.147, Innistrad: Midnight Hunt) — keyword + static ability
 * + triggered ability.
 *
 * "This creature can't block, and when it attacks, sacrifice it at end of combat."
 *
 * The keyword is display-only (no separate Decayed handler exists); the behavior is
 * composed here from existing primitives: a [CantBlock] static ability on the source
 * for "can't block", plus an attack-triggered [CreateDelayedTriggerEffect] scheduled
 * for [Step.END_COMBAT] that sacrifices the source (mirroring Mardu Blazebringer's
 * "sacrifice it at end of combat" wiring).
 */
fun CardBuilder.decayed() {
    keywordSet.add(Keyword.DECAYED)
    staticAbilities.add(CantBlock(GroupFilter.source()))
    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.self.attacks(),
            effect = CreateDelayedTriggerEffect(
                step = Step.END_COMBAT,
                effect = Effects.SacrificeTarget(EffectTarget.Self)
            ),
            descriptionOverride = "Decayed (This creature can't block, and when it " +
                "attacks, sacrifice it at end of combat.)"
        )
    )
}
