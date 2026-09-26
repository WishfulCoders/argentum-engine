package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Venus, Torn Between Worlds
 * {4}{G}
 * Legendary Creature — Mutant Frog Turtle
 * 5/5
 *
 * Whenever Venus is dealt damage, put that many +1/+1 counters on her. (She must
 * survive the damage to get the counters.)
 * Whenever a creature you control with a counter on it deals combat damage to a
 * player, you may pay {U}. If you do, draw a card.
 */
val VenusTornBetweenWorlds = card("Venus, Torn Between Worlds") {
    manaCost = "{4}{G}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Mutant Frog Turtle"
    oracleText = "Whenever Venus is dealt damage, put that many +1/+1 counters on her. (She must survive the damage to get the counters.)\nWhenever a creature you control with a counter on it deals combat damage to a player, you may pay {U}. If you do, draw a card."
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.triggerDamageAmount(),
            EffectTarget.Self
        )
        description = "Whenever Venus is dealt damage, put that many +1/+1 counters on her."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withAnyCounter()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.MayPay(ManaCost.parse("{U}"), Effects.DrawCards(1))
        description = "Whenever a creature you control with a counter on it deals combat damage to a player, you may pay {U}. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "April Prime"
        flavorText = "Built from scraps. Defined by self."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcb59b16-3d76-478e-9306-969dd2cb1b5a.jpg?1771586980"
    }
}
