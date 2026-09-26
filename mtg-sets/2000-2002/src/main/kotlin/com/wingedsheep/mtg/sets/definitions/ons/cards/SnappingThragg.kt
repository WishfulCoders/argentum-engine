package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Snapping Thragg
 * {4}{R}
 * Creature — Beast
 * 3/3
 * Whenever Snapping Thragg deals combat damage to a player, you may have it deal 3 damage
 * to target creature that player controls.
 * Morph {4}{R}{R}
 */
val SnappingThragg = card("Snapping Thragg") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "Whenever Snapping Thragg deals combat damage to a player, you may have it deal 3 damage to target creature that player controls.\nMorph {4}{R}{R}"

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.May(Effects.DealDamage(3, t))
    }

    morph = "{4}{R}{R}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Iain McCaig"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8a47d41-b893-46b9-90c9-ccd8f9f78855.jpg?1562942401"
    }
}
