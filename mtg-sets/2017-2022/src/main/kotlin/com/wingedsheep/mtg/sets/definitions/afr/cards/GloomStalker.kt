package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Gloom Stalker
 * {2}{W}
 * Creature — Dwarf Ranger
 * 2/3
 * As long as you've completed a dungeon, this creature has double strike.
 */
val GloomStalker = card("Gloom Stalker") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Ranger"
    power = 2
    toughness = 3
    oracleText = "As long as you've completed a dungeon, this creature has double strike."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.Self),
            condition = Conditions.YouCompletedADungeon
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Tomas Duchek"
        flavorText = "\"I'll make sure you never mistake a roper for a stalagmite again.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/8/388722ad-dcc6-41b9-afd8-a93be91b30e0.jpg?1783926532"
        ruling("2021-07-23", "Gloom Stalker will have double strike even if it wasn't on the battlefield as you completed a dungeon.")
    }
}
