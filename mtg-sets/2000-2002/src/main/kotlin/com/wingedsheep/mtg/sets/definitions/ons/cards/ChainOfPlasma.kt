package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.dsl.Targets

/**
 * Chain of Plasma
 * {1}{R}
 * Instant
 * Chain of Plasma deals 3 damage to any target. Then that player or that permanent's
 * controller may discard a card. If the player does, they may copy this spell and may
 * choose a new target for that copy.
 */
val ChainOfPlasma = card("Chain of Plasma") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Chain of Plasma deals 3 damage to any target. Then that player or that permanent's controller may discard a card. If the player does, they may copy this spell and may choose a new target for that copy."

    spell {
        val t = target(Targets.Any)
        effect = Effects.ChainCopy(
            action = Effects.DealDamage(3, t),
            target = t,
            offerTo = CopyRecipient.AFFECTED_PLAYER,
            copyTarget = Targets.Any,
            copyCost = Costs.pay.Discard()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Gary Ruddell"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f94aa774-9036-4016-8880-4bde2710cb90.jpg?1562954081"
    }
}
