package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Targets

/**
 * Jinxed Choker — Mirrodin #189
 * {3} · Artifact
 *
 * At the beginning of your end step, target opponent gains control of this artifact and puts a
 * charge counter on it.
 * At the beginning of your upkeep, this artifact deals damage to you equal to the number of charge
 * counters on it.
 * {3}: Put a charge counter on this artifact or remove one from it.
 *
 * The activated ability chooses its action on resolution, as the card's ruling requires. Removing
 * is only offered while a charge counter exists; otherwise adding is the sole legal choice.
 */
val JinxedChoker = card("Jinxed Choker") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your end step, target opponent gains control of this artifact " +
        "and puts a charge counter on it.\n" +
        "At the beginning of your upkeep, this artifact deals damage to you equal to the number of " +
        "charge counters on it.\n" +
        "{3}: Put a charge counter on this artifact or remove one from it."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val opponent = target(Targets.Opponent)
        effect = Effects.GiveControl(
            permanent = EffectTarget.Self,
            newController = opponent,
        ) then
            Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.countersOnSelf(CounterType.CHARGE),
            target = EffectTarget.Controller,
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        val addCounter = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        effect = Effects.If(
            condition = Conditions.SourceHasCounter(CounterType.CHARGE),
            then = Effects.ChooseAction(
                choices = listOf(
                    EffectChoice("Put a charge counter on Jinxed Choker", addCounter),
                    EffectChoice(
                        "Remove a charge counter from Jinxed Choker",
                        Effects.RemoveCounters(CounterType.CHARGE, 1, EffectTarget.Self),
                    ),
                ),
            ),
            otherwise = addCounter,
        )
        description = "Put a charge counter on this artifact or remove one from it"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Mike Dringenberg"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/987910b0-0419-45ff-bda6-c6683fd00e49.jpg?1783944517"
        ruling("2004-12-01", "“You” is always Jinxed Choker’s current controller.")
        ruling("2004-12-01", "If you activate Jinxed Choker’s activated ability, you choose to either add or remove a counter when the ability resolves.")
    }
}
