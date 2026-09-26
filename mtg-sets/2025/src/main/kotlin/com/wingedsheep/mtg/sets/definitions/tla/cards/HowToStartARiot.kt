package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Targets

/**
 * How to Start a Riot
 * {2}{R}
 * Instant — Lesson
 *
 * Target creature gains menace until end of turn. (It can't be blocked except by two or more creatures.)
 * Creatures target player controls get +2/+0 until end of turn.
 */
val HowToStartARiot = card("How to Start a Riot") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant — Lesson"
    oracleText = "Target creature gains menace until end of turn. " +
        "(It can't be blocked except by two or more creatures.)\n" +
        "Creatures target player controls get +2/+0 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        val player = target(Targets.Player)
        effect = Effects.GrantKeyword(Keyword.MENACE, creature) then
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)),
                effect = Effects.ModifyStats(2, 0, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Robin Olausson"
        flavorText = "\"Hey! RIOT!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23b3bf1e-ea85-47f5-8473-4d16615f68d7.jpg?1764120961"
    }
}
