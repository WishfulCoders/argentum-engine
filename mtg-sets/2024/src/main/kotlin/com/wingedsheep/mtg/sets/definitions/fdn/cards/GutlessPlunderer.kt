package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Gutless Plunderer
 * {2}{B}
 * Creature — Skeleton Pirate
 * 2/2
 *
 * Deathtouch
 * Raid — When this creature enters, if you attacked this turn, look at the top three cards of
 * your library. You may put one of those cards back on top of your library. Put the rest into
 * your graveyard.
 *
 * Raid is the intervening-"if" [Conditions.YouAttackedThisTurn] on the ETB trigger. The effect
 * is a Gather → Select (up to one) → Move pipeline: gather the top three, the controller may keep
 * one on top of the library, and the remainder is milled to the graveyard.
 */
val GutlessPlunderer = card("Gutless Plunderer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Pirate"
    power = 2
    toughness = 2
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\n" +
        "Raid — When this creature enters, if you attacked this turn, look at the top three cards " +
        "of your library. You may put one of those cards back on top of your library. Put the rest " +
        "into your graveyard."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        effect = Effects.Pipeline {
            val gpLooked = gather(CardSource.TopOfLibrary(3), revealed = false)
            val (gpKept, gpRest) = chooseUpToSplit(
                1,
                from = gpLooked,
                prompt = "You may put a card back on top of your library",
                selectedLabel = "Put on top of your library",
                remainderLabel = "Put into your graveyard"
            )
            toLibraryTop(gpKept, order = CardOrder.Preserve)
            toGraveyard(gpRest)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Loïc Canavaggia"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/909d7778-c7f8-4fa4-89f2-8b32e86e96e4.jpg?1782689215"
    }
}
