package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Packsong Pup
 * {1}{G}
 * Creature — Wolf
 * 1/1
 * At the beginning of combat on your turn, if you control another Wolf or Werewolf, put a +1/+1
 * counter on this creature.
 * When this creature dies, you gain life equal to its power.
 */
val PacksongPup = card("Packsong Pup") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    oracleText = "At the beginning of combat on your turn, if you control another Wolf or Werewolf, put a +1/+1 counter on this creature.\nWhen this creature dies, you gain life equal to its power."
    power = 1
    toughness = 1
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControl(
            filter = GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.WOLF, Subtype.WEREWOLF)),
            excludeSelf = true
        )
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = EffectTarget.Self)
    }
    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.GainLife(DynamicAmounts.sourcePower())
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "April Prime"
        flavorText = "A wolf pup is always dangerous, because a wolf pup is never alone."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d43d9686-a5e4-413b-8a34-3430788dd1b9.jpg?1782703044"
    }
}
