package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hardlight Containment
 * {W}
 * Enchantment — Aura
 * Enchant artifact you control
 * When this Aura enters, exile target creature an opponent controls until this Aura leaves the battlefield.
 * Enchanted permanent has ward {1}.
 */
val HardlightContainment = card("Hardlight Containment") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact you control\nWhen this Aura enters, exile target creature an opponent controls until this Aura leaves the battlefield.\nEnchanted permanent has ward {1}."

    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Artifact.youControl()))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ExileUntilLeaves(creature)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    staticAbility { ability = GrantWard(WardCost.Mana("{1}")) }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Dominik Mayer"
        flavorText = "\"I have forgotten sleep in their prison.\"\n—Korelaa, prison escapee"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b934241-4d6b-4b9c-99f1-c49cb387cf56.jpg?1752946630"
    }
}
