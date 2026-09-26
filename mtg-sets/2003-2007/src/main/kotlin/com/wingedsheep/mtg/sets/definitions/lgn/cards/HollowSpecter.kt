package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Hollow Specter
 * {1}{B}{B}
 * Creature — Specter
 * 2/2
 * Flying
 * Whenever Hollow Specter deals combat damage to a player, you may pay {X}.
 * If you do, that player reveals X cards from their hand and you choose one of them.
 * That player discards that card.
 */
val HollowSpecter = card("Hollow Specter") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Specter"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever Hollow Specter deals combat damage to a player, you may pay {X}. If you do, that player reveals X cards from their hand and you choose one of them. That player discards that card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.MayPayX(
            then = Effects.Pipeline {
                // 1. Gather all cards from damaged player's hand
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.TriggeringPlayer))
                // 2. Damaged player chooses X cards to reveal
                val revealed = chooseExactly(DynamicAmounts.xValue(), from = hand, chooser = Chooser.TriggeringPlayer)
                // 3. Controller chooses 1 card to discard
                val toDiscard = chooseExactly(1, from = revealed, chooser = Chooser.Controller)
                // 4. Move chosen card to damaged player's graveyard
                discard(toDiscard, Player.TriggeringPlayer)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2db779fd-0e01-417b-aee2-786db2c0b8c8.jpg?1562904303"
        ruling("2004-10-04", "You decide on the value of X and pay {X} during resolution.")
        ruling("2004-10-04", "X can be zero, but then that player discards nothing.")
    }
}
