package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Valley Mightcaller
 * {G}
 * Creature — Frog Warrior
 * 1/1
 *
 * Trample
 * Whenever another Frog, Rabbit, Raccoon, or Squirrel you control enters,
 * put a +1/+1 counter on this creature.
 */
val ValleyMightcaller = card("Valley Mightcaller") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Frog Warrior"
    power = 1
    toughness = 1
    oracleText = "Trample\nWhenever another Frog, Rabbit, Raccoon, or Squirrel you control enters, put a +1/+1 counter on this creature."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature
                    .withAnyOfSubtypes(
                        listOf(
                            Subtype("Frog"),
                            Subtype("Rabbit"),
                            Subtype("Raccoon"),
                            Subtype("Squirrel")
                        )
                    )
                    .youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Matt Stewart"
        flavorText = "\"There is no path to the Root Maze's center. Bard, sing a song while I create one.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/2/7256451f-0122-452a-88e8-0fb0f6bea3f3.jpg?1721639437"
    }
}
