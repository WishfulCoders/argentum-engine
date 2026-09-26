package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * War Elemental — Mirrodin #112
 * {R}{R}{R} · Creature — Elemental · 1/1
 *
 * When this creature enters, sacrifice it unless an opponent was dealt damage this turn.
 * Whenever an opponent is dealt damage, put that many +1/+1 counters on this creature.
 *
 * The entry trigger checks opponents' accumulated damage when it resolves; it is not an
 * intervening-if trigger. The second ability observes every damage source and reads the amount
 * actually dealt from the trigger context.
 */
val WarElemental = card("War Elemental") {
    manaCost = "{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "When this creature enters, sacrifice it unless an opponent was dealt damage this turn.\n" +
        "Whenever an opponent is dealt damage, put that many +1/+1 counters on this creature."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.damageReceivedThisTurn(Player.EachOpponent),
                ComparisonOperator.LT,
                1,
            ),
            then = Effects.SacrificeTarget(EffectTarget.Self),
        )
    }

    triggeredAbility {
        trigger = Triggers.a().dealsDamage(Recipient.Opponent)
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cc32bfc-7d87-46d9-a424-eac64eefd7ea.jpg?1783944535"
    }
}
