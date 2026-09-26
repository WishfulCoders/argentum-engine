package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Palantír of Orthanc
 * {3}
 * Legendary Artifact
 *
 * At the beginning of your end step, put an influence counter on Palantír of Orthanc and scry 2.
 * Then target opponent may have you draw a card. If that player doesn't, you mill X cards, where X
 * is the number of influence counters on Palantír of Orthanc, and that player loses life equal to
 * the total mana value of those cards.
 *
 * The "target opponent may" is routed to the targeted opponent (not the controller) via
 * [Effects.May]'s `decisionMaker`. On yes → the controller draws a card; on no → the else-branch
 * mills X cards into the "milled" collection and the opponent loses life equal to their total
 * mana value (DynamicAmount.ManaValueSumOfCollection / DynamicAmounts.manaValueSumOf).
 */
val PalantirOfOrthanc = card("Palantír of Orthanc") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact"
    oracleText = "At the beginning of your end step, put an influence counter on Palantír of Orthanc and scry 2. " +
        "Then target opponent may have you draw a card. If that player doesn't, you mill X cards, where X is " +
        "the number of influence counters on Palantír of Orthanc, and that player loses life equal to the total " +
        "mana value of those cards."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val opponent = target(Targets.Opponent)

        val influenceCount = DynamicAmounts.countersOnSelf(CounterType.INFLUENCE)

        effect = Effects.AddCounters(CounterType.INFLUENCE, 1, EffectTarget.Self) then
            Patterns.Library.scry(2) then
            Effects.May(
                effect = Effects.DrawCards(1, EffectTarget.Controller),
                descriptionOverride = "Have Palantír of Orthanc's controller draw a card?",
                decisionMaker = opponent,
                otherwise = Patterns.Library.mill(influenceCount, EffectTarget.Controller) then
                    Effects.LoseLife(DynamicAmounts.manaValueSumOf(Patterns.Library.milled), opponent)
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "247"
        artist = "Tatiana Veryayskaya"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6efb6a69-562c-4d95-858d-b067444cfd7e.jpg?1686970247"
    }
}
