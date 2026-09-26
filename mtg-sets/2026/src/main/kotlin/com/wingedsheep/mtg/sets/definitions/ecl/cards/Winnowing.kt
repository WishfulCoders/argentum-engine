package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.withSubtypeInEachStoredGroup
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Winnowing
 * {4}{W}{W}
 * Sorcery
 *
 * Convoke
 * For each player, you choose a creature that player controls. Then each player
 * sacrifices all other creatures they control that don't share a creature type
 * with the chosen creature they control.
 */
val Winnowing = card("Winnowing") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "For each player, you choose a creature that player controls. Then each player sacrifices all other creatures they control that don't share a creature type with the chosen creature they control."

    keywords(Keyword.CONVOKE)

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.ActivePlayerFirst,
            Effects.Pipeline {
                val playerCreatures = gather(
                    CardSource.ControlledPermanents(
                        player = Player.You,
                        filter = GameObjectFilter.Creature
                    )
                )
                val (chosen, rest) = chooseExactlySplit(
                    1,
                    from = playerCreatures,
                    chooser = Chooser.SourceController,
                    prompt = "Choose a creature this player controls",
                    useTargetingUI = true
                )
                val chosenSubtypes = gatherSubtypes(chosen)
                val (_, noShareTypes) = filterSplit(
                    rest,
                    GameObjectFilter.Creature.withSubtypeInEachStoredGroup(chosenSubtypes)
                )
                sacrifice(noShareTypes)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f943a7d8-9550-427e-8c45-ef834329d345.jpg?1767659128"
    }
}
