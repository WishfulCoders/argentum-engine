package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Beastmaster Ascension
 * {2}{G}
 * Enchantment
 *
 * Whenever a creature you control attacks, you may put a quest counter on Beastmaster Ascension.
 * As long as Beastmaster Ascension has seven or more quest counters on it, creatures you
 * control get +5/+5.
 */
val BeastmasterAscension = card("Beastmaster Ascension") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control attacks, you may put a quest counter on Beastmaster Ascension.\n" +
        "As long as Beastmaster Ascension has seven or more quest counters on it, creatures you control get +5/+5."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        optional = true
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 5,
                toughnessBonus = 5,
                filter = GroupFilter.AllCreaturesYouControl
            ),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.countersOnSelf(CounterType.QUEST),
                ComparisonOperator.GTE,
                7
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Justin Gerard"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bccf8a9-d135-431e-bcc0-15ab29f8f8cb.jpg?1721428757"

        ruling("2009-10-01", "If you attack with multiple creatures, Beastmaster Ascension's first ability triggers multiple times.")
    }
}
