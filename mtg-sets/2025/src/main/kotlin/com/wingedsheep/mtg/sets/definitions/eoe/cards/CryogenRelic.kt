package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Cryogen Relic
 * {1}{U}
 * Artifact
 * When this artifact enters or leaves the battlefield, draw a card.
 * {1}{U}, Sacrifice this artifact: Put a stun counter on up to one target tapped creature. (If a permanent with a stun counter would become untapped, remove one from it instead.)
 */
val CryogenRelic = card("Cryogen Relic") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "When this artifact enters or leaves the battlefield, draw a card.\n{1}{U}, Sacrifice this artifact: Put a stun counter on up to one target tapped creature. (If a permanent with a stun counter would become untapped, remove one from it instead.)"

    // ETB trigger: draw a card
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    // Leaves battlefield trigger: draw a card
    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.DrawCards(1)
    }

    // Activated ability: sacrifice to put stun counter on tapped creature
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{U}"),
            Costs.SacrificeSelf
        )
        val target = target(
            com.wingedsheep.sdk.scripting.filters.unified.TargetFilter(GameObjectFilter.Creature.tapped()),
            optional = true,
        )
        effect = Effects.AddCounters(CounterType.STUN, 1, target)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Eelis Kyttanen"
        flavorText = "Older than the Eumidian seedships that populated Evendo."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bfb33b6-e2bf-498f-8c58-ae21a840cf75.jpg?1752946757"
    }
}
