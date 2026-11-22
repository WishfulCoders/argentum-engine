package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Harnessed Snubhorn — March of the Machine: The Aftermath #3
 * {3}{W} · Creature — Dinosaur · 2/5
 *
 * Vigilance
 * Whenever this creature deals combat damage to a player, return target artifact or enchantment
 * card from your graveyard to the battlefield.
 */
val HarnessedSnubhorn = card("Harnessed Snubhorn") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 5
    oracleText = "Vigilance\n" +
        "Whenever this creature deals combat damage to a player, return target artifact or " +
        "enchantment card from your graveyard to the battlefield."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val card = target(TargetFilter(GameObjectFilter.ArtifactOrEnchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Quintin Gleim"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/553ed1d8-4d5c-48f8-8513-031bd8977d75.jpg?1783916530"
    }
}
