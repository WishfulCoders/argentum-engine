package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step

/**
 * Seymour Flux
 * {4}{B}
 * Legendary Creature — Spirit Avatar
 * 5/5
 * At the beginning of your upkeep, you may pay 1 life. If you do, draw a card and put a
 * +1/+1 counter on Seymour Flux.
 *
 * The "you may pay 1 life. If you do, …" pay-then-payoff is a [GatedEffect] with a
 * [Gate.MayPay] paying life ([PayLifeEffect]); the [then] branch draws and adds the counter
 * to the source ([EffectTarget.Self]).
 */
val SeymourFlux = card("Seymour Flux") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Spirit Avatar"
    power = 5
    toughness = 5
    oracleText = "At the beginning of your upkeep, you may pay 1 life. If you do, draw a card and " +
        "put a +1/+1 counter on Seymour Flux."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(
            cost = Effects.PayLife(1),
            then = Effects.DrawCards(1) then
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "558"
        artist = "K-SUWABE"
        flavorText = "\"Your hope ends here. And your meaningless existence with it.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5fdc78e-0815-443c-8c26-35387b6f4f37.jpg?1782686125"
    }
}
