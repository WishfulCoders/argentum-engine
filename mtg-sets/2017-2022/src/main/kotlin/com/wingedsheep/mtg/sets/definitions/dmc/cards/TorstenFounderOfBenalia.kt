package com.wingedsheep.mtg.sets.definitions.dmc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Torsten, Founder of Benalia — Dominaria United Commander #47 (mythic)
 * {5}{G}{W} · Legendary Creature — Human Soldier · 7/7
 *
 * When Torsten enters, reveal the top seven cards of your library. Put any number of creature
 * and/or land cards from among them into your hand and the rest on the bottom of your library in
 * a random order.
 * When Torsten dies, create seven 1/1 white Soldier creature tokens.
 *
 * The enters trigger is the reveal → select → move pipeline (Rip, Spawn Hunter is the same shape):
 * the seven cards are revealed, the controller keeps any number of creature and/or land cards
 * ([SelectionMode.ChooseAnyNumber], so zero is a legal choice), and everything not kept — the
 * unchosen creatures and lands as well as every other card — goes to the bottom in a random order.
 *
 * The Soldiers carry no explicit art: DMC has no Soldier on its token sheet, so they resolve through
 * the set's borrowed Dominaria United sheet (`DominariaUnitedCommanderSet.tokenArt`).
 */
val TorstenFounderOfBenalia = card("Torsten, Founder of Benalia") {
    manaCost = "{5}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 7
    toughness = 7
    oracleText = "When Torsten enters, reveal the top seven cards of your library. Put any number of " +
        "creature and/or land cards from among them into your hand and the rest on the bottom of your " +
        "library in a random order.\nWhen Torsten dies, create seven 1/1 white Soldier creature tokens."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(7), revealed = true)
            val split = chooseAnyNumberSplit(
                from = revealed,
                filter = GameObjectFilter.Creature or GameObjectFilter.Land,
                showAllCards = true,
                prompt = "Put any number of creature and/or land cards into your hand",
                selectedLabel = "Into your hand",
                remainderLabel = "Bottom of library (random order)",
            )
            toHand(split.selected)
            toLibraryBottom(split.remainder, order = CardOrder.Random)
        }
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            count = 7,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "47"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0783b426-a527-42c1-9271-be28b229e1c6.jpg?1783921458"
    }
}
