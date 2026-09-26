package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Salvation Swan
 * {3}{W}
 * Creature — Bird Cleric
 * 3/3
 *
 * Flash
 * Flying
 * Whenever this creature or another Bird you control enters, exile up to one
 * target creature you control without flying. Return it to the battlefield under
 * its owner's control with a flying counter on it at the beginning of the next
 * end step.
 */
val SalvationSwan = card("Salvation Swan") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Cleric"
    power = 3
    toughness = 3
    oracleText = "Flash\nFlying\nWhenever this creature or another Bird you control enters, exile up to one target creature you control without flying. Return it to the battlefield under its owner's control with a flying counter on it at the beginning of the next end step."

    keywords(Keyword.FLASH, Keyword.FLYING)

    // Whenever this creature or another Bird you control enters
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withSubtype(Subtype("Bird"))).enters()

        // Target up to one creature you control without flying
        val creature = target(TargetFilter.Creature.youControl().withoutKeyword(Keyword.FLYING), optional = true)

        // Exile target, then return with flying counter at end step
        effect = Effects.Move(creature, Zone.EXILE) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Move(creature, Zone.BATTLEFIELD) then
                    Effects.AddCounters(CounterType.FLYING, 1, creature)
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2656160-d319-4530-a6e5-c418596c3f12.jpg?1721425931"
    }
}
