package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Covert Technician — Kamigawa: Neon Dynasty #49 (canonical printing)
 * {2}{U} · Artifact Creature — Human Ninja · 2/4
 *
 * Ninjutsu {1}{U}
 * Whenever this creature deals combat damage to a player, you may put an artifact card with mana
 * value less than or equal to that damage from your hand onto the battlefield.
 *
 * "That damage" is read off the triggering damage event (the Glamdring shape), stored as the cap,
 * then the hand's artifact cards are filtered to MV ≤ cap and the player chooses up to one to put
 * onto the battlefield (the Anticausal Vestige shape) — choosing none is the "may" decline.
 */
val CovertTechnician = card("Covert Technician") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Human Ninja"
    power = 2
    toughness = 4
    oracleText = "Ninjutsu {1}{U} ({1}{U}, Return an unblocked attacker you control to hand: Put this card " +
        "onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, you may put an artifact card with mana " +
        "value less than or equal to that damage from your hand onto the battlefield."

    ninjutsu("{1}{U}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val combatDamage = storeNumber(DynamicAmounts.triggerDamageAmount())
            val handArtifacts = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Artifact))
            val eligible = filter(handArtifacts, GameObjectFilter.Any.manaValueAtMostDynamic(combatDamage.amount))
            val chosen = chooseUpTo(
                1,
                from = eligible,
                prompt = "Choose an artifact card to put onto the battlefield (optional)"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
        description = "Whenever this creature deals combat damage to a player, you may put an artifact card " +
            "with mana value less than or equal to that damage from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbae9991-43a2-4afb-a882-fff6b0d89d12.jpg?1783923906"
    }
}
