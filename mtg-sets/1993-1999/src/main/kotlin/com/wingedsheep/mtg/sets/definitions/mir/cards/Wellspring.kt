package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Wellspring
 * {1}{G}{W}
 * Enchantment — Aura
 * Enchant land
 * When this Aura enters, gain control of enchanted land until end of turn.
 * At the beginning of your upkeep, untap enchanted land. You gain control of
 * that land until end of turn.
 */
val Wellspring = card("Wellspring") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, gain control of enchanted land until end of turn.\n" +
        "At the beginning of your upkeep, untap enchanted land. You gain control of that land until end of turn."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainControl(EffectTarget.EnchantedPermanent, Duration.EndOfTurn)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Untap(EffectTarget.EnchantedPermanent) then
            Effects.GainControl(EffectTarget.EnchantedPermanent, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "288"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c69ba095-dd78-4999-87a9-63f7165846e4.jpg?1562721895"

        ruling("2004-10-04", "If Wellspring leaves the battlefield, the control effect still lasts until end of turn.")
    }
}
