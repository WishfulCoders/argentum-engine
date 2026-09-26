package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cauldron Dance
 * {4}{B}{R}
 * Instant
 * Cast this spell only during combat.
 * Return target creature card from your graveyard to the battlefield. That creature gains
 * haste. Return it to your hand at the beginning of the next end step.
 * You may put a creature card from your hand onto the battlefield. That creature gains haste.
 * Its controller sacrifices it at the beginning of the next end step.
 */
val CauldronDance = card("Cauldron Dance") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Cast this spell only during combat.\n" +
        "Return target creature card from your graveyard to the battlefield. That creature gains " +
        "haste. Return it to your hand at the beginning of the next end step.\n" +
        "You may put a creature card from your hand onto the battlefield. That creature gains haste. " +
        "Its controller sacrifices it at the beginning of the next end step."

    spell {
        val creatureCardInYourGraveyard = target(TargetFilter.CreatureInYourGraveyard)
        castOnlyDuring(Phase.COMBAT)

        effect = Effects.Pipeline {
            // Part 1 — reanimate the targeted graveyard creature, give it haste, and bounce
            // it to its owner's hand at the next end step.
            val reanimated = gather(CardSource.ChosenTargets)
            move(reanimated, CardDestination.ToZone(Zone.BATTLEFIELD))
            ifNotEmpty(reanimated) {
                run(Effects.GrantKeyword(
                    keyword = Keyword.HASTE,
                    target = reanimated.asTarget,
                    duration = Duration.Permanent
                ) then
                    Effects.CreateDelayedTrigger(
                        step = Step.END,
                        effect = Effects.ReturnToHand(reanimated.asTarget)
                    ))
            }

            // Part 2 — optionally drop a creature from hand, give it haste, and sacrifice it
            // at the next end step.
            val candidates = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature))
            val putting = chooseUpTo(1, from = candidates)
            move(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            ifNotEmpty(putting) {
                run(Effects.GrantKeyword(
                    keyword = Keyword.HASTE,
                    target = putting.asTarget,
                    duration = Duration.Permanent
                ))
                run(Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.SacrificeTarget(putting.asTarget)
                ))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dadcae0-f2b2-487c-bb93-0a2c073044c0.jpg?1562923646"
    }
}
