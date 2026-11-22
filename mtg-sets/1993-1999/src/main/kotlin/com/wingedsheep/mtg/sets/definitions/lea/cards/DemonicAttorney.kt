package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Demonic Attorney
 * {1}{B}{B}
 * Sorcery
 * Remove this card from your deck before playing if you're not playing for ante.
 * Each player antes the top card of their library.
 *
 * Only legal in a game played for ante (CR 407.3, enforced by deck validation). Each player
 * antes their own library top — only a card's owner can ante it (CR 407.4) — in APNAP order.
 */
val DemonicAttorney = card("Demonic Attorney") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Remove this card from your deck before playing if you're not playing for ante.\n" +
        "Each player antes the top card of their library."

    spell {
        effect = Effects.ForEachPlayer(Player.ActivePlayerFirst, Patterns.Library.anteTop())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "102"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd891fc6-d9d6-494e-ae65-8bea8f44b575.jpg?1783948696"
    }
}
