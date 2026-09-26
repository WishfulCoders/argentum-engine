package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Weapons Manufacturing
 * {1}{R}
 * Enchantment
 * Whenever a nontoken artifact you control enters, create a colorless artifact token
 * named Munitions with "When this token leaves the battlefield, it deals 2 damage to
 * any target."
 */
val WeaponsManufacturing = card("Weapons Manufacturing") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken artifact you control enters, create a colorless artifact token " +
        "named Munitions with \"When this token leaves the battlefield, it deals 2 damage to any target.\""

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl().nontoken()).enters()
        effect = Effects.CreateMunitionsToken()
        description = "Whenever a nontoken artifact you control enters, create a Munitions token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Marco Gorlei"
        flavorText = "\"Soon we'll have enough firepower to rid Evendo of its bug infestation.\"\n" +
            "—General Tekvu, Kavaron Memorial Navy"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a058f1a6-318c-4bba-981e-ace079ada806.jpg?1752947235"
    }
}
