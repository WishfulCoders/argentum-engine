package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Psychopath
 * {3}{R}
 * Creature — Goblin Mutant
 * 5/5
 * Whenever Goblin Psychopath attacks or blocks, flip a coin. If you lose
 * the flip, the next time it would deal combat damage this turn, it deals
 * that damage to you instead.
 */
val GoblinPsychopath = card("Goblin Psychopath") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Mutant"
    power = 5
    toughness = 5
    oracleText = "Whenever Goblin Psychopath attacks or blocks, flip a coin. If you lose the flip, the next time it would deal combat damage this turn, it deals that damage to you instead."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.FlipCoin(
            lostEffect = Effects.RedirectCombatDamageToController(EffectTarget.Self)
        )
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.FlipCoin(
            lostEffect = Effects.RedirectCombatDamageToController(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Pete Venters"
        flavorText = "The destruction he causes is nothing next to the chaos in his mind."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52287036-00f1-4b6d-8cd8-b8cbc70c5135.jpg?1562529064"
    }
}
