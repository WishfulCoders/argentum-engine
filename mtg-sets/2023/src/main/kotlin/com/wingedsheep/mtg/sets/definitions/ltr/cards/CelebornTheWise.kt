package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Celeborn the Wise
 * {3}{G}
 * Legendary Creature — Elf Noble
 * 3/3
 *
 * Whenever you attack with one or more Elves, scry 1.
 * Whenever you scry, Celeborn gets +1/+1 until end of turn for each card looked at while scrying this way.
 */
val CelebornTheWise = card("Celeborn the Wise") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Noble"
    power = 3
    toughness = 3
    oracleText = "Whenever you attack with one or more Elves, scry 1.\n" +
        "Whenever you scry, Celeborn gets +1/+1 until end of turn for each card looked at while scrying this way."

    triggeredAbility {
        trigger = Triggers.you.attacks(GameObjectFilter.Creature.withSubtype(Subtype.ELF))
        effect = Patterns.Library.scry(1)
    }

    triggeredAbility {
        trigger = Triggers.you.scries()
        effect = Effects.ModifyStats(
            power = DynamicAmounts.triggerScryCount(),
            toughness = DynamicAmounts.triggerScryCount(),
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Wangjie Li"
        flavorText = "His eyes were keen as lances in starlight, and yet profound, the wells of deep memory."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2daf449a-5f3e-44fd-968e-55d8517ae797.jpg?1686969259"
    }
}
