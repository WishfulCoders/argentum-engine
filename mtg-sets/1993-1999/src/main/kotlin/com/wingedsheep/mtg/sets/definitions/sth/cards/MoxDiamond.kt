package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersOnlyIfCostPaid
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mox Diamond — Stronghold #138
 * {0} · Artifact
 *
 * If this artifact would enter, you may discard a land card instead. If you do, put this artifact
 * onto the battlefield. If you don't, put it into its owner's graveyard.
 * {T}: Add one mana of any color.
 *
 * The first line is [EntersOnlyIfCostPaid] with a "discard a land card" cost: a self-replacement on
 * its own entry (CR 614.1a, 614.12), settled before it enters (CR 614.12a) however it would enter —
 * cast, or put onto the battlefield by an effect. Unpaid, it goes straight to its owner's graveyard
 * and never enters (2008-05-01 ruling).
 */
val MoxDiamond = card("Mox Diamond") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "If this artifact would enter, you may discard a land card instead. If you do, put this artifact onto the battlefield. If you don't, put it into its owner's graveyard.\n{T}: Add one mana of any color."

    replacementEffect(EntersOnlyIfCostPaid(Costs.pay.Discard(GameObjectFilter.Land)))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28028830-83ed-45e2-b495-3b9ad9d3e988.jpg?1783946538"
        ruling("2008-05-01", "If you don't discard a land card, Mox Diamond never enters. It won't trigger abilities that look for something entering, and you won't get the opportunity to tap it for mana.")
    }
}
