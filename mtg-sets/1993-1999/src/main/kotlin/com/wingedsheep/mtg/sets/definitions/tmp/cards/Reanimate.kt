package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reanimate — Tempest #151 (canonical printing)
 * {B} · Sorcery
 *
 * Put target creature card from a graveyard onto the battlefield under your control. You lose life
 * equal to that card's mana value.
 *
 * The mana value is the *card's* in the graveyard (ruling 2025-09-19), so it is frozen with
 * [Effects.StoreNumber] before the move — a reanimated Clone that enters as a copy of something
 * else must still cost its own mana value. The life loss itself happens after the creature is on
 * the battlefield, as printed, so its enters triggers resolve after it.
 */
val Reanimate = card("Reanimate") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Put target creature card from a graveyard onto the battlefield under your control. " +
        "You lose life equal to that card's mana value."

    spell {
        val creature = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.StoreNumber("mana_value", DynamicAmounts.manaValueOf(creature)) then
            Effects.Move(
                creature,
                Zone.BATTLEFIELD,
                controllerOverride = EffectTarget.Controller,
                fromZone = Zone.GRAVEYARD
            ) then
            Effects.LoseLife(DynamicAmounts.storedNumber("mana_value"), EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Robert Bliss"
        flavorText = "\"You will learn to earn death.\"\n—Volrath"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae1ef31c-8ca5-444c-8f39-e1d1827318f5.jpg?1783946636"
        ruling(
            "2025-09-19",
            "The amount of life you lose is determined by the mana value of the card in your graveyard, " +
                "not the creature once it's on the battlefield."
        )
        ruling("2025-09-19", "If a card in a graveyard has {X} in its mana cost, X is 0.")
    }
}
