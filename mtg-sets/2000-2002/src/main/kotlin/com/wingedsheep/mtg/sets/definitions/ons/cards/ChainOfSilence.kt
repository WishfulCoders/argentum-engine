package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Chain of Silence
 * {1}{W}
 * Instant
 * Prevent all damage target creature would deal this turn. That creature's controller
 * may sacrifice a land of their choice. If the player does, they may copy this spell
 * and may choose a new target for that copy.
 */
val ChainOfSilence = card("Chain of Silence") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent all damage target creature would deal this turn. That creature's controller may sacrifice a land of their choice. If the player does, they may copy this spell and may choose a new target for that copy."

    spell {
        val creature = TargetObject(filter = TargetFilter.Creature)
        val t = target(creature)
        effect = Effects.ChainCopy(
            action = Effects.PreventAllDamageDealtBy(t),
            target = t,
            offerTo = CopyRecipient.TARGET_CONTROLLER,
            copyTarget = creature,
            copyCost = Costs.pay.Sacrifice(GameObjectFilter.Land)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a60ac8e-11eb-433f-86f9-8e593b38c617.jpg?1562931386"
    }
}
