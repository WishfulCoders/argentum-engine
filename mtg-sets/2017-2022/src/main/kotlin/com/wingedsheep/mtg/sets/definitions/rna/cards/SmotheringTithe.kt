package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Smothering Tithe — Ravnica Allegiance #22 (canonical printing)
 * {3}{W} · Enchantment
 *
 * Whenever an opponent draws a card, that player may pay {2}. If the player doesn't, you create a
 * Treasure token.
 *
 * Rhystic Study's shape: [Effects.PayOrSuffer] with the triggering opponent as the payer. The
 * suffer branch runs in the trigger's context, so the Treasure goes to Smothering Tithe's
 * controller. One trigger per card drawn, each paid for separately.
 */
val SmotheringTithe = card("Smothering Tithe") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent draws a card, that player may pay {2}. If the player doesn't, " +
        "you create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one " +
        "mana of any color.\")"

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{2}"),
            suffer = Effects.CreateTreasure(
                1,
                imageUri = "https://cards.scryfall.io/normal/front/0/5/0559f9f3-eff0-465d-93c1-e875a8afe87f.jpg?1783933603"
            ),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            consequenceDescription = "let Smothering Tithe's controller create a Treasure token",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Mark Behm"
        flavorText = "\"I await your donation.\"\n—Dasha, Orzhov priest"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7af082fa-86a3-4f7b-966d-2be1f1d0c0bc.jpg?1783933717"
        ruling(
            "2023-09-01",
            "If an opponent is instructed to draw multiple cards, that player draws all of them before deciding " +
                "how many times to pay as the multiple triggered abilities from Smothering Tithe resolve."
        )
    }
}
