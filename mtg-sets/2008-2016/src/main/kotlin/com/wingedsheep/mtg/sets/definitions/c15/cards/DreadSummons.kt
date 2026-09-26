package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dread Summons
 * {X}{B}{B}
 * Sorcery
 *
 * Each player mills X cards. For each creature card put into a graveyard this way, you
 * create a tapped 2/2 black Zombie creature token.
 *
 * Pipeline (Saruman of Many Colors pattern): [Patterns.Library.mill] aimed at
 * [Player.Each] fans out across all players (turn order) and surfaces every milled card
 * in the "milled" collection; [SelectFromCollectionEffect] with [SelectionMode.All]
 * auto-filters the creature cards into "milledCreatures"; [ForEachInCollectionEffect]
 * then creates one tapped Zombie per creature card milled this way. X is read at
 * resolution time via [DynamicAmount.XValue].
 */
val DreadSummons = card("Dread Summons") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each player mills X cards. For each creature card put into a graveyard this way, you create a tapped 2/2 black Zombie creature token."

    spell {
        effect = Effects.Pipeline {
            val milled = mill(DynamicAmounts.xValue(), Player.Each)
            val milledCreatures = selectAll(from = milled, filter = GameObjectFilter.Creature)
            run(Effects.ForEachInCollection(
                milledCreatures,
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                    tapped = true,
                    imageUri = "https://cards.scryfall.io/normal/front/8/f/8ffaa67e-32fa-4843-8858-feed2ebb40df.jpg?1783938021"
                )
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Izzy"
        flavorText = "\"Did you have a nice nap?\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2c20cb1-3e3d-4fea-b617-bd6d796c8d10.jpg?1783938113"
    }
}
