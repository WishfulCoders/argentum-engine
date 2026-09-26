package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Markov Retribution
 * {2}{R}
 * Sorcery
 *
 * Choose one or both —
 * • Creatures you control get +1/+0 until end of turn.
 * • Target Vampire you control deals damage equal to its power to another target creature.
 *
 * "Choose one or both" is modeled as three modes (pump only / bite only / both) chosen with
 * `chooseCount = 1` — the Winterflame / Overwhelming Surge shape. The pump is a group modify
 * ([Patterns.Group.modifyStatsForAll] over `creaturesYouControl`). The bite is the Itzquinth
 * idiom: the Vampire is target index 0, the victim is a [TargetOther] target creature, damage
 * equals the Vampire's power ([DynamicAmounts.targetPower(0)]) and its source is the Vampire
 * (`damageSource`). In the "both" mode the Vampire (0) and victim (1) are declared first so the
 * dynamic power reference stays index-0.
 */
val MarkovRetribution = card("Markov Retribution") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Creatures you control get +1/+0 until end of turn.\n" +
        "• Target Vampire you control deals damage equal to its power to another target creature."

    val pumpAll = Patterns.Group.modifyStatsForAll(1, 0, Filters.Group.creaturesYouControl)

    spell {
        modal(chooseCount = 1) {
            mode("Creatures you control get +1/+0 until end of turn") {
                effect = pumpAll
            }
            mode("Target Vampire you control deals damage equal to its power to another target creature") {
                val vampire = target(TargetFilter.Creature.withSubtype("Vampire").youControl())
                val victim = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
                effect = Effects.DealDamage(DynamicAmounts.powerOf(vampire), victim, damageSource = vampire)
            }
            mode(
                "Creatures you control get +1/+0 until end of turn and target Vampire you " +
                    "control deals damage equal to its power to another target creature"
            ) {
                val vampire = target(TargetFilter.Creature.withSubtype("Vampire").youControl())
                val victim = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
                effect = pumpAll then
                    Effects.DealDamage(DynamicAmounts.powerOf(vampire), victim, damageSource = vampire)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48d3840c-db27-4512-bfe3-92249094e5b4.jpg?1782703070"
    }
}
