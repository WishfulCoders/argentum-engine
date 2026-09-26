package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Curiosity
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature deals damage to an opponent, you may draw a card.
 *
 * A `Triggers.attached.dealsDamage` of any damage (not only combat, 2011 ruling) to
 * [Recipient.Opponent]. The attachment detector measures "opponent" against the Aura's
 * controller and makes that player the ability's controller — "you" and "an opponent" are both
 * relative to Curiosity's controller, not the enchanted creature's (rulings). A planeswalker or a
 * battle is not an opponent, so damage to one does not trigger it.
 */
val Curiosity = card("Curiosity") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhenever enchanted creature deals damage to an opponent, you may draw a card."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dealsDamage(to = Recipient.Opponent, damageType = DamageType.Any)
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Val Mayerik"
        flavorText = "All Mirri wanted to do was rest, but she couldn't ignore a nagging suspicion as she followed Crovax's skulking form."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fee17ef5-7e1a-42ae-b680-df81204df7dd.jpg?1783946527"
        ruling("2023-09-01", "Curiosity doesn't trigger if the enchanted creature deals damage to a planeswalker or to a battle.")
        ruling("2023-09-01", "You draw one card each time the enchanted creature deals damage to an opponent, no matter how much damage it deals.")
        ruling("2023-09-01", "If you control Curiosity and it's enchanting an opponent's creature, you won't draw a card when that creature deals damage to you. The creature has to deal damage to one of your opponents for the ability to trigger.")
        ruling("2011-09-22", "\"You\" refers to the controller of Curiosity, which may be different from the controller of the enchanted creature. \"An opponent\" refers to an opponent of Curiosity's controller.")
        ruling("2011-09-22", "Any damage dealt by the enchanted creature to an opponent will cause Curiosity to trigger, not just combat damage.")
    }
}
