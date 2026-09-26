package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Smelt-Ward Minotaur
 * {2}{R}
 * Creature — Minotaur Warrior
 * 2/3
 * Whenever you cast an instant or sorcery spell, target creature an opponent controls can't block this turn.
 */
val SmeltWardMinotaur = card("Smelt-Ward Minotaur") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Warrior"
    oracleText = "Whenever you cast an instant or sorcery spell, target creature an opponent controls can't block this turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantBlock(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Wisnu Tan"
        flavorText = "\"Don't arrest him—enlist him!\"\n—Commander Yaszen"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f851d74e-90ad-417f-8372-8437d2d68b0d.jpg?1783934157"
    }
}
