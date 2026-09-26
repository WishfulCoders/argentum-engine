package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Push // Pull — Murders at Karlov Manor #250
 *
 * Pull gathers its surviving targets, moves them together under the caster's control, and records
 * only the cards that actually entered. Haste and the delayed sacrifice are then attached to each
 * moved permanent independently, so a target that becomes illegal does not affect the other one.
 */
val PushPull = card("Push // Pull") {
    layout = CardLayout.SPLIT
    colorIdentity = "WBR"

    face("Push") {
        manaCost = "{1}{W/B}"
        typeLine = "Sorcery"
        oracleText = "Destroy target tapped creature."

        spell {
            val creature = target(TargetFilter.TappedCreature)
            effect = Effects.Destroy(creature)
        }
    }

    face("Pull") {
        manaCost = "{4}{B/R}{B/R}"
        typeLine = "Sorcery"
        oracleText = "Put up to two target creature cards from a single graveyard onto the battlefield under your control. " +
            "They gain haste until end of turn. Sacrifice them at the beginning of the next end step."

        spell {
            targets(TargetFilter.CreatureInGraveyard, count = 2, optional = true, sameOwner = true)
            effect = Effects.Pipeline {
                val targets = gather(CardSource.ChosenTargets)
                val entered = moveTracked(
                    from = targets,
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                )
                run(
                    Effects.ForEachInCollection(
                        entered,
                        Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn) then
                            Effects.CreateDelayedTrigger(
                                step = Step.END,
                                effect = Effects.SacrificeTarget(EffectTarget.IterationEntity),
                            ),
                    ),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Eli Minaya"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85835473-b9b6-4f4a-bb93-fef93d5ec57b.jpg?1783912830"
    }
}
