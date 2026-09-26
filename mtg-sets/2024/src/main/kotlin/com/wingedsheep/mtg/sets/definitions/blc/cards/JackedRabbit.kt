package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ravenous
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jacked Rabbit — Bloomburrow Commander #9
 * {X}{1}{W} · Creature — Rabbit Warrior · 1/2
 *
 * Ravenous
 * Whenever this creature attacks, create a number of 1/1 white Rabbit creature tokens equal to
 * this creature's power.
 *
 * The token count is the Rabbit's power as the trigger resolves — a value read, so it falls back
 * to last-known power if the Rabbit has left the battlefield by then (per the ruling).
 */
val JackedRabbit = card("Jacked Rabbit") {
    manaCost = "{X}{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rabbit Warrior"
    power = 1
    toughness = 2
    oracleText = "Ravenous (This creature enters with X +1/+1 counters on it. If X is 5 or more, draw a card " +
        "when it enters.)\n" +
        "Whenever this creature attacks, create a number of 1/1 white Rabbit creature tokens equal to this " +
        "creature's power."

    ravenous()

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            count = DynamicAmounts.powerOf(EffectTarget.Self),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Rabbit"),
            imageUri = "https://cards.scryfall.io/normal/front/8/1/81de52ef-7515-4958-abea-fb8ebdcef93c.jpg?1783909772"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Scott Murphy"
        flavorText = "\"Squash. What it is and what it does.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c695df6-6bf2-4e6b-8500-e3116137ca27.jpg?1783910736"
        ruling(
            "2024-07-26",
            "The triggered ability that checks to see if X is 5 or greater refers to the value of X that was " +
                "chosen as the spell was cast, which may be different from the number of counters it entered " +
                "with if there are replacement effects involved."
        )
        ruling(
            "2024-07-26",
            "If Jacked Rabbit leaves the battlefield while its last ability is on the stack, use its power as it " +
                "last existed on the battlefield to determine how many Rabbit tokens to create."
        )
    }
}
