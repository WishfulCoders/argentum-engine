package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val AradeshTheFounder = card("Aradesh, the Founder") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 1
    toughness = 4
    oracleText = "Enlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)\nWhenever a creature you control attacks, if it enlisted a creature this combat, the creature that attacked gains double strike until end of turn. If that creature's power is 4 or greater, draw a card."
    staticAbility { ability = Enlist }
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.enlistmentsThisCombat(EffectTarget.TriggeringEntity), ComparisonOperator.GTE, 1,
        )
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.TriggeringEntity) then Effects.If(
            Conditions.CompareAmounts(DynamicAmounts.powerOf(EffectTarget.TriggeringEntity), ComparisonOperator.GTE, 4),
            Effects.DrawCards(1),
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6701dae4-348e-40f6-9db4-d6f0039be831.jpg?1783912420"
        ruling("2024-03-08", "The attacking player chooses whether to tap a creature for an enlist ability immediately after they tap the creatures that they have chosen to attack with. You can’t choose to enlist a creature later.")
        ruling("2024-03-08", "To enlist a creature, that creature must be untapped, it must not be attacking (even if it has vigilance), and it must have haste or have been under that attacking player’s control since the beginning of their current turn.")
        ruling("2024-03-08", "When a player taps a creature for an attacking creature’s enlist ability, that attacking creature gets +X/+0 until end of turn, where X is the tapped creature’s power. This is a triggered ability that goes on the stack immediately after attackers have been declared in the declare attackers step.")
        ruling("2024-03-08", "You may tap only one creature for an enlist ability of an attacking creature, and a single creature can’t be tapped for more than one enlist ability.")
        ruling("2024-03-08", "Aradesh’s last ability has an intervening if clause. If a creature that attacks hasn’t enlisted a creature this combat, the ability won’t trigger at all for that creature.")
        ruling("2024-03-08", "It doesn’t matter what happens to the enlisted creature after Aradesh’s last ability triggers. As long as the creature that attacked enlisted a creature this combat, when Aradesh’s last ability resolves, the creature that attacked will still get double strike, and if that creature’s power is 4 or greater, you’ll still draw a card.")
    }
}
