package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Circle of Protection: Black
 * {1}{W}
 * Enchantment
 * {1}: The next time a black source of your choice would deal damage to you this turn, prevent
 *   that damage.
 *
 * Accidentally left out of Limited Edition Alpha, so Beta is its earliest printing. Same shape as
 * the rest of the Circle of Protection cycle (see Alpha's Circle of Protection: White).
 */
val CircleOfProtectionBlack = card("Circle of Protection: Black") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time a black source of your choice would deal damage to you this " +
        "turn, prevent that damage."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.PreventDamage(
            sources = PreventionSourceFilter.Chosen(GameObjectFilter.Any.withColor(Color.BLACK)),
            nextInstanceOnly = true
        )
        description = "{1}: The next time a black source of your choice would deal damage to you this turn, prevent that damage."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa47b4cd-8da4-4544-b011-ba92b7009203.jpg?1783948654"
    }
}
