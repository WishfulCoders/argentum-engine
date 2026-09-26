package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Stew the Coneys
 * {2}{G}
 * Instant
 *
 * Target creature you control deals damage equal to its power to target creature you don't control.
 * Create a Food token. (It's an artifact with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 */
val StewTheConeys = card("Stew the Coneys") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control deals damage equal to its power to target creature you don't control. " +
        "Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.powerOf(myCreature),
            target = theirCreature,
            damageSource = myCreature
        ) then Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Eelis Kyttanen"
        flavorText = "\"Stew the rabbits?\" squealed Gollum in dismay. \"Spoil beautiful meat Sméagol saved saved for you, poor hungry Sméagol! What for?\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcc7c41c-f416-457e-91ba-1f338f45eeac.jpg?1686969611"
    }
}
