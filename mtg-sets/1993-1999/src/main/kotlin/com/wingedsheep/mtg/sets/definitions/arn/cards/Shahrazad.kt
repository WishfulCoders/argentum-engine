package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shahrazad
 * {W}{W}
 * Sorcery
 * Players play a Magic subgame, using their libraries as their decks. Each player who doesn't win
 * the subgame loses half their life, rounded up.
 *
 * The subgame (CR 729) is a whole game of its own: every player's library becomes their deck, the
 * main game waits with Shahrazad still on the stack, and when the subgame is over each player's
 * cards go back into their main-game library. Only then does Shahrazad finish resolving — the
 * players who didn't win (everyone, after a draw) each lose half their life, rounded up.
 */
val Shahrazad = card("Shahrazad") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Players play a Magic subgame, using their libraries as their decks. Each player " +
        "who doesn't win the subgame loses half their life, rounded up."

    spell {
        effect = Effects.PlaySubgame { nonWinners ->
            Effects.ForEachPlayer(nonWinners.asPlayers, Effects.LoseHalfLife(roundUp = true))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0014def3-4063-4929-ac51-76aef1bb2a68.jpg?1783948393"

        ruling(
            "2007-07-15",
            "At the end of a subgame, each player puts all cards they own that are in the subgame " +
                "into their library in the main game, then shuffles them. This includes cards in the " +
                "subgame's Exile zone."
        )
        ruling(
            "2004-10-04",
            "At the start of the sub-game both players draw their initial hand (usually 7 cards). " +
                "If one player has fewer cards than required, that player loses. If both have fewer " +
                "than required, both players lose."
        )
        ruling(
            "2004-10-04",
            "Events in a Shahrazad sub-game do not normally trigger abilities in the main game. And " +
                "continuous effects in the main game do not carry over into the sub-game."
        )
    }
}
