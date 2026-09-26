package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets

/**
 * Blackmail
 * {B}
 * Sorcery
 * Target player reveals three cards from their hand and you choose one of them.
 * That player discards that card.
 */
val Blackmail = card("Blackmail") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player reveals three cards from their hand and you choose one of them. That player discards that card."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            // 1. Gather all cards from target player's hand
            val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            // 2. Target player chooses 3 to reveal (auto-selects all if ≤3, skips if empty)
            val revealed = chooseExactly(3, from = hand, chooser = Chooser.TargetPlayer)
            // 3. Controller chooses 1 to discard
            val toDiscard = chooseExactly(1, from = revealed, chooser = Chooser.Controller)
            // 4. Move chosen card to target player's graveyard
            discard(toDiscard, player.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Christopher Moeller"
        flavorText = "\"Even the most virtuous person is only one secret away from being owned by the Cabal.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b40f6eb-e2a4-46d2-8822-b0f3dc508b73.jpg?1562931568"
    }
}
