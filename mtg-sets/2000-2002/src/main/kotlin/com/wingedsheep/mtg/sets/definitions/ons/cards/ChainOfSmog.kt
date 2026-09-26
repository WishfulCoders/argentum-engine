package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.dsl.Targets

/**
 * Chain of Smog
 * {1}{B}
 * Sorcery
 * Target player discards two cards. That player may copy this spell and may choose
 * a new target for that copy.
 */
val ChainOfSmog = card("Chain of Smog") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards. That player may copy this spell and may choose a new target for that copy."

    spell {
        val t = target(Targets.Player)
        effect = Effects.ChainCopy(
            action = Patterns.Hand.discardCards(2, t),
            target = t,
            offerTo = CopyRecipient.TARGET_PLAYER,
            copyTarget = Targets.Player
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bfe64f9-8b03-41f6-a47b-fade397ad9d1.jpg?1562920423"
    }
}
