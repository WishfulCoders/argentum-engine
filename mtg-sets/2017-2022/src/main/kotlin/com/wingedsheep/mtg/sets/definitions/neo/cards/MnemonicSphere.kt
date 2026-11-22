package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mnemonic Sphere — Kamigawa: Neon Dynasty #64
 * {1}{U} · Artifact
 *
 * {1}{U}, Sacrifice this artifact: Draw two cards.
 * Channel — {U}, Discard this card: Draw a card.
 */
val MnemonicSphere = card("Mnemonic Sphere") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{1}{U}, Sacrifice this artifact: Draw two cards.\n" +
        "Channel — {U}, Discard this card: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(2)
        description = "{1}{U}, Sacrifice this artifact: Draw two cards."
    }

    // Channel — {U}, Discard this card (from hand): Draw a card.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Effects.DrawCards(1)
        description = "Channel — {U}, Discard this card: Draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Robin Olausson"
        flavorText = "\"Let the Imperials search our laboratories. Will they think to look for secrets in a " +
            "pretty bauble?\"\n—Emire, director of the Saiba Futurists"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a15d8d9-0575-4add-ae8d-4576e8ed2cbe.jpg?1783923900"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
        ruling(
            "2022-02-18",
            "If a channel ability requires a target, you may not activate it without a target just to " +
                "discard the card."
        )
    }
}
