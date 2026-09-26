package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thrashing Mudspawn
 * {3}{B}{B}
 * Creature — Beast
 * 4/4
 * Whenever Thrashing Mudspawn is dealt damage, you lose that much life.
 * Morph {1}{B}{B}
 */
val ThrashingMudspawn = card("Thrashing Mudspawn") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Whenever Thrashing Mudspawn is dealt damage, you lose that much life.\nMorph {1}{B}{B}"

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.LoseLife(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.Controller
        )
    }

    morph = "{1}{B}{B}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Thomas M. Baxa"
        flavorText = "\"It just obeys you. It doesn't like you.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da84de0e-a4cd-4dff-8ee3-87c9debf0969.jpg?1562947056"
    }
}
