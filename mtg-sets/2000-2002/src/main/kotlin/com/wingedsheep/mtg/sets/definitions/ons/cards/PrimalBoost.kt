package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Primal Boost
 * {2}{G}
 * Instant
 * Target creature gets +4/+4 until end of turn.
 * Cycling {2}{G}
 * When you cycle Primal Boost, you may have target creature get +1/+1 until end of turn.
 */
val PrimalBoost = card("Primal Boost") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +4/+4 until end of turn.\nCycling {2}{G}\nWhen you cycle Primal Boost, you may have target creature get +1/+1 until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            power = 4,
            toughness = 4,
            target = t
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}{G}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        val t = target(TargetFilter.Creature)
        effect = Effects.May(
            Effects.ModifyStats(
                power = 1,
                toughness = 1,
                target = t
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "277"
        artist = "Eric Peterson"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1b91a5a-9328-4fc6-a2f6-a7879281e145.jpg?1562952412"
    }
}
