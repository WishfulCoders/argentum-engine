package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.dsl.Targets

/**
 * The Torment of Gollum
 * {3}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards that card.
 * Amass Orcs 2.
 */
val TheTormentOfGollum = card("The Torment of Gollum") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player " +
        "discards that card.\n" +
        "Amass Orcs 2. (Put two +1/+1 counters on an Army you control. It's also an Orc. If you don't " +
        "control an Army, create a 0/0 black Orc Army creature token first.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard"
            )
            discard(toDiscard, opponent.asPlayer)
            run(Effects.Amass(2, "Orc"))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        flavorText = "In Mordor, all that he knew was forced from him."
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba2c8c25-1fa9-4cc4-a378-5eccc25bacf0.jpg?1686968743"
    }
}
