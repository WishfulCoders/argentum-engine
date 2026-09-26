package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Riptide Entrancer
 * {1}{U}{U}
 * Creature — Human Wizard
 * 1/1
 * Whenever Riptide Entrancer deals combat damage to a player, you may sacrifice it.
 * If you do, gain control of target creature that player controls.
 * (This effect lasts indefinitely.)
 * Morph {U}{U}
 */
val RiptideEntrancer = card("Riptide Entrancer") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "Whenever Riptide Entrancer deals combat damage to a player, you may sacrifice it. If you do, gain control of target creature that player controls. (This effect lasts indefinitely.)\nMorph {U}{U}"

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.May(
            SacrificeSelfEffect then Effects.GainControl(t)
        )
    }

    morph = "{U}{U}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Scott Hampton"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cd9abc9-f289-4294-bc0f-4addc8b92a4e.jpg?1562905497"
    }
}
