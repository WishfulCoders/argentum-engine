package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aang's Journey
 * {2}
 * Sorcery — Lesson
 *
 * Kicker {2} (You may pay an additional {2} as you cast this spell.)
 * Search your library for a basic land card. If this spell was kicked, instead search your
 * library for a basic land card and a Shrine card. Reveal those cards, put them into your
 * hand, then shuffle.
 * You gain 2 life.
 *
 * The kicker swaps the entire search clause, so it is modeled as a [Effects.If] keyed
 * on [WasKicked] (resolution-time state test, no decision/pause): the unkicked branch is the
 * ordinary single basic-land tutor-to-hand ([Patterns.Library.searchLibrary]); the kicked
 * branch adds a second selection for a Shrine card, then reveals + moves both finds to hand
 * with a single shuffle afterward. All selections are `ChooseUpTo(1)` because a library search
 * may legally fail to find (CR 701.19). The +2 life always follows, regardless of kicker.
 */
val AangsJourney = card("Aang's Journey") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Sorcery — Lesson"
    oracleText = "Kicker {2} (You may pay an additional {2} as you cast this spell.)\n" +
        "Search your library for a basic land card. If this spell was kicked, instead search " +
        "your library for a basic land card and a Shrine card. Reveal those cards, put them " +
        "into your hand, then shuffle.\n" +
        "You gain 2 life."

    keywordAbility(KeywordAbility.kicker("{2}"))

    spell {
        val kickedSearch = Effects.Pipeline {
            val landSearchable = gather(CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand))
            val foundLand = chooseUpTo(1, from = landSearchable, prompt = "Search your library for a basic land card")
            val shrineSearchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Any.withSubtype("Shrine")
                )
            )
            val foundShrine = chooseUpTo(1, from = shrineSearchable, prompt = "Search your library for a Shrine card")
            toHand(foundLand, revealed = true)
            toHand(foundShrine, revealed = true)
            run(Effects.ShuffleLibrary())
        }

        effect = Effects.If(
            condition = WasKicked,
            then = kickedSearch,
            otherwise = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        ) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Kotakan"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e51f727-5a9b-4bc7-83a9-dbcf1c933e15.jpg?1778833139"
    }
}
