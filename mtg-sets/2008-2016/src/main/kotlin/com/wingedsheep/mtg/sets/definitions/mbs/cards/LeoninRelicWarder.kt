package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Leonin Relic-Warder
 * {W}{W}
 * Creature — Cat Cleric
 * 2/2
 * When this creature enters, you may exile target artifact or enchantment.
 * When this creature leaves the battlefield, return the exiled card to the battlefield under its
 * owner's control.
 *
 * Two separate triggers for an artifact or enchantment: if it leaves before the enters trigger
 * resolves, the card is exiled for good (2011-06-01 ruling). So the exile is a plain `Move` to exile
 * linked to the source plus a linked return, not `ExileUntilLeaves`, which skips the exile once its
 * source has left the battlefield.
 */
val LeoninRelicWarder = card("Leonin Relic-Warder") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Cleric"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may exile target artifact or enchantment.\n" +
        "When this creature leaves the battlefield, return the exiled card to the battlefield under its owner's control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val exiled = target(TargetFilter.ArtifactOrEnchantment, optional = true)
        effect = Effects.Move(exiled, Zone.EXILE, linkToSource = true)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd0900e1-df78-466d-b747-33f22c273d67.jpg?1783941392"
        ruling("2011-06-01", "If Leonin Relic-Warder leaves the battlefield before its first ability has resolved, its second ability will trigger and when it resolves, do nothing. Then its first ability will resolve and exile the targeted artifact or enchantment forever.")
    }
}
