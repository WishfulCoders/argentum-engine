package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Marang River Regent // Coil and Catch — Tarkir: Dragonstorm #51
 * {4}{U}{U} · Creature — Dragon · 6/7
 *
 * Flying
 * When this creature enters, return up to two other target nonland permanents to their owners' hands.
 *
 * Omen: Coil and Catch — {3}{U}, Instant — Omen
 * Draw three cards, then discard a card.
 *
 * The ETB returns up to two OTHER target nonland permanents (excludeSelf via
 * [TargetFilter.NonlandPermanent.other]) — optional, so a cast with zero or one legal target still
 * resolves. (Omen, Tarkir: Dragonstorm: casting the Omen face shuffles this card into its owner's
 * library on resolution instead of going to the graveyard — see
 * [com.wingedsheep.sdk.model.CardLayout.OMEN].)
 */
val MarangRiverRegent = card("Marang River Regent") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 7
    oracleText = "Flying\n" +
        "When this creature enters, return up to two other target nonland permanents to their owners' hands."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.NonlandPermanent.other(), count = 2, optional = true)
        effect = Effects.Pipeline {
            val marangRiverRegentTargets = gather(CardSource.ChosenTargets)
            toHand(marangRiverRegentTargets)
        }
        description = "When this creature enters, return up to two other target nonland permanents to " +
            "their owners' hands."
    }

    // Omen: Coil and Catch — Instant. Draw three cards, then discard a card.
    omen("Coil and Catch") {
        manaCost = "{3}{U}"
        typeLine = "Instant — Omen"
        oracleText = "Draw three cards, then discard a card. " +
            "(Then shuffle this card into its owner's library.)"
        spell {
            effect = Effects.DrawCards(3) then Effects.Discard(1)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "John Tedrick"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f890bdc7-32e6-4492-bac7-7cabf54a8bfd.jpg?1764062994"
    }
}
