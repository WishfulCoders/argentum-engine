package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nadaar, Selfless Paladin
 * {2}{W}
 * Legendary Creature — Dragon Knight
 * 3/3
 * Vigilance
 * Whenever Nadaar enters or attacks, venture into the dungeon.
 * Other creatures you control get +1/+1 as long as you've completed a dungeon.
 *
 * "Enters or attacks" is two sibling triggered abilities with the same effect.
 */
val NadaarSelflessPaladin = card("Nadaar, Selfless Paladin") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dragon Knight"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever Nadaar enters or attacks, venture into the dungeon. (Enter the first room or advance to the next room.)\n" +
        "Other creatures you control get +1/+1 as long as you've completed a dungeon."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.VentureIntoTheDungeon()
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.VentureIntoTheDungeon()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 1,
                filter = GroupFilter.OtherCreaturesYouControl
            ),
            condition = Conditions.YouCompletedADungeon
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/878a0d8c-11c1-4c65-9051-78e036ac496f.jpg?1783926528"
        ruling("2021-07-23", "Nadaar doesn't need to have been on the battlefield when you completed a dungeon for its last ability to function.")
        ruling("2021-07-23", "The player venturing into the dungeon chooses which dungeon they will venture into. They may choose a dungeon that they have already completed this game.")
        ruling("2021-07-23", "If you somehow venture into the dungeon while a room's ability is on the stack, you will continue on in the dungeon. If you're already in the last room, complete that dungeon and start a new one.")
    }
}
