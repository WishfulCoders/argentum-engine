package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ambrosia Whiteheart
 * {1}{W}
 * Legendary Creature — Bird
 * 2/2
 *
 * Flash
 * When Ambrosia Whiteheart enters, you may return another permanent you control to its owner's hand.
 * Landfall — Whenever a land you control enters, Ambrosia Whiteheart gets +1/+0 until end of turn.
 */
val AmbrosiaWhiteheart = card("Ambrosia Whiteheart") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flash\n" +
        "When Ambrosia Whiteheart enters, you may return another permanent you control to its owner's hand.\n" +
        "Landfall — Whenever a land you control enters, Ambrosia Whiteheart gets +1/+0 until end of turn."

    keywords(Keyword.FLASH)

    // When Ambrosia enters, you may return another permanent you control to its owner's hand.
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.Permanent.youControl(), excludeSelf = true), optional = true)
        effect = Effects.ReturnToHand(t)
    }

    // Landfall — Whenever a land you control enters, Ambrosia gets +1/+0 until end of turn.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "Landfall — Whenever a land you control enters, Ambrosia Whiteheart gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2596767-7d19-4110-86ed-3cfc93ac7483.jpg?1748705777"
    }
}
