package com.wingedsheep.mtg.sets.definitions.unf

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Unfinity (2022) — an Un-set (Scryfall `set_type` funny). Scaffolded only for the cards that are
 * eternal-legal and on Arena (Comet, Stellar Pup) plus the Printing rows it owes implemented
 * reprints; acorn and sticker cards are not implemented.
 */
object UnfinitySet : MtgSet {
    override val code = "UNF"
    override val displayName = "Unfinity"
    override val releaseDate = "2022-10-07"
    override val sealedSupported = false
    override val incomplete = true
    override val cards: List<CardDefinition> by lazy { CardDiscovery.findIn(CARDS_PACKAGE) }
    override val printings: List<Printing> by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }
    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.unf.cards"
}
