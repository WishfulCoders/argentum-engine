package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sandskin
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Prevent all combat damage that would be dealt to and dealt by enchanted creature.
 */
val Sandskin = card("Sandskin") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nPrevent all combat damage that would be dealt to and dealt by enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // Prevent combat damage TO enchanted creature
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.EnchantedCreature,
                damageType = DamageType.Combat
            )
        )
    )

    // Prevent combat damage FROM enchanted creature
    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.attachedToBySource(),
                damageType = DamageType.Combat
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Glen Angus"
        flavorText = "\"Those who live by the sword will die by the sword. I choose to do neither.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80b59844-c9d4-4bc1-86e6-4cc596d9165d.jpg?1562925378"
    }
}
