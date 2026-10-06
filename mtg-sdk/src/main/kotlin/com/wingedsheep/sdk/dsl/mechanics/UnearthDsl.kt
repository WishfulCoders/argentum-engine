package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.MoveTrackedBattlefieldObjectEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Unearth ability (CR 702.84a):
 *
 *   "[Cost]: Return this card from your graveyard to the battlefield. It gains haste. Exile it at
 *    the beginning of the next end step. If it would leave the battlefield, exile it instead of
 *    putting it anywhere else. Activate only as a sorcery."
 *
 * Like [embalm] and [renew], unearth needs no engine subsystem of its own — it is an ordinary
 * graveyard-activated ability composed of existing primitives:
 *  - the mana [cost] alone (unlike embalm, the card is *not* exiled as part of the cost — it stays
 *    in the graveyard until the ability resolves, so an opponent can respond by removing it, in
 *    which case the ability does nothing),
 *  - `activateFromZone = Zone.GRAVEYARD` + `timing = TimingRule.SorcerySpeed`,
 *  - a return to the battlefield of the card itself, then — gated on the card actually having
 *    arrived, per the 2022-10-14 ruling that an unearth whose card left the graveyard does nothing:
 *    - a permanent haste grant ("it gains haste", tied to this object; it ends when the object
 *      leaves the battlefield),
 *    - a delayed trigger at the next end step running a [MoveTrackedBattlefieldObjectEffect] to
 *      exile — the delayed-trigger executor snapshots the battlefield entry timestamp, so a blinked
 *      permanent (a new object, CR 400.7) is not exiled by the old trigger, and
 *    - [Effects.GrantExileOnLeave], the "if it would leave the battlefield, exile it instead"
 *      replacement. It is a component on the object, not an ability, so per the unearth rulings it
 *      still applies after the permanent loses all its abilities, and it is stripped when the
 *      object leaves the battlefield.
 *
 * The id is keyword-scoped (like [embalmAbility]) so the ability needs no surrounding card scope.
 */
fun unearthAbility(cost: ManaCost): ActivatedAbility = ActivatedAbility(
    id = AbilityId("unearth_$cost"),
    cost = AbilityCost.Atom(CostAtom.Mana(cost)),
    effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self) then
        Effects.If(
            Conditions.SourceInZone(Zone.BATTLEFIELD),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self, Duration.Permanent) then
                Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = MoveTrackedBattlefieldObjectEffect(EffectTarget.Self, Zone.EXILE),
                ) then
                Effects.GrantExileOnLeave(EffectTarget.Self)
        ),
    timing = TimingRule.SorcerySpeed,
    activateFromZone = Zone.GRAVEYARD,
    descriptionOverride = "Unearth $cost ($cost: Return this card from your graveyard to the " +
        "battlefield. It gains haste. Exile it at the beginning of the next end step or if it would " +
        "leave the battlefield. Unearth only as a sorcery.)",
)

/**
 * Add Unearth [cost] (CR 702.84, Shards of Alara) to a card.
 *
 * ```kotlin
 * unearth("{W}")
 * ```
 *
 * See [unearthAbility] for how the ability is composed.
 */
fun CardBuilder.unearth(cost: String) {
    activatedAbilities.add(unearthAbility(ManaCost.parse(cost)))
    keywordSet.add(Keyword.UNEARTH)
}
