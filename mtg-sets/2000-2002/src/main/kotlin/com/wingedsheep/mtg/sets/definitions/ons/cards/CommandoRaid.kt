package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Commando Raid
 * {2}{R}
 * Instant
 * Until end of turn, target creature you control gains "Whenever this creature deals
 * combat damage to a player, you may have it deal damage equal to its power to target
 * creature that player controls."
 */
val CommandoRaid = card("Commando Raid") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature you control gains \"Whenever this creature deals combat damage to a player, you may have it deal damage equal to its power to target creature that player controls.\""

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
                val creatureOpponentControls = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.May(Effects.DealDamage(DynamicAmounts.sourcePower(), creatureOpponentControls))
            },
            target = t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "195"
        artist = "Ron Spencer"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb237330-ac2e-411d-836c-6628f96f3262.jpg?1562936979"
    }
}
