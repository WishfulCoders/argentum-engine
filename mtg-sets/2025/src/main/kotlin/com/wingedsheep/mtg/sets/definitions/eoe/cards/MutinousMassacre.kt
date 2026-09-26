package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mutinous Massacre
 * {3}{B}{B}{R}{R}
 * Sorcery
 * Choose odd or even. Destroy each creature with mana value of the chosen quality.
 * Then gain control of all creatures until end of turn. Untap them.
 * They gain haste until end of turn. (Zero is even.)
 *
 * Modeled as a 2-mode modal spell: the player chooses Odd or Even, the chosen mode wipes
 * creatures with the matching parity, then takes the Insurrection-style swing on the
 * survivors.
 */
val MutinousMassacre = card("Mutinous Massacre") {
    manaCost = "{3}{B}{B}{R}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Choose odd or even. Destroy each creature with mana value of the chosen quality. " +
        "Then gain control of all creatures until end of turn. Untap them. " +
        "They gain haste until end of turn. (Zero is even.)"

    spell {
        modal(chooseCount = 1) {
            mode("Odd") {
                effect = Effects.DestroyAll(GameObjectFilter.Creature.manaValueIsOdd()) then
                    Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.GainControl(EffectTarget.IterationEntity, Duration.EndOfTurn)) then
                    Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.Untap(EffectTarget.IterationEntity)) then
                    Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn))
            }
            mode("Even") {
                effect = Effects.DestroyAll(GameObjectFilter.Creature.manaValueIsEven()) then
                    Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.GainControl(EffectTarget.IterationEntity, Duration.EndOfTurn)) then
                    Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.Untap(EffectTarget.IterationEntity)) then
                    Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Quintin Gleim"
        flavorText = "Kavaron's degradation poisoned many hearts, which the Monoists were happy to exploit."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42d5034f-18f0-4d57-9840-6be52c286247.jpg?1752947466"

        ruling("2025-07-25", "If a creature has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
        ruling("2025-07-25", "You'll untap all creatures, including those you already control. They'll also gain haste even if they were already untapped.")
    }
}
