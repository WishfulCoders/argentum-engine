package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fallen Shinobi
 * {3}{U}{B}
 * Creature — Zombie Ninja
 * 5/4
 *
 * Ninjutsu {2}{U}{B}
 * Whenever this creature deals combat damage to a player, that player exiles the top two cards of
 * their library. Until end of turn, you may play those cards without paying their mana costs.
 *
 * The damaged player exiles (Raven Guild Master's shape); the Shinobi's controller then gets a
 * may-play grant on exactly those two cards for the turn, paired with the free-play permission
 * (Narset's shape). Lands are still playable only with a land play available, and normal timing
 * applies — the grant changes cost, not timing (2019-06-14 ruling).
 */
val FallenShinobi = card("Fallen Shinobi") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie Ninja"
    power = 5
    toughness = 4
    oracleText = "Ninjutsu {2}{U}{B} ({2}{U}{B}, Return an unblocked attacker you control to hand: " +
        "Put this card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, that player exiles the top two cards " +
        "of their library. Until end of turn, you may play those cards without paying their mana costs."

    ninjutsu("{2}{U}{B}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(2, Player.TriggeringPlayer))
            exile(exiled, Player.TriggeringPlayer)
            run(Effects.GrantMayPlayFromExile(exiled))
            run(Effects.GrantPlayWithoutPayingCost(exiled))
        }
        description = "Whenever this creature deals combat damage to a player, that player exiles the top two " +
            "cards of their library. Until end of turn, you may play those cards without paying their mana costs."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/900c9dfd-ece1-4b09-a801-0fa05e1994b9.jpg?1783933085"

        ruling("2019-06-14", "Fallen Shinobi's triggered ability doesn't change when you can play the exiled cards. For example, if a sorcery card is exiled, you can cast it only during your main phase when the stack is empty. If a land card is exiled, you can play it only during your main phase and only if you have an available land play remaining.")
        ruling("2019-06-14", "Casting an exiled card causes it to leave exile. You can't cast it multiple times.")
        ruling("2019-06-14", "Any cards you don't cast will remain in exile.")
    }
}
