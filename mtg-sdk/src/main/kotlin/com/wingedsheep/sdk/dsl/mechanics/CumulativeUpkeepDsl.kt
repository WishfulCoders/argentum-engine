package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Add Cumulative upkeep [cost] (CR 702.24) — the display keyword plus the triggered ability it
 * abbreviates:
 *
 * > **CR 702.24a** — "Cumulative upkeep [cost]" means "At the beginning of your upkeep, if this
 * > permanent is on the battlefield, put an age counter on this permanent. Then you may pay [cost]
 * > for each age counter on it. If you don't, sacrifice it." … either the entire set of costs is
 * > paid, or none of them is paid. Partial payments aren't allowed.
 *
 * Pure composition, one triggered ability per call:
 *  - `Triggers.you.beginningOf(UPKEEP)` with the intervening `if` [Conditions.SourceInZone]
 *    (BATTLEFIELD), so a permanent that left with the trigger on the stack does nothing;
 *  - [Effects.AddCounters] one [CounterType.AGE] counter on itself, then
 *  - [Effects.MayPay] of `cost × age counters` as one [Effects.PayDynamicMana] — counted at
 *    resolution, after the new counter, and all-or-nothing — whose `otherwise` sacrifices it.
 *    An unaffordable total skips the prompt straight to the sacrifice.
 *
 * Multiple calls add multiple abilities, which trigger separately and each count *all* the age
 * counters on the permanent when they resolve (CR 702.24b).
 *
 * **Scope: mana costs made of one kind of symbol** — `{N}` generic (Mystic Remora's `{1}`) or
 * repeated copies of one colored symbol (`{G}`, `{R}{R}`), which [Effects.PayDynamicMana] multiplies
 * exactly. A mixed cost (`{1}{U}`) or a non-mana cost ("pay 1 life", "sacrifice a creature") is
 * rejected at authoring time rather than approximated.
 */
fun CardBuilder.cumulativeUpkeep(cost: ManaCost) {
    val (perCounter, color) = cumulativeUpkeepUnit(cost)
    keywordSet.add(Keyword.CUMULATIVE_UPKEEP)
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.SourceInZone(Zone.BATTLEFIELD)
        effect = Effects.AddCounters(CounterType.AGE, 1, EffectTarget.Self) then
            Effects.MayPay(
                cost = Effects.PayDynamicMana(
                    amount = DynamicAmounts.countersOn(EffectTarget.Self, CounterType.AGE) * perCounter,
                    color = color,
                ),
                then = Effects.Nothing,
                otherwise = Effects.SacrificeTarget(EffectTarget.Self),
                descriptionOverride = "Pay cumulative upkeep ($cost for each age counter), or sacrifice this permanent?",
            )
        description = "Cumulative upkeep $cost (At the beginning of your upkeep, put an age counter on " +
            "this permanent, then sacrifice it unless you pay its upkeep cost for each age counter on it.)"
    }
}

/**
 * The per-age-counter payment of a cumulative upkeep [cost]: how many symbols, and of which color
 * (`null` = generic). Only single-kind mana costs are expressible as one dynamic payment.
 */
private fun cumulativeUpkeepUnit(cost: ManaCost): Pair<Int, Color?> {
    val symbols = cost.symbols
    require(symbols.isNotEmpty()) { "Cumulative upkeep needs a cost" }
    if (symbols.all { it is ManaSymbol.Generic }) {
        val n = symbols.sumOf { (it as ManaSymbol.Generic).amount }
        require(n > 0) { "Cumulative upkeep {0} is not a cost" }
        return n to null
    }
    val colors = symbols.map { (it as? ManaSymbol.Colored)?.color }
    require(colors.all { it != null } && colors.toSet().size == 1) {
        "cumulativeUpkeep supports a generic cost or repeated copies of one colored symbol, not $cost"
    }
    return symbols.size to colors.first()
}
