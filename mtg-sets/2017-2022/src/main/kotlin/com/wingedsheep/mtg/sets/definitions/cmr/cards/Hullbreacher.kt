package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hullbreacher
 * {2}{U}
 * Creature — Merfolk Pirate
 * 3/2
 *
 * Flash
 * If an opponent would draw a card except the first one they draw in each of their draw steps,
 * instead you create a Treasure token.
 *
 * A per-card draw replacement (CR 121.6, CR 614.1a) scoped to opponents. The exemption is
 * `DrawEvent.exceptFirstInDrawStep`: only the first card an opponent actually draws in their own
 * draw step (CR 504.1) is let through — every other draw, from any instruction, in any step, is
 * replaced.
 *
 * The replacement runs in the drawing player's context (the processor hands the affected player the
 * event), so "**you** create" names Hullbreacher's controller explicitly through
 * [Player.ControllerOfSource] — the Treasure is created by, owned by and controlled by Hullbreacher's
 * controller (CR 111.2), which is also what a token-doubling effect on their side reads.
 *
 * Several Hullbreachers make one Treasure per draw (the ruling): once one replacement has applied the
 * event is no longer a draw. When it competes with another draw replacement the drawing player
 * chooses the order (CR 616.1). A draw a per-turn cap forbids (Narset, Parter of Veils) can't be
 * replaced (CR 614.17c), so it makes no Treasure.
 */
val Hullbreacher = card("Hullbreacher") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Pirate"
    power = 3
    toughness = 2
    oracleText = "Flash\n" +
        "If an opponent would draw a card except the first one they draw in each of their draw " +
        "steps, instead you create a Treasure token. (It's an artifact with \"{T}, Sacrifice this " +
        "token: Add one mana of any color.\")"

    keywords(Keyword.FLASH)

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.CreateTreasure(
                controller = EffectTarget.PlayerRef(Player.ControllerOfSource),
                imageUri = "https://cards.scryfall.io/normal/front/2/8/284ec798-2725-4741-8748-578c259d0623.jpg?1783928585"
            ),
            appliesTo = EventPattern.DrawEvent(
                player = Player.EachOpponent,
                exceptFirstInDrawStep = true,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"I don't need a map to find riches.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4df8aabc-7fcb-4b7b-980b-18f499e6c170.jpg?1783928862"

        ruling("2020-11-10", "If multiple replacement effects apply to the same card draw, the player drawing the card chooses the order in which to apply them.")
        ruling("2020-11-10", "If you control multiple Hullbreachers while an opponent would draw a card except their first one in their draw step, you'll create only one Treasure.")
    }
}
