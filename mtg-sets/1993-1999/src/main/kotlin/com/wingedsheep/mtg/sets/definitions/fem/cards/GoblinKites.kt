package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Goblin Kites
 * {1}{R}
 * Enchantment
 * {R}: Target creature you control with toughness 2 or less gains flying until end of turn. Flip a
 * coin at the beginning of the next end step. If you lose the flip, sacrifice that creature.
 *
 * The flip is not made now — the ability schedules a delayed trigger for the next end step, and
 * the flip happens when *that* resolves. The creature keeps flying either way; only losing the
 * flip costs it.
 */
val GoblinKites = card("Goblin Kites") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "{R}: Target creature you control with toughness 2 or less gains flying until " +
        "end of turn. Flip a coin at the beginning of the next end step. If you lose the flip, " +
        "sacrifice that creature."

    activatedAbility {
        cost = Costs.Mana("{R}")
        val t = target(TargetFilter(GameObjectFilter.Creature.youControl().toughnessAtMost(2)))
        effect = Effects.GrantKeyword(Keyword.FLYING, t) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.FlipCoin(
                    wonEffect = null,
                    lostEffect = Effects.SacrificeTarget(t),
                )
            )
        description = "{R}: Target creature you control with toughness 2 or less gains flying until end of turn. Flip a coin at the beginning of the next end step. If you lose the flip, sacrifice that creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0a27ac3-2273-469a-92ba-3f4a3d55de6f.jpg?1783947894"
    }
}
