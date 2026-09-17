package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Heartless Pillage
 * {2}{B}
 * Sorcery
 * Target opponent discards two cards.
 * Raid — If you attacked this turn, create a Treasure token.
 *
 * The Treasure comes whatever the opponent could discard (2020-08-07 ruling), so the raid check is
 * its own conditional after the discard.
 */
val HeartlessPillage = card("Heartless Pillage") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards.\n" +
        "Raid — If you attacked this turn, create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Discard(2, opponent).then(
            ConditionalEffect(
                condition = Conditions.YouAttackedThisTurn,
                effect = Effects.CreateTreasure(1),
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Sara Winters"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5e791ce-1380-4de0-b314-246ea6dfc3cc.jpg?1783935759"
        ruling("2020-08-07", "If you've attacked with a creature this turn, you'll get a Treasure even if the target opponent discards one or zero cards.")
        ruling("2020-08-07", "You create only one Treasure token if you attacked this turn, no matter how many creatures you attacked with beyond the first.")
    }
}
