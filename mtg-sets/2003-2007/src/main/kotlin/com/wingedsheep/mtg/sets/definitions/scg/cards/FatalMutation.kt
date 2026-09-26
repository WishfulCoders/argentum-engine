package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

val FatalMutation = card("Fatal Mutation") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nWhen enchanted creature is turned face up, destroy it. It can't be regenerated."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.turnedFaceUp()
        effect = Effects.CantBeRegenerated(EffectTarget.EnchantedCreature) then
                Effects.Move(EffectTarget.EnchantedCreature, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Erica Gassalasca-Jape"
        flavorText = "\"You wear that shell as a mask. Now it's your coffin.\" —Cabal cleric"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57cf9f50-8858-44a6-8bd5-0ce1e281a584.jpg?1562529447"
    }
}
