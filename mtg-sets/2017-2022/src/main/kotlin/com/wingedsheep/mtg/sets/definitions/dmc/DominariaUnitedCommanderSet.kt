package com.wingedsheep.mtg.sets.definitions.dmc

import com.wingedsheep.mtg.sets.definitions.dmu.DominariaUnitedSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.mtg.sets.tokens.TokenArtData
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * Dominaria United Commander (2022)
 *
 * Commander preconstructed decks released alongside the Dominaria United main set.
 *
 * Set Code: DMC
 * Release Date: September 9, 2022
 */
object DominariaUnitedCommanderSet : MtgSet {

    override val code = "DMC"
    override val displayName = "Dominaria United Commander"
    override val releaseDate = "2022-09-09"
    override val sealedSupported = false
    override val incomplete = true

    // The Commander decks shipped alongside Dominaria United and share its token sheet (Scryfall has
    // no `tdmc`), so the tokens its cards mint — Torsten's Soldiers — are Dominaria United tokens.
    override val tokenArt: List<TokenPrinting> by lazy {
        TokenArtData.borrowedFrom(DominariaUnitedSet.code, code)
    }

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.dmc.cards"
}
