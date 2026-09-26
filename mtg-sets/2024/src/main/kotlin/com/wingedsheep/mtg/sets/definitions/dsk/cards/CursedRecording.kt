package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Cursed Recording
 * {2}{R}{R}
 * Artifact
 *
 * Whenever you cast an instant or sorcery spell, put a time counter on this artifact. Then if there
 * are seven or more time counters on it, remove those counters and it deals 20 damage to you.
 * {T}: When you next cast an instant or sorcery spell this turn, copy that spell. You may choose new
 *      targets for the copy.
 *
 * The "Then if ..." clause is a resolution-time check ([Effects.If] +
 * [Conditions.SourceCounterCountAtLeast]), not an intervening-if on the trigger. Because the count
 * is checked after every single counter is added, it can only ever reach exactly seven, so removing
 * seven time counters is faithful to "remove those counters". The activated ability arms a
 * one-shot delayed copy of the next instant/sorcery spell via [Effects.CopyNextSpellCast]
 * (which lets the controller choose new targets for the copy).
 */
val CursedRecording = card("Cursed Recording") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "Whenever you cast an instant or sorcery spell, put a time counter on this " +
        "artifact. Then if there are seven or more time counters on it, remove those counters and " +
        "it deals 20 damage to you.\n{T}: When you next cast an instant or sorcery spell this turn, " +
        "copy that spell. You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.AddCounters(CounterType.TIME, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.TIME, 7),
                then = Effects.RemoveCounters(CounterType.TIME, 7, EffectTarget.Self) then
                    Effects.DealDamage(20, EffectTarget.Controller, damageSource = EffectTarget.Self),
            )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.CopyNextSpellCast(1)
        description = "{T}: When you next cast an instant or sorcery spell this turn, copy that " +
            "spell. You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Kim Sokol"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d13a247c-c941-488a-b13b-bffb1f1f368a.jpg?1726286337"
    }
}
