package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gimli, Mournful Avenger
 * {1}{R}{G}
 * Legendary Creature — Dwarf Warrior
 * 3/2
 *
 * Gimli has indestructible as long as two or more creatures died under your control this turn.
 * Whenever another creature you control dies, put a +1/+1 counter on Gimli. When this ability
 * resolves for the third time this turn, Gimli fights up to one target creature you don't control.
 */
val GimliMournfulAvenger = card("Gimli, Mournful Avenger") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "GR"
    typeLine = "Legendary Creature — Dwarf Warrior"
    power = 3
    toughness = 2
    oracleText = "Gimli has indestructible as long as two or more creatures died under your control this turn.\n" +
        "Whenever another creature you control dies, put a +1/+1 counter on Gimli. When this ability resolves for the third time this turn, Gimli fights up to one target creature you don't control."

    // Gimli has indestructible as long as two or more creatures died under your control this turn.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.CompareAmounts(
                left = DynamicAmounts.creaturesDiedThisTurn(Player.You),
                operator = ComparisonOperator.GTE,
                right = 2
            )
        )
    }

    // Whenever another creature you control dies, put a +1/+1 counter on Gimli.
    // When this ability resolves for the third time this turn, Gimli fights up to one
    // target creature you don't control.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(3),
                then = Effects.ReflexiveTrigger(
                    action = Effects.Nothing,
                    optional = false,
                    descriptionOverride = "Gimli fights up to one target creature you don't control"
                ) {
                    val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls, optional = true)
                    effect = Effects.Fight(EffectTarget.Self, creatureOpponentControls)
                }
            )
        description = "Whenever another creature you control dies, put a +1/+1 counter on Gimli. When this ability resolves for the third time this turn, Gimli fights up to one target creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Bartłomiej Gaweł"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df4be38c-3f93-4ff4-bff4-94753b96f2f3.jpg?1686969830"
    }
}
