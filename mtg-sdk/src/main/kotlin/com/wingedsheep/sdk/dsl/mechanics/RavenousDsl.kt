package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Add Ravenous (CR 702.156) — "This permanent enters with X +1/+1 counters on it" and "When this
 * permanent enters, if X is 5 or more, draw a card."
 *
 * Both halves read [DynamicAmounts.castX] — the X the spell was cast with, which CR 107.3m hands to
 * a permanent's own enters replacement and enters trigger even though the permanent's X is 0. A
 * ravenous creature put onto the battlefield without being cast has no cast-time X, so it enters
 * with no counters and draws nothing. The draw is an intervening-if (CR 603.4); X can't change
 * between trigger and resolution, so the recheck is a formality.
 */
fun CardBuilder.ravenous() {
    keywordSet.add(Keyword.RAVENOUS)
    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.castX()))
    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.self.enters(),
            interveningIf = Conditions.CompareAmounts(DynamicAmounts.castX(), ComparisonOperator.GTE, 5),
            effect = Effects.DrawCards(1),
            descriptionOverride = "When this creature enters, if X is 5 or more, draw a card.",
        )
    )
}
