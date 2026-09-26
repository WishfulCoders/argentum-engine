package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets

/**
 * Intimidation Tactics
 * {B}
 * Sorcery
 * Target opponent reveals their hand. You choose an artifact or creature card from it. Exile that card.
 * Cycling {3}
 */
val IntimidationTactics = card("Intimidation Tactics") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose an artifact or creature card from it. Exile that card.\n" +
        "Cycling {3} ({3}, Discard this card: Draw a card.)"

    spell {
        val t = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(t))
            val hand = gather(CardSource.FromZone(Zone.HAND, t.asPlayer))
            val toExile = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
                prompt = "Choose an artifact or creature card to exile"
            )
            exile(toExile, t.asPlayer)
        }
    }

    keywordAbility(KeywordAbility.cycling("{3}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Cristi Balanescu"
        flavorText = "\"This is still a negotiation. We haven't broken anything... yet.\"\n—Far Fortune"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b4e6022-44d2-4dfe-8f7a-51581e298f23.jpg?1738356324"
    }
}
