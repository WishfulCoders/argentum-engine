package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Death Match
 * {3}{B}
 * Enchantment
 * Whenever a creature enters, that creature's controller may have target creature
 * of their choice get -3/-3 until end of turn.
 */
val DeathMatch = card("Death Match") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature enters, that creature's controller may have target creature of their choice get -3/-3 until end of turn."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).enters()
        controlledByTriggeringEntityController = true
        val t = target(TargetFilter.Creature)
        effect = Effects.May(
            Effects.ModifyStats(
                power = -3,
                toughness = -3,
                target = t,
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Mark Brill"
        flavorText = "When one enters the arena, another feels the pain."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/143e9057-267a-4c78-b72a-4f8018b627a8.jpg?1562899865"
    }
}
