package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Biomass Mutation
 * {X}{G/U}{G/U}
 * Instant
 * Creatures you control have base power and toughness X/X until end of turn.
 *
 * The group is gathered when the spell resolves, so creatures that arrive later in the turn are
 * unaffected (CR 611.2c). Setting base P/T is layer 7b, so it overwrites earlier setting effects
 * while pumps, counters and switches still apply on top — the same shape as Mirror Entity.
 */
val BiomassMutation = card("Biomass Mutation") {
    manaCost = "{X}{G/U}{G/U}"
    colorIdentity = "UG"
    typeLine = "Instant"
    oracleText = "Creatures you control have base power and toughness X/X until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.SetBasePowerAndToughness(
                power = DynamicAmounts.xValue(),
                toughness = DynamicAmounts.xValue(),
                target = EffectTarget.IterationEntity,
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Dan Murayama Scott"
        flavorText = "\"A regression to ancient forms? A glimpse of future evolutions? Or a fleeting " +
            "alteration of the present?\"\n—Vorel of the Hull Clade"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbe0a11d-0390-437e-8ece-3229863c76db.jpg?1783940096"

        ruling(
            "2013-01-24",
            "Choosing 0 as the value for X will likely cause creatures you control to become 0/0 " +
                "and be put into the graveyard."
        )
        ruling(
            "2013-01-24",
            "Biomass Mutation overwrites any effects that set the power and/or toughness of a " +
                "creature you control to a specific value. Effects that modify power and/or " +
                "toughness but don't set them to a specific value (like the one created by Giant " +
                "Growth), power/toughness changes from counters, and effects that switch a " +
                "creature's power and toughness will continue to apply."
        )
    }
}
