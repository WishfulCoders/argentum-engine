package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Journey to Nowhere
 * {1}{W}
 * Enchantment
 * When this enchantment enters, exile target creature.
 * When this enchantment leaves the battlefield, return the exiled card to the battlefield under its
 * owner's control.
 *
 * Two separate triggers, not a duration: if the Journey leaves before its enters trigger resolves,
 * the leaves trigger finds nothing and the creature is exiled for good (2009-10-01 ruling). So the
 * exile is a plain `Move` to exile linked to the source (Synod Sanctum's shape) plus a linked
 * return, not `ExileUntilLeaves`, which skips the exile once its source has left the battlefield
 * (the "until … leaves the battlefield" template).
 */
val JourneyToNowhere = card("Journey to Nowhere") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target creature.\n" +
        "When this enchantment leaves the battlefield, return the exiled card to the battlefield under its owner's control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val exiled = target(TargetFilter.Creature)
        effect = Effects.Move(exiled, Zone.EXILE, linkToSource = true)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09cfe585-8a55-4b27-89e0-dfb6946fe1f3.jpg?1783942173"
        ruling("2009-10-01", "If Journey to Nowhere leaves the battlefield before its first ability has resolved, its second ability will trigger and do nothing. Then its first ability will resolve and exile the targeted creature forever.")
    }
}
