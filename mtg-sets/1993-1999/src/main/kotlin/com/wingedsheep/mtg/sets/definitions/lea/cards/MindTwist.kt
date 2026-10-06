package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Mind Twist
 * {X}{B}
 * Sorcery
 * Target player discards X cards at random.
 *
 * Rag Man's gather → random select → discard pipeline with the count read from X. A random selection
 * larger than the hand takes the whole hand.
 */
val MindTwist = card("Mind Twist") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards X cards at random."

    spell {
        val victim = target(Targets.Player)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(zone = Zone.HAND, player = victim.asPlayer))
            val discarded = chooseRandom(DynamicAmounts.xValue(), from = hand)
            discard(discarded, victim.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Julie Baroh"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eee9e106-a248-49d2-b8c8-6bbcd56ce739.jpg?1783948693"
    }
}
