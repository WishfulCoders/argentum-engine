package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sheoldred, the Apocalypse — Dominaria United #107
 * {2}{B}{B} · Legendary Creature — Phyrexian Praetor · 4/5 · Mythic
 *
 * Deathtouch
 * Whenever you draw a card, you gain 2 life.
 * Whenever an opponent draws a card, they lose 2 life.
 *
 * Two per-card draw triggers: `Triggers.you.draws()` fires once per card drawn (a "draw two"
 * gains 4), and `Triggers.anOpponent.draws()` binds the drawing opponent as the triggering player
 * (the Razorkin Needlehead shape), who loses the life — life loss, not damage.
 */
val SheoldredTheApocalypse = card("Sheoldred, the Apocalypse") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    power = 4
    toughness = 5
    oracleText = "Deathtouch\n" +
        "Whenever you draw a card, you gain 2 life.\n" +
        "Whenever an opponent draws a card, they lose 2 life."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.GainLife(2)
        description = "Whenever you draw a card, you gain 2 life."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent draws a card, they lose 2 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "107"
        artist = "Chris Rahn"
        flavorText = "\"Gix failed. I shall not.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d67be074-cdd4-41d9-ac89-0a0456c4e4b2.jpg?1783921327"

        ruling(
            "2022-09-09",
            "If you and an opponent draw a card at the same time, you choose the order that the triggered " +
                "abilities will resolve in."
        )
    }
}
