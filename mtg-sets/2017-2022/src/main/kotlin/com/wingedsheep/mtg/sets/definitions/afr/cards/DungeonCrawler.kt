package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dungeon Crawler
 * {B}
 * Creature — Zombie
 * 2/1
 * This creature enters tapped.
 * Whenever you complete a dungeon, you may return this card from your graveyard to your hand.
 */
val DungeonCrawler = card("Dungeon Crawler") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 1
    oracleText = "This creature enters tapped.\n" +
        "Whenever you complete a dungeon, you may return this card from your graveyard to your hand."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.you.completesADungeon()
        triggerZone = Zone.GRAVEYARD
        optional = true
        effect = Effects.Move(target = EffectTarget.Self, destination = Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Svetlin Velinov"
        flavorText = "\"I think of it as giving them another chance to achieve their potential.\"\n—Nestus Wrasse, necromancer"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4138b66d-cf18-42a5-8522-241bd3969624.jpg?1783926498"
        ruling("2021-07-23", "Once you resolve the last room ability of a dungeon, that dungeon is now completed and is removed from the game.")
    }
}
