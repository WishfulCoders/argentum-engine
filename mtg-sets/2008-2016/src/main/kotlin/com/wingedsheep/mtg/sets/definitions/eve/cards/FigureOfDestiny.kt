package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Figure of Destiny
 * {R/W}
 * Creature — Kithkin
 * 1/1
 *
 * {R/W}: This creature becomes a Kithkin Spirit with base power and toughness 2/2.
 * {R/W}{R/W}{R/W}: If this creature is a Spirit, it becomes a Kithkin Spirit Warrior with base power and toughness 4/4.
 * {R/W}{R/W}{R/W}{R/W}{R/W}{R/W}: If this creature is a Warrior, it becomes a Kithkin Spirit Warrior Avatar
 * with base power and toughness 8/8, flying, and first strike.
 *
 * Per the 2008-08-01 rulings, the second and third abilities check the creature types on
 * resolution, so the gate is an [Effects.If] inside the effect rather than an activation
 * restriction. None of the effects has a duration.
 */
val FigureOfDestiny = card("Figure of Destiny") {
    manaCost = "{R/W}"
    colorIdentity = "WR"
    typeLine = "Creature — Kithkin"
    power = 1
    toughness = 1
    oracleText = "{R/W}: This creature becomes a Kithkin Spirit with base power and toughness 2/2.\n" +
        "{R/W}{R/W}{R/W}: If this creature is a Spirit, it becomes a Kithkin Spirit Warrior with base power and toughness 4/4.\n" +
        "{R/W}{R/W}{R/W}{R/W}{R/W}{R/W}: If this creature is a Warrior, it becomes a Kithkin Spirit Warrior Avatar with base power and toughness 8/8, flying, and first strike."

    activatedAbility {
        cost = Costs.Mana("{R/W}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 2,
            toughness = 2,
            creatureTypes = setOf("Kithkin", "Spirit"),
            duration = Duration.Permanent
        )
    }

    activatedAbility {
        cost = Costs.Mana("{R/W}{R/W}{R/W}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype.SPIRIT),
            then = Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 4,
                toughness = 4,
                creatureTypes = setOf("Kithkin", "Spirit", "Warrior"),
                duration = Duration.Permanent
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{R/W}{R/W}{R/W}{R/W}{R/W}{R/W}")
        effect = Effects.If(
            condition = Conditions.SourceHasSubtype(Subtype.WARRIOR),
            then = Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 8,
                toughness = 8,
                keywords = setOf(Keyword.FLYING, Keyword.FIRST_STRIKE),
                creatureTypes = setOf("Kithkin", "Spirit", "Warrior", "Avatar"),
                duration = Duration.Permanent
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0da69523-cece-425a-b08a-fb27fac29374.jpg?1783942664"
        ruling("2008-08-01", "None of these abilities has a duration. If one of them resolves, it will remain in effect until the game ends, Figure of Destiny leaves the battlefield, or some subsequent effect changes its characteristics, whichever comes first.")
        ruling("2008-08-01", "Figure of Destiny's abilities overwrite its power, toughness, and creature types. Typically, those abilities are activated in the order they appear on the card. However, if Figure of Destiny is an 8/8 Kithkin Spirit Warrior Avatar with flying and first strike, and you activate its first ability, it will become a 2/2 Kithkin Spirit that still has flying and first strike.")
        ruling("2008-08-01", "You can activate Figure of Destiny's second and third abilities regardless of what creature types it is. Each of those abilities checks Figure of Destiny's creature types when that ability resolves. If Figure of Destiny isn't the appropriate creature type at that time, the ability does nothing.")
    }
}
