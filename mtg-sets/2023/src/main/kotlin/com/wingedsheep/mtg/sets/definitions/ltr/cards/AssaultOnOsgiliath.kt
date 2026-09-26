package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Assault on Osgiliath
 * {X}{R}{R}{R}
 * Sorcery
 *
 * Amass Orcs X, then Goblins and Orcs you control gain double strike and haste until end of turn.
 */
val AssaultOnOsgiliath = card("Assault on Osgiliath") {
    manaCost = "{X}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Amass Orcs X, then Goblins and Orcs you control gain double strike and haste until end of turn. " +
        "(To amass Orcs X, put X +1/+1 counters on an Army you control. It's also an Orc. If you don't control " +
        "an Army, create a 0/0 black Orc Army creature token first.)"

    spell {
        effect = Effects.Amass(DynamicAmounts.xValue(), "Orc") then
            Effects.ForEachInGroup(
                filter = GroupFilter(
                    com.wingedsheep.sdk.scripting.GameObjectFilter.Creature
                        .withAnySubtype("Goblin", "Orc").youControl()
                ),
                effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.IterationEntity, Duration.EndOfTurn) then
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "285"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2549b70a-6bd0-4c9b-96b7-4ab775a30cd3.jpg?1719684269"
    }
}
