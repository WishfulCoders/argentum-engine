package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marshaling the Troops
 * {1}{G}
 * Sorcery
 * Tap any number of untapped creatures you control. You gain 4 life for each creature tapped this way.
 */
val MarshalingTheTroops = card("Marshaling the Troops") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Tap any number of untapped creatures you control. You gain 4 life for each creature tapped this way."

    spell {
        effect = Effects.Pipeline {
            val untapped = gather(
                CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature.untapped())
            )
            val tapped = chooseAnyNumber(
                from = untapped,
                chooser = Chooser.Controller,
                useTargetingUI = true,
                prompt = "Tap any number of untapped creatures you control (gain 4 life for each)"
            )
            run(Effects.ForEachInCollection(tapped, Effects.Tap(EffectTarget.IterationEntity)))
            run(Effects.GainLife(tapped.count * 4))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Liu Shangying"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56e4df87-7908-4274-a07f-81d42be89f18.jpg?1783946100"
    }
}
