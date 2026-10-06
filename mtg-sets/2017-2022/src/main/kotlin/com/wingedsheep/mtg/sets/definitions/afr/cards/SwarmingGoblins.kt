package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

private const val GOBLIN_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/1/4/1425e965-7eea-419c-a7ec-c8169fa9edbf.jpg?1783926342"

/** A 1/1 red Goblin creature token — "those tokens" in every row of the table. */
private fun goblins(count: Int) = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.RED),
    creatureTypes = setOf("Goblin"),
    count = count,
    imageUri = GOBLIN_TOKEN_IMAGE,
)

/**
 * Swarming Goblins
 * {4}{R}
 * Creature — Goblin
 * 4/3
 * When this creature enters, roll a d20.
 * 1—9 | Create a 1/1 red Goblin creature token.
 * 10—19 | Create two of those tokens.
 * 20 | Create three of those tokens.
 *
 * The d20 results table (CR 706.3) is [Patterns.Mechanic.rollDie]; each row creates the same token
 * ("those tokens"), one, two or three of them.
 */
val SwarmingGoblins = card("Swarming Goblins") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "When this creature enters, roll a d20.\n1—9 | Create a 1/1 red Goblin creature token.\n" +
        "10—19 | Create two of those tokens.\n20 | Create three of those tokens."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Mechanic.rollDie(
            20,
            1..9 to goblins(1),
            10..19 to goblins(2),
            20..20 to goblins(3),
        )
        description = "When this creature enters, roll a d20. 1—9 | Create a 1/1 red Goblin creature token. " +
            "10—19 | Create two of those tokens. 20 | Create three of those tokens."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/623c1d1b-69a4-4bc4-b388-17a7600fd960.jpg?1783926472"
    }
}
