package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kolaghan Warmonger — March of the Machine: The Aftermath #17
 * {2}{R} · Creature — Ogre Warrior · 3/2
 *
 * Haste
 * Whenever this creature attacks, look at the top six cards of your library. You may reveal a
 * Dragon card from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order.
 */
val KolaghanWarmonger = card("Kolaghan Warmonger") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 2
    oracleText = "Haste\n" +
        "Whenever this creature attacks, look at the top six cards of your library. You may reveal " +
        "a Dragon card from among them and put it into your hand. Put the rest on the bottom of " +
        "your library in a random order."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 6,
            filter = GameObjectFilter.Any.withSubtype(Subtype.DRAGON),
            prompt = "You may reveal a Dragon card from among them and put it into your hand",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8361d9e2-b9fc-4d46-a1ba-a24139157f26.jpg?1783916523"
    }
}
