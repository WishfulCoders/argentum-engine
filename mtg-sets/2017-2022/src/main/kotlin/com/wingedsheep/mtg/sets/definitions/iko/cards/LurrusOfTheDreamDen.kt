package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard

/**
 * Lurrus of the Dream-Den
 * {1}{W/B}{W/B}
 * Legendary Creature — Cat Nightmare
 * 3/2
 *
 * Companion — Each permanent card in your starting deck has mana value 2 or less.
 * Lifelink
 * Once during each of your turns, you may cast a permanent spell with mana value 2 or less from
 * your graveyard.
 *
 * The companion clause is a deckbuilding / outside-the-game ability (CR 702.139): it has no effect
 * while the card is in the starting deck or on the battlefield, so it is carried in the oracle text
 * only — like "A deck can have any number of cards named …". The engine has no companion zone, so
 * Lurrus plays as a main-deck card; its battlefield abilities are complete.
 *
 * The graveyard line is a [MayCastFromGraveyard] grant with `oncePerTurn` tracked on this permanent
 * (a second Lurrus that turn brings a second use, per ruling). "Permanent spell" excludes lands by
 * construction — the grant authorizes casts, and lands are played, never cast.
 */
val LurrusOfTheDreamDen = card("Lurrus of the Dream-Den") {
    manaCost = "{1}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Cat Nightmare"
    power = 3
    toughness = 2
    oracleText = "Companion — Each permanent card in your starting deck has mana value 2 or less. " +
        "(If this card is your chosen companion, you may put it into your hand from outside the game for {3} as a sorcery.)\n" +
        "Lifelink\n" +
        "Once during each of your turns, you may cast a permanent spell with mana value 2 or less from your graveyard."

    keywords(Keyword.LIFELINK)

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.Permanent.manaValueAtMost(2),
            duringYourTurnOnly = true,
            oncePerTurn = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5ad36fb2-c44e-4085-ba0d-54277841ad3a.jpg?1783931010"

        ruling("2020-04-17", "You must follow the normal timing permissions and restrictions of the spell you cast from your graveyard.")
        ruling("2020-04-17", "You must pay the costs to cast that spell. If it has an alternative cost, such as a mutate cost, you may cast it for that cost instead.")
        ruling("2020-04-17", "If you cast a spell from your graveyard using another permission, Lurrus's effect doesn't apply. You can cast another permanent spell from your graveyard.")
        ruling("2020-04-17", "If you cast one permanent spell from your graveyard and then have a new Lurrus come under your control in the same turn, you may cast another permanent spell from your graveyard that turn.")
        ruling("2020-04-17", "Lurrus doesn't let you play lands from your graveyard.")
        ruling("2020-04-17", "The companion's other abilities apply only if the creature is on the battlefield. They have no effect while the companion is outside the game.")
    }
}
