package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.vividEtb

/**
 * Aurora Awakener
 * {6}{G}
 * Creature — Giant Druid
 * 7/7
 *
 * Trample
 * Vivid — When this creature enters, reveal cards from the top of your library until you
 * reveal X permanent cards, where X is the number of colors among permanents you control.
 * Put any number of those permanent cards onto the battlefield, then put the rest of the
 * revealed cards on the bottom of your library in a random order.
 */
val AuroraAwakener = card("Aurora Awakener") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant Druid"
    oracleText = "Trample\nVivid — When this creature enters, reveal cards from the top of your library " +
        "until you reveal X permanent cards, where X is the number of colors among permanents you control. " +
        "Put any number of those permanent cards onto the battlefield, then put the rest of the revealed " +
        "cards on the bottom of your library in a random order."
    power = 7
    toughness = 7

    keywords(Keyword.TRAMPLE)

    vividEtb { colorCount ->
        Effects.Pipeline {
            val (_, allRevealed) = gatherUntilMatch(GameObjectFilter.Permanent, count = colorCount)
            // Public reveal so spectators/opponent see what was walked. The caster's
            // selection modal below supersedes this reveal in the UI.
            reveal(allRevealed)
            // Single-step reveal+select for the caster: the modal shows every revealed
            // card with only the permanents selectable.
            val toBattlefield = chooseAnyNumber(
                from = allRevealed,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put on bottom of library"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD))
            // Everything revealed minus the cards that went to the battlefield goes to
            // the bottom of the library in a random order.
            val toBottom = exclude(allRevealed, minus = toBattlefield)
            toLibraryBottom(toBottom, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "165"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/913977c2-73f9-466b-bd01-827c1736e070.jpg?1767658343"
    }
}
