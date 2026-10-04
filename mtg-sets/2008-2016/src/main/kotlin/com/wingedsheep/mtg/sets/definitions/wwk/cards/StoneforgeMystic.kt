package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Stoneforge Mystic — Worldwake #20
 * {1}{W} · Creature — Kor Artificer · 1/2
 *
 * When this creature enters, you may search your library for an Equipment card, reveal it, put it
 * into your hand, then shuffle.
 * {1}{W}, {T}: You may put an Equipment card from your hand onto the battlefield.
 *
 * - The enters trigger is the Godo, Bandit Warlord search with `optional = true` for the "you may",
 *   revealed and sent to hand instead of the battlefield.
 * - The activated ability is [Patterns.Hand.putFromHand] (Goblin Wizard's shape): its up-to-one
 *   selection is the "you may", and it may put *any* Equipment card from hand, not just the one
 *   the trigger found (2010-03-01 ruling). The Equipment enters unattached.
 */
val StoneforgeMystic = card("Stoneforge Mystic") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Artificer"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, you may search your library for an Equipment card, reveal it, put it into your hand, then shuffle.\n" +
        "{1}{W}, {T}: You may put an Equipment card from your hand onto the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
            destination = SearchDestination.HAND,
            reveal = true,
        )
        description = "When this creature enters, you may search your library for an Equipment card, reveal it, put it into your hand, then shuffle."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
            count = 1,
            prompt = "Put an Equipment card from your hand onto the battlefield?",
        )
        description = "{1}{W}, {T}: You may put an Equipment card from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19557351-b65f-4b04-b971-66abdc07000a.jpg?1783942065"
        ruling("2010-03-01", "When Stoneforge Mystic's second ability resolves, you may put any Equipment card from your hand onto the battlefield, not just the one you searched for with its first ability. The Equipment is put onto the battlefield unattached.")
    }
}
