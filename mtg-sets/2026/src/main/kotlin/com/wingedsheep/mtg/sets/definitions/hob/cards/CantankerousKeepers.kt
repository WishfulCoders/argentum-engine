package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cantankerous Keepers
 * {5}{G}
 * Creature — Elf Soldier
 * 4/3
 *
 * Affinity for Elves (This spell costs {1} less to cast for each Elf you control.)
 * When this creature enters, mill four cards, then put all Elf cards from among them into your hand.
 *
 * The enters trigger is a Gather → Move → Filter → Move pipeline: the four cards genuinely hit the
 * graveyard first (so mill triggers and graveyard-count payoffs see them), and only then are the Elf
 * cards among *those four* pulled to hand. The partition is a choice-free [FilterCollectionEffect] —
 * "all Elf cards" is mandatory, with no selection prompt.
 */
val CantankerousKeepers = card("Cantankerous Keepers") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Soldier"
    power = 4
    toughness = 3
    oracleText = "Affinity for Elves (This spell costs {1} less to cast for each Elf you control.)\n" +
        "When this creature enters, mill four cards, then put all Elf cards from among them into your hand."

    keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.ELF))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            // Mill four cards.
            val milled = gather(CardSource.TopOfLibrary(4, Player.You, isMill = true))
            toGraveyard(milled)
            // Then put all Elf cards from among them into your hand.
            val elves = filter(milled, GameObjectFilter.Any.withSubtype(Subtype.ELF))
            toHand(elves)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Ramza Psyru"
        flavorText = "The guards were decidedly less than pleased, upon waking, to find their " +
            "prisoners escaped."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fae46a70-a6d3-4584-859d-6c7425fb1508.jpg?1785152413"
    }
}
