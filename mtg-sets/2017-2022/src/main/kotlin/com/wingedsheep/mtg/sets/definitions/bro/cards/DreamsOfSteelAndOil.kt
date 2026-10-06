package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Dreams of Steel and Oil
 * {B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose an artifact or creature card from it, then
 * choose an artifact or creature card from their graveyard. Exile the chosen cards.
 *
 * Both choices are made before anything moves, then the chosen cards are exiled together.
 * Each choice is mandatory when a legal card exists (2022-10-14 rulings), and an empty hand
 * pool doesn't stop the graveyard choice.
 */
val DreamsOfSteelAndOil = card("Dreams of Steel and Oil") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose an artifact or creature card from it, " +
        "then choose an artifact or creature card from their graveyard. Exile the chosen cards."

    spell {
        val opponent = target(Targets.Opponent)

        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val handPool = gather(
                CardSource.FromZone(
                    zone = Zone.HAND,
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
                )
            )
            val fromHand = chooseExactly(
                1,
                from = handPool,
                chooser = Chooser.Controller,
                prompt = "Choose an artifact or creature card from their hand to exile"
            )
            val graveyardPool = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
                )
            )
            val fromGraveyard = chooseExactly(
                1,
                from = graveyardPool,
                chooser = Chooser.Controller,
                prompt = "Choose an artifact or creature card from their graveyard to exile"
            )
            exile(fromHand, opponent.asPlayer)
            exile(fromGraveyard, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Jeremy Wilson"
        flavorText = "Mishra's nightmares of Phyrexia rang with screams of agony and groans of twisted " +
            "metal. The sounds stayed with him long after he awoke."
        imageUri =
            "https://cards.scryfall.io/normal/front/2/6/261ac92e-c61a-4c11-aa6a-9ae1cb703e5c.jpg?1783920092"
        ruling(
            "2022-10-14",
            "You must choose an artifact or creature card from their hand if they reveal one. Similarly, " +
                "you must choose an artifact or creature card from their graveyard if one exists there."
        )
        ruling(
            "2022-10-14",
            "If the opponent has no artifact or creature cards in their hand, you must still choose an " +
                "artifact or creature card from their graveyard to exile."
        )
    }
}
