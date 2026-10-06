package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Brazen Borrower // Petty Theft
 * {1}{U}{U}
 * Creature — Faerie Rogue
 * 3/1
 * Flash
 * Flying
 * This creature can block only creatures with flying.
 *
 * Adventure: Petty Theft — {1}{U}, Instant — Adventure
 * Return target nonland permanent an opponent controls to its owner's hand.
 *
 * Flash rides on the creature face, so after Petty Theft exiles the card the Borrower can be cast
 * from exile at instant speed too — the adventure permission keeps the card's own timing.
 */
val BrazenBorrower = card("Brazen Borrower") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    oracleText = "Flash\nFlying\nThis creature can block only creatures with flying."
    power = 3
    toughness = 1

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }

    adventure("Petty Theft") {
        manaCost = "{1}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Return target nonland permanent an opponent controls to its owner's hand. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target(TargetFilter.NonlandPermanentOpponentControls)
            effect = Effects.Move(t, Zone.HAND)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "39"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2089ec9-0665-448f-bfe9-d181de127814.jpg?1783932663"
        ruling(
            "2025-06-06",
            "If a spell is cast as an Adventure, its controller exiles it instead of putting it into its " +
                "owner's graveyard as it resolves. For as long as it remains exiled, that player may play it " +
                "using its primary characteristics. If an Adventure spell leaves the stack in any way other " +
                "than resolving, that card won't be exiled and the spell's controller won't be able to play " +
                "that card from exile later."
        )
    }
}
