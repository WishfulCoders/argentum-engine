package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Jeskai Ascendancy
 * {U}{R}{W}
 * Enchantment
 * Whenever you cast a noncreature spell, creatures you control get +1/+1 until end of turn.
 * Untap those creatures.
 * Whenever you cast a noncreature spell, you may draw a card. If you do, discard a card.
 */
val JeskaiAscendancy = card("Jeskai Ascendancy") {
    manaCost = "{U}{R}{W}"
    colorIdentity = "WUR"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a noncreature spell, creatures you control get +1/+1 until end of turn. Untap those creatures.\nWhenever you cast a noncreature spell, you may draw a card. If you do, discard a card."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        ) then
            Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                Effects.Untap(EffectTarget.IterationEntity)
            )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca9c2522-5606-4bbd-863d-f0ab0a612b4e.jpg?1562793510"
    }
}
