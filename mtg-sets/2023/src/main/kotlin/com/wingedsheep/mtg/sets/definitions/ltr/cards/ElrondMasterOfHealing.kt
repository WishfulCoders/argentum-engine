package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elrond, Master of Healing
 * {2}{G}{U}
 * Legendary Creature — Elf Noble
 * 4/4
 *
 * Whenever you scry, put a +1/+1 counter on each of up to X target creatures,
 * where X is the number of cards looked at while scrying this way.
 * Whenever a creature you control with a +1/+1 counter on it becomes the
 * target of a spell or ability an opponent controls, you may draw a card.
 */
val ElrondMasterOfHealing = card("Elrond, Master of Healing") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elf Noble"
    power = 4
    toughness = 4
    oracleText = "Whenever you scry, put a +1/+1 counter on each of up to X target creatures, " +
        "where X is the number of cards looked at while scrying this way.\n" +
        "Whenever a creature you control with a +1/+1 counter on it becomes the target of a spell " +
        "or ability an opponent controls, you may draw a card."

    triggeredAbility {
        trigger = Triggers.you.scries()
        targets(TargetFilter.Creature, optional = true, dynamicMaxCount = DynamicAmounts.triggerScryCount())
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl()).becomesTarget(byOpponent = true)
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        artist = "Wangjie Li"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d26ffb2c-f7a5-4a4f-9b99-c8de9dfd49da.jpg?1686969733"
    }
}
