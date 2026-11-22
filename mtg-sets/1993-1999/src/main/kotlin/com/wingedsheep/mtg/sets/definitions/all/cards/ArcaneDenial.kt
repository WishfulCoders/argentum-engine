package com.wingedsheep.mtg.sets.definitions.all.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arcane Denial — Alliances #22a (canonical printing)
 * {1}{U} · Instant
 *
 * Counter target spell. Its controller may draw up to two cards at the beginning of the next
 * turn's upkeep.
 * You draw a card at the beginning of the next turn's upkeep.
 *
 * Two step-based delayed triggers on the next turn's upkeep (`timing = NEXT_TURN`, no
 * `fireOnPlayer`, so whoever's turn comes next). "Its controller" is [EffectTarget.TargetController],
 * which the delayed-trigger executor fixes to a concrete player when the trigger is created — by the
 * time the upkeep comes the countered spell is gone. The triggers are scheduled ahead of the counter
 * so the caster is read off the spell while it is still on the stack (a spell cast by a non-owner
 * would otherwise fall back to its owner). Both happen even if the spell can't be countered; a
 * fizzled Arcane Denial (its target gone) creates neither. "May draw up to two" is a 0–2 number
 * choice made by that player when the trigger resolves, before drawing (ruling).
 */
val ArcaneDenial = card("Arcane Denial") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. Its controller may draw up to two cards at the beginning of the " +
        "next turn's upkeep.\nYou draw a card at the beginning of the next turn's upkeep."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            timing = DelayedTriggerTiming.NEXT_TURN,
            effect = Effects.DrawUpTo(2, EffectTarget.TargetController)
        ) then Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            timing = DelayedTriggerTiming.NEXT_TURN,
            effect = Effects.DrawCards(1)
        ) then Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22a"
        artist = "Richard Kane Ferguson"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0c5728e-43e7-417a-ba18-5038345cec67.jpg?1783947197"
        ruling("2007-09-16", "The controller of the countered spell doesn't choose how many cards to draw until the relevant ability resolves. The player may draw 0, 1, or 2 cards. They choose the number before drawing any cards.")
    }
}
