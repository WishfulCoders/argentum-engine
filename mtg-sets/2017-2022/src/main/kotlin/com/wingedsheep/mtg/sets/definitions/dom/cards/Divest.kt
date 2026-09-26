package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets

/**
 * Divest
 * {B}
 * Sorcery
 * Target player reveals their hand. You choose an artifact or creature card from it.
 * That player discards that card.
 */
val Divest = card("Divest") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player reveals their hand. You choose an artifact or creature card from it. That player discards that card."

    spell {
        val t = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(t))
            val hand = gather(CardSource.FromZone(Zone.HAND, t.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
                prompt = "Choose an artifact or creature card to discard"
            )
            discard(toDiscard, t.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Jesper Ejsing"
        flavorText = "\"Flittersprites collect unusually valuable things: coins of fallen empires, baby teeth, and memories of treasured names.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6619810-7933-4844-8556-2a575ed7f18a.jpg?1562742646"
    }
}
