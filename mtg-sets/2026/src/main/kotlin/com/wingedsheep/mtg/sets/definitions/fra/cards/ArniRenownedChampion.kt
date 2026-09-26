package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val ArniRenownedChampion = card("Arni, Renowned Champion") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Berserker"
    oracleText = "Trample\nWhenever another creature you control enters, Arni gets +X/+0 until end of turn, " +
        "where X is that creature's power."
    power = 1
    toughness = 5

    keywords(Keyword.TRAMPLE)

    // X is read on resolution from the entered creature (EffectTarget.TriggeringEntity).
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.ModifyStats(
            DynamicAmounts.triggeringPower(),
            DynamicAmounts.fixed(0),
            EffectTarget.Self
        )
        description = "Arni gets +X/+0 until end of turn, where X is that creature's power."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Vincent Christiaens"
        flavorText = "There is no man with more bravado in all Bretagard, yet he has never exaggerated a single detail."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd8db649-1dba-457d-8327-e1f1da1aab36.jpg?1789557035"
        inBooster = false
    }
}
