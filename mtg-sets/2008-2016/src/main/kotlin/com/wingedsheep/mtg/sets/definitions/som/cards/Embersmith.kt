package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Embersmith — Scars of Mirrodin #87
 * {1}{R} · Creature — Human Artificer · 2 / 1
 *
 * Whenever you cast an artifact spell, you may pay {1}. If you do, this creature deals 1 damage to any target.
 *
 * The Lightning Rift shape: an optional mana payment gating the payoff. [Effects.MayPay] lowers
 * to a `GatedEffect` over `Gate.MayPay`, which the engine recognizes as the flat optional-mana
 * form — the controller taps for the {1} at resolution and only then chooses the target.
 */
val Embersmith = card("Embersmith") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 1
    oracleText = "Whenever you cast an artifact spell, you may pay {1}. If you do, this creature deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Artifact)
        val t = target(Targets.Any)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}"),
            then = Effects.DealDamage(1, t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Eric Deschamps"
        flavorText = "The Vulshok see the artificer as a catalyst, bringing the spark of creation that ignites change."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee86cfc8-9faa-474c-90a9-5405f3f6037c.jpg?1783941726"
    }
}
