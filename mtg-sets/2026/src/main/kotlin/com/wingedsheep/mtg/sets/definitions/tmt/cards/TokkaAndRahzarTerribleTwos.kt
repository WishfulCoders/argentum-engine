package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Tokka & Rahzar, Terrible Twos
 * {B/R}{B/R}
 * Legendary Creature — Turtle Wolf Mutant
 * 3/2
 *
 * This spell can't be countered.
 * Menace
 * Whenever a player casts a spell, if the amount of mana spent to cast it was less
 * than its mana value, Tokka & Rahzar deal 3 damage to that player.
 */
val TokkaAndRahzarTerribleTwos = card("Tokka & Rahzar, Terrible Twos") {
    manaCost = "{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Turtle Wolf Mutant"
    oracleText = "This spell can't be countered.\nMenace\nWhenever a player casts a spell, if the amount of mana spent to cast it was less than its mana value, Tokka & Rahzar deal 3 damage to that player."
    power = 3
    toughness = 2
    cantBeCountered = true

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts()
        // "if the amount of mana spent to cast it was less than its mana value" — compares the
        // triggering spell's actual mana spent against its printed mana value.
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.propertyOf(EffectTarget.TriggeringEntity, EntityNumericProperty.ManaSpent),
            ComparisonOperator.LT,
            DynamicAmounts.triggeringManaValue()
        )
        effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever a player casts a spell, if the amount of mana spent to cast it was less than its mana value, Tokka & Rahzar deal 3 damage to that player."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Yuhong Ding"
        flavorText = "\"Master say have fun . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/2/8/284f9012-a58d-41da-be7c-962dca052711.jpg?1769006390"
    }
}
