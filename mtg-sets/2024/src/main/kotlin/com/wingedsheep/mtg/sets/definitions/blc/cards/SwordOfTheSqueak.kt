package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sword of the Squeak — Bloomburrow Commander #40
 * {2} · Artifact — Equipment
 *
 * Equipped creature gets +1/+1 for each creature you control with base power or toughness 1.
 * Whenever a Hamster, Mouse, Rat, or Squirrel you control enters, you may attach this Equipment
 * to that creature.
 * Equip {2}
 *
 * "Base power or toughness 1" reads each creature's layer-7b value (printed, copied, or set by an
 * effect — not counters or pumps), so a 1/1 Squirrel wearing the Sword still counts itself.
 */
val SwordOfTheSqueak = card("Sword of the Squeak") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each creature you control with base power or toughness 1.\n" +
        "Whenever a Hamster, Mouse, Rat, or Squirrel you control enters, you may attach this Equipment to " +
        "that creature.\n" +
        "Equip {2}"

    staticAbility {
        val smallCreatures = DynamicAmounts.count(
            Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.basePowerOrToughness(1)
        )
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = smallCreatures,
            toughnessBonus = smallCreatures,
        )
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.youControl().withAnyOfSubtypes(
                listOf(Subtype("Hamster"), Subtype.MOUSE, Subtype.RAT, Subtype.SQUIRREL)
            )
        ).enters()
        effect = Effects.May(Effects.AttachEquipment(EffectTarget.TriggeringEntity))
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3f4d41d-69a5-412f-aa25-f1acc5cacab2.jpg?1783910725"
        ruling(
            "2024-07-26",
            "Normally, a creature's base power and toughness are the power and toughness printed on the card or, " +
                "for a token, the power and toughness set by the effect that created it. If another effect sets a " +
                "creature's power and toughness to specific numbers or values, those become its base power and " +
                "toughness. If an effect modifies a creature's power and/or toughness without setting them, that is " +
                "not included when determining its base power and toughness."
        )
    }
}
