package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Starwinder
 * {5}{U}{U}
 * Creature — Leviathan
 * 7/7
 *
 * Whenever a creature you control deals combat damage to a player, you may draw that many cards.
 * Warp {2}{U}{U}
 */
val Starwinder = card("Starwinder") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Leviathan"
    power = 7
    toughness = 7
    oracleText = "Whenever a creature you control deals combat damage to a player, you may draw that many cards.\n" +
        "Warp {2}{U}{U} (You may cast this card from your hand for its warp cost. Exile this creature at " +
        "the beginning of the next end step, then you may cast it from exile on a later turn.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(
            Effects.DrawCards(DynamicAmounts.triggerDamageAmount())
        )
        description = "Whenever a creature you control deals combat damage to a player, you may draw that many cards."
    }

    warp = "{2}{U}{U}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Devin Elle Kurtz"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27d1a010-5790-4b35-9fdc-0e366eed021d.jpg?1752946871"
    }
}
