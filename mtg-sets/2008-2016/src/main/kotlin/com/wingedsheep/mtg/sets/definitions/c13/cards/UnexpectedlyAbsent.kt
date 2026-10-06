package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unexpectedly Absent
 * {X}{W}{W}
 * Instant
 *
 * Put target nonland permanent into its owner's library just beneath the top X cards of that
 * library.
 *
 * The position is read on resolution: X = 0 puts it on top, and if that library has fewer than X
 * cards it goes on the bottom (2016-06-08 rulings).
 */
val UnexpectedlyAbsent = card("Unexpectedly Absent") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Put target nonland permanent into its owner's library just beneath the top X cards of that library."

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.PutIntoLibraryBeneathTop(permanent, DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Min Yum"
        flavorText = "Once you've been dragged down the currents of time, you'll never quite trust your own permanence again."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dff437b-ef68-48f7-afd3-3b72d3c56187.jpg?1783939687"
        ruling("2016-06-08", "If there are fewer than X cards in that player's library, put that permanent on the bottom of that library.")
        ruling("2016-06-08", "If you choose 0 as the value for X, put that permanent on top of its owner's library.")
    }
}
