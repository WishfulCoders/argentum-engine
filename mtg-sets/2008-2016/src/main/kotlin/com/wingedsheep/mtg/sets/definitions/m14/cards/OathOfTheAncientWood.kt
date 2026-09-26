package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Oath of the Ancient Wood
 * {2}{G}
 * Enchantment
 * Whenever this enchantment or another enchantment you control enters, you may put a +1/+1 counter on target creature.
 */
val OathOfTheAncientWood = card("Oath of the Ancient Wood") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever this enchantment or another enchantment you control enters, you may put a +1/+1 counter on target creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val recipient = target(TargetFilter.Creature)
        optional = true
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, recipient)
        description = "Whenever this enchantment or another enchantment you control enters, " +
            "you may put a +1/+1 counter on target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Dan Murayama Scott"
        flavorText = "\"Some gaze upon the forest and see trees. I see true power.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bc42032-8727-4f78-b369-ba103d965b73.jpg"
    }
}
