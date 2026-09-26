package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Dawn of a New Age
 * {1}{W}
 * Enchantment
 *
 * This enchantment enters with a hope counter on it for each creature you control.
 * At the beginning of your end step, remove a hope counter from this enchantment.
 * If you do, draw a card. Then if this enchantment has no hope counters on it,
 * sacrifice it and you gain 4 life.
 *
 * Rules notes:
 * - The end-step instruction "remove a counter. If you do, draw a card" only draws
 *   when the removal happens — i.e. when at least one hope counter is on the
 *   enchantment. Gated on `Conditions.SourceHasCounter(HOPE)`, which is equivalent
 *   to checking that the removal will succeed (there are no replacement effects on
 *   counter removal in standard LTR scope).
 * - The "Then if this enchantment has no hope counters" clause is a sequential
 *   instruction carried out during resolution (CR 608.2c), re-evaluating the
 *   counter count *after* the remove step — not an intervening-"if" (CR 603.4
 *   covers only the "if" immediately following a trigger condition). So it
 *   correctly fires after removing the last counter and also when the enchantment
 *   entered with zero counters.
 */
val DawnOfANewAge = card("Dawn of a New Age") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "This enchantment enters with a hope counter on it for each creature you control.\n" +
        "At the beginning of your end step, remove a hope counter from this enchantment. If you do, " +
        "draw a card. Then if this enchantment has no hope counters on it, sacrifice it and you gain 4 life."

    // Enters with a hope counter for each creature you control.
    replacementEffect(
        EntersWithDynamicCounters(
            counterType = CounterType.HOPE,
            count = DynamicAmounts.creaturesYouControl()
        )
    )

    // End step: remove → draw (gated on having a counter), then sac+gain4 if empty.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.If(
            condition = Conditions.SourceHasCounter(CounterType.HOPE),
            then = Effects.RemoveCounters(CounterType.HOPE, 1, EffectTarget.Self) then Effects.DrawCards(1)
        ) then
            Effects.If(
                condition = Conditions.Not(
                    Conditions.SourceHasCounter(CounterType.HOPE)
                ),
                then = Effects.SacrificeTarget(EffectTarget.Self) then Effects.GainLife(4)
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "Anato Finnstark"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb966ee6-bf1b-4bb6-9277-8de6f3918ae2.jpg?1686967678"
    }
}
