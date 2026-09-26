package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Ring Goes South
 * {3}{G}
 * Sorcery
 *
 * The Ring tempts you. Then reveal cards from the top of your library until you reveal X land
 * cards, where X is the number of legendary creatures you control. Put those land cards onto
 * the battlefield tapped and the rest on the bottom of your library in a random order.
 *
 * Composes the Ring tempt with GatherUntilMatch (X = legendary creatures you control) +
 * two filtered MoveCollections: lands → battlefield tapped, the rest → bottom of library at
 * random.
 */
val TheRingGoesSouth = card("The Ring Goes South") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "The Ring tempts you. Then reveal cards from the top of your library until you " +
        "reveal X land cards, where X is the number of legendary creatures you control. Put those " +
        "land cards onto the battlefield tapped and the rest on the bottom of your library in a " +
        "random order."

    spell {
        effect = Effects.Pipeline {
            run(Effects.TheRingTemptsYou())
            val (_, allRevealed) = gatherUntilMatch(
                GameObjectFilter.Land,
                player = Player.You,
                count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.legendary()).count()
            )
            reveal(allRevealed)
            move(
                allRevealed,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped),
                filter = GameObjectFilter.Land
            )
            move(
                allRevealed,
                CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Bottom),
                order = CardOrder.Random,
                filter = GameObjectFilter.Nonland
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Wangjie Li"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c8a4c7d-527c-49ea-a115-a9e747c0fd03.jpg?1686969579"
    }
}
