package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skirk Commando
 * {1}{R}{R}
 * Creature — Goblin
 * 2/1
 * Whenever Skirk Commando deals combat damage to a player, you may have it deal 2 damage
 * to target creature that player controls.
 * Morph {2}{R}
 */
val SkirkCommando = card("Skirk Commando") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 1
    oracleText = "Whenever Skirk Commando deals combat damage to a player, you may have it deal 2 damage to target creature that player controls.\nMorph {2}{R}"

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.May(Effects.DealDamage(2, t))
    }

    morph = "{2}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "228"
        artist = "Dave Dorman"
        flavorText = "Physical prowess and a complete lack of morals are the only requirements for the job."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c870a66-4cd5-4a8d-9948-feffa7d4ff11.jpg?1562928132"
    }
}
