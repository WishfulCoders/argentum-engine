package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Smuggler's Copter
 * {2}
 * Artifact — Vehicle
 * 3/3
 * Flying
 * Whenever this Vehicle attacks or blocks, you may draw a card. If you do, discard a card.
 * Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle becomes
 * an artifact creature until end of turn.)
 *
 * "Attacks or blocks" is two triggers on one printed line — `Triggers.self.attacks()` and
 * `Triggers.self.blocks()` — the same split Lesser Gargadon uses; a creature can't do both in one
 * combat, so it never loots twice. The loot is the optional [Patterns.Hand.loot] under
 * [Effects.May]: declining draws nothing and discards nothing ("If you do").
 */
val SmugglersCopter = card("Smuggler's Copter") {
    manaCost = "{2}"
    typeLine = "Artifact — Vehicle"
    oracleText = "Flying\n" +
        "Whenever this Vehicle attacks or blocks, you may draw a card. If you do, discard a card.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(Patterns.Hand.loot())
        description = "Whenever this Vehicle attacks, you may draw a card. If you do, discard a card."
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.May(Patterns.Hand.loot())
        description = "Whenever this Vehicle blocks, you may draw a card. If you do, discard a card."
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Florian de Gesincourt"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7832abb5-5107-4603-904e-491b221bd3e3.jpg?1783937147"
    }
}
