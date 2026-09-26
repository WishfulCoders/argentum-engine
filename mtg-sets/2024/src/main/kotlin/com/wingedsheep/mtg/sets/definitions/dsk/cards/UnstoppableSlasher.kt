package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.CounterType
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
import com.wingedsheep.sdk.scripting.events.Recipient

val UnstoppableSlasher = card("Unstoppable Slasher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Assassin"
    oracleText = "Deathtouch\n" +
        "Whenever this creature deals combat damage to a player, they lose half their life, rounded up.\n" +
        "When this creature dies, if it had no counters on it, return it to the battlefield tapped under its owner's control with two stun counters on it."
    power = 2
    toughness = 3

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.LoseHalfLife(
            roundUp = true,
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            lifePlayer = Player.TriggeringPlayer
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.lastKnownCounterCount(),
                ComparisonOperator.EQ,
                0
            ),
            then = Effects.PutOntoBattlefield(EffectTarget.Self, tapped = true) then
                Effects.AddCounters(CounterType.STUN, 2, EffectTarget.Self)
        )
        description = "When this creature dies, if it had no counters on it, return it to the battlefield tapped under its owner's control with two stun counters on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c78da035-6b5b-4136-9ab6-f622b64fdc54.jpg?1726286292"
    }
}
