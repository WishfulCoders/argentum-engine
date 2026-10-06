package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Esper Sentinel
 * {W}
 * Artifact Creature — Human Soldier
 * 1/1
 *
 * Whenever an opponent casts their first noncreature spell each turn, draw a card unless that
 * player pays {X}, where X is this creature's power.
 *
 * "First noncreature spell each turn" counts every noncreature spell that opponent cast this turn,
 * including ones cast before Esper Sentinel was on the battlefield (2021-06-18 ruling). X is read as
 * the ability resolves — by last-known information if Esper Sentinel has left — and a negative X is
 * {0}, which the opponent may still decline to pay ([Costs.pay.PayDynamicMana]).
 */
val EsperSentinel = card("Esper Sentinel") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Whenever an opponent casts their first noncreature spell each turn, draw a card unless " +
        "that player pays {X}, where X is this creature's power."

    triggeredAbility {
        trigger = Triggers.anOpponent.castsNth(1, GameObjectFilter.Noncreature)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.PayDynamicMana(DynamicAmounts.sourcePower()),
            suffer = Effects.DrawCards(1),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            consequenceDescription = "let Esper Sentinel's controller draw a card",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Eric Deschamps"
        flavorText = "The more Esper changes, the more he refuses to."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3537373-ef54-4578-9d05-6216420ee349.jpg?1783926893"
        ruling("2021-06-18", "If a noncreature spell was already cast by an opponent the turn Esper Sentinel enters the battlefield, that opponent already cast their first noncreature spell this turn, and Esper Sentinel's ability won't trigger for that opponent that turn.")
        ruling("2021-06-18", "This ability checks Esper Sentinel's power when it resolves, not when the ability goes on the stack. If Esper Sentinel is no longer on the battlefield when it resolves, use the power it had the last time it was on the battlefield.")
        ruling("2021-06-18", "If Esper Sentinel's has negative power when this ability resolves, then {X} is {0}. The opponent may still choose not to pay the cost if they want you to draw a card.")
    }
}
