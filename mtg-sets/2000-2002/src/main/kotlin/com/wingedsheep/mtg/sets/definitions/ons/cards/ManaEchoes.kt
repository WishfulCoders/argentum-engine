package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mana Echoes
 * {2}{R}{R}
 * Enchantment
 * Whenever a creature enters, you may add an amount of {C} equal to the number
 * of creatures you control that share a creature type with it.
 */
val ManaEchoes = card("Mana Echoes") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature enters, you may add an amount of {C} equal to the number of creatures you control that share a creature type with it."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).enters()
        effect = Effects.May(
            Effects.AddColorlessMana(
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Creature.sharingCreatureTypeWith(EffectTarget.TriggeringEntity)
                ).count()
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Scott M. Fischer"
        flavorText = "When the ground is saturated with mana, even the lightest footstep can bring it to the surface."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b15d04c-62cb-4704-8cc7-9842cef27a1b.jpg?1562899467"
    }
}
