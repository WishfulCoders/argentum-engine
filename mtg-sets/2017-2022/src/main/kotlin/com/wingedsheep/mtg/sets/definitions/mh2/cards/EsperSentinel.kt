package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Esper Sentinel — Modern Horizons 2 #12 (canonical printing; the earlier pmh2 #12s is a promo)
 * {W} · Artifact Creature — Human Soldier · 1/1
 *
 * Whenever an opponent casts their first noncreature spell each turn, draw a card unless that
 * player pays {X}, where X is this creature's power.
 *
 *  - The trigger is The Queen of Dale's `castsNth(1, Noncreature)`: the count reads the caster's
 *    cast history, so a noncreature spell cast before the Sentinel arrived closes the window
 *    (ruling).
 *  - "Draw unless that player pays {X}" is a [Effects.MayPay] whose *decision maker and payer* are
 *    the caster ([Player.TriggeringPlayer]) and whose `otherwise` is your draw. X is read when the
 *    ability resolves, from the Sentinel's last-known power if it has left (ruling); a negative
 *    power pays {0}, which the caster may still decline (ruling). An unaffordable X skips the prompt
 *    straight to the draw.
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
        effect = Effects.MayPay(
            cost = Effects.PayDynamicMana(
                amount = DynamicAmounts.sourcePower(),
                payer = Player.TriggeringPlayer
            ),
            then = Effects.Nothing,
            otherwise = Effects.DrawCards(1),
            decisionMaker = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            descriptionOverride = "Pay {X}, where X is Esper Sentinel's power, so its controller doesn't draw a card?"
        )
        description = "Whenever an opponent casts their first noncreature spell each turn, draw a card " +
            "unless that player pays {X}, where X is this creature's power."
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
