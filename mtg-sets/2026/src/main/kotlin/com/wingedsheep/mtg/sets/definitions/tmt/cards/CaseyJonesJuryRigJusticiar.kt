package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Casey Jones, Jury-Rig Justiciar
 * {1}{R}
 * Legendary Creature — Human Berserker
 * 2/1
 *
 * Haste
 * When Casey Jones enters, look at the top four cards of your library.
 * You may reveal an artifact card from among them and put it into your
 * hand. Put the rest on the bottom of your library in a random order.
 */
val CaseyJonesJuryRigJusticiar = card("Casey Jones, Jury-Rig Justiciar") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Berserker"
    oracleText = "Haste\nWhen Casey Jones enters, look at the top four cards of your library. You may reveal an artifact card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."
    power = 2
    toughness = 1

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.IsArtifact)
                ),
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Lordigan"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/808a5bc0-0999-47cf-854c-30db6277efe5.jpg?1760114588"
    }
}
