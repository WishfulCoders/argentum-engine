package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Kingpin of Crime
 * {1}{W}{B}
 * Legendary Creature — Human Villain
 * 1/5
 *
 * Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life and
 *   you gain that much life.)
 * Whenever you attack, you may pay 2 life. If you do, until end of turn, creatures you control
 *   with toughness greater than their power assign combat damage equal to their toughness rather
 *   than their power.
 *
 *  - **Extort** (CR 702.101) has no `Keyword` of its own here, and it does not need one: it *is*
 *    exactly "whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1 life
 *    and you gain that much life" — `Triggers.you.casts()` over [Patterns.Mechanic.extort], an
 *    [Effects.MayPay] of the hybrid `{W/B}` over [Effects.DrainLife]`(1)`, shared with Blind
 *    Obedience. The hybrid parses and pays as a hybrid symbol (either color, or two
 *    generic-equivalent sources of either), and `DrainLife(1)` is the single-event "each opponent
 *    loses 1, you gain that much" shape — so a multiplayer drain gains the total, not 1 per
 *    opponent.
 *
 *  - **The attack ability** is a [Gate.MayPay] over [PayLifeEffect]`(2)` — a cost the engine checks
 *    for affordability before prompting, so a controller who cannot pay is never offered the choice.
 *    `GatedEffectExecutor.canAfford` is `life >= amount`, so paying at exactly 2 life *is* offered:
 *    CR 118.3/118.3b bar only a payment you lack the life for, so paying down to 0 is legal, and it
 *    is the separate state-based action (CR 704.5a) that ends the game afterwards.
 *    Fires on `Triggers.you.attacks()` (declare attackers, once per combat,
 *    regardless of whether the Kingpin himself attacks — he is a 1/5 that would rather stay home).
 *
 *  - **"creatures you control with toughness greater than their power"** stays a *dynamic* set for
 *    the rest of the turn, not a snapshot taken when the trigger resolves. The printed effect
 *    changes no object's characteristics and no object's controller, so by CR 611.2c it modifies
 *    the rules of the game and "can affect objects that weren't affected when that continuous
 *    effect began": a creature that only becomes toughness-heavy later — a combat trick after
 *    blockers, a counter — must pick it up, and one whose power outgrows its toughness must lose it.
 *
 *    So this is not a grant of anything to the creatures. It is the *durational form of Bedrock
 *    Tortoise's printed sentence*, and it is written as exactly that: the Kingpin grants
 *    **himself** [AssignDamageEqualToToughness]`(AllCreaturesYouControl, onlyWhenToughnessGreater…)`
 *    until end of turn. `CombatDamageUtils` reads that ability — printed or granted — at the point
 *    of use, against the final projected power and toughness, so the set is re-decided per creature
 *    per damage step, and both the first-strike and regular steps honor it.
 *
 *    Two shapes are deliberately *not* used. [Effects.ForEachInGroup] + `GrantKeyword` snapshots the
 *    group at resolution, which is right for an ability grant ("creatures you control gain trample")
 *    and wrong for a rules modification. A floating group-flag effect would keep membership dynamic
 *    across projections but resolve its filter in layer 6, before layer 7 has applied — so it still
 *    could not see a toughness pumped after the trigger resolved, which is the whole point here.
 *
 *    Residual: the grant is anchored to the Kingpin, so it stops applying if he leaves the
 *    battlefield mid-turn, where the printed effect would outlive him.
 */
val TheKingpinOfCrime = card("The Kingpin of Crime") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Villain"
    power = 1
    toughness = 5
    oracleText = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each opponent " +
        "loses 1 life and you gain that much life.)\n" +
        "Whenever you attack, you may pay 2 life. If you do, until end of turn, creatures you " +
        "control with toughness greater than their power assign combat damage equal to their " +
        "toughness rather than their power."

    // Extort — whenever you cast a spell, you may pay {W/B}. If you do, each opponent loses 1
    // life and you gain that much life.
    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Patterns.Mechanic.extort()
        description = "Extort (Whenever you cast a spell, you may pay {W/B}. If you do, each " +
            "opponent loses 1 life and you gain that much life.)"
    }

    // Whenever you attack, you may pay 2 life. If you do, until end of turn, creatures you
    // control with toughness greater than their power assign combat damage equal to their
    // toughness rather than their power.
    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayLife(2),
            then = Effects.GrantStaticAbility(
                AssignDamageEqualToToughness(
                    filter = GroupFilter.AllCreaturesYouControl,
                    onlyWhenToughnessGreaterThanPower = true,
                ),
                EffectTarget.Self,
                Duration.EndOfTurn,
            ),
            descriptionOverride = "You may pay 2 life. If you do, until end of turn, creatures " +
                "you control with toughness greater than their power assign combat damage equal " +
                "to their toughness rather than their power.",
        )
        description = "Whenever you attack, you may pay 2 life. If you do, until end of turn, " +
            "creatures you control with toughness greater than their power assign combat damage " +
            "equal to their toughness rather than their power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Steve Morris"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/495c08ea-5502-4bfe-aa15-fa85556755ae.jpg?1783902900"
    }
}
