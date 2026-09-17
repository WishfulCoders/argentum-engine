package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Back for More
 * {4}{B}{G}
 * Instant
 * Return target creature card from your graveyard to the battlefield. When you do, it fights up to
 * one target creature you don't control.
 *
 * Curse of the Werefox's shape: the fight is a reflexive trigger whose target is chosen only once
 * the creature has returned (2024-04-12 ruling), so `ReflexiveTriggerEffect` around a guarded
 * graveyard return, with "it" read back from the spell's chosen target.
 */
val BackForMore = card("Back for More") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Return target creature card from your graveyard to the battlefield. When you do, it fights up to " +
        "one target creature you don't control. (Each deals damage equal to its power to the other.)"

    spell {
        val card = target("target creature card from your graveyard", Targets.CreatureCardInYourGraveyard)
        effect = Effects.Pipeline(
            descriptionOverride = "Return target creature card from your graveyard to the battlefield. " +
                "When you do, it fights up to one target creature you don't control."
        ) {
            val returned = gather(CardSource.ChosenTargets, name = "returned")
            run(
                ReflexiveTriggerEffect(
                    action = Effects.PutOntoBattlefieldFromGraveyard(card),
                    optional = false,
                    reflexiveEffect = Effects.Fight(
                        EffectTarget.PipelineTarget(returned.key),
                        EffectTarget.ContextTarget(0)
                    ),
                    reflexiveTargetRequirements = listOf(
                        TargetCreature(optional = true, filter = TargetFilter.CreatureOpponentControls)
                    ),
                    descriptionOverride = "When you do, it fights up to one target creature you don't control."
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Daarken"
        flavorText = "\"How can you be sure you pierced its heart? Do you even know where its heart is?\"\n—Trundan, Indatha poacher"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fc7210c-da23-4cec-9195-4de75587f40f.jpg?1783931029"
        ruling("2024-04-12", "You don't choose a target creature you don't control at the time you cast Back for More. Rather, a \"reflexive\" ability triggers when you return the target creature card from your graveyard to the battlefield. You choose a target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal. Notably, Back for More isn't a crime, but the reflexive triggered ability potentially is.")
        ruling("2020-04-17", "The reflexive triggered ability from Back for More is put onto the stack at the same time as any other triggered abilities caused by the creature entering the battlefield.")
        ruling("2020-04-17", "If the target creature is an illegal target when the reflexive triggered ability tries to resolve, the ability doesn't resolve. If the creature you put onto the battlefield is no longer on the battlefield or no longer a creature, the target creature won't deal or be dealt damage.")
    }
}
