package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pollywog Prodigy — Bloomburrow Commander #15
 * {1}{U} · Creature — Frog Wizard · 1/3
 *
 * Evolve
 * Whenever an opponent casts a noncreature spell with mana value less than this creature's power,
 * draw a card.
 *
 * "Less than this creature's power" is a mana-value cap of power − 1, read against the Prodigy's
 * projected power as the spell is cast — so every evolve counter widens the net. Once triggered,
 * the draw happens even if the Prodigy shrinks or leaves (no intervening "if").
 */
val PollywogProdigy = card("Pollywog Prodigy") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Frog Wizard"
    power = 1
    toughness = 3
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)\n" +
        "Whenever an opponent casts a noncreature spell with mana value less than this creature's power, " +
        "draw a card."

    evolve()

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(
            GameObjectFilter.Noncreature.manaValueAtMostDynamic(DynamicAmounts.powerOf(EffectTarget.Self) - 1)
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Caroline Gariba"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94a020df-714b-4465-bfa2-ac15f27d8412.jpg?1783910733"
        ruling(
            "2024-07-26",
            "If evolve triggers, the stat comparison will happen again when the ability tries to resolve. If " +
                "neither stat of the new creature is greater, the ability will do nothing. If the creature that " +
                "entered the battlefield leaves the battlefield before evolve tries to resolve, use its last known " +
                "power and toughness to compare the stats."
        )
        ruling(
            "2024-07-26",
            "Once Pollywog Prodigy's last ability has triggered, it doesn't matter what happens to Pollywog " +
                "Prodigy after that. Reducing its power or removing it from the battlefield won't stop the " +
                "ability from resolving."
        )
    }
}
