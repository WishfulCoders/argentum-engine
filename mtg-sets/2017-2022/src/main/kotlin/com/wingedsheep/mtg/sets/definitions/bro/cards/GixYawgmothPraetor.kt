package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gix, Yawgmoth Praetor
 * {1}{B}{B}
 * Legendary Creature — Phyrexian Praetor
 * 3/3
 *
 * Whenever a creature deals combat damage to one of your opponents, its controller may pay 1 life.
 * If they do, they draw a card.
 * {4}{B}{B}{B}, Discard X cards: Exile the top X cards of target opponent's library. You may play
 * lands and cast spells from among cards exiled this way without paying their mana costs.
 *
 * **Draw trigger** — the same ANY-bound combat-damage observer as Gonti, Night Minister: "a
 * creature" binds the damaging creature as the triggering entity, so "its controller" is
 * [EffectTarget.ControllerOfTriggeringEntity] — in a pod that may be one of Gix's controller's
 * opponents. That player is offered the payment, pays the life, and draws.
 *
 * **Activated ability** — X is defined only by the discard ([Costs.DiscardX]): the activation
 * pauses for any number of hand cards and X is how many were discarded. The exiled cards are
 * played *during resolution* (ruling: "You can't wait and play them later"), lands included
 * ([Effects.PlayAnyNumberFromCollectionWithoutPayingCost]) — a land only on your own turn with a
 * land play left, per the rulings.
 */
val GixYawgmothPraetor = card("Gix, Yawgmoth Praetor") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    power = 3
    toughness = 3
    oracleText = "Whenever a creature deals combat damage to one of your opponents, its controller " +
        "may pay 1 life. If they do, they draw a card.\n" +
        "{4}{B}{B}{B}, Discard X cards: Exile the top X cards of target opponent's library. You " +
        "may play lands and cast spells from among cards exiled this way without paying their " +
        "mana costs."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dealsCombatDamage(Recipient.Opponent)
        effect = Effects.MayPay(
            cost = Effects.PayDynamicLife(DynamicAmounts.fixed(1), payer = Player.ControllerOfTriggeringEntity),
            then = Effects.DrawCards(1, EffectTarget.ControllerOfTriggeringEntity),
            decisionMaker = EffectTarget.ControllerOfTriggeringEntity,
        )
        description = "Whenever a creature deals combat damage to one of your opponents, its " +
            "controller may pay 1 life. If they do, they draw a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{B}{B}{B}"), Costs.DiscardX())
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.xValue(), player = Player.TargetOpponent))
            exile(exiled, Player.TargetOpponent)
            run(Effects.PlayAnyNumberFromCollectionWithoutPayingCost(exiled))
        }
        description = "Exile the top X cards of target opponent's library. You may play lands and " +
            "cast spells from among cards exiled this way without paying their mana costs."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "95"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c76f7e0-37e7-4e87-93a3-a25ba0674645.jpg?1783920090"
        ruling("2022-10-14", "You must play the cards as you resolve the last ability. You can't wait and play them later.")
        ruling("2022-10-14", "You may play a land this way only during your own turn and only if you have not yet played a land this turn.")
        ruling("2022-10-14", "If a spell you cast this way has an {X} in its mana cost, you must choose 0 for X.")
    }
}
