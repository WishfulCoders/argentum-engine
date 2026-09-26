package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Decree of Silence
 * {6}{U}{U}
 * Enchantment
 * Whenever an opponent casts a spell, counter that spell and put a depletion counter
 * on Decree of Silence. If there are three or more depletion counters on Decree of
 * Silence, sacrifice it.
 * Cycling {4}{U}{U}
 * When you cycle Decree of Silence, you may counter target spell.
 */
val DecreeOfSilence = card("Decree of Silence") {
    manaCost = "{6}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent casts a spell, counter that spell and put a depletion counter on Decree of Silence. If there are three or more depletion counters on Decree of Silence, sacrifice it.\nCycling {4}{U}{U}\nWhen you cycle Decree of Silence, you may counter target spell."

    // Ability 1: Whenever an opponent casts a spell, counter that spell + depletion counter + sacrifice check
    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        effect = Effects.CounterTriggeringSpell() then
            Effects.AddCounters(CounterType.DEPLETION, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.countersOnSelf(CounterType.DEPLETION),
                    ComparisonOperator.GTE,
                    3
                ),
                then = SacrificeSelfEffect
            )
    }

    // Cycling {4}{U}{U}
    keywordAbility(KeywordAbility.cycling("{4}{U}{U}"))

    // When you cycle this card, you may counter target spell.
    triggeredAbility {
        trigger = Triggers.self.isCycled()
        val t = target(TargetFilter.SpellOnStack)
        effect = Effects.May(Effects.CounterSpell())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2fc46e2-5e19-4999-a4cd-1e84697066c1.jpg?1562536891"
        ruling("2022-12-08", "When you cycle this card, first the cycling ability goes on the stack, then the triggered ability goes on the stack on top of it. The triggered ability will resolve before you draw a card from the cycling ability.")
        ruling("2022-12-08", "The cycling ability and the triggered ability are separate. If the triggered ability doesn't resolve (because, for example, it has been countered, or all of its targets have become illegal), the cycling ability will still resolve, and you'll draw a card.")
        ruling("2022-12-08", "You can cycle this card even if there are no legal targets for the triggered ability.")
    }
}
