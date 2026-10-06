package com.wingedsheep.mtg.sets.definitions.onc

import com.wingedsheep.mtg.sets.definitions.one.PhyrexiaAllWillBeOneSet
import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.mtg.sets.tokens.TokenArtData
import com.wingedsheep.sdk.model.TokenPrinting
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

object PhyrexiaAllWillBeOneCommanderSet : MtgSet {
    override val code = "ONC"
    override val displayName = "Phyrexia: All Will Be One Commander"
    override val releaseDate = "2023-02-10"
    override val sealedSupported = false
    override val incomplete = true

    // The Commander decks shipped alongside Phyrexia: All Will Be One and share its token sheet —
    // the Rebel that For Mirrodin! (Glimmer Lens) creates is an ONE token.
    override val tokenArt: List<TokenPrinting> by lazy {
        TokenArtData.borrowedFrom(PhyrexiaAllWillBeOneSet.code, code)
    }
    override val cards: List<CardDefinition> by lazy { CardDiscovery.findIn(CARDS_PACKAGE) }
    override val printings: List<Printing> by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }
    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.onc.cards"
}
