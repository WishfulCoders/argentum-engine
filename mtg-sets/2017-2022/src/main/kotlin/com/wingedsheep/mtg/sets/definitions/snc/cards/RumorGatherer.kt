package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect

/**
 * Rumor Gatherer
 * {1}{W}{W}
 * Creature — Elf Wizard
 * 2/1
 *
 * Alliance — Whenever another creature you control enters, scry 1. If this is the second time
 * this ability has resolved this turn, draw a card instead.
 *
 * Same resolution-count pattern as Venom Connoisseur / Teething Wurmlet: bump the source's
 * per-turn resolution counter, then branch on [Conditions.SourceAbilityResolvedNTimes] (== 2) —
 * the second resolution draws instead of scrying; every other resolution scries 1.
 */
val RumorGatherer = card("Rumor Gatherer") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elf Wizard"
    power = 2
    toughness = 1
    oracleText = "Alliance — Whenever another creature you control enters, scry 1. If this is the " +
        "second time this ability has resolved this turn, draw a card instead."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.DrawCards(1),
                otherwise = Effects.Scry(1),
            )
        description = "Alliance — Whenever another creature you control enters, scry 1. If this is " +
            "the second time this ability has resolved this turn, draw a card instead."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Simon Dominic"
        flavorText = "\"It's delightful, the things their owners reveal when they think no one is listening.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8b87807-cac5-4e33-a8ed-a9ced0cd83a1.jpg?1783923152"
    }
}
