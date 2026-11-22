package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val OverseerOfVault76 = card("Overseer of Vault 76") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Creature — Human Advisor"
    power = 3
    toughness = 3
    oracleText = "First Contact — Whenever Overseer of Vault 76 or another creature you control with power 3 or less enters, put a quest counter on Overseer of Vault 76.\nAt the beginning of combat on your turn, you may remove three quest counters from among permanents you control. When you do, put a +1/+1 counter on each creature you control and they gain vigilance until end of turn."

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Any.sourceItself() or GameObjectFilter.Creature.youControl().powerAtMost(3)
        ).enters()
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val quests = DynamicAmounts.battlefield(Player.You).totalCounters(CounterType.QUEST)
        effect = Effects.Pipeline(
            descriptionOverride = "You may remove three quest counters from among permanents you control. " +
                "When you do, put a +1/+1 counter on each creature you control and they gain vigilance until end of turn.",
        ) {
            // PayOrSuffer preserves the pipeline across its optional allocation prompt. Only a
            // complete payment decreases this total by three and creates the reflexive trigger.
            val before = storeNumber(quests)
            run(Effects.PayOrSuffer(
                cost = Costs.pay.RemoveCounters(count = 3, counterType = CounterType.QUEST),
                suffer = Effects.Nothing,
                consequenceDescription = "do nothing",
            ))
            run(Effects.If(
                Conditions.CompareAmounts(before.amount - quests, ComparisonOperator.EQ, 3),
                Effects.ReflexiveTrigger(
                    action = Effects.Nothing,
                    optional = false,
                    reflexiveEffect = Effects.ForEachInGroup(
                        GroupFilter(GameObjectFilter.Creature.youControl()),
                        Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity) then
                            Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity),
                    ),
                    descriptionOverride = "Put a +1/+1 counter on each creature you control and they gain vigilance until end of turn",
                ),
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35848917-d658-4c3b-b675-4f0185f76dc4.jpg?1783912418"
        ruling("2024-03-08", "If Overseer of Vault 76 and one or more other creatures with power 3 or less enter the battlefield under your control at the same time, Overseer of Vault 76’s first ability will trigger for each of them.")
        ruling("2024-03-08", "Overseer of Vault 76’s last ability lets you choose to remove exactly three quest counters from among permanents you control when it resolves. You can’t remove more quest counters to multiply the effect.")
    }
}
