package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Network Disruptor — Kamigawa: Neon Dynasty #71 (canonical printing)
 * {U} · Artifact Creature — Moonfolk Rogue · 1/1
 *
 * Flying
 * When this creature enters, tap target permanent.
 *
 * "Target permanent", not "target creature" — a land or an artifact is a legal target, which is
 * what makes this a one-mana tempo play rather than a combat trick.
 */
val NetworkDisruptor = card("Network Disruptor") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Moonfolk Rogue"
    power = 1
    toughness = 1
    oracleText = "Flying\nWhen this creature enters, tap target permanent."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent)
        effect = Effects.Tap(t)
        description = "When this creature enters, tap target permanent."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71"
        artist = "Viko Menezes"
        flavorText = "\"Just as I suspected. They thought they'd hidden the terminal, so they got " +
            "lazy with the encryption.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a37719a-2a87-4870-8f25-12544ca2f2cf.jpg?1783923897"
    }
}
