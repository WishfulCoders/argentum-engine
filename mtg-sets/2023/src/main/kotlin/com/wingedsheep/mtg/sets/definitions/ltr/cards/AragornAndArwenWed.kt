package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aragorn and Arwen, Wed
 * {4}{G}{W}
 * Legendary Creature — Human Elf Noble
 * 3/6
 *
 * Vigilance
 * Whenever Aragorn and Arwen enters or attacks, put a +1/+1 counter on each other creature you
 * control. You gain 1 life for each other creature you control.
 */
val AragornAndArwenWed = card("Aragorn and Arwen, Wed") {
    manaCost = "{4}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Elf Noble"
    power = 3
    toughness = 6
    oracleText = "Vigilance\n" +
        "Whenever Aragorn and Arwen enters or attacks, put a +1/+1 counter on each other creature you control. " +
        "You gain 1 life for each other creature you control."

    keywords(Keyword.VIGILANCE)

    val effectBody = Effects.ForEachInGroup(
        filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
    ) then
        Effects.GainLife(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
                excludeSelf = true
            ).count(),
            EffectTarget.Controller
        )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = effectBody
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = effectBody
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "287"
        artist = "Magali Villeneuve"
        flavorText = "Aragorn the King Elessar wedded Arwen Undómiel, and the tale of their long waiting and labors came to fulfillment."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7d4c97a-9319-4534-9a49-da000f41a02d.jpg?1715720374"
    }
}
