package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Kraven the Hunter
 * {1}{B}{G}
 * Legendary Creature — Human Warrior Villain
 * 4/3
 *
 * Trample
 * Whenever a creature an opponent controls with the greatest power among creatures
 * that player controls dies, draw a card and put a +1/+1 counter on Kraven the Hunter.
 *
 * Tie-handling follows the standard ruling for "with the greatest power among" (e.g.,
 * Toralf, God of Fury): if multiple of that opponent's creatures share the maximum
 * power, each tied creature qualifies and triggers Kraven separately. The filter is
 * implemented via [GameObjectFilter.hasGreatestPower] + `opponentControls()`, which
 * compares last-known power against the dying creature's last-known controller's
 * surviving creatures and so handles stolen creatures and combat deaths uniformly.
 */
val KravenTheHunter = card("Kraven the Hunter") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Warrior Villain"
    power = 4
    toughness = 3
    oracleText = "Trample\n" +
        "Whenever a creature an opponent controls with the greatest power among " +
        "creatures that player controls dies, draw a card and put a +1/+1 counter " +
        "on Kraven the Hunter."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls().hasGreatestPower()).dies()
        effect = Effects.DrawCards(1) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Greg Staples"
        flavorText = "\"I have found honor, not in the civilized, but in the primal.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afdab464-3674-449b-be01-1cbd21fced23.jpg?1757377713"
    }
}
