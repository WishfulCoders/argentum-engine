package com.wingedsheep.mtg.sets.definitions.apc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fervent Charge
 * {1}{R}{W}{B}
 * Enchantment
 *
 * Whenever a creature you control attacks, it gets +2/+2 until end of turn.
 *
 * One trigger per attacking creature; "it" is the triggering creature.
 */
val FerventCharge = card("Fervent Charge") {
    manaCost = "{1}{R}{W}{B}"
    colorIdentity = "WBR"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control attacks, it gets +2/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        effect = Effects.ModifyStats(2, 2, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Mark Tedin"
        flavorText = "Crovax was nearly buried beneath the weight of his opponents."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d610a9d5-c650-45ad-a9b0-b55113701e05.jpg?1783945335"
    }
}
