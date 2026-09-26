package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Retreat to Valakut
 * {2}{R}
 * Enchantment
 * Landfall — Whenever a land you control enters, choose one —
 * • Target creature gets +2/+0 until end of turn.
 * • Target creature can't block this turn.
 */
val RetreatToValakut = card("Retreat to Valakut") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Landfall — Whenever a land you control enters, choose one —\n" +
        "• Target creature gets +2/+0 until end of turn.\n" +
        "• Target creature can't block this turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = ModalEffect.chooseOne(
            mode("Target creature gets +2/+0 until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(2, 0, creature)
            },
            mode("Target creature can't block this turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.CantBlock(creature)
            },
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7a9ce97-ef6d-468c-b389-8d19c76174c2.jpg?1783938193"
    }
}
