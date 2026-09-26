package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tephraderm
 * {4}{R}
 * Creature — Beast
 * 4/5
 * Whenever a creature deals damage to Tephraderm, Tephraderm deals that much damage to that creature.
 * Whenever a spell deals damage to Tephraderm, Tephraderm deals that much damage to that spell's controller.
 */
val Tephraderm = card("Tephraderm") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 5
    oracleText = "Whenever a creature deals damage to Tephraderm, Tephraderm deals that much damage to that creature.\nWhenever a spell deals damage to Tephraderm, Tephraderm deals that much damage to that spell's controller."

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage(GameObjectFilter.Creature)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.TriggeringEntity
        )
    }

    triggeredAbility {
        // The only spells that deal damage as spells are instants and sorceries (a permanent spell
        // resolves into a permanent first), and by the time this triggers the spell has finished
        // resolving and left the stack — so "a spell" is read off the card's type, not its zone.
        trigger = Triggers.self.isDealtDamage(GameObjectFilter.InstantOrSorcery)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.ControllerOfTriggeringEntity
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "239"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41b65eba-140b-4c1d-b796-8134b7c1ede8.jpg?1562910455"
    }
}
