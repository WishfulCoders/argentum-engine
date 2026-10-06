package com.wingedsheep.mtg.sets.definitions.me2

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Masters Edition II (ME2)
 *
 * MTGO-only reprint set of older cards (Ice Age block, Fallen Empires, Homelands and others). Its
 * printing of Mana Crypt is that card's earliest real-expansion printing (the 1995 HarperPrism book
 * promo doesn't count), so Mana Crypt's canonical [CardDefinition] lives here.
 *
 * Set Code: ME2
 * Released: September 22, 2008
 */
object MastersEditionIISet : MtgSet {
    override val code = "ME2"
    override val displayName = "Masters Edition II"
    override val releaseDate = "2008-09-22"
    override val sealedSupported = false
    override val incomplete = true
    override val cards: List<CardDefinition> by lazy { CardDiscovery.findIn(CARDS_PACKAGE) }
    override val printings: List<Printing> by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }
    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.me2.cards"
}
