package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Lumberknot
 * {2}{G}{G}
 * Creature — Treefolk
 * 1/1
 * Hexproof (This creature can't be the target of spells or abilities your opponents control.)
 * Whenever a creature dies, put a +1/+1 counter on this creature.
 */
val Lumberknot = card("Lumberknot") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    oracleText = "Hexproof (This creature can't be the target of spells or abilities your opponents control.)\n" +
        "Whenever a creature dies, put a +1/+1 counter on this creature."
    power = 1
    toughness = 1

    keywords(Keyword.HEXPROOF)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Jason A. Engle"
        flavorText = "Animated by geists fused in its oak, it hungers for more life to add to its core."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c86c84e-9bab-4a2c-b594-7f7b4b6bba88.jpg?1782714711"
    }
}
