package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bottle-Cap Blast
 * {4}{R}
 * Instant
 *
 * Improvise
 * Bottle-Cap Blast deals 5 damage to any target. If excess damage was dealt to a permanent this
 * way, create that many tapped Treasure tokens.
 *
 * The damage step stores the excess it dealt (CR 120.4a — above lethal for a creature, counting
 * damage already marked and deathtouch; above loyalty for a planeswalker; above defense for a
 * battle) in a pipeline number slot. A player has no excess, so the slot stays 0 and no Treasure
 * is created.
 */
val BottleCapBlast = card("Bottle-Cap Blast") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\nBottle-Cap Blast deals 5 damage to " +
        "any target. If excess damage was dealt to a permanent this way, create that many tapped " +
        "Treasure tokens. (They're artifacts with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    keywords(Keyword.IMPROVISE)

    spell {
        val t = target(Targets.Any)
        effect = Effects.Pipeline {
            val excess = runStoringNumber { Effects.DealDamage(5, t, excessDamageVariable = it) }
            run(Effects.CreateTreasure(excess.amount, tapped = true))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Liiga Smilshkalne"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bdceec0-0950-4d52-86fc-d0f04e8487ce.jpg?1783912404"
        ruling("2024-03-08", "Improvise doesn't change Bottle-Cap Blast's mana cost or mana value, and it can't reduce Bottle-Cap Blast's cost below {R}.")
        ruling("2024-03-08", "Excess damage has been dealt to a creature if the damage dealt to it is greater than lethal damage. Usually, this means damage greater than its toughness, although damage already marked on the creature is taken into account.")
        ruling("2024-03-08", "For a planeswalker, excess damage means damage in excess of the planeswalker's loyalty (the number of loyalty counters it has on it). For a battle, excess damage means damage in excess of the battle's defense (the number of defense counters it has on it).")
    }
}
