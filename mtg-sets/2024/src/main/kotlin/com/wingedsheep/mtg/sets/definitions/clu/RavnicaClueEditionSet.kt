package com.wingedsheep.mtg.sets.definitions.clu

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

object RavnicaClueEditionSet : MtgSet {
    override val code = "CLU"
    override val displayName = "Ravnica: Clue Edition"
    override val releaseDate = "2024-02-23"
    override val sealedSupported = false
    override val incomplete = true
    override val cards: List<CardDefinition> by lazy { CardDiscovery.findIn(CARDS_PACKAGE) }
    override val printings: List<Printing> by lazy { CardDiscovery.findPrintingsIn(CARDS_PACKAGE) }
    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.clu.cards"
}
