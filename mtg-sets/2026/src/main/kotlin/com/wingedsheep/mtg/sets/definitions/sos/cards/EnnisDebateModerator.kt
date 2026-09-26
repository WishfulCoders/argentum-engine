package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ennis, Debate Moderator — Secrets of Strixhaven #14
 * {1}{W} · Legendary Creature — Human Cleric · 1/1
 *
 * When Ennis enters, exile up to one other target creature you control. Return that card to the
 * battlefield under its owner's control at the beginning of the next end step.
 * At the beginning of your end step, if one or more cards were put into exile this turn, put a
 * +1/+1 counter on Ennis.
 *
 * The ETB blink mirrors Kykar, Zephyr Awakener: [Effects.Exile] the targeted creature, then a
 * [CreateDelayedTriggerEffect] returns it (under its owner's control by default) at the next end
 * step. The target is "up to one" — an optional [TargetCreature]; declining exiles nothing.
 *
 * The end-step trigger is an intervening-if (CR 603.4) on [Conditions.CardsPutIntoExileThisTurn],
 * a game-wide count of cards put into exile this turn (so Ennis's own blink — or any other exile —
 * grows it). See the `CARDS_PUT_INTO_EXILE` turn tracker.
 */
val EnnisDebateModerator = card("Ennis, Debate Moderator") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "When Ennis enters, exile up to one other target creature you control. Return " +
        "that card to the battlefield under its owner's control at the beginning of the next end " +
        "step.\nAt the beginning of your end step, if one or more cards were put into exile this " +
        "turn, put a +1/+1 counter on Ennis."

    // ETB: exile up to one other target creature you control, return at next end step.
    triggeredAbility {
        val creature = target(TargetFilter.OtherCreatureYouControl, optional = true)
        trigger = Triggers.self.enters()
        effect = Effects.Exile(creature) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Move(creature, Zone.BATTLEFIELD),
            )
    }

    // At the beginning of your end step, if one or more cards were put into exile this turn,
    // put a +1/+1 counter on Ennis.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CardsPutIntoExileThisTurn()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Marie Magny"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2ef31b4-24fa-4443-9f05-c8e99c3522e5.jpg?1775937005"
    }
}
