package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Step

/**
 * Old Rutstein — Innistrad: Crimson Vow #244
 * {1}{B}{G} · Legendary Creature — Human Peasant · Rare · 1/4
 * Artist: Greg Staples
 *
 * When Old Rutstein enters and at the beginning of your upkeep, mill a card. If a land card is
 * milled this way, create a Treasure token. If a creature card is milled this way, create a 1/1
 * green Insect creature token. If a noncreature, nonland card is milled this way, create a Blood
 * token.
 *
 * "When … enters and at the beginning of your upkeep" is one ability on two events — modeled as two
 * [triggeredAbility] blocks sharing one hoisted effect (`Triggers.self.enters()` +
 * `Triggers.you.beginningOf(Step.UPKEEP)`, the Obsessive Pursuit idiom). The effect mills one card into the "milled"
 * collection ([Patterns.Library.mill]) and then branches on that card's type with three independent
 * [Effects.If] gates (Bonehoard Dracosaur shape). Because exactly one card is milled and the
 * three filters — land / creature / (noncreature ∧ nonland) — partition every card type, at most one
 * branch fires per resolution, matching the oracle's mutually-exclusive "If a … card is milled".
 */
val OldRutstein = card("Old Rutstein") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Peasant"
    power = 1
    toughness = 4
    oracleText = "When Old Rutstein enters and at the beginning of your upkeep, mill a card. If a " +
        "land card is milled this way, create a Treasure token. If a creature card is milled this " +
        "way, create a 1/1 green Insect creature token. If a noncreature, nonland card is milled " +
        "this way, create a Blood token."

    // Mill a card into the "milled" collection (then to the graveyard).
    val millAndReact = Patterns.Library.mill(1) then
        // If a land card is milled this way, create a Treasure token.
        Effects.If(
            condition = Conditions.CollectionContainsMatch(Patterns.Library.milled, GameObjectFilter.Land),
            then = Effects.CreateTreasure()
        ) then
        // If a creature card is milled this way, create a 1/1 green Insect creature token.
        Effects.If(
            condition = Conditions.CollectionContainsMatch(Patterns.Library.milled, GameObjectFilter.Creature),
            then = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Insect")
            )
        ) then
        // If a noncreature, nonland card is milled this way, create a Blood token.
        Effects.If(
            condition = Conditions.CollectionContainsMatch(Patterns.Library.milled,
                GameObjectFilter.Noncreature and GameObjectFilter.Nonland
            ),
            then = Effects.CreateBlood()
        )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = millAndReact
        description = "When Old Rutstein enters, mill a card. If a land card is milled this way, " +
            "create a Treasure token. If a creature card is milled this way, create a 1/1 green " +
            "Insect creature token. If a noncreature, nonland card is milled this way, create a " +
            "Blood token."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = millAndReact
        description = "At the beginning of your upkeep, mill a card. If a land card is milled this " +
            "way, create a Treasure token. If a creature card is milled this way, create a 1/1 " +
            "green Insect creature token. If a noncreature, nonland card is milled this way, " +
            "create a Blood token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/625b8023-2ef1-4b7b-9e48-4f774fee14e0.jpg?1783924790"
    }
}
