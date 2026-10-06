package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mishra's Foundry
 * Land
 *
 * {T}: Add {C}.
 * {2}: This land becomes a 2/2 Assembly-Worker artifact creature until end of turn. It's still a land.
 * {1}, {T}: Target attacking Assembly-Worker gets +2/+2 until end of turn.
 *
 * The Mishra's Factory shape: [Effects.BecomeCreature] with `addTypes = ARTIFACT` keeps the Land type
 * (Layer 4 additions are additive). The pump targets an *attacking* Assembly-Worker — only creatures
 * attack, so the filter is creature + Assembly-Worker + attacking, and it can target the Foundry itself
 * only if it attacked without tapping (vigilance), since the pump needs {T}.
 */
val MishrasFoundry = card("Mishra's Foundry") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{2}: This land becomes a 2/2 Assembly-Worker artifact creature until end of turn. " +
        "It's still a land.\n" +
        "{1}, {T}: Target attacking Assembly-Worker gets +2/+2 until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 2,
            toughness = 2,
            creatureTypes = setOf(Subtype.ASSEMBLY_WORKER.value),
            addTypes = setOf(CardType.ARTIFACT.name),
            duration = Duration.EndOfTurn,
        )
        description = "{2}: This land becomes a 2/2 Assembly-Worker artifact creature until end of " +
            "turn. It's still a land."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val worker = target(
            TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.ASSEMBLY_WORKER).attacking()),
        )
        effect = Effects.ModifyStats(2, 2, worker, Duration.EndOfTurn)
        description = "{1}, {T}: Target attacking Assembly-Worker gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "265"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da7699b2-e1af-4bc0-8c5b-84ba3e868d7c.jpg?1783920005"
        ruling(
            "2022-10-14",
            "If Mishra's Foundry becomes a creature and you haven't controlled it continuously since the " +
                "beginning of your most recent turn, you won't be able to activate its first or last " +
                "abilities, and it won't be able to attack (unless it somehow gains haste).",
        )
        ruling(
            "2022-10-14",
            "If Mishra's Foundry is already a creature, activating the second ability will override any " +
                "previous effects that set its power and/or toughness to specific values. Other effects " +
                "that are affecting its power and/or toughness, including +1/+1 counters and the effects " +
                "of spells like Giant Growth, will continue to apply.",
        )
    }
}
