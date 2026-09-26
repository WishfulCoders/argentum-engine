package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Batterhorn
 * {4}{R}
 * Creature — Beast
 * 4/3
 *
 * When this creature enters, you may destroy target artifact.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * "You may" on a triggered ability is the builder's `optional = true`, which lowers to a
 * `Effects.May` around the declared effect — one consent gate, asked at resolution. The target is
 * still chosen when the ability goes on the stack, so declining wastes the choice (CR 603.3d).
 */
val Batterhorn = card("Batterhorn") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    oracleText = "When this creature enters, you may destroy target artifact."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val t = target(TargetFilter.Artifact)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Dave Kendall"
        flavorText = "Novice shopkeeps spend hours deciding how best to display their wares. Veterans focus on portability."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7b40f74-893f-4bfc-87b2-7f8df4c912d8.jpg?1783940357"
    }
}
