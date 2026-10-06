package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Inquisition of Kozilek — Rise of the Eldrazi #115 (canonical printing)
 * {B} · Sorcery
 *
 * Target player reveals their hand. You choose a nonland card from it with mana value 3 or less.
 * That player discards that card.
 *
 * Pilfer's reveal → choose → discard pipeline with the mana-value cap added to the filter, and
 * "target player" rather than "target opponent" (you may target yourself; per the ruling, you
 * still reveal your whole hand).
 */
val InquisitionOfKozilek = card("Inquisition of Kozilek") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player reveals their hand. You choose a nonland card from it with mana value " +
        "3 or less. That player discards that card."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(player))
            val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Nonland.manaValueAtMost(3),
                prompt = "Choose a nonland card with mana value 3 or less to discard",
                alwaysPrompt = true,
                showAllCards = true
            )
            discard(toDiscard, player.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Tomasz Jedruszek"
        flavorText = "You will scream out your innermost secrets just to make it stop."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a3ff5c3-0fdb-4d54-b4e5-ce7bad9953f0.jpg?1783941984"
        ruling(
            "2014-02-01",
            "If you target yourself with this spell, you must reveal your entire hand to the other " +
                "players just as any other player would."
        )
    }
}
