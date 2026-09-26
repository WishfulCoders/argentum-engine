package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.core.Step

/**
 * Entrails Feaster
 * {B}
 * Creature — Zombie Cat
 * 1/1
 * At the beginning of your upkeep, you may exile a creature card from a graveyard.
 * If you do, put a +1/+1 counter on Entrails Feaster.
 * If you don't, tap Entrails Feaster.
 */
val EntrailsFeaster = card("Entrails Feaster") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Cat"
    power = 1
    toughness = 1
    oracleText = "At the beginning of your upkeep, you may exile a creature card from a graveyard. If you do, put a +1/+1 counter on Entrails Feaster. If you don't, tap Entrails Feaster."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        optional = true
        val t = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(t, Zone.EXILE) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        elseEffect = Effects.Tap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "John Matson"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdddab92-3e1f-49dc-afd0-8c84d0d952c2.jpg?1562943619"
    }
}
