package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Théoden, King of Rohan
 * {1}{R}{W}
 * Legendary Creature — Human Noble
 * 2/3
 *
 * Whenever Théoden or another Human you control enters, target creature gains
 * double strike until end of turn.
 */
val TheodenKingOfRohan = card("Théoden, King of Rohan") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Noble"
    power = 2
    toughness = 3
    oracleText = "Whenever Théoden or another Human you control enters, target creature gains double strike until end of turn."

    triggeredAbility {
        val creature = target(TargetFilter(GameObjectFilter.Creature))
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl().withSubtype("Human")).enters()
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Kieran Yanner"
        flavorText = "\"Dark have been my dreams of late, but I feel new-awakened. I only fear that already you have come too late, Gandalf.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6dcd1ca-4943-46e4-bb5d-c14949e21e23.jpg?1686970093"
    }
}
