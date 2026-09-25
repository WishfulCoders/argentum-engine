package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Moonstone Eulogist — Bloomburrow Commander #19
 * {3}{B}{B} · Creature — Bat Warlock · 4/4
 *
 * Flying
 * Whenever a creature an opponent controls dies, you create a Blood token.
 * Whenever you sacrifice an artifact, put a +1/+1 counter on this creature and you gain 1 life.
 *
 * The two abilities feed each other: cracking a Blood token is an artifact sacrifice. Both are
 * per-object triggers — a board wipe taking three opposing creatures makes three Blood tokens.
 */
val MoonstoneEulogist = card("Moonstone Eulogist") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat Warlock"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever a creature an opponent controls dies, you create a Blood token. (It's an artifact with " +
        "\"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you sacrifice an artifact, put a +1/+1 counter on this creature and you gain 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        effect = Effects.CreateBlood(1)
    }

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Artifact)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Yohann Schepacz"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13158f3b-9d85-4689-9afa-7d9a8a2d1b09.jpg?1783910733"
        ruling(
            "2024-07-26",
            "If Moonstone Eulogist and one or more creatures opponents control die at the same time, its " +
                "second ability will trigger for each of those creatures opponents controlled that died."
        )
    }
}
