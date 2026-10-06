package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wheel of Fortune
 * {2}{R}
 * Sorcery
 *
 * Each player discards their hand, then draws seven cards.
 *
 * Two passes over [Player.Each] in APNAP order: every player discards their hand first, and only
 * then does every player draw seven. "Then" sequences the whole discard before the whole draw,
 * so no player's draw happens while another player still holds their old hand.
 */
val WheelOfFortune = card("Wheel of Fortune") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Each player discards their hand, then draws seven cards."

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effects = listOf(Patterns.Hand.discardHand()),
        ) then Effects.ForEachPlayer(
            players = Player.Each,
            effects = listOf(Effects.DrawCards(7)),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67b369c4-faa8-45c8-a1b9-98f228b69682.jpg?1783948679"
    }
}
