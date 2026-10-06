package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fading N (CR 702.32) as pure data — the two abilities every fading permanent has and none of
 * them prints as separate lines.
 *
 * CR 702.32a: "Fading N" means "This permanent enters with N fade counters on it" and "At the
 * beginning of your upkeep, remove a fade counter from this permanent. If you can't, sacrifice
 * the permanent."
 *
 *  1. **"This permanent enters with N fade counters on it."** A replacement effect (CR 614.1c),
 *     and the only half that needs `N` — hence [entersWithCounters], a factory. The engine reads
 *     `N` back off the card's [KeywordAbility.Numeric] at entry, exactly as it does for
 *     [Vanishing].
 *  2. **"At the beginning of your upkeep, remove a fade counter from this permanent. If you
 *     can't, sacrifice the permanent."** [upkeepCountdown] — *one* ability with an "otherwise"
 *     branch, granted by the engine to every permanent whose *projected* keywords include
 *     [Keyword.FADING] (so a permanent that gains fading fades, and one that loses all abilities
 *     stops).
 *
 * How it differs from its neighbour [Vanishing] (CR 702.63), and why it is not a parameterised
 * copy of it:
 *
 *  - **No intervening-`if`, and no "last counter" trigger.** Vanishing sacrifices the moment the
 *    last time counter leaves; fading sacrifices only at an upkeep that finds *no* counter to
 *    remove. A Fading 1 permanent therefore survives its first upkeep (1 → 0) and is sacrificed
 *    at the second. Stripping every fade counter off-turn (Vampire Hexmage) does not sacrifice
 *    it on the spot either — it goes at its controller's next upkeep.
 *  - **"If you can't"** is decided when the ability resolves, not when it triggers: the trigger
 *    always fires, and on resolution it removes a counter if there is one and sacrifices
 *    otherwise. Spending fade counters in response (Parallax Wave's "Remove a fade counter: …")
 *    with the trigger on the stack can therefore turn a removal into a sacrifice. The check is
 *    "does this permanent have a fade counter" — nothing in the rules prevents removing one that
 *    is there, so "can remove" and "has one" coincide.
 *  - **A distinct counter type** ([CounterType.FADE]) — time counters on a fading permanent are
 *    irrelevant to it, and vice versa.
 *
 * Multiple instances: CR 702.32 has no special rule, so each instance is its own pair of
 * abilities — the printed N's are summed at entry ([printedCount]) like vanishing's.
 */
object Fading {

    /** "this permanent has a fade counter on it" — whether the upkeep removal can happen. */
    private val hasFadeCounter = EntityMatches(
        EffectTarget.Self,
        GameObjectFilter.Any.copy(
            statePredicates = listOf(StatePredicate.HasCounter(CounterType.FADE))
        )
    )

    /**
     * CR 702.32a — "At the beginning of your upkeep, remove a fade counter from this permanent.
     * If you can't, sacrifice the permanent."
     */
    val upkeepCountdown: TriggeredAbility = TriggeredAbility(
        id = AbilityId("fading_countdown"),
        trigger = EventPattern.StepEvent(Step.UPKEEP, Player.You),
        binding = TriggerBinding.SELF,
        activeZones = setOf(Zone.BATTLEFIELD),
        effect = GatedEffect(
            gate = Gate.WhenCondition(hasFadeCounter),
            then = RemoveCountersEffect(CounterType.FADE, DynamicAmount.Fixed(1), EffectTarget.Self),
            otherwise = SacrificeTargetEffect(EffectTarget.Self),
            descriptionOverride = "Remove a fade counter from this permanent. If you can't, " +
                "sacrifice it.",
        ),
        descriptionOverride = "At the beginning of your upkeep, remove a fade counter from " +
            "this permanent. If you can't, sacrifice it.",
    )

    /**
     * CR 702.32a — "This permanent enters with N fade counters on it."
     *
     * `selfOnly` because the replacement applies to the fading permanent itself, never to other
     * things entering under its controller.
     */
    fun entersWithCounters(n: Int): EntersWithCounters = EntersWithCounters(
        counterType = CounterType.FADE,
        count = n,
        selfOnly = true,
    )

    /**
     * The N on a card's printed `Fading N`, or `null` if it has none. Multiple printed instances
     * each set up their own counters, so they are summed.
     */
    fun printedCount(cardDef: CardDefinition): Int? {
        val total = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Numeric>()
            .filter { it.keyword == Keyword.FADING }
            .sumOf { it.n }
        return total.takeIf { it > 0 }
    }
}
